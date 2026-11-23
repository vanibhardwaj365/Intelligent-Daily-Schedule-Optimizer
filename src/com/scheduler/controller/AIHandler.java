package com.scheduler.controller;

import com.scheduler.config.DBConnection;
import com.scheduler.dao.TaskDao;
import com.scheduler.model.Task;
import com.scheduler.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Backend Controller for NVIDIA Cloud AI (NIM)
 * Uses Meta Llama 3.1 / 3.3 models to analyze user's schedule and provide actionable advice.
 */
public class AIHandler implements HttpHandler {
    private final TaskDao taskDao = new TaskDao();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, X-User-Id");
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            int userId = extractUserId(exchange);
            if (userId <= 0) {
                sendResponse(exchange, 401, JsonUtil.errorResponse("Authentication required. Please log in."));
                return;
            }

            // 1. Fetch user's pending tasks
            List<Task> pendingTasks = taskDao.getPendingTasksByUser(userId);
            if (pendingTasks.isEmpty()) {
                String emptyAdvice = "You currently have no pending tasks! Click 'Add New Task' to schedule your activities, and I will analyze them for you.";
                sendResponse(exchange, 200, String.format("{\"success\":true,\"advice\":\"%s\",\"model\":\"local\"}", JsonUtil.escape(emptyAdvice)));
                return;
            }

            // 2. Build contextual prompt for NVIDIA AI
            String taskPrompt = buildTaskSummaryPrompt(pendingTasks);

            // 3. Call NVIDIA Cloud API
            String aiAdvice = callNvidiaAPI(taskPrompt);

            // 4. Return AI response
            String jsonResp = String.format("{\"success\":true,\"advice\":\"%s\",\"model\":\"%s\"}",
                    JsonUtil.escape(aiAdvice),
                    DBConnection.getProperty("ai.model", "meta/llama-3.1-8b-instruct"));

            sendResponse(exchange, 200, jsonResp);

        } catch (Exception e) {
            e.printStackTrace();
            String fallback = generateFallbackAdvice();
            String jsonResp = String.format("{\"success\":true,\"advice\":\"%s\",\"model\":\"rule-based-advisor\"}", JsonUtil.escape(fallback));
            sendResponse(exchange, 200, jsonResp);
        }
    }

    private String buildTaskSummaryPrompt(List<Task> tasks) {
        StringBuilder sb = new StringBuilder();
        sb.append("Current time is ").append(LocalDateTime.now().toLocalTime().withNano(0)).append(". ");
        sb.append("Here is the list of pending tasks for today: ");

        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            sb.append(i + 1).append(") ")
              .append(t.getTitle().replace("\"", "'"))
              .append(" [Priority: ").append(t.getPriority())
              .append(", Category: ").append(t.getCategory())
              .append(", Duration: ").append(t.getEstimatedMinutes()).append(" mins");
            if (t.getDeadline() != null) {
                sb.append(", Deadline: ").append(t.getDeadline().toString());
            }
            sb.append("]. ");
        }
        return sb.toString();
    }

    private String callNvidiaAPI(String taskPrompt) {
        String apiUrl = DBConnection.getProperty("ai.api.url", "https://integrate.api.nvidia.com/v1/chat/completions");
        String apiKey = DBConnection.getProperty("ai.api.key", "");
        String model = DBConnection.getProperty("ai.model", "meta/llama-3.1-8b-instruct");

        if (apiKey.trim().isEmpty() || apiKey.contains("your-key-here")) {
            return generateFallbackAdvice();
        }

        String systemPrompt = "You are an intelligent daily schedule advisor. "
                + "Analyze the user's tasks, deadlines, and priorities. "
                + "Provide 3 to 4 concise, crisp bullet points on the best execution strategy, order of tasks, and rest breaks. "
                + "Keep your tone motivating and professional.";

        String requestBody = "{"
                + "\"model\": \"" + model.trim() + "\","
                + "\"messages\": ["
                + "  {\"role\": \"system\", \"content\": \"" + JsonUtil.escape(systemPrompt) + "\"},"
                + "  {\"role\": \"user\", \"content\": \"" + JsonUtil.escape(taskPrompt) + "\"}"
                + "],"
                + "\"temperature\": 0.5,"
                + "\"max_tokens\": 350"
                + "}";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return extractContentFromNvidiaResponse(response.body());
            } else {
                System.err.println("[NVIDIA API Error] Status: " + response.statusCode() + " Body: " + response.body());
                return generateFallbackAdvice();
            }
        } catch (Exception e) {
            System.err.println("[NVIDIA API Connection Note] " + e.getMessage());
            return generateFallbackAdvice();
        }
    }

    private String extractContentFromNvidiaResponse(String responseJson) {
        try {
            Pattern pattern = Pattern.compile("\"content\"\\s*:\\s*\"(.*?)(?<!\\\\)\"", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(responseJson);
            if (matcher.find()) {
                String content = matcher.group(1);
                return content.replace("\\n", "\n")
                              .replace("\\\"", "\"")
                              .replace("\\\\", "\\")
                              .trim();
            }
        } catch (Exception ignored) {}
        return generateFallbackAdvice();
    }

    private String generateFallbackAdvice() {
        return "1. Tackle your **High Priority** tasks first while your focus and cognitive energy are at peak levels.\n"
             + "2. Group smaller tasks together into a 30-minute sprint to build quick momentum.\n"
             + "3. Take a 10-minute rest buffer between complex tasks to prevent burnout.\n"
             + "4. Pay special attention to tasks due in the next 3 hours to ensure timely delivery.";
    }

    private int extractUserId(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("X-User-Id");
        if (header != null && !header.isEmpty()) {
            try { return Integer.parseInt(header.trim()); } catch (NumberFormatException ignored) {}
        }
        return -1;
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
        byte[] bytes = responseText.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}

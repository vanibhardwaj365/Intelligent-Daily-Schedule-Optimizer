package com.scheduler.controller;

import com.scheduler.dao.TaskDao;
import com.scheduler.model.Task;
import com.scheduler.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class TaskHandler implements HttpHandler {
    private final TaskDao taskDao = new TaskDao();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        // Enable CORS
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, X-User-Id");
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            int userId = extractUserId(exchange);
            if (userId <= 0) {
                sendResponse(exchange, 401, JsonUtil.errorResponse("Authentication required. Please log in."));
                return;
            }

            if ("GET".equalsIgnoreCase(method)) {
                handleGetTasks(exchange, userId);
            } else if ("POST".equalsIgnoreCase(method)) {
                if (path.contains("/toggle")) {
                    handleToggleTask(exchange, userId, path);
                } else {
                    handleCreateTask(exchange, userId);
                }
            } else if ("PUT".equalsIgnoreCase(method)) {
                handleUpdateTask(exchange, userId, path);
            } else if ("DELETE".equalsIgnoreCase(method)) {
                handleDeleteTask(exchange, userId, path);
            } else {
                sendResponse(exchange, 405, JsonUtil.errorResponse("Method Not Allowed"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, JsonUtil.errorResponse("Task operation failed: " + e.getMessage()));
        }
    }

    private int extractUserId(HttpExchange exchange) {
        // Try header first
        String header = exchange.getRequestHeaders().getFirst("X-User-Id");
        if (header != null && !header.isEmpty()) {
            try { return Integer.parseInt(header.trim()); } catch (NumberFormatException ignored) {}
        }
        // Try query param
        String query = exchange.getRequestURI().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2 && ("userId".equalsIgnoreCase(pair[0]) || "user_id".equalsIgnoreCase(pair[0]))) {
                    try { return Integer.parseInt(pair[1].trim()); } catch (NumberFormatException ignored) {}
                }
            }
        }
        return -1;
    }

    private void handleGetTasks(HttpExchange exchange, int userId) throws IOException {
        try {
            List<Task> tasks = taskDao.getAllTasksByUser(userId);
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < tasks.size(); i++) {
                sb.append(tasks.get(i).toJson());
                if (i < tasks.size() - 1) sb.append(",");
            }
            sb.append("]");
            sendResponse(exchange, 200, JsonUtil.successResponse("Tasks retrieved", sb.toString()));
        } catch (Exception e) {
            sendResponse(exchange, 500, JsonUtil.errorResponse("Database error: " + e.getMessage()));
        }
    }

    private void handleCreateTask(HttpExchange exchange, int userId) throws IOException {
        String body = JsonUtil.readRequestBody(exchange.getRequestBody());
        Map<String, String> data = JsonUtil.parseJsonMap(body);

        String title = data.get("title");
        String description = data.get("description");
        String priority = data.get("priority");
        String category = data.get("category");
        String deadlineStr = data.get("deadline");
        String estMinStr = data.get("estimatedMinutes");

        if (title == null || title.trim().isEmpty() || deadlineStr == null || deadlineStr.trim().isEmpty()) {
            sendResponse(exchange, 400, JsonUtil.errorResponse("Task title and deadline are required."));
            return;
        }

        try {
            LocalDateTime deadline = parseDateTime(deadlineStr);
            int estimatedMinutes = 30;
            if (estMinStr != null && !estMinStr.trim().isEmpty()) {
                try {
                    estimatedMinutes = Integer.parseInt(estMinStr.trim());
                } catch (NumberFormatException ignored) {}
            }

            Task task = new Task();
            task.setUserId(userId);
            task.setTitle(title.trim());
            task.setDescription(description != null ? description.trim() : "");
            task.setPriority(priority != null ? priority.toUpperCase().trim() : "MEDIUM");
            task.setCategory(category != null ? category.toUpperCase().trim() : "GENERAL");
            task.setDeadline(deadline);
            task.setEstimatedMinutes(estimatedMinutes);
            task.setStatus("PENDING");

            boolean created = taskDao.createTask(task);
            if (created) {
                sendResponse(exchange, 201, JsonUtil.successResponse("Task created successfully", task.toJson()));
            } else {
                sendResponse(exchange, 500, JsonUtil.errorResponse("Failed to create task"));
            }
        } catch (Exception e) {
            sendResponse(exchange, 400, JsonUtil.errorResponse("Invalid input or date format: " + e.getMessage()));
        }
    }

    private void handleUpdateTask(HttpExchange exchange, int userId, String path) throws IOException {
        String body = JsonUtil.readRequestBody(exchange.getRequestBody());
        Map<String, String> data = JsonUtil.parseJsonMap(body);

        int taskId = extractIdFromPath(path, exchange);
        if (taskId <= 0 && data.containsKey("id")) {
            try { taskId = Integer.parseInt(data.get("id")); } catch (NumberFormatException ignored) {}
        }

        if (taskId <= 0) {
            sendResponse(exchange, 400, JsonUtil.errorResponse("Valid Task ID is required"));
            return;
        }

        try {
            Task task = taskDao.getTaskById(taskId, userId);
            if (task == null) {
                sendResponse(exchange, 404, JsonUtil.errorResponse("Task not found"));
                return;
            }

            if (data.containsKey("title") && !data.get("title").trim().isEmpty()) {
                task.setTitle(data.get("title").trim());
            }
            if (data.containsKey("description")) {
                task.setDescription(data.get("description").trim());
            }
            if (data.containsKey("priority")) {
                task.setPriority(data.get("priority").toUpperCase().trim());
            }
            if (data.containsKey("category")) {
                task.setCategory(data.get("category").toUpperCase().trim());
            }
            if (data.containsKey("deadline") && !data.get("deadline").trim().isEmpty()) {
                task.setDeadline(parseDateTime(data.get("deadline")));
            }
            if (data.containsKey("estimatedMinutes")) {
                try { task.setEstimatedMinutes(Integer.parseInt(data.get("estimatedMinutes"))); } catch (NumberFormatException ignored) {}
            }
            if (data.containsKey("status")) {
                task.setStatus(data.get("status").toUpperCase().trim());
            }

            boolean updated = taskDao.updateTask(task);
            if (updated) {
                sendResponse(exchange, 200, JsonUtil.successResponse("Task updated successfully", task.toJson()));
            } else {
                sendResponse(exchange, 500, JsonUtil.errorResponse("Failed to update task"));
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, JsonUtil.errorResponse("Error updating task: " + e.getMessage()));
        }
    }

    private void handleDeleteTask(HttpExchange exchange, int userId, String path) throws IOException {
        int taskId = extractIdFromPath(path, exchange);
        if (taskId <= 0) {
            sendResponse(exchange, 400, JsonUtil.errorResponse("Valid Task ID is required"));
            return;
        }

        try {
            boolean deleted = taskDao.deleteTask(taskId, userId);
            if (deleted) {
                sendResponse(exchange, 200, JsonUtil.successResponse("Task deleted successfully", "{\"id\":" + taskId + "}"));
            } else {
                sendResponse(exchange, 404, JsonUtil.errorResponse("Task not found or not owned by user"));
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, JsonUtil.errorResponse("Error deleting task: " + e.getMessage()));
        }
    }

    private void handleToggleTask(HttpExchange exchange, int userId, String path) throws IOException {
        int taskId = extractIdFromPath(path, exchange);
        if (taskId <= 0) {
            sendResponse(exchange, 400, JsonUtil.errorResponse("Valid Task ID is required"));
            return;
        }

        try {
            boolean toggled = taskDao.toggleTaskStatus(taskId, userId);
            if (toggled) {
                Task updated = taskDao.getTaskById(taskId, userId);
                sendResponse(exchange, 200, JsonUtil.successResponse("Task status toggled", updated != null ? updated.toJson() : "null"));
            } else {
                sendResponse(exchange, 404, JsonUtil.errorResponse("Task not found"));
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, JsonUtil.errorResponse("Error toggling task: " + e.getMessage()));
        }
    }

    private int extractIdFromPath(String path, HttpExchange exchange) {
        // Example: /api/tasks/12 or /api/tasks/12/toggle
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length; i++) {
            if ("tasks".equalsIgnoreCase(parts[i]) && i + 1 < parts.length) {
                try {
                    return Integer.parseInt(parts[i + 1]);
                } catch (NumberFormatException ignored) {}
            }
        }
        // Try query param 'id'
        String query = exchange.getRequestURI().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2 && "id".equalsIgnoreCase(pair[0])) {
                    try { return Integer.parseInt(pair[1]); } catch (NumberFormatException ignored) {}
                }
            }
        }
        return -1;
    }

    private LocalDateTime parseDateTime(String raw) {
        raw = raw.replace("T", " ").trim();
        if (raw.length() == 16) {
            raw += ":00";
        }
        return LocalDateTime.parse(raw, FORMATTER);
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
        byte[] bytes = responseText.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}

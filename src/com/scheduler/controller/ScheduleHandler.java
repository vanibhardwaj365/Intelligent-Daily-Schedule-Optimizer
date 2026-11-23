package com.scheduler.controller;

import com.scheduler.dao.TaskDao;
import com.scheduler.model.ScheduleSummary;
import com.scheduler.model.Task;
import com.scheduler.service.ScheduleOptimizer;
import com.scheduler.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ScheduleHandler implements HttpHandler {
    private final ScheduleOptimizer optimizer = new ScheduleOptimizer();
    private final TaskDao taskDao = new TaskDao();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        // Enable CORS
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
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

            if ("POST".equalsIgnoreCase(method) && path.endsWith("/optimize")) {
                handleOptimizeSchedule(exchange, userId);
            } else if ("GET".equalsIgnoreCase(method) && path.endsWith("/summary")) {
                handleGetSummary(exchange, userId);
            } else {
                sendResponse(exchange, 404, JsonUtil.errorResponse("Schedule endpoint not found"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, JsonUtil.errorResponse("Schedule operation failed: " + e.getMessage()));
        }
    }

    private int extractUserId(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("X-User-Id");
        if (header != null && !header.isEmpty()) {
            try { return Integer.parseInt(header.trim()); } catch (NumberFormatException ignored) {}
        }
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

    private void handleOptimizeSchedule(HttpExchange exchange, int userId) throws IOException {
        try {
            List<Task> optimized = optimizer.optimizeSchedule(userId);
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < optimized.size(); i++) {
                sb.append(optimized.get(i).toJson());
                if (i < optimized.size() - 1) sb.append(",");
            }
            sb.append("]");

            sendResponse(exchange, 200, JsonUtil.successResponse("Schedule intelligently optimized successfully!", sb.toString()));
        } catch (Exception e) {
            sendResponse(exchange, 500, JsonUtil.errorResponse("Optimization error: " + e.getMessage()));
        }
    }

    private void handleGetSummary(HttpExchange exchange, int userId) throws IOException {
        try {
            ScheduleSummary summary = taskDao.getScheduleSummary(userId);
            sendResponse(exchange, 200, JsonUtil.successResponse("Daily summary loaded", summary.toJson()));
        } catch (Exception e) {
            sendResponse(exchange, 500, JsonUtil.errorResponse("Error fetching summary: " + e.getMessage()));
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
        byte[] bytes = responseText.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}

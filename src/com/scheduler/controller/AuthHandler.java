package com.scheduler.controller;

import com.scheduler.dao.UserDao;
import com.scheduler.model.User;
import com.scheduler.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class AuthHandler implements HttpHandler {
    private final UserDao userDao = new UserDao();

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
            if ("POST".equalsIgnoreCase(method) && path.endsWith("/register")) {
                handleRegister(exchange);
            } else if ("POST".equalsIgnoreCase(method) && path.endsWith("/login")) {
                handleLogin(exchange);
            } else {
                sendResponse(exchange, 404, JsonUtil.errorResponse("Auth endpoint not found"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, JsonUtil.errorResponse("Internal server error: " + e.getMessage()));
        }
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        String body = JsonUtil.readRequestBody(exchange.getRequestBody());
        Map<String, String> data = JsonUtil.parseJsonMap(body);

        String username = data.get("username");
        String password = data.get("password");
        String email = data.get("email");
        String fullName = data.get("fullName");

        if (username == null || username.trim().isEmpty() ||
            password == null || password.trim().isEmpty() ||
            email == null || email.trim().isEmpty()) {
            sendResponse(exchange, 400, JsonUtil.errorResponse("Username, email, and password are required."));
            return;
        }

        try {
            if (userDao.isUsernameTaken(username)) {
                sendResponse(exchange, 400, JsonUtil.errorResponse("Username is already registered."));
                return;
            }
            if (userDao.isEmailTaken(email)) {
                sendResponse(exchange, 400, JsonUtil.errorResponse("Email address is already in use."));
                return;
            }

            User user = new User();
            user.setUsername(username.trim());
            user.setPassword(password);
            user.setEmail(email.trim());
            user.setFullName(fullName != null ? fullName.trim() : username.trim());

            boolean success = userDao.register(user);
            if (success) {
                sendResponse(exchange, 201, JsonUtil.successResponse("Registration successful! Please login.", user.toJson()));
            } else {
                sendResponse(exchange, 500, JsonUtil.errorResponse("Failed to register user."));
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, JsonUtil.errorResponse("Database error: " + e.getMessage()));
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        String body = JsonUtil.readRequestBody(exchange.getRequestBody());
        Map<String, String> data = JsonUtil.parseJsonMap(body);

        String username = data.get("username");
        String password = data.get("password");

        if (username == null || username.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            sendResponse(exchange, 400, JsonUtil.errorResponse("Please enter both username and password."));
            return;
        }

        try {
            User user = userDao.login(username, password);
            if (user != null) {
                sendResponse(exchange, 200, JsonUtil.successResponse("Login successful!", user.toJson()));
            } else {
                sendResponse(exchange, 401, JsonUtil.errorResponse("Invalid username or password."));
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, JsonUtil.errorResponse("Database error: " + e.getMessage()));
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

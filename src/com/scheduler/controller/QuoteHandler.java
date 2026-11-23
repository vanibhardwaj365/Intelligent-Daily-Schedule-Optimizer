package com.scheduler.controller;

import com.scheduler.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * Backend REST API endpoint: /api/quote
 * Returns daily inspirational & time-management quotes.
 */
public class QuoteHandler implements HttpHandler {
    private static final String[][] QUOTES = {
        {"Focus on being productive instead of busy.", "Tim Ferriss"},
        {"The key is not to prioritize what's on your schedule, but to schedule your priorities.", "Stephen Covey"},
        {"Time is what we want most, but what we use worst.", "William Penn"},
        {"Action is the foundational key to all success.", "Pablo Picasso"},
        {"Small daily improvements over time lead to stunning results.", "Robin Sharma"},
        {"You don't have to be extreme, just consistent.", "Anonymous"},
        {"Do the hard jobs first. The easy jobs will take care of themselves.", "Dale Carnegie"},
        {"It is not enough to be busy. The question is: what are we busy about?", "Henry David Thoreau"}
    };

    private final Random random = new Random();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        int index = random.nextInt(QUOTES.length);
        String[] q = QUOTES[index];

        String json = String.format("{\"quote\":\"%s\",\"author\":\"%s\"}", JsonUtil.escape(q[0]), JsonUtil.escape(q[1]));
        String response = JsonUtil.successResponse("Quote fetched", json);

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}

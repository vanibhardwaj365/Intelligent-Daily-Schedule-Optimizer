package com.scheduler.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight, zero-dependency JSON & Request utility.
 * Eliminates external Maven dependencies (Jackson/Gson) so the project runs directly with javac.
 */
public class JsonUtil {

    public static String readRequestBody(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString().trim();
    }

    /**
     * Parses simple top-level key-value pairs from JSON string
     */
    public static Map<String, String> parseJsonMap(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.trim().isEmpty()) return map;

        // Regex matches "key" : "value" or "key" : number/boolean
        Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]*)\"|([0-9.]+|true|false|null))");
        Matcher matcher = pattern.matcher(json);
        while (matcher.find()) {
            String key = matcher.group(1);
            String strVal = matcher.group(2);
            String literalVal = matcher.group(3);
            String val = (strVal != null) ? strVal : ((literalVal != null && !literalVal.equals("null")) ? literalVal : "");
            map.put(key, val);
        }
        return map;
    }

    public static String successResponse(String message, String dataJson) {
        if (dataJson == null || dataJson.isEmpty()) {
            dataJson = "null";
        }
        return String.format("{\"success\":true,\"message\":\"%s\",\"data\":%s}", escape(message), dataJson);
    }

    public static String errorResponse(String message) {
        return String.format("{\"success\":false,\"message\":\"%s\"}", escape(message));
    }

    public static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}

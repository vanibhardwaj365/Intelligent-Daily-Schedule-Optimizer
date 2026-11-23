package com.scheduler;

import com.scheduler.config.DBConnection;
import com.scheduler.controller.AuthHandler;
import com.scheduler.controller.ScheduleHandler;
import com.scheduler.controller.StaticFileHandler;
import com.scheduler.controller.TaskHandler;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.Executors;

public class Main {
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        try {
            String configPort = DBConnection.getProperty("server.port", String.valueOf(DEFAULT_PORT));
            port = Integer.parseInt(configPort);
        } catch (Exception ignored) {}

        printBanner(port);

        // 1. Initialize Database Schema & Seed Data
        System.out.println("[1/3] Initializing MySQL Database Connection...");
        try {
            DBConnection.initializeDatabase();
            System.out.println("      MySQL Database ready.");
        } catch (Exception e) {
            System.err.println("      Database note: " + e.getMessage());
        }

        // 2. Start Built-in Java HTTP Server
        System.out.println("[2/3] Starting Java Web Server on port " + port + "...");
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

            // API Endpoints
            server.createContext("/api/auth", new AuthHandler());
            server.createContext("/api/tasks", new TaskHandler());
            server.createContext("/api/schedule", new ScheduleHandler());
            server.createContext("/api/quote", new com.scheduler.controller.QuoteHandler());
            server.createContext("/api/ai/advisor", new com.scheduler.controller.AIHandler());

            // Static Files Frontend (HTML/CSS/JS)
            File webDir = new File("web");
            if (!webDir.exists()) {
                // If running from src or parent directory
                webDir = new File("../web");
            }
            server.createContext("/", new StaticFileHandler(webDir.getAbsolutePath()));

            // Multi-threaded executor
            server.setExecutor(Executors.newFixedThreadPool(10));
            server.start();

            System.out.println("[3/3] Application Started Successfully!");
            System.out.println("=============================================================");
            System.out.println(" Access Application: http://localhost:" + port);
            System.out.println(" Login Page:         http://localhost:" + port + "/login.html");
            System.out.println(" Default Demo User:  Username: demo  |  Password: admin123");
            System.out.println(" Press Ctrl+C in terminal to stop server.");
            System.out.println("=============================================================");

            // 3. Auto-open browser
            openBrowser("http://localhost:" + port + "/login.html");

        } catch (IOException e) {
            System.err.println("[FATAL] Could not start server on port " + port + ": " + e.getMessage());
            System.err.println("Tip: If port " + port + " is in use, change server.port in db.properties.");
        }
    }

    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
                }
            }
        } catch (Exception ignored) {
            // User can manually open the link from terminal
        }
    }

    private static void printBanner(int port) {
        System.out.println("=============================================================");
        System.out.println("      INTELLIGENT DAILY SCHEDULE OPTIMIZER                  ");
        System.out.println("   Core Java + JDBC + MySQL + HTML5/CSS3 Dashboard          ");
        System.out.println("=============================================================");
    }
}

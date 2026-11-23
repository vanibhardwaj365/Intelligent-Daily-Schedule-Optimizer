package com.scheduler.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DBConnection {
    private static Properties properties = new Properties();
    private static boolean driverLoaded = false;
    private static boolean schemaInitialized = false;

    static {
        loadProperties();
        loadDriver();
    }

    private static void loadProperties() {
        File file = new File("db.properties");
        if (file.exists()) {
            try (InputStream input = new FileInputStream(file)) {
                properties.load(input);
                System.out.println("[DB] Loaded configuration from db.properties");
            } catch (Exception e) {
                System.err.println("[DB Warning] Failed to read db.properties, using defaults: " + e.getMessage());
            }
        } else {
            System.out.println("[DB] db.properties not found in working dir, using defaults.");
        }
    }

    private static void loadDriver() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            driverLoaded = true;
            System.out.println("[DB] MySQL JDBC Driver successfully loaded.");
        } catch (ClassNotFoundException e) {
            System.err.println("[DB ERROR] MySQL JDBC Driver not found in classpath!");
            System.err.println("[DB ERROR] Make sure mysql-connector-j.jar is in the lib/ folder.");
        }
    }

    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public static Connection getConnection() throws SQLException {
        if (!driverLoaded) {
            loadDriver();
        }

        String url = getProperty("db.url", "jdbc:mysql://localhost:3306/schedule_optimizer_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        String user = getProperty("db.user", "root");
        String pass = getProperty("db.password", "root");

        try {
            return DriverManager.getConnection(url, user, pass);
        } catch (SQLException e) {
            // If the database does not exist yet (error 1049: Unknown database), try to bootstrap it!
            if (e.getErrorCode() == 1049 || e.getMessage().contains("Unknown database")) {
                System.out.println("[DB] Database does not exist yet. Attempting auto-creation...");
                bootstrapDatabase();
                return DriverManager.getConnection(url, user, pass);
            }
            throw e;
        }
    }

    /**
     * Attempts to create the database and tables automatically on first run
     */
    public static synchronized void initializeDatabase() {
        if (schemaInitialized) return;
        try {
            bootstrapDatabase();
            schemaInitialized = true;
        } catch (Exception e) {
            System.err.println("[DB Warning] Database auto-bootstrap check: " + e.getMessage());
        }
    }

    private static void bootstrapDatabase() {
        String host = getProperty("db.host", "localhost");
        String port = getProperty("db.port", "3306");
        String user = getProperty("db.user", "root");
        String pass = getProperty("db.password", "root");
        String baseUrl = "jdbc:mysql://" + host + ":" + port + "/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        try (Connection rootConn = DriverManager.getConnection(baseUrl, user, pass);
             Statement stmt = rootConn.createStatement()) {

            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS schedule_optimizer_db");
            stmt.executeUpdate("USE schedule_optimizer_db");

            // Create users table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  username VARCHAR(50) NOT NULL UNIQUE," +
                "  password VARCHAR(255) NOT NULL," +
                "  email VARCHAR(100) NOT NULL UNIQUE," +
                "  full_name VARCHAR(100)," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            // Create tasks table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS tasks (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  user_id INT NOT NULL," +
                "  title VARCHAR(200) NOT NULL," +
                "  description TEXT," +
                "  priority ENUM('HIGH', 'MEDIUM', 'LOW') NOT NULL DEFAULT 'MEDIUM'," +
                "  category VARCHAR(50) NOT NULL DEFAULT 'GENERAL'," +
                "  deadline DATETIME NOT NULL," +
                "  estimated_minutes INT NOT NULL DEFAULT 30," +
                "  status ENUM('PENDING', 'COMPLETED') DEFAULT 'PENDING'," +
                "  scheduled_start DATETIME NULL," +
                "  scheduled_end DATETIME NULL," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ")"
            );

            // Ensure category column exists in case table was created earlier
            try {
                stmt.executeUpdate("ALTER TABLE tasks ADD COLUMN category VARCHAR(50) NOT NULL DEFAULT 'GENERAL'");
            } catch (SQLException ignored) {
                // Column already exists
            }

            // Seed demo user: username 'demo', password 'admin123' (SHA-256)
            stmt.executeUpdate(
                "INSERT IGNORE INTO users (id, username, password, email, full_name) VALUES " +
                "(1, 'demo', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'demo@example.com', 'Demo Student')"
            );

            // Seed sample tasks if table is empty
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM tasks");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.executeUpdate(
                    "INSERT INTO tasks (user_id, title, description, priority, deadline, estimated_minutes, status) VALUES " +
                    "(1, 'Finalize Java Project Documentation', 'Prepare project report, architecture diagrams, and DB schema documentation.', 'HIGH', DATE_ADD(NOW(), INTERVAL 4 HOUR), 90, 'PENDING')," +
                    "(1, 'Review Database Indexes and Queries', 'Ensure JDBC queries execute efficiently under concurrent user load.', 'MEDIUM', DATE_ADD(NOW(), INTERVAL 8 HOUR), 45, 'PENDING')," +
                    "(1, 'Test Schedule Optimization Algorithm', 'Validate edge cases with overdue tasks and multiple priority conflicts.', 'HIGH', DATE_ADD(NOW(), INTERVAL 2 HOUR), 60, 'PENDING')," +
                    "(1, 'Submit Project Code in Classroom', 'Package source zip and commit to GitHub repository.', 'LOW', DATE_ADD(NOW(), INTERVAL 12 HOUR), 30, 'PENDING')," +
                    "(1, 'Team Standup & Progress Sync', 'Discuss completion of morning task schedule with mentor.', 'MEDIUM', DATE_SUB(NOW(), INTERVAL 1 HOUR), 30, 'COMPLETED')"
                );
            }

            System.out.println("[DB] Database and tables verified / initialized successfully.");
        } catch (SQLException e) {
            System.err.println("[DB Initialization Notice] Could not auto-bootstrap MySQL tables: " + e.getMessage());
            System.err.println("[DB Tip] If you haven't started MySQL yet, please start MySQL service and run database.sql.");
        }
    }
}

-- ========================================================
-- Database Schema for Intelligent Daily Schedule Optimizer
-- Technology: MySQL 8.x / MariaDB
-- ========================================================

CREATE DATABASE IF NOT EXISTS schedule_optimizer_db;
USE schedule_optimizer_db;

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tasks Table
CREATE TABLE IF NOT EXISTS tasks (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    priority ENUM('HIGH', 'MEDIUM', 'LOW') NOT NULL DEFAULT 'MEDIUM',
    category VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    deadline DATETIME NOT NULL,
    estimated_minutes INT NOT NULL DEFAULT 30,
    status ENUM('PENDING', 'COMPLETED') DEFAULT 'PENDING',
    scheduled_start DATETIME NULL,
    scheduled_end DATETIME NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Sample Demo User (Password is 'admin123' in SHA-256)
INSERT IGNORE INTO users (id, username, password, email, full_name) 
VALUES (1, 'demo', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'demo@example.com', 'Demo Student');

-- Sample Initial Tasks with Categories
INSERT IGNORE INTO tasks (id, user_id, title, description, priority, category, deadline, estimated_minutes, status)
VALUES 
(1, 1, 'Finalize Java Project Documentation', 'Prepare project report, architecture diagrams, and DB schema documentation.', 'HIGH', 'STUDY', DATE_ADD(NOW(), INTERVAL 4 HOUR), 90, 'PENDING'),
(2, 1, 'Review Database Indexes and Queries', 'Ensure JDBC queries execute efficiently under concurrent user load.', 'MEDIUM', 'WORK', DATE_ADD(NOW(), INTERVAL 8 HOUR), 45, 'PENDING'),
(3, 1, 'Test Schedule Optimization Algorithm', 'Validate edge cases with overdue tasks and multiple priority conflicts.', 'HIGH', 'CODING', DATE_ADD(NOW(), INTERVAL 2 HOUR), 60, 'PENDING'),
(4, 1, 'Submit Project Code in Classroom', 'Package source zip and commit to GitHub repository.', 'LOW', 'STUDY', DATE_ADD(NOW(), INTERVAL 12 HOUR), 30, 'PENDING'),
(5, 1, 'Evening Gym Workout & Cardio', '45-minute fitness and endurance training.', 'MEDIUM', 'HEALTH', DATE_SUB(NOW(), INTERVAL 1 HOUR), 45, 'COMPLETED');

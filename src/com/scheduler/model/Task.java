package com.scheduler.model;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Task implements Comparable<Task> {
    private int id;
    private int userId;
    private String title;
    private String description;
    private String priority; // HIGH, MEDIUM, LOW
    private String category; // STUDY, WORK, PERSONAL, HEALTH, OTHER
    private LocalDateTime deadline;
    private int estimatedMinutes;
    private String status; // PENDING, COMPLETED
    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Computed fields for optimizer
    private double optimizationScore;
    private boolean deadlineRisk;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Task() {
        this.status = "PENDING";
        this.priority = "MEDIUM";
        this.category = "GENERAL";
        this.estimatedMinutes = 30;
    }

    public Task(int id, int userId, String title, String description, String priority,
                String category, LocalDateTime deadline, int estimatedMinutes, String status) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.category = category != null ? category : "GENERAL";
        this.deadline = deadline;
        this.estimatedMinutes = estimatedMinutes;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getCategory() { return category != null ? category : "GENERAL"; }
    public void setCategory(String category) { this.category = category; }

    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }

    public int getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(int estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getScheduledStart() { return scheduledStart; }
    public void setScheduledStart(LocalDateTime scheduledStart) { this.scheduledStart = scheduledStart; }

    public LocalDateTime getScheduledEnd() { return scheduledEnd; }
    public void setScheduledEnd(LocalDateTime scheduledEnd) { this.scheduledEnd = scheduledEnd; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public double getOptimizationScore() { return optimizationScore; }
    public void setOptimizationScore(double optimizationScore) { this.optimizationScore = optimizationScore; }

    public boolean isDeadlineRisk() { return deadlineRisk; }
    public void setDeadlineRisk(boolean deadlineRisk) { this.deadlineRisk = deadlineRisk; }

    @Override
    public int compareTo(Task o) {
        return Double.compare(o.optimizationScore, this.optimizationScore);
    }

    public String toJson() {
        return String.format(
            "{" +
            "\"id\":%d," +
            "\"userId\":%d," +
            "\"title\":\"%s\"," +
            "\"description\":\"%s\"," +
            "\"priority\":\"%s\"," +
            "\"category\":\"%s\"," +
            "\"deadline\":\"%s\"," +
            "\"estimatedMinutes\":%d," +
            "\"status\":\"%s\"," +
            "\"scheduledStart\":%s," +
            "\"scheduledEnd\":%s," +
            "\"optimizationScore\":%.2f," +
            "\"deadlineRisk\":%b" +
            "}",
            id,
            userId,
            escapeJson(title),
            escapeJson(description != null ? description : ""),
            priority,
            category != null ? category : "GENERAL",
            deadline != null ? deadline.format(FORMATTER) : "",
            estimatedMinutes,
            status,
            scheduledStart != null ? "\"" + scheduledStart.format(FORMATTER) + "\"" : "null",
            scheduledEnd != null ? "\"" + scheduledEnd.format(FORMATTER) + "\"" : "null",
            optimizationScore,
            deadlineRisk
        );
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}

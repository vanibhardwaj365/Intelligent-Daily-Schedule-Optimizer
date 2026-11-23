package com.scheduler.dao;

import com.scheduler.config.DBConnection;
import com.scheduler.model.ScheduleSummary;
import com.scheduler.model.Task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TaskDao {

    public boolean createTask(Task task) throws SQLException {
        String sql = "INSERT INTO tasks (user_id, title, description, priority, category, deadline, estimated_minutes, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, task.getUserId());
            stmt.setString(2, task.getTitle().trim());
            stmt.setString(3, task.getDescription() != null ? task.getDescription().trim() : "");
            stmt.setString(4, task.getPriority() != null ? task.getPriority() : "MEDIUM");
            stmt.setString(5, task.getCategory() != null ? task.getCategory().toUpperCase() : "GENERAL");
            stmt.setTimestamp(6, Timestamp.valueOf(task.getDeadline()));
            stmt.setInt(7, task.getEstimatedMinutes() > 0 ? task.getEstimatedMinutes() : 30);
            stmt.setString(8, task.getStatus() != null ? task.getStatus() : "PENDING");

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        task.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean updateTask(Task task) throws SQLException {
        String sql = "UPDATE tasks SET title = ?, description = ?, priority = ?, category = ?, deadline = ?, estimated_minutes = ?, status = ? WHERE id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, task.getTitle().trim());
            stmt.setString(2, task.getDescription() != null ? task.getDescription().trim() : "");
            stmt.setString(3, task.getPriority() != null ? task.getPriority() : "MEDIUM");
            stmt.setString(4, task.getCategory() != null ? task.getCategory().toUpperCase() : "GENERAL");
            stmt.setTimestamp(5, Timestamp.valueOf(task.getDeadline()));
            stmt.setInt(6, task.getEstimatedMinutes());
            stmt.setString(7, task.getStatus());
            stmt.setInt(8, task.getId());
            stmt.setInt(9, task.getUserId());

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean deleteTask(int taskId, int userId) throws SQLException {
        String sql = "DELETE FROM tasks WHERE id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, taskId);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    public Task getTaskById(int taskId, int userId) throws SQLException {
        String sql = "SELECT * FROM tasks WHERE id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, taskId);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToTask(rs);
                }
            }
        }
        return null;
    }

    public List<Task> getAllTasksByUser(int userId) throws SQLException {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks WHERE user_id = ? ORDER BY CASE status WHEN 'PENDING' THEN 1 ELSE 2 END, deadline ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tasks.add(mapRowToTask(rs));
                }
            }
        }
        return tasks;
    }

    public List<Task> getPendingTasksByUser(int userId) throws SQLException {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks WHERE user_id = ? AND status = 'PENDING' ORDER BY deadline ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tasks.add(mapRowToTask(rs));
                }
            }
        }
        return tasks;
    }

    public boolean toggleTaskStatus(int taskId, int userId) throws SQLException {
        String sql = "UPDATE tasks SET status = CASE WHEN status = 'COMPLETED' THEN 'PENDING' ELSE 'COMPLETED' END WHERE id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, taskId);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    public void saveOptimizedSchedule(List<Task> tasks) throws SQLException {
        String sql = "UPDATE tasks SET scheduled_start = ?, scheduled_end = ? WHERE id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            for (Task t : tasks) {
                if (t.getScheduledStart() != null) {
                    stmt.setTimestamp(1, Timestamp.valueOf(t.getScheduledStart()));
                } else {
                    stmt.setNull(1, java.sql.Types.TIMESTAMP);
                }
                if (t.getScheduledEnd() != null) {
                    stmt.setTimestamp(2, Timestamp.valueOf(t.getScheduledEnd()));
                } else {
                    stmt.setNull(2, java.sql.Types.TIMESTAMP);
                }
                stmt.setInt(3, t.getId());
                stmt.setInt(4, t.getUserId());
                stmt.addBatch();
            }
            stmt.executeBatch();
            conn.commit();
        }
    }

    public ScheduleSummary getScheduleSummary(int userId) throws SQLException {
        ScheduleSummary summary = new ScheduleSummary();
        String sql = "SELECT " +
                     "  COUNT(*) AS total, " +
                     "  SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed, " +
                     "  SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) AS pending, " +
                     "  COALESCE(SUM(estimated_minutes), 0) AS total_minutes, " +
                     "  COALESCE(SUM(CASE WHEN status = 'PENDING' THEN estimated_minutes ELSE 0 END), 0) AS pending_minutes, " +
                     "  SUM(CASE WHEN priority = 'HIGH' THEN 1 ELSE 0 END) AS high_p, " +
                     "  SUM(CASE WHEN priority = 'MEDIUM' THEN 1 ELSE 0 END) AS med_p, " +
                     "  SUM(CASE WHEN priority = 'LOW' THEN 1 ELSE 0 END) AS low_p, " +
                     "  SUM(CASE WHEN status = 'PENDING' AND deadline < NOW() THEN 1 ELSE 0 END) AS overdue " +
                     "FROM tasks WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int total = rs.getInt("total");
                    int completed = rs.getInt("completed");
                    int pending = rs.getInt("pending");
                    int totalMins = rs.getInt("total_minutes");
                    int pendingMins = rs.getInt("pending_minutes");
                    int highP = rs.getInt("high_p");
                    int medP = rs.getInt("med_p");
                    int lowP = rs.getInt("low_p");
                    int overdue = rs.getInt("overdue");

                    double rate = total > 0 ? ((double) completed / total) * 100.0 : 0.0;

                    summary.setTotalTasks(total);
                    summary.setCompletedTasks(completed);
                    summary.setPendingTasks(pending);
                    summary.setCompletionRate(rate);
                    summary.setTotalEstimatedMinutes(totalMins);
                    summary.setPendingEstimatedMinutes(pendingMins);
                    summary.setHighPriorityCount(highP);
                    summary.setMediumPriorityCount(medP);
                    summary.setLowPriorityCount(lowP);
                    summary.setOverdueCount(overdue);
                }
            }
        }
        return summary;
    }

    private Task mapRowToTask(ResultSet rs) throws SQLException {
        Task t = new Task();
        t.setId(rs.getInt("id"));
        t.setUserId(rs.getInt("user_id"));
        t.setTitle(rs.getString("title"));
        t.setDescription(rs.getString("description"));
        t.setPriority(rs.getString("priority"));

        try {
            String cat = rs.getString("category");
            t.setCategory(cat != null ? cat : "GENERAL");
        } catch (SQLException ignored) {
            t.setCategory("GENERAL");
        }

        Timestamp dl = rs.getTimestamp("deadline");
        if (dl != null) t.setDeadline(dl.toLocalDateTime());

        t.setEstimatedMinutes(rs.getInt("estimated_minutes"));
        t.setStatus(rs.getString("status"));

        Timestamp ss = rs.getTimestamp("scheduled_start");
        if (ss != null) t.setScheduledStart(ss.toLocalDateTime());

        Timestamp se = rs.getTimestamp("scheduled_end");
        if (se != null) t.setScheduledEnd(se.toLocalDateTime());

        t.setCreatedAt(rs.getTimestamp("created_at"));
        t.setUpdatedAt(rs.getTimestamp("updated_at"));

        return t;
    }
}

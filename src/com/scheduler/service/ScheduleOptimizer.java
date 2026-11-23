package com.scheduler.service;

import com.scheduler.dao.TaskDao;
import com.scheduler.model.Task;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Intelligent Daily Schedule Optimizer
 * Implements a hybrid Earliest Deadline First (EDF) + Priority Weighted Scoring (PWS) algorithm
 * with automatic buffer intervals and deadline risk detection.
 */
public class ScheduleOptimizer {
    private final TaskDao taskDao;

    public ScheduleOptimizer() {
        this.taskDao = new TaskDao();
    }

    public ScheduleOptimizer(TaskDao taskDao) {
        this.taskDao = taskDao;
    }

    /**
     * Optimizes schedule for all pending tasks of a user
     * @param userId The ID of the user
     * @return List of tasks ordered and allocated into optimal daily time slots
     */
    public List<Task> optimizeSchedule(int userId) throws SQLException {
        List<Task> pendingTasks = taskDao.getPendingTasksByUser(userId);
        if (pendingTasks.isEmpty()) {
            return pendingTasks;
        }

        LocalDateTime now = LocalDateTime.now();

        // 1. Calculate Multi-Factor Optimization Score for each task
        for (Task task : pendingTasks) {
            double score = calculateTaskScore(task, now);
            task.setOptimizationScore(score);
        }

        // 2. Sort tasks by Optimization Score (Highest score first)
        Collections.sort(pendingTasks);

        // 3. Allocate optimal sequential time slots throughout the day
        LocalDateTime slotCursor = getInitialStartTime(now);
        final int BUFFER_MINUTES = 10; // 10-minute rest/transition break between tasks

        for (Task task : pendingTasks) {
            LocalDateTime taskStart = slotCursor;
            LocalDateTime taskEnd = taskStart.plusMinutes(task.getEstimatedMinutes());

            task.setScheduledStart(taskStart);
            task.setScheduledEnd(taskEnd);

            // Deadline Risk Flag: Check if the task will finish after its stated deadline
            if (task.getDeadline() != null && taskEnd.isAfter(task.getDeadline())) {
                task.setDeadlineRisk(true);
            } else {
                task.setDeadlineRisk(false);
            }

            // Move cursor forward for the next task (task duration + break)
            slotCursor = taskEnd.plusMinutes(BUFFER_MINUTES);
        }

        // 4. Persist the generated schedule into MySQL
        taskDao.saveOptimizedSchedule(pendingTasks);

        return pendingTasks;
    }

    /**
     * Multi-Factor Scoring Formula:
     * Score = Priority_Weight + Urgency_Weight - (Duration * Efficiency_Factor)
     */
    private double calculateTaskScore(Task task, LocalDateTime now) {
        // A. Priority Weight
        double priorityWeight;
        String p = task.getPriority() != null ? task.getPriority().toUpperCase() : "MEDIUM";
        switch (p) {
            case "HIGH":
                priorityWeight = 300.0;
                break;
            case "LOW":
                priorityWeight = 50.0;
                break;
            case "MEDIUM":
            default:
                priorityWeight = 150.0;
                break;
        }

        // B. Deadline Urgency Factor
        double urgencyWeight = 0.0;
        if (task.getDeadline() != null) {
            long minutesUntilDeadline = Duration.between(now, task.getDeadline()).toMinutes();

            if (minutesUntilDeadline < 0) {
                // OVERDUE: Urgent escalation needed
                urgencyWeight = 800.0 + Math.abs(minutesUntilDeadline) * 0.2;
            } else if (minutesUntilDeadline <= 180) {
                // Due within 3 hours: Critical urgency gradient (500 -> 250)
                urgencyWeight = 250.0 + (250.0 * (1.0 - ((double) minutesUntilDeadline / 180.0)));
            } else if (minutesUntilDeadline <= 1440) {
                // Due within 24 hours: Moderate urgency gradient (250 -> 50)
                urgencyWeight = 50.0 + (200.0 * (1.0 - ((double) minutesUntilDeadline / 1440.0)));
            } else {
                // Due later than 24 hours
                urgencyWeight = Math.max(10.0, 50.0 - (minutesUntilDeadline / 144.0));
            }
        }

        // C. Duration factor (Slight preference to complete shorter tasks faster to reduce WIP)
        double durationPenalty = task.getEstimatedMinutes() * 0.15;

        return priorityWeight + urgencyWeight - durationPenalty;
    }

    /**
     * Determines initial schedule start time (rounded up to nearest 5 minutes)
     */
    private LocalDateTime getInitialStartTime(LocalDateTime now) {
        int minute = now.getMinute();
        int roundedMinute = ((minute / 5) + 1) * 5;
        if (roundedMinute >= 60) {
            return now.plusHours(1).withMinute(0).withSecond(0).withNano(0);
        } else {
            return now.withMinute(roundedMinute).withSecond(0).withNano(0);
        }
    }
}

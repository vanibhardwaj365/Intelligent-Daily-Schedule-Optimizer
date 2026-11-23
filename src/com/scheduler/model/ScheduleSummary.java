package com.scheduler.model;

public class ScheduleSummary {
    private int totalTasks;
    private int completedTasks;
    private int pendingTasks;
    private double completionRate;
    private int totalEstimatedMinutes;
    private int pendingEstimatedMinutes;
    private int highPriorityCount;
    private int mediumPriorityCount;
    private int lowPriorityCount;
    private int overdueCount;

    public ScheduleSummary() {}

    public int getTotalTasks() { return totalTasks; }
    public void setTotalTasks(int totalTasks) { this.totalTasks = totalTasks; }

    public int getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(int completedTasks) { this.completedTasks = completedTasks; }

    public int getPendingTasks() { return pendingTasks; }
    public void setPendingTasks(int pendingTasks) { this.pendingTasks = pendingTasks; }

    public double getCompletionRate() { return completionRate; }
    public void setCompletionRate(double completionRate) { this.completionRate = completionRate; }

    public int getTotalEstimatedMinutes() { return totalEstimatedMinutes; }
    public void setTotalEstimatedMinutes(int totalEstimatedMinutes) { this.totalEstimatedMinutes = totalEstimatedMinutes; }

    public int getPendingEstimatedMinutes() { return pendingEstimatedMinutes; }
    public void setPendingEstimatedMinutes(int pendingEstimatedMinutes) { this.pendingEstimatedMinutes = pendingEstimatedMinutes; }

    public int getHighPriorityCount() { return highPriorityCount; }
    public void setHighPriorityCount(int highPriorityCount) { this.highPriorityCount = highPriorityCount; }

    public int getMediumPriorityCount() { return mediumPriorityCount; }
    public void setMediumPriorityCount(int mediumPriorityCount) { this.mediumPriorityCount = mediumPriorityCount; }

    public int getLowPriorityCount() { return lowPriorityCount; }
    public void setLowPriorityCount(int lowPriorityCount) { this.lowPriorityCount = lowPriorityCount; }

    public int getOverdueCount() { return overdueCount; }
    public void setOverdueCount(int overdueCount) { this.overdueCount = overdueCount; }

    public String toJson() {
        return String.format(
            "{" +
            "\"totalTasks\":%d," +
            "\"completedTasks\":%d," +
            "\"pendingTasks\":%d," +
            "\"completionRate\":%.1f," +
            "\"totalEstimatedMinutes\":%d," +
            "\"pendingEstimatedMinutes\":%d," +
            "\"highPriorityCount\":%d," +
            "\"mediumPriorityCount\":%d," +
            "\"lowPriorityCount\":%d," +
            "\"overdueCount\":%d" +
            "}",
            totalTasks,
            completedTasks,
            pendingTasks,
            completionRate,
            totalEstimatedMinutes,
            pendingEstimatedMinutes,
            highPriorityCount,
            mediumPriorityCount,
            lowPriorityCount,
            overdueCount
        );
    }
}

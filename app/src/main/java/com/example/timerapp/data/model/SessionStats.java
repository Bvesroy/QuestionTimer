package com.example.timerapp.data.model;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Model representing past and current session statistics,
 * aggregating question count, target, total duration, average pace,
 * and reflective metadata.
 */
public class SessionStats implements Serializable {
    private static final long serialVersionUID = 2L;

    private long sessionId;
    private String sessionName;
    private int targetCount;
    private int solvedCount;
    private long totalDurationMs;
    private long averageDurationMs;
    private long createdAt;
    private long updatedAt;
    private boolean isActive;

    private String subject;
    private String notes;
    private String wentWell;
    private String needsImprovement;
    private String status;
    private String profileId;

    public SessionStats() {
        this.subject = "General";
        this.notes = "";
        this.wentWell = "";
        this.needsImprovement = "";
        this.status = "COMPLETED";
        this.profileId = "default";
    }

    public SessionStats(long sessionId, String sessionName, int targetCount, int solvedCount,
                        long totalDurationMs, long averageDurationMs, long createdAt,
                        long updatedAt, boolean isActive) {
        this(sessionId, sessionName, targetCount, solvedCount, totalDurationMs, averageDurationMs,
                createdAt, updatedAt, isActive, "General", "", "", "", "COMPLETED", "default");
    }

    public SessionStats(long sessionId, String sessionName, int targetCount, int solvedCount,
                        long totalDurationMs, long averageDurationMs, long createdAt,
                        long updatedAt, boolean isActive, String subject, String notes,
                        String wentWell, String needsImprovement, String status, String profileId) {
        this.sessionId = sessionId;
        this.sessionName = sessionName;
        this.targetCount = targetCount;
        this.solvedCount = solvedCount;
        this.totalDurationMs = totalDurationMs;
        this.averageDurationMs = averageDurationMs;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isActive = isActive;
        this.subject = (subject != null && !subject.trim().isEmpty()) ? subject.trim() : "General";
        this.notes = (notes != null) ? notes : "";
        this.wentWell = (wentWell != null) ? wentWell : "";
        this.needsImprovement = (needsImprovement != null) ? needsImprovement : "";
        this.status = (status != null && !status.trim().isEmpty()) ? status.trim() : "COMPLETED";
        this.profileId = (profileId != null && !profileId.trim().isEmpty()) ? profileId.trim() : "default";
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public int getTargetCount() {
        return targetCount;
    }

    public void setTargetCount(int targetCount) {
        this.targetCount = targetCount;
    }

    public int getSolvedCount() {
        return solvedCount;
    }

    public void setSolvedCount(int solvedCount) {
        this.solvedCount = solvedCount;
    }

    public long getTotalDurationMs() {
        return totalDurationMs;
    }

    public void setTotalDurationMs(long totalDurationMs) {
        this.totalDurationMs = totalDurationMs;
    }

    public long getAverageDurationMs() {
        return averageDurationMs;
    }

    public void setAverageDurationMs(long averageDurationMs) {
        this.averageDurationMs = averageDurationMs;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getSubject() {
        return subject != null ? subject : "General";
    }

    public void setSubject(String subject) {
        this.subject = (subject != null && !subject.trim().isEmpty()) ? subject.trim() : "General";
    }

    public String getNotes() {
        return notes != null ? notes : "";
    }

    public void setNotes(String notes) {
        this.notes = (notes != null) ? notes : "";
    }

    public String getWentWell() {
        return wentWell != null ? wentWell : "";
    }

    public void setWentWell(String wentWell) {
        this.wentWell = (wentWell != null) ? wentWell : "";
    }

    public String getNeedsImprovement() {
        return needsImprovement != null ? needsImprovement : "";
    }

    public void setNeedsImprovement(String needsImprovement) {
        this.needsImprovement = (needsImprovement != null) ? needsImprovement : "";
    }

    public String getStatus() {
        return status != null ? status : "COMPLETED";
    }

    public void setStatus(String status) {
        this.status = (status != null && !status.trim().isEmpty()) ? status.trim() : "COMPLETED";
    }

    public String getProfileId() {
        return profileId != null ? profileId : "default";
    }

    public void setProfileId(String profileId) {
        this.profileId = (profileId != null && !profileId.trim().isEmpty()) ? profileId.trim() : "default";
    }

    /**
     * Calculates the percentage towards the target count.
     * Returns 0.0 if targetCount <= 0. Caps at 100.0 or displays accurate ratio.
     */
    public double getProgressPercentage() {
        if (targetCount <= 0) {
            return 0.0;
        }
        return Math.min(100.0, ((double) solvedCount / targetCount) * 100.0);
    }

    /**
     * Formats duration in HH:mm:ss or mm:ss for clean UI presentation.
     */
    public static String formatDuration(long millis) {
        if (millis <= 0) {
            return "00:00";
        }
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        if (hours > 0) {
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.US, "%02d:%02d", minutes, seconds);
        }
    }

    public String getFormattedTotalDuration() {
        return formatDuration(totalDurationMs);
    }

    public String getFormattedAverageDuration() {
        return formatDuration(averageDurationMs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SessionStats that = (SessionStats) o;
        return sessionId == that.sessionId &&
                targetCount == that.targetCount &&
                solvedCount == that.solvedCount &&
                totalDurationMs == that.totalDurationMs &&
                averageDurationMs == that.averageDurationMs &&
                createdAt == that.createdAt &&
                updatedAt == that.updatedAt &&
                isActive == that.isActive &&
                Objects.equals(sessionName, that.sessionName) &&
                Objects.equals(subject, that.subject) &&
                Objects.equals(status, that.status) &&
                Objects.equals(profileId, that.profileId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, sessionName, targetCount, solvedCount,
                totalDurationMs, averageDurationMs, createdAt, updatedAt, isActive,
                subject, status, profileId);
    }

    @Override
    public String toString() {
        return "SessionStats{" +
                "sessionId=" + sessionId +
                ", sessionName='" + sessionName + '\'' +
                ", subject='" + subject + '\'' +
                ", targetCount=" + targetCount +
                ", solvedCount=" + solvedCount +
                ", totalDurationMs=" + totalDurationMs +
                ", averageDurationMs=" + averageDurationMs +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", isActive=" + isActive +
                ", status='" + status + '\'' +
                '}';
    }
}

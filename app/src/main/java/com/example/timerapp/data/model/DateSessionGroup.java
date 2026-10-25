package com.example.timerapp.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Encapsulates a collection of study sessions grouped by a calendar date.
 * Contains aggregated totals for the day along with individual session statistics.
 */
public class DateSessionGroup implements Serializable {
    private static final long serialVersionUID = 1L;

    private String dateString;       // e.g. "2026-10-25"
    private String formattedDate;   // e.g. "October 25, 2026"
    private long rawDateMillis;     // Midnight epoch millis (00:00:00.000 local time)
    private int totalSessionsCount;
    private long totalPracticeDurationMs;
    private int totalQuestionsSolved;
    private List<SessionStats> sessions;

    public DateSessionGroup() {
        this.sessions = new ArrayList<>();
    }

    public DateSessionGroup(String dateString, String formattedDate, long rawDateMillis,
                            int totalSessionsCount, long totalPracticeDurationMs,
                            int totalQuestionsSolved, List<SessionStats> sessions) {
        this.dateString = dateString;
        this.formattedDate = formattedDate;
        this.rawDateMillis = rawDateMillis;
        this.totalSessionsCount = totalSessionsCount;
        this.totalPracticeDurationMs = totalPracticeDurationMs;
        this.totalQuestionsSolved = totalQuestionsSolved;
        this.sessions = (sessions != null) ? sessions : new ArrayList<>();
    }

    public String getDateString() {
        return dateString;
    }

    public void setDateString(String dateString) {
        this.dateString = dateString;
    }

    public String getFormattedDate() {
        return formattedDate;
    }

    public void setFormattedDate(String formattedDate) {
        this.formattedDate = formattedDate;
    }

    public long getRawDateMillis() {
        return rawDateMillis;
    }

    public void setRawDateMillis(long rawDateMillis) {
        this.rawDateMillis = rawDateMillis;
    }

    public int getTotalSessionsCount() {
        return totalSessionsCount;
    }

    public void setTotalSessionsCount(int totalSessionsCount) {
        this.totalSessionsCount = totalSessionsCount;
    }

    public long getTotalPracticeDurationMs() {
        return totalPracticeDurationMs;
    }

    public void setTotalPracticeDurationMs(long totalPracticeDurationMs) {
        this.totalPracticeDurationMs = totalPracticeDurationMs;
    }

    public int getTotalQuestionsSolved() {
        return totalQuestionsSolved;
    }

    public void setTotalQuestionsSolved(int totalQuestionsSolved) {
        this.totalQuestionsSolved = totalQuestionsSolved;
    }

    public List<SessionStats> getSessions() {
        return sessions != null ? sessions : Collections.emptyList();
    }

    public void setSessions(List<SessionStats> sessions) {
        this.sessions = (sessions != null) ? sessions : new ArrayList<>();
    }

    /**
     * Calculates the overall average time per question for the day across all sessions.
     * Returns 0 if no questions were solved.
     */
    public long getAverageTimePerQuestionMs() {
        if (totalQuestionsSolved <= 0) {
            return 0L;
        }
        return totalPracticeDurationMs / totalQuestionsSolved;
    }

    /**
     * Formats the day's total practice duration as HH:mm:ss or mm:ss.
     */
    public String getFormattedTotalDuration() {
        return SessionStats.formatDuration(totalPracticeDurationMs);
    }

    /**
     * Formats average time per question for this day as mm:ss.
     */
    public String getFormattedAverageTimePerQuestion() {
        return SessionStats.formatDuration(getAverageTimePerQuestionMs());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DateSessionGroup that = (DateSessionGroup) o;
        return rawDateMillis == that.rawDateMillis &&
                totalSessionsCount == that.totalSessionsCount &&
                totalPracticeDurationMs == that.totalPracticeDurationMs &&
                totalQuestionsSolved == that.totalQuestionsSolved &&
                Objects.equals(dateString, that.dateString) &&
                Objects.equals(formattedDate, that.formattedDate) &&
                Objects.equals(sessions, that.sessions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dateString, formattedDate, rawDateMillis, totalSessionsCount,
                totalPracticeDurationMs, totalQuestionsSolved, sessions);
    }

    @Override
    public String toString() {
        return "DateSessionGroup{" +
                "dateString='" + dateString + '\'' +
                ", formattedDate='" + formattedDate + '\'' +
                ", rawDateMillis=" + rawDateMillis +
                ", totalSessionsCount=" + totalSessionsCount +
                ", totalPracticeDurationMs=" + totalPracticeDurationMs +
                ", totalQuestionsSolved=" + totalQuestionsSolved +
                ", sessionsCount=" + (sessions != null ? sessions.size() : 0) +
                '}';
    }
}

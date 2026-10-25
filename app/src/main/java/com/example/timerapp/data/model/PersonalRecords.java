package com.example.timerapp.data.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Encapsulates historic personal best records for the user.
 */
public class PersonalRecords implements Serializable {
    private static final long serialVersionUID = 1L;

    private SessionStats longestSession;
    private long mostPracticeDayMillis;
    private String mostPracticeDayLabel;
    private long mostPracticeDayDurationMs;

    private long mostSessionsDayMillis;
    private String mostSessionsDayLabel;
    private int mostSessionsDayCount;

    private SessionStats fastestPaceSession;
    private int longestStreakDays;

    public PersonalRecords() {
        this.mostPracticeDayLabel = "N/A";
        this.mostSessionsDayLabel = "N/A";
    }

    public PersonalRecords(SessionStats longestSession,
                           long mostPracticeDayMillis, String mostPracticeDayLabel, long mostPracticeDayDurationMs,
                           long mostSessionsDayMillis, String mostSessionsDayLabel, int mostSessionsDayCount,
                           SessionStats fastestPaceSession, int longestStreakDays) {
        this.longestSession = longestSession;
        this.mostPracticeDayMillis = mostPracticeDayMillis;
        this.mostPracticeDayLabel = mostPracticeDayLabel != null ? mostPracticeDayLabel : "N/A";
        this.mostPracticeDayDurationMs = mostPracticeDayDurationMs;
        this.mostSessionsDayMillis = mostSessionsDayMillis;
        this.mostSessionsDayLabel = mostSessionsDayLabel != null ? mostSessionsDayLabel : "N/A";
        this.mostSessionsDayCount = mostSessionsDayCount;
        this.fastestPaceSession = fastestPaceSession;
        this.longestStreakDays = longestStreakDays;
    }

    public SessionStats getLongestSession() {
        return longestSession;
    }

    public void setLongestSession(SessionStats longestSession) {
        this.longestSession = longestSession;
    }

    public long getMostPracticeDayMillis() {
        return mostPracticeDayMillis;
    }

    public void setMostPracticeDayMillis(long mostPracticeDayMillis) {
        this.mostPracticeDayMillis = mostPracticeDayMillis;
    }

    public String getMostPracticeDayLabel() {
        return mostPracticeDayLabel;
    }

    public void setMostPracticeDayLabel(String mostPracticeDayLabel) {
        this.mostPracticeDayLabel = mostPracticeDayLabel;
    }

    public long getMostPracticeDayDurationMs() {
        return mostPracticeDayDurationMs;
    }

    public void setMostPracticeDayDurationMs(long mostPracticeDayDurationMs) {
        this.mostPracticeDayDurationMs = mostPracticeDayDurationMs;
    }

    public long getMostSessionsDayMillis() {
        return mostSessionsDayMillis;
    }

    public void setMostSessionsDayMillis(long mostSessionsDayMillis) {
        this.mostSessionsDayMillis = mostSessionsDayMillis;
    }

    public String getMostSessionsDayLabel() {
        return mostSessionsDayLabel;
    }

    public void setMostSessionsDayLabel(String mostSessionsDayLabel) {
        this.mostSessionsDayLabel = mostSessionsDayLabel;
    }

    public int getMostSessionsDayCount() {
        return mostSessionsDayCount;
    }

    public void setMostSessionsDayCount(int mostSessionsDayCount) {
        this.mostSessionsDayCount = mostSessionsDayCount;
    }

    public SessionStats getFastestPaceSession() {
        return fastestPaceSession;
    }

    public void setFastestPaceSession(SessionStats fastestPaceSession) {
        this.fastestPaceSession = fastestPaceSession;
    }

    public int getLongestStreakDays() {
        return longestStreakDays;
    }

    public void setLongestStreakDays(int longestStreakDays) {
        this.longestStreakDays = longestStreakDays;
    }

    public String getFormattedMostPracticeDayDuration() {
        return SessionStats.formatDuration(mostPracticeDayDurationMs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PersonalRecords that = (PersonalRecords) o;
        return mostPracticeDayMillis == that.mostPracticeDayMillis &&
                mostPracticeDayDurationMs == that.mostPracticeDayDurationMs &&
                mostSessionsDayMillis == that.mostSessionsDayMillis &&
                mostSessionsDayCount == that.mostSessionsDayCount &&
                longestStreakDays == that.longestStreakDays &&
                Objects.equals(longestSession, that.longestSession) &&
                Objects.equals(fastestPaceSession, that.fastestPaceSession);
    }

    @Override
    public int hashCode() {
        return Objects.hash(longestSession, mostPracticeDayMillis, mostPracticeDayDurationMs,
                mostSessionsDayMillis, mostSessionsDayCount, fastestPaceSession, longestStreakDays);
    }

    @Override
    public String toString() {
        return "PersonalRecords{" +
                "longestStreakDays=" + longestStreakDays +
                ", mostPracticeDayLabel='" + mostPracticeDayLabel + '\'' +
                ", mostSessionsDayCount=" + mostSessionsDayCount +
                '}';
    }
}

package com.example.timerapp.data.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Data point for daily practice time charts.
 */
public class DailyPracticeDataPoint implements Serializable {
    private static final long serialVersionUID = 1L;

    private long dateMillis;
    private String dateLabel;
    private double practiceMinutes;
    private long practiceDurationMs;
    private int sessionCount;
    private int totalQuestionsSolved;

    public DailyPracticeDataPoint() {
    }

    public DailyPracticeDataPoint(long dateMillis, String dateLabel, double practiceMinutes,
                                 long practiceDurationMs, int sessionCount, int totalQuestionsSolved) {
        this.dateMillis = dateMillis;
        this.dateLabel = dateLabel;
        this.practiceMinutes = practiceMinutes;
        this.practiceDurationMs = practiceDurationMs;
        this.sessionCount = sessionCount;
        this.totalQuestionsSolved = totalQuestionsSolved;
    }

    public long getDateMillis() {
        return dateMillis;
    }

    public void setDateMillis(long dateMillis) {
        this.dateMillis = dateMillis;
    }

    public String getDateLabel() {
        return dateLabel;
    }

    public void setDateLabel(String dateLabel) {
        this.dateLabel = dateLabel;
    }

    public double getPracticeMinutes() {
        return practiceMinutes;
    }

    public void setPracticeMinutes(double practiceMinutes) {
        this.practiceMinutes = practiceMinutes;
    }

    public long getPracticeDurationMs() {
        return practiceDurationMs;
    }

    public void setPracticeDurationMs(long practiceDurationMs) {
        this.practiceDurationMs = practiceDurationMs;
    }

    public int getSessionCount() {
        return sessionCount;
    }

    public void setSessionCount(int sessionCount) {
        this.sessionCount = sessionCount;
    }

    public int getTotalQuestionsSolved() {
        return totalQuestionsSolved;
    }

    public void setTotalQuestionsSolved(int totalQuestionsSolved) {
        this.totalQuestionsSolved = totalQuestionsSolved;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DailyPracticeDataPoint that = (DailyPracticeDataPoint) o;
        return dateMillis == that.dateMillis &&
                Double.compare(that.practiceMinutes, practiceMinutes) == 0 &&
                practiceDurationMs == that.practiceDurationMs &&
                sessionCount == that.sessionCount &&
                totalQuestionsSolved == that.totalQuestionsSolved &&
                Objects.equals(dateLabel, that.dateLabel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dateMillis, dateLabel, practiceMinutes, practiceDurationMs,
                sessionCount, totalQuestionsSolved);
    }

    @Override
    public String toString() {
        return "DailyPracticeDataPoint{" +
                "dateLabel='" + dateLabel + '\'' +
                ", practiceMinutes=" + practiceMinutes +
                ", sessionCount=" + sessionCount +
                ", totalQuestionsSolved=" + totalQuestionsSolved +
                '}';
    }
}

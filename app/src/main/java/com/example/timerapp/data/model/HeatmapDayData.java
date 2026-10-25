package com.example.timerapp.data.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Data item representing a single day in a monthly productivity heatmap.
 */
public class HeatmapDayData implements Serializable {
    private static final long serialVersionUID = 1L;

    private int dayOfMonth;
    private long dateMillis;
    private double practiceMinutes;
    private long practiceDurationMs;
    private int sessionCount;
    private int totalQuestionsSolved;
    private int intensityLevel; // 0 to 4

    public HeatmapDayData() {
    }

    public HeatmapDayData(int dayOfMonth, long dateMillis, double practiceMinutes,
                          long practiceDurationMs, int sessionCount, int totalQuestionsSolved) {
        this.dayOfMonth = dayOfMonth;
        this.dateMillis = dateMillis;
        this.practiceMinutes = practiceMinutes;
        this.practiceDurationMs = practiceDurationMs;
        this.sessionCount = sessionCount;
        this.totalQuestionsSolved = totalQuestionsSolved;
        this.intensityLevel = calculateIntensity(practiceMinutes);
    }

    public static int calculateIntensity(double minutes) {
        if (minutes <= 0.0) return 0;
        if (minutes < 30.0) return 1;
        if (minutes < 60.0) return 2;
        if (minutes < 120.0) return 3;
        return 4;
    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(int dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    public long getDateMillis() {
        return dateMillis;
    }

    public void setDateMillis(long dateMillis) {
        this.dateMillis = dateMillis;
    }

    public double getPracticeMinutes() {
        return practiceMinutes;
    }

    public void setPracticeMinutes(double practiceMinutes) {
        this.practiceMinutes = practiceMinutes;
        this.intensityLevel = calculateIntensity(practiceMinutes);
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

    public int getIntensityLevel() {
        return intensityLevel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HeatmapDayData that = (HeatmapDayData) o;
        return dayOfMonth == that.dayOfMonth &&
                dateMillis == that.dateMillis &&
                Double.compare(that.practiceMinutes, practiceMinutes) == 0 &&
                practiceDurationMs == that.practiceDurationMs &&
                sessionCount == that.sessionCount &&
                totalQuestionsSolved == that.totalQuestionsSolved &&
                intensityLevel == that.intensityLevel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dayOfMonth, dateMillis, practiceMinutes, practiceDurationMs,
                sessionCount, totalQuestionsSolved, intensityLevel);
    }

    @Override
    public String toString() {
        return "HeatmapDayData{" +
                "day=" + dayOfMonth +
                ", minutes=" + practiceMinutes +
                ", intensity=" + intensityLevel +
                '}';
    }
}

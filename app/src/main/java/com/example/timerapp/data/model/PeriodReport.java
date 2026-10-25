package com.example.timerapp.data.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Detailed statistical report aggregated for a calendar period (Week or Month).
 */
public class PeriodReport implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum PeriodType {
        WEEKLY,
        MONTHLY
    }

    private PeriodType periodType;
    private String periodLabel;
    private long startTimestamp;
    private long endTimestamp;

    private int sessionCount;
    private long totalPracticeTimeMs;
    private long averageSessionDurationMs;
    private int totalQuestionsSolved;
    private long averagePaceMs;

    private double comparisonPercentageVsPrevious; // e.g. +12.5% or -8.0%
    private String bestPerformingDayLabel;
    private double bestPerformingDayMinutes;
    private double consistencyScore;

    private List<DailyPracticeDataPoint> dailyBreakdown;

    public PeriodReport() {
        this.dailyBreakdown = Collections.emptyList();
    }

    public PeriodReport(PeriodType periodType, String periodLabel, long startTimestamp, long endTimestamp,
                        int sessionCount, long totalPracticeTimeMs, long averageSessionDurationMs,
                        int totalQuestionsSolved, long averagePaceMs, double comparisonPercentageVsPrevious,
                        String bestPerformingDayLabel, double bestPerformingDayMinutes,
                        double consistencyScore, List<DailyPracticeDataPoint> dailyBreakdown) {
        this.periodType = periodType;
        this.periodLabel = periodLabel;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.sessionCount = sessionCount;
        this.totalPracticeTimeMs = totalPracticeTimeMs;
        this.averageSessionDurationMs = averageSessionDurationMs;
        this.totalQuestionsSolved = totalQuestionsSolved;
        this.averagePaceMs = averagePaceMs;
        this.comparisonPercentageVsPrevious = comparisonPercentageVsPrevious;
        this.bestPerformingDayLabel = (bestPerformingDayLabel != null) ? bestPerformingDayLabel : "N/A";
        this.bestPerformingDayMinutes = bestPerformingDayMinutes;
        this.consistencyScore = consistencyScore;
        this.dailyBreakdown = (dailyBreakdown != null) ? dailyBreakdown : Collections.emptyList();
    }

    public PeriodType getPeriodType() {
        return periodType;
    }

    public void setPeriodType(PeriodType periodType) {
        this.periodType = periodType;
    }

    public String getPeriodLabel() {
        return periodLabel;
    }

    public void setPeriodLabel(String periodLabel) {
        this.periodLabel = periodLabel;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(long endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public int getSessionCount() {
        return sessionCount;
    }

    public void setSessionCount(int sessionCount) {
        this.sessionCount = sessionCount;
    }

    public long getTotalPracticeTimeMs() {
        return totalPracticeTimeMs;
    }

    public void setTotalPracticeTimeMs(long totalPracticeTimeMs) {
        this.totalPracticeTimeMs = totalPracticeTimeMs;
    }

    public long getAverageSessionDurationMs() {
        return averageSessionDurationMs;
    }

    public void setAverageSessionDurationMs(long averageSessionDurationMs) {
        this.averageSessionDurationMs = averageSessionDurationMs;
    }

    public int getTotalQuestionsSolved() {
        return totalQuestionsSolved;
    }

    public void setTotalQuestionsSolved(int totalQuestionsSolved) {
        this.totalQuestionsSolved = totalQuestionsSolved;
    }

    public long getAveragePaceMs() {
        return averagePaceMs;
    }

    public void setAveragePaceMs(long averagePaceMs) {
        this.averagePaceMs = averagePaceMs;
    }

    public double getComparisonPercentageVsPrevious() {
        return comparisonPercentageVsPrevious;
    }

    public void setComparisonPercentageVsPrevious(double comparisonPercentageVsPrevious) {
        this.comparisonPercentageVsPrevious = comparisonPercentageVsPrevious;
    }

    public String getBestPerformingDayLabel() {
        return bestPerformingDayLabel;
    }

    public void setBestPerformingDayLabel(String bestPerformingDayLabel) {
        this.bestPerformingDayLabel = bestPerformingDayLabel;
    }

    public double getBestPerformingDayMinutes() {
        return bestPerformingDayMinutes;
    }

    public void setBestPerformingDayMinutes(double bestPerformingDayMinutes) {
        this.bestPerformingDayMinutes = bestPerformingDayMinutes;
    }

    public double getConsistencyScore() {
        return consistencyScore;
    }

    public void setConsistencyScore(double consistencyScore) {
        this.consistencyScore = consistencyScore;
    }

    public List<DailyPracticeDataPoint> getDailyBreakdown() {
        return dailyBreakdown;
    }

    public void setDailyBreakdown(List<DailyPracticeDataPoint> dailyBreakdown) {
        this.dailyBreakdown = (dailyBreakdown != null) ? dailyBreakdown : Collections.emptyList();
    }

    public String getFormattedTotalPracticeTime() {
        return SessionStats.formatDuration(totalPracticeTimeMs);
    }

    public String getFormattedAverageSessionDuration() {
        return SessionStats.formatDuration(averageSessionDurationMs);
    }

    public String getFormattedAveragePace() {
        return SessionStats.formatDuration(averagePaceMs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PeriodReport that = (PeriodReport) o;
        return startTimestamp == that.startTimestamp &&
                endTimestamp == that.endTimestamp &&
                sessionCount == that.sessionCount &&
                totalPracticeTimeMs == that.totalPracticeTimeMs &&
                averageSessionDurationMs == that.averageSessionDurationMs &&
                totalQuestionsSolved == that.totalQuestionsSolved &&
                averagePaceMs == that.averagePaceMs &&
                Double.compare(that.comparisonPercentageVsPrevious, comparisonPercentageVsPrevious) == 0 &&
                Double.compare(that.bestPerformingDayMinutes, bestPerformingDayMinutes) == 0 &&
                Double.compare(that.consistencyScore, consistencyScore) == 0 &&
                periodType == that.periodType &&
                Objects.equals(periodLabel, that.periodLabel) &&
                Objects.equals(bestPerformingDayLabel, that.bestPerformingDayLabel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periodType, periodLabel, startTimestamp, endTimestamp, sessionCount,
                totalPracticeTimeMs, averageSessionDurationMs, totalQuestionsSolved, averagePaceMs,
                comparisonPercentageVsPrevious, bestPerformingDayLabel, bestPerformingDayMinutes, consistencyScore);
    }

    @Override
    public String toString() {
        return "PeriodReport{" +
                "periodLabel='" + periodLabel + '\'' +
                ", sessionCount=" + sessionCount +
                ", totalPracticeTimeMs=" + totalPracticeTimeMs +
                ", comparisonPercentage=" + comparisonPercentageVsPrevious + "%" +
                ", consistencyScore=" + consistencyScore +
                '}';
    }
}

package com.example.timerapp.data.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregated Key Performance Indicators (KPIs) calculated over a specific time range.
 *
 * Best-performing session algorithm:
 *   Score = (questions_solved * 100.0) / (avg_duration_seconds)
 *
 * Rules:
 *   1. Sessions with questions_solved >= 3 are evaluated in the primary pool to guarantee
 *      statistical significance of pacing consistency.
 *   2. If no session has >= 3 questions, the algorithm falls back to sessions with >= 1 question.
 *   3. The session yielding the highest score is ranked as best-performing.
 *      Ties are broken by higher questions solved, then shorter total duration.
 */
public class TimeRangeKpiSummary implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String BEST_PERFORMING_FORMULA_DOC =
            "Score = (questions_solved * 100) / (avg_duration_seconds). Evaluates sessions with >= 3 questions " +
            "to ensure pacing consistency; falls back to >= 1 question if no sessions meet threshold.";

    private TimeRangeFilter rangeFilter;
    private long startTimestamp;
    private long endTimestamp;

    private int totalSessionsCount;
    private int totalCompletedSessions;
    private long totalPracticeTimeMs;
    private long averageSessionDurationMs;
    private int totalQuestionsCompleted;
    private long overallAverageTimePerQuestionMs;

    private SessionStats fastestSession;
    private SessionStats bestPerformingSession;
    private double bestPerformingScore;
    private String bestPerformingFormula;

    private List<SessionStats> sessionList;

    public TimeRangeKpiSummary() {
        this.bestPerformingFormula = BEST_PERFORMING_FORMULA_DOC;
    }

    public TimeRangeKpiSummary(TimeRangeFilter rangeFilter, long startTimestamp, long endTimestamp,
                               int totalSessionsCount, int totalCompletedSessions,
                               long totalPracticeTimeMs, long averageSessionDurationMs,
                               int totalQuestionsCompleted, long overallAverageTimePerQuestionMs,
                               SessionStats fastestSession, SessionStats bestPerformingSession,
                               double bestPerformingScore, List<SessionStats> sessionList) {
        this.rangeFilter = rangeFilter;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.totalSessionsCount = totalSessionsCount;
        this.totalCompletedSessions = totalCompletedSessions;
        this.totalPracticeTimeMs = totalPracticeTimeMs;
        this.averageSessionDurationMs = averageSessionDurationMs;
        this.totalQuestionsCompleted = totalQuestionsCompleted;
        this.overallAverageTimePerQuestionMs = overallAverageTimePerQuestionMs;
        this.fastestSession = fastestSession;
        this.bestPerformingSession = bestPerformingSession;
        this.bestPerformingScore = bestPerformingScore;
        this.bestPerformingFormula = BEST_PERFORMING_FORMULA_DOC;
        this.sessionList = (sessionList != null) ? sessionList : Collections.emptyList();
    }

    public TimeRangeFilter getRangeFilter() {
        return rangeFilter;
    }

    public void setRangeFilter(TimeRangeFilter rangeFilter) {
        this.rangeFilter = rangeFilter;
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

    public int getTotalSessionsCount() {
        return totalSessionsCount;
    }

    public void setTotalSessionsCount(int totalSessionsCount) {
        this.totalSessionsCount = totalSessionsCount;
    }

    public int getTotalCompletedSessions() {
        return totalCompletedSessions;
    }

    public void setTotalCompletedSessions(int totalCompletedSessions) {
        this.totalCompletedSessions = totalCompletedSessions;
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

    public int getTotalQuestionsCompleted() {
        return totalQuestionsCompleted;
    }

    public void setTotalQuestionsCompleted(int totalQuestionsCompleted) {
        this.totalQuestionsCompleted = totalQuestionsCompleted;
    }

    public long getOverallAverageTimePerQuestionMs() {
        return overallAverageTimePerQuestionMs;
    }

    public void setOverallAverageTimePerQuestionMs(long overallAverageTimePerQuestionMs) {
        this.overallAverageTimePerQuestionMs = overallAverageTimePerQuestionMs;
    }

    public SessionStats getFastestSession() {
        return fastestSession;
    }

    public void setFastestSession(SessionStats fastestSession) {
        this.fastestSession = fastestSession;
    }

    public SessionStats getBestPerformingSession() {
        return bestPerformingSession;
    }

    public void setBestPerformingSession(SessionStats bestPerformingSession) {
        this.bestPerformingSession = bestPerformingSession;
    }

    public double getBestPerformingScore() {
        return bestPerformingScore;
    }

    public void setBestPerformingScore(double bestPerformingScore) {
        this.bestPerformingScore = bestPerformingScore;
    }

    public String getBestPerformingFormula() {
        return bestPerformingFormula;
    }

    public void setBestPerformingFormula(String bestPerformingFormula) {
        this.bestPerformingFormula = bestPerformingFormula;
    }

    public List<SessionStats> getSessionList() {
        return sessionList != null ? sessionList : Collections.emptyList();
    }

    public void setSessionList(List<SessionStats> sessionList) {
        this.sessionList = (sessionList != null) ? sessionList : Collections.emptyList();
    }

    // =========================================================================
    // UI Helpers
    // =========================================================================

    public String getFormattedTotalPracticeTime() {
        return SessionStats.formatDuration(totalPracticeTimeMs);
    }

    public String getFormattedAverageSessionDuration() {
        return SessionStats.formatDuration(averageSessionDurationMs);
    }

    public String getFormattedOverallAverageTimePerQuestion() {
        return SessionStats.formatDuration(overallAverageTimePerQuestionMs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeRangeKpiSummary that = (TimeRangeKpiSummary) o;
        return startTimestamp == that.startTimestamp &&
                endTimestamp == that.endTimestamp &&
                totalSessionsCount == that.totalSessionsCount &&
                totalCompletedSessions == that.totalCompletedSessions &&
                totalPracticeTimeMs == that.totalPracticeTimeMs &&
                averageSessionDurationMs == that.averageSessionDurationMs &&
                totalQuestionsCompleted == that.totalQuestionsCompleted &&
                overallAverageTimePerQuestionMs == that.overallAverageTimePerQuestionMs &&
                Double.compare(that.bestPerformingScore, bestPerformingScore) == 0 &&
                rangeFilter == that.rangeFilter &&
                Objects.equals(fastestSession, that.fastestSession) &&
                Objects.equals(bestPerformingSession, that.bestPerformingSession);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rangeFilter, startTimestamp, endTimestamp, totalSessionsCount,
                totalCompletedSessions, totalPracticeTimeMs, averageSessionDurationMs,
                totalQuestionsCompleted, overallAverageTimePerQuestionMs, fastestSession,
                bestPerformingSession, bestPerformingScore);
    }

    @Override
    public String toString() {
        return "TimeRangeKpiSummary{" +
                "rangeFilter=" + rangeFilter +
                ", totalSessionsCount=" + totalSessionsCount +
                ", totalCompletedSessions=" + totalCompletedSessions +
                ", totalPracticeTimeMs=" + totalPracticeTimeMs +
                ", averageSessionDurationMs=" + averageSessionDurationMs +
                ", totalQuestionsCompleted=" + totalQuestionsCompleted +
                ", overallAverageTimePerQuestionMs=" + overallAverageTimePerQuestionMs +
                ", bestPerformingScore=" + bestPerformingScore +
                '}';
    }
}

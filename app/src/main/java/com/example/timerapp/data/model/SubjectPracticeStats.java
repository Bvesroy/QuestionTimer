package com.example.timerapp.data.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Statistics aggregated for a specific study subject.
 */
public class SubjectPracticeStats implements Serializable {
    private static final long serialVersionUID = 1L;

    private String subject;
    private String colorHex;
    private long practiceTimeMs;
    private int sessionCount;
    private int questionsSolved;
    private long averageQuestionPaceMs;
    private double percentageOfTotalPracticeTime;

    public SubjectPracticeStats() {
        this.subject = "General";
        this.colorHex = "#8E8E93";
    }

    public SubjectPracticeStats(String subject, String colorHex, long practiceTimeMs,
                                int sessionCount, int questionsSolved, long averageQuestionPaceMs,
                                double percentageOfTotalPracticeTime) {
        this.subject = (subject != null && !subject.trim().isEmpty()) ? subject : "General";
        this.colorHex = (colorHex != null && !colorHex.trim().isEmpty()) ? colorHex : "#8E8E93";
        this.practiceTimeMs = practiceTimeMs;
        this.sessionCount = sessionCount;
        this.questionsSolved = questionsSolved;
        this.averageQuestionPaceMs = averageQuestionPaceMs;
        this.percentageOfTotalPracticeTime = percentageOfTotalPracticeTime;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }

    public long getPracticeTimeMs() {
        return practiceTimeMs;
    }

    public void setPracticeTimeMs(long practiceTimeMs) {
        this.practiceTimeMs = practiceTimeMs;
    }

    public int getSessionCount() {
        return sessionCount;
    }

    public void setSessionCount(int sessionCount) {
        this.sessionCount = sessionCount;
    }

    public int getQuestionsSolved() {
        return questionsSolved;
    }

    public void setQuestionsSolved(int questionsSolved) {
        this.questionsSolved = questionsSolved;
    }

    public long getAverageQuestionPaceMs() {
        return averageQuestionPaceMs;
    }

    public void setAverageQuestionPaceMs(long averageQuestionPaceMs) {
        this.averageQuestionPaceMs = averageQuestionPaceMs;
    }

    public double getPercentageOfTotalPracticeTime() {
        return percentageOfTotalPracticeTime;
    }

    public void setPercentageOfTotalPracticeTime(double percentageOfTotalPracticeTime) {
        this.percentageOfTotalPracticeTime = percentageOfTotalPracticeTime;
    }

    public String getFormattedPracticeTime() {
        return SessionStats.formatDuration(practiceTimeMs);
    }

    public String getFormattedAveragePace() {
        return SessionStats.formatDuration(averageQuestionPaceMs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubjectPracticeStats that = (SubjectPracticeStats) o;
        return practiceTimeMs == that.practiceTimeMs &&
                sessionCount == that.sessionCount &&
                questionsSolved == that.questionsSolved &&
                averageQuestionPaceMs == that.averageQuestionPaceMs &&
                Double.compare(that.percentageOfTotalPracticeTime, percentageOfTotalPracticeTime) == 0 &&
                Objects.equals(subject, that.subject) &&
                Objects.equals(colorHex, that.colorHex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, colorHex, practiceTimeMs, sessionCount, questionsSolved,
                averageQuestionPaceMs, percentageOfTotalPracticeTime);
    }

    @Override
    public String toString() {
        return "SubjectPracticeStats{" +
                "subject='" + subject + '\'' +
                ", practiceTimeMs=" + practiceTimeMs +
                ", sessionCount=" + sessionCount +
                ", percentage=" + percentageOfTotalPracticeTime + "%" +
                '}';
    }
}

package com.example.timerapp.data.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Analytical report for JEE Main / JEE Advanced mock tests and practice sessions.
 */
public class JeeAnalyticsReport implements Serializable {
    private static final long serialVersionUID = 1L;

    private String profileName;
    private long startTimestamp;
    private long endTimestamp;

    private int totalQuestionsExam;
    private int questionsAttempted;
    private int questionsCorrect;
    private int questionsIncorrect;
    private int questionsSkipped;

    private double attemptRatePercentage;
    private double accuracyPercentage;
    private double averagePaceSeconds;
    private int questionsExceedingTargetPace;

    private double projectedScore;
    private double maxPossibleScore;

    private long totalStudyTimeMs;
    private long totalBreakTimeMs;

    // Subject/section distribution: Subject -> practice time in ms
    private Map<String, Long> subjectTimeDistribution;
    // Subject/section question counts: Subject -> questions attempted
    private Map<String, Integer> subjectQuestionCounts;
    // Mistake tag distribution: Mistake Tag -> count
    private Map<String, Integer> mistakeDistribution;

    public JeeAnalyticsReport() {
        this.profileName = "JEE_MAIN";
        this.subjectTimeDistribution = new HashMap<>();
        this.subjectQuestionCounts = new HashMap<>();
        this.mistakeDistribution = new HashMap<>();
    }

    public JeeAnalyticsReport(String profileName, long startTimestamp, long endTimestamp,
                              int totalQuestionsExam, int questionsAttempted, int questionsCorrect,
                              int questionsIncorrect, int questionsSkipped, double attemptRatePercentage,
                              double accuracyPercentage, double averagePaceSeconds,
                              int questionsExceedingTargetPace, double projectedScore,
                              double maxPossibleScore, long totalStudyTimeMs, long totalBreakTimeMs,
                              Map<String, Long> subjectTimeDistribution,
                              Map<String, Integer> subjectQuestionCounts,
                              Map<String, Integer> mistakeDistribution) {
        this.profileName = profileName != null ? profileName : "JEE_MAIN";
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.totalQuestionsExam = totalQuestionsExam;
        this.questionsAttempted = questionsAttempted;
        this.questionsCorrect = questionsCorrect;
        this.questionsIncorrect = questionsIncorrect;
        this.questionsSkipped = questionsSkipped;
        this.attemptRatePercentage = attemptRatePercentage;
        this.accuracyPercentage = accuracyPercentage;
        this.averagePaceSeconds = averagePaceSeconds;
        this.questionsExceedingTargetPace = questionsExceedingTargetPace;
        this.projectedScore = projectedScore;
        this.maxPossibleScore = maxPossibleScore;
        this.totalStudyTimeMs = totalStudyTimeMs;
        this.totalBreakTimeMs = totalBreakTimeMs;
        this.subjectTimeDistribution = (subjectTimeDistribution != null) ? subjectTimeDistribution : new HashMap<>();
        this.subjectQuestionCounts = (subjectQuestionCounts != null) ? subjectQuestionCounts : new HashMap<>();
        this.mistakeDistribution = (mistakeDistribution != null) ? mistakeDistribution : new HashMap<>();
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName != null ? profileName : "JEE_MAIN";
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

    public int getTotalQuestionsExam() {
        return totalQuestionsExam;
    }

    public void setTotalQuestionsExam(int totalQuestionsExam) {
        this.totalQuestionsExam = totalQuestionsExam;
    }

    public int getQuestionsAttempted() {
        return questionsAttempted;
    }

    public void setQuestionsAttempted(int questionsAttempted) {
        this.questionsAttempted = questionsAttempted;
    }

    public int getQuestionsCorrect() {
        return questionsCorrect;
    }

    public void setQuestionsCorrect(int questionsCorrect) {
        this.questionsCorrect = questionsCorrect;
    }

    public int getQuestionsIncorrect() {
        return questionsIncorrect;
    }

    public void setQuestionsIncorrect(int questionsIncorrect) {
        this.questionsIncorrect = questionsIncorrect;
    }

    public int getQuestionsSkipped() {
        return questionsSkipped;
    }

    public void setQuestionsSkipped(int questionsSkipped) {
        this.questionsSkipped = questionsSkipped;
    }

    public double getAttemptRatePercentage() {
        return attemptRatePercentage;
    }

    public void setAttemptRatePercentage(double attemptRatePercentage) {
        this.attemptRatePercentage = attemptRatePercentage;
    }

    public double getAccuracyPercentage() {
        return accuracyPercentage;
    }

    public void setAccuracyPercentage(double accuracyPercentage) {
        this.accuracyPercentage = accuracyPercentage;
    }

    public double getAveragePaceSeconds() {
        return averagePaceSeconds;
    }

    public void setAveragePaceSeconds(double averagePaceSeconds) {
        this.averagePaceSeconds = averagePaceSeconds;
    }

    public int getQuestionsExceedingTargetPace() {
        return questionsExceedingTargetPace;
    }

    public void setQuestionsExceedingTargetPace(int questionsExceedingTargetPace) {
        this.questionsExceedingTargetPace = questionsExceedingTargetPace;
    }

    public double getProjectedScore() {
        return projectedScore;
    }

    public void setProjectedScore(double projectedScore) {
        this.projectedScore = projectedScore;
    }

    public double getMaxPossibleScore() {
        return maxPossibleScore;
    }

    public void setMaxPossibleScore(double maxPossibleScore) {
        this.maxPossibleScore = maxPossibleScore;
    }

    public long getTotalStudyTimeMs() {
        return totalStudyTimeMs;
    }

    public void setTotalStudyTimeMs(long totalStudyTimeMs) {
        this.totalStudyTimeMs = totalStudyTimeMs;
    }

    public long getTotalBreakTimeMs() {
        return totalBreakTimeMs;
    }

    public void setTotalBreakTimeMs(long totalBreakTimeMs) {
        this.totalBreakTimeMs = totalBreakTimeMs;
    }

    public Map<String, Long> getSubjectTimeDistribution() {
        return Collections.unmodifiableMap(subjectTimeDistribution);
    }

    public void setSubjectTimeDistribution(Map<String, Long> subjectTimeDistribution) {
        this.subjectTimeDistribution = (subjectTimeDistribution != null) ? subjectTimeDistribution : new HashMap<>();
    }

    public Map<String, Integer> getSubjectQuestionCounts() {
        return Collections.unmodifiableMap(subjectQuestionCounts);
    }

    public void setSubjectQuestionCounts(Map<String, Integer> subjectQuestionCounts) {
        this.subjectQuestionCounts = (subjectQuestionCounts != null) ? subjectQuestionCounts : new HashMap<>();
    }

    public Map<String, Integer> getMistakeDistribution() {
        return Collections.unmodifiableMap(mistakeDistribution);
    }

    public void setMistakeDistribution(Map<String, Integer> mistakeDistribution) {
        this.mistakeDistribution = (mistakeDistribution != null) ? mistakeDistribution : new HashMap<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JeeAnalyticsReport that = (JeeAnalyticsReport) o;
        return startTimestamp == that.startTimestamp &&
                endTimestamp == that.endTimestamp &&
                questionsAttempted == that.questionsAttempted &&
                Double.compare(that.projectedScore, projectedScore) == 0 &&
                Objects.equals(profileName, that.profileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(profileName, startTimestamp, endTimestamp, questionsAttempted, projectedScore);
    }

    @Override
    public String toString() {
        return "JeeAnalyticsReport{" +
                "profile='" + profileName + '\'' +
                ", attempted=" + questionsAttempted +
                ", accuracy=" + accuracyPercentage + "%" +
                ", score=" + projectedScore + "/" + maxPossibleScore +
                ", pace=" + averagePaceSeconds + "s" +
                '}';
    }
}

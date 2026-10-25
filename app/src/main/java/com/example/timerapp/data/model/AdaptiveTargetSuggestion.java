package com.example.timerapp.data.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Adaptive recommendation for daily practice targets based on 7-day rolling median with +10% progression.
 */
public class AdaptiveTargetSuggestion implements Serializable {
    private static final long serialVersionUID = 1L;

    private double currentDailyAverageMinutes;
    private int suggestedDailyMinutes;
    private int suggestedQuestionsPerDay;
    private double progressionPercentage;
    private String reasoning;

    public AdaptiveTargetSuggestion() {
        this.progressionPercentage = 10.0;
        this.suggestedDailyMinutes = 60;
        this.suggestedQuestionsPerDay = 50;
        this.reasoning = "Default baseline study target.";
    }

    public AdaptiveTargetSuggestion(double currentDailyAverageMinutes, int suggestedDailyMinutes,
                                    int suggestedQuestionsPerDay, double progressionPercentage,
                                    String reasoning) {
        this.currentDailyAverageMinutes = currentDailyAverageMinutes;
        this.suggestedDailyMinutes = suggestedDailyMinutes;
        this.suggestedQuestionsPerDay = suggestedQuestionsPerDay;
        this.progressionPercentage = progressionPercentage;
        this.reasoning = (reasoning != null) ? reasoning : "";
    }

    public double getCurrentDailyAverageMinutes() {
        return currentDailyAverageMinutes;
    }

    public void setCurrentDailyAverageMinutes(double currentDailyAverageMinutes) {
        this.currentDailyAverageMinutes = currentDailyAverageMinutes;
    }

    public int getSuggestedDailyMinutes() {
        return suggestedDailyMinutes;
    }

    public void setSuggestedDailyMinutes(int suggestedDailyMinutes) {
        this.suggestedDailyMinutes = suggestedDailyMinutes;
    }

    public int getSuggestedQuestionsPerDay() {
        return suggestedQuestionsPerDay;
    }

    public void setSuggestedQuestionsPerDay(int suggestedQuestionsPerDay) {
        this.suggestedQuestionsPerDay = suggestedQuestionsPerDay;
    }

    public double getProgressionPercentage() {
        return progressionPercentage;
    }

    public void setProgressionPercentage(double progressionPercentage) {
        this.progressionPercentage = progressionPercentage;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = (reasoning != null) ? reasoning : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdaptiveTargetSuggestion that = (AdaptiveTargetSuggestion) o;
        return Double.compare(that.currentDailyAverageMinutes, currentDailyAverageMinutes) == 0 &&
                suggestedDailyMinutes == that.suggestedDailyMinutes &&
                suggestedQuestionsPerDay == that.suggestedQuestionsPerDay &&
                Double.compare(that.progressionPercentage, progressionPercentage) == 0 &&
                Objects.equals(reasoning, that.reasoning);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentDailyAverageMinutes, suggestedDailyMinutes,
                suggestedQuestionsPerDay, progressionPercentage, reasoning);
    }

    @Override
    public String toString() {
        return "AdaptiveTargetSuggestion{" +
                "currentDailyAvgMin=" + currentDailyAverageMinutes +
                ", suggestedDailyMin=" + suggestedDailyMinutes +
                ", suggestedQuestions=" + suggestedQuestionsPerDay +
                ", reasoning='" + reasoning + '\'' +
                '}';
    }
}

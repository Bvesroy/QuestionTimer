package com.example.timerapp.data.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Model representing an actionable, statistical study coach recommendation.
 */
public class StudyRecommendation implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum ConfidenceLevel {
        LOW,
        MEDIUM,
        HIGH
    }

    private String key;
    private String title;
    private String observation;
    private String timeframe;
    private int sampleSize;
    private String suggestedAction;
    private String rationale;
    private ConfidenceLevel confidenceLevel;
    private boolean isActionable;

    public StudyRecommendation() {
        this.confidenceLevel = ConfidenceLevel.MEDIUM;
        this.isActionable = true;
    }

    public StudyRecommendation(String key, String title, String observation, String timeframe,
                               int sampleSize, String suggestedAction, String rationale,
                               ConfidenceLevel confidenceLevel) {
        this(key, title, observation, timeframe, sampleSize, suggestedAction, rationale, confidenceLevel, true);
    }

    public StudyRecommendation(String key, String title, String observation, String timeframe,
                               int sampleSize, String suggestedAction, String rationale,
                               ConfidenceLevel confidenceLevel, boolean isActionable) {
        this.key = key != null ? key : "";
        this.title = title != null ? title : "";
        this.observation = observation != null ? observation : "";
        this.timeframe = timeframe != null ? timeframe : "";
        this.sampleSize = sampleSize;
        this.suggestedAction = suggestedAction != null ? suggestedAction : "";
        this.rationale = rationale != null ? rationale : "";
        this.confidenceLevel = confidenceLevel != null ? confidenceLevel : ConfidenceLevel.MEDIUM;
        this.isActionable = isActionable;
    }

    public static StudyRecommendation createInsufficientData(int sampleSize, String timeframe) {
        return new StudyRecommendation(
                "insufficient_data",
                "Insufficient Practice Data",
                "Less than 5 questions or fewer than 2 sessions logged in the selected window.",
                timeframe,
                sampleSize,
                "Complete at least 2 sessions with 5+ questions to unlock personalized coach insights.",
                "Statistical confidence requires a baseline sample size to avoid false trend correlations.",
                ConfidenceLevel.LOW,
                false
        );
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key != null ? key : "";
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title != null ? title : "";
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation != null ? observation : "";
    }

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe != null ? timeframe : "";
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }

    public String getSuggestedAction() {
        return suggestedAction;
    }

    public void setSuggestedAction(String suggestedAction) {
        this.suggestedAction = suggestedAction != null ? suggestedAction : "";
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale != null ? rationale : "";
    }

    public ConfidenceLevel getConfidenceLevel() {
        return confidenceLevel;
    }

    public void setConfidenceLevel(ConfidenceLevel confidenceLevel) {
        this.confidenceLevel = confidenceLevel != null ? confidenceLevel : ConfidenceLevel.MEDIUM;
    }

    public boolean isActionable() {
        return isActionable;
    }

    public void setActionable(boolean actionable) {
        isActionable = actionable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StudyRecommendation that = (StudyRecommendation) o;
        return sampleSize == that.sampleSize &&
                isActionable == that.isActionable &&
                Objects.equals(key, that.key) &&
                Objects.equals(title, that.title) &&
                confidenceLevel == that.confidenceLevel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, title, sampleSize, confidenceLevel, isActionable);
    }

    @Override
    public String toString() {
        return "StudyRecommendation{" +
                "key='" + key + '\'' +
                ", title='" + title + '\'' +
                ", confidence=" + confidenceLevel +
                ", sampleSize=" + sampleSize +
                '}';
    }
}

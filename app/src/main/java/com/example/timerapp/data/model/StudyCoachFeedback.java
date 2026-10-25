package com.example.timerapp.data.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Model representing user feedback or dismissal state for study coach recommendations.
 */
public class StudyCoachFeedback implements Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private String recommendationKey;
    private String feedback;
    private boolean dismissed;
    private long createdAt;

    public StudyCoachFeedback() {
        this.createdAt = System.currentTimeMillis();
    }

    public StudyCoachFeedback(long id, String recommendationKey, String feedback, boolean dismissed, long createdAt) {
        this.id = id;
        this.recommendationKey = recommendationKey != null ? recommendationKey : "";
        this.feedback = feedback != null ? feedback : "";
        this.dismissed = dismissed;
        this.createdAt = createdAt;
    }

    public StudyCoachFeedback(String recommendationKey, String feedback, boolean dismissed) {
        this(0, recommendationKey, feedback, dismissed, System.currentTimeMillis());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRecommendationKey() {
        return recommendationKey;
    }

    public void setRecommendationKey(String recommendationKey) {
        this.recommendationKey = recommendationKey != null ? recommendationKey : "";
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback != null ? feedback : "";
    }

    public boolean isDismissed() {
        return dismissed;
    }

    public void setDismissed(boolean dismissed) {
        this.dismissed = dismissed;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StudyCoachFeedback that = (StudyCoachFeedback) o;
        return id == that.id &&
                dismissed == that.dismissed &&
                createdAt == that.createdAt &&
                Objects.equals(recommendationKey, that.recommendationKey) &&
                Objects.equals(feedback, that.feedback);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, recommendationKey, feedback, dismissed, createdAt);
    }

    @Override
    public String toString() {
        return "StudyCoachFeedback{" +
                "id=" + id +
                ", key='" + recommendationKey + '\'' +
                ", dismissed=" + dismissed +
                ", createdAt=" + createdAt +
                '}';
    }
}

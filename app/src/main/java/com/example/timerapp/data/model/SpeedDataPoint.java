package com.example.timerapp.data.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Data point for speed-over-time charts and pace analytics.
 */
public class SpeedDataPoint implements Serializable {
    private static final long serialVersionUID = 1L;

    private long sessionId;
    private int sessionIndex;
    private String sessionName;
    private long timestamp;
    private String dateLabel;
    private double avgTimePerQuestionSeconds;
    private int questionsSolved;

    public SpeedDataPoint() {
    }

    public SpeedDataPoint(long sessionId, int sessionIndex, String sessionName,
                          long timestamp, String dateLabel, double avgTimePerQuestionSeconds,
                          int questionsSolved) {
        this.sessionId = sessionId;
        this.sessionIndex = sessionIndex;
        this.sessionName = sessionName;
        this.timestamp = timestamp;
        this.dateLabel = dateLabel;
        this.avgTimePerQuestionSeconds = avgTimePerQuestionSeconds;
        this.questionsSolved = questionsSolved;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public int getSessionIndex() {
        return sessionIndex;
    }

    public void setSessionIndex(int sessionIndex) {
        this.sessionIndex = sessionIndex;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getDateLabel() {
        return dateLabel;
    }

    public void setDateLabel(String dateLabel) {
        this.dateLabel = dateLabel;
    }

    public double getAvgTimePerQuestionSeconds() {
        return avgTimePerQuestionSeconds;
    }

    public void setAvgTimePerQuestionSeconds(double avgTimePerQuestionSeconds) {
        this.avgTimePerQuestionSeconds = avgTimePerQuestionSeconds;
    }

    public int getQuestionsSolved() {
        return questionsSolved;
    }

    public void setQuestionsSolved(int questionsSolved) {
        this.questionsSolved = questionsSolved;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpeedDataPoint that = (SpeedDataPoint) o;
        return sessionId == that.sessionId &&
                sessionIndex == that.sessionIndex &&
                timestamp == that.timestamp &&
                Double.compare(that.avgTimePerQuestionSeconds, avgTimePerQuestionSeconds) == 0 &&
                questionsSolved == that.questionsSolved &&
                Objects.equals(sessionName, that.sessionName) &&
                Objects.equals(dateLabel, that.dateLabel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, sessionIndex, sessionName, timestamp,
                dateLabel, avgTimePerQuestionSeconds, questionsSolved);
    }

    @Override
    public String toString() {
        return "SpeedDataPoint{" +
                "sessionIndex=" + sessionIndex +
                ", sessionName='" + sessionName + '\'' +
                ", avgTimePerQuestionSeconds=" + avgTimePerQuestionSeconds +
                ", dateLabel='" + dateLabel + '\'' +
                '}';
    }
}

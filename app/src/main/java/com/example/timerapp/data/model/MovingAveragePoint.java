package com.example.timerapp.data.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Data point for rolling moving average analysis over a window of sessions.
 */
public class MovingAveragePoint implements Serializable {
    private static final long serialVersionUID = 1L;

    private int sessionIndex;
    private long sessionId;
    private String sessionName;
    private long timestamp;
    private String dateLabel;
    private double rollingAvgPaceSeconds;
    private double rollingAvgDurationMinutes;
    private int windowSize;

    public MovingAveragePoint() {
    }

    public MovingAveragePoint(int sessionIndex, long sessionId, String sessionName,
                              long timestamp, String dateLabel, double rollingAvgPaceSeconds,
                              double rollingAvgDurationMinutes, int windowSize) {
        this.sessionIndex = sessionIndex;
        this.sessionId = sessionId;
        this.sessionName = sessionName;
        this.timestamp = timestamp;
        this.dateLabel = dateLabel;
        this.rollingAvgPaceSeconds = rollingAvgPaceSeconds;
        this.rollingAvgDurationMinutes = rollingAvgDurationMinutes;
        this.windowSize = windowSize;
    }

    public int getSessionIndex() {
        return sessionIndex;
    }

    public void setSessionIndex(int sessionIndex) {
        this.sessionIndex = sessionIndex;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
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

    public double getRollingAvgPaceSeconds() {
        return rollingAvgPaceSeconds;
    }

    public void setRollingAvgPaceSeconds(double rollingAvgPaceSeconds) {
        this.rollingAvgPaceSeconds = rollingAvgPaceSeconds;
    }

    public double getRollingAvgDurationMinutes() {
        return rollingAvgDurationMinutes;
    }

    public void setRollingAvgDurationMinutes(double rollingAvgDurationMinutes) {
        this.rollingAvgDurationMinutes = rollingAvgDurationMinutes;
    }

    public int getWindowSize() {
        return windowSize;
    }

    public void setWindowSize(int windowSize) {
        this.windowSize = windowSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MovingAveragePoint that = (MovingAveragePoint) o;
        return sessionIndex == that.sessionIndex &&
                sessionId == that.sessionId &&
                timestamp == that.timestamp &&
                Double.compare(that.rollingAvgPaceSeconds, rollingAvgPaceSeconds) == 0 &&
                Double.compare(that.rollingAvgDurationMinutes, rollingAvgDurationMinutes) == 0 &&
                windowSize == that.windowSize &&
                Objects.equals(sessionName, that.sessionName) &&
                Objects.equals(dateLabel, that.dateLabel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionIndex, sessionId, sessionName, timestamp, dateLabel,
                rollingAvgPaceSeconds, rollingAvgDurationMinutes, windowSize);
    }

    @Override
    public String toString() {
        return "MovingAveragePoint{" +
                "sessionIndex=" + sessionIndex +
                ", rollingAvgPaceSeconds=" + rollingAvgPaceSeconds +
                ", rollingAvgDurationMinutes=" + rollingAvgDurationMinutes +
                ", windowSize=" + windowSize +
                '}';
    }
}

package com.example.timerapp.data.model;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

/**
 * Model representing a recorded break interval within a session.
 */
public class BreakLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private long sessionId;
    private long startTime;
    private long endTime;
    private long durationMs;

    public BreakLog() {
    }

    public BreakLog(long id, long sessionId, long startTime, long endTime, long durationMs) {
        this.id = id;
        this.sessionId = sessionId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMs = durationMs > 0 ? durationMs : Math.max(0, endTime - startTime);
    }

    public BreakLog(long sessionId, long startTime, long endTime) {
        this(0, sessionId, startTime, endTime, Math.max(0, endTime - startTime));
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
        if (endTime >= startTime) {
            this.durationMs = endTime - startTime;
        }
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public boolean isActive() {
        return endTime <= 0 || endTime < startTime;
    }

    public String getFormattedDuration() {
        long totalSeconds = durationMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BreakLog breakLog = (BreakLog) o;
        return id == breakLog.id &&
                sessionId == breakLog.sessionId &&
                startTime == breakLog.startTime &&
                endTime == breakLog.endTime &&
                durationMs == breakLog.durationMs;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sessionId, startTime, endTime, durationMs);
    }

    @Override
    public String toString() {
        return "BreakLog{" +
                "id=" + id +
                ", sessionId=" + sessionId +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", durationMs=" + durationMs +
                '}';
    }
}

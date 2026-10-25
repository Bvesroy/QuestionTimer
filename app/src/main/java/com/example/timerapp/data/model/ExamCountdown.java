package com.example.timerapp.data.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Model representing an upcoming exam countdown target.
 */
public class ExamCountdown implements Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private String title;
    private long targetDateMillis;
    private String description;

    public ExamCountdown() {
        this.targetDateMillis = System.currentTimeMillis();
    }

    public ExamCountdown(long id, String title, long targetDateMillis, String description) {
        this.id = id;
        this.title = title;
        this.targetDateMillis = targetDateMillis;
        this.description = description != null ? description : "";
    }

    public ExamCountdown(String title, long targetDateMillis, String description) {
        this(0, title, targetDateMillis, description);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getTargetDateMillis() {
        return targetDateMillis;
    }

    public void setTargetDateMillis(long targetDateMillis) {
        this.targetDateMillis = targetDateMillis;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }

    /**
     * Calculates the remaining full days until the exam date relative to current time.
     * Returns 0 or negative value if exam date has already passed.
     */
    public long getDaysRemaining(long nowMillis) {
        long diff = targetDateMillis - nowMillis;
        if (diff <= 0) return 0L;
        return TimeUnit.MILLISECONDS.toDays(diff);
    }

    public boolean isExpired(long nowMillis) {
        return targetDateMillis < nowMillis;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExamCountdown that = (ExamCountdown) o;
        return id == that.id &&
                targetDateMillis == that.targetDateMillis &&
                Objects.equals(title, that.title) &&
                Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, targetDateMillis, description);
    }

    @Override
    public String toString() {
        return "ExamCountdown{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", targetDateMillis=" + targetDateMillis +
                ", description='" + description + '\'' +
                '}';
    }
}

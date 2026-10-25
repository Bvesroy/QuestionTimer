package com.example.timerapp.data.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/**
 * Model representing an unlocked milestone achievement.
 */
public class Achievement implements Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private String key;
    private String title;
    private long unlockedAt;

    public Achievement() {
        this.unlockedAt = System.currentTimeMillis();
    }

    public Achievement(long id, String key, String title, long unlockedAt) {
        this.id = id;
        this.key = key;
        this.title = title;
        this.unlockedAt = unlockedAt;
    }

    public Achievement(String key, String title, long unlockedAt) {
        this(0, key, title, unlockedAt);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getUnlockedAt() {
        return unlockedAt;
    }

    public void setUnlockedAt(long unlockedAt) {
        this.unlockedAt = unlockedAt;
    }

    public String getFormattedUnlockedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        return sdf.format(new Date(unlockedAt));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Achievement that = (Achievement) o;
        return id == that.id &&
                unlockedAt == that.unlockedAt &&
                Objects.equals(key, that.key) &&
                Objects.equals(title, that.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, key, title, unlockedAt);
    }

    @Override
    public String toString() {
        return "Achievement{" +
                "id=" + id +
                ", key='" + key + '\'' +
                ", title='" + title + '\'' +
                ", unlockedAt=" + unlockedAt +
                '}';
    }
}

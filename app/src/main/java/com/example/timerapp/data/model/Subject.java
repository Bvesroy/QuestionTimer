package com.example.timerapp.data.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Model representing a study subject with associated theme color.
 */
public class Subject implements Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private String name;
    private String colorHex;
    private long createdAt;

    public Subject() {
        this.colorHex = "#8E8E93";
        this.createdAt = System.currentTimeMillis();
    }

    public Subject(long id, String name, String colorHex, long createdAt) {
        this.id = id;
        this.name = name;
        this.colorHex = colorHex;
        this.createdAt = createdAt;
    }

    public Subject(String name, String colorHex) {
        this(0, name, colorHex, System.currentTimeMillis());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
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
        Subject subject = (Subject) o;
        return id == subject.id &&
                createdAt == subject.createdAt &&
                Objects.equals(name, subject.name) &&
                Objects.equals(colorHex, subject.colorHex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, colorHex, createdAt);
    }

    @Override
    public String toString() {
        return "Subject{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", colorHex='" + colorHex + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

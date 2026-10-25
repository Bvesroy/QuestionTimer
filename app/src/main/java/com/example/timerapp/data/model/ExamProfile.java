package com.example.timerapp.data.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Model representing an exam pattern configuration (e.g. JEE Main, JEE Advanced).
 */
public class ExamProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String PROFILE_JEE_MAIN = "JEE_MAIN";
    public static final String PROFILE_JEE_ADVANCED = "JEE_ADVANCED";
    public static final String PROFILE_PRACTICE = "PRACTICE";

    private long id;
    private String name;
    private int totalDurationMinutes;
    private int totalQuestions;
    private double correctMarks;
    private double negativeMarks;
    private String sectionsJson;

    public ExamProfile() {
        this.name = PROFILE_PRACTICE;
        this.totalDurationMinutes = 180;
        this.totalQuestions = 75;
        this.correctMarks = 4.0;
        this.negativeMarks = 1.0;
        this.sectionsJson = "[]";
    }

    public ExamProfile(long id, String name, int totalDurationMinutes, int totalQuestions,
                       double correctMarks, double negativeMarks, String sectionsJson) {
        this.id = id;
        this.name = (name != null) ? name.trim().toUpperCase() : PROFILE_PRACTICE;
        this.totalDurationMinutes = totalDurationMinutes;
        this.totalQuestions = totalQuestions;
        this.correctMarks = correctMarks;
        this.negativeMarks = negativeMarks;
        this.sectionsJson = (sectionsJson != null) ? sectionsJson : "[]";
    }

    public ExamProfile(String name, int totalDurationMinutes, int totalQuestions,
                       double correctMarks, double negativeMarks, String sectionsJson) {
        this(0, name, totalDurationMinutes, totalQuestions, correctMarks, negativeMarks, sectionsJson);
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
        this.name = (name != null) ? name.trim().toUpperCase() : PROFILE_PRACTICE;
    }

    public int getTotalDurationMinutes() {
        return totalDurationMinutes;
    }

    public void setTotalDurationMinutes(int totalDurationMinutes) {
        this.totalDurationMinutes = totalDurationMinutes;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public double getCorrectMarks() {
        return correctMarks;
    }

    public void setCorrectMarks(double correctMarks) {
        this.correctMarks = correctMarks;
    }

    public double getNegativeMarks() {
        return negativeMarks;
    }

    public void setNegativeMarks(double negativeMarks) {
        this.negativeMarks = negativeMarks;
    }

    public String getSectionsJson() {
        return sectionsJson;
    }

    public void setSectionsJson(String sectionsJson) {
        this.sectionsJson = (sectionsJson != null) ? sectionsJson : "[]";
    }

    public long getTotalDurationMillis() {
        return totalDurationMinutes * 60L * 1000L;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExamProfile that = (ExamProfile) o;
        return id == that.id &&
                totalDurationMinutes == that.totalDurationMinutes &&
                totalQuestions == that.totalQuestions &&
                Double.compare(that.correctMarks, correctMarks) == 0 &&
                Double.compare(that.negativeMarks, negativeMarks) == 0 &&
                Objects.equals(name, that.name) &&
                Objects.equals(sectionsJson, that.sectionsJson);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, totalDurationMinutes, totalQuestions, correctMarks, negativeMarks, sectionsJson);
    }

    @Override
    public String toString() {
        return "ExamProfile{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", durationMin=" + totalDurationMinutes +
                ", questions=" + totalQuestions +
                ", +marks=" + correctMarks +
                ", -marks=" + negativeMarks +
                '}';
    }
}

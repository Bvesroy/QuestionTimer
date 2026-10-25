package com.example.timerapp.data.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Encapsulates user practice goals and thresholds:
 * - target_time_per_question_sec (e.g., 120 seconds for JEE Advanced)
 * - target_session_duration_min (e.g., 60 minutes)
 * - target_session_questions (e.g., 50 questions)
 * - target_date (default '2027-01-20')
 * - two_zone_enabled preference
 * - wallpaper preferences
 * - widget settings
 */
public class TargetSettings implements Serializable {
    private static final long serialVersionUID = 2L;

    public static final int DEFAULT_TIME_PER_QUESTION_SEC = 120; // 2 minutes
    public static final int DEFAULT_SESSION_DURATION_MIN = 60;   // 1 hour
    public static final int DEFAULT_SESSION_QUESTIONS = 50;      // 50 questions
    public static final String DEFAULT_TARGET_DATE = "2027-01-20";

    private int targetTimePerQuestionSec;
    private int targetSessionDurationMin;
    private int targetSessionQuestions;

    // Version 4 extended fields
    private String targetDate;
    private boolean twoZoneEnabled;
    private WallpaperPreferences wallpaperPreferences;
    private WidgetSettings widgetSettings;

    public TargetSettings() {
        this.targetTimePerQuestionSec = DEFAULT_TIME_PER_QUESTION_SEC;
        this.targetSessionDurationMin = DEFAULT_SESSION_DURATION_MIN;
        this.targetSessionQuestions = DEFAULT_SESSION_QUESTIONS;
        this.targetDate = DEFAULT_TARGET_DATE;
        this.twoZoneEnabled = false;
        this.wallpaperPreferences = new WallpaperPreferences();
        this.widgetSettings = new WidgetSettings();
    }

    public TargetSettings(int targetTimePerQuestionSec, int targetSessionDurationMin, int targetSessionQuestions) {
        this.targetTimePerQuestionSec = Math.max(1, targetTimePerQuestionSec);
        this.targetSessionDurationMin = Math.max(1, targetSessionDurationMin);
        this.targetSessionQuestions = Math.max(1, targetSessionQuestions);
        this.targetDate = DEFAULT_TARGET_DATE;
        this.twoZoneEnabled = false;
        this.wallpaperPreferences = new WallpaperPreferences();
        this.widgetSettings = new WidgetSettings();
    }

    public TargetSettings(int targetTimePerQuestionSec, int targetSessionDurationMin, int targetSessionQuestions,
                          String targetDate, boolean twoZoneEnabled) {
        this.targetTimePerQuestionSec = Math.max(1, targetTimePerQuestionSec);
        this.targetSessionDurationMin = Math.max(1, targetSessionDurationMin);
        this.targetSessionQuestions = Math.max(1, targetSessionQuestions);
        this.targetDate = (targetDate != null && !targetDate.trim().isEmpty()) ? targetDate.trim() : DEFAULT_TARGET_DATE;
        this.twoZoneEnabled = twoZoneEnabled;
        this.wallpaperPreferences = new WallpaperPreferences();
        this.widgetSettings = new WidgetSettings();
    }

    public TargetSettings(int targetTimePerQuestionSec, int targetSessionDurationMin, int targetSessionQuestions,
                          String targetDate, boolean twoZoneEnabled,
                          WallpaperPreferences wallpaperPreferences, WidgetSettings widgetSettings) {
        this.targetTimePerQuestionSec = Math.max(1, targetTimePerQuestionSec);
        this.targetSessionDurationMin = Math.max(1, targetSessionDurationMin);
        this.targetSessionQuestions = Math.max(1, targetSessionQuestions);
        this.targetDate = (targetDate != null && !targetDate.trim().isEmpty()) ? targetDate.trim() : DEFAULT_TARGET_DATE;
        this.twoZoneEnabled = twoZoneEnabled;
        this.wallpaperPreferences = (wallpaperPreferences != null) ? wallpaperPreferences : new WallpaperPreferences();
        this.widgetSettings = (widgetSettings != null) ? widgetSettings : new WidgetSettings();
    }

    public int getTargetTimePerQuestionSec() {
        return targetTimePerQuestionSec;
    }

    public void setTargetTimePerQuestionSec(int targetTimePerQuestionSec) {
        this.targetTimePerQuestionSec = Math.max(1, targetTimePerQuestionSec);
    }

    public int getTargetSessionDurationMin() {
        return targetSessionDurationMin;
    }

    public void setTargetSessionDurationMin(int targetSessionDurationMin) {
        this.targetSessionDurationMin = Math.max(1, targetSessionDurationMin);
    }

    public int getTargetSessionQuestions() {
        return targetSessionQuestions;
    }

    public void setTargetSessionQuestions(int targetSessionQuestions) {
        this.targetSessionQuestions = Math.max(1, targetSessionQuestions);
    }

    public String getTargetDate() {
        return targetDate != null ? targetDate : DEFAULT_TARGET_DATE;
    }

    public void setTargetDate(String targetDate) {
        this.targetDate = (targetDate != null && !targetDate.trim().isEmpty()) ? targetDate.trim() : DEFAULT_TARGET_DATE;
    }

    public boolean isTwoZoneEnabled() {
        return twoZoneEnabled;
    }

    public void setTwoZoneEnabled(boolean twoZoneEnabled) {
        this.twoZoneEnabled = twoZoneEnabled;
    }

    public WallpaperPreferences getWallpaperPreferences() {
        if (wallpaperPreferences == null) {
            wallpaperPreferences = new WallpaperPreferences();
        }
        return wallpaperPreferences;
    }

    public void setWallpaperPreferences(WallpaperPreferences wallpaperPreferences) {
        this.wallpaperPreferences = (wallpaperPreferences != null) ? wallpaperPreferences : new WallpaperPreferences();
    }

    public WidgetSettings getWidgetSettings() {
        if (widgetSettings == null) {
            widgetSettings = new WidgetSettings();
        }
        return widgetSettings;
    }

    public void setWidgetSettings(WidgetSettings widgetSettings) {
        this.widgetSettings = (widgetSettings != null) ? widgetSettings : new WidgetSettings();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TargetSettings that = (TargetSettings) o;
        return targetTimePerQuestionSec == that.targetTimePerQuestionSec &&
                targetSessionDurationMin == that.targetSessionDurationMin &&
                targetSessionQuestions == that.targetSessionQuestions &&
                twoZoneEnabled == that.twoZoneEnabled &&
                Objects.equals(getTargetDate(), that.getTargetDate()) &&
                Objects.equals(getWallpaperPreferences(), that.getWallpaperPreferences()) &&
                Objects.equals(getWidgetSettings(), that.getWidgetSettings());
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetTimePerQuestionSec, targetSessionDurationMin, targetSessionQuestions,
                getTargetDate(), twoZoneEnabled, getWallpaperPreferences(), getWidgetSettings());
    }

    @Override
    public String toString() {
        return "TargetSettings{" +
                "targetTimePerQuestionSec=" + targetTimePerQuestionSec +
                ", targetSessionDurationMin=" + targetSessionDurationMin +
                ", targetSessionQuestions=" + targetSessionQuestions +
                ", targetDate='" + getTargetDate() + '\'' +
                ", twoZoneEnabled=" + twoZoneEnabled +
                ", wallpaperPreferences=" + getWallpaperPreferences() +
                ", widgetSettings=" + getWidgetSettings() +
                '}';
    }
}

package com.example.timerapp.data.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Objects;

/**
 * Encapsulates Android home screen and lock screen widget configuration settings.
 */
public class WidgetSettings implements Serializable {
    private static final long serialVersionUID = 1L;

    private String theme;
    private boolean showCountdown;
    private boolean showDailyGoal;
    private float opacity;
    private int refreshIntervalMinutes;

    public WidgetSettings() {
        this.theme = "AMOLED";
        this.showCountdown = true;
        this.showDailyGoal = true;
        this.opacity = 1.0f;
        this.refreshIntervalMinutes = 15;
    }

    public WidgetSettings(String theme, boolean showCountdown, boolean showDailyGoal, float opacity, int refreshIntervalMinutes) {
        this.theme = (theme != null) ? theme : "AMOLED";
        this.showCountdown = showCountdown;
        this.showDailyGoal = showDailyGoal;
        this.opacity = Math.max(0.0f, Math.min(1.0f, opacity));
        this.refreshIntervalMinutes = Math.max(1, refreshIntervalMinutes);
    }

    public String getTheme() {
        return theme != null ? theme : "AMOLED";
    }

    public void setTheme(String theme) {
        this.theme = (theme != null) ? theme : "AMOLED";
    }

    public boolean isShowCountdown() {
        return showCountdown;
    }

    public void setShowCountdown(boolean showCountdown) {
        this.showCountdown = showCountdown;
    }

    public boolean isShowDailyGoal() {
        return showDailyGoal;
    }

    public void setShowDailyGoal(boolean showDailyGoal) {
        this.showDailyGoal = showDailyGoal;
    }

    public float getOpacity() {
        return opacity;
    }

    public void setOpacity(float opacity) {
        this.opacity = Math.max(0.0f, Math.min(1.0f, opacity));
    }

    public int getRefreshIntervalMinutes() {
        return refreshIntervalMinutes;
    }

    public void setRefreshIntervalMinutes(int refreshIntervalMinutes) {
        this.refreshIntervalMinutes = Math.max(1, refreshIntervalMinutes);
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("theme", getTheme());
            obj.put("show_countdown", showCountdown);
            obj.put("show_daily_goal", showDailyGoal);
            obj.put("opacity", (double) opacity);
            obj.put("refresh_interval_minutes", refreshIntervalMinutes);
        } catch (JSONException ignored) {
        }
        return obj;
    }

    public static WidgetSettings fromJson(JSONObject json) {
        if (json == null) return new WidgetSettings();
        String theme = json.optString("theme", "AMOLED");
        boolean showCountdown = json.optBoolean("show_countdown", true);
        boolean showDailyGoal = json.optBoolean("show_daily_goal", true);
        float opacity = (float) json.optDouble("opacity", 1.0);
        int refreshIntervalMinutes = json.optInt("refresh_interval_minutes", 15);
        return new WidgetSettings(theme, showCountdown, showDailyGoal, opacity, refreshIntervalMinutes);
    }

    public static WidgetSettings fromJsonString(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return new WidgetSettings();
        }
        try {
            return fromJson(new JSONObject(jsonStr));
        } catch (JSONException e) {
            return new WidgetSettings();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WidgetSettings that = (WidgetSettings) o;
        return showCountdown == that.showCountdown &&
                showDailyGoal == that.showDailyGoal &&
                Float.compare(that.opacity, opacity) == 0 &&
                refreshIntervalMinutes == that.refreshIntervalMinutes &&
                Objects.equals(theme, that.theme);
    }

    @Override
    public int hashCode() {
        return Objects.hash(theme, showCountdown, showDailyGoal, opacity, refreshIntervalMinutes);
    }

    @Override
    public String toString() {
        return "WidgetSettings{" +
                "theme='" + theme + '\'' +
                ", showCountdown=" + showCountdown +
                ", showDailyGoal=" + showDailyGoal +
                ", opacity=" + opacity +
                ", refreshIntervalMinutes=" + refreshIntervalMinutes +
                '}';
    }
}

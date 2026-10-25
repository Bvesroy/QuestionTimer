package com.example.timerapp.data.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Objects;

/**
 * Encapsulates user custom background wallpaper preferences.
 */
public class WallpaperPreferences implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean enabled;
    private String uri;
    private float dimAlpha;
    private int blurRadius;
    private String style;

    public WallpaperPreferences() {
        this.enabled = false;
        this.uri = "";
        this.dimAlpha = 0.5f;
        this.blurRadius = 0;
        this.style = "DEFAULT";
    }

    public WallpaperPreferences(boolean enabled, String uri, float dimAlpha, int blurRadius, String style) {
        this.enabled = enabled;
        this.uri = (uri != null) ? uri : "";
        this.dimAlpha = Math.max(0.0f, Math.min(1.0f, dimAlpha));
        this.blurRadius = Math.max(0, blurRadius);
        this.style = (style != null) ? style : "DEFAULT";
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUri() {
        return uri != null ? uri : "";
    }

    public void setUri(String uri) {
        this.uri = (uri != null) ? uri : "";
    }

    public float getDimAlpha() {
        return dimAlpha;
    }

    public void setDimAlpha(float dimAlpha) {
        this.dimAlpha = Math.max(0.0f, Math.min(1.0f, dimAlpha));
    }

    public int getBlurRadius() {
        return blurRadius;
    }

    public void setBlurRadius(int blurRadius) {
        this.blurRadius = Math.max(0, blurRadius);
    }

    public String getStyle() {
        return style != null ? style : "DEFAULT";
    }

    public void setStyle(String style) {
        this.style = (style != null) ? style : "DEFAULT";
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("enabled", enabled);
            obj.put("uri", getUri());
            obj.put("dim_alpha", (double) dimAlpha);
            obj.put("blur_radius", blurRadius);
            obj.put("style", getStyle());
        } catch (JSONException ignored) {
        }
        return obj;
    }

    public static WallpaperPreferences fromJson(JSONObject json) {
        if (json == null) return new WallpaperPreferences();
        boolean enabled = json.optBoolean("enabled", false);
        String uri = json.optString("uri", json.optString("path", ""));
        float dimAlpha = (float) json.optDouble("dim_alpha", json.optDouble("dim", 0.5));
        int blur = json.optInt("blur_radius", json.optInt("blur", 0));
        String style = json.optString("style", "DEFAULT");
        return new WallpaperPreferences(enabled, uri, dimAlpha, blur, style);
    }

    public static WallpaperPreferences fromJsonString(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return new WallpaperPreferences();
        }
        try {
            return fromJson(new JSONObject(jsonStr));
        } catch (JSONException e) {
            return new WallpaperPreferences();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WallpaperPreferences that = (WallpaperPreferences) o;
        return enabled == that.enabled &&
                Float.compare(that.dimAlpha, dimAlpha) == 0 &&
                blurRadius == that.blurRadius &&
                Objects.equals(uri, that.uri) &&
                Objects.equals(style, that.style);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, uri, dimAlpha, blurRadius, style);
    }

    @Override
    public String toString() {
        return "WallpaperPreferences{" +
                "enabled=" + enabled +
                ", uri='" + uri + '\'' +
                ", dimAlpha=" + dimAlpha +
                ", blurRadius=" + blurRadius +
                ", style='" + style + '\'' +
                '}';
    }
}

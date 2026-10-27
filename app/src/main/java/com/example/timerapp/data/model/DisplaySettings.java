package com.example.timerapp.data.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Encapsulates display, layout decluttering, corner positioning,
 * and end-timer effect preferences for QuestionTimer.
 */
public class DisplaySettings implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String PREFS_NAME = "question_timer_prefs";
    public static final String KEY_SHOW_STATUS_HINTS = "show_status_hints";
    public static final String KEY_CLEAN_COUNTING_MODE = "clean_counting_mode";
    public static final String KEY_STUDY_TIMER_POSITION = "study_timer_position";
    public static final String KEY_TIMER_FONT = "timer_font_family";
    public static final String KEY_END_TIMER_EFFECT = "end_timer_effect";
    public static final String KEY_END_TIMER_TRANSITION = "end_timer_transition";
    public static final String KEY_END_TIMER_DURATION_MS = "end_timer_duration_ms";
    public static final String KEY_END_TIMER_TEXT_STYLE = "end_timer_text_style";
    public static final String KEY_EXAM_BANNER_ENABLED = "exam_banner_enabled";

    // Two-Zone Click Feedback Effect Keys & Constants
    public static final String KEY_ZONE_EFFECT_ENABLED = "zone_click_effect_enabled";
    public static final String KEY_ZONE_EFFECT_DURATION_MS = "zone_click_effect_duration_ms";
    public static final String KEY_ZONE_EFFECT_STYLE = "zone_click_effect_style";

    public static final String EFFECT_STYLE_FADE = "FADE_OUT";
    public static final String EFFECT_STYLE_SCALE = "RIPPLE_SCALE";
    public static final String EFFECT_STYLE_POP = "INSTANT_POP";

    // Corner positioning constants
    public static final String POS_BOTTOM_LEFT = "BOTTOM_LEFT";
    public static final String POS_BOTTOM_RIGHT = "BOTTOM_RIGHT";
    public static final String POS_TOP_LEFT = "TOP_LEFT";
    public static final String POS_TOP_RIGHT = "TOP_RIGHT";
    public static final String POS_BOTTOM_CENTER = "BOTTOM_CENTER";
    public static final String POS_CENTER = "CENTER";
    public static final String POS_HIDDEN = "HIDDEN";

    // Timer font family constants
    public static final String FONT_MONOSPACE = "MONOSPACE";
    public static final String FONT_SANS_SERIF_BOLD = "SANS_SERIF_BOLD";
    public static final String FONT_CONDENSED = "CONDENSED";
    public static final String FONT_SERIF = "SERIF";
    public static final String FONT_LIGHT = "LIGHT";

    // End timer visual effect constants
    public static final String EFFECT_GLOW_PULSE = "GLOW_PULSE";
    public static final String EFFECT_FLASH = "FLASH";
    public static final String EFFECT_SUBTLE_BOUNCE = "SUBTLE_BOUNCE";
    public static final String EFFECT_NONE = "NONE";

    // End timer transition constants
    public static final String TRANSITION_SMOOTH_FADE = "SMOOTH_FADE";
    public static final String TRANSITION_SCALE_POP = "SCALE_POP";
    public static final String TRANSITION_SLIDE_DOWN = "SLIDE_DOWN";
    public static final String TRANSITION_INSTANT = "INSTANT";

    // End timer text style constants
    public static final String TEXT_STYLE_STANDARD = "STANDARD";
    public static final String TEXT_STYLE_JEE_FOCUS = "JEE_FOCUS";
    public static final String TEXT_STYLE_MINIMAL = "MINIMAL";
    public static final String TEXT_STYLE_ENCOURAGING = "ENCOURAGING";

    private boolean showStatusHints;
    private boolean cleanCountingMode;
    private String studyTimerPosition;
    private String timerFontFamily;
    private String endTimerEffect;
    private String endTimerTransition;
    private int endTimerDurationMs;
    private String endTimerTextStyle;
    private boolean examBannerEnabled;
    private boolean zoneEffectEnabled;
    private int zoneEffectDurationMs;
    private String zoneEffectStyle;

    public DisplaySettings() {
        this.showStatusHints = true;
        this.cleanCountingMode = true;
        this.studyTimerPosition = POS_BOTTOM_LEFT;
        this.timerFontFamily = FONT_MONOSPACE;
        this.endTimerEffect = EFFECT_GLOW_PULSE;
        this.endTimerTransition = TRANSITION_SMOOTH_FADE;
        this.endTimerDurationMs = 250;
        this.endTimerTextStyle = TEXT_STYLE_STANDARD;
        this.examBannerEnabled = true;
        this.zoneEffectEnabled = true;
        this.zoneEffectDurationMs = 300;
        this.zoneEffectStyle = EFFECT_STYLE_FADE;
    }

    public DisplaySettings(boolean showStatusHints, boolean cleanCountingMode,
                           String studyTimerPosition, String timerFontFamily,
                           String endTimerEffect, String endTimerTransition,
                           String endTimerTextStyle, boolean examBannerEnabled) {
        this(showStatusHints, cleanCountingMode, studyTimerPosition, timerFontFamily,
             endTimerEffect, endTimerTransition, 250, endTimerTextStyle, examBannerEnabled,
             true, 300, EFFECT_STYLE_FADE);
    }

    public DisplaySettings(boolean showStatusHints, boolean cleanCountingMode,
                           String studyTimerPosition, String timerFontFamily,
                           String endTimerEffect, String endTimerTransition,
                           String endTimerTextStyle, boolean examBannerEnabled,
                           boolean zoneEffectEnabled, int zoneEffectDurationMs,
                           String zoneEffectStyle) {
        this(showStatusHints, cleanCountingMode, studyTimerPosition, timerFontFamily,
             endTimerEffect, endTimerTransition, 250, endTimerTextStyle, examBannerEnabled,
             zoneEffectEnabled, zoneEffectDurationMs, zoneEffectStyle);
    }

    public DisplaySettings(boolean showStatusHints, boolean cleanCountingMode,
                           String studyTimerPosition, String timerFontFamily,
                           String endTimerEffect, String endTimerTransition,
                           int endTimerDurationMs,
                           String endTimerTextStyle, boolean examBannerEnabled,
                           boolean zoneEffectEnabled, int zoneEffectDurationMs,
                           String zoneEffectStyle) {
        this.showStatusHints = showStatusHints;
        this.cleanCountingMode = cleanCountingMode;
        this.studyTimerPosition = (studyTimerPosition != null) ? studyTimerPosition : POS_BOTTOM_LEFT;
        this.timerFontFamily = (timerFontFamily != null) ? timerFontFamily : FONT_MONOSPACE;
        this.endTimerEffect = (endTimerEffect != null) ? endTimerEffect : EFFECT_GLOW_PULSE;
        this.endTimerTransition = (endTimerTransition != null) ? endTimerTransition : TRANSITION_SMOOTH_FADE;
        this.endTimerDurationMs = (endTimerDurationMs > 0) ? endTimerDurationMs : 250;
        this.endTimerTextStyle = (endTimerTextStyle != null) ? endTimerTextStyle : TEXT_STYLE_STANDARD;
        this.examBannerEnabled = examBannerEnabled;
        this.zoneEffectEnabled = zoneEffectEnabled;
        this.zoneEffectDurationMs = (zoneEffectDurationMs > 0) ? zoneEffectDurationMs : 300;
        this.zoneEffectStyle = (zoneEffectStyle != null) ? zoneEffectStyle : EFFECT_STYLE_FADE;
    }

    public boolean isShowStatusHints() {
        return showStatusHints;
    }

    public void setShowStatusHints(boolean showStatusHints) {
        this.showStatusHints = showStatusHints;
    }

    public boolean isCleanCountingMode() {
        return cleanCountingMode;
    }

    public void setCleanCountingMode(boolean cleanCountingMode) {
        this.cleanCountingMode = cleanCountingMode;
    }

    public String getStudyTimerPosition() {
        return studyTimerPosition != null ? studyTimerPosition : POS_BOTTOM_LEFT;
    }

    public void setStudyTimerPosition(String studyTimerPosition) {
        this.studyTimerPosition = (studyTimerPosition != null) ? studyTimerPosition : POS_BOTTOM_LEFT;
    }

    public String getTimerFontFamily() {
        return timerFontFamily != null ? timerFontFamily : FONT_MONOSPACE;
    }

    public void setTimerFontFamily(String timerFontFamily) {
        this.timerFontFamily = (timerFontFamily != null) ? timerFontFamily : FONT_MONOSPACE;
    }

    public String getEndTimerEffect() {
        return endTimerEffect != null ? endTimerEffect : EFFECT_GLOW_PULSE;
    }

    public void setEndTimerEffect(String endTimerEffect) {
        this.endTimerEffect = (endTimerEffect != null) ? endTimerEffect : EFFECT_GLOW_PULSE;
    }

    public String getEndTimerTransition() {
        return endTimerTransition != null ? endTimerTransition : TRANSITION_SMOOTH_FADE;
    }

    public void setEndTimerTransition(String endTimerTransition) {
        this.endTimerTransition = (endTimerTransition != null) ? endTimerTransition : TRANSITION_SMOOTH_FADE;
    }

    public int getEndTimerDurationMs() {
        return endTimerDurationMs;
    }

    public void setEndTimerDurationMs(int endTimerDurationMs) {
        this.endTimerDurationMs = (endTimerDurationMs > 0) ? endTimerDurationMs : 250;
    }

    public String getEndTimerTextStyle() {
        return endTimerTextStyle != null ? endTimerTextStyle : TEXT_STYLE_STANDARD;
    }

    public void setEndTimerTextStyle(String endTimerTextStyle) {
        this.endTimerTextStyle = (endTimerTextStyle != null) ? endTimerTextStyle : TEXT_STYLE_STANDARD;
    }

    public boolean isExamBannerEnabled() {
        return examBannerEnabled;
    }

    public void setExamBannerEnabled(boolean examBannerEnabled) {
        this.examBannerEnabled = examBannerEnabled;
    }

    public boolean isZoneEffectEnabled() {
        return zoneEffectEnabled;
    }

    public void setZoneEffectEnabled(boolean zoneEffectEnabled) {
        this.zoneEffectEnabled = zoneEffectEnabled;
    }

    public int getZoneEffectDurationMs() {
        return zoneEffectDurationMs;
    }

    public void setZoneEffectDurationMs(int zoneEffectDurationMs) {
        this.zoneEffectDurationMs = (zoneEffectDurationMs > 0) ? zoneEffectDurationMs : 300;
    }

    public String getZoneEffectStyle() {
        return zoneEffectStyle != null ? zoneEffectStyle : EFFECT_STYLE_FADE;
    }

    public void setZoneEffectStyle(String zoneEffectStyle) {
        this.zoneEffectStyle = (zoneEffectStyle != null) ? zoneEffectStyle : EFFECT_STYLE_FADE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DisplaySettings that = (DisplaySettings) o;
        return showStatusHints == that.showStatusHints &&
                cleanCountingMode == that.cleanCountingMode &&
                examBannerEnabled == that.examBannerEnabled &&
                Objects.equals(getStudyTimerPosition(), that.getStudyTimerPosition()) &&
                Objects.equals(getTimerFontFamily(), that.getTimerFontFamily()) &&
                Objects.equals(getEndTimerEffect(), that.getEndTimerEffect()) &&
                Objects.equals(getEndTimerTransition(), that.getEndTimerTransition()) &&
                Objects.equals(getEndTimerTextStyle(), that.getEndTimerTextStyle());
    }

    @Override
    public int hashCode() {
        return Objects.hash(showStatusHints, cleanCountingMode, getStudyTimerPosition(),
                getTimerFontFamily(), getEndTimerEffect(), getEndTimerTransition(),
                getEndTimerTextStyle(), examBannerEnabled);
    }
}

package com.example.timerapp.data;

import com.example.timerapp.data.model.DisplaySettings;
import com.example.timerapp.model.QuestionLog;
import com.example.timerapp.model.TimerSession;
import com.example.timerapp.model.TimerState;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit Test Suite for Display, Landscape Decluttering, and End-Timer Customization Settings.
 * Validates:
 * 1. SharedPreferences contract keys and default identifiers.
 * 2. Total Study Clock corner positioning constants.
 * 3. Selectable Timer font family constants.
 * 4. End-Timer transition and visual effect constants.
 * 5. End-Timer text style formatting constants.
 * 6. Exam Countdown Banner and Status Cue Hints toggles.
 * 7. Multi-question study clock continuity without reset across question intervals.
 */
public class DisplayAndLandscapeSettingsTest {

    private TimerSession session;
    private DisplaySettings displaySettings;

    @Before
    public void setUp() {
        session = new TimerSession(201L, "JEE ADVANCED REVISION", 50);
        displaySettings = new DisplaySettings();
    }

    // ==========================================
    // 1. Settings Contract Keys Verification
    // ==========================================

    @Test
    public void testSettingsContractKeys() {
        Assert.assertEquals("show_status_hints", DisplaySettings.KEY_SHOW_STATUS_HINTS);
        Assert.assertEquals("clean_counting_mode", DisplaySettings.KEY_CLEAN_COUNTING_MODE);
        Assert.assertEquals("study_timer_position", DisplaySettings.KEY_STUDY_TIMER_POSITION);
        Assert.assertEquals("timer_font_family", DisplaySettings.KEY_TIMER_FONT);
        Assert.assertEquals("end_timer_effect", DisplaySettings.KEY_END_TIMER_EFFECT);
        Assert.assertEquals("end_timer_transition", DisplaySettings.KEY_END_TIMER_TRANSITION);
        Assert.assertEquals("end_timer_text_style", DisplaySettings.KEY_END_TIMER_TEXT_STYLE);
        Assert.assertEquals("exam_banner_enabled", DisplaySettings.KEY_EXAM_BANNER_ENABLED);
    }

    // ==========================================
    // 2. Corner Placement Constants Verification
    // ==========================================

    @Test
    public void testStudyTimerPositionConstants() {
        Assert.assertEquals("BOTTOM_LEFT", DisplaySettings.POS_BOTTOM_LEFT);
        Assert.assertEquals("BOTTOM_RIGHT", DisplaySettings.POS_BOTTOM_RIGHT);
        Assert.assertEquals("TOP_LEFT", DisplaySettings.POS_TOP_LEFT);
        Assert.assertEquals("TOP_RIGHT", DisplaySettings.POS_TOP_RIGHT);
        Assert.assertEquals("BOTTOM_CENTER", DisplaySettings.POS_BOTTOM_CENTER);
        Assert.assertEquals("CENTER", DisplaySettings.POS_CENTER);
        Assert.assertEquals("HIDDEN", DisplaySettings.POS_HIDDEN);

        // Verify default position is BOTTOM_LEFT corner
        Assert.assertEquals(DisplaySettings.POS_BOTTOM_LEFT, displaySettings.getStudyTimerPosition());
        displaySettings.setStudyTimerPosition(DisplaySettings.POS_BOTTOM_RIGHT);
        Assert.assertEquals(DisplaySettings.POS_BOTTOM_RIGHT, displaySettings.getStudyTimerPosition());
    }

    // ==========================================
    // 3. Timer Font Family Constants Verification
    // ==========================================

    @Test
    public void testTimerFontFamilyConstants() {
        Assert.assertEquals("MONOSPACE", DisplaySettings.FONT_MONOSPACE);
        Assert.assertEquals("SANS_SERIF_BOLD", DisplaySettings.FONT_SANS_SERIF_BOLD);
        Assert.assertEquals("CONDENSED", DisplaySettings.FONT_CONDENSED);
        Assert.assertEquals("SERIF", DisplaySettings.FONT_SERIF);
        Assert.assertEquals("LIGHT", DisplaySettings.FONT_LIGHT);

        // Verify default font is Monospace tabular digital
        Assert.assertEquals(DisplaySettings.FONT_MONOSPACE, displaySettings.getTimerFontFamily());
        displaySettings.setTimerFontFamily(DisplaySettings.FONT_SANS_SERIF_BOLD);
        Assert.assertEquals(DisplaySettings.FONT_SANS_SERIF_BOLD, displaySettings.getTimerFontFamily());
    }

    // ==========================================
    // 4. End Timer Visual Effect & Transitions
    // ==========================================

    @Test
    public void testEndTimerEffectsAndTransitions() {
        Assert.assertEquals("GLOW_PULSE", DisplaySettings.EFFECT_GLOW_PULSE);
        Assert.assertEquals("FLASH", DisplaySettings.EFFECT_FLASH);
        Assert.assertEquals("SUBTLE_BOUNCE", DisplaySettings.EFFECT_SUBTLE_BOUNCE);
        Assert.assertEquals("NONE", DisplaySettings.EFFECT_NONE);

        Assert.assertEquals("SMOOTH_FADE", DisplaySettings.TRANSITION_SMOOTH_FADE);
        Assert.assertEquals("SCALE_POP", DisplaySettings.TRANSITION_SCALE_POP);
        Assert.assertEquals("SLIDE_DOWN", DisplaySettings.TRANSITION_SLIDE_DOWN);
        Assert.assertEquals("INSTANT", DisplaySettings.TRANSITION_INSTANT);

        Assert.assertEquals(DisplaySettings.EFFECT_GLOW_PULSE, displaySettings.getEndTimerEffect());
        Assert.assertEquals(DisplaySettings.TRANSITION_SMOOTH_FADE, displaySettings.getEndTimerTransition());
    }

    // ==========================================
    // 5. End Timer Text Style Constants
    // ==========================================

    @Test
    public void testEndTimerTextStyleConstants() {
        Assert.assertEquals("STANDARD", DisplaySettings.TEXT_STYLE_STANDARD);
        Assert.assertEquals("JEE_FOCUS", DisplaySettings.TEXT_STYLE_JEE_FOCUS);
        Assert.assertEquals("MINIMAL", DisplaySettings.TEXT_STYLE_MINIMAL);
        Assert.assertEquals("ENCOURAGING", DisplaySettings.TEXT_STYLE_ENCOURAGING);

        Assert.assertEquals(DisplaySettings.TEXT_STYLE_STANDARD, displaySettings.getEndTimerTextStyle());
        displaySettings.setEndTimerTextStyle(DisplaySettings.TEXT_STYLE_JEE_FOCUS);
        Assert.assertEquals(DisplaySettings.TEXT_STYLE_JEE_FOCUS, displaySettings.getEndTimerTextStyle());
    }

    // ==========================================
    // 6. Total Study Clock Continuous Timing
    // ==========================================

    @Test
    public void testTotalStudyClockAccumulationAcrossQuestions() {
        // Initial state before study starts
        Assert.assertEquals(0L, session.getTotalStudyDurationMs());
        Assert.assertEquals(1, session.getCurrentQuestion());

        // Question 1 solves in 45 seconds (45,000 ms)
        long q1Duration = 45_000L;
        QuestionLog q1 = new QuestionLog(1001L, session.getId(), 1, q1Duration, System.currentTimeMillis());
        session.addLog(q1);
        session.setTotalStudyDurationMs(q1Duration);

        Assert.assertEquals(2, session.getCurrentQuestion());
        Assert.assertEquals(45_000L, session.getTotalStudyDurationMs());

        // Question 2 solves in 75 seconds (75,000 ms)
        long q2Duration = 75_000L;
        QuestionLog q2 = new QuestionLog(1002L, session.getId(), 2, q2Duration, System.currentTimeMillis());
        session.addLog(q2);
        session.setTotalStudyDurationMs(session.getTotalStudyDurationMs() + q2Duration);

        // Assert question number advanced to 3 while total study clock accumulated to 120,000 ms (2m 00s)
        Assert.assertEquals(3, session.getCurrentQuestion());
        Assert.assertEquals(120_000L, session.getTotalStudyDurationMs());
        Assert.assertEquals(2, session.getCompletedCount());
        Assert.assertEquals(60_000L, session.getAverageDurationMs());
    }

    // ==========================================
    // 7. Clean Counting & Overlap Prevention Logic
    // ==========================================

    @Test
    public void testCleanCountingModeTransitions() {
        // State loop: IDLE -> RUNNING_QUESTION -> PAUSED -> RUNNING_QUESTION
        TimerState state = TimerState.IDLE;
        Assert.assertFalse(state.isRunning());

        // Timer starts: running state hides controls & secondary header
        state = TimerState.RUNNING_QUESTION;
        Assert.assertTrue(state.isRunning());

        // User pauses timer: state is paused, controls and session info restore
        state = TimerState.PAUSED;
        Assert.assertFalse(state.isRunning());
        Assert.assertTrue(state.isPaused());

        Assert.assertTrue(displaySettings.isCleanCountingMode());
        Assert.assertTrue(displaySettings.isShowStatusHints());
        Assert.assertTrue(displaySettings.isExamBannerEnabled());
    }

    // ==========================================
    // 8. Two-Zone Visual Click Effect Settings
    // ==========================================

    @Test
    public void testTwoZoneClickFeedbackSettings_defaultsAndMutations() {
        Assert.assertEquals("zone_click_effect_enabled", DisplaySettings.KEY_ZONE_EFFECT_ENABLED);
        Assert.assertEquals("zone_click_effect_duration_ms", DisplaySettings.KEY_ZONE_EFFECT_DURATION_MS);
        Assert.assertEquals("zone_click_effect_style", DisplaySettings.KEY_ZONE_EFFECT_STYLE);

        // Verify default state: enabled=true, duration=300ms, style=FADE_OUT
        Assert.assertTrue(displaySettings.isZoneEffectEnabled());
        Assert.assertEquals(300, displaySettings.getZoneEffectDurationMs());
        Assert.assertEquals(DisplaySettings.EFFECT_STYLE_FADE, displaySettings.getZoneEffectStyle());

        // Test mutations
        displaySettings.setZoneEffectEnabled(false);
        Assert.assertFalse(displaySettings.isZoneEffectEnabled());

        displaySettings.setZoneEffectDurationMs(500);
        Assert.assertEquals(500, displaySettings.getZoneEffectDurationMs());

        displaySettings.setZoneEffectDurationMs(800);
        Assert.assertEquals(800, displaySettings.getZoneEffectDurationMs());

        displaySettings.setZoneEffectStyle(DisplaySettings.EFFECT_STYLE_SCALE);
        Assert.assertEquals("RIPPLE_SCALE", displaySettings.getZoneEffectStyle());

        displaySettings.setZoneEffectStyle(DisplaySettings.EFFECT_STYLE_POP);
        Assert.assertEquals("INSTANT_POP", displaySettings.getZoneEffectStyle());
    }

    @Test
    public void testTwoZoneClickFeedbackStyleConstants() {
        Assert.assertEquals("FADE_OUT", DisplaySettings.EFFECT_STYLE_FADE);
        Assert.assertEquals("RIPPLE_SCALE", DisplaySettings.EFFECT_STYLE_SCALE);
        Assert.assertEquals("INSTANT_POP", DisplaySettings.EFFECT_STYLE_POP);
    }

    // ==========================================
    // 9. End Timer Duration & Transition Settings
    // ==========================================

    @Test
    public void testEndTimerDurationSettings_defaultsAndMutations() {
        Assert.assertEquals("end_timer_duration_ms", DisplaySettings.KEY_END_TIMER_DURATION_MS);

        // Verify default duration is 250ms
        Assert.assertEquals(250, displaySettings.getEndTimerDurationMs());

        // Test mutations
        displaySettings.setEndTimerDurationMs(150);
        Assert.assertEquals(150, displaySettings.getEndTimerDurationMs());

        displaySettings.setEndTimerDurationMs(400);
        Assert.assertEquals(400, displaySettings.getEndTimerDurationMs());

        displaySettings.setEndTimerDurationMs(600);
        Assert.assertEquals(600, displaySettings.getEndTimerDurationMs());

        displaySettings.setEndTimerDurationMs(1000);
        Assert.assertEquals(1000, displaySettings.getEndTimerDurationMs());

        // Guard against zero / negative duration
        displaySettings.setEndTimerDurationMs(0);
        Assert.assertEquals(250, displaySettings.getEndTimerDurationMs());
    }

    @Test
    public void testFullConstructorWithEndTimerDuration() {
        DisplaySettings custom = new DisplaySettings(
                false, false, DisplaySettings.POS_TOP_RIGHT, DisplaySettings.FONT_CONDENSED,
                DisplaySettings.EFFECT_FLASH, DisplaySettings.TRANSITION_SCALE_POP,
                400, DisplaySettings.TEXT_STYLE_JEE_FOCUS, false,
                true, 500, DisplaySettings.EFFECT_STYLE_SCALE
        );

        Assert.assertFalse(custom.isShowStatusHints());
        Assert.assertFalse(custom.isCleanCountingMode());
        Assert.assertEquals(DisplaySettings.POS_TOP_RIGHT, custom.getStudyTimerPosition());
        Assert.assertEquals(DisplaySettings.FONT_CONDENSED, custom.getTimerFontFamily());
        Assert.assertEquals(DisplaySettings.EFFECT_FLASH, custom.getEndTimerEffect());
        Assert.assertEquals(DisplaySettings.TRANSITION_SCALE_POP, custom.getEndTimerTransition());
        Assert.assertEquals(400, custom.getEndTimerDurationMs());
        Assert.assertEquals(DisplaySettings.TEXT_STYLE_JEE_FOCUS, custom.getEndTimerTextStyle());
        Assert.assertFalse(custom.isExamBannerEnabled());
        Assert.assertTrue(custom.isZoneEffectEnabled());
        Assert.assertEquals(500, custom.getZoneEffectDurationMs());
        Assert.assertEquals(DisplaySettings.EFFECT_STYLE_SCALE, custom.getZoneEffectStyle());
    }
}

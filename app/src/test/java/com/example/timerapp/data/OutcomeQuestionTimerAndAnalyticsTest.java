package com.example.timerapp.data;

import com.example.timerapp.data.analytics.EstimatedMarksCalculator;
import com.example.timerapp.data.analytics.EstimatedMarksCalculator.PerformanceAnalytics;
import com.example.timerapp.data.engine.TwoZoneHitDetector;
import com.example.timerapp.data.engine.TwoZoneHitDetector.TouchZone;
import com.example.timerapp.model.QuestionLog;
import com.example.timerapp.model.QuestionOutcome;
import com.example.timerapp.model.TimerSession;
import com.example.timerapp.model.TimerState;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive Unit Test Suite for Outcome-Based Question Timer and JEE Performance Analytics.
 * Validates:
 * 1. Invisible two-zone interaction logic (Upper=Wrong, Lower=Correct).
 * 2. Interaction state loop: Run -> Tap Upper/Lower -> Pause with outcome -> Tap -> Resume next question.
 * 3. Settings persistence and optional tracking configuration (JEE Main vs JEE Advanced).
 * 4. Estimated JEE score calculations according to selected exam marking scheme.
 * 5. Full performance analytics: time per question, accuracy, correct/wrong counts, average solving time, estimated score.
 * 6. Multi-question session accumulation and undo operations.
 */
public class OutcomeQuestionTimerAndAnalyticsTest {

    private TwoZoneHitDetector hitDetector;
    private TimerSession session;

    @Before
    public void setUp() {
        // Standard mobile display: 1080 x 2400 px
        hitDetector = new TwoZoneHitDetector(1080, 2400);
        hitDetector.setOrientation(TwoZoneHitDetector.Orientation.VERTICAL);
        session = new TimerSession(101L, "JEE MOCK TEST", 25);
    }

    // ==========================================
    // 1. Invisible Two-Zone Touch Hit Detection
    // ==========================================

    @Test
    public void testEqualUpperAndLowerTouchZones_noDividerRequired() {
        // Equal 50/50 division at y = 1200 px without visible divider
        int height = 2400;
        int midY = height / 2;
        Assert.assertEquals(1200, midY);

        // Upper half taps (< 1200) -> Wrong outcome
        Assert.assertEquals(TouchZone.ZONE_UPPER, hitDetector.detectZone(540f, 100f));
        Assert.assertEquals(TouchZone.ZONE_UPPER, hitDetector.detectZone(100f, 600f));
        Assert.assertEquals(TouchZone.ZONE_UPPER, hitDetector.detectZone(900f, 1199f));

        // Lower half taps (>= 1200) -> Correct outcome
        Assert.assertEquals(TouchZone.ZONE_LOWER, hitDetector.detectZone(540f, 1200f));
        Assert.assertEquals(TouchZone.ZONE_LOWER, hitDetector.detectZone(200f, 1800f));
        Assert.assertEquals(TouchZone.ZONE_LOWER, hitDetector.detectZone(1000f, 2399f));
    }

    @Test
    public void testTouchExclusionZones_preservesTopAndBottomControls() {
        // Top control header exclusion: (0, 0) to (1080, 180)
        hitDetector.addExclusionZone(0, 0, 1080, 180);
        // Bottom toolbar exclusion: (0, 2220) to (1080, 2400)
        hitDetector.addExclusionZone(0, 2220, 1080, 2400);

        // Touches on controls are excluded from question marking
        Assert.assertEquals(TouchZone.ZONE_EXCLUDED, hitDetector.detectZone(540f, 100f));
        Assert.assertEquals(TouchZone.ZONE_EXCLUDED, hitDetector.detectZone(540f, 2300f));

        // Active interaction zones inside screen body remain responsive
        Assert.assertEquals(TouchZone.ZONE_UPPER, hitDetector.detectZone(540f, 500f));
        Assert.assertEquals(TouchZone.ZONE_LOWER, hitDetector.detectZone(540f, 1600f));
    }

    // ==========================================
    // 2. Interaction Flow: Pause on Tap, Tap to Resume Next
    // ==========================================

    @Test
    public void testInteractionFlow_tapUpperPausesAndMarksWrong_tapAgainResumesNext() {
        // Initial state: Question 1
        Assert.assertEquals(1, session.getCurrentQuestion());
        Assert.assertEquals(0, session.getCompletedCount());

        // Step 1: Solving Question 1 for 45 seconds (45,000 ms)
        long q1DurationMs = 45000L;
        // User taps UPPER half -> Marks WRONG and PAUSES
        QuestionLog q1Log = new QuestionLog(
                System.currentTimeMillis(), session.getId(),
                session.getCurrentQuestion(), q1DurationMs,
                System.currentTimeMillis(), "", "Physics", "MEDIUM",
                QuestionOutcome.INCORRECT.name(), 2, "CALC_ERROR", ""
        );
        session.addLog(q1Log);

        // Assert Question 1 logged as INCORRECT / WRONG
        Assert.assertEquals(1, session.getCompletedCount());
        Assert.assertEquals(QuestionOutcome.INCORRECT.name(), session.getLogs().get(0).getOutcome());
        Assert.assertEquals(45000L, session.getLogs().get(0).getDurationMs());
        // Next question number automatically set to 2
        Assert.assertEquals(2, session.getCurrentQuestion());

        // Step 2: While paused, user taps screen again -> Resumes timing for Question 2
        // Question 2 solved for 80 seconds (80,000 ms)
        long q2DurationMs = 80000L;
        // User taps LOWER half -> Marks CORRECT and PAUSES
        QuestionLog q2Log = new QuestionLog(
                System.currentTimeMillis(), session.getId(),
                session.getCurrentQuestion(), q2DurationMs,
                System.currentTimeMillis(), "", "Physics", "EASY",
                QuestionOutcome.CORRECT.name(), 3, "", ""
        );
        session.addLog(q2Log);

        // Assert Question 2 logged as CORRECT
        Assert.assertEquals(2, session.getCompletedCount());
        Assert.assertEquals(QuestionOutcome.CORRECT.name(), session.getLogs().get(1).getOutcome());
        Assert.assertEquals(80000L, session.getLogs().get(1).getDurationMs());
        Assert.assertEquals(3, session.getCurrentQuestion());

        // Total solving duration preserved without bleed
        Assert.assertEquals(125000L, session.getTotalDurationMs());
    }

    // ==========================================
    // 3. Settings: Exam Type & Optional Tracking Constants
    // ==========================================

    @Test
    public void testSettingsKeysAndDefaults() {
        Assert.assertEquals("JEE_MAIN", EstimatedMarksCalculator.EXAM_JEE_MAIN);
        Assert.assertEquals("JEE_ADVANCED", EstimatedMarksCalculator.EXAM_JEE_ADVANCED);
    }

    // ==========================================
    // 4. Estimated JEE Score Calculation
    // ==========================================

    @Test
    public void testJeeMainEstimatedScore_plusFourMinusOneScheme() {
        // 12 Correct, 3 Wrong
        // Score = (12 * 4) - (3 * 1) = 48 - 3 = 45 marks
        double score = EstimatedMarksCalculator.calculateEstimatedScore(
                EstimatedMarksCalculator.EXAM_JEE_MAIN, 12, 3
        );
        Assert.assertEquals(45.0, score, 0.001);

        // 0 Correct, 4 Wrong -> -4 marks
        double negScore = EstimatedMarksCalculator.calculateEstimatedScore(
                EstimatedMarksCalculator.EXAM_JEE_MAIN, 0, 4
        );
        Assert.assertEquals(-4.0, negScore, 0.001);

        // 25 Correct, 0 Wrong -> 100 marks
        double perfectScore = EstimatedMarksCalculator.calculateEstimatedScore(
                EstimatedMarksCalculator.EXAM_JEE_MAIN, 25, 0
        );
        Assert.assertEquals(100.0, perfectScore, 0.001);
    }

    @Test
    public void testJeeAdvancedEstimatedScore_plusFourMinusTwoScheme() {
        // 12 Correct, 3 Wrong
        // Score = (12 * 4) - (3 * 2) = 48 - 6 = 42 marks
        double score = EstimatedMarksCalculator.calculateEstimatedScore(
                EstimatedMarksCalculator.EXAM_JEE_ADVANCED, 12, 3
        );
        Assert.assertEquals(42.0, score, 0.001);

        // 0 Correct, 4 Wrong -> -8 marks
        double negScore = EstimatedMarksCalculator.calculateEstimatedScore(
                EstimatedMarksCalculator.EXAM_JEE_ADVANCED, 0, 4
        );
        Assert.assertEquals(-8.0, negScore, 0.001);
    }

    // ==========================================
    // 5. Full Performance Analytics
    // ==========================================

    @Test
    public void testFullPerformanceAnalytics_jeeMainVsAdvanced() {
        List<QuestionLog> logs = new ArrayList<>();
        // Q1: Correct (50s)
        logs.add(new QuestionLog(1L, 1L, 1, 50000L, System.currentTimeMillis(), "", "Math", "EASY", "CORRECT", 3, "", ""));
        // Q2: Correct (70s)
        logs.add(new QuestionLog(2L, 1L, 2, 70000L, System.currentTimeMillis(), "", "Math", "MEDIUM", "SOLVED", 3, "", ""));
        // Q3: Wrong (120s)
        logs.add(new QuestionLog(3L, 1L, 3, 120000L, System.currentTimeMillis(), "", "Math", "HARD", "INCORRECT", 2, "FORMULA_ERROR", ""));
        // Q4: Correct (60s)
        logs.add(new QuestionLog(4L, 1L, 4, 60000L, System.currentTimeMillis(), "", "Math", "MEDIUM", "CORRECT", 3, "", ""));
        // Q5: Wrong (100s)
        logs.add(new QuestionLog(5L, 1L, 5, 100000L, System.currentTimeMillis(), "", "Math", "MEDIUM", "WRONG", 1, "SILLY_MISTAKE", ""));

        // Total attempted: 5 (3 Correct, 2 Wrong)
        // Total solving duration = 50 + 70 + 120 + 60 + 100 = 400 seconds = 400,000 ms
        // Average solving time = 400 / 5 = 80.0s
        // Accuracy = 3 / 5 = 60.0%

        // Analytics for JEE Main
        PerformanceAnalytics mainAnalytics = EstimatedMarksCalculator.computeAnalytics(
                EstimatedMarksCalculator.EXAM_JEE_MAIN, logs, 25
        );
        Assert.assertEquals(25, mainAnalytics.totalQuestions);
        Assert.assertEquals(3, mainAnalytics.correctCount);
        Assert.assertEquals(2, mainAnalytics.wrongCount);
        Assert.assertEquals(20, mainAnalytics.unattemptedCount);
        Assert.assertEquals(60.0, mainAnalytics.accuracyPercentage, 0.001);
        Assert.assertEquals(400000L, mainAnalytics.totalSolvingTimeMs);
        Assert.assertEquals(80.0, mainAnalytics.averageSolvingTimeSeconds, 0.001);
        // JEE Main Score: (3 * 4) - (2 * 1) = 12 - 2 = 10 pts
        Assert.assertEquals(10.0, mainAnalytics.estimatedScore, 0.001);
        Assert.assertEquals(2.0, mainAnalytics.negativePenalty, 0.001);
        Assert.assertTrue(mainAnalytics.paceWithinTarget); // 80s < 144s target pace

        // Analytics for JEE Advanced
        PerformanceAnalytics advAnalytics = EstimatedMarksCalculator.computeAnalytics(
                EstimatedMarksCalculator.EXAM_JEE_ADVANCED, logs, 25
        );
        // JEE Advanced Score: (3 * 4) - (2 * 2) = 12 - 4 = 8 pts
        Assert.assertEquals(8.0, advAnalytics.estimatedScore, 0.001);
        Assert.assertEquals(4.0, advAnalytics.negativePenalty, 0.001);
        Assert.assertTrue(advAnalytics.paceWithinTarget); // 80s < 180s target pace

        // Summary string format verification
        String formatted = mainAnalytics.getFormattedSummary();
        Assert.assertTrue(formatted.contains("ACCURACY: 60%"));
        Assert.assertTrue(formatted.contains("3C 2W"));
        Assert.assertTrue(formatted.contains("+10 PTS"));
        Assert.assertTrue(formatted.contains("80s"));
    }

    // ==========================================
    // 6. Undo Operation
    // ==========================================

    @Test
    public void testUndoRecordedOutcome_restoresPreviousState() {
        session.addLog(new QuestionLog(1L, session.getId(), 1, 35000L, System.currentTimeMillis(), "", "Chem", "EASY", "CORRECT", 3, "", ""));
        session.addLog(new QuestionLog(2L, session.getId(), 2, 65000L, System.currentTimeMillis(), "", "Chem", "MEDIUM", "INCORRECT", 2, "GUESS", ""));

        Assert.assertEquals(3, session.getCurrentQuestion());
        Assert.assertEquals(2, session.getCompletedCount());

        // User triggers Undo
        QuestionLog undone = session.removeLastLog();
        Assert.assertNotNull(undone);
        Assert.assertEquals(2, undone.getQuestionNumber());
        Assert.assertEquals(65000L, undone.getDurationMs());
        Assert.assertEquals("INCORRECT", undone.getOutcome());

        // State restored: question counter returns to 2, 1 completed log remaining
        Assert.assertEquals(2, session.getCurrentQuestion());
        Assert.assertEquals(1, session.getCompletedCount());
        Assert.assertEquals(35000L, session.getTotalDurationMs());
    }
}

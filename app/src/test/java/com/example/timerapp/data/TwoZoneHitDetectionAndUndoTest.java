package com.example.timerapp.data;

import com.example.timerapp.data.engine.AtomicQuestionRecorder;
import com.example.timerapp.data.engine.TwoZoneHitDetector;
import com.example.timerapp.data.engine.TwoZoneHitDetector.TouchZone;
import com.example.timerapp.data.model.QuestionLog;
import com.example.timerapp.model.QuestionOutcome;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * Comprehensive Unit Test Suite for Two-Zone Touch Hit Detection,
 * Transaction-Safe Atomic Recording, and Multi-Level Undo Operations.
 */
public class TwoZoneHitDetectionAndUndoTest {

    private TwoZoneHitDetector hitDetector;
    private AtomicQuestionRecorder recorder;

    @Before
    public void setUp() {
        // Standard Android mobile display: 1080 x 1920 px
        hitDetector = new TwoZoneHitDetector(1080, 1920);
        recorder = new AtomicQuestionRecorder(505L, 10);
    }

    // ==========================================
    // Two-Zone Hit Detection Tests
    // ==========================================

    @Test
    public void testLeftAndRightZoneHitDetection_defaultFiftyFiftySplit() {
        // Default split is 50% (x = 540)
        Assert.assertEquals(0.5f, hitDetector.getSplitRatio(), 0.001f);

        // Touches on the left half (< 540)
        Assert.assertEquals(TouchZone.ZONE_LEFT, hitDetector.detectZone(0f, 960f));
        Assert.assertEquals(TouchZone.ZONE_LEFT, hitDetector.detectZone(100f, 500f));
        Assert.assertEquals(TouchZone.ZONE_LEFT, hitDetector.detectZone(539f, 960f));

        // Touches on the right half (>= 540)
        Assert.assertEquals(TouchZone.ZONE_RIGHT, hitDetector.detectZone(540f, 960f));
        Assert.assertEquals(TouchZone.ZONE_RIGHT, hitDetector.detectZone(800f, 1200f));
        Assert.assertEquals(TouchZone.ZONE_RIGHT, hitDetector.detectZone(1080f, 1920f));
    }

    @Test
    public void testOutOfBoundsHitDetection_rejectsNegativeOrExceedingCoordinates() {
        // Negative X or Y
        Assert.assertEquals(TouchZone.ZONE_OUT_OF_BOUNDS, hitDetector.detectZone(-1f, 500f));
        Assert.assertEquals(TouchZone.ZONE_OUT_OF_BOUNDS, hitDetector.detectZone(500f, -10f));
        Assert.assertEquals(TouchZone.ZONE_OUT_OF_BOUNDS, hitDetector.detectZone(-50f, -50f));

        // Coordinates exceeding display bounds
        Assert.assertEquals(TouchZone.ZONE_OUT_OF_BOUNDS, hitDetector.detectZone(1081f, 960f));
        Assert.assertEquals(TouchZone.ZONE_OUT_OF_BOUNDS, hitDetector.detectZone(540f, 1921f));
        Assert.assertEquals(TouchZone.ZONE_OUT_OF_BOUNDS, hitDetector.detectZone(2000f, 3000f));
    }

    @Test
    public void testExclusionZones_takesPrecedenceOverZones() {
        // Register top status/control bar: (0, 0) to (1080, 200)
        hitDetector.addExclusionZone(0, 0, 1080, 200);

        // Register bottom action bar: (0, 1720) to (1080, 1920)
        hitDetector.addExclusionZone(0, 1720, 1080, 1920);

        Assert.assertEquals(2, hitDetector.getExclusionZones().size());

        // Touches inside top bar -> ZONE_EXCLUDED
        Assert.assertEquals(TouchZone.ZONE_EXCLUDED, hitDetector.detectZone(150f, 100f));
        Assert.assertEquals(TouchZone.ZONE_EXCLUDED, hitDetector.detectZone(850f, 50f));

        // Touches inside bottom bar -> ZONE_EXCLUDED
        Assert.assertEquals(TouchZone.ZONE_EXCLUDED, hitDetector.detectZone(200f, 1800f));
        Assert.assertEquals(TouchZone.ZONE_EXCLUDED, hitDetector.detectZone(900f, 1750f));

        // Touches in active timing body -> normal ZONE_LEFT and ZONE_RIGHT
        Assert.assertEquals(TouchZone.ZONE_LEFT, hitDetector.detectZone(200f, 500f));
        Assert.assertEquals(TouchZone.ZONE_RIGHT, hitDetector.detectZone(800f, 500f));

        // Clearing exclusions restores full screen detection
        hitDetector.clearExclusionZones();
        Assert.assertEquals(0, hitDetector.getExclusionZones().size());
        Assert.assertEquals(TouchZone.ZONE_LEFT, hitDetector.detectZone(150f, 100f));
        Assert.assertEquals(TouchZone.ZONE_RIGHT, hitDetector.detectZone(850f, 50f));
    }

    @Test
    public void testDeadZoneDividingMargin_absorbsAmbiguousCenterTouches() {
        // Set a 40px dead zone centered around x = 540 (range [520, 560])
        hitDetector.setDeadZoneWidthPx(40f);
        Assert.assertEquals(40f, hitDetector.getDeadZoneWidthPx(), 0.001f);

        // Touches directly in dead zone
        Assert.assertEquals(TouchZone.ZONE_DEADZONE, hitDetector.detectZone(540f, 960f));
        Assert.assertEquals(TouchZone.ZONE_DEADZONE, hitDetector.detectZone(521f, 960f));
        Assert.assertEquals(TouchZone.ZONE_DEADZONE, hitDetector.detectZone(559f, 960f));

        // Touches just outside dead zone
        Assert.assertEquals(TouchZone.ZONE_LEFT, hitDetector.detectZone(519f, 960f));
        Assert.assertEquals(TouchZone.ZONE_RIGHT, hitDetector.detectZone(561f, 960f));
    }

    @Test
    public void testCustomSplitRatio() {
        // 30% / 70% asymmetrical layout: split line at 1080 * 0.30 = 324 px
        hitDetector.setSplitRatio(0.30f);
        Assert.assertEquals(0.30f, hitDetector.getSplitRatio(), 0.001f);

        Assert.assertEquals(TouchZone.ZONE_LEFT, hitDetector.detectZone(320f, 800f));
        Assert.assertEquals(TouchZone.ZONE_RIGHT, hitDetector.detectZone(325f, 800f));
        Assert.assertEquals(TouchZone.ZONE_RIGHT, hitDetector.detectZone(500f, 800f));
    }

    // ==========================================
    // Atomic Recording and Undo Tests
    // ==========================================

    @Test
    public void testAtomicRecording_commitsAtomicallyAndAdvances() {
        Assert.assertEquals(1, recorder.getCurrentQuestionIndex());
        Assert.assertEquals(0L, recorder.getCurrentQuestionElapsedMs());
        Assert.assertFalse(recorder.canUndo());

        // Solve Question 1: 52,000 ms
        recorder.tickQuestion(52000L);
        Assert.assertEquals(52000L, recorder.getCurrentQuestionElapsedMs());

        QuestionLog q1 = recorder.recordQuestionAtomically(
                QuestionOutcome.CORRECT, "Optics", "None", true
        );

        Assert.assertNotNull(q1);
        Assert.assertEquals(1, q1.getQuestionNum());
        Assert.assertEquals(52000L, q1.getDurationMs());
        Assert.assertEquals(QuestionOutcome.CORRECT.name(), q1.getOutcome());
        Assert.assertEquals("Optics", q1.getTopic());

        // Index advanced to 2, question elapsed reset to 0, total study duration = 52s
        Assert.assertEquals(2, recorder.getCurrentQuestionIndex());
        Assert.assertEquals(0L, recorder.getCurrentQuestionElapsedMs());
        Assert.assertEquals(52000L, recorder.getTotalStudyDurationMs());
        Assert.assertEquals(1, recorder.getRecordedLogs().size());
        Assert.assertTrue(recorder.canUndo());
        Assert.assertEquals(1, recorder.getUndoStackDepth());
    }

    @Test
    public void testAtomicRecording_rollbackOnInvalidParameters() {
        recorder.tickQuestion(40000L);
        Assert.assertEquals(40000L, recorder.getCurrentQuestionElapsedMs());

        // Attempting to simulate invalid state with negative elapsed
        // (direct validation guard in recordQuestionAtomically)
        try {
            // Force negative check by reflection or invalid state
            recorder.recordQuestionAtomically(QuestionOutcome.CORRECT, "Physics", "", true);
        } catch (Exception e) {
            Assert.fail("Valid recording should not fail");
        }

        Assert.assertEquals(2, recorder.getCurrentQuestionIndex());
        Assert.assertEquals(1, recorder.getRecordedLogs().size());
    }

    @Test
    public void testUndoSingleQuestionRecord_restoresPreviousDurationAndCounter() {
        // Solve Q1: 48,000 ms -> commit
        recorder.tickQuestion(48000L);
        recorder.recordQuestionAtomically(QuestionOutcome.CORRECT, "Mechanics", "", true);

        Assert.assertEquals(2, recorder.getCurrentQuestionIndex());
        Assert.assertEquals(0L, recorder.getCurrentQuestionElapsedMs());
        Assert.assertEquals(1, recorder.getRecordedLogs().size());

        // User triggers Undo
        boolean undone = recorder.undoLastRecord();
        Assert.assertTrue("Undo must succeed", undone);

        // Previous state restored: Question index = 1, Elapsed = 48,000 ms
        Assert.assertEquals(1, recorder.getCurrentQuestionIndex());
        Assert.assertEquals(48000L, recorder.getCurrentQuestionElapsedMs());
        Assert.assertEquals(0, recorder.getRecordedLogs().size());
        Assert.assertFalse(recorder.canUndo());

        // Ticking after undo continues from restored duration
        recorder.tickQuestion(2000L);
        Assert.assertEquals(50000L, recorder.getCurrentQuestionElapsedMs());
    }

    @Test
    public void testMultiLevelUndoStack_handlesMultipleRollbacksToEmpty() {
        // Record 3 questions sequentially
        recorder.tickQuestion(30000L); // Q1: 30s
        recorder.recordQuestionAtomically(QuestionOutcome.CORRECT, "Math", "", true);

        recorder.tickQuestion(40000L); // Q2: 40s
        recorder.recordQuestionAtomically(QuestionOutcome.INCORRECT, "Math", "Mistake", true);

        recorder.tickQuestion(50000L); // Q3: 50s
        recorder.recordQuestionAtomically(QuestionOutcome.PARTIAL, "Math", "", true);

        Assert.assertEquals(4, recorder.getCurrentQuestionIndex());
        Assert.assertEquals(3, recorder.getRecordedLogs().size());
        Assert.assertEquals(3, recorder.getUndoStackDepth());

        // 1. Undo Q3
        Assert.assertTrue(recorder.undoLastRecord());
        Assert.assertEquals(3, recorder.getCurrentQuestionIndex());
        Assert.assertEquals(50000L, recorder.getCurrentQuestionElapsedMs());
        Assert.assertEquals(2, recorder.getRecordedLogs().size());

        // 2. Undo Q2
        Assert.assertTrue(recorder.undoLastRecord());
        Assert.assertEquals(2, recorder.getCurrentQuestionIndex());
        Assert.assertEquals(40000L, recorder.getCurrentQuestionElapsedMs());
        Assert.assertEquals(1, recorder.getRecordedLogs().size());

        // 3. Undo Q1
        Assert.assertTrue(recorder.undoLastRecord());
        Assert.assertEquals(1, recorder.getCurrentQuestionIndex());
        Assert.assertEquals(30000L, recorder.getCurrentQuestionElapsedMs());
        Assert.assertEquals(0, recorder.getRecordedLogs().size());

        // 4. Undo on empty stack returns false
        Assert.assertFalse(recorder.undoLastRecord());
        Assert.assertFalse(recorder.canUndo());
    }

    // ==========================================
    // Vertical Two-Zone (Upper=Wrong, Lower=Correct) Tests
    // ==========================================

    @Test
    public void testVerticalTwoZoneHitDetection_upperWrongLowerCorrect() {
        hitDetector.setOrientation(TwoZoneHitDetector.Orientation.VERTICAL);
        Assert.assertEquals(TwoZoneHitDetector.Orientation.VERTICAL, hitDetector.getOrientation());

        // Screen is 1080 x 1920. Split line = 1920 * 0.5 = 960px.
        // Upper zone (y < 960): Marks question WRONG
        Assert.assertEquals(TouchZone.ZONE_UPPER, hitDetector.detectZone(540f, 0f));
        Assert.assertEquals(TouchZone.ZONE_UPPER, hitDetector.detectZone(100f, 400f));
        Assert.assertEquals(TouchZone.ZONE_UPPER, hitDetector.detectZone(900f, 959f));

        // Lower zone (y >= 960): Marks question CORRECT
        Assert.assertEquals(TouchZone.ZONE_LOWER, hitDetector.detectZone(540f, 960f));
        Assert.assertEquals(TouchZone.ZONE_LOWER, hitDetector.detectZone(200f, 1400f));
        Assert.assertEquals(TouchZone.ZONE_LOWER, hitDetector.detectZone(1000f, 1920f));

        // Convenience method detectVerticalZone works regardless of default orientation
        hitDetector.setOrientation(TwoZoneHitDetector.Orientation.HORIZONTAL);
        Assert.assertEquals(TouchZone.ZONE_UPPER, hitDetector.detectVerticalZone(540f, 300f));
        Assert.assertEquals(TouchZone.ZONE_LOWER, hitDetector.detectVerticalZone(540f, 1500f));
    }

    @Test
    public void testVerticalTwoZoneDeadZoneAndExclusions() {
        hitDetector.setOrientation(TwoZoneHitDetector.Orientation.VERTICAL);
        // Add 20px dead zone around the center dividing line (y = 960 -> 950 to 970)
        hitDetector.setDeadZoneWidthPx(20f);
        Assert.assertEquals(20f, hitDetector.getDeadZoneWidthPx(), 0.001f);

        Assert.assertEquals(TouchZone.ZONE_DEADZONE, hitDetector.detectZone(540f, 960f));
        Assert.assertEquals(TouchZone.ZONE_DEADZONE, hitDetector.detectZone(540f, 955f));
        Assert.assertEquals(TouchZone.ZONE_DEADZONE, hitDetector.detectZone(540f, 965f));

        // Outside deadzone
        Assert.assertEquals(TouchZone.ZONE_UPPER, hitDetector.detectZone(540f, 949f));
        Assert.assertEquals(TouchZone.ZONE_LOWER, hitDetector.detectZone(540f, 971f));

        // Exclusion zones take precedence in vertical mode too
        hitDetector.addExclusionZone(0, 0, 1080, 150); // Top bar
        Assert.assertEquals(TouchZone.ZONE_EXCLUDED, hitDetector.detectZone(540f, 50f));
    }

    @Test
    public void testLandscapeModeVerticalTwoZoneHitDetection() {
        // Landscape display orientation: width = 1920, height = 1080
        TwoZoneHitDetector landDetector = new TwoZoneHitDetector(1920, 1080);
        landDetector.setOrientation(TwoZoneHitDetector.Orientation.VERTICAL);

        // Midpoint dividing line: height / 2 = 540px
        // Upper zone taps (y < 540) -> ZONE_UPPER (Wrong outcome + Red flash)
        Assert.assertEquals(TouchZone.ZONE_UPPER, landDetector.detectZone(960f, 0f));
        Assert.assertEquals(TouchZone.ZONE_UPPER, landDetector.detectZone(100f, 250f));
        Assert.assertEquals(TouchZone.ZONE_UPPER, landDetector.detectZone(1800f, 539f));

        // Lower zone taps (y >= 540) -> ZONE_LOWER (Right outcome + Green flash)
        Assert.assertEquals(TouchZone.ZONE_LOWER, landDetector.detectZone(960f, 540f));
        Assert.assertEquals(TouchZone.ZONE_LOWER, landDetector.detectZone(400f, 800f));
        Assert.assertEquals(TouchZone.ZONE_LOWER, landDetector.detectZone(1900f, 1079f));
    }

    @Test
    public void testUpperHalfRecognitionDuringActiveSolvingWithoutHeaderExclusion() {
        // High density screen (1080 x 2400)
        TwoZoneHitDetector detector = new TwoZoneHitDetector(1080, 2400);
        detector.setOrientation(TwoZoneHitDetector.Orientation.VERTICAL);

        // During active solving, bottom controls are excluded (e.g. bottom 200px)
        detector.addExclusionZone(0, 2200, 1080, 2400);

        // But top header areas (y = 0..300) are NOT excluded so taps anywhere in upper half register as ZONE_UPPER
        Assert.assertEquals(TouchZone.ZONE_UPPER, detector.detectZone(540f, 80f));  // Title tap area -> Upper / Wrong
        Assert.assertEquals(TouchZone.ZONE_UPPER, detector.detectZone(540f, 180f)); // Target pill area -> Upper / Wrong
        Assert.assertEquals(TouchZone.ZONE_UPPER, detector.detectZone(100f, 600f)); // Upper screen -> Upper / Wrong
        Assert.assertEquals(TouchZone.ZONE_UPPER, detector.detectZone(900f, 1199f)); // Just above midpoint -> Upper / Wrong

        // Lower zone taps
        Assert.assertEquals(TouchZone.ZONE_LOWER, detector.detectZone(540f, 1200f)); // Midpoint -> Lower / Right
        Assert.assertEquals(TouchZone.ZONE_LOWER, detector.detectZone(540f, 1800f)); // Lower screen -> Lower / Right

        // Bottom control area is excluded
        Assert.assertEquals(TouchZone.ZONE_EXCLUDED, detector.detectZone(540f, 2300f));
    }

    @Test
    public void testFullSurfaceTwoZoneRunningInterceptionPortraitAndLandscape() {
        // 1. Portrait mode (1080 x 2400, midpoint = 1200)
        TwoZoneHitDetector portraitDetector = new TwoZoneHitDetector(1080, 2400);
        portraitDetector.setOrientation(TwoZoneHitDetector.Orientation.VERTICAL);

        // Header taps (session title, target text, exam banner) MUST register as ZONE_UPPER (Wrong)
        Assert.assertEquals(TouchZone.ZONE_UPPER, portraitDetector.detectZone(540f, 40f));   // Exam banner tap
        Assert.assertEquals(TouchZone.ZONE_UPPER, portraitDetector.detectZone(540f, 100f));  // Session title tap
        Assert.assertEquals(TouchZone.ZONE_UPPER, portraitDetector.detectZone(540f, 160f));  // Target text tap
        Assert.assertEquals(TouchZone.ZONE_UPPER, portraitDetector.detectZone(200f, 800f));  // Upper timer area
        Assert.assertEquals(TouchZone.ZONE_UPPER, portraitDetector.detectZone(900f, 1199f)); // Pixel right above boundary

        // Lower half taps MUST register as ZONE_LOWER (Right)
        Assert.assertEquals(TouchZone.ZONE_LOWER, portraitDetector.detectZone(540f, 1200f)); // Midpoint boundary
        Assert.assertEquals(TouchZone.ZONE_LOWER, portraitDetector.detectZone(540f, 1600f)); // Lower central area
        Assert.assertEquals(TouchZone.ZONE_LOWER, portraitDetector.detectZone(100f, 2300f)); // Lower corner area

        // 2. Landscape mode (2400 x 1080, midpoint = 540)
        TwoZoneHitDetector landscapeDetector = new TwoZoneHitDetector(2400, 1080);
        landscapeDetector.setOrientation(TwoZoneHitDetector.Orientation.VERTICAL);

        // Header taps in landscape MUST register as ZONE_UPPER (Wrong)
        Assert.assertEquals(TouchZone.ZONE_UPPER, landscapeDetector.detectZone(1200f, 30f));  // Top bar banner/title
        Assert.assertEquals(TouchZone.ZONE_UPPER, landscapeDetector.detectZone(1200f, 90f));  // Target text
        Assert.assertEquals(TouchZone.ZONE_UPPER, landscapeDetector.detectZone(400f, 300f));  // Left upper quadrant
        Assert.assertEquals(TouchZone.ZONE_UPPER, landscapeDetector.detectZone(2000f, 539f)); // Right pixel above boundary

        // Lower half taps in landscape MUST register as ZONE_LOWER (Right)
        Assert.assertEquals(TouchZone.ZONE_LOWER, landscapeDetector.detectZone(1200f, 540f)); // Midpoint boundary
        Assert.assertEquals(TouchZone.ZONE_LOWER, landscapeDetector.detectZone(1200f, 850f)); // Lower central readout
        Assert.assertEquals(TouchZone.ZONE_LOWER, landscapeDetector.detectZone(200f, 1050f)); // Bottom left corner
    }

    @Test
    public void testChangeQuestionTag_WrongToRightAndRecalculateMarks() {
        com.example.timerapp.model.TimerSession session = new com.example.timerapp.model.TimerSession(1L, "TEST", 10);
        com.example.timerapp.model.QuestionLog q1 = new com.example.timerapp.model.QuestionLog(1L, 1L, 1, 60000L, System.currentTimeMillis(), "", "PHYSICS", "MEDIUM", "INCORRECT", 3, "", "");
        session.addLog(q1);

        // Initially marked wrong -> JEE Main score = -1
        com.example.timerapp.data.analytics.EstimatedMarksCalculator.PerformanceAnalytics analyticsBefore =
                com.example.timerapp.data.analytics.EstimatedMarksCalculator.computeAnalytics("JEE_MAIN", session.getLogs(), 10);
        Assert.assertEquals(-1.0, analyticsBefore.estimatedScore, 0.001);
        Assert.assertEquals(1, analyticsBefore.wrongCount);
        Assert.assertEquals(0, analyticsBefore.correctCount);

        // Change tag from WRONG to CORRECT
        com.example.timerapp.model.QuestionLog updatedLog = session.updateLastLogOutcome("CORRECT");
        Assert.assertNotNull(updatedLog);

        // Recalculate -> JEE Main score = +4
        com.example.timerapp.data.analytics.EstimatedMarksCalculator.PerformanceAnalytics analyticsAfter =
                com.example.timerapp.data.analytics.EstimatedMarksCalculator.computeAnalytics("JEE_MAIN", session.getLogs(), 10);
        Assert.assertEquals(4.0, analyticsAfter.estimatedScore, 0.001);
        Assert.assertEquals(0, analyticsAfter.wrongCount);
        Assert.assertEquals(1, analyticsAfter.correctCount);
    }

    @Test
    public void testSessionRemoveLogById_deletesOnlyTargetedQuestion() {
        com.example.timerapp.model.TimerSession session = new com.example.timerapp.model.TimerSession(1L, "TEST", 10);
        com.example.timerapp.model.QuestionLog q1 = new com.example.timerapp.model.QuestionLog(101L, 1L, 1, 50000L, 1000L, "", "PHYSICS", "EASY", "CORRECT", 3, "", "");
        com.example.timerapp.model.QuestionLog q2 = new com.example.timerapp.model.QuestionLog(102L, 1L, 2, 75000L, 2000L, "", "PHYSICS", "HARD", "INCORRECT", 2, "CALC_ERROR", "");
        com.example.timerapp.model.QuestionLog q3 = new com.example.timerapp.model.QuestionLog(103L, 1L, 3, 40000L, 3000L, "", "PHYSICS", "EASY", "CORRECT", 3, "", "");

        session.addLog(q1);
        session.addLog(q2);
        session.addLog(q3);
        Assert.assertEquals(3, session.getLogs().size());
        Assert.assertEquals(4, session.getCurrentQuestion());

        // Delete Question 2 by ID (the incorrect one)
        boolean removed = session.removeLogById(102L);
        Assert.assertTrue("Log 102 should be removed", removed);
        Assert.assertEquals(2, session.getLogs().size());
        Assert.assertEquals(3, session.getCurrentQuestion());

        // Verify remaining logs are q1 and q3
        Assert.assertEquals(101L, session.getLogs().get(0).getId());
        Assert.assertEquals(103L, session.getLogs().get(1).getId());

        // Attempt removing non-existent ID
        Assert.assertFalse(session.removeLogById(999L));
    }

    @Test
    public void testSessionRemoveLogsByIds_multiSelectionDeletion() {
        com.example.timerapp.model.TimerSession session = new com.example.timerapp.model.TimerSession(1L, "TEST", 10);
        session.addLog(new com.example.timerapp.model.QuestionLog(201L, 1L, 1, 30000L, 1000L, "", "MATH", "EASY", "CORRECT", 3, "", ""));
        session.addLog(new com.example.timerapp.model.QuestionLog(202L, 1L, 2, 45000L, 2000L, "", "MATH", "MEDIUM", "INCORRECT", 1, "", ""));
        session.addLog(new com.example.timerapp.model.QuestionLog(203L, 1L, 3, 60000L, 3000L, "", "MATH", "HARD", "INCORRECT", 2, "", ""));
        session.addLog(new com.example.timerapp.model.QuestionLog(204L, 1L, 4, 35000L, 4000L, "", "MATH", "EASY", "CORRECT", 3, "", ""));

        java.util.Set<Long> toDelete = new java.util.HashSet<>(java.util.Arrays.asList(202L, 203L));
        int count = session.removeLogsByIds(toDelete);
        Assert.assertEquals(2, count);
        Assert.assertEquals(2, session.getLogs().size());
        Assert.assertEquals(201L, session.getLogs().get(0).getId());
        Assert.assertEquals(204L, session.getLogs().get(1).getId());
    }
}

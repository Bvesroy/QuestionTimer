package com.example.timerapp.data;

import com.example.timerapp.data.engine.DualClockEngine;
import com.example.timerapp.data.model.BreakLog;
import com.example.timerapp.data.model.QuestionLog;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * JUnit 4 Test Suite for Dual-Clock Engine, Question Reset, Accidental Recovery,
 * Break Exclusions, and Multi-Hour Long Session Simulation.
 */
public class DualClockAndResetTest {

    private DualClockEngine engine;

    @Before
    public void setUp() {
        engine = new DualClockEngine(1L);
    }

    @Test
    public void testCurrentQuestionReset_preservesTotalStudyClockAndLogs() {
        // Complete Question 1: 45,000 ms
        engine.tickQuestion(45000L);
        QuestionLog q1 = engine.commitCurrentQuestion();
        Assert.assertEquals(1, q1.getQuestionNum());
        Assert.assertEquals(45000L, q1.getDurationMs());

        // Complete Question 2: 60,000 ms
        engine.tickQuestion(60000L);
        QuestionLog q2 = engine.commitCurrentQuestion();
        Assert.assertEquals(2, q2.getQuestionNum());
        Assert.assertEquals(60000L, q2.getDurationMs());

        long studyTimeBeforeQ3 = engine.getTotalStudyClockMs();
        Assert.assertEquals(105000L, studyTimeBeforeQ3);
        Assert.assertEquals(2, engine.getQuestionLogs().size());

        // Working on Question 3: 35,000 ms accumulated so far
        engine.tickQuestion(35000L);
        Assert.assertEquals(35000L, engine.getCurrentQuestionElapsedMs());
        Assert.assertEquals(140000L, engine.getTotalStudyClockMs());

        // Action: User resets current question timer
        engine.resetCurrentQuestion();

        // 1. Current question timer reset to 0
        Assert.assertEquals(0L, engine.getCurrentQuestionElapsedMs());

        // 2. Previous completed question logs are NOT deleted
        List<QuestionLog> logs = engine.getQuestionLogs();
        Assert.assertEquals(2, logs.size());
        Assert.assertEquals(45000L, logs.get(0).getDurationMs());
        Assert.assertEquals(60000L, logs.get(1).getDurationMs());

        // 3. Completed questions total remains unchanged
        Assert.assertEquals(105000L, engine.getCompletedQuestionsTotalDurationMs());

        // 4. Total Study Clock retains completed study time (105,000 ms)
        Assert.assertEquals(105000L, engine.getTotalStudyClockMs());
    }

    @Test
    public void testAccidentalResetRecovery_undoRestoresElapsedMilliseconds() {
        // Solving Question 1: accumulated 82,450 ms
        engine.tickQuestion(82450L);
        Assert.assertEquals(82450L, engine.getCurrentQuestionElapsedMs());

        // Accidental reset triggered
        engine.resetCurrentQuestion();
        Assert.assertEquals(0L, engine.getCurrentQuestionElapsedMs());
        Assert.assertEquals(82450L, engine.getLastDiscardedQuestionElapsedMs());
        Assert.assertTrue(engine.canUndoReset());

        // Action: User presses Undo
        boolean restored = engine.undoResetCurrentQuestion();
        Assert.assertTrue("Undo must report success", restored);
        Assert.assertEquals("Restored elapsed time must exactly match previous value", 82450L, engine.getCurrentQuestionElapsedMs());
        Assert.assertEquals(0L, engine.getLastDiscardedQuestionElapsedMs());
        Assert.assertFalse("Cannot undo twice", engine.canUndoReset());

        // Further tick continues from restored time
        engine.tickQuestion(550L);
        Assert.assertEquals(83000L, engine.getCurrentQuestionElapsedMs());
    }

    @Test
    public void testTotalStudyClockContinuity_accumulatesAcrossQuestionsAndPauses() {
        // Question 1: 40,000 ms -> commit
        engine.tickQuestion(40000L);
        engine.commitCurrentQuestion();

        // Pause state simulated
        engine.setPaused(true);
        engine.tickQuestion(10000L); // Should not accumulate while paused
        Assert.assertEquals(40000L, engine.getTotalStudyClockMs());
        engine.setPaused(false);

        // Question 2: 50,000 ms -> commit
        engine.tickQuestion(50000L);
        engine.commitCurrentQuestion();

        // Question 3: 30,000 ms in progress
        engine.tickQuestion(30000L);

        // Total Study Clock must be continuous: 40,000 + 50,000 + 30,000 = 120,000 ms
        Assert.assertEquals(120000L, engine.getTotalStudyClockMs());
        Assert.assertEquals(90000L, engine.getCompletedQuestionsTotalDurationMs());
        Assert.assertEquals(30000L, engine.getCurrentQuestionElapsedMs());
    }

    @Test
    public void testExplicitBreakExclusion_subtractsFromStudyTimePreservingQuestionClock() {
        // Solving question: 75,000 ms in progress
        engine.tickQuestion(75000L);
        Assert.assertEquals(75000L, engine.getCurrentQuestionElapsedMs());
        Assert.assertEquals(75000L, engine.getTotalStudyClockMs());

        // Explicit break started at t=100000
        long breakStart = 100000L;
        engine.startBreak(breakStart);
        Assert.assertTrue(engine.hasActiveBreak());
        Assert.assertTrue(engine.isPaused());

        // During break, ticks should not increase question or study time
        engine.tickQuestion(15000L);
        Assert.assertEquals(75000L, engine.getCurrentQuestionElapsedMs());

        // Break ends 15 minutes (900,000 ms) later at t=1000000
        long breakEnd = breakStart + 900000L;
        engine.endBreak(breakEnd);

        Assert.assertFalse(engine.hasActiveBreak());
        Assert.assertFalse(engine.isPaused());

        // Assert break duration recorded
        Assert.assertEquals(900000L, engine.getTotalBreakDurationMs());
        List<BreakLog> breaks = engine.getBreakLogs();
        Assert.assertEquals(1, breaks.size());
        Assert.assertEquals(900000L, breaks.get(0).getDurationMs());

        // Active question clock preserved at 75,000 ms
        Assert.assertEquals(75000L, engine.getCurrentQuestionElapsedMs());

        // Net Total Study Clock strictly excludes break time: still 75,000 ms
        Assert.assertEquals(75000L, engine.getTotalStudyClockMs());

        // Gross session time includes break: 75,000 + 900,000 = 975,000 ms
        Assert.assertEquals(975000L, engine.getGrossSessionDurationMs());
    }

    @Test
    public void testLongSessionSimulation_noDriftOrOverflowOnMultiHourSession() {
        // Simulate an intense 12-hour JEE marathon study session
        // 12 hours = 12 * 3600 * 1000 = 43,200,000 ms
        int totalQuestions = 180; // 180 questions solved
        long avgQuestionDurationMs = 210000L; // 3.5 minutes per question = 37,800,000 ms study
        long breakDurationMs = 1800000L; // 3 breaks of 30 minutes = 5,400,000 ms breaks

        for (int i = 1; i <= totalQuestions; i++) {
            engine.tickQuestion(avgQuestionDurationMs);
            engine.commitCurrentQuestion();

            // Insert a break every 60 questions
            if (i % 60 == 0) {
                long now = 1700000000000L + (i * avgQuestionDurationMs);
                engine.startBreak(now);
                engine.endBreak(now + breakDurationMs);
            }
        }

        long expectedStudyMs = totalQuestions * avgQuestionDurationMs; // 37,800,000 ms
        long expectedBreakMs = 3 * breakDurationMs;                   // 5,400,000 ms
        long expectedGrossMs = expectedStudyMs + expectedBreakMs;     // 43,200,000 ms (12.0 hours)

        Assert.assertEquals(180, engine.getQuestionLogs().size());
        Assert.assertEquals(3, engine.getBreakLogs().size());

        // Exact millisecond assertion without long integer precision loss or drift
        Assert.assertEquals(expectedStudyMs, engine.getTotalStudyClockMs());
        Assert.assertEquals(expectedBreakMs, engine.getTotalBreakDurationMs());
        Assert.assertEquals(expectedGrossMs, engine.getGrossSessionDurationMs());

        // Assert average duration calculation
        long calculatedAvg = engine.getCompletedQuestionsTotalDurationMs() / engine.getQuestionLogs().size();
        Assert.assertEquals(avgQuestionDurationMs, calculatedAvg);
    }
}

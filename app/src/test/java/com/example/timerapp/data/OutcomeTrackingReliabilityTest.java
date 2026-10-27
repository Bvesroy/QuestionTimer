package com.example.timerapp.data;

import com.example.timerapp.data.analytics.EstimatedMarksCalculator;
import com.example.timerapp.data.analytics.EstimatedMarksCalculator.PerformanceAnalytics;
import com.example.timerapp.data.analytics.OutcomeClassifier;
import com.example.timerapp.model.QuestionLog;
import com.example.timerapp.model.TimerSession;
import com.example.timerapp.wallpaper.JeeWallpaperGenerator.WallpaperMetrics;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Reliability tests for the single-source outcome classifier shared by JEE analytics
 * and wallpaper metrics. Pure JVM: no Android runtime calls, no Robolectric.
 */
public class OutcomeTrackingReliabilityTest {

    private static final long NOW = 1_700_000_000_000L;
    private static final double DELTA = 0.001;

    private static QuestionLog log(long id, String outcome, long durationMs) {
        return new QuestionLog(id, 1L, (int) id, durationMs, NOW,
                "", "Math", "MEDIUM", outcome, 3, "", "");
    }

    private static List<QuestionLog> twoZoneLogs() {
        List<QuestionLog> logs = new ArrayList<>();
        logs.add(log(1L, "RIGHT", 40_000L));
        logs.add(log(2L, "WRONG", 90_000L));
        logs.add(log(3L, "RIGHT", 50_000L));
        logs.add(log(4L, "PAUSED", 30_000L));
        return logs;
    }

    // ---------------------------------------------------------------- classifier

    @Test
    public void classifierTreatsCorrectSolvedAndRightAsCorrect() {
        for (String outcome : new String[]{"CORRECT", "SOLVED", "RIGHT",
                "correct", "Solved", " right ", "RiGhT"}) {
            Assert.assertTrue("expected correct: [" + outcome + "]",
                    OutcomeClassifier.isCorrect(outcome));
            Assert.assertTrue("expected attempted: [" + outcome + "]",
                    OutcomeClassifier.isAttempted(outcome));
            Assert.assertFalse("must not be wrong: [" + outcome + "]",
                    OutcomeClassifier.isWrong(outcome));
            Assert.assertEquals("CORRECT", OutcomeClassifier.normalize(outcome));
        }
        Assert.assertFalse(OutcomeClassifier.isCorrect("INCORRECT"));
        Assert.assertFalse(OutcomeClassifier.isCorrect("WRONG"));
    }

    @Test
    public void classifierTreatsIncorrectAndWrongAsWrong() {
        for (String outcome : new String[]{"INCORRECT", "WRONG",
                "incorrect", "Wrong", "  wRoNg  "}) {
            Assert.assertTrue("expected wrong: [" + outcome + "]",
                    OutcomeClassifier.isWrong(outcome));
            Assert.assertTrue("expected attempted: [" + outcome + "]",
                    OutcomeClassifier.isAttempted(outcome));
            Assert.assertFalse("must not be correct: [" + outcome + "]",
                    OutcomeClassifier.isCorrect(outcome));
            Assert.assertEquals("WRONG", OutcomeClassifier.normalize(outcome));
        }
        Assert.assertFalse(OutcomeClassifier.isWrong("CORRECT"));
    }

    @Test
    public void classifierTreatsPausedSkippedUnattemptedNullAndBlankAsUnattempted() {
        String[] neutral = {"PAUSED", "SKIPPED", "UNATTEMPTED", "REVISED",
                "UNKNOWN", "", "   ", null};
        for (String outcome : neutral) {
            Assert.assertFalse(OutcomeClassifier.isCorrect(outcome));
            Assert.assertFalse(OutcomeClassifier.isWrong(outcome));
            Assert.assertFalse("must not be attempted: [" + outcome + "]",
                    OutcomeClassifier.isAttempted(outcome));
            Assert.assertEquals("UNATTEMPTED", OutcomeClassifier.normalize(outcome));
        }
    }

    @Test
    public void normalizeCollapsesSynonymsAndIsIdempotent() {
        Assert.assertEquals("CORRECT", OutcomeClassifier.normalize("SOLVED"));
        Assert.assertEquals("CORRECT", OutcomeClassifier.normalize("RIGHT"));
        Assert.assertEquals("CORRECT", OutcomeClassifier.normalize("correct"));
        Assert.assertEquals("WRONG", OutcomeClassifier.normalize("INCORRECT"));
        Assert.assertEquals("WRONG", OutcomeClassifier.normalize("wrong"));
        Assert.assertEquals("UNATTEMPTED", OutcomeClassifier.normalize("PAUSED"));
        Assert.assertEquals("UNATTEMPTED", OutcomeClassifier.normalize(""));
        Assert.assertEquals("UNATTEMPTED", OutcomeClassifier.normalize(null));
        Assert.assertEquals("CORRECT",
                OutcomeClassifier.normalize(OutcomeClassifier.normalize("right")));
        Assert.assertEquals("WRONG",
                OutcomeClassifier.normalize(OutcomeClassifier.normalize("INCORRECT")));
    }

    // ---------------------------------------------------------------- analytics

    @Test
    public void analyticsJeeMainCountsOnlyAttemptedDurations() {
        PerformanceAnalytics a = EstimatedMarksCalculator.computeAnalytics(
                EstimatedMarksCalculator.EXAM_JEE_MAIN, twoZoneLogs(), 10);

        Assert.assertEquals("JEE_MAIN", a.examType);
        Assert.assertEquals(10, a.totalQuestions);
        Assert.assertEquals(2, a.correctCount);
        Assert.assertEquals(1, a.wrongCount);
        Assert.assertEquals(7, a.unattemptedCount);
        Assert.assertEquals(200.0 / 3.0, a.accuracyPercentage, DELTA);

        // PAUSED 30s must be excluded from duration and average.
        Assert.assertEquals(180_000L, a.totalSolvingTimeMs);
        Assert.assertEquals(60.0, a.averageSolvingTimeSeconds, DELTA);

        Assert.assertEquals(7.0, a.estimatedScore, DELTA);
        Assert.assertEquals(1.0, a.negativePenalty, DELTA);
        Assert.assertEquals(40.0, a.maxPossibleScore, DELTA);

        Assert.assertEquals(144.0, a.targetPaceSeconds, DELTA);
        Assert.assertTrue(a.paceWithinTarget); // 60s < 144s

        Assert.assertEquals(90_000L, a.correctSolvingTimeMs);
        Assert.assertEquals(45.0, a.averageCorrectSolvingTimeSeconds, DELTA);
        Assert.assertEquals(90_000L, a.incorrectSolvingTimeMs);
        Assert.assertEquals(90.0, a.averageIncorrectSolvingTimeSeconds, DELTA);
    }

    @Test
    public void analyticsJeeAdvancedAppliesTwoMarkPenalty() {
        PerformanceAnalytics a = EstimatedMarksCalculator.computeAnalytics(
                EstimatedMarksCalculator.EXAM_JEE_ADVANCED, twoZoneLogs(), 10);

        Assert.assertEquals("JEE_ADVANCED", a.examType);
        Assert.assertEquals(2, a.correctCount);
        Assert.assertEquals(1, a.wrongCount);
        Assert.assertEquals(7, a.unattemptedCount);
        Assert.assertEquals(200.0 / 3.0, a.accuracyPercentage, DELTA);
        Assert.assertEquals(6.0, a.estimatedScore, DELTA);   // 8 - 2
        Assert.assertEquals(2.0, a.negativePenalty, DELTA);
        Assert.assertEquals(180.0, a.targetPaceSeconds, DELTA);
        Assert.assertTrue(a.paceWithinTarget); // 60s < 180s
        Assert.assertEquals(180_000L, a.totalSolvingTimeMs);
        Assert.assertEquals(60.0, a.averageSolvingTimeSeconds, DELTA);
    }

    @Test
    public void analyticsWithoutAttemptsReturnsZeroedMetricsWithoutDivisionByZero() {
        PerformanceAnalytics a = EstimatedMarksCalculator.computeAnalytics(
                EstimatedMarksCalculator.EXAM_JEE_MAIN, Collections.emptyList(), 5);

        Assert.assertEquals(5, a.totalQuestions);
        Assert.assertEquals(0, a.correctCount);
        Assert.assertEquals(0, a.wrongCount);
        Assert.assertEquals(5, a.unattemptedCount);
        Assert.assertEquals(0.0, a.accuracyPercentage, DELTA);
        Assert.assertEquals(0L, a.totalSolvingTimeMs);
        Assert.assertEquals(0.0, a.averageSolvingTimeSeconds, DELTA);
        Assert.assertEquals(0.0, a.averageCorrectSolvingTimeSeconds, DELTA);
        Assert.assertEquals(0.0, a.averageIncorrectSolvingTimeSeconds, DELTA);
        Assert.assertEquals(0.0, a.estimatedScore, DELTA);
        Assert.assertEquals(0.0, a.negativePenalty, DELTA);
        Assert.assertEquals(20.0, a.maxPossibleScore, DELTA);
        Assert.assertFalse(a.paceWithinTarget);
        Assert.assertNotNull(a.getFormattedSummary());

        // Null list is tolerated identically.
        PerformanceAnalytics nullList = EstimatedMarksCalculator.computeAnalytics(
                EstimatedMarksCalculator.EXAM_JEE_ADVANCED, null, 5);
        Assert.assertEquals(0, nullList.correctCount);
        Assert.assertEquals(0, nullList.wrongCount);
        Assert.assertEquals(5, nullList.unattemptedCount);
        Assert.assertEquals(0.0, nullList.estimatedScore, DELTA);

        // Explicit helper guards.
        Assert.assertEquals(0.0, EstimatedMarksCalculator.calculateAccuracy(0, 0), DELTA);
        Assert.assertEquals(0.0,
                EstimatedMarksCalculator.calculateAverageSolvingTimeSec(0L, 0), DELTA);
    }

    @Test
    public void analyticsClassifiesMeldedLegacyOutcomes() {
        List<QuestionLog> logs = Arrays.asList(
                log(1L, "CORRECT", 10_000L),
                log(2L, "SOLVED", 20_000L),
                log(3L, "INCORRECT", 30_000L),
                log(4L, "WRONG", 40_000L),
                log(5L, "UNATTEMPTED", 50_000L));

        PerformanceAnalytics a = EstimatedMarksCalculator.computeAnalytics(
                EstimatedMarksCalculator.EXAM_JEE_MAIN, logs, 5);

        Assert.assertEquals(2, a.correctCount);
        Assert.assertEquals(2, a.wrongCount);
        Assert.assertEquals(1, a.unattemptedCount);
        Assert.assertEquals(50.0, a.accuracyPercentage, DELTA);
        Assert.assertEquals(100_000L, a.totalSolvingTimeMs);
        Assert.assertEquals(25.0, a.averageSolvingTimeSeconds, DELTA);
        Assert.assertEquals(6.0, a.estimatedScore, DELTA);
        Assert.assertEquals(2.0, a.negativePenalty, DELTA);
        Assert.assertEquals(30_000L, a.correctSolvingTimeMs);
        Assert.assertEquals(15.0, a.averageCorrectSolvingTimeSeconds, DELTA);
        Assert.assertEquals(70_000L, a.incorrectSolvingTimeMs);
        Assert.assertEquals(35.0, a.averageIncorrectSolvingTimeSeconds, DELTA);

        Assert.assertEquals("CORRECT", OutcomeClassifier.normalize("SOLVED"));
        Assert.assertEquals("CORRECT", OutcomeClassifier.normalize("CORRECT"));
        Assert.assertEquals("WRONG", OutcomeClassifier.normalize("INCORRECT"));
        Assert.assertEquals("WRONG", OutcomeClassifier.normalize("WRONG"));
        Assert.assertEquals("UNATTEMPTED", OutcomeClassifier.normalize("UNATTEMPTED"));
    }

    // ---------------------------------------------------------------- wallpaper

    @Test
    public void wallpaperMetricsCountsRightAndWrongAndExcludesPaused() {
        TimerSession sessionA = new TimerSession(1L, "SESSION A", 10);
        sessionA.addLog(log(1L, "RIGHT", 40_000L));
        sessionA.addLog(log(2L, "WRONG", 100_000L));

        TimerSession sessionB = new TimerSession(2L, "SESSION B", 5);
        sessionB.addLog(log(3L, "PAUSED", 20_000L));

        long target = NOW + 30L * 24L * 60L * 60L * 1000L;
        WallpaperMetrics m = WallpaperMetrics.fromSessions(
                Arrays.asList(sessionA, sessionB), target, 5);

        Assert.assertEquals(3, m.totalQuestions);
        Assert.assertEquals(1, m.solvedCorrect);
        Assert.assertEquals(1, m.solvedIncorrect);
        Assert.assertEquals(50, m.accuracyPercentage); // 1 of 2 attempted
        Assert.assertEquals(2, m.totalSessions);
        Assert.assertEquals(5, m.streakDays);
        Assert.assertTrue(m.daysRemaining >= 0);
    }

    // ---------------------------------------------------------------- scoring edges

    @Test
    public void estimatedScoreEdgeCases() {
        Assert.assertEquals(45.0, EstimatedMarksCalculator.calculateEstimatedScore(
                EstimatedMarksCalculator.EXAM_JEE_MAIN, 12, 3), DELTA);
        Assert.assertEquals(42.0, EstimatedMarksCalculator.calculateEstimatedScore(
                EstimatedMarksCalculator.EXAM_JEE_ADVANCED, 12, 3), DELTA);
        Assert.assertEquals(100.0, EstimatedMarksCalculator.calculateEstimatedScore(
                EstimatedMarksCalculator.EXAM_JEE_MAIN, 25, 0), DELTA);
        Assert.assertEquals(0.0, EstimatedMarksCalculator.calculateEstimatedScore(
                EstimatedMarksCalculator.EXAM_JEE_MAIN, 0, 0), DELTA);
        Assert.assertEquals(0.0, EstimatedMarksCalculator.calculateEstimatedScore(
                EstimatedMarksCalculator.EXAM_JEE_MAIN, -5, -3), DELTA);
        Assert.assertEquals(0.0, EstimatedMarksCalculator.calculateEstimatedScore(
                EstimatedMarksCalculator.EXAM_JEE_ADVANCED, -1, -7), DELTA);
    }

    @Test
    public void negativeEstimatedScoresArePreserved() {
        List<QuestionLog> allWrong = Arrays.asList(
                log(1L, "WRONG", 10_000L),
                log(2L, "WRONG", 10_000L),
                log(3L, "WRONG", 10_000L),
                log(4L, "WRONG", 10_000L));

        PerformanceAnalytics main = EstimatedMarksCalculator.computeAnalytics(
                EstimatedMarksCalculator.EXAM_JEE_MAIN, allWrong, 4);
        Assert.assertEquals(0, main.correctCount);
        Assert.assertEquals(4, main.wrongCount);
        Assert.assertEquals(0, main.unattemptedCount);
        Assert.assertEquals(0.0, main.accuracyPercentage, DELTA);
        Assert.assertEquals(-4.0, main.estimatedScore, DELTA);
        Assert.assertEquals(4.0, main.negativePenalty, DELTA);

        PerformanceAnalytics adv = EstimatedMarksCalculator.computeAnalytics(
                EstimatedMarksCalculator.EXAM_JEE_ADVANCED, allWrong, 4);
        Assert.assertEquals(-8.0, adv.estimatedScore, DELTA);
        Assert.assertEquals(8.0, adv.negativePenalty, DELTA);

        Assert.assertEquals(-4.0, EstimatedMarksCalculator.calculateEstimatedScore(
                EstimatedMarksCalculator.EXAM_JEE_MAIN, 0, 4), DELTA);
        Assert.assertEquals(-8.0, EstimatedMarksCalculator.calculateEstimatedScore(
                EstimatedMarksCalculator.EXAM_JEE_ADVANCED, 0, 4), DELTA);
    }
}

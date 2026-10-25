package com.example.timerapp.data;

import com.example.timerapp.data.analytics.StudyCoachEngine;
import com.example.timerapp.data.model.QuestionLog;
import com.example.timerapp.data.model.SessionStats;
import com.example.timerapp.data.model.StudyRecommendation;
import com.example.timerapp.model.TimerSession;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * JUnit 4 Test Suite for Study Coach Rules-Based Logic:
 * 1. Insufficient data fallback (< 5 questions or < 2 sessions)
 * 2. Median vs Target Pace warnings (> 15% threshold)
 * 3. Speed vs Accuracy Trade-off (> 18% error rate gap between fast and slow attempts)
 * 4. Subject Outlier (> 35% longer than overall average pace)
 * 5. Mistake clustering (dominant error category >= 40%)
 * 6. Target feasibility adjustment (unrealistic vs trivial target)
 * 7. Full pipeline integration and sample size transparency
 * 8. Legacy CoachCard generation for UI backwards compatibility
 */
public class StudyCoachEngineTest {

    @Test
    public void testInsufficientDataHandling_safeFallbackWhenSampleSizeLessThanFive() {
        List<SessionStats> sessions = Arrays.asList(
                new SessionStats(1L, "Session 1", 5, 2, 120000, 60000, 1000L, 2000L, false),
                new SessionStats(2L, "Session 2", 5, 1, 60000, 60000, 3000L, 4000L, false)
        );

        // Only 3 questions logged in total
        List<QuestionLog> logs = Arrays.asList(
                new QuestionLog(1L, 1L, 1, 60000, 1500L),
                new QuestionLog(2L, 1L, 2, 60000, 1800L),
                new QuestionLog(3L, 2L, 1, 60000, 3500L)
        );

        List<StudyRecommendation> recs = StudyCoachEngine.evaluateRecommendations(sessions, logs, 60, 10);

        Assert.assertNotNull(recs);
        Assert.assertEquals(1, recs.size());

        StudyRecommendation fallback = recs.get(0);
        Assert.assertEquals("insufficient_data", fallback.getKey());
        Assert.assertEquals("Insufficient Practice Data", fallback.getTitle());
        Assert.assertEquals(3, fallback.getSampleSize());
        Assert.assertEquals(StudyRecommendation.ConfidenceLevel.LOW, fallback.getConfidenceLevel());
        Assert.assertFalse("Insufficient data recommendation must not be actionable", fallback.isActionable());
    }

    @Test
    public void testInsufficientDataHandling_safeFallbackWhenSessionCountLessThanTwo() {
        List<SessionStats> sessions = Collections.singletonList(
                new SessionStats(1L, "Session 1", 10, 6, 360000, 60000, 1000L, 2000L, false)
        );

        List<QuestionLog> logs = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            logs.add(new QuestionLog(i, 1L, i, 60000, 1000L + i * 100));
        }

        List<StudyRecommendation> recs = StudyCoachEngine.evaluateRecommendations(sessions, logs, 60, 10);

        Assert.assertNotNull(recs);
        Assert.assertEquals(1, recs.size());
        Assert.assertEquals("insufficient_data", recs.get(0).getKey());
    }

    @Test
    public void testMedianVsTargetPace_triggersWhenMedianExceedsTargetByThreshold() {
        // Target pace = 60s
        // Durations: 60s, 75s, 80s, 95s, 110s -> median = 80s (33.3% slower than target > 15%)
        List<QuestionLog> logs = Arrays.asList(
                new QuestionLog(1L, 1L, 1, 60_000L, 1000L),
                new QuestionLog(2L, 1L, 2, 75_000L, 2000L),
                new QuestionLog(3L, 1L, 3, 80_000L, 3000L),
                new QuestionLog(4L, 1L, 4, 95_000L, 4000L),
                new QuestionLog(5L, 1L, 5, 110_000L, 5000L)
        );

        StudyRecommendation paceRec = StudyCoachEngine.evaluateMedianPace(logs, 60);

        Assert.assertNotNull("Pace recommendation should trigger", paceRec);
        Assert.assertEquals("pace_adherence", paceRec.getKey());
        Assert.assertEquals("Pace Below Target Speed", paceRec.getTitle());
        Assert.assertTrue(paceRec.getObservation().contains("33% slower"));
        Assert.assertEquals(5, paceRec.getSampleSize());
        Assert.assertEquals("All Sessions", paceRec.getTimeframe());
        Assert.assertEquals(StudyRecommendation.ConfidenceLevel.HIGH, paceRec.getConfidenceLevel());
        Assert.assertNotNull(paceRec.getSuggestedAction());
        Assert.assertNotNull(paceRec.getRationale());
    }

    @Test
    public void testSpeedVsAccuracyTradeOff_triggersWhenFastAttemptsHaveHigherErrorRate() {
        // 10 questions: 5 fast (10s, 15s, 20s, 25s, 30s) and 5 slow (70s, 80s, 90s, 100s, 110s)
        // Fast questions: 4 INCORRECT out of 5 (80% error rate)
        // Slow questions: 1 INCORRECT out of 5 (20% error rate)
        // Difference = 80% - 20% = 60% > 18% threshold and fastErrors (4) >= 2
        List<QuestionLog> logs = new ArrayList<>();

        logs.add(new QuestionLog(1, 1L, 1, 10_000L, 1000L, "Q1", "Math", "EASY", "INCORRECT", 1, "Careless", ""));
        logs.add(new QuestionLog(2, 1L, 2, 15_000L, 2000L, "Q2", "Math", "EASY", "INCORRECT", 1, "Careless", ""));
        logs.add(new QuestionLog(3, 1L, 3, 20_000L, 3000L, "Q3", "Math", "EASY", "INCORRECT", 2, "Careless", ""));
        logs.add(new QuestionLog(4, 1L, 4, 25_000L, 4000L, "Q4", "Math", "MEDIUM", "INCORRECT", 2, "Calculation", ""));
        logs.add(new QuestionLog(5, 1L, 5, 30_000L, 5000L, "Q5", "Math", "MEDIUM", "CORRECT", 4, "", ""));

        logs.add(new QuestionLog(6, 1L, 6, 70_000L, 6000L, "Q6", "Math", "HARD", "CORRECT", 5, "", ""));
        logs.add(new QuestionLog(7, 1L, 7, 80_000L, 7000L, "Q7", "Math", "HARD", "CORRECT", 4, "", ""));
        logs.add(new QuestionLog(8, 1L, 8, 90_000L, 8000L, "Q8", "Math", "HARD", "CORRECT", 5, "", ""));
        logs.add(new QuestionLog(9, 1L, 9, 100_000L, 9000L, "Q9", "Math", "HARD", "CORRECT", 4, "", ""));
        logs.add(new QuestionLog(10, 1L, 10, 110_000L, 10000L, "Q10", "Math", "HARD", "INCORRECT", 3, "Concept", ""));

        StudyRecommendation rec = StudyCoachEngine.evaluateSpeedAccuracyTradeoff(logs);

        Assert.assertNotNull("Speed-accuracy tradeoff warning should trigger", rec);
        Assert.assertEquals("speed_accuracy_tradeoff", rec.getKey());
        Assert.assertEquals("Rushing Causes Accuracy Drops", rec.getTitle());
        Assert.assertTrue(rec.getObservation().contains("error rate of 80% vs 20%"));
        Assert.assertEquals(10, rec.getSampleSize());
        Assert.assertEquals(StudyRecommendation.ConfidenceLevel.HIGH, rec.getConfidenceLevel());
    }

    @Test
    public void testSubjectOutlier_triggersWhenSubjectTakesOverThirtyFivePercentLongerThanAverage() {
        // 3 Sessions: Physics (Session 1), Chemistry (Session 2), Mathematics (Session 3)
        List<SessionStats> sessions = Arrays.asList(
                new SessionStats(1L, "Physics Session", 3, 3, 330_000L, 110_000L, 1000L, 2000L, false, "Physics", "", "", "", "COMPLETED", "default"),
                new SessionStats(2L, "Chem Session", 3, 3, 150_000L, 50_000L, 3000L, 4000L, false, "Chemistry", "", "", "", "COMPLETED", "default"),
                new SessionStats(3L, "Math Session", 3, 3, 150_000L, 50_000L, 5000L, 6000L, false, "Mathematics", "", "", "", "COMPLETED", "default")
        );

        List<QuestionLog> logs = new ArrayList<>();
        // Physics: 3 questions @ 110s each = 330s
        logs.add(new QuestionLog(1, 1L, 1, 110_000L, 1100L));
        logs.add(new QuestionLog(2, 1L, 2, 110_000L, 1200L));
        logs.add(new QuestionLog(3, 1L, 3, 110_000L, 1300L));

        // Chemistry: 3 questions @ 50s each = 150s
        logs.add(new QuestionLog(4, 2L, 1, 50_000L, 3100L));
        logs.add(new QuestionLog(5, 2L, 2, 50_000L, 3200L));
        logs.add(new QuestionLog(6, 2L, 3, 50_000L, 3300L));

        // Mathematics: 3 questions @ 50s each = 150s
        logs.add(new QuestionLog(7, 3L, 1, 50_000L, 5100L));
        logs.add(new QuestionLog(8, 3L, 2, 50_000L, 5200L));
        logs.add(new QuestionLog(9, 3L, 3, 50_000L, 5300L));

        // Overall: 9 questions, total 630s -> avg 70s.
        // Physics avg = 110s -> deviation = (110 - 70) / 70 = 40 / 70 = 57.1% (> 35%)
        StudyRecommendation outlier = StudyCoachEngine.evaluateSubjectOutliers(sessions, logs);

        Assert.assertNotNull("Physics subject outlier should trigger", outlier);
        Assert.assertEquals("subject_outlier_bottleneck", outlier.getKey());
        Assert.assertEquals("Physics Pace Bottleneck", outlier.getTitle());
        Assert.assertTrue(outlier.getObservation().contains("Physics questions average 110.0s"));
        Assert.assertTrue(outlier.getObservation().contains("57% slower than your overall average"));
        Assert.assertEquals(3, outlier.getSampleSize());
        Assert.assertEquals(StudyRecommendation.ConfidenceLevel.HIGH, outlier.getConfidenceLevel());
    }

    @Test
    public void testMistakePatternClustering_detectsDominantErrorCategory() {
        // 6 mistakes logged:
        // 4 x "Calculation Error" (4 / 6 = 66.7% >= 40%)
        // 1 x "Conceptual"
        // 1 x "Misread Question"
        List<QuestionLog> logs = new ArrayList<>();
        logs.add(new QuestionLog(1, 1L, 1, 60_000L, 1000L, "Q1", "Math", "EASY", "INCORRECT", 2, "Calculation Error", ""));
        logs.add(new QuestionLog(2, 1L, 2, 60_000L, 2000L, "Q2", "Math", "EASY", "INCORRECT", 2, "Calculation Error", ""));
        logs.add(new QuestionLog(3, 1L, 3, 60_000L, 3000L, "Q3", "Math", "MEDIUM", "INCORRECT", 2, "Calculation Error", ""));
        logs.add(new QuestionLog(4, 1L, 4, 60_000L, 4000L, "Q4", "Math", "MEDIUM", "INCORRECT", 3, "Calculation Error", ""));
        logs.add(new QuestionLog(5, 1L, 5, 60_000L, 5000L, "Q5", "Physics", "HARD", "INCORRECT", 1, "Conceptual", ""));
        logs.add(new QuestionLog(6, 1L, 6, 60_000L, 6000L, "Q6", "Chem", "HARD", "INCORRECT", 1, "Misread Question", ""));

        StudyRecommendation mistakeRec = StudyCoachEngine.evaluateMistakePatterns(logs);

        Assert.assertNotNull("Dominant mistake recommendation should trigger", mistakeRec);
        Assert.assertEquals("dominant_mistake_pattern", mistakeRec.getKey());
        Assert.assertEquals("Dominant Mistake: Calculation Error", mistakeRec.getTitle());
        Assert.assertTrue(mistakeRec.getObservation().contains("Calculation Error"));
        Assert.assertTrue(mistakeRec.getObservation().contains("67% of your logged errors"));
        Assert.assertEquals(6, mistakeRec.getSampleSize());
        Assert.assertEquals(StudyRecommendation.ConfidenceLevel.HIGH, mistakeRec.getConfidenceLevel());
        Assert.assertTrue(mistakeRec.getSuggestedAction().contains("intermediate step formulas"));
    }

    @Test
    public void testTargetFeasibility_detectsOverloadedGoal() {
        long oneDayMs = 24L * 3600 * 1000L;
        long day1 = 1000000000000L;
        long day2 = day1 + oneDayMs;
        long day3 = day2 + oneDayMs;

        List<SessionStats> sessions = Arrays.asList(
                new SessionStats(1L, "Day 1", 10, 10, 600000, 60000, day1, day1, false),
                new SessionStats(2L, "Day 2", 10, 10, 600000, 60000, day2, day2, false),
                new SessionStats(3L, "Day 3", 10, 10, 600000, 60000, day3, day3, false)
        );

        // Daily median solved = 10. Target daily questions = 30 (> 2x median).
        StudyRecommendation rec = StudyCoachEngine.evaluateTargetFeasibility(sessions, 30);

        Assert.assertNotNull("Overloaded goal recommendation should trigger", rec);
        Assert.assertEquals("target_feasibility", rec.getKey());
        Assert.assertEquals("Daily Goal Overloaded", rec.getTitle());
        Assert.assertTrue(rec.getObservation().contains("over 2x your current daily median"));
        Assert.assertEquals(3, rec.getSampleSize());
    }

    @Test
    public void testFullPipeline_transparencyAndCompleteness() {
        // Multi-session setup with pace warning, mistake cluster, etc.
        List<SessionStats> sessions = Arrays.asList(
                new SessionStats(1L, "S1", 10, 5, 500000, 100000, 1000000L, 2000000L, false, "Physics", "", "", "", "COMPLETED", "default"),
                new SessionStats(2L, "S2", 10, 5, 500000, 100000, 3000000L, 4000000L, false, "Physics", "", "", "", "COMPLETED", "default")
        );

        List<QuestionLog> logs = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            logs.add(new QuestionLog(i, 1L, i, 100_000L, 1000000L + i * 1000, "Q" + i, "Physics", "MEDIUM", "INCORRECT", 2, "Calculation Slip", ""));
        }

        List<StudyRecommendation> recs = StudyCoachEngine.evaluateRecommendations(sessions, logs, 60, 5);

        Assert.assertNotNull(recs);
        Assert.assertFalse("Should produce recommendations", recs.isEmpty());

        for (StudyRecommendation r : recs) {
            Assert.assertNotNull("Key must not be null", r.getKey());
            Assert.assertNotNull("Title must not be null", r.getTitle());
            Assert.assertNotNull("Observation must not be null", r.getObservation());
            Assert.assertNotNull("Timeframe must not be null", r.getTimeframe());
            Assert.assertTrue("Sample size must be positive", r.getSampleSize() > 0);
            Assert.assertNotNull("Suggested action must not be null", r.getSuggestedAction());
            Assert.assertNotNull("Rationale must not be null", r.getRationale());
            Assert.assertNotNull("Confidence level must not be null", r.getConfidenceLevel());
        }
    }

    @Test
    public void testLegacyCoachCardGeneration_producesProperCards() {
        // Test backwards compatible generateInsights for UI CoachCard
        List<TimerSession> emptySessions = Collections.emptyList();
        List<StudyCoachEngine.CoachCard> emptyCards = StudyCoachEngine.generateInsights(emptySessions);
        Assert.assertEquals(1, emptyCards.size());
        Assert.assertEquals("card_insufficient_data", emptyCards.get(0).id);
        Assert.assertEquals("INFO", emptyCards.get(0).priority);

        // Session with high pace (> 160s)
        List<TimerSession> activeSessions = new ArrayList<>();
        TimerSession s = new TimerSession(1L, "Physics Sprint", 5);
        s.setSubject("Physics");
        for (int i = 1; i <= 6; i++) {
            com.example.timerapp.model.QuestionLog q = new com.example.timerapp.model.QuestionLog(
                    i, 1L, i, 180_000L, System.currentTimeMillis(), "Q" + i, "Optics", "HARD", "SOLVED", 4, "Formula", "notes"
            );
            s.addLog(q);
        }
        activeSessions.add(s);

        List<StudyCoachEngine.CoachCard> cards = StudyCoachEngine.generateInsights(activeSessions);
        Assert.assertFalse(cards.isEmpty());
        Assert.assertTrue(cards.stream().anyMatch(c -> c.id.equals("card_pacing_high")));
    }
}

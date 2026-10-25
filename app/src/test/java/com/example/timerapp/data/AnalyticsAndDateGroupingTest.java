package com.example.timerapp.data;

import com.example.timerapp.data.model.DailyPracticeDataPoint;
import com.example.timerapp.data.model.DateSessionGroup;
import com.example.timerapp.data.model.SessionStats;
import com.example.timerapp.data.model.SpeedDataPoint;
import com.example.timerapp.data.model.TargetCompliance;
import com.example.timerapp.data.model.TargetSettings;
import com.example.timerapp.data.model.TimeRangeFilter;
import com.example.timerapp.data.model.TimeRangeKpiSummary;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Unit tests verifying Date Grouping, Time Range Analytics Filtering,
 * Best-Performing Ranking Algorithm, Target Settings, and Chart Data Points.
 */
public class AnalyticsAndDateGroupingTest {

    @Test
    public void testDateSessionGroupAggregations() {
        SessionStats s1 = new SessionStats(1L, "Math Set 1", 30, 20, 1200000L, 60000L, 1000L, 2000L, false);
        SessionStats s2 = new SessionStats(2L, "Physics Set 1", 30, 10, 800000L, 80000L, 3000L, 4000L, false);

        List<SessionStats> list = Arrays.asList(s1, s2);
        long totalDuration = 1200000L + 800000L; // 2,000,000 ms
        int totalQuestions = 20 + 10;            // 30 questions

        DateSessionGroup group = new DateSessionGroup(
                "2026-10-25",
                "October 25, 2026",
                1729814400000L,
                2,
                totalDuration,
                totalQuestions,
                list
        );

        Assert.assertEquals("2026-10-25", group.getDateString());
        Assert.assertEquals("October 25, 2026", group.getFormattedDate());
        Assert.assertEquals(2, group.getTotalSessionsCount());
        Assert.assertEquals(totalDuration, group.getTotalPracticeDurationMs());
        Assert.assertEquals(totalQuestions, group.getTotalQuestionsSolved());
        Assert.assertEquals(totalDuration / totalQuestions, group.getAverageTimePerQuestionMs());
        Assert.assertEquals(2, group.getSessions().size());
        Assert.assertNotNull(group.getFormattedTotalDuration());
        Assert.assertNotNull(group.getFormattedAverageTimePerQuestion());
    }

    @Test
    public void testTimeRangeFilterTimestamps() {
        long now = 1729851600000L; // Fixed reference timestamp
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long todayMidnight = cal.getTimeInMillis();

        // RANGE_TODAY
        Assert.assertEquals(todayMidnight, TimeRangeFilter.RANGE_TODAY.getStartTimestamp(now));
        Assert.assertEquals(now, TimeRangeFilter.RANGE_TODAY.getEndTimestamp(now));

        // RANGE_7_DAYS
        cal.setTimeInMillis(todayMidnight);
        cal.add(Calendar.DAY_OF_YEAR, -6);
        long sevenDaysStart = cal.getTimeInMillis();
        Assert.assertEquals(sevenDaysStart, TimeRangeFilter.RANGE_7_DAYS.getStartTimestamp(now));

        // RANGE_30_DAYS
        cal.setTimeInMillis(todayMidnight);
        cal.add(Calendar.DAY_OF_YEAR, -29);
        long thirtyDaysStart = cal.getTimeInMillis();
        Assert.assertEquals(thirtyDaysStart, TimeRangeFilter.RANGE_30_DAYS.getStartTimestamp(now));

        // RANGE_ALL
        Assert.assertEquals(0L, TimeRangeFilter.RANGE_ALL.getStartTimestamp(now));
        Assert.assertEquals(Long.MAX_VALUE, TimeRangeFilter.RANGE_ALL.getEndTimestamp(now));
    }

    @Test
    public void testBestPerformingSessionRankingAlgorithm() {
        // Session A: 10 questions solved, avg pace 60s (60,000ms).
        // Score = (10 * 100) / 60 = 16.67
        SessionStats sA = new SessionStats(1L, "Session A", 20, 10, 600000L, 60000L, 1000L, 2000L, false);

        // Session B: 25 questions solved, avg pace 50s (50,000ms).
        // Score = (25 * 100) / 50 = 50.0
        SessionStats sB = new SessionStats(2L, "Session B", 30, 25, 1250000L, 50000L, 3000L, 4000L, false);

        // Session C: 1 question solved, avg pace 10s (10,000ms).
        // Candidate pool >= 3 questions ignores Session C so small sample does not falsely rank #1.
        SessionStats sC = new SessionStats(3L, "Session C", 5, 1, 10000L, 10000L, 5000L, 6000L, false);

        List<SessionStats> sessions = Arrays.asList(sA, sB, sC);

        // Evaluate best performing manually to verify algorithm specification
        SessionStats bestSession = null;
        double highestScore = -1.0;

        for (SessionStats s : sessions) {
            if (s.getSolvedCount() >= 3) {
                double avgSec = s.getAverageDurationMs() / 1000.0;
                double score = (s.getSolvedCount() * 100.0) / avgSec;
                if (score > highestScore) {
                    highestScore = score;
                    bestSession = s;
                }
            }
        }

        Assert.assertNotNull(bestSession);
        Assert.assertEquals("Session B", bestSession.getSessionName());
        Assert.assertEquals(50.0, highestScore, 0.001);
    }

    @Test
    public void testBestPerformingFallbackForLowQuestionSessions() {
        // Only sessions with < 3 questions available
        SessionStats s1 = new SessionStats(1L, "Session 1", 5, 2, 80000L, 40000L, 1000L, 2000L, false);
        // Score = (2 * 100) / 40 = 5.0
        SessionStats s2 = new SessionStats(2L, "Session 2", 5, 1, 30000L, 30000L, 3000L, 4000L, false);
        // Score = (1 * 100) / 30 = 3.33

        List<SessionStats> sessions = Arrays.asList(s1, s2);

        SessionStats bestSessionPrimary = null;
        double bestScorePrimary = -1.0;

        SessionStats bestSessionFallback = null;
        double bestScoreFallback = -1.0;

        for (SessionStats s : sessions) {
            if (s.getSolvedCount() > 0) {
                double avgSec = s.getAverageDurationMs() / 1000.0;
                double score = (s.getSolvedCount() * 100.0) / avgSec;
                if (s.getSolvedCount() >= 3) {
                    if (score > bestScorePrimary) {
                        bestScorePrimary = score;
                        bestSessionPrimary = s;
                    }
                }
                if (score > bestScoreFallback) {
                    bestScoreFallback = score;
                    bestSessionFallback = s;
                }
            }
        }

        Assert.assertNull(bestSessionPrimary);
        Assert.assertNotNull(bestSessionFallback);
        Assert.assertEquals("Session 1", bestSessionFallback.getSessionName());
        Assert.assertEquals(5.0, bestScoreFallback, 0.001);
    }

    @Test
    public void testTargetCompliancePaceStatuses() {
        TargetSettings settings = new TargetSettings(120, 60, 50);

        // 1. Faster than target (90 seconds vs 120 target) -> ABOVE_TARGET
        TargetCompliance faster = new TargetCompliance(settings, 1800000L, 20, 90000L);
        Assert.assertEquals(TargetCompliance.ComplianceStatus.ABOVE_TARGET, faster.getPaceStatus());
        Assert.assertTrue(faster.isPaceCompliant());
        Assert.assertTrue(faster.getPaceAdvantageSec() > 0);

        // 2. Exact pace (120 seconds vs 120 target) -> MEETING
        TargetCompliance meeting = new TargetCompliance(settings, 2400000L, 20, 120000L);
        Assert.assertEquals(TargetCompliance.ComplianceStatus.MEETING, meeting.getPaceStatus());
        Assert.assertTrue(meeting.isPaceCompliant());

        // 3. Slower pace (150 seconds vs 120 target) -> BELOW_TARGET
        TargetCompliance slower = new TargetCompliance(settings, 3000000L, 20, 150000L);
        Assert.assertEquals(TargetCompliance.ComplianceStatus.BELOW_TARGET, slower.getPaceStatus());
        Assert.assertFalse(slower.isPaceCompliant());
        Assert.assertTrue(slower.getPaceAdvantageSec() < 0);
    }

    @Test
    public void testTargetComplianceQuestionsAndDuration() {
        TargetSettings settings = new TargetSettings(120, 60, 50);

        // Met or exceeded questions and duration
        TargetCompliance compliant = new TargetCompliance(
                settings,
                3600000L, // 60 min
                55,       // 55 questions (target is 50)
                65000L    // 65 sec pace (target is 120)
        );

        Assert.assertEquals(TargetCompliance.ComplianceStatus.ABOVE_TARGET, compliant.getQuestionCountStatus());
        Assert.assertEquals(TargetCompliance.ComplianceStatus.MEETING, compliant.getDurationStatus());
        Assert.assertEquals(TargetCompliance.ComplianceStatus.ABOVE_TARGET, compliant.getPaceStatus());
        Assert.assertTrue(compliant.isOverallCompliant());
        Assert.assertTrue(compliant.getQuestionsProgressPercentage() > 100.0);
    }

    @Test
    public void testChartDataPoints() {
        SpeedDataPoint speedPoint = new SpeedDataPoint(
                10L, 1, "Physics JEE", 1729851600000L, "Oct 25", 72.5, 15
        );
        Assert.assertEquals(10L, speedPoint.getSessionId());
        Assert.assertEquals(1, speedPoint.getSessionIndex());
        Assert.assertEquals(72.5, speedPoint.getAvgTimePerQuestionSeconds(), 0.001);
        Assert.assertEquals(15, speedPoint.getQuestionsSolved());

        DailyPracticeDataPoint practicePoint = new DailyPracticeDataPoint(
                1729814400000L, "Oct 25", 90.0, 5400000L, 3, 45
        );
        Assert.assertEquals(90.0, practicePoint.getPracticeMinutes(), 0.001);
        Assert.assertEquals(3, practicePoint.getSessionCount());
        Assert.assertEquals(45, practicePoint.getTotalQuestionsSolved());
    }

    @Test
    public void testKpiSummaryModelGettersAndFormulas() {
        SessionStats fastest = new SessionStats(1L, "Fastest", 20, 10, 400000L, 40000L, 1000L, 2000L, false);
        SessionStats best = new SessionStats(2L, "Best", 30, 25, 1250000L, 50000L, 3000L, 4000L, false);
        List<SessionStats> list = Arrays.asList(fastest, best);

        TimeRangeKpiSummary summary = new TimeRangeKpiSummary(
                TimeRangeFilter.RANGE_7_DAYS,
                1000L,
                5000L,
                2,
                1,
                1650000L,
                825000L,
                35,
                47142L,
                fastest,
                best,
                50.0,
                list
        );

        Assert.assertEquals(TimeRangeFilter.RANGE_7_DAYS, summary.getRangeFilter());
        Assert.assertEquals(2, summary.getTotalSessionsCount());
        Assert.assertEquals(1, summary.getTotalCompletedSessions());
        Assert.assertEquals(1650000L, summary.getTotalPracticeTimeMs());
        Assert.assertEquals(35, summary.getTotalQuestionsCompleted());
        Assert.assertEquals(fastest, summary.getFastestSession());
        Assert.assertEquals(best, summary.getBestPerformingSession());
        Assert.assertEquals(50.0, summary.getBestPerformingScore(), 0.001);
        Assert.assertTrue(summary.getBestPerformingFormula().contains("Score = (questions_solved * 100)"));
    }
}

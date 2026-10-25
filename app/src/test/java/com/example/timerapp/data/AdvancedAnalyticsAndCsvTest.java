package com.example.timerapp.data;

import com.example.timerapp.data.analytics.AnalyticsMath;
import com.example.timerapp.data.backup.CsvManager;
import com.example.timerapp.data.model.Achievement;
import com.example.timerapp.data.model.AdaptiveTargetSuggestion;
import com.example.timerapp.data.model.DailyPracticeDataPoint;
import com.example.timerapp.data.model.DataIntegrityReport;
import com.example.timerapp.data.model.DateSessionGroup;
import com.example.timerapp.data.model.ExamCountdown;
import com.example.timerapp.data.model.HeatmapDayData;
import com.example.timerapp.data.model.MovingAveragePoint;
import com.example.timerapp.data.model.PerformanceTrend;
import com.example.timerapp.data.model.PeriodReport;
import com.example.timerapp.data.model.PersonalRecords;
import com.example.timerapp.data.model.QuestionLog;
import com.example.timerapp.data.model.Session;
import com.example.timerapp.data.model.SessionResumeData;
import com.example.timerapp.data.model.SessionStats;
import com.example.timerapp.data.model.Subject;
import com.example.timerapp.data.model.SubjectPracticeStats;
import com.example.timerapp.data.model.TimeSlotStats;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive JUnit 4 test suite verifying:
 * 1. Speed improvement percentage math (positive when faster, zero-division handling)
 * 2. Consistency score (0-100 weighted formula)
 * 3. Focus score calculations and pace decay
 * 4. Streak calculations (consecutive days, duration threshold, calendar deduplication, break detection)
 * 5. Rolling moving averages (7-session and 30-session sliding windows)
 * 6. Personal records aggregation (longest session, max daily duration, max daily sessions, fastest pace, longest streak)
 * 7. PerformanceTrend enum evaluation (+-5% pacing thresholds)
 * 8. RFC 4180 CSV export and multiline parsing with escaped quotes and commas
 * 9. Session resume logic (e.g. 70 solved resumes at question 71)
 * 10. Data integrity diagnostic auditing (orphaned logs, negative durations, corrupt sessions)
 */
public class AdvancedAnalyticsAndCsvTest {

    // =========================================================================
    // 1. Speed Improvement Percentage Tests
    // =========================================================================

    @Test
    public void testSpeedImprovementPercentage_fasterPace() {
        // Previous pace = 90.0s, current pace = 72.0s
        // Improvement = ((90.0 - 72.0) / 90.0) * 100.0 = +20.0%
        double prevPace = 90.0;
        double currPace = 72.0;

        double improvement = AnalyticsMath.calculateSpeedImprovementPercentage(prevPace, currPace);
        Assert.assertEquals(20.0, improvement, 0.001);
        Assert.assertTrue("Faster pace must yield a positive percentage", improvement > 0);
    }

    @Test
    public void testSpeedImprovementPercentage_slowerPace() {
        // Previous pace = 60.0s, current pace = 75.0s
        // Improvement = ((60.0 - 75.0) / 60.0) * 100.0 = -25.0%
        double prevPace = 60.0;
        double currPace = 75.0;

        double improvement = AnalyticsMath.calculateSpeedImprovementPercentage(prevPace, currPace);
        Assert.assertEquals(-25.0, improvement, 0.001);
        Assert.assertTrue("Slower pace must yield a negative percentage", improvement < 0);
    }

    @Test
    public void testSpeedImprovementPercentage_divisionByZeroAndIdenticalPace() {
        // Prev pace <= 0: division by zero protection
        Assert.assertEquals(0.0, AnalyticsMath.calculateSpeedImprovementPercentage(0.0, 50.0), 0.001);
        Assert.assertEquals(0.0, AnalyticsMath.calculateSpeedImprovementPercentage(-10.0, 50.0), 0.001);

        // Identical pace
        Assert.assertEquals(0.0, AnalyticsMath.calculateSpeedImprovementPercentage(60.0, 60.0), 0.001);
    }

    // =========================================================================
    // 2. Consistency Score Tests
    // =========================================================================

    @Test
    public void testConsistencyScore_perfectAdherence() {
        // 7 active days out of 7, 10 sessions met target out of 10
        // Score = (7/7 * 60) + (10/10 * 40) = 100.0
        double score = AnalyticsMath.calculateConsistencyScore(7, 7, 10, 10);
        Assert.assertEquals(100.0, score, 0.001);
    }

    @Test
    public void testConsistencyScore_partialAndZeroEdgeCases() {
        // 5 active days out of 7 (42.857 pts), 4 met targets out of 8 (20.0 pts) -> 62.857
        double score = AnalyticsMath.calculateConsistencyScore(5, 7, 4, 8);
        double expected = (5.0 / 7.0 * 60.0) + (4.0 / 8.0 * 40.0);
        Assert.assertEquals(expected, score, 0.001);

        // Zero days and zero sessions: handles division by zero safely
        Assert.assertEquals(0.0, AnalyticsMath.calculateConsistencyScore(0, 0, 0, 0), 0.001);

        // Clamp checks: active days > total days should not exceed 100
        Assert.assertEquals(100.0, AnalyticsMath.calculateConsistencyScore(10, 7, 15, 10), 0.001);
    }

    // =========================================================================
    // 3. Focus Score Tests
    // =========================================================================

    @Test
    public void testFocusScore_calculationAndDecay() {
        // Full completion on target pace: 30 of 30 questions, 50s pace vs 60s target
        double perfectScore = AnalyticsMath.calculateFocusScore(30, 30, 50.0, 60.0);
        Assert.assertEquals(100.0, perfectScore, 0.001);

        // Slower pace penalty: 30 of 30 questions, but actual 90s vs 60s target
        // Question component = 50.0, Pace component penalty = ((90-60)/60)*50 = 25.0 -> Pace score = 25.0
        // Total focus score = 75.0
        double slowerScore = AnalyticsMath.calculateFocusScore(30, 30, 90.0, 60.0);
        Assert.assertEquals(75.0, slowerScore, 0.001);

        // Zero questions solved
        Assert.assertEquals(0.0, AnalyticsMath.calculateFocusScore(0, 30, 45.0, 60.0), 0.001);
    }

    // =========================================================================
    // 4. Streak Calculation Tests
    // =========================================================================

    @Test
    public void testStreakCalculation_consecutiveDays() {
        long oneDay = 24L * 3600 * 1000L;
        long now = 1729851600000L; // Fixed reference timestamp
        long todayMidnight = AnalyticsMath.getMidnightMillis(now);

        // 5 consecutive days of practice ending today
        List<Long> timestamps = Arrays.asList(
                todayMidnight - (4 * oneDay) + 1000L,
                todayMidnight - (3 * oneDay) + 2000L,
                todayMidnight - (2 * oneDay) + 3000L,
                todayMidnight - (1 * oneDay) + 4000L,
                todayMidnight + 5000L
        );

        int[] streaks = AnalyticsMath.calculateStreaks(timestamps, null, 0L, now);
        Assert.assertEquals(5, streaks[0]); // Current active streak
        Assert.assertEquals(5, streaks[1]); // Longest streak
    }

    @Test
    public void testStreakCalculation_brokenStreakWithGap() {
        long oneDay = 24L * 3600 * 1000L;
        long now = 1729851600000L;
        long todayMidnight = AnalyticsMath.getMidnightMillis(now);

        // Days: Today (day 0), Day -1, Day -3, Day -4, Day -5 (Gap at Day -2)
        List<Long> timestamps = Arrays.asList(
                todayMidnight - (5 * oneDay),
                todayMidnight - (4 * oneDay),
                todayMidnight - (3 * oneDay),
                // Day -2 missed
                todayMidnight - (1 * oneDay),
                todayMidnight
        );

        int[] streaks = AnalyticsMath.calculateStreaks(timestamps, null, 0L, now);
        Assert.assertEquals(2, streaks[0]); // Current active streak: Day -1 and Today = 2
        Assert.assertEquals(3, streaks[1]); // Longest streak: Day -5, -4, -3 = 3
    }

    @Test
    public void testStreakCalculation_minimumDurationFilter() {
        long oneDay = 24L * 3600 * 1000L;
        long now = 1729851600000L;
        long todayMidnight = AnalyticsMath.getMidnightMillis(now);

        long day0 = todayMidnight;
        long day1 = todayMidnight - oneDay;
        long day2 = todayMidnight - (2 * oneDay);

        List<Long> timestamps = Arrays.asList(day2 + 100L, day1 + 100L, day0 + 100L);

        Map<Long, Long> dayDurations = new HashMap<>();
        dayDurations.put(day2, 900000L);  // 15 min (qualifies)
        dayDurations.put(day1, 300000L);  // 5 min (disqualified: < 10 min threshold)
        dayDurations.put(day0, 1200000L); // 20 min (qualifies)

        long min10MinThreshold = 600000L; // 10 minutes
        int[] streaks = AnalyticsMath.calculateStreaks(timestamps, dayDurations, min10MinThreshold, now);

        // Because day1 only had 5 min, the streak is broken
        Assert.assertEquals(1, streaks[0]); // Current streak is 1 (only today)
        Assert.assertEquals(1, streaks[1]); // Longest streak is 1
    }

    @Test
    public void testStreakCalculation_sameDayMultipleSessionsDeduplication() {
        long now = 1729851600000L;
        long todayMidnight = AnalyticsMath.getMidnightMillis(now);

        // 4 sessions all conducted on the same calendar day
        List<Long> timestamps = Arrays.asList(
                todayMidnight + 1000L,
                todayMidnight + 10000L,
                todayMidnight + 50000L,
                todayMidnight + 70000L
        );

        int[] streaks = AnalyticsMath.calculateStreaks(timestamps, null, 0L, now);
        Assert.assertEquals(1, streaks[0]);
        Assert.assertEquals(1, streaks[1]);
    }

    // =========================================================================
    // 5. Moving Average Calculations Tests
    // =========================================================================

    @Test
    public void testMovingAverages_7SessionRollingWindow() {
        // Paces in seconds: 70, 65, 60, 55, 50, 45, 40, 35, 30, 25
        double[] paces = {70.0, 65.0, 60.0, 55.0, 50.0, 45.0, 40.0, 35.0, 30.0, 25.0};
        List<SessionStats> sessions = new ArrayList<>();

        for (int i = 0; i < paces.length; i++) {
            long paceMs = (long) (paces[i] * 1000.0);
            long totalMs = paceMs * 10; // 10 questions solved each
            sessions.add(new SessionStats((long) (i + 1), "Session " + (i + 1), 10, 10,
                    totalMs, paceMs, 1000L * i, 1000L * i + totalMs, false));
        }

        List<MovingAveragePoint> maPoints = AnalyticsMath.calculateMovingAverages(sessions, 7);

        Assert.assertEquals(10, maPoints.size());

        // Point 1: 1 session in window -> rolling avg pace = 70.0s
        Assert.assertEquals(70.0, maPoints.get(0).getRollingAvgPaceSeconds(), 0.001);
        Assert.assertEquals(1, maPoints.get(0).getWindowSize());

        // Point 7: 7 sessions in window -> avg of (70,65,60,55,50,45,40) = 385 / 7 = 55.0s
        Assert.assertEquals(55.0, maPoints.get(6).getRollingAvgPaceSeconds(), 0.001);
        Assert.assertEquals(7, maPoints.get(6).getWindowSize());

        // Point 8: 7 sessions window (drops 70, includes 35) -> avg of (65,60,55,50,45,40,35) = 350 / 7 = 50.0s
        Assert.assertEquals(50.0, maPoints.get(7).getRollingAvgPaceSeconds(), 0.001);
        Assert.assertEquals(7, maPoints.get(7).getWindowSize());

        // Point 10: 7 sessions window -> avg of (55,50,45,40,35,30,25) = 280 / 7 = 40.0s
        Assert.assertEquals(40.0, maPoints.get(9).getRollingAvgPaceSeconds(), 0.001);
        Assert.assertEquals(7, maPoints.get(9).getWindowSize());
    }

    @Test
    public void testMovingAverages_30SessionRollingWindowWithShortDataset() {
        // Dataset smaller than window size (3 sessions vs window 30)
        List<SessionStats> sessions = Arrays.asList(
                new SessionStats(1L, "S1", 10, 5, 300000L, 60000L, 1000L, 2000L, false), // 60s
                new SessionStats(2L, "S2", 10, 5, 200000L, 40000L, 3000L, 4000L, false), // 40s
                new SessionStats(3L, "S3", 10, 5, 100000L, 20000L, 5000L, 6000L, false)  // 20s
        );

        List<MovingAveragePoint> maPoints = AnalyticsMath.calculateMovingAverages(sessions, 30);
        Assert.assertEquals(3, maPoints.size());

        // Point 3: avg of 60, 40, 20 = 40.0s
        Assert.assertEquals(40.0, maPoints.get(2).getRollingAvgPaceSeconds(), 0.001);
        Assert.assertEquals(3, maPoints.get(2).getWindowSize());
    }

    // =========================================================================
    // 6. Personal Records Calculations Tests
    // =========================================================================

    @Test
    public void testPersonalRecords_calculation() {
        SessionStats s1 = new SessionStats(1L, "Short Practice", 20, 10, 600000L, 60000L, 1000L, 2000L, false);
        SessionStats s2 = new SessionStats(2L, "Marathon Session", 50, 45, 3600000L, 80000L, 3000L, 4000L, false); // Longest
        SessionStats s3 = new SessionStats(3L, "Sprint Session", 15, 15, 450000L, 30000L, 5000L, 6000L, false);   // Fastest pace: 30s

        List<SessionStats> sessions = Arrays.asList(s1, s2, s3);

        DateSessionGroup d1 = new DateSessionGroup("2026-10-24", "October 24, 2026", 1729728000000L,
                2, 2000000L, 25, Collections.emptyList());
        DateSessionGroup d2 = new DateSessionGroup("2026-10-25", "October 25, 2026", 1729814400000L,
                4, 4500000L, 60, Collections.emptyList()); // Most practice & most sessions

        List<DateSessionGroup> dateGroups = Arrays.asList(d1, d2);

        PersonalRecords pr = AnalyticsMath.calculatePersonalRecords(sessions, dateGroups, 12);

        Assert.assertNotNull(pr.getLongestSession());
        Assert.assertEquals("Marathon Session", pr.getLongestSession().getSessionName());
        Assert.assertEquals(3600000L, pr.getLongestSession().getTotalDurationMs());

        Assert.assertNotNull(pr.getFastestPaceSession());
        Assert.assertEquals("Sprint Session", pr.getFastestPaceSession().getSessionName());
        Assert.assertEquals(30000L, pr.getFastestPaceSession().getAverageDurationMs());

        Assert.assertEquals("October 25, 2026", pr.getMostPracticeDayLabel());
        Assert.assertEquals(4500000L, pr.getMostPracticeDayDurationMs());
        Assert.assertEquals(4, pr.getMostSessionsDayCount());
        Assert.assertEquals(12, pr.getLongestStreakDays());
    }

    // =========================================================================
    // 7. PerformanceTrend Enum Evaluation Tests
    // =========================================================================

    @Test
    public void testPerformanceTrend_improvingStableDecliningThresholds() {
        double baselinePace = 100.0; // 100 seconds per question

        // Accelerating by > 5%: 94.0s -> improvement = 6.0% -> IMPROVING
        Assert.assertEquals(PerformanceTrend.IMPROVING,
                AnalyticsMath.evaluatePerformanceTrend(baselinePace, 94.0));

        // Exact boundary +5%: 95.0s -> improvement = 5.0% -> IMPROVING
        Assert.assertEquals(PerformanceTrend.IMPROVING,
                AnalyticsMath.evaluatePerformanceTrend(baselinePace, 95.0));

        // Consistent pacing within +-5%: 98.0s -> improvement = 2.0% -> STABLE
        Assert.assertEquals(PerformanceTrend.STABLE,
                AnalyticsMath.evaluatePerformanceTrend(baselinePace, 98.0));

        // Consistent pacing: 102.0s -> improvement = -2.0% -> STABLE
        Assert.assertEquals(PerformanceTrend.STABLE,
                AnalyticsMath.evaluatePerformanceTrend(baselinePace, 102.0));

        // Decelerating by > 5%: 106.0s -> improvement = -6.0% -> DECLINING
        Assert.assertEquals(PerformanceTrend.DECLINING,
                AnalyticsMath.evaluatePerformanceTrend(baselinePace, 106.0));

        // Exact boundary -5%: 105.263s -> improvement = -5.26% -> DECLINING
        Assert.assertEquals(PerformanceTrend.DECLINING,
                AnalyticsMath.evaluatePerformanceTrend(baselinePace, 105.5));

        // Non-positive baselines default to STABLE
        Assert.assertEquals(PerformanceTrend.STABLE,
                AnalyticsMath.evaluatePerformanceTrend(0.0, 80.0));
    }

    // =========================================================================
    // 8. RFC 4180 CSV Export and Parsing Tests
    // =========================================================================

    @Test
    public void testRfc4180CsvExport_escapingCommasQuotesAndNewlines() {
        SessionStats session = new SessionStats(
                1L,
                "JEE Mock Test, Physics & Math", // Contains comma
                30,
                25,
                1800000L,
                72000L,
                1700000000000L,
                1700001800000L,
                false
        );
        session.setNotes("Great focus! \"Solved all electrostatics\"\nNeeds improvement: Optics");

        String csv = CsvManager.exportSessionsToCsv(Collections.singletonList(session));

        Assert.assertTrue(csv.startsWith(CsvManager.SESSION_CSV_HEADER));
        // Verify comma escaping in name
        Assert.assertTrue(csv.contains("\"JEE Mock Test, Physics & Math\""));
        // Verify quotes escaping in notes ("" -> escaped)
        Assert.assertTrue(csv.contains("\"\"Solved all electrostatics\"\""));
        // Verify multiline newline preserved inside quotes
        Assert.assertTrue(csv.contains("\nNeeds improvement: Optics"));
    }

    @Test
    public void testRfc4180CsvParser_multilineAndEscapedQuotesRoundtrip() throws IOException {
        String testCsv =
                "SessionId,SessionName,Notes\r\n" +
                "101,\"Physics, Mechanics\",\"Line 1: Excellent\r\nLine 2: \"\"Focus on vectors\"\"\"\r\n" +
                "102,Chemistry,Standard note\r\n";

        List<List<String>> parsed = CsvManager.parseCsv(testCsv);

        Assert.assertEquals(3, parsed.size()); // Header + 2 data rows

        // Verify Row 1 (Header)
        Assert.assertEquals("SessionId", parsed.get(0).get(0));
        Assert.assertEquals("SessionName", parsed.get(0).get(1));
        Assert.assertEquals("Notes", parsed.get(0).get(2));

        // Verify Row 2 (Data row with comma and multiline quote)
        Assert.assertEquals("101", parsed.get(1).get(0));
        Assert.assertEquals("Physics, Mechanics", parsed.get(1).get(1));
        String multilineNote = parsed.get(1).get(2);
        Assert.assertTrue(multilineNote.contains("Line 1: Excellent"));
        Assert.assertTrue(multilineNote.contains("Line 2: \"Focus on vectors\""));

        // Verify Row 3
        Assert.assertEquals("102", parsed.get(2).get(0));
        Assert.assertEquals("Chemistry", parsed.get(2).get(1));
        Assert.assertEquals("Standard note", parsed.get(2).get(2));
    }

    @Test
    public void testQuestionLogCsvExportAndParseRoundtrip() throws IOException {
        List<QuestionLog> logs = new ArrayList<>();
        logs.add(new QuestionLog(1L, 10L, 1, 45000L, 1700000045000L));
        logs.add(new QuestionLog(2L, 10L, 2, 62000L, 1700000107000L));
        logs.add(new QuestionLog(3L, 10L, 3, 38500L, 1700000145500L));

        String csv = CsvManager.exportQuestionLogsToCsv(logs);
        Assert.assertTrue(csv.startsWith(CsvManager.QUESTION_LOG_CSV_HEADER));

        List<List<String>> parsed = CsvManager.parseCsv(csv);
        Assert.assertEquals(4, parsed.size()); // Header + 3 log rows

        // Check Row 1
        Assert.assertEquals("10", parsed.get(1).get(0));
        Assert.assertEquals("1", parsed.get(1).get(1));
        Assert.assertEquals("45000", parsed.get(1).get(2));

        // Check Row 3
        Assert.assertEquals("10", parsed.get(3).get(0));
        Assert.assertEquals("3", parsed.get(3).get(1));
        Assert.assertEquals("38500", parsed.get(3).get(2));
    }

    // =========================================================================
    // 9. Session Resume Logic Tests
    // =========================================================================

    @Test
    public void testSessionResumeLogic_comprehensiveScenarios() {
        Session session = new Session(100L, "JEE 2026 Mathematics", 50, 1000L, 2000L, true);

        // Scenario A: 70 questions solved -> next question must be 71
        List<QuestionLog> logs70 = new ArrayList<>();
        long totalDuration = 0L;
        for (int i = 1; i <= 70; i++) {
            long duration = 40000L + (i * 500L);
            totalDuration += duration;
            logs70.add(new QuestionLog(i, 100L, i, duration, 2000L + i));
        }

        SessionResumeData resume70 = new SessionResumeData(
                session,
                logs70.size(),
                logs70.get(logs70.size() - 1).getQuestionNum() + 1,
                totalDuration,
                totalDuration / logs70.size(),
                logs70
        );

        Assert.assertEquals(70, resume70.getSolvedCount());
        Assert.assertEquals(71, resume70.getNextQuestionNum());
        Assert.assertEquals(totalDuration, resume70.getTotalElapsedMs());
        Assert.assertEquals(totalDuration / 70, resume70.getAverageDurationMs());

        // Scenario B: 0 questions solved -> next question must be 1
        SessionResumeData resume0 = new SessionResumeData(
                session,
                0,
                1,
                0L,
                0L,
                Collections.emptyList()
        );

        Assert.assertEquals(0, resume0.getSolvedCount());
        Assert.assertEquals(1, resume0.getNextQuestionNum());
        Assert.assertEquals(0L, resume0.getTotalElapsedMs());
        Assert.assertEquals(0L, resume0.getAverageDurationMs());
    }

    // =========================================================================
    // 10. Data Integrity Diagnostics Tests
    // =========================================================================

    @Test
    public void testDataIntegrityDiagnostics_orphanedLogsAndCorruptSessions() {
        Session s1 = new Session(1L, "Valid Physics", 30, 1000L, 2000L, true);
        Session s2 = new Session(2L, "Valid Chemistry", 25, 2000L, 3000L, false);

        // Corrupt sessions:
        Session sCorrupt1 = new Session(3L, "", 20, 1000L, 2000L, false);        // Empty name
        Session sCorrupt2 = new Session(4L, "Bad Target", -5, 1000L, 2000L, false); // Negative target
        Session sCorrupt3 = new Session(5L, "Time Inverted", 10, 3000L, 1000L, false); // updated_at < created_at

        List<Session> sessions = Arrays.asList(s1, s2, sCorrupt1, sCorrupt2, sCorrupt3);

        List<QuestionLog> logs = new ArrayList<>();
        logs.add(new QuestionLog(1L, 1L, 1, 45000L, 2000L)); // Valid
        logs.add(new QuestionLog(2L, 2L, 1, 50000L, 3000L)); // Valid
        logs.add(new QuestionLog(3L, 999L, 1, 30000L, 4000L)); // Orphaned: session 999 does not exist
        logs.add(new QuestionLog(4L, 999L, 2, 40000L, 5000L)); // Orphaned: session 999 does not exist
        logs.add(new QuestionLog(5L, 1L, 2, -5000L, 6000L));  // Negative duration anomaly

        DataIntegrityReport report = AnalyticsMath.diagnoseIntegrity(sessions, logs);

        Assert.assertFalse("Dataset with anomalies must not report healthy", report.isHealthy());
        Assert.assertEquals(2, report.getOrphanedLogsRemoved());
        Assert.assertEquals(1, report.getNegativeDurationsFixed());
        Assert.assertEquals(3, report.getCorruptSessionsFixed());
        Assert.assertEquals(6, report.getTotalRepairs());
        Assert.assertEquals(6, report.getDiagnosticMessages().size());
        Assert.assertTrue(report.getSummaryText().contains("Repaired 2 orphaned logs"));
    }

    @Test
    public void testDataIntegrityDiagnostics_healthyDataset() {
        Session s1 = new Session(1L, "Valid Session", 20, 1000L, 2000L, true);
        QuestionLog l1 = new QuestionLog(1L, 1L, 1, 60000L, 2000L);

        DataIntegrityReport report = AnalyticsMath.diagnoseIntegrity(
                Collections.singletonList(s1),
                Collections.singletonList(l1)
        );

        Assert.assertTrue(report.isHealthy());
        Assert.assertEquals(0, report.getTotalRepairs());
        Assert.assertTrue(report.getSummaryText().contains("Zero anomalies detected"));
    }

    // =========================================================================
    // 11. Heatmap Intensity Levels Tests
    // =========================================================================

    @Test
    public void testHeatmapIntensityLevels() {
        // Thresholds:
        // <= 0 min: 0
        // < 30 min: 1
        // < 60 min: 2
        // < 120 min: 3
        // >= 120 min: 4
        Assert.assertEquals(0, HeatmapDayData.calculateIntensity(0.0));
        Assert.assertEquals(0, HeatmapDayData.calculateIntensity(-5.0));

        Assert.assertEquals(1, HeatmapDayData.calculateIntensity(0.1));
        Assert.assertEquals(1, HeatmapDayData.calculateIntensity(15.0));
        Assert.assertEquals(1, HeatmapDayData.calculateIntensity(29.99));

        Assert.assertEquals(2, HeatmapDayData.calculateIntensity(30.0));
        Assert.assertEquals(2, HeatmapDayData.calculateIntensity(45.0));
        Assert.assertEquals(2, HeatmapDayData.calculateIntensity(59.99));

        Assert.assertEquals(3, HeatmapDayData.calculateIntensity(60.0));
        Assert.assertEquals(3, HeatmapDayData.calculateIntensity(90.0));
        Assert.assertEquals(3, HeatmapDayData.calculateIntensity(119.99));

        Assert.assertEquals(4, HeatmapDayData.calculateIntensity(120.0));
        Assert.assertEquals(4, HeatmapDayData.calculateIntensity(240.0));

        HeatmapDayData day = new HeatmapDayData(25, 1729851600000L, 75.0, 4500000L, 2, 40);
        Assert.assertEquals(25, day.getDayOfMonth());
        Assert.assertEquals(1729851600000L, day.getDateMillis());
        Assert.assertEquals(75.0, day.getPracticeMinutes(), 0.001);
        Assert.assertEquals(4500000L, day.getPracticeDurationMs());
        Assert.assertEquals(2, day.getSessionCount());
        Assert.assertEquals(40, day.getTotalQuestionsSolved());
        Assert.assertEquals(3, day.getIntensityLevel()); // 75 min -> level 3
    }

    // =========================================================================
    // 12. Time Slot Mapping Tests (24-Hour Coverage)
    // =========================================================================

    @Test
    public void testTimeSlotStats_allHoursSlotMapping() {
        // Early Morning: 04:00 - 08:00
        for (int h = 4; h < 8; h++) {
            Assert.assertEquals("Hour " + h + " must be EARLY_MORNING",
                    TimeSlotStats.SlotType.EARLY_MORNING, TimeSlotStats.SlotType.fromHour(h));
        }

        // Morning: 08:00 - 12:00
        for (int h = 8; h < 12; h++) {
            Assert.assertEquals("Hour " + h + " must be MORNING",
                    TimeSlotStats.SlotType.MORNING, TimeSlotStats.SlotType.fromHour(h));
        }

        // Afternoon: 12:00 - 17:00
        for (int h = 12; h < 17; h++) {
            Assert.assertEquals("Hour " + h + " must be AFTERNOON",
                    TimeSlotStats.SlotType.AFTERNOON, TimeSlotStats.SlotType.fromHour(h));
        }

        // Evening: 17:00 - 21:00
        for (int h = 17; h < 21; h++) {
            Assert.assertEquals("Hour " + h + " must be EVENING",
                    TimeSlotStats.SlotType.EVENING, TimeSlotStats.SlotType.fromHour(h));
        }

        // Night: 21:00 - 04:00 (21..23, 0..3)
        int[] nightHours = {21, 22, 23, 0, 1, 2, 3};
        for (int h : nightHours) {
            Assert.assertEquals("Hour " + h + " must be NIGHT",
                    TimeSlotStats.SlotType.NIGHT, TimeSlotStats.SlotType.fromHour(h));
        }

        TimeSlotStats morningStats = new TimeSlotStats(
                TimeSlotStats.SlotType.MORNING,
                5,
                7200000L,
                100,
                72000L,
                45000L
        );
        Assert.assertEquals("Morning", morningStats.getSlotName());
        Assert.assertEquals("08:00 - 12:00", morningStats.getHoursRange());
        Assert.assertEquals(5, morningStats.getSessionCount());
        Assert.assertEquals(7200000L, morningStats.getTotalPracticeTimeMs());
        Assert.assertEquals(100, morningStats.getTotalQuestionsSolved());
        Assert.assertEquals(72000L, morningStats.getAveragePaceMs());
        Assert.assertEquals(45000L, morningStats.getBestPaceMs());
    }

    // =========================================================================
    // 13. Period Report Model Tests
    // =========================================================================

    @Test
    public void testPeriodReport_weeklyAndMonthlyAggregation() {
        DailyPracticeDataPoint day1 = new DailyPracticeDataPoint(1700000000000L, "Mon", 45.0, 2700000L, 2, 30);
        DailyPracticeDataPoint day2 = new DailyPracticeDataPoint(1700086400000L, "Tue", 60.0, 3600000L, 3, 40);

        PeriodReport weekly = new PeriodReport(
                PeriodReport.PeriodType.WEEKLY,
                "Week 43 (Oct 2026)",
                1700000000000L,
                1700604800000L,
                5,
                6300000L,
                1260000L,
                70,
                90000L,
                15.5,
                "Tue",
                60.0,
                85.0,
                Arrays.asList(day1, day2)
        );

        Assert.assertEquals(PeriodReport.PeriodType.WEEKLY, weekly.getPeriodType());
        Assert.assertEquals("Week 43 (Oct 2026)", weekly.getPeriodLabel());
        Assert.assertEquals(5, weekly.getSessionCount());
        Assert.assertEquals(6300000L, weekly.getTotalPracticeTimeMs());
        Assert.assertEquals(1260000L, weekly.getAverageSessionDurationMs());
        Assert.assertEquals(70, weekly.getTotalQuestionsSolved());
        Assert.assertEquals(90000L, weekly.getAveragePaceMs());
        Assert.assertEquals(15.5, weekly.getComparisonPercentageVsPrevious(), 0.001);
        Assert.assertEquals("Tue", weekly.getBestPerformingDayLabel());
        Assert.assertEquals(60.0, weekly.getBestPerformingDayMinutes(), 0.001);
        Assert.assertEquals(85.0, weekly.getConsistencyScore(), 0.001);
        Assert.assertEquals(2, weekly.getDailyBreakdown().size());
    }

    // =========================================================================
    // 14. Exam Countdown Tests
    // =========================================================================

    @Test
    public void testExamCountdown_daysRemainingAndExpiration() {
        long now = 1729851600000L;
        long oneDayMs = 24L * 3600 * 1000L;

        // Exam in 45 days
        long examDate = now + (45L * oneDayMs) + 3600000L; // 45 days + 1 hr
        ExamCountdown countdown = new ExamCountdown(1L, "JEE Advanced 2026", examDate, "Paper 1 & 2");

        Assert.assertEquals(1L, countdown.getId());
        Assert.assertEquals("JEE Advanced 2026", countdown.getTitle());
        Assert.assertEquals("Paper 1 & 2", countdown.getDescription());
        Assert.assertEquals(45L, countdown.getDaysRemaining(now));
        Assert.assertFalse(countdown.isExpired(now));

        // Past exam
        long pastDate = now - (2L * oneDayMs);
        ExamCountdown expiredCountdown = new ExamCountdown(2L, "Past Mock", pastDate, "Completed");
        Assert.assertEquals(0L, expiredCountdown.getDaysRemaining(now));
        Assert.assertTrue(expiredCountdown.isExpired(now));
    }

    // =========================================================================
    // 15. Adaptive Target Suggestion Tests
    // =========================================================================

    @Test
    public void testAdaptiveTargetSuggestion_defaultsAndProgression() {
        AdaptiveTargetSuggestion defaultSuggestion = new AdaptiveTargetSuggestion();
        Assert.assertEquals(60, defaultSuggestion.getSuggestedDailyMinutes());
        Assert.assertEquals(50, defaultSuggestion.getSuggestedQuestionsPerDay());
        Assert.assertEquals(10.0, defaultSuggestion.getProgressionPercentage(), 0.001);

        AdaptiveTargetSuggestion custom = new AdaptiveTargetSuggestion(
                45.0, // current daily avg minutes
                50,   // suggested minutes (+10% rounded)
                40,   // suggested questions
                10.0,
                "Based on 7-day rolling median of 45 mins with +10% progression"
        );

        Assert.assertEquals(45.0, custom.getCurrentDailyAverageMinutes(), 0.001);
        Assert.assertEquals(50, custom.getSuggestedDailyMinutes());
        Assert.assertEquals(40, custom.getSuggestedQuestionsPerDay());
        Assert.assertEquals(10.0, custom.getProgressionPercentage(), 0.001);
        Assert.assertTrue(custom.getReasoning().contains("7-day rolling median"));
    }

    // =========================================================================
    // 16. Subject & Subject Practice Stats Tests
    // =========================================================================

    @Test
    public void testSubjectAndSubjectPracticeStats() {
        Subject s = new Subject("Physics", "#4A90E2");
        Assert.assertEquals("Physics", s.getName());
        Assert.assertEquals("#4A90E2", s.getColorHex());

        Subject defaultSubject = new Subject();
        Assert.assertEquals("#8E8E93", defaultSubject.getColorHex());

        SubjectPracticeStats stats = new SubjectPracticeStats(
                "Mathematics",
                "#FF9500",
                3600000L,
                4,
                60,
                60000L,
                40.0
        );

        Assert.assertEquals("Mathematics", stats.getSubject());
        Assert.assertEquals("#FF9500", stats.getColorHex());
        Assert.assertEquals(3600000L, stats.getPracticeTimeMs());
        Assert.assertEquals(4, stats.getSessionCount());
        Assert.assertEquals(60, stats.getQuestionsSolved());
        Assert.assertEquals(60000L, stats.getAverageQuestionPaceMs());
        Assert.assertEquals(40.0, stats.getPercentageOfTotalPracticeTime(), 0.001);

        // Fallback for null subject name
        SubjectPracticeStats fallback = new SubjectPracticeStats(null, null, 1000L, 1, 1, 1000L, 10.0);
        Assert.assertEquals("General", fallback.getSubject());
        Assert.assertEquals("#8E8E93", fallback.getColorHex());
    }

    // =========================================================================
    // 17. Achievement Model Tests
    // =========================================================================

    @Test
    public void testAchievement_modelAndFields() {
        long now = 1729851600000L;
        Achievement achievement = new Achievement(10L, "streak_7", "7-Day Streak Warrior", now);

        Assert.assertEquals(10L, achievement.getId());
        Assert.assertEquals("streak_7", achievement.getKey());
        Assert.assertEquals("7-Day Streak Warrior", achievement.getTitle());
        Assert.assertEquals(now, achievement.getUnlockedAt());
    }

    // =========================================================================
    // 18. CSV Edge Cases & Defensive Parsing Tests
    // =========================================================================

    @Test
    public void testCsvManager_emptyAndEdgeCaseInputs() throws IOException {
        // Empty CSV returns empty list
        Assert.assertTrue(CsvManager.parseCsv("").isEmpty());
        Assert.assertTrue(CsvManager.parseCsv("   \n\r\n").isEmpty());

        // Header only returns 1 row
        List<List<String>> headerOnly = CsvManager.parseCsv("Col1,Col2,Col3\r\n");
        Assert.assertEquals(1, headerOnly.size());
        Assert.assertEquals(3, headerOnly.get(0).size());

        // Session with null notes and special chars in name
        SessionStats s = new SessionStats(
                5L, "Math & \"Calculus\", Advanced", 10, 5, 300000L, 60000L, 1000L, 2000L, false
        );
        s.setNotes(null); // null notes should export safely as empty string

        String exported = CsvManager.exportSessionsToCsv(Collections.singletonList(s));
        Assert.assertNotNull(exported);

        List<List<String>> parsed = CsvManager.parseCsv(exported);
        Assert.assertEquals(2, parsed.size()); // Header + 1 record
        List<String> record = parsed.get(1);
        Assert.assertEquals("5", record.get(0));
        Assert.assertEquals("Math & \"Calculus\", Advanced", record.get(1));
        // Check session name preserved with escaped quotes and commas in raw CSV
        Assert.assertTrue(exported.contains("\"Math & \"\"Calculus\"\", Advanced\""));
    }
}

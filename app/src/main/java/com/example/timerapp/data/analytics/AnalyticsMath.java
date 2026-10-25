package com.example.timerapp.data.analytics;

import com.example.timerapp.data.model.DataIntegrityReport;
import com.example.timerapp.data.model.DateSessionGroup;
import com.example.timerapp.data.model.MovingAveragePoint;
import com.example.timerapp.data.model.PerformanceTrend;
import com.example.timerapp.data.model.PersonalRecords;
import com.example.timerapp.data.model.QuestionLog;
import com.example.timerapp.data.model.Session;
import com.example.timerapp.data.model.SessionStats;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Pure mathematical and algorithmic utility functions for timer analytics.
 * Zero external dependencies; fully compatible with Android and standalone JVM unit testing.
 */
public final class AnalyticsMath {

    private AnalyticsMath() {
        // Prevent instantiation
    }

    /**
     * Calculates speed improvement percentage comparing previous/baseline pace to current/recent pace.
     * Formula: ((prevAvgPace - currAvgPace) / prevAvgPace) * 100.0
     * Positive value indicates acceleration / faster pace.
     * Negative value indicates deceleration / slower pace.
     *
     * @param prevAvgPaceSeconds Previous or baseline pace in seconds.
     * @param currAvgPaceSeconds Current or recent pace in seconds.
     * @return Improvement percentage. Returns 0.0 if prevAvgPaceSeconds <= 0.
     */
    public static double calculateSpeedImprovementPercentage(double prevAvgPaceSeconds, double currAvgPaceSeconds) {
        if (prevAvgPaceSeconds <= 0.0) {
            return 0.0;
        }
        return ((prevAvgPaceSeconds - currAvgPaceSeconds) / prevAvgPaceSeconds) * 100.0;
    }

    /**
     * Calculates Consistency Score (0.0 to 100.0) based on practice day adherence and target completion rate.
     * Formula: (active_days / total_days * 60.0) + (met_targets / total_sessions * 40.0)
     *
     * @param activeDays    Number of calendar days with study practice.
     * @param totalDays     Total calendar days in the observation window.
     * @param metTargets    Number of sessions where question target was achieved.
     * @param totalSessions Total number of sessions completed.
     * @return Consistency score clamped between 0.0 and 100.0.
     */
    public static double calculateConsistencyScore(int activeDays, int totalDays, int metTargets, int totalSessions) {
        double dayScore = 0.0;
        if (totalDays > 0) {
            double dayRatio = Math.min(1.0, Math.max(0.0, (double) activeDays / totalDays));
            dayScore = dayRatio * 60.0;
        }

        double targetScore = 0.0;
        if (totalSessions > 0) {
            double targetRatio = Math.min(1.0, Math.max(0.0, (double) metTargets / totalSessions));
            targetScore = targetRatio * 40.0;
        }

        double total = dayScore + targetScore;
        return Math.max(0.0, Math.min(100.0, total));
    }

    /**
     * Calculates Focus Score (0.0 to 100.0) combining target question completion and pace adherence.
     *
     * @param solvedQuestions Questions solved in the session.
     * @param targetQuestions Target questions planned.
     * @param actualPaceSec   Actual average pace in seconds per question.
     * @param targetPaceSec   Target pace in seconds per question.
     * @return Clamped focus score.
     */
    public static double calculateFocusScore(int solvedQuestions, int targetQuestions, double actualPaceSec, double targetPaceSec) {
        if (solvedQuestions <= 0) {
            return 0.0;
        }

        double questionRatio = (targetQuestions > 0)
                ? Math.min(1.0, (double) solvedQuestions / targetQuestions)
                : 1.0;
        double questionScore = questionRatio * 50.0;

        double paceScore = 50.0;
        if (targetPaceSec > 0 && actualPaceSec > 0) {
            if (actualPaceSec <= targetPaceSec) {
                paceScore = 50.0; // Met or exceeded target pace
            } else {
                // Decay pace score proportionally as pace slows down
                double penalty = ((actualPaceSec - targetPaceSec) / targetPaceSec) * 50.0;
                paceScore = Math.max(0.0, 50.0 - penalty);
            }
        }

        return Math.max(0.0, Math.min(100.0, questionScore + paceScore));
    }

    /**
     * Calculates current and longest consecutive study streaks.
     * A day qualifies if it has >= 1 session or total practice >= minDailyPracticeMs (e.g. 10 min).
     *
     * @param sessionTimestamps  Epoch timestamps of all sessions.
     * @param dayPracticeDurationMs Map of midnight timestamp -> total duration in ms.
     * @param minDailyPracticeMs Minimum duration threshold (e.g. 600,000 ms for 10 min), or 0 to count any session.
     * @param referenceNowMs     Current time reference for checking active streak.
     * @return int array [currentStreakDays, longestStreakDays].
     */
    public static int[] calculateStreaks(List<Long> sessionTimestamps, Map<Long, Long> dayPracticeDurationMs,
                                         long minDailyPracticeMs, long referenceNowMs) {
        if (sessionTimestamps == null || sessionTimestamps.isEmpty()) {
            return new int[]{0, 0};
        }

        // Collect qualifying calendar day midnights in ascending order
        TreeSet<Long> qualifyingDays = new TreeSet<>();

        for (Long ts : sessionTimestamps) {
            if (ts == null || ts <= 0) continue;
            long midnight = getMidnightMillis(ts);

            if (minDailyPracticeMs > 0 && dayPracticeDurationMs != null) {
                Long duration = dayPracticeDurationMs.get(midnight);
                if (duration != null && duration >= minDailyPracticeMs) {
                    qualifyingDays.add(midnight);
                }
            } else {
                qualifyingDays.add(midnight);
            }
        }

        if (qualifyingDays.isEmpty()) {
            return new int[]{0, 0};
        }

        List<Long> sortedDays = new ArrayList<>(qualifyingDays);

        int maxStreak = 0;
        int currentStreak = 0;
        int tempStreak = 1;

        long oneDayMs = 24L * 60 * 60 * 1000L;

        for (int i = 1; i < sortedDays.size(); i++) {
            long prev = sortedDays.get(i - 1);
            long curr = sortedDays.get(i);

            // Check if days are consecutive (allow small daylight saving variance: 23h - 25h)
            long diff = curr - prev;
            if (diff >= (23L * 3600 * 1000L) && diff <= (25L * 3600 * 1000L)) {
                tempStreak++;
            } else {
                if (tempStreak > maxStreak) {
                    maxStreak = tempStreak;
                }
                tempStreak = 1;
            }
        }
        if (tempStreak > maxStreak) {
            maxStreak = tempStreak;
        }

        // Evaluate active streak relative to referenceNowMs
        long todayMidnight = getMidnightMillis(referenceNowMs);
        long yesterdayMidnight = todayMidnight - oneDayMs;

        long lastQualifyingDay = sortedDays.get(sortedDays.size() - 1);

        if (lastQualifyingDay == todayMidnight || lastQualifyingDay == yesterdayMidnight) {
            // Count backward from last qualifying day
            int activeCount = 1;
            for (int i = sortedDays.size() - 1; i > 0; i--) {
                long curr = sortedDays.get(i);
                long prev = sortedDays.get(i - 1);
                long diff = curr - prev;
                if (diff >= (23L * 3600 * 1000L) && diff <= (25L * 3600 * 1000L)) {
                    activeCount++;
                } else {
                    break;
                }
            }
            currentStreak = activeCount;
        } else {
            currentStreak = 0; // Streak broken
        }

        return new int[]{currentStreak, maxStreak};
    }

    /**
     * Calculates rolling moving average pace and duration over a sliding window.
     *
     * @param chronologicalSessions Sessions in chronological order (oldest to newest).
     * @param windowSize            Rolling window size (e.g. 7 or 30).
     * @return List of {@link MovingAveragePoint} data points.
     */
    public static List<MovingAveragePoint> calculateMovingAverages(List<SessionStats> chronologicalSessions, int windowSize) {
        if (chronologicalSessions == null || chronologicalSessions.isEmpty() || windowSize <= 0) {
            return Collections.emptyList();
        }

        List<MovingAveragePoint> result = new ArrayList<>(chronologicalSessions.size());
        SimpleDateFormat labelFormat = new SimpleDateFormat("MMM d", Locale.getDefault());

        List<SessionStats> validSessions = new ArrayList<>();
        for (SessionStats s : chronologicalSessions) {
            if (s.getSolvedCount() > 0) {
                validSessions.add(s);
            }
        }

        for (int i = 0; i < validSessions.size(); i++) {
            SessionStats curr = validSessions.get(i);
            int startIdx = Math.max(0, i - windowSize + 1);
            int countInWindow = i - startIdx + 1;

            double sumPaceSec = 0.0;
            double sumDurationMin = 0.0;

            for (int k = startIdx; k <= i; k++) {
                SessionStats w = validSessions.get(k);
                sumPaceSec += (w.getAverageDurationMs() / 1000.0);
                sumDurationMin += (w.getTotalDurationMs() / 60000.0);
            }

            double rollingAvgPace = sumPaceSec / countInWindow;
            double rollingAvgDuration = sumDurationMin / countInWindow;

            long ts = curr.getCreatedAt() > 0 ? curr.getCreatedAt() : curr.getUpdatedAt();
            String dateLabel = labelFormat.format(new Date(ts));

            result.add(new MovingAveragePoint(
                    i + 1,
                    curr.getSessionId(),
                    curr.getSessionName(),
                    ts,
                    dateLabel,
                    rollingAvgPace,
                    rollingAvgDuration,
                    countInWindow
            ));
        }

        return result;
    }

    /**
     * Evaluates performance pacing trend comparing recent sessions moving average pace to baseline.
     * Improvement >= +5% -> IMPROVING
     * Decline <= -5% -> DECLINING
     * Within [-5%, +5%] -> STABLE
     */
    public static PerformanceTrend evaluatePerformanceTrend(double baselinePaceSec, double recentPaceSec) {
        if (baselinePaceSec <= 0.0 || recentPaceSec <= 0.0) {
            return PerformanceTrend.STABLE;
        }

        double improvement = calculateSpeedImprovementPercentage(baselinePaceSec, recentPaceSec);
        if (improvement >= 5.0) {
            return PerformanceTrend.IMPROVING;
        } else if (improvement <= -5.0) {
            return PerformanceTrend.DECLINING;
        } else {
            return PerformanceTrend.STABLE;
        }
    }

    /**
     * Calculates personal records across sessions and date groups.
     */
    public static PersonalRecords calculatePersonalRecords(List<SessionStats> sessions,
                                                           List<DateSessionGroup> dateGroups,
                                                           int longestStreakDays) {
        PersonalRecords records = new PersonalRecords();
        records.setLongestStreakDays(longestStreakDays);

        if (sessions != null && !sessions.isEmpty()) {
            SessionStats longestSession = null;
            SessionStats fastestSession = null;
            long maxDuration = -1L;
            long minAvgPace = Long.MAX_VALUE;

            for (SessionStats s : sessions) {
                if (s.getTotalDurationMs() > maxDuration) {
                    maxDuration = s.getTotalDurationMs();
                    longestSession = s;
                }

                if (s.getSolvedCount() >= 1) {
                    if (s.getAverageDurationMs() < minAvgPace) {
                        minAvgPace = s.getAverageDurationMs();
                        fastestSession = s;
                    }
                }
            }

            records.setLongestSession(longestSession);
            records.setFastestPaceSession(fastestSession);
        }

        if (dateGroups != null && !dateGroups.isEmpty()) {
            DateSessionGroup maxPracticeDay = null;
            DateSessionGroup maxSessionsDay = null;

            long maxDayDuration = -1L;
            int maxDaySessions = -1;

            for (DateSessionGroup g : dateGroups) {
                if (g.getTotalPracticeDurationMs() > maxDayDuration) {
                    maxDayDuration = g.getTotalPracticeDurationMs();
                    maxPracticeDay = g;
                }
                if (g.getTotalSessionsCount() > maxDaySessions) {
                    maxDaySessions = g.getTotalSessionsCount();
                    maxSessionsDay = g;
                }
            }

            if (maxPracticeDay != null) {
                records.setMostPracticeDayMillis(maxPracticeDay.getRawDateMillis());
                records.setMostPracticeDayLabel(maxPracticeDay.getFormattedDate());
                records.setMostPracticeDayDurationMs(maxPracticeDay.getTotalPracticeDurationMs());
            }

            if (maxSessionsDay != null) {
                records.setMostSessionsDayMillis(maxSessionsDay.getRawDateMillis());
                records.setMostSessionsDayLabel(maxSessionsDay.getFormattedDate());
                records.setMostSessionsDayCount(maxSessionsDay.getTotalSessionsCount());
            }
        }

        return records;
    }

    /**
     * Audits dataset integrity, detecting orphaned logs, negative durations, and corrupt sessions.
     */
    public static DataIntegrityReport diagnoseIntegrity(List<Session> sessions, List<QuestionLog> logs) {
        DataIntegrityReport report = new DataIntegrityReport();

        Set<Long> validSessionIds = new HashSet<>();
        int corruptSessions = 0;

        if (sessions != null) {
            for (Session s : sessions) {
                validSessionIds.add(s.getId());
                boolean isCorrupt = false;

                if (s.getName() == null || s.getName().trim().isEmpty()) {
                    isCorrupt = true;
                    report.addDiagnosticMessage("Session id=" + s.getId() + " has empty/null name.");
                }
                if (s.getTargetCount() < 0) {
                    isCorrupt = true;
                    report.addDiagnosticMessage("Session id=" + s.getId() + " has negative target count (" + s.getTargetCount() + ").");
                }
                if (s.getUpdatedAt() < s.getCreatedAt()) {
                    isCorrupt = true;
                    report.addDiagnosticMessage("Session id=" + s.getId() + " has updated_at < created_at.");
                }

                if (isCorrupt) {
                    corruptSessions++;
                }
            }
        }

        int orphanedLogs = 0;
        int negativeDurations = 0;

        if (logs != null) {
            for (QuestionLog log : logs) {
                if (!validSessionIds.contains(log.getSessionId())) {
                    orphanedLogs++;
                    report.addDiagnosticMessage("Orphaned log id=" + log.getId() + " references unknown sessionId=" + log.getSessionId());
                }
                if (log.getDurationMs() < 0) {
                    negativeDurations++;
                    report.addDiagnosticMessage("Log id=" + log.getId() + " has negative duration (" + log.getDurationMs() + "ms).");
                }
            }
        }

        report.setCorruptSessionsFixed(corruptSessions);
        report.setOrphanedLogsRemoved(orphanedLogs);
        report.setNegativeDurationsFixed(negativeDurations);
        report.setHealthy(corruptSessions == 0 && orphanedLogs == 0 && negativeDurations == 0);

        return report;
    }

    public static long getMidnightMillis(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}

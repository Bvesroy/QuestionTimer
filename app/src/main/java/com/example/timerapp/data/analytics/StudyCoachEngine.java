package com.example.timerapp.data.analytics;

import com.example.timerapp.data.model.ExamProfile;
import com.example.timerapp.data.model.JeeAnalyticsReport;
import com.example.timerapp.data.model.QuestionLog;
import com.example.timerapp.data.model.Session;
import com.example.timerapp.data.model.SessionStats;
import com.example.timerapp.data.model.StudyRecommendation;
import com.example.timerapp.model.TimerSession;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 100% offline, deterministic rules-based Study Coach & JEE Analytics Engine.
 * Generates personalized, statistical recommendations and competitive JEE score projections.
 */
public final class StudyCoachEngine {

    // =========================================================================
    // UI CoachCard Compatibility
    // =========================================================================

    public static class CoachCard {
        public final String id;
        public final String title;
        public final String observation;
        public final String timeframe;
        public final String sampleSize;
        public final String recommendation;
        public final String rationale;
        public final String priority; // HIGH, MEDIUM, INFO

        public CoachCard(String id, String title, String observation, String timeframe,
                         String sampleSize, String recommendation, String rationale, String priority) {
            this.id = id;
            this.title = title;
            this.observation = observation;
            this.timeframe = timeframe;
            this.sampleSize = sampleSize;
            this.recommendation = recommendation;
            this.rationale = rationale;
            this.priority = priority;
        }
    }

    private StudyCoachEngine() {}

    /**
     * Legacy UI method: analyzes session history and produces actionable coaching recommendation cards.
     */
    public static List<CoachCard> generateInsights(List<TimerSession> sessions) {
        List<CoachCard> cards = new ArrayList<>();
        if (sessions == null || sessions.isEmpty()) {
            cards.add(createInsufficientDataCard(0));
            return cards;
        }

        List<com.example.timerapp.model.QuestionLog> allLogs = new ArrayList<>();
        Map<String, Integer> subjectCount = new HashMap<>();
        Map<String, Integer> mistakeCounts = new HashMap<>();
        long totalDurationMs = 0L;

        for (TimerSession s : sessions) {
            String subj = (s.getSubject() != null && !s.getSubject().isEmpty()) ? s.getSubject() : "General";
            for (com.example.timerapp.model.QuestionLog log : s.getLogs()) {
                allLogs.add(log);
                totalDurationMs += log.getDurationMs();
                subjectCount.put(subj, subjectCount.getOrDefault(subj, 0) + 1);

                if (log.getMistakeTag() != null && !log.getMistakeTag().isEmpty()) {
                    String m = log.getMistakeTag().toUpperCase(Locale.ROOT);
                    mistakeCounts.put(m, mistakeCounts.getOrDefault(m, 0) + 1);
                }
            }
        }

        int totalQuestions = allLogs.size();
        if (totalQuestions < 5) {
            cards.add(createInsufficientDataCard(totalQuestions));
            return cards;
        }

        long avgPaceSec = (totalDurationMs / totalQuestions) / 1000;

        // 1. Pacing & Speed vs Target
        if (avgPaceSec > 160) {
            cards.add(new CoachCard(
                    "card_pacing_high",
                    "Pacing Optimization",
                    String.format(Locale.ROOT, "Average solve pace is %ds/question (exceeding standard JEE target of 144s).", avgPaceSec),
                    "Overall History",
                    String.format(Locale.ROOT, "%d questions analyzed", totalQuestions),
                    "Adopt the 2-pass strategy: clear single-concept problems first (<90s) and defer calculation-heavy proofs.",
                    "JEE Main tests quick execution across 75 questions in 180 minutes. Improving initial question triage increases overall attempt volume.",
                    "HIGH"
            ));
        } else if (avgPaceSec < 90) {
            cards.add(new CoachCard(
                    "card_pacing_fast",
                    "Rapid Pace Warning",
                    String.format(Locale.ROOT, "Average solve pace is exceptionally fast at %ds/question.", avgPaceSec),
                    "Recent Sessions",
                    String.format(Locale.ROOT, "%d questions analyzed", totalQuestions),
                    "Verify final calculation steps before hitting next question to guard against careless sign and formula traps.",
                    "High speed without accuracy verification frequently leads to negative marking (-1) in competitive scoring.",
                    "MEDIUM"
            ));
        }

        // 2. Cognitive Fatigue & Second-Half Drop-off
        long firstHalfDuration = 0L;
        int firstHalfCount = 0;
        long secondHalfDuration = 0L;
        int secondHalfCount = 0;

        for (TimerSession s : sessions) {
            List<com.example.timerapp.model.QuestionLog> sLogs = s.getLogs();
            if (sLogs.size() >= 4) {
                int mid = sLogs.size() / 2;
                for (int i = 0; i < mid; i++) {
                    firstHalfDuration += sLogs.get(i).getDurationMs();
                    firstHalfCount++;
                }
                for (int i = mid; i < sLogs.size(); i++) {
                    secondHalfDuration += sLogs.get(i).getDurationMs();
                    secondHalfCount++;
                }
            }
        }

        if (firstHalfCount >= 4 && secondHalfCount >= 4) {
            long avgFirst = firstHalfDuration / firstHalfCount;
            long avgSecond = secondHalfDuration / secondHalfCount;
            if (avgSecond > (long) (avgFirst * 1.30)) {
                int pctSlower = (int) (((avgSecond - avgFirst) / (float) avgFirst) * 100);
                cards.add(new CoachCard(
                        "card_fatigue_dropoff",
                        "Stamina & Fatigue Management",
                        String.format(Locale.ROOT, "Second-half solve speed slows down by +%d%% compared to session start.", pctSlower),
                        "Multi-Question Sessions",
                        String.format(Locale.ROOT, "%d questions analyzed", (firstHalfCount + secondHalfCount)),
                        "Implement structured 3 to 5-minute hydration and eye-relief breaks every 25 minutes of continuous study.",
                        "Cognitive stamina decays under continuous problem-solving pressure. Micro-breaks restore short-term working memory capacity.",
                        "HIGH"
                ));
            }
        }

        // 3. Subject Asymmetry
        if (subjectCount.size() >= 2) {
            int maxSubjCount = 0;
            String maxSubj = "";
            int minSubjCount = Integer.MAX_VALUE;
            String minSubj = "";

            for (Map.Entry<String, Integer> e : subjectCount.entrySet()) {
                if (e.getValue() > maxSubjCount) {
                    maxSubjCount = e.getValue();
                    maxSubj = e.getKey();
                }
                if (e.getValue() < minSubjCount) {
                    minSubjCount = e.getValue();
                    minSubj = e.getKey();
                }
            }

            if (maxSubjCount >= 10 && minSubjCount <= (maxSubjCount / 3) && !maxSubj.equals(minSubj)) {
                cards.add(new CoachCard(
                        "card_subject_asymmetry",
                        "Subject Distribution Balance",
                        String.format(Locale.ROOT, "Heavy focus on %s (%d Qs) vs limited practice in %s (%d Qs).", maxSubj, maxSubjCount, minSubj, minSubjCount),
                        "Last 30 Days",
                        String.format(Locale.ROOT, "%d total subject questions", totalQuestions),
                        String.format(Locale.ROOT, "Allocate upcoming practice slots strictly to %s to balance syllabus readiness.", minSubj),
                        "JEE percentiles depend on clearing subject-wise cutoffs and maintaining a balanced overall score across Physics, Chemistry, and Mathematics.",
                        "MEDIUM"
                ));
            }
        }

        // 4. Mistake Categorization Insights
        int calcMistakes = mistakeCounts.getOrDefault("CALCULATION", 0) + mistakeCounts.getOrDefault("CARELESS / SILLY", 0);
        int conceptMistakes = mistakeCounts.getOrDefault("CONCEPTUAL", 0) + mistakeCounts.getOrDefault("MISREAD QUESTION", 0);

        if (calcMistakes >= 3) {
            cards.add(new CoachCard(
                    "card_mistake_calc",
                    "Arithmetic & Scratchpad Discipline",
                    String.format(Locale.ROOT, "%d tagged calculation or careless errors identified.", calcMistakes),
                    "Tagged Questions",
                    String.format(Locale.ROOT, "%d error tags logged", calcMistakes),
                    "Write key intermediate arithmetic steps clearly on your rough sheet instead of computing mental shortcuts under time stress.",
                    "Silly errors in algebra and unit conversion are the #1 contributor to negative marks in JEE Advanced.",
                    "HIGH"
            ));
        }

        if (conceptMistakes >= 3) {
            cards.add(new CoachCard(
                    "card_mistake_concept",
                    "Conceptual Foundation Review",
                    String.format(Locale.ROOT, "%d tagged conceptual or question misread errors identified.", conceptMistakes),
                    "Tagged Questions",
                    String.format(Locale.ROOT, "%d error tags logged", conceptMistakes),
                    "Flag these questions for end-of-week revision; re-derive fundamental formulas and boundary conditions before attempting similar problems.",
                    "Identifying knowledge gaps in theory prevents recurring mistakes across mock tests.",
                    "MEDIUM"
            ));
        }

        if (cards.isEmpty()) {
            cards.add(new CoachCard(
                    "card_steady_progress",
                    "Consistent Performance",
                    String.format(Locale.ROOT, "Pacing is steady at %ds/Q with balanced subject allocation.", avgPaceSec),
                    "Recent Sessions",
                    String.format(Locale.ROOT, "%d questions analyzed", totalQuestions),
                    "Continue maintaining this study rhythm and progressively challenge yourself with JEE Advanced multi-concept problem sets.",
                    "Consistent daily deliberate practice is the proven driver of high JEE percentiles.",
                    "INFO"
            ));
        }

        return cards;
    }

    private static CoachCard createInsufficientDataCard(int currentCount) {
        return new CoachCard(
                "card_insufficient_data",
                "Baseline Assessment",
                String.format(Locale.ROOT, "Only %d question%s logged so far.", currentCount, currentCount == 1 ? "" : "s"),
                "Initial Onboarding",
                String.format(Locale.ROOT, "%d / 5 questions required", currentCount),
                "Solve at least 5 questions across 2 study sessions to unlock personalized algorithmic coach recommendations.",
                "The statistical coach engine requires representative solve samples to reliably identify pacing trends, fatigue points, and subject balance.",
                "INFO"
        );
    }

    // =========================================================================
    // Master Statistical Study Recommendations Engine
    // =========================================================================

    /**
     * Evaluates stored session logs against target pacing and goals, producing
     * actionable study recommendations with statistical confidence levels.
     */
    public static List<StudyRecommendation> evaluateRecommendations(
            List<SessionStats> sessions,
            List<QuestionLog> logs,
            int targetPaceSeconds,
            int targetDailyQuestions) {

        List<StudyRecommendation> recommendations = new ArrayList<>();

        // 1. Sample Size Safety Guard (< 5 questions or < 2 sessions)
        int totalQuestions = (logs != null) ? logs.size() : 0;
        int totalSessions = (sessions != null) ? sessions.size() : 0;

        if (totalQuestions < 5 || totalSessions < 2) {
            recommendations.add(StudyRecommendation.createInsufficientData(totalQuestions, "Current Window"));
            return recommendations;
        }

        // 2. Median vs Target Pace
        StudyRecommendation paceRec = evaluateMedianPace(logs, targetPaceSeconds);
        if (paceRec != null) recommendations.add(paceRec);

        // 3. Speed vs Accuracy Trade-off
        StudyRecommendation speedAccRec = evaluateSpeedAccuracyTradeoff(logs);
        if (speedAccRec != null) recommendations.add(speedAccRec);

        // 4. Subject Outlier Detection (>35% slower than user average)
        StudyRecommendation subjectOutlierRec = evaluateSubjectOutliers(sessions, logs);
        if (subjectOutlierRec != null) recommendations.add(subjectOutlierRec);

        // 5. Mistake Pattern Clustering
        StudyRecommendation mistakeRec = evaluateMistakePatterns(logs);
        if (mistakeRec != null) recommendations.add(mistakeRec);

        // 6. Target Feasibility
        StudyRecommendation targetRec = evaluateTargetFeasibility(sessions, targetDailyQuestions);
        if (targetRec != null) recommendations.add(targetRec);

        // 7. Best Study Hour
        StudyRecommendation hourRec = evaluateBestStudyHour(logs);
        if (hourRec != null) recommendations.add(hourRec);

        return recommendations;
    }

    /**
     * Algorithm 1: Median vs Target Pace.
     */
    public static StudyRecommendation evaluateMedianPace(List<QuestionLog> logs, int targetPaceSeconds) {
        if (logs == null || logs.size() < 5 || targetPaceSeconds <= 0) return null;

        List<Double> paces = new ArrayList<>();
        for (QuestionLog q : logs) {
            if (q.getDurationMs() > 0) {
                paces.add(q.getDurationMs() / 1000.0);
            }
        }
        if (paces.size() < 5) return null;
        Collections.sort(paces);

        double medianPaceSec = paces.get(paces.size() / 2);
        double ratio = (medianPaceSec - targetPaceSeconds) / (double) targetPaceSeconds;

        if (ratio > 0.15) { // Median pace > 15% slower than target
            int pctSlower = (int) Math.round(ratio * 100);
            return new StudyRecommendation(
                    "pace_adherence",
                    "Pace Below Target Speed",
                    String.format(Locale.US, "Your median solving pace is %.1fs, which is %d%% slower than your %ds target.",
                            medianPaceSec, pctSlower, targetPaceSeconds),
                    "All Sessions",
                    logs.size(),
                    "Practice 10-question sprint sets with strict time cutoffs, and skip lengthy questions during your first pass.",
                    "Sustained solving above target pace during practice creates severe time deficits in full 3-hour competitive exams.",
                    StudyRecommendation.ConfidenceLevel.HIGH
            );
        }
        return null;
    }

    /**
     * Algorithm 2: Speed vs Accuracy Trade-off.
     */
    public static StudyRecommendation evaluateSpeedAccuracyTradeoff(List<QuestionLog> logs) {
        if (logs == null || logs.size() < 8) return null;

        List<QuestionLog> evaluated = new ArrayList<>();
        for (QuestionLog q : logs) {
            String outcome = q.getOutcome().toUpperCase(Locale.ROOT);
            if (outcome.contains("CORRECT") || outcome.contains("SOLVED") || outcome.contains("INCORRECT") || outcome.contains("WRONG")) {
                evaluated.add(q);
            }
        }
        if (evaluated.size() < 8) return null;

        // Sort by duration
        List<QuestionLog> sorted = new ArrayList<>(evaluated);
        Collections.sort(sorted, (a, b) -> Long.compare(a.getDurationMs(), b.getDurationMs()));

        int mid = sorted.size() / 2;
        int fastErrors = 0;
        int slowErrors = 0;

        for (int i = 0; i < mid; i++) {
            if (isIncorrect(sorted.get(i))) fastErrors++;
        }
        for (int i = mid; i < sorted.size(); i++) {
            if (isIncorrect(sorted.get(i))) slowErrors++;
        }

        double fastErrorRate = (double) fastErrors / mid;
        double slowErrorRate = (double) slowErrors / (sorted.size() - mid);

        if (fastErrorRate > slowErrorRate + 0.18 && fastErrors >= 2) {
            int fastPct = (int) Math.round(fastErrorRate * 100);
            int slowPct = (int) Math.round(slowErrorRate * 100);
            return new StudyRecommendation(
                    "speed_accuracy_tradeoff",
                    "Rushing Causes Accuracy Drops",
                    String.format(Locale.US, "Questions answered in your fastest quartile have an error rate of %d%% vs %d%% on steadier attempts.",
                            fastPct, slowPct),
                    "Recent Questions",
                    evaluated.size(),
                    "Invest an extra 10-15 seconds per question to re-read problem constraints and verify intermediate arithmetic.",
                    "Aggressive speed without pacing calibration leads to negative marking penalties that outweigh speed advantages.",
                    StudyRecommendation.ConfidenceLevel.HIGH
            );
        }
        return null;
    }

    /**
     * Algorithm 3: Subject Outlier Detection (>35% slower than user average).
     */
    public static StudyRecommendation evaluateSubjectOutliers(List<SessionStats> sessions, List<QuestionLog> logs) {
        if (sessions == null || sessions.size() < 3 || logs == null || logs.size() < 6) return null;

        long totalDuration = 0;
        int totalQuestions = 0;
        Map<String, Long> subjectDuration = new HashMap<>();
        Map<String, Integer> subjectCounts = new HashMap<>();

        Map<Long, String> sessionSubjectMap = new HashMap<>();
        for (SessionStats s : sessions) {
            sessionSubjectMap.put(s.getSessionId(), s.getSubject());
        }

        for (QuestionLog q : logs) {
            if (q.getDurationMs() <= 0) continue;
            String subj = sessionSubjectMap.get(q.getSessionId());
            if (subj == null || subj.trim().isEmpty()) subj = "General";

            totalDuration += q.getDurationMs();
            totalQuestions++;

            subjectDuration.put(subj, subjectDuration.getOrDefault(subj, 0L) + q.getDurationMs());
            subjectCounts.put(subj, subjectCounts.getOrDefault(subj, 0) + 1);
        }

        if (totalQuestions < 6) return null;
        double overallAvgSec = (totalDuration / (double) totalQuestions) / 1000.0;

        String slowestSubject = null;
        double slowestSubjectAvg = 0.0;
        double highestDeviation = 0.0;

        for (Map.Entry<String, Integer> entry : subjectCounts.entrySet()) {
            String subj = entry.getKey();
            int count = entry.getValue();
            if (count >= 3) {
                double avgSec = (subjectDuration.get(subj) / (double) count) / 1000.0;
                double deviation = (avgSec - overallAvgSec) / overallAvgSec;
                if (deviation > 0.35 && deviation > highestDeviation) {
                    highestDeviation = deviation;
                    slowestSubject = subj;
                    slowestSubjectAvg = avgSec;
                }
            }
        }

        if (slowestSubject != null) {
            int pctSlower = (int) Math.round(highestDeviation * 100);
            return new StudyRecommendation(
                    "subject_outlier_bottleneck",
                    slowestSubject + " Pace Bottleneck",
                    String.format(Locale.US, "%s questions average %.1fs, which is %d%% slower than your overall average of %.1fs.",
                            slowestSubject, slowestSubjectAvg, pctSlower, overallAvgSec),
                    "All Practice",
                    subjectCounts.get(slowestSubject),
                    String.format(Locale.US, "Dedicate focused formula recall drills and topical test sets to %s to improve baseline speed.", slowestSubject),
                    "Balancing subject speeds ensures time is preserved for high-yield scoring sections during examination.",
                    StudyRecommendation.ConfidenceLevel.HIGH
            );
        }
        return null;
    }

    /**
     * Algorithm 4: Mistake Pattern Clustering.
     */
    public static StudyRecommendation evaluateMistakePatterns(List<QuestionLog> logs) {
        if (logs == null || logs.isEmpty()) return null;

        Map<String, Integer> mistakeCounts = new HashMap<>();
        int totalMistakes = 0;

        for (QuestionLog q : logs) {
            String tag = q.getMistakeTag();
            if (tag != null && !tag.trim().isEmpty()) {
                tag = tag.trim();
                mistakeCounts.put(tag, mistakeCounts.getOrDefault(tag, 0) + 1);
                totalMistakes++;
            }
        }

        if (totalMistakes < 3) return null;

        String dominantTag = null;
        int maxCount = 0;

        for (Map.Entry<String, Integer> e : mistakeCounts.entrySet()) {
            if (e.getValue() > maxCount) {
                maxCount = e.getValue();
                dominantTag = e.getKey();
            }
        }

        if (dominantTag != null && (double) maxCount / totalMistakes >= 0.40) {
            int pct = (int) Math.round(((double) maxCount / totalMistakes) * 100);
            String action;
            String lower = dominantTag.toLowerCase(Locale.ROOT);
            if (lower.contains("calc") || lower.contains("arith")) {
                action = "Write down intermediate step formulas rather than relying solely on mental arithmetic.";
            } else if (lower.contains("concept") || lower.contains("theory")) {
                action = "Review core conceptual derivations and fundamental principles before attempting next problem set.";
            } else if (lower.contains("misread") || lower.contains("question")) {
                action = "Underline the requested units and given boundary values before beginning your calculations.";
            } else {
                action = "Maintain an error log specifically tracking '" + dominantTag + "' mistakes to review before each practice session.";
            }

            return new StudyRecommendation(
                    "dominant_mistake_pattern",
                    "Dominant Mistake: " + dominantTag,
                    String.format(Locale.US, "'%s' accounts for %d%% of your logged errors (%d of %d mistakes).",
                            dominantTag, pct, maxCount, totalMistakes),
                    "Recent Sessions",
                    totalMistakes,
                    action,
                    "Targeting the single most frequent error mode produces immediate double-digit accuracy gains.",
                    StudyRecommendation.ConfidenceLevel.HIGH
            );
        }
        return null;
    }

    /**
     * Algorithm 5: Target Feasibility.
     */
    public static StudyRecommendation evaluateTargetFeasibility(List<SessionStats> sessions, int targetDailyQuestions) {
        if (sessions == null || sessions.size() < 3 || targetDailyQuestions <= 0) return null;

        Map<Long, Integer> dayQuestions = new HashMap<>();
        long oneDayMs = 24L * 3600 * 1000L;

        for (SessionStats s : sessions) {
            long ts = s.getCreatedAt() > 0 ? s.getCreatedAt() : s.getUpdatedAt();
            long midnight = (ts / oneDayMs) * oneDayMs;
            dayQuestions.put(midnight, dayQuestions.getOrDefault(midnight, 0) + s.getSolvedCount());
        }

        if (dayQuestions.size() < 3) return null;

        List<Integer> counts = new ArrayList<>(dayQuestions.values());
        Collections.sort(counts);
        int medianDailySolved = counts.get(counts.size() / 2);

        // Case A: Goal is unrealistically high (> 2.0x median)
        if (targetDailyQuestions > medianDailySolved * 2 && medianDailySolved > 0) {
            int recommendedTarget = Math.max(10, (int) Math.round(medianDailySolved * 1.25));
            return new StudyRecommendation(
                    "target_feasibility",
                    "Daily Goal Overloaded",
                    String.format(Locale.US, "Your target of %d questions is over 2x your current daily median of %d questions.",
                            targetDailyQuestions, medianDailySolved),
                    "Last 7 Active Days",
                    counts.size(),
                    String.format(Locale.US, "Lower daily target to %d questions to maintain sustainable consistency without cognitive fatigue.", recommendedTarget),
                    "Achievable targets build compounding daily momentum, while overambitious goals trigger avoidance habits.",
                    StudyRecommendation.ConfidenceLevel.MEDIUM
            );
        }

        // Case B: Goal is trivially easy (< 0.4x median)
        if (targetDailyQuestions < medianDailySolved * 0.4 && medianDailySolved >= 15) {
            int recommendedTarget = (int) Math.round(medianDailySolved * 1.10);
            return new StudyRecommendation(
                    "target_feasibility",
                    "Daily Goal Ready for Progression",
                    String.format(Locale.US, "You consistently solve %d questions/day, easily outperforming your %d question target.",
                            medianDailySolved, targetDailyQuestions),
                    "Last 7 Active Days",
                    counts.size(),
                    String.format(Locale.US, "Raise your daily target to %d questions for progressive overload.", recommendedTarget),
                    "Periodically advancing practice goals expands test endurance ahead of multi-hour national entrance exams.",
                    StudyRecommendation.ConfidenceLevel.MEDIUM
            );
        }

        return null;
    }

    /**
     * Algorithm 6: Best Study Hour.
     */
    public static StudyRecommendation evaluateBestStudyHour(List<QuestionLog> logs) {
        if (logs == null || logs.size() < 10) return null;

        Map<Integer, List<Long>> hourDurations = new HashMap<>();
        Calendar cal = Calendar.getInstance();

        for (QuestionLog q : logs) {
            if (q.getCompletedAt() > 0 && q.getDurationMs() > 0) {
                cal.setTimeInMillis(q.getCompletedAt());
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                if (!hourDurations.containsKey(hour)) {
                    hourDurations.put(hour, new ArrayList<>());
                }
                hourDurations.get(hour).add(q.getDurationMs());
            }
        }

        int bestHour = -1;
        double bestPace = Double.MAX_VALUE;
        int bestHourCount = 0;

        for (Map.Entry<Integer, List<Long>> entry : hourDurations.entrySet()) {
            if (entry.getValue().size() >= 4) { // At least 4 questions logged in this hour
                long sum = 0;
                for (Long d : entry.getValue()) sum += d;
                double avgSec = (sum / (double) entry.getValue().size()) / 1000.0;
                if (avgSec < bestPace) {
                    bestPace = avgSec;
                    bestHour = entry.getKey();
                    bestHourCount = entry.getValue().size();
                }
            }
        }

        if (bestHour != -1) {
            String timeWindow = String.format(Locale.US, "%02d:00 - %02d:00", bestHour, (bestHour + 1) % 24);
            return new StudyRecommendation(
                    "peak_performance_hour",
                    "Peak Focus Window: " + timeWindow,
                    String.format(Locale.US, "Your fastest solving pace (%.1fs/Q) occurs during %s.", bestPace, timeWindow),
                    "Historical Logs",
                    bestHourCount,
                    "Schedule high-difficulty mock tests and complex numerical sets during this optimal cognitive window.",
                    "Aligning demanding problem sets with your natural circadian peak maximizes retention and accuracy.",
                    StudyRecommendation.ConfidenceLevel.MEDIUM
            );
        }
        return null;
    }

    // =========================================================================
    // JEE Profile Analytics Engine
    // =========================================================================

    /**
     * Computes comprehensive analytical report for JEE Main or JEE Advanced tests.
     */
    public static JeeAnalyticsReport calculateJeeAnalytics(
            String profileName,
            List<Session> sessions,
            List<QuestionLog> logs,
            ExamProfile profile,
            long startMillis,
            long endMillis,
            int targetPaceSeconds) {

        String profName = (profileName != null) ? profileName : "JEE_MAIN";
        int totalExamQuestions = profile != null ? profile.getTotalQuestions() : 75;
        double correctMarks = profile != null ? profile.getCorrectMarks() : 4.0;
        double negativeMarks = profile != null ? profile.getNegativeMarks() : 1.0;

        int attempted = 0;
        int correct = 0;
        int incorrect = 0;
        int skipped = 0;
        long totalStudyTimeMs = 0;
        long totalBreakTimeMs = 0;
        long totalQuestionDurationMs = 0;
        int questionsExceedingTarget = 0;

        Map<String, Long> subjectTimeDist = new HashMap<>();
        Map<String, Integer> subjectCounts = new HashMap<>();
        Map<String, Integer> mistakeDist = new HashMap<>();

        Map<Long, String> sessionSubjectMap = new HashMap<>();
        if (sessions != null) {
            for (Session s : sessions) {
                sessionSubjectMap.put(s.getId(), s.getSubject());
                totalStudyTimeMs += s.getTotalStudyDurationMs();
                totalBreakTimeMs += s.getTotalBreakDurationMs();
            }
        }

        if (logs != null) {
            for (QuestionLog q : logs) {
                if (q.getCompletedAt() >= startMillis && q.getCompletedAt() <= endMillis) {
                    attempted++;
                    totalQuestionDurationMs += q.getDurationMs();

                    if (targetPaceSeconds > 0 && (q.getDurationMs() / 1000) > targetPaceSeconds) {
                        questionsExceedingTarget++;
                    }

                    String outcome = q.getOutcome().toUpperCase(Locale.ROOT);
                    if (outcome.contains("CORRECT") || outcome.contains("SOLVED")) {
                        correct++;
                    } else if (outcome.contains("INCORRECT") || outcome.contains("WRONG")) {
                        incorrect++;
                    } else if (outcome.contains("SKIP")) {
                        skipped++;
                    } else {
                        correct++;
                    }

                    // Subject distribution
                    String subj = q.getTopic();
                    if (subj == null || subj.trim().isEmpty()) {
                        subj = sessionSubjectMap.get(q.getSessionId());
                        if (subj == null || subj.trim().isEmpty()) subj = "General";
                    }
                    subjectTimeDist.put(subj, subjectTimeDist.getOrDefault(subj, 0L) + q.getDurationMs());
                    subjectCounts.put(subj, subjectCounts.getOrDefault(subj, 0) + 1);

                    // Mistake distribution
                    String mistake = q.getMistakeTag();
                    if (mistake != null && !mistake.trim().isEmpty()) {
                        mistakeDist.put(mistake, mistakeDist.getOrDefault(mistake, 0) + 1);
                    }
                }
            }
        }

        double attemptRate = totalExamQuestions > 0 ? (attempted / (double) totalExamQuestions) * 100.0 : 0.0;
        double accuracy = attempted > 0 ? (correct / (double) attempted) * 100.0 : 0.0;
        double avgPaceSec = attempted > 0 ? (totalQuestionDurationMs / (double) attempted) / 1000.0 : 0.0;

        // Score projection: Correct * +marks - Incorrect * -marks
        double projectedScore = Math.max(0.0, (correct * correctMarks) - (incorrect * negativeMarks));
        double maxScore = totalExamQuestions * correctMarks;

        return new JeeAnalyticsReport(
                profName, startMillis, endMillis, totalExamQuestions, attempted,
                correct, incorrect, skipped,
                Math.round(attemptRate * 10.0) / 10.0,
                Math.round(accuracy * 10.0) / 10.0,
                Math.round(avgPaceSec * 10.0) / 10.0,
                questionsExceedingTarget,
                Math.round(projectedScore * 10.0) / 10.0,
                maxScore,
                totalStudyTimeMs,
                totalBreakTimeMs,
                subjectTimeDist,
                subjectCounts,
                mistakeDist
        );
    }

    private static boolean isIncorrect(QuestionLog q) {
        String outcome = q.getOutcome().toUpperCase(Locale.ROOT);
        return outcome.contains("INCORRECT") || outcome.contains("WRONG") ||
                (q.getMistakeTag() != null && !q.getMistakeTag().trim().isEmpty());
    }
}

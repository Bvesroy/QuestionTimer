package com.example.timerapp.data.analytics;

import com.example.timerapp.data.analytics.JeeScoringCalculator.JeeAdvQuestionAttempt;

import java.io.Serializable;
import java.util.List;

/**
 * Comprehensive Estimated Marks and Scoring Calculator for JEE Main and JEE Advanced.
 * Supports:
 * 1. JEE Main (+4 / -1 / 0) standard scoring, negative penalty metrics, and percentile projection.
 * 2. JEE Advanced multi-correct variable marking (+4, +3, +2, +1, -2, 0), single-correct (+3, -1, 0),
 *    and numerical (+3, 0, 0).
 * 3. Subject-wise cutoffs and rank-list qualification evaluation.
 */
public class EstimatedMarksCalculator implements Serializable {
    private static final long serialVersionUID = 1L;

    public static class JeeMainResult implements Serializable {
        private static final long serialVersionUID = 1L;
        public final int totalQuestions;
        public final int correctCount;
        public final int incorrectCount;
        public final int skippedCount;
        public final double netScore;
        public final double maxPossibleScore;
        public final double accuracyPercentage;
        public final double attemptRatePercentage;
        public final double negativeMarksPenalty;
        public final double marksLostFromPotential;
        public final double estimatedPercentile;

        public JeeMainResult(int totalQuestions, int correctCount, int incorrectCount, int skippedCount,
                             double netScore, double maxPossibleScore, double accuracyPercentage,
                             double attemptRatePercentage, double negativeMarksPenalty,
                             double marksLostFromPotential, double estimatedPercentile) {
            this.totalQuestions = totalQuestions;
            this.correctCount = correctCount;
            this.incorrectCount = incorrectCount;
            this.skippedCount = skippedCount;
            this.netScore = netScore;
            this.maxPossibleScore = maxPossibleScore;
            this.accuracyPercentage = accuracyPercentage;
            this.attemptRatePercentage = attemptRatePercentage;
            this.negativeMarksPenalty = negativeMarksPenalty;
            this.marksLostFromPotential = marksLostFromPotential;
            this.estimatedPercentile = estimatedPercentile;
        }
    }

    public static class JeeAdvancedResult implements Serializable {
        private static final long serialVersionUID = 1L;
        public final int totalQuestions;
        public final int attemptedQuestions;
        public final double netScore;
        public final double maxPossibleScore;
        public final double positiveMarks;
        public final double negativeDeductions;
        public final double accuracyPercentage;
        public final boolean qualifiedSubjectPhysics;
        public final boolean qualifiedSubjectChemistry;
        public final boolean qualifiedSubjectMathematics;
        public final boolean qualifiedAggregate;

        public JeeAdvancedResult(int totalQuestions, int attemptedQuestions, double netScore,
                                 double maxPossibleScore, double positiveMarks, double negativeDeductions,
                                 double accuracyPercentage, boolean qualifiedSubjectPhysics,
                                 boolean qualifiedSubjectChemistry, boolean qualifiedSubjectMathematics,
                                 boolean qualifiedAggregate) {
            this.totalQuestions = totalQuestions;
            this.attemptedQuestions = attemptedQuestions;
            this.netScore = netScore;
            this.maxPossibleScore = maxPossibleScore;
            this.positiveMarks = positiveMarks;
            this.negativeDeductions = negativeDeductions;
            this.accuracyPercentage = accuracyPercentage;
            this.qualifiedSubjectPhysics = qualifiedSubjectPhysics;
            this.qualifiedSubjectChemistry = qualifiedSubjectChemistry;
            this.qualifiedSubjectMathematics = qualifiedSubjectMathematics;
            this.qualifiedAggregate = qualifiedAggregate;
        }

        public boolean isFullyQualified() {
            return qualifiedSubjectPhysics && qualifiedSubjectChemistry &&
                    qualifiedSubjectMathematics && qualifiedAggregate;
        }
    }

    /**
     * Calculates complete JEE Main performance and score metrics.
     */
    public static JeeMainResult calculateJeeMain(int correct, int incorrect, int skipped, int totalQuestions) {
        int validTotal = Math.max(correct + incorrect + skipped, totalQuestions);
        if (validTotal <= 0) validTotal = 75; // Default standard JEE Main questions

        double netScore = (correct * 4.0) + (incorrect * -1.0);
        double maxScore = validTotal * 4.0;

        int attempted = correct + incorrect;
        double accuracy = attempted > 0 ? ((double) correct / attempted) * 100.0 : 0.0;
        double attemptRate = ((double) attempted / validTotal) * 100.0;

        double negativePenalty = incorrect * 1.0;
        // Opportunity cost: incorrect costs 4 (unearned) + 1 (penalty) = 5 marks; skipped costs 4 marks.
        double marksLost = (incorrect * 5.0) + (skipped * 4.0);

        double percentile = estimateJeeMainPercentile(netScore);

        return new JeeMainResult(
                validTotal, correct, incorrect, skipped,
                netScore, maxScore, accuracy, attemptRate,
                negativePenalty, marksLost, percentile
        );
    }

    /**
     * Estimates percentile from raw score based on calibrated percentile curve.
     */
    public static double estimateJeeMainPercentile(double score) {
        if (score <= 0.0) return 0.0;
        if (score >= 280.0) {
            return 99.9 + Math.min(0.099, ((score - 280.0) / 20.0) * 0.099);
        } else if (score >= 250.0) {
            return 99.5 + ((score - 250.0) / 30.0) * 0.4;
        } else if (score >= 200.0) {
            return 98.8 + ((score - 200.0) / 50.0) * 0.7;
        } else if (score >= 160.0) {
            return 97.0 + ((score - 160.0) / 40.0) * 1.8;
        } else if (score >= 120.0) {
            return 94.0 + ((score - 120.0) / 40.0) * 3.0;
        } else if (score >= 90.0) {
            return 88.0 + ((score - 90.0) / 30.0) * 6.0;
        } else if (score >= 60.0) {
            return 75.0 + ((score - 60.0) / 30.0) * 13.0;
        } else if (score >= 30.0) {
            return 50.0 + ((score - 30.0) / 30.0) * 25.0;
        } else {
            return Math.max(0.0, (score / 30.0) * 50.0);
        }
    }

    /**
     * Calculates JEE Advanced score breakdown with subject qualification and aggregate cutoff check.
     */
    public static JeeAdvancedResult calculateJeeAdvanced(
            List<JeeAdvQuestionAttempt> attempts,
            double physicsMarks,
            double chemMarks,
            double mathMarks,
            double subjectMaxMarks,
            double aggregateMinPercentage,
            double subjectMinPercentage) {

        int totalQuestions = attempts != null ? attempts.size() : 0;
        int attempted = 0;
        double netScore = 0.0;
        double positiveMarks = 0.0;
        double negativeDeductions = 0.0;
        int fullyOrPartiallyCorrect = 0;

        if (attempts != null) {
            for (JeeAdvQuestionAttempt a : attempts) {
                if (a.isAttempted()) {
                    attempted++;
                }
                double marks = a.calculateMarks();
                netScore += marks;
                if (marks > 0) {
                    positiveMarks += marks;
                    fullyOrPartiallyCorrect++;
                } else if (marks < 0) {
                    negativeDeductions += Math.abs(marks);
                }
            }
        }

        double maxPossibleScore = subjectMaxMarks * 3.0;
        double accuracy = attempted > 0 ? ((double) fullyOrPartiallyCorrect / attempted) * 100.0 : 0.0;

        double subMinReq = subjectMaxMarks * (subjectMinPercentage / 100.0);
        boolean qualP = physicsMarks >= subMinReq;
        boolean qualC = chemMarks >= subMinReq;
        boolean qualM = mathMarks >= subMinReq;

        double aggScore = physicsMarks + chemMarks + mathMarks;
        double aggMinReq = maxPossibleScore * (aggregateMinPercentage / 100.0);
        boolean qualAgg = aggScore >= aggMinReq;

        return new JeeAdvancedResult(
                totalQuestions, attempted, netScore, maxPossibleScore,
                positiveMarks, negativeDeductions, accuracy,
                qualP, qualC, qualM, qualAgg
        );
    }

    public static final String EXAM_JEE_MAIN = "JEE_MAIN";
    public static final String EXAM_JEE_ADVANCED = "JEE_ADVANCED";

    public static class PerformanceAnalytics implements Serializable {
        private static final long serialVersionUID = 1L;
        public final String examType;
        public final int totalQuestions;
        public final int correctCount;
        public final int wrongCount;
        public final int unattemptedCount;
        public final double accuracyPercentage;
        public final long totalSolvingTimeMs;
        public final double averageSolvingTimeSeconds;
        public final double estimatedScore;
        public final double maxPossibleScore;
        public final double negativePenalty;
        public final double targetPaceSeconds;
        public final boolean paceWithinTarget;
        public final long correctSolvingTimeMs;
        public final long incorrectSolvingTimeMs;
        public final double averageCorrectSolvingTimeSeconds;
        public final double averageIncorrectSolvingTimeSeconds;

        public PerformanceAnalytics(String examType, int totalQuestions, int correctCount, int wrongCount,
                                    int unattemptedCount, double accuracyPercentage, long totalSolvingTimeMs,
                                    double averageSolvingTimeSeconds, double estimatedScore,
                                    double maxPossibleScore, double negativePenalty,
                                    double targetPaceSeconds, boolean paceWithinTarget) {
            this(examType, totalQuestions, correctCount, wrongCount, unattemptedCount, accuracyPercentage,
                 totalSolvingTimeMs, averageSolvingTimeSeconds, estimatedScore, maxPossibleScore, negativePenalty,
                 targetPaceSeconds, paceWithinTarget, 0L, 0L, 0.0, 0.0);
        }

        public PerformanceAnalytics(String examType, int totalQuestions, int correctCount, int wrongCount,
                                    int unattemptedCount, double accuracyPercentage, long totalSolvingTimeMs,
                                    double averageSolvingTimeSeconds, double estimatedScore,
                                    double maxPossibleScore, double negativePenalty,
                                    double targetPaceSeconds, boolean paceWithinTarget,
                                    long correctSolvingTimeMs, long incorrectSolvingTimeMs,
                                    double averageCorrectSolvingTimeSeconds, double averageIncorrectSolvingTimeSeconds) {
            this.examType = examType;
            this.totalQuestions = totalQuestions;
            this.correctCount = correctCount;
            this.wrongCount = wrongCount;
            this.unattemptedCount = unattemptedCount;
            this.accuracyPercentage = accuracyPercentage;
            this.totalSolvingTimeMs = totalSolvingTimeMs;
            this.averageSolvingTimeSeconds = averageSolvingTimeSeconds;
            this.estimatedScore = estimatedScore;
            this.maxPossibleScore = maxPossibleScore;
            this.negativePenalty = negativePenalty;
            this.targetPaceSeconds = targetPaceSeconds;
            this.paceWithinTarget = paceWithinTarget;
            this.correctSolvingTimeMs = correctSolvingTimeMs;
            this.incorrectSolvingTimeMs = incorrectSolvingTimeMs;
            this.averageCorrectSolvingTimeSeconds = averageCorrectSolvingTimeSeconds;
            this.averageIncorrectSolvingTimeSeconds = averageIncorrectSolvingTimeSeconds;
        }

        public String getFormattedSummary() {
            String wrongTiming = (wrongCount > 0)
                    ? String.format(java.util.Locale.ROOT, " • WRONG TIME: %.0fs", averageIncorrectSolvingTimeSeconds)
                    : "";
            return String.format(java.util.Locale.ROOT,
                    "ACCURACY: %.0f%% (%d/%d) • %d Right, %d Wrong (%dC %dW) • EST: %s%.0f PTS • AVG: %.0fs%s",
                    accuracyPercentage, correctCount, (correctCount + wrongCount),
                    correctCount, wrongCount, correctCount, wrongCount,
                    estimatedScore >= 0 ? "+" : "", estimatedScore,
                    averageSolvingTimeSeconds, wrongTiming);
        }
    }

    public static double calculateEstimatedScore(String examType, int correctCount, int wrongCount) {
        int validCorrect = Math.max(0, correctCount);
        int validWrong = Math.max(0, wrongCount);
        boolean isAdv = EXAM_JEE_ADVANCED.equalsIgnoreCase(examType);
        double penalty = isAdv ? 2.0 : 1.0;
        return (validCorrect * 4.0) - (validWrong * penalty);
    }

    public static double calculateAccuracy(int correctCount, int totalAttempted) {
        return (totalAttempted > 0) ? (Math.max(0, correctCount) * 100.0 / totalAttempted) : 0.0;
    }

    public static double calculateAverageSolvingTimeSec(long totalSolvingTimeMs, int totalAttempted) {
        return (totalAttempted > 0) ? (totalSolvingTimeMs / 1000.0 / totalAttempted) : 0.0;
    }

    public static PerformanceAnalytics computeAnalytics(String examType, List<com.example.timerapp.model.QuestionLog> logs, int targetCount) {
        int correct = 0;
        int wrong = 0;
        long totalMs = 0L;
        long correctMs = 0L;
        long wrongMs = 0L;
        if (logs != null) {
            for (com.example.timerapp.model.QuestionLog log : logs) {
                // Outcome synonyms (RIGHT/WRONG vs CORRECT/SOLVED/INCORRECT) are centralized here.
                // Unattempted outcomes (null, blank, PAUSED, SKIPPED, REVISED, ...) are ignored entirely so
                // they cannot inflate total solving time or the derived average pace.
                String outcome = log.getOutcome();
                if (!OutcomeClassifier.isAttempted(outcome)) {
                    continue;
                }
                long dur = log.getDurationMs();
                totalMs += dur;
                if (OutcomeClassifier.isCorrect(outcome)) {
                    correct++;
                    correctMs += dur;
                } else {
                    wrong++;
                    wrongMs += dur;
                }
            }
        }
        int attempted = correct + wrong;
        int validTotal = Math.max(attempted, targetCount);
        int unattempted = Math.max(0, validTotal - attempted);
        double accuracy = calculateAccuracy(correct, attempted);
        double avgSec = calculateAverageSolvingTimeSec(totalMs, attempted);
        double avgCorrectSec = (correct > 0) ? (correctMs / 1000.0 / correct) : 0.0;
        double avgWrongSec = (wrong > 0) ? (wrongMs / 1000.0 / wrong) : 0.0;

        boolean isAdv = EXAM_JEE_ADVANCED.equalsIgnoreCase(examType);
        double penaltyPerWrong = isAdv ? 2.0 : 1.0;
        double targetPace = isAdv ? 180.0 : 144.0;
        double estimatedScore = (correct * 4.0) - (wrong * penaltyPerWrong);
        double maxScore = validTotal * 4.0;
        double negativePenalty = wrong * penaltyPerWrong;
        boolean paceWithin = attempted > 0 && avgSec <= targetPace;

        return new PerformanceAnalytics(
                isAdv ? EXAM_JEE_ADVANCED : EXAM_JEE_MAIN,
                validTotal, correct, wrong, unattempted, accuracy,
                totalMs, avgSec, estimatedScore, maxScore, negativePenalty,
                targetPace, paceWithin,
                correctMs, wrongMs, avgCorrectSec, avgWrongSec
        );
    }
}

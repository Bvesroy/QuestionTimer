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
}

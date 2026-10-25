package com.example.timerapp.data.analytics;

import com.example.timerapp.data.model.ExamProfile;
import com.example.timerapp.data.model.JeeAnalyticsReport;
import com.example.timerapp.data.model.Session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scoring and Analytics Calculator for JEE Main and JEE Advanced exam patterns.
 * Supports:
 * 1. JEE Main standard marking (+4 correct, -1 incorrect, 0 unattempted).
 * 2. 3-Hour Exam Pacing standard (180 mins / 75 questions = 144s average pace).
 * 3. JEE Advanced multi-correct & partial marking schemes (+4, +3, +2, +1, -2, 0).
 * 4. Profile separation to isolate JEE Main vs JEE Advanced datasets.
 */
public class JeeScoringCalculator {

    public static final int JEE_MAIN_TOTAL_QUESTIONS = 75;
    public static final int JEE_MAIN_EXAM_MINUTES = 180;
    public static final double JEE_MAIN_TARGET_PACE_SECONDS = (JEE_MAIN_EXAM_MINUTES * 60.0) / JEE_MAIN_TOTAL_QUESTIONS; // 144.0s

    /**
     * Calculates score under standard JEE Main marking rule (+4 correct, -1 incorrect, 0 unattempted).
     *
     * @param correctCount   Number of correct answers.
     * @param incorrectCount Number of incorrect answers.
     * @param skippedCount   Number of unattempted / skipped questions.
     * @return Net JEE Main score.
     */
    public static double calculateJeeMainScore(int correctCount, int incorrectCount, int skippedCount) {
        int validCorrect = Math.max(0, correctCount);
        int validIncorrect = Math.max(0, incorrectCount);
        return (validCorrect * 4.0) + (validIncorrect * -1.0);
    }

    /**
     * Calculates question pacing speed relative to standard 3-hour (180-min) JEE exam limits.
     * Returns true if average pace meets the 144s per question threshold.
     */
    public static boolean isPaceWithinExamLimit(double avgPaceSeconds, int totalQuestions, int durationMinutes) {
        if (totalQuestions <= 0 || durationMinutes <= 0) return false;
        double targetPace = (durationMinutes * 60.0) / totalQuestions;
        return avgPaceSeconds <= targetPace;
    }

    /**
     * Represents the outcome of an individual JEE Advanced question attempt.
     */
    public static class JeeAdvQuestionAttempt {
        public enum QuestionType {
            SINGLE_CORRECT,
            MULTI_CORRECT,
            NUMERICAL
        }

        private final QuestionType type;
        private final int correctOptionsTotal;
        private final int correctOptionsChosen;
        private final int incorrectOptionsChosen;
        private final boolean isAttempted;

        public JeeAdvQuestionAttempt(QuestionType type, int correctOptionsTotal,
                                     int correctOptionsChosen, int incorrectOptionsChosen,
                                     boolean isAttempted) {
            this.type = type;
            this.correctOptionsTotal = correctOptionsTotal;
            this.correctOptionsChosen = correctOptionsChosen;
            this.incorrectOptionsChosen = incorrectOptionsChosen;
            this.isAttempted = isAttempted;
        }

        public QuestionType getType() {
            return type;
        }

        public boolean isAttempted() {
            return isAttempted;
        }

        /**
         * Evaluates marks awarded for this question under standard JEE Advanced rules:
         * - Multi-correct:
         *   +4 if all correct options chosen and no wrong option.
         *   +3 if 3 out of 4 correct options chosen and no wrong option.
         *   +2 if 2 or more correct options chosen and no wrong option (when total correct >= 3).
         *   +1 if 1 correct option chosen and no wrong option (when total correct >= 2).
         *   -2 if any wrong option chosen.
         *    0 if unattempted.
         * - Single-correct: +3 / -1 / 0.
         * - Numerical: +3 / 0 / 0.
         */
        public double calculateMarks() {
            if (!isAttempted) {
                return 0.0;
            }

            if (type == QuestionType.MULTI_CORRECT) {
                if (incorrectOptionsChosen > 0) {
                    return -2.0; // Negative marking for choosing any incorrect option
                }

                if (correctOptionsChosen == correctOptionsTotal && correctOptionsTotal > 0) {
                    return 4.0; // Full marks
                } else if (correctOptionsChosen == 3 && correctOptionsTotal == 4) {
                    return 3.0; // Partial
                } else if (correctOptionsChosen >= 2 && correctOptionsTotal >= 3) {
                    return 2.0; // Partial
                } else if (correctOptionsChosen >= 1 && correctOptionsTotal >= 2) {
                    return 1.0; // Partial
                } else {
                    return 0.0;
                }
            } else if (type == QuestionType.SINGLE_CORRECT) {
                if (incorrectOptionsChosen > 0) {
                    return -1.0;
                }
                return correctOptionsChosen == 1 ? 3.0 : 0.0;
            } else if (type == QuestionType.NUMERICAL) {
                // Numerical questions generally have zero negative marking
                if (incorrectOptionsChosen > 0) {
                    return 0.0;
                }
                return correctOptionsChosen == 1 ? 3.0 : 0.0;
            }

            return 0.0;
        }
    }

    /**
     * Calculates total marks across a set of JEE Advanced question attempts.
     */
    public static double calculateJeeAdvancedScore(List<JeeAdvQuestionAttempt> attempts) {
        if (attempts == null || attempts.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        for (JeeAdvQuestionAttempt a : attempts) {
            total += a.calculateMarks();
        }
        return total;
    }

    /**
     * Filters sessions strictly by exam profile to prevent data bleed between JEE Main and JEE Advanced.
     */
    public static List<Session> filterSessionsByProfile(List<Session> allSessions, String profileId) {
        if (allSessions == null || allSessions.isEmpty()) {
            return Collections.emptyList();
        }
        String targetProfile = (profileId != null) ? profileId.trim().toUpperCase() : ExamProfile.PROFILE_PRACTICE;
        List<Session> filtered = new ArrayList<>();
        for (Session s : allSessions) {
            String profile = (s.getExamProfile() != null && !s.getExamProfile().trim().isEmpty())
                    ? s.getExamProfile().trim().toUpperCase()
                    : (s.getProfileId() != null ? s.getProfileId().trim().toUpperCase() : ExamProfile.PROFILE_PRACTICE);

            if (targetProfile.equals(profile)) {
                filtered.add(s);
            }
        }
        return filtered;
    }

    /**
     * Aggregates a complete JEE Analytics Report for a specific profile.
     */
    public static JeeAnalyticsReport generateJeeReport(
            String profileName,
            List<Double> questionPacesSec,
            int questionsAttempted,
            int questionsCorrect,
            int questionsIncorrect,
            int totalExamQuestions,
            Map<String, Long> subjectTimeDistribution,
            Map<String, Integer> mistakeDistribution) {

        int skipped = Math.max(0, totalExamQuestions - questionsAttempted);
        double attemptRate = totalExamQuestions > 0 ? ((double) questionsAttempted / totalExamQuestions) * 100.0 : 0.0;
        double accuracy = questionsAttempted > 0 ? ((double) questionsCorrect / questionsAttempted) * 100.0 : 0.0;

        double sumPace = 0.0;
        int exceedingPace = 0;
        if (questionPacesSec != null) {
            for (Double p : questionPacesSec) {
                sumPace += p;
                if (p > JEE_MAIN_TARGET_PACE_SECONDS) {
                    exceedingPace++;
                }
            }
        }
        double avgPace = (questionPacesSec != null && !questionPacesSec.isEmpty())
                ? (sumPace / questionPacesSec.size())
                : 0.0;

        double projectedScore = calculateJeeMainScore(questionsCorrect, questionsIncorrect, skipped);
        double maxScore = totalExamQuestions * 4.0;

        return new JeeAnalyticsReport(
                profileName,
                System.currentTimeMillis() - 86400000L,
                System.currentTimeMillis(),
                totalExamQuestions,
                questionsAttempted,
                questionsCorrect,
                questionsIncorrect,
                skipped,
                attemptRate,
                accuracy,
                avgPace,
                exceedingPace,
                projectedScore,
                maxScore,
                (long) (sumPace * 1000L),
                0L,
                subjectTimeDistribution,
                null,
                mistakeDistribution
        );
    }
}

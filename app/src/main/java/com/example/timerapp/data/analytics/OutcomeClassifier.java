package com.example.timerapp.data.analytics;

import java.util.Locale;

/**
 * Single source of truth for interpreting a {@code QuestionLog} outcome string.
 *
 * <p>Historically the app wrote several synonyms for the same result:
 * <ul>
 *   <li>Correct: {@code CORRECT}, {@code SOLVED} (legacy) and {@code RIGHT} (two-zone timer)</li>
 *   <li>Wrong: {@code INCORRECT} (legacy) and {@code WRONG} (two-zone timer)</li>
 * </ul>
 * Anything else ({@code REVISED}, {@code PAUSED}, {@code SKIPPED}, blank, {@code null}) is treated
 * as unattempted so it never inflates accuracy, attempt counts, or average solving time.
 *
 * <p>Pure Java 8 utility with no Android dependencies so it can be reused by analytics,
 * wallpaper generation and UI code as well as standalone JVM unit tests.
 */
public final class OutcomeClassifier {

    private static final String OUTCOME_CORRECT = "CORRECT";
    private static final String OUTCOME_SOLVED = "SOLVED";
    private static final String OUTCOME_RIGHT = "RIGHT";

    private static final String OUTCOME_WRONG = "WRONG";
    private static final String OUTCOME_INCORRECT = "INCORRECT";

    private static final String CANONICAL_CORRECT = "CORRECT";
    private static final String CANONICAL_WRONG = "WRONG";
    private static final String CANONICAL_UNATTEMPTED = "UNATTEMPTED";

    private OutcomeClassifier() {
        // Utility class: prevent instantiation.
    }

    /**
     * @return the outcome trimmed and upper-cased with {@link Locale#ROOT}, or {@code ""} for null.
     */
    private static String canonicalize(String outcome) {
        return (outcome == null) ? "" : outcome.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * @return true when the outcome represents a correct attempt
     *         ({@code CORRECT}, {@code SOLVED} or {@code RIGHT}), case-insensitive and trimmed.
     *         Null and blank outcomes return false.
     */
    public static boolean isCorrect(String outcome) {
        String canonical = canonicalize(outcome);
        return OUTCOME_CORRECT.equals(canonical)
                || OUTCOME_SOLVED.equals(canonical)
                || OUTCOME_RIGHT.equals(canonical);
    }

    /**
     * @return true when the outcome represents a wrong attempt
     *         ({@code INCORRECT} or {@code WRONG}), case-insensitive and trimmed.
     *         Null and blank outcomes return false.
     */
    public static boolean isWrong(String outcome) {
        String canonical = canonicalize(outcome);
        return OUTCOME_INCORRECT.equals(canonical) || OUTCOME_WRONG.equals(canonical);
    }

    /**
     * @return true when the outcome is either a correct or a wrong attempt.
     */
    public static boolean isAttempted(String outcome) {
        return isCorrect(outcome) || isWrong(outcome);
    }

    /**
     * Collapses the legacy synonyms into one of {@code CORRECT}, {@code WRONG} or {@code UNATTEMPTED}.
     *
     * @param outcome raw outcome string, may be null or blank.
     * @return {@code "CORRECT"} for correct attempts, {@code "WRONG"} for wrong attempts,
     *         otherwise {@code "UNATTEMPTED"}.
     */
    public static String normalize(String outcome) {
        if (isCorrect(outcome)) {
            return CANONICAL_CORRECT;
        }
        if (isWrong(outcome)) {
            return CANONICAL_WRONG;
        }
        return CANONICAL_UNATTEMPTED;
    }
}

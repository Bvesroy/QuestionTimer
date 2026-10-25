package com.example.timerapp.model;

/**
 * Question outcome classifications for JEE solving analysis.
 */
public enum QuestionOutcome {
    CORRECT("Solved Correctly", 4.0),
    INCORRECT("Incorrect / Negative", -1.0),
    PARTIAL("Partial Correct", 2.0),
    SKIPPED("Skipped", 0.0),
    UNATTEMPTED("Unattempted", 0.0);

    private final String description;
    private final double defaultMarks;

    QuestionOutcome(String description, double defaultMarks) {
        this.description = description;
        this.defaultMarks = defaultMarks;
    }

    public String getDescription() {
        return description;
    }

    public double getDefaultMarks() {
        return defaultMarks;
    }
}

package com.example.timerapp.data.model;

/**
 * Performance pacing trend comparing recent sessions moving average pace to overall historic average.
 */
public enum PerformanceTrend {
    IMPROVING("Improving", "Pacing has accelerated by >= 5% compared to overall historical average."),
    STABLE("Stable", "Pacing is consistent within +- 5% of historical average."),
    DECLINING("Declining", "Pacing has decelerated by >= 5% compared to historical average.");

    private final String label;
    private final String description;

    PerformanceTrend(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}

package com.example.timerapp.data.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Statistics aggregated for a specific time of day slot.
 */
public class TimeSlotStats implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum SlotType {
        EARLY_MORNING("Early Morning", "04:00 - 08:00"),
        MORNING("Morning", "08:00 - 12:00"),
        AFTERNOON("Afternoon", "12:00 - 17:00"),
        EVENING("Evening", "17:00 - 21:00"),
        NIGHT("Night", "21:00 - 04:00");

        private final String displayName;
        private final String hoursRange;

        SlotType(String displayName, String hoursRange) {
            this.displayName = displayName;
            this.hoursRange = hoursRange;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getHoursRange() {
            return hoursRange;
        }

        /**
         * Resolves the SlotType corresponding to an hour of the day (0-23).
         */
        public static SlotType fromHour(int hourOfDay) {
            if (hourOfDay >= 4 && hourOfDay < 8) {
                return EARLY_MORNING;
            } else if (hourOfDay >= 8 && hourOfDay < 12) {
                return MORNING;
            } else if (hourOfDay >= 12 && hourOfDay < 17) {
                return AFTERNOON;
            } else if (hourOfDay >= 17 && hourOfDay < 21) {
                return EVENING;
            } else {
                return NIGHT;
            }
        }
    }

    private SlotType slotType;
    private int sessionCount;
    private long totalPracticeTimeMs;
    private int totalQuestionsSolved;
    private long averagePaceMs;
    private long bestPaceMs;

    public TimeSlotStats() {
    }

    public TimeSlotStats(SlotType slotType, int sessionCount, long totalPracticeTimeMs,
                         int totalQuestionsSolved, long averagePaceMs, long bestPaceMs) {
        this.slotType = slotType;
        this.sessionCount = sessionCount;
        this.totalPracticeTimeMs = totalPracticeTimeMs;
        this.totalQuestionsSolved = totalQuestionsSolved;
        this.averagePaceMs = averagePaceMs;
        this.bestPaceMs = bestPaceMs;
    }

    public SlotType getSlotType() {
        return slotType;
    }

    public String getSlotName() {
        return slotType != null ? slotType.getDisplayName() : "Unknown";
    }

    public String getHoursRange() {
        return slotType != null ? slotType.getHoursRange() : "";
    }

    public int getSessionCount() {
        return sessionCount;
    }

    public void setSessionCount(int sessionCount) {
        this.sessionCount = sessionCount;
    }

    public long getTotalPracticeTimeMs() {
        return totalPracticeTimeMs;
    }

    public void setTotalPracticeTimeMs(long totalPracticeTimeMs) {
        this.totalPracticeTimeMs = totalPracticeTimeMs;
    }

    public int getTotalQuestionsSolved() {
        return totalQuestionsSolved;
    }

    public void setTotalQuestionsSolved(int totalQuestionsSolved) {
        this.totalQuestionsSolved = totalQuestionsSolved;
    }

    public long getAveragePaceMs() {
        return averagePaceMs;
    }

    public void setAveragePaceMs(long averagePaceMs) {
        this.averagePaceMs = averagePaceMs;
    }

    public long getBestPaceMs() {
        return bestPaceMs;
    }

    public void setBestPaceMs(long bestPaceMs) {
        this.bestPaceMs = bestPaceMs;
    }

    public String getFormattedTotalPracticeTime() {
        return SessionStats.formatDuration(totalPracticeTimeMs);
    }

    public String getFormattedAveragePace() {
        return SessionStats.formatDuration(averagePaceMs);
    }

    public String getFormattedBestPace() {
        return (bestPaceMs > 0 && bestPaceMs < Long.MAX_VALUE) ? SessionStats.formatDuration(bestPaceMs) : "--:--";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeSlotStats that = (TimeSlotStats) o;
        return sessionCount == that.sessionCount &&
                totalPracticeTimeMs == that.totalPracticeTimeMs &&
                totalQuestionsSolved == that.totalQuestionsSolved &&
                averagePaceMs == that.averagePaceMs &&
                bestPaceMs == that.bestPaceMs &&
                slotType == that.slotType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(slotType, sessionCount, totalPracticeTimeMs, totalQuestionsSolved, averagePaceMs, bestPaceMs);
    }

    @Override
    public String toString() {
        return "TimeSlotStats{" +
                "slot=" + getSlotName() +
                ", sessionCount=" + sessionCount +
                ", totalPracticeTimeMs=" + totalPracticeTimeMs +
                ", averagePaceMs=" + averagePaceMs +
                '}';
    }
}

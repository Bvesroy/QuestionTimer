package com.example.timerapp.data.model;

import java.util.Calendar;

/**
 * Filter presets for time range analytics.
 */
public enum TimeRangeFilter {
    RANGE_TODAY("Today"),
    RANGE_7_DAYS("Last 7 Days"),
    RANGE_30_DAYS("Last 30 Days"),
    RANGE_ALL("All Time");

    private final String displayName;

    TimeRangeFilter(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Calculates the inclusive start timestamp (epoch millis) for this range relative to referenceNow.
     * Starts at 00:00:00.000 in the local timezone for calendar-aligned intervals.
     *
     * @param referenceNow Epoch timestamp of the current moment.
     * @return Start timestamp in milliseconds.
     */
    public long getStartTimestamp(long referenceNow) {
        if (referenceNow <= 0) {
            referenceNow = System.currentTimeMillis();
        }

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(referenceNow);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        switch (this) {
            case RANGE_TODAY:
                return cal.getTimeInMillis();
            case RANGE_7_DAYS:
                cal.add(Calendar.DAY_OF_YEAR, -6); // 7 full calendar days including today
                return cal.getTimeInMillis();
            case RANGE_30_DAYS:
                cal.add(Calendar.DAY_OF_YEAR, -29); // 30 full calendar days including today
                return cal.getTimeInMillis();
            case RANGE_ALL:
            default:
                return 0L;
        }
    }

    /**
     * Calculates the end timestamp (epoch millis) for this range relative to referenceNow.
     *
     * @param referenceNow Epoch timestamp of the current moment.
     * @return End timestamp in milliseconds.
     */
    public long getEndTimestamp(long referenceNow) {
        if (referenceNow <= 0) {
            referenceNow = System.currentTimeMillis();
        }
        if (this == RANGE_ALL) {
            return Long.MAX_VALUE;
        }
        return referenceNow;
    }
}

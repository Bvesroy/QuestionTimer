package com.example.timerapp.data.analytics;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Pure timezone-aware mathematical engine for Target Exam Date Countdowns.
 * Specifically handles JEE Main 2027 (20 January 2027) calendar day computations.
 * Resilient against:
 * - Timezone offsets and International Date Line crossing.
 * - Daylight Saving Time (DST) 23-hour and 25-hour calendar days.
 * - Same-day edge cases (00:00:01 vs 23:59:59).
 */
public class TargetDateCountdownMath implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int TARGET_YEAR_JEE_2027 = 2027;
    public static final int TARGET_MONTH_JEE_2027 = Calendar.JANUARY; // 0-indexed
    public static final int TARGET_DAY_JEE_2027 = 20;

    public static final String DEFAULT_TIMEZONE_ID = "Asia/Kolkata";

    /**
     * Obtains midnight epoch millis for 20 Jan 2027 in the specified timezone.
     */
    public static long getJeeMain2027TargetEpochMs(TimeZone timeZone) {
        TimeZone tz = timeZone != null ? timeZone : TimeZone.getTimeZone(DEFAULT_TIMEZONE_ID);
        Calendar cal = Calendar.getInstance(tz, Locale.ROOT);
        cal.set(Calendar.YEAR, TARGET_YEAR_JEE_2027);
        cal.set(Calendar.MONTH, TARGET_MONTH_JEE_2027);
        cal.set(Calendar.DAY_OF_MONTH, TARGET_DAY_JEE_2027);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * Calculates exact calendar day difference between fromEpochMs and targetEpochMs
     * evaluated in the specified timezone.
     *
     * @param fromEpochMs   Reference timestamp (e.g. current system time).
     * @param targetEpochMs Target timestamp (e.g. 20 Jan 2027).
     * @param timeZone      Timezone to evaluate calendar boundaries.
     * @return Positive number of days if target is in the future, 0 if same calendar day, negative if passed.
     */
    public static long calculateCalendarDaysDifference(long fromEpochMs, long targetEpochMs, TimeZone timeZone) {
        TimeZone tz = timeZone != null ? timeZone : TimeZone.getDefault();

        Calendar calFrom = Calendar.getInstance(tz, Locale.ROOT);
        calFrom.setTimeInMillis(fromEpochMs);
        calFrom.set(Calendar.HOUR_OF_DAY, 0);
        calFrom.set(Calendar.MINUTE, 0);
        calFrom.set(Calendar.SECOND, 0);
        calFrom.set(Calendar.MILLISECOND, 0);

        Calendar calTarget = Calendar.getInstance(tz, Locale.ROOT);
        calTarget.setTimeInMillis(targetEpochMs);
        calTarget.set(Calendar.HOUR_OF_DAY, 0);
        calTarget.set(Calendar.MINUTE, 0);
        calTarget.set(Calendar.SECOND, 0);
        calTarget.set(Calendar.MILLISECOND, 0);

        long diffMs = calTarget.getTimeInMillis() - calFrom.getTimeInMillis();

        // Rounding safely absorbs 23h or 25h DST transition days
        return Math.round((double) diffMs / (24.0 * 60.0 * 60.0 * 1000.0));
    }

    /**
     * Calculates remaining calendar days until JEE Main 2027 (20 Jan 2027).
     */
    public static long getCalendarDaysUntilJeeMain2027(long nowEpochMs, TimeZone timeZone) {
        TimeZone tz = timeZone != null ? timeZone : TimeZone.getTimeZone(DEFAULT_TIMEZONE_ID);
        long targetMs = getJeeMain2027TargetEpochMs(tz);
        return calculateCalendarDaysDifference(nowEpochMs, targetMs, tz);
    }

    /**
     * Checks if given timestamp falls on the exact calendar day of JEE Main 2027.
     */
    public static boolean isExamDay(long nowEpochMs, TimeZone timeZone) {
        return getCalendarDaysUntilJeeMain2027(nowEpochMs, timeZone) == 0;
    }

    /**
     * Checks if the JEE Main 2027 date has passed.
     */
    public static boolean isExamPassed(long nowEpochMs, TimeZone timeZone) {
        return getCalendarDaysUntilJeeMain2027(nowEpochMs, timeZone) < 0;
    }

    /**
     * Generates standard formatted banner string.
     */
    public static String formatCountdownBanner(long nowEpochMs, TimeZone timeZone, String examName) {
        String name = (examName != null && !examName.trim().isEmpty()) ? examName.trim().toUpperCase(Locale.ROOT) : "JEE MAIN 2027";
        long days = getCalendarDaysUntilJeeMain2027(nowEpochMs, timeZone);

        if (days > 1) {
            return String.format(Locale.ROOT, "%s IN %d DAYS • FOCUS", name, days);
        } else if (days == 1) {
            return String.format(Locale.ROOT, "%s IS TOMORROW • STAY CONFIDENT", name);
        } else if (days == 0) {
            return String.format(Locale.ROOT, "%s IS TODAY • GIVE YOUR BEST", name);
        } else {
            return String.format(Locale.ROOT, "%s CONCLUDED", name);
        }
    }
}

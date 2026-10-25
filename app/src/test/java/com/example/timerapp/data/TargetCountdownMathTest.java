package com.example.timerapp.data;

import com.example.timerapp.data.analytics.TargetDateCountdownMath;

import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Comprehensive Unit Test Suite for Target Date Countdown Math (20 Jan 2027),
 * Timezone-Aware Calendar Day Differences, DST Invariance, and Edge-of-Midnight Handling.
 */
public class TargetCountdownMathTest {

    private static final String TZ_IST = "Asia/Kolkata";
    private static final String TZ_UTC = "UTC";
    private static final String TZ_EST = "America/New_York";
    private static final String TZ_NZ = "Pacific/Auckland";

    @Test
    public void testCountdownTo20Jan2027_fromOctober25() {
        TimeZone ist = TimeZone.getTimeZone(TZ_IST);

        // Reference Date: 2026-10-25 15:30:00 IST
        Calendar calOct25 = Calendar.getInstance(ist, Locale.ROOT);
        calOct25.set(2026, Calendar.OCTOBER, 25, 15, 30, 0);
        calOct25.set(Calendar.MILLISECOND, 0);
        long nowEpochMs = calOct25.getTimeInMillis();

        // Calendar day breakdown:
        // Oct 25 to Oct 31 = 6 days (26, 27, 28, 29, 30, 31)
        // Nov 1 to Nov 30 = 30 days
        // Dec 1 to Dec 31 = 31 days
        // Jan 1 to Jan 20 = 20 days
        // Expected Total = 6 + 30 + 31 + 20 = 87 calendar days
        long daysRemaining = TargetDateCountdownMath.getCalendarDaysUntilJeeMain2027(nowEpochMs, ist);
        Assert.assertEquals("Expected exactly 87 calendar days from 25 Oct 2026 to 20 Jan 2027", 87L, daysRemaining);

        // Even at 23:59:59 on Oct 25, the calendar days remaining to Jan 20 must still be 87 days
        calOct25.set(Calendar.HOUR_OF_DAY, 23);
        calOct25.set(Calendar.MINUTE, 59);
        calOct25.set(Calendar.SECOND, 59);
        Assert.assertEquals(87L, TargetDateCountdownMath.getCalendarDaysUntilJeeMain2027(calOct25.getTimeInMillis(), ist));

        // And at 00:00:01 on Oct 25, the calendar days remaining must still be 87 days
        calOct25.set(Calendar.HOUR_OF_DAY, 0);
        calOct25.set(Calendar.MINUTE, 0);
        calOct25.set(Calendar.SECOND, 1);
        Assert.assertEquals(87L, TargetDateCountdownMath.getCalendarDaysUntilJeeMain2027(calOct25.getTimeInMillis(), ist));
    }

    @Test
    public void testExamDay_returnsZeroDaysRemaining() {
        TimeZone ist = TimeZone.getTimeZone(TZ_IST);

        // Morning of Exam: 2027-01-20 00:00:05 IST
        Calendar calExam = Calendar.getInstance(ist, Locale.ROOT);
        calExam.set(2027, Calendar.JANUARY, 20, 0, 0, 5);
        calExam.set(Calendar.MILLISECOND, 0);
        long startOfExamDay = calExam.getTimeInMillis();

        Assert.assertEquals(0L, TargetDateCountdownMath.getCalendarDaysUntilJeeMain2027(startOfExamDay, ist));
        Assert.assertTrue(TargetDateCountdownMath.isExamDay(startOfExamDay, ist));
        Assert.assertFalse(TargetDateCountdownMath.isExamPassed(startOfExamDay, ist));

        // Noon of Exam: 2027-01-20 12:00:00 IST
        calExam.set(Calendar.HOUR_OF_DAY, 12);
        long midExamDay = calExam.getTimeInMillis();
        Assert.assertEquals(0L, TargetDateCountdownMath.getCalendarDaysUntilJeeMain2027(midExamDay, ist));
        Assert.assertTrue(TargetDateCountdownMath.isExamDay(midExamDay, ist));

        // Night of Exam: 2027-01-20 23:59:50 IST
        calExam.set(Calendar.HOUR_OF_DAY, 23);
        calExam.set(Calendar.MINUTE, 59);
        calExam.set(Calendar.SECOND, 50);
        long endOfExamDay = calExam.getTimeInMillis();
        Assert.assertEquals(0L, TargetDateCountdownMath.getCalendarDaysUntilJeeMain2027(endOfExamDay, ist));
        Assert.assertTrue(TargetDateCountdownMath.isExamDay(endOfExamDay, ist));
        Assert.assertFalse(TargetDateCountdownMath.isExamPassed(endOfExamDay, ist));
    }

    @Test
    public void testEveOfExam_returnsOneDayRemaining() {
        TimeZone ist = TimeZone.getTimeZone(TZ_IST);

        // One second before midnight: 2027-01-19 23:59:59 IST
        Calendar calEve = Calendar.getInstance(ist, Locale.ROOT);
        calEve.set(2027, Calendar.JANUARY, 19, 23, 59, 59);
        calEve.set(Calendar.MILLISECOND, 999);

        long eveEpochMs = calEve.getTimeInMillis();
        Assert.assertEquals(1L, TargetDateCountdownMath.getCalendarDaysUntilJeeMain2027(eveEpochMs, ist));
        Assert.assertFalse(TargetDateCountdownMath.isExamDay(eveEpochMs, ist));
        Assert.assertFalse(TargetDateCountdownMath.isExamPassed(eveEpochMs, ist));
    }

    @Test
    public void testExamPassed_returnsNegativeDays() {
        TimeZone ist = TimeZone.getTimeZone(TZ_IST);

        // 1 day after exam: 2027-01-21 09:00:00 IST
        Calendar calAfter = Calendar.getInstance(ist, Locale.ROOT);
        calAfter.set(2027, Calendar.JANUARY, 21, 9, 0, 0);

        long afterMs = calAfter.getTimeInMillis();
        Assert.assertEquals(-1L, TargetDateCountdownMath.getCalendarDaysUntilJeeMain2027(afterMs, ist));
        Assert.assertFalse(TargetDateCountdownMath.isExamDay(afterMs, ist));
        Assert.assertTrue(TargetDateCountdownMath.isExamPassed(afterMs, ist));

        // 10 days after exam: 2027-01-30
        calAfter.set(2027, Calendar.JANUARY, 30, 10, 0, 0);
        Assert.assertEquals(-10L, TargetDateCountdownMath.getCalendarDaysUntilJeeMain2027(calAfter.getTimeInMillis(), ist));
    }

    @Test
    public void testTimezoneAwareness_boundaryEvaluationAcrossTimezones() {
        // Universal instant: 2027-01-19 19:30:00 UTC
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone(TZ_UTC), Locale.ROOT);
        utcCal.set(2027, Calendar.JANUARY, 19, 19, 30, 0);
        utcCal.set(Calendar.MILLISECOND, 0);
        long instantMs = utcCal.getTimeInMillis();

        // 1. In UTC: It is still Jan 19, 19:30 -> 1 day remaining
        long utcDays = TargetDateCountdownMath.calculateCalendarDaysDifference(
                instantMs,
                TargetDateCountdownMath.getJeeMain2027TargetEpochMs(TimeZone.getTimeZone(TZ_UTC)),
                TimeZone.getTimeZone(TZ_UTC)
        );
        Assert.assertEquals(1L, utcDays);

        // 2. In Asia/Kolkata (+05:30): UTC 19:30 + 5h30m = Jan 20, 01:00 AM -> Exam Day (0 days remaining)!
        long istDays = TargetDateCountdownMath.getCalendarDaysUntilJeeMain2027(instantMs, TimeZone.getTimeZone(TZ_IST));
        Assert.assertEquals(0L, istDays);
        Assert.assertTrue(TargetDateCountdownMath.isExamDay(instantMs, TimeZone.getTimeZone(TZ_IST)));

        // 3. In America/New_York (UTC-5): UTC 19:30 - 5h = Jan 19, 14:30 -> 1 day remaining
        long estDays = TargetDateCountdownMath.calculateCalendarDaysDifference(
                instantMs,
                TargetDateCountdownMath.getJeeMain2027TargetEpochMs(TimeZone.getTimeZone(TZ_EST)),
                TimeZone.getTimeZone(TZ_EST)
        );
        Assert.assertEquals(1L, estDays);

        // 4. In Pacific/Auckland (UTC+13): UTC 19:30 + 13h = Jan 20, 08:30 AM -> Exam Day (0 days remaining)
        long nzDays = TargetDateCountdownMath.calculateCalendarDaysDifference(
                instantMs,
                TargetDateCountdownMath.getJeeMain2027TargetEpochMs(TimeZone.getTimeZone(TZ_NZ)),
                TimeZone.getTimeZone(TZ_NZ)
        );
        Assert.assertEquals(0L, nzDays);
    }

    @Test
    public void testDaylightSavingTimeTransitionResilience() {
        // America/New_York has DST transitions (spring forward 23h, fall back 25h).
        TimeZone nyTz = TimeZone.getTimeZone("America/New_York");

        // 2026 Fall back occurs on Nov 1, 2026 (day has 25 hours).
        Calendar calOct30 = Calendar.getInstance(nyTz, Locale.ROOT);
        calOct30.set(2026, Calendar.OCTOBER, 30, 12, 0, 0);

        Calendar calNov03 = Calendar.getInstance(nyTz, Locale.ROOT);
        calNov03.set(2026, Calendar.NOVEMBER, 3, 12, 0, 0);

        // Difference must be exactly 4 calendar days (Oct 30 -> Oct 31 -> Nov 1 -> Nov 2 -> Nov 3)
        long daysDiff = TargetDateCountdownMath.calculateCalendarDaysDifference(
                calOct30.getTimeInMillis(), calNov03.getTimeInMillis(), nyTz
        );
        Assert.assertEquals(4L, daysDiff);
    }

    @Test
    public void testBannerFormatting() {
        TimeZone ist = TimeZone.getTimeZone(TZ_IST);

        // 87 days out
        Calendar cal = Calendar.getInstance(ist, Locale.ROOT);
        cal.set(2026, Calendar.OCTOBER, 25, 12, 0, 0);
        String banner87 = TargetDateCountdownMath.formatCountdownBanner(cal.getTimeInMillis(), ist, "JEE MAIN 2027");
        Assert.assertEquals("JEE MAIN 2027 IN 87 DAYS • FOCUS", banner87);

        // Tomorrow (1 day out)
        cal.set(2027, Calendar.JANUARY, 19, 12, 0, 0);
        String banner1 = TargetDateCountdownMath.formatCountdownBanner(cal.getTimeInMillis(), ist, "JEE MAIN 2027");
        Assert.assertEquals("JEE MAIN 2027 IS TOMORROW • STAY CONFIDENT", banner1);

        // Today (0 days out)
        cal.set(2027, Calendar.JANUARY, 20, 8, 30, 0);
        String banner0 = TargetDateCountdownMath.formatCountdownBanner(cal.getTimeInMillis(), ist, "JEE MAIN 2027");
        Assert.assertEquals("JEE MAIN 2027 IS TODAY • GIVE YOUR BEST", banner0);

        // Concluded
        cal.set(2027, Calendar.JANUARY, 21, 12, 0, 0);
        String bannerPassed = TargetDateCountdownMath.formatCountdownBanner(cal.getTimeInMillis(), ist, "JEE MAIN 2027");
        Assert.assertEquals("JEE MAIN 2027 CONCLUDED", bannerPassed);
    }
}

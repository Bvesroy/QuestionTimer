package com.example.timerapp.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;

import com.example.timerapp.data.SessionStorage;
import com.example.timerapp.model.TimerSession;
import com.example.timerapp.model.TimerState;
import com.example.timerapp.wallpaper.JeeWallpaperGenerator;
import com.example.timerapp.widget.WidgetUpdateHelper;

import java.util.Calendar;
import java.util.List;

/**
 * AlarmManager receiver for daily wallpaper and countdown refresh.
 * Triggers background update of adaptive JEE wallpaper and home-screen widgets.
 */
public class DailyWallpaperRefreshReceiver extends BroadcastReceiver {

    private static final String TAG = "WallpaperRefresh";

    public static final String ACTION_DAILY_REFRESH = "com.example.timerapp.ACTION_DAILY_WALLPAPER_REFRESH";
    private static final int REQUEST_CODE_DAILY_ALARM = 8801;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null) return;
        String action = (intent != null) ? intent.getAction() : null;
        Log.i(TAG, "DailyWallpaperRefreshReceiver invoked with action: " + action);

        // Perform daily refresh work
        performDailyRefresh(context);

        // Reschedule next daily alarm (e.g. after reboot or alarm trigger)
        scheduleDailyRefresh(context);
    }

    /**
     * Executes the background refresh for wallpaper and widgets.
     */
    public static void performDailyRefresh(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(JeeWallpaperGenerator.PREFS_NAME, Context.MODE_PRIVATE);
            boolean autoApply = prefs.getBoolean(JeeWallpaperGenerator.KEY_WALLPAPER_AUTO_APPLY, false);
            String targetScreen = prefs.getString(JeeWallpaperGenerator.KEY_WALLPAPER_TARGET, "BOTH");
            long targetDate = prefs.getLong(JeeWallpaperGenerator.KEY_EXAM_TARGET_DATE_MILLIS, JeeWallpaperGenerator.DEFAULT_JEE_2027_MILLIS);

            SessionStorage sessionStorage = new SessionStorage(context);
            List<TimerSession> sessions = sessionStorage.loadSessions();

            int streakDays = 1;
            JeeWallpaperGenerator.WallpaperMetrics metrics =
                    JeeWallpaperGenerator.WallpaperMetrics.fromSessions(sessions, targetDate, streakDays);

            // Update widgets with latest metrics
            WidgetUpdateHelper.updateAllWidgets(
                    context,
                    TimerState.IDLE,
                    0L,
                    metrics.totalStudyDurationMs,
                    false,
                    metrics.totalQuestions,
                    metrics.accuracyPercentage,
                    metrics.streakDays
            );

            // If auto wallpaper is enabled, generate and apply wallpaper
            if (autoApply) {
                Bitmap wallpaper = JeeWallpaperGenerator.generateWallpaper(context, metrics);
                if (wallpaper != null) {
                    boolean success = JeeWallpaperGenerator.applyWallpaper(context, wallpaper, targetScreen);
                    Log.i(TAG, "Daily wallpaper auto-apply status: " + success);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error performing daily wallpaper/countdown refresh: " + e.getMessage(), e);
        }
    }

    /**
     * Schedules daily alarm with AlarmManager to fire at 04:00 AM every day.
     */
    public static void scheduleDailyRefresh(Context context) {
        if (context == null) return;
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;

            Intent intent = new Intent(context, DailyWallpaperRefreshReceiver.class);
            intent.setAction(ACTION_DAILY_REFRESH);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_DAILY_ALARM,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Schedule for tomorrow 04:00 AM
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, 4);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            // If 04:00 AM already passed today, advance to tomorrow
            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            long triggerMillis = calendar.getTimeInMillis();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent);
            }
            Log.i(TAG, "Scheduled next daily refresh alarm at: " + triggerMillis);
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule daily refresh alarm: " + e.getMessage(), e);
        }
    }

    /**
     * Cancels scheduled daily refresh alarm.
     */
    public static void cancelDailyRefresh(Context context) {
        if (context == null) return;
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;

            Intent intent = new Intent(context, DailyWallpaperRefreshReceiver.class);
            intent.setAction(ACTION_DAILY_REFRESH);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_DAILY_ALARM,
                    intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );

            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
                Log.i(TAG, "Cancelled daily refresh alarm.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling daily refresh alarm: " + e.getMessage(), e);
        }
    }
}

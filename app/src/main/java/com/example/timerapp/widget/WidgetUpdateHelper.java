package com.example.timerapp.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.View;
import android.widget.RemoteViews;

import com.example.timerapp.R;
import com.example.timerapp.model.TimerState;
import com.example.timerapp.ui.MainActivity;
import com.example.timerapp.wallpaper.JeeWallpaperGenerator;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Central update dispatcher for all 4 QuestionTimer home-screen widgets.
 */
public class WidgetUpdateHelper {

    public static final String ACTION_WIDGET_TOGGLE = "com.example.timerapp.ACTION_WIDGET_TOGGLE";
    public static final String ACTION_WIDGET_BREAK = "com.example.timerapp.ACTION_WIDGET_BREAK";
    public static final String ACTION_WIDGET_OPEN = "com.example.timerapp.ACTION_WIDGET_OPEN";

    public static void updateAllWidgets(Context context, TimerState state, long currentElapsedMs,
                                        long totalStudyMs, boolean isBreak, int completedQuestions,
                                        int accuracyPct, int streakDays) {
        if (context == null) return;
        AppWidgetManager manager = AppWidgetManager.getInstance(context);

        updateLiveTimerWidgets(context, manager, state, currentElapsedMs, totalStudyMs, isBreak);
        updateQuickControlWidgets(context, manager, state, currentElapsedMs, isBreak);
        updateExamCountdownWidgets(context, manager);
        updateProgressWidgets(context, manager, accuracyPct, completedQuestions, streakDays);
    }

    public static void updateLiveTimerWidgets(Context context, AppWidgetManager manager,
                                             TimerState state, long currentElapsedMs,
                                             long totalStudyMs, boolean isBreak) {
        ComponentName component = new ComponentName(context, LiveTimerWidgetProvider.class);
        int[] widgetIds = manager.getAppWidgetIds(component);
        if (widgetIds == null || widgetIds.length == 0) return;

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_live_timer);

        // Timer readout
        views.setTextViewText(R.id.tvWidgetTimerReadout, formatTimerMs(currentElapsedMs));

        // Total study clock
        views.setTextViewText(R.id.tvWidgetTotalStudyClock, "TOTAL: " + formatStudyClockMs(totalStudyMs));

        // State Badge
        String badge;
        int color;
        if (isBreak) {
            badge = "ON BREAK";
            color = Color.parseColor("#FF9F0A");
        } else if (state != null && state.isRunning()) {
            badge = "SOLVING";
            color = Color.parseColor("#30D158");
        } else if (state != null && state.isPaused()) {
            badge = "PAUSED";
            color = Color.parseColor("#FF9F0A");
        } else {
            badge = "READY";
            color = Color.parseColor("#8E8E93");
        }
        views.setTextViewText(R.id.tvWidgetStatusBadge, badge);
        views.setTextColor(R.id.tvWidgetStatusBadge, color);

        // Tap to open MainActivity
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(context, 101, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetLiveTimerRoot, pi);

        manager.updateAppWidget(component, views);
    }

    public static void updateQuickControlWidgets(Context context, AppWidgetManager manager,
                                                 TimerState state, long currentElapsedMs, boolean isBreak) {
        ComponentName component = new ComponentName(context, QuickControlWidgetProvider.class);
        int[] widgetIds = manager.getAppWidgetIds(component);
        if (widgetIds == null || widgetIds.length == 0) return;

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_quick_control);

        views.setTextViewText(R.id.tvQuickControlTimer, formatMinutesSeconds(currentElapsedMs));

        boolean running = (state != null && state.isRunning());
        views.setTextViewText(R.id.btnWidgetToggle, running ? "PAUSE" : "START");
        views.setTextViewText(R.id.btnWidgetBreak, isBreak ? "RESUME" : "BREAK");

        String status = isBreak ? "ON BREAK" : (running ? "SOLVING" : (state != null && state.isPaused() ? "PAUSED" : "IDLE"));
        views.setTextViewText(R.id.tvQuickControlStatus, "JEE TIMER • " + status);

        // Toggle PendingIntent
        Intent toggleIntent = new Intent(context, QuickControlWidgetProvider.class);
        toggleIntent.setAction(ACTION_WIDGET_TOGGLE);
        PendingIntent togglePi = PendingIntent.getBroadcast(context, 201, toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.btnWidgetToggle, togglePi);

        // Break PendingIntent
        Intent breakIntent = new Intent(context, QuickControlWidgetProvider.class);
        breakIntent.setAction(ACTION_WIDGET_BREAK);
        PendingIntent breakPi = PendingIntent.getBroadcast(context, 202, breakIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.btnWidgetBreak, breakPi);

        // Open PendingIntent
        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPi = PendingIntent.getActivity(context, 203, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.btnWidgetOpen, openPi);
        views.setOnClickPendingIntent(R.id.widgetQuickControlRoot, openPi);

        manager.updateAppWidget(component, views);
    }

    public static void updateExamCountdownWidgets(Context context, AppWidgetManager manager) {
        ComponentName component = new ComponentName(context, ExamCountdownWidgetProvider.class);
        int[] widgetIds = manager.getAppWidgetIds(component);
        if (widgetIds == null || widgetIds.length == 0) return;

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_exam_countdown);

        SharedPreferences prefs = context.getSharedPreferences("question_timer_prefs", Context.MODE_PRIVATE);
        long targetDate = prefs.getLong(JeeWallpaperGenerator.KEY_EXAM_TARGET_DATE_MILLIS,
                JeeWallpaperGenerator.DEFAULT_JEE_2027_MILLIS);

        long diffMs = targetDate - System.currentTimeMillis();
        int days = (int) Math.max(0, TimeUnit.MILLISECONDS.toDays(diffMs));

        views.setTextViewText(R.id.tvWidgetDaysRemaining, String.valueOf(days));
        views.setTextViewText(R.id.tvWidgetExamLabel, "JEE 2027 COUNTDOWN");
        views.setTextViewText(R.id.tvWidgetCountdownSubtitle, "DAYS LEFT • 20 JAN 2027");

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(context, 301, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetExamCountdownRoot, pi);

        manager.updateAppWidget(component, views);
    }

    public static void updateProgressWidgets(Context context, AppWidgetManager manager,
                                            int accuracyPct, int solvedCount, int streakDays) {
        ComponentName component = new ComponentName(context, ProgressWidgetProvider.class);
        int[] widgetIds = manager.getAppWidgetIds(component);
        if (widgetIds == null || widgetIds.length == 0) return;

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_progress);

        views.setTextViewText(R.id.tvWidgetAccuracy, accuracyPct + "%");
        views.setTextViewText(R.id.tvWidgetSolvedCount, String.valueOf(solvedCount));
        views.setTextViewText(R.id.tvWidgetStreak, streakDays + "d");

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(context, 401, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetProgressRoot, pi);

        manager.updateAppWidget(component, views);
    }

    private static String formatTimerMs(long ms) {
        long totalSeconds = ms / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        long tenths = (ms % 1000L) / 100L;
        return String.format(Locale.ROOT, "%02d:%02d.%d", minutes, seconds, tenths);
    }

    private static String formatMinutesSeconds(long ms) {
        long totalSeconds = ms / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    private static String formatStudyClockMs(long ms) {
        long totalSeconds = ms / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0) {
            return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
        }
    }
}

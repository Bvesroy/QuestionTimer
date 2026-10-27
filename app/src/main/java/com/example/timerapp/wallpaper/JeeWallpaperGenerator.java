package com.example.timerapp.wallpaper;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import com.example.timerapp.data.analytics.OutcomeClassifier;
import com.example.timerapp.model.QuestionLog;
import com.example.timerapp.model.TimerSession;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 100% Offline Native Canvas Wallpaper Generator.
 * Produces high-contrast AMOLED minimalist digital artwork reflecting real study data:
 * - Accuracy percentage & solved question counts
 * - Cumulative study time & sessions
 * - Consistency streaks & focus stamina
 * - Configurable top-right exam countdown to 20 January 2027
 *
 * Supports Mode A (Automatic background refresh via WallpaperManager)
 * and Mode B (Manual in-app preview, set, export).
 */
public class JeeWallpaperGenerator {

    private static final String TAG = "JeeWallpaperGenerator";

    public static final String PREFS_NAME = "question_timer_prefs";
    public static final String KEY_WALLPAPER_AUTO_APPLY = "wallpaper_auto_apply";
    public static final String KEY_WALLPAPER_TARGET = "wallpaper_target_screen"; // "BOTH", "HOME", "LOCK"
    public static final String KEY_EXAM_TARGET_DATE_MILLIS = "exam_target_date_millis";

    // Standard default JEE 2027 Date: 20 January 2027 09:00:00 IST
    public static final long DEFAULT_JEE_2027_MILLIS;
    static {
        Calendar cal = Calendar.getInstance();
        cal.set(2027, Calendar.JANUARY, 20, 9, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        DEFAULT_JEE_2027_MILLIS = cal.getTimeInMillis();
    }

    /**
     * Data bundle summarizing metrics for wallpaper generation.
     */
    public static class WallpaperMetrics {
        public int daysRemaining;
        public int totalQuestions;
        public int solvedCorrect;
        public int solvedIncorrect;
        public int accuracyPercentage;
        public long totalStudyDurationMs;
        public int streakDays;
        public int totalSessions;
        public double avgSecondsPerQuestion;
        public String targetExamLabel = "JEE 2027";

        public static WallpaperMetrics fromSessions(List<TimerSession> sessions, long targetDateMillis, int streak) {
            WallpaperMetrics m = new WallpaperMetrics();
            long now = System.currentTimeMillis();
            long diffMillis = targetDateMillis - now;
            m.daysRemaining = (int) Math.max(0, TimeUnit.MILLISECONDS.toDays(diffMillis));

            if (sessions != null) {
                m.totalSessions = sessions.size();
                for (TimerSession s : sessions) {
                    m.totalStudyDurationMs += s.getTotalStudyDurationMs() > 0 ? s.getTotalStudyDurationMs() : s.getTotalDurationMs();
                    for (QuestionLog q : s.getLogs()) {
                        m.totalQuestions++;
                        String out = q.getOutcome();
                        if (OutcomeClassifier.isCorrect(out)) {
                            m.solvedCorrect++;
                        } else if (OutcomeClassifier.isWrong(out)) {
                            m.solvedIncorrect++;
                        }
                    }
                }
            }

            if (m.totalQuestions > 0) {
                int attempted = m.solvedCorrect + m.solvedIncorrect;
                m.accuracyPercentage = (attempted > 0)
                        ? (int) Math.round((m.solvedCorrect * 100.0) / attempted)
                        : 0;
                long totalSec = m.totalStudyDurationMs / 1000L;
                m.avgSecondsPerQuestion = (attempted > 0)
                        ? (double) totalSec / attempted
                        : 0;
            } else {
                m.accuracyPercentage = 100;
                m.avgSecondsPerQuestion = 0;
            }

            m.streakDays = Math.max(1, streak);
            return m;
        }
    }

    /**
     * Generates a high-resolution AMOLED wallpaper bitmap tailored to device screen dimensions.
     */
    public static Bitmap generateWallpaper(Context context, WallpaperMetrics metrics) {
        int width = 1080;
        int height = 2400;

        try {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (wm != null) {
                DisplayMetrics dm = new DisplayMetrics();
                wm.getDefaultDisplay().getRealMetrics(dm);
                if (dm.widthPixels > 0 && dm.heightPixels > 0) {
                    width = dm.widthPixels;
                    height = dm.heightPixels;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to resolve screen dimensions, fallback to 1080x2400: " + e.getMessage());
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // AMOLED Pure Black Background
        canvas.drawColor(Color.BLACK);

        // Paints
        Paint paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintText.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        Paint paintSub = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintSub.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        paintSub.setColor(Color.parseColor("#8E8E93"));

        Paint paintArcBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintArcBg.setStyle(Paint.Style.STROKE);
        paintArcBg.setStrokeCap(Paint.Cap.ROUND);
        paintArcBg.setColor(Color.parseColor("#1C1C1E"));

        Paint paintArcAccent = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintArcAccent.setStyle(Paint.Style.STROKE);
        paintArcAccent.setStrokeCap(Paint.Cap.ROUND);

        // -------------------------------------------------------------
        // 1. TOP-RIGHT CORNER COUNTDOWN ("XX DAYS LEFT")
        // -------------------------------------------------------------
        float paddingX = width * 0.08f;
        float topY = height * 0.09f;

        paintText.setTextAlign(Paint.Align.RIGHT);
        paintText.setColor(Color.parseColor("#FF9F0A")); // JEE Vibrant Amber/Orange
        paintText.setTextSize(width * 0.075f);
        canvas.drawText(metrics.daysRemaining + " DAYS LEFT", width - paddingX, topY, paintText);

        paintSub.setTextAlign(Paint.Align.RIGHT);
        paintSub.setTextSize(width * 0.032f);
        paintSub.setColor(Color.parseColor("#A1A1A6"));
        paintSub.setLetterSpacing(0.12f);
        canvas.drawText("TARGET: " + metrics.targetExamLabel + " • 20 JAN 2027", width - paddingX, topY + (width * 0.045f), paintSub);

        // Top-Left Branding Cue
        Paint paintBrand = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBrand.setColor(Color.parseColor("#FFFFFF"));
        paintBrand.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        paintBrand.setTextSize(width * 0.045f);
        paintBrand.setLetterSpacing(0.15f);
        canvas.drawText("QUESTION TIMER", paddingX, topY - (width * 0.015f), paintBrand);

        paintSub.setTextAlign(Paint.Align.LEFT);
        paintSub.setTextSize(width * 0.028f);
        canvas.drawText("DAILY JEE MASTERY ENGINE", paddingX, topY + (width * 0.025f), paintSub);

        // -------------------------------------------------------------
        // 2. CENTER PIECE: HIGH-CONTRAST CONCENTRIC ARC GAUGES
        // -------------------------------------------------------------
        float centerX = width / 2f;
        float centerY = height * 0.42f;
        float outerRadius = width * 0.36f;
        float strokeOuter = width * 0.038f;
        float strokeMiddle = width * 0.028f;
        float strokeInner = width * 0.020f;

        // Outer Arc: Accuracy %
        paintArcBg.setStrokeWidth(strokeOuter);
        RectF outerRect = new RectF(centerX - outerRadius, centerY - outerRadius, centerX + outerRadius, centerY + outerRadius);
        canvas.drawArc(outerRect, 135, 270, false, paintArcBg);

        float accuracySweep = (Math.max(0, Math.min(100, metrics.accuracyPercentage)) / 100f) * 270f;
        paintArcAccent.setStrokeWidth(strokeOuter);
        paintArcAccent.setColor(Color.parseColor("#30D158")); // Emerald Green
        canvas.drawArc(outerRect, 135, accuracySweep, false, paintArcAccent);

        // Middle Arc: Streak / Consistency
        float middleRadius = outerRadius - (strokeOuter * 1.5f);
        paintArcBg.setStrokeWidth(strokeMiddle);
        RectF middleRect = new RectF(centerX - middleRadius, centerY - middleRadius, centerX + middleRadius, centerY + middleRadius);
        canvas.drawArc(middleRect, 135, 270, false, paintArcBg);

        float streakSweep = (Math.min(30, metrics.streakDays) / 30f) * 270f;
        paintArcAccent.setStrokeWidth(strokeMiddle);
        paintArcAccent.setColor(Color.parseColor("#0A84FF")); // Precision Blue
        canvas.drawArc(middleRect, 135, Math.max(15, streakSweep), false, paintArcAccent);

        // Inner Arc: Study Hours Goal (e.g. 6 hours benchmark)
        float innerRadius = middleRadius - (strokeMiddle * 1.6f);
        paintArcBg.setStrokeWidth(strokeInner);
        RectF innerRect = new RectF(centerX - innerRadius, centerY - innerRadius, centerX + innerRadius, centerY + innerRadius);
        canvas.drawArc(innerRect, 135, 270, false, paintArcBg);

        long studyHours = TimeUnit.MILLISECONDS.toHours(metrics.totalStudyDurationMs);
        float studySweep = (Math.min(10, Math.max(1, studyHours)) / 10f) * 270f;
        paintArcAccent.setStrokeWidth(strokeInner);
        paintArcAccent.setColor(Color.parseColor("#FF9F0A")); // Amber
        canvas.drawArc(innerRect, 135, studySweep, false, paintArcAccent);

        // Inner Gauge Center Display
        Paint paintCenterValue = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintCenterValue.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        paintCenterValue.setTextAlign(Paint.Align.CENTER);
        paintCenterValue.setColor(Color.WHITE);
        paintCenterValue.setTextSize(width * 0.13f);
        canvas.drawText(metrics.accuracyPercentage + "%", centerX, centerY + (width * 0.035f), paintCenterValue);

        Paint paintCenterLabel = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintCenterLabel.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        paintCenterLabel.setTextAlign(Paint.Align.CENTER);
        paintCenterLabel.setColor(Color.parseColor("#8E8E93"));
        paintCenterLabel.setTextSize(width * 0.032f);
        paintCenterLabel.setLetterSpacing(0.18f);
        canvas.drawText("SOLVE ACCURACY", centerX, centerY + (width * 0.085f), paintCenterLabel);

        // -------------------------------------------------------------
        // 3. STATISTICAL BREAKDOWN TILES (BOTTOM HALF)
        // -------------------------------------------------------------
        float tileTop = height * 0.62f;
        float tileHeight = height * 0.08f;
        float cardWidth = width - (paddingX * 2);

        Paint paintCardBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintCardBg.setColor(Color.parseColor("#121212"));
        paintCardBg.setStyle(Paint.Style.FILL);

        Paint paintCardBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintCardBorder.setColor(Color.parseColor("#222224"));
        paintCardBorder.setStyle(Paint.Style.STROKE);
        paintCardBorder.setStrokeWidth(2f);

        // Tile 1: Study Duration & Solved Questions
        RectF card1 = new RectF(paddingX, tileTop, paddingX + cardWidth, tileTop + tileHeight);
        canvas.drawRoundRect(card1, 20f, 20f, paintCardBg);
        canvas.drawRoundRect(card1, 20f, 20f, paintCardBorder);

        drawTileRow(canvas, width, paddingX, tileTop, tileHeight,
                "TOTAL PRACTICE", formatDurationHoursMinutes(metrics.totalStudyDurationMs),
                "QUESTIONS SOLVED", String.valueOf(metrics.totalQuestions));

        // Tile 2: Streak & Average Speed
        float tile2Top = tileTop + tileHeight + (height * 0.016f);
        RectF card2 = new RectF(paddingX, tile2Top, paddingX + cardWidth, tile2Top + tileHeight);
        canvas.drawRoundRect(card2, 20f, 20f, paintCardBg);
        canvas.drawRoundRect(card2, 20f, 20f, paintCardBorder);

        String speedStr = (metrics.avgSecondsPerQuestion > 0) ? String.format(Locale.ROOT, "%.0fs / Q", metrics.avgSecondsPerQuestion) : "--";
        drawTileRow(canvas, width, paddingX, tile2Top, tileHeight,
                "STUDY STREAK", metrics.streakDays + " DAYS",
                "AVERAGE PACE", speedStr);

        // -------------------------------------------------------------
        // 4. FOOTER MOTTO
        // -------------------------------------------------------------
        Paint paintFooter = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintFooter.setTextAlign(Paint.Align.CENTER);
        paintFooter.setColor(Color.parseColor("#636366"));
        paintFooter.setTextSize(width * 0.028f);
        paintFooter.setLetterSpacing(0.15f);
        canvas.drawText("CONSISTENCY OVER INTENSITY • TARGET AIR < 500", centerX, height * 0.90f, paintFooter);

        return bitmap;
    }

    private static void drawTileRow(Canvas canvas, int width, float paddingX, float top, float height,
                                    String label1, String value1, String label2, String value2) {
        float col1X = paddingX + (width * 0.05f);
        float col2X = paddingX + ((width - (paddingX * 2)) / 2f) + (width * 0.05f);

        Paint paintLabel = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintLabel.setColor(Color.parseColor("#8E8E93"));
        paintLabel.setTextSize(width * 0.026f);
        paintLabel.setLetterSpacing(0.1f);

        Paint paintVal = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintVal.setColor(Color.WHITE);
        paintVal.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        paintVal.setTextSize(width * 0.042f);

        // Column 1
        canvas.drawText(label1, col1X, top + (height * 0.40f), paintLabel);
        canvas.drawText(value1, col1X, top + (height * 0.80f), paintVal);

        // Column 2
        canvas.drawText(label2, col2X, top + (height * 0.40f), paintLabel);
        canvas.drawText(value2, col2X, top + (height * 0.80f), paintVal);
    }

    /**
     * Applies generated bitmap as wallpaper via Android WallpaperManager.
     * Mode A or B support.
     *
     * @param target "BOTH", "HOME", or "LOCK"
     * @return true if successfully applied
     */
    public static boolean applyWallpaper(Context context, Bitmap bitmap, String target) {
        if (bitmap == null || context == null) return false;
        try {
            WallpaperManager wm = WallpaperManager.getInstance(context.getApplicationContext());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                int which;
                if ("HOME".equalsIgnoreCase(target)) {
                    which = WallpaperManager.FLAG_SYSTEM;
                } else if ("LOCK".equalsIgnoreCase(target)) {
                    which = WallpaperManager.FLAG_LOCK;
                } else {
                    which = WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK;
                }
                wm.setBitmap(bitmap, null, true, which);
            } else {
                wm.setBitmap(bitmap);
            }
            Log.i(TAG, "Successfully applied JEE wallpaper to " + target);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to apply wallpaper: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Exports bitmap to a local PNG file for sharing or saving.
     */
    public static File exportWallpaperImage(Context context, Bitmap bitmap) throws IOException {
        File dir = new File(context.getCacheDir(), "wallpapers");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, "jee_progress_wallpaper_" + System.currentTimeMillis() + ".png");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
        }
        return file;
    }

    private static String formatDurationHoursMinutes(long ms) {
        long totalMinutes = ms / 60000L;
        long hours = totalMinutes / 60L;
        long mins = totalMinutes % 60L;
        if (hours > 0) {
            return String.format(Locale.ROOT, "%02dh %02dm", hours, mins);
        } else {
            return String.format(Locale.ROOT, "%02dm", mins);
        }
    }
}

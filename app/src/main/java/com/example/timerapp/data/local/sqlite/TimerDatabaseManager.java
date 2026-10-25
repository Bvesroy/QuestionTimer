package com.example.timerapp.data.local.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.timerapp.data.analytics.StudyCoachEngine;
import com.example.timerapp.data.backup.BackupManager;
import com.example.timerapp.data.backup.SafBackupEngine;
import com.example.timerapp.data.model.Achievement;
import com.example.timerapp.data.model.AdaptiveTargetSuggestion;
import com.example.timerapp.data.model.AttemptAuditRecord;
import com.example.timerapp.data.model.BreakLog;
import com.example.timerapp.data.model.DailyPracticeDataPoint;
import com.example.timerapp.data.model.DataIntegrityReport;
import com.example.timerapp.data.model.DateSessionGroup;
import com.example.timerapp.data.model.ExamCountdown;
import com.example.timerapp.data.model.ExamProfile;
import com.example.timerapp.data.model.HeatmapDayData;
import com.example.timerapp.data.model.JeeAnalyticsReport;
import com.example.timerapp.data.model.MovingAveragePoint;
import com.example.timerapp.data.model.PeriodReport;
import com.example.timerapp.data.model.PerformanceTrend;
import com.example.timerapp.data.model.PersonalRecords;
import com.example.timerapp.data.model.QuestionLog;
import com.example.timerapp.data.model.Session;
import com.example.timerapp.data.model.SessionResumeData;
import com.example.timerapp.data.model.SessionStats;
import com.example.timerapp.data.model.SpeedDataPoint;
import com.example.timerapp.data.model.StudyCoachFeedback;
import com.example.timerapp.data.model.StudyRecommendation;
import com.example.timerapp.data.model.Subject;
import com.example.timerapp.data.model.SubjectPracticeStats;
import com.example.timerapp.data.model.TargetCompliance;
import com.example.timerapp.data.model.TargetSettings;
import com.example.timerapp.data.model.TimeRangeFilter;
import com.example.timerapp.data.model.TimeRangeKpiSummary;
import com.example.timerapp.data.model.TimeSlotStats;
import com.example.timerapp.data.model.WallpaperPreferences;
import com.example.timerapp.data.model.WidgetSettings;
import com.example.timerapp.model.TimerSession;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe manager for all local database interactions in the Question Solving Timer app.
 * Provides APIs for session lifecycle, per-question time logging, resumption with stats,
 * date-grouped aggregations, time-range KPI analytics, speed charts, subject tagging,
 * streak tracking, consistency & focus scoring, reports, CSV export/import, and data recovery.
 */
public class TimerDatabaseManager {

    private static final String TAG = "TimerDatabaseManager";
    private static volatile TimerDatabaseManager sInstance;

    public static final String PREFS_NAME = "question_timer_prefs";
    public static final String KEY_TARGET_TIME_PER_QUESTION_SEC = "target_time_per_question_sec";
    public static final String KEY_TARGET_SESSION_DURATION_MIN = "target_session_duration_min";
    public static final String KEY_TARGET_SESSION_QUESTIONS = "target_session_questions";
    public static final String KEY_INTERRUPTED_SESSION = "interrupted_session_state";

    private final DatabaseHelper dbHelper;
    private final Context context;
    private TargetSettings inMemoryTargetSettings = new TargetSettings();

    /**
     * Obtains singleton instance of TimerDatabaseManager.
     */
    public static TimerDatabaseManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (TimerDatabaseManager.class) {
                if (sInstance == null) {
                    sInstance = new TimerDatabaseManager(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    public TimerDatabaseManager(Context context) {
        this.context = (context != null) ? context.getApplicationContext() : null;
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    /**
     * For unit testing or custom injection of DatabaseHelper.
     */
    public TimerDatabaseManager(DatabaseHelper helper) {
        this.context = null;
        this.dbHelper = helper;
    }

    public TimerDatabaseManager(Context context, DatabaseHelper helper) {
        this.context = (context != null) ? context.getApplicationContext() : null;
        this.dbHelper = helper;
    }

    // =========================================================================
    // Core Session Operations
    // =========================================================================

    public synchronized long createSession(String name, int target) {
        return createSession(name, target, "General", "", "COMPLETED", "PRACTICE", false);
    }

    public synchronized long createSession(String name, int target, String subject, String notes, String status) {
        return createSession(name, target, subject, notes, status, "PRACTICE", false);
    }

    public synchronized long createSession(String name, int target, String subject, String notes, String status,
                                           String examProfile, boolean isMockTest) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        String finalName = (name != null && !name.trim().isEmpty())
                ? name.trim()
                : "Session " + new SimpleDateFormat("MMM dd HH:mm", Locale.US).format(new Date());

        int finalTarget = Math.max(0, target);
        long now = System.currentTimeMillis();

        boolean hasActive = hasAnyActiveSession(db);
        int isActiveVal = hasActive ? 0 : 1;

        ContentValues values = new ContentValues();
        values.put(DatabaseContract.SessionEntry.COLUMN_NAME, finalName);
        values.put(DatabaseContract.SessionEntry.COLUMN_TARGET_COUNT, finalTarget);
        values.put(DatabaseContract.SessionEntry.COLUMN_CREATED_AT, now);
        values.put(DatabaseContract.SessionEntry.COLUMN_UPDATED_AT, now);
        values.put(DatabaseContract.SessionEntry.COLUMN_IS_ACTIVE, isActiveVal);
        values.put(DatabaseContract.SessionEntry.COLUMN_SUBJECT, (subject != null && !subject.trim().isEmpty()) ? subject.trim() : "General");
        values.put(DatabaseContract.SessionEntry.COLUMN_NOTES, notes != null ? notes : "");
        values.put(DatabaseContract.SessionEntry.COLUMN_STATUS, (status != null && !status.trim().isEmpty()) ? status.trim() : "COMPLETED");
        values.put(DatabaseContract.SessionEntry.COLUMN_PROFILE_ID, "default");
        values.put(DatabaseContract.SessionEntry.COLUMN_EXAM_PROFILE, (examProfile != null && !examProfile.trim().isEmpty()) ? examProfile.trim() : "PRACTICE");
        values.put(DatabaseContract.SessionEntry.COLUMN_TOTAL_STUDY_DURATION_MS, 0L);
        values.put(DatabaseContract.SessionEntry.COLUMN_TOTAL_BREAK_DURATION_MS, 0L);
        values.put(DatabaseContract.SessionEntry.COLUMN_IS_MOCK_TEST, isMockTest ? 1 : 0);

        long id = db.insertOrThrow(DatabaseContract.SessionEntry.TABLE_NAME, null, values);
        Log.d(TAG, "Created session id=" + id + " name='" + finalName + "' subject='" + subject + "' profile=" + examProfile);
        return id;
    }

    public synchronized boolean renameSession(long sessionId, String newName) {
        if (sessionId <= 0 || newName == null || newName.trim().isEmpty()) {
            return false;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.SessionEntry.COLUMN_NAME, newName.trim());
        values.put(DatabaseContract.SessionEntry.COLUMN_UPDATED_AT, System.currentTimeMillis());

        int rows = db.update(
                DatabaseContract.SessionEntry.TABLE_NAME,
                values,
                DatabaseContract.SessionEntry.COLUMN_ALT_ID + " = ?",
                new String[]{String.valueOf(sessionId)}
        );
        return rows > 0;
    }

    public synchronized boolean setSessionTarget(long sessionId, int target) {
        if (sessionId <= 0) {
            return false;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.SessionEntry.COLUMN_TARGET_COUNT, Math.max(0, target));
        values.put(DatabaseContract.SessionEntry.COLUMN_UPDATED_AT, System.currentTimeMillis());

        int rows = db.update(
                DatabaseContract.SessionEntry.TABLE_NAME,
                values,
                DatabaseContract.SessionEntry.COLUMN_ALT_ID + " = ?",
                new String[]{String.valueOf(sessionId)}
        );
        return rows > 0;
    }

    public synchronized boolean updateSessionReflection(long sessionId, String subject, String notes,
                                                        String wentWell, String needsImprovement) {
        if (sessionId <= 0) return false;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        if (subject != null) values.put(DatabaseContract.SessionEntry.COLUMN_SUBJECT, subject.trim());
        if (notes != null) values.put(DatabaseContract.SessionEntry.COLUMN_NOTES, notes);
        if (wentWell != null) values.put(DatabaseContract.SessionEntry.COLUMN_WENT_WELL, wentWell);
        if (needsImprovement != null) values.put(DatabaseContract.SessionEntry.COLUMN_NEEDS_IMPROVEMENT, needsImprovement);
        values.put(DatabaseContract.SessionEntry.COLUMN_UPDATED_AT, System.currentTimeMillis());

        int rows = db.update(
                DatabaseContract.SessionEntry.TABLE_NAME,
                values,
                DatabaseContract.SessionEntry.COLUMN_ALT_ID + " = ?",
                new String[]{String.valueOf(sessionId)}
        );
        return rows > 0;
    }

    public synchronized boolean saveSessionNotes(long sessionId, String subject, String notes,
                                                 String wentWell, String needsImprovement) {
        return updateSessionReflection(sessionId, subject, notes, wentWell, needsImprovement);
    }

    public synchronized boolean updateSessionStatus(long sessionId, String status) {
        if (sessionId <= 0 || status == null) return false;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.SessionEntry.COLUMN_STATUS, status.trim());
        values.put(DatabaseContract.SessionEntry.COLUMN_UPDATED_AT, System.currentTimeMillis());

        int rows = db.update(
                DatabaseContract.SessionEntry.TABLE_NAME,
                values,
                DatabaseContract.SessionEntry.COLUMN_ALT_ID + " = ?",
                new String[]{String.valueOf(sessionId)}
        );
        return rows > 0;
    }

    // =========================================================================
    // Question Logging Operations
    // =========================================================================

    public synchronized long logQuestionTime(long sessionId, int questionNum, long durationMs) {
        return logQuestionTime(sessionId, questionNum, durationMs, "", "", "MEDIUM", "SOLVED", 3, "", "");
    }

    public synchronized long logQuestionTime(long sessionId, int questionNum, long durationMs,
                                             String label, String topic, String difficulty,
                                             String outcome, int confidence, String mistakeTag, String notes) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long now = System.currentTimeMillis();

        db.beginTransaction();
        try {
            ContentValues logValues = new ContentValues();
            logValues.put(DatabaseContract.QuestionLogEntry.COLUMN_SESSION_ID, sessionId);
            logValues.put(DatabaseContract.QuestionLogEntry.COLUMN_QUESTION_NUM, questionNum);
            logValues.put(DatabaseContract.QuestionLogEntry.COLUMN_DURATION_MS, Math.max(0, durationMs));
            logValues.put(DatabaseContract.QuestionLogEntry.COLUMN_COMPLETED_AT, now);
            logValues.put(DatabaseContract.QuestionLogEntry.COLUMN_LABEL, label != null ? label : "");
            logValues.put(DatabaseContract.QuestionLogEntry.COLUMN_TOPIC, topic != null ? topic : "");
            logValues.put(DatabaseContract.QuestionLogEntry.COLUMN_DIFFICULTY, difficulty != null ? difficulty : "MEDIUM");
            logValues.put(DatabaseContract.QuestionLogEntry.COLUMN_OUTCOME, outcome != null ? outcome : "SOLVED");
            logValues.put(DatabaseContract.QuestionLogEntry.COLUMN_CONFIDENCE, confidence);
            logValues.put(DatabaseContract.QuestionLogEntry.COLUMN_MISTAKE_TAG, mistakeTag != null ? mistakeTag : "");
            logValues.put(DatabaseContract.QuestionLogEntry.COLUMN_NOTES, notes != null ? notes : "");

            long logId = db.insertOrThrow(DatabaseContract.QuestionLogEntry.TABLE_NAME, null, logValues);

            ContentValues sessionValues = new ContentValues();
            sessionValues.put(DatabaseContract.SessionEntry.COLUMN_UPDATED_AT, now);
            db.update(
                    DatabaseContract.SessionEntry.TABLE_NAME,
                    sessionValues,
                    DatabaseContract.SessionEntry.COLUMN_ALT_ID + " = ?",
                    new String[]{String.valueOf(sessionId)}
            );

            db.setTransactionSuccessful();
            Log.d(TAG, "Logged question #" + questionNum + " for session " + sessionId + " (" + durationMs + "ms)");
            return logId;
        } finally {
            db.endTransaction();
        }
    }

    // =========================================================================
    // Session Resumption and Active Session Management
    // =========================================================================

    public synchronized SessionResumeData resumeSession(long sessionId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Session session = getSessionById(sessionId);
        if (session == null) {
            Log.e(TAG, "Cannot resume non-existent session ID: " + sessionId);
            return null;
        }

        long now = System.currentTimeMillis();

        db.beginTransaction();
        try {
            ContentValues deactivate = new ContentValues();
            deactivate.put(DatabaseContract.SessionEntry.COLUMN_IS_ACTIVE, 0);
            db.update(DatabaseContract.SessionEntry.TABLE_NAME, deactivate, null, null);

            ContentValues activate = new ContentValues();
            activate.put(DatabaseContract.SessionEntry.COLUMN_IS_ACTIVE, 1);
            activate.put(DatabaseContract.SessionEntry.COLUMN_UPDATED_AT, now);
            db.update(
                    DatabaseContract.SessionEntry.TABLE_NAME,
                    activate,
                    DatabaseContract.SessionEntry.COLUMN_ALT_ID + " = ?",
                    new String[]{String.valueOf(sessionId)}
            );

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        session.setActive(true);
        session.setUpdatedAt(now);

        int solvedCount = 0;
        int maxQuestionNum = 0;
        long totalElapsedMs = 0L;

        String statsQuery = "SELECT COUNT(*), COALESCE(MAX(" +
                DatabaseContract.QuestionLogEntry.COLUMN_QUESTION_NUM + "), 0), " +
                "COALESCE(SUM(" + DatabaseContract.QuestionLogEntry.COLUMN_DURATION_MS + "), 0) " +
                "FROM " + DatabaseContract.QuestionLogEntry.TABLE_NAME + " " +
                "WHERE " + DatabaseContract.QuestionLogEntry.COLUMN_SESSION_ID + " = ?";

        try (Cursor cursor = db.rawQuery(statsQuery, new String[]{String.valueOf(sessionId)})) {
            if (cursor.moveToFirst()) {
                solvedCount = cursor.getInt(0);
                maxQuestionNum = cursor.getInt(1);
                totalElapsedMs = cursor.getLong(2);
            }
        }

        int nextQuestionNum = (maxQuestionNum > 0) ? (maxQuestionNum + 1) : 1;
        long averageDurationMs = (solvedCount > 0) ? (totalElapsedMs / solvedCount) : 0L;

        List<QuestionLog> logs = getQuestionsForSession(sessionId);

        return new SessionResumeData(
                session,
                solvedCount,
                nextQuestionNum,
                totalElapsedMs,
                averageDurationMs,
                logs
        );
    }

    public synchronized Session getActiveSession() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        String activeQuery = "SELECT " +
                DatabaseContract.SessionEntry.COLUMN_ALT_ID + ", " +
                DatabaseContract.SessionEntry.COLUMN_NAME + ", " +
                DatabaseContract.SessionEntry.COLUMN_TARGET_COUNT + ", " +
                DatabaseContract.SessionEntry.COLUMN_CREATED_AT + ", " +
                DatabaseContract.SessionEntry.COLUMN_UPDATED_AT + ", " +
                DatabaseContract.SessionEntry.COLUMN_IS_ACTIVE + ", " +
                DatabaseContract.SessionEntry.COLUMN_SUBJECT + ", " +
                DatabaseContract.SessionEntry.COLUMN_NOTES + ", " +
                DatabaseContract.SessionEntry.COLUMN_WENT_WELL + ", " +
                DatabaseContract.SessionEntry.COLUMN_NEEDS_IMPROVEMENT + ", " +
                DatabaseContract.SessionEntry.COLUMN_STATUS + ", " +
                DatabaseContract.SessionEntry.COLUMN_PROFILE_ID +
                " FROM " + DatabaseContract.SessionEntry.TABLE_NAME +
                " WHERE " + DatabaseContract.SessionEntry.COLUMN_IS_ACTIVE + " = 1 " +
                " ORDER BY " + DatabaseContract.SessionEntry.COLUMN_UPDATED_AT + " DESC LIMIT 1";

        try (Cursor cursor = db.rawQuery(activeQuery, null)) {
            if (cursor.moveToFirst()) {
                return parseSessionCursor(cursor);
            }
        }

        String latestQuery = "SELECT " +
                DatabaseContract.SessionEntry.COLUMN_ALT_ID + ", " +
                DatabaseContract.SessionEntry.COLUMN_NAME + ", " +
                DatabaseContract.SessionEntry.COLUMN_TARGET_COUNT + ", " +
                DatabaseContract.SessionEntry.COLUMN_CREATED_AT + ", " +
                DatabaseContract.SessionEntry.COLUMN_UPDATED_AT + ", " +
                DatabaseContract.SessionEntry.COLUMN_IS_ACTIVE + ", " +
                DatabaseContract.SessionEntry.COLUMN_SUBJECT + ", " +
                DatabaseContract.SessionEntry.COLUMN_NOTES + ", " +
                DatabaseContract.SessionEntry.COLUMN_WENT_WELL + ", " +
                DatabaseContract.SessionEntry.COLUMN_NEEDS_IMPROVEMENT + ", " +
                DatabaseContract.SessionEntry.COLUMN_STATUS + ", " +
                DatabaseContract.SessionEntry.COLUMN_PROFILE_ID +
                " FROM " + DatabaseContract.SessionEntry.TABLE_NAME +
                " ORDER BY " + DatabaseContract.SessionEntry.COLUMN_UPDATED_AT + " DESC LIMIT 1";

        try (Cursor cursor = db.rawQuery(latestQuery, null)) {
            if (cursor.moveToFirst()) {
                Session session = parseSessionCursor(cursor);
                ContentValues activate = new ContentValues();
                activate.put(DatabaseContract.SessionEntry.COLUMN_IS_ACTIVE, 1);
                db.update(
                        DatabaseContract.SessionEntry.TABLE_NAME,
                        activate,
                        DatabaseContract.SessionEntry.COLUMN_ALT_ID + " = ?",
                        new String[]{String.valueOf(session.getId())}
                );
                session.setActive(true);
                return session;
            }
        }

        long newId = createSession("DEFAULT SESSION", 30);
        ContentValues activate = new ContentValues();
        activate.put(DatabaseContract.SessionEntry.COLUMN_IS_ACTIVE, 1);
        db.update(
                DatabaseContract.SessionEntry.TABLE_NAME,
                activate,
                DatabaseContract.SessionEntry.COLUMN_ALT_ID + " = ?",
                new String[]{String.valueOf(newId)}
        );

        return getSessionById(newId);
    }

    public synchronized TimerSession getActiveTimerSession() {
        Session active = getActiveSession();
        if (active == null) return null;
        return loadTimerSession(active.getId());
    }

    public synchronized TimerSession loadTimerSession(long sessionId) {
        Session session = getSessionById(sessionId);
        if (session == null) return null;

        TimerSession timerSession = new TimerSession(session.getId(), session.getName(), session.getTargetCount());
        timerSession.setCreatedAt(session.getCreatedAt());
        timerSession.setUpdatedAt(session.getUpdatedAt());
        timerSession.setSubject(session.getSubject());
        timerSession.setNotes(session.getNotes());
        timerSession.setWhatWentWell(session.getWentWell());
        timerSession.setNeedsImprovement(session.getNeedsImprovement());
        timerSession.setStatus(session.getStatus());
        timerSession.setProfileId(session.getProfileId());

        List<QuestionLog> logs = getQuestionsForSession(sessionId);
        for (QuestionLog log : logs) {
            timerSession.addLog(log.toUiModel());
        }
        return timerSession;
    }

    // =========================================================================
    // Statistics & Queries
    // =========================================================================

    public synchronized List<SessionStats> getAllSessionsWithStats() {
        return getSessionsWithStatsInRange(0L, Long.MAX_VALUE);
    }

    public synchronized List<SessionStats> getSessionsWithStatsInRange(long startTimestamp, long endTimestamp) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<SessionStats> list = new ArrayList<>();

        boolean isAllTime = (startTimestamp <= 0 && endTimestamp >= Long.MAX_VALUE);
        String query;
        String[] args;

        String selectColumns = "s." + DatabaseContract.SessionEntry.COLUMN_ALT_ID + ", " +
                "s." + DatabaseContract.SessionEntry.COLUMN_NAME + ", " +
                "s." + DatabaseContract.SessionEntry.COLUMN_TARGET_COUNT + ", " +
                "s." + DatabaseContract.SessionEntry.COLUMN_CREATED_AT + ", " +
                "s." + DatabaseContract.SessionEntry.COLUMN_UPDATED_AT + ", " +
                "s." + DatabaseContract.SessionEntry.COLUMN_IS_ACTIVE + ", " +
                "s." + DatabaseContract.SessionEntry.COLUMN_SUBJECT + ", " +
                "s." + DatabaseContract.SessionEntry.COLUMN_NOTES + ", " +
                "s." + DatabaseContract.SessionEntry.COLUMN_WENT_WELL + ", " +
                "s." + DatabaseContract.SessionEntry.COLUMN_NEEDS_IMPROVEMENT + ", " +
                "s." + DatabaseContract.SessionEntry.COLUMN_STATUS + ", " +
                "s." + DatabaseContract.SessionEntry.COLUMN_PROFILE_ID + ", " +
                "COUNT(q." + DatabaseContract.QuestionLogEntry.COLUMN_ALT_ID + ") AS solved_count, " +
                "COALESCE(SUM(q." + DatabaseContract.QuestionLogEntry.COLUMN_DURATION_MS + "), 0) AS total_duration ";

        if (isAllTime) {
            query = "SELECT " + selectColumns +
                    "FROM " + DatabaseContract.SessionEntry.TABLE_NAME + " s " +
                    "LEFT JOIN " + DatabaseContract.QuestionLogEntry.TABLE_NAME + " q " +
                    "ON s." + DatabaseContract.SessionEntry.COLUMN_ALT_ID + " = q." + DatabaseContract.QuestionLogEntry.COLUMN_SESSION_ID + " " +
                    "GROUP BY s." + DatabaseContract.SessionEntry.COLUMN_ALT_ID + " " +
                    "ORDER BY s." + DatabaseContract.SessionEntry.COLUMN_CREATED_AT + " ASC";
            args = null;
        } else {
            query = "SELECT " + selectColumns +
                    "FROM " + DatabaseContract.SessionEntry.TABLE_NAME + " s " +
                    "LEFT JOIN " + DatabaseContract.QuestionLogEntry.TABLE_NAME + " q " +
                    "ON s." + DatabaseContract.SessionEntry.COLUMN_ALT_ID + " = q." + DatabaseContract.QuestionLogEntry.COLUMN_SESSION_ID + " " +
                    "WHERE (s." + DatabaseContract.SessionEntry.COLUMN_CREATED_AT + " >= ? AND s." + DatabaseContract.SessionEntry.COLUMN_CREATED_AT + " <= ?) " +
                    "   OR (s." + DatabaseContract.SessionEntry.COLUMN_UPDATED_AT + " >= ? AND s." + DatabaseContract.SessionEntry.COLUMN_UPDATED_AT + " <= ?) " +
                    "GROUP BY s." + DatabaseContract.SessionEntry.COLUMN_ALT_ID + " " +
                    "ORDER BY s." + DatabaseContract.SessionEntry.COLUMN_CREATED_AT + " ASC";
            args = new String[]{
                    String.valueOf(startTimestamp), String.valueOf(endTimestamp),
                    String.valueOf(startTimestamp), String.valueOf(endTimestamp)
            };
        }

        try (Cursor cursor = db.rawQuery(query, args)) {
            while (cursor.moveToNext()) {
                long sessionId = cursor.getLong(0);
                String name = cursor.getString(1);
                int targetCount = cursor.getInt(2);
                long createdAt = cursor.getLong(3);
                long updatedAt = cursor.getLong(4);
                boolean isActive = cursor.getInt(5) == 1;
                String subject = cursor.getString(6);
                String notes = cursor.getString(7);
                String wentWell = cursor.getString(8);
                String needsImprovement = cursor.getString(9);
                String status = cursor.getString(10);
                String profileId = cursor.getString(11);
                int solvedCount = cursor.getInt(12);
                long totalDurationMs = cursor.getLong(13);
                long averageDurationMs = (solvedCount > 0) ? (totalDurationMs / solvedCount) : 0L;

                list.add(new SessionStats(
                        sessionId,
                        name,
                        targetCount,
                        solvedCount,
                        totalDurationMs,
                        averageDurationMs,
                        createdAt,
                        updatedAt,
                        isActive,
                        subject,
                        notes,
                        wentWell,
                        needsImprovement,
                        status,
                        profileId
                ));
            }
        }
        return list;
    }

    // =========================================================================
    // Date Grouping Operations
    // =========================================================================

    public synchronized List<DateSessionGroup> getSessionsGroupedByDate() {
        return getSessionsGroupedByDate(0L, Long.MAX_VALUE);
    }

    public synchronized List<DateSessionGroup> getSessionsGroupedByDate(TimeRangeFilter filter) {
        long now = System.currentTimeMillis();
        long start = (filter != null) ? filter.getStartTimestamp(now) : 0L;
        long end = (filter != null) ? filter.getEndTimestamp(now) : Long.MAX_VALUE;
        return getSessionsGroupedByDate(start, end);
    }

    public synchronized List<DateSessionGroup> getSessionsGroupedByDate(long startTimestamp, long endTimestamp) {
        List<SessionStats> sessions = getSessionsWithStatsInRange(startTimestamp, endTimestamp);
        Map<Long, List<SessionStats>> groupedMap = new TreeMap<>(Collections.reverseOrder());

        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());

        for (SessionStats session : sessions) {
            long ts = session.getCreatedAt() > 0 ? session.getCreatedAt() : session.getUpdatedAt();
            if (ts <= 0) ts = System.currentTimeMillis();

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(ts);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long midnight = cal.getTimeInMillis();

            List<SessionStats> dayList = groupedMap.computeIfAbsent(midnight, k -> new ArrayList<>());
            dayList.add(session);
        }

        List<DateSessionGroup> result = new ArrayList<>(groupedMap.size());
        for (Map.Entry<Long, List<SessionStats>> entry : groupedMap.entrySet()) {
            long midnight = entry.getKey();
            List<SessionStats> daySessions = entry.getValue();

            Date dateObj = new Date(midnight);
            String isoDate = isoFormat.format(dateObj);
            String formattedDate = displayFormat.format(dateObj);

            long totalDurationMs = 0L;
            int totalQuestionsSolved = 0;
            for (SessionStats s : daySessions) {
                totalDurationMs += s.getTotalDurationMs();
                totalQuestionsSolved += s.getSolvedCount();
            }

            result.add(new DateSessionGroup(
                    isoDate,
                    formattedDate,
                    midnight,
                    daySessions.size(),
                    totalDurationMs,
                    totalQuestionsSolved,
                    daySessions
            ));
        }

        return result;
    }

    // =========================================================================
    // Time Range Analytics Filtering & KPIs
    // =========================================================================

    public synchronized TimeRangeKpiSummary getAnalyticsSummary(TimeRangeFilter filter) {
        long now = System.currentTimeMillis();
        long start = (filter != null) ? filter.getStartTimestamp(now) : 0L;
        long end = (filter != null) ? filter.getEndTimestamp(now) : Long.MAX_VALUE;
        return getAnalyticsSummary(start, end, filter);
    }

    public synchronized TimeRangeKpiSummary getAnalyticsSummary(long startTimestamp, long endTimestamp) {
        return getAnalyticsSummary(startTimestamp, endTimestamp, null);
    }

    public synchronized TimeRangeKpiSummary getAnalyticsSummary(long startTimestamp, long endTimestamp, TimeRangeFilter rangeFilter) {
        List<SessionStats> sessions = getSessionsWithStatsInRange(startTimestamp, endTimestamp);

        int totalSessionsCount = sessions.size();
        int totalCompletedSessions = 0;
        long totalPracticeTimeMs = 0L;
        int totalQuestionsCompleted = 0;

        SessionStats fastestSession = null;
        long fastestAvgPaceMs = Long.MAX_VALUE;

        SessionStats bestSessionPrimary = null;
        double bestScorePrimary = -1.0;

        SessionStats bestSessionFallback = null;
        double bestScoreFallback = -1.0;

        for (SessionStats s : sessions) {
            totalPracticeTimeMs += s.getTotalDurationMs();
            totalQuestionsCompleted += s.getSolvedCount();

            if ((s.getTargetCount() > 0 && s.getSolvedCount() >= s.getTargetCount()) ||
                (s.getTargetCount() == 0 && s.getSolvedCount() > 0)) {
                totalCompletedSessions++;
            }

            if (s.getSolvedCount() > 0) {
                if (s.getAverageDurationMs() < fastestAvgPaceMs) {
                    fastestAvgPaceMs = s.getAverageDurationMs();
                    fastestSession = s;
                } else if (s.getAverageDurationMs() == fastestAvgPaceMs && fastestSession != null) {
                    if (s.getTotalDurationMs() < fastestSession.getTotalDurationMs()) {
                        fastestSession = s;
                    }
                }

                double avgSec = s.getAverageDurationMs() / 1000.0;
                double score = (s.getSolvedCount() * 100.0) / Math.max(0.001, avgSec);

                if (s.getSolvedCount() >= 3) {
                    if (score > bestScorePrimary) {
                        bestScorePrimary = score;
                        bestSessionPrimary = s;
                    } else if (Math.abs(score - bestScorePrimary) < 1e-6 && bestSessionPrimary != null) {
                        if (s.getSolvedCount() > bestSessionPrimary.getSolvedCount()) {
                            bestSessionPrimary = s;
                        } else if (s.getSolvedCount() == bestSessionPrimary.getSolvedCount() &&
                                   s.getTotalDurationMs() < bestSessionPrimary.getTotalDurationMs()) {
                            bestSessionPrimary = s;
                        }
                    }
                }

                if (score > bestScoreFallback) {
                    bestScoreFallback = score;
                    bestSessionFallback = s;
                }
            }
        }

        SessionStats bestPerformingSession = (bestSessionPrimary != null) ? bestSessionPrimary : bestSessionFallback;
        double bestScore = (bestSessionPrimary != null) ? bestScorePrimary : Math.max(0.0, bestScoreFallback);

        long averageSessionDurationMs = (totalSessionsCount > 0) ? (totalPracticeTimeMs / totalSessionsCount) : 0L;
        long overallAverageTimePerQuestionMs = (totalQuestionsCompleted > 0) ? (totalPracticeTimeMs / totalQuestionsCompleted) : 0L;

        return new TimeRangeKpiSummary(
                rangeFilter,
                startTimestamp,
                endTimestamp,
                totalSessionsCount,
                totalCompletedSessions,
                totalPracticeTimeMs,
                averageSessionDurationMs,
                totalQuestionsCompleted,
                overallAverageTimePerQuestionMs,
                fastestSession,
                bestPerformingSession,
                bestScore,
                sessions
        );
    }

    // =========================================================================
    // Advanced Analytics & Statistical Engines
    // =========================================================================

    public synchronized double calculateConsistencyScore() {
        return calculateConsistencyScore(TimeRangeFilter.RANGE_30_DAYS);
    }

    public synchronized double calculateConsistencyScore(TimeRangeFilter filter) {
        long now = System.currentTimeMillis();
        long start = (filter != null) ? filter.getStartTimestamp(now) : 0L;
        long end = (filter != null) ? filter.getEndTimestamp(now) : Long.MAX_VALUE;
        return calculateConsistencyScore(start, end);
    }

    public synchronized double calculateConsistencyScore(long startTimestamp, long endTimestamp) {
        List<SessionStats> sessions = getSessionsWithStatsInRange(startTimestamp, endTimestamp);
        if (sessions.isEmpty()) {
            return 0.0;
        }

        Set<Long> activeDays = new HashSet<>();
        int sessionsMeetingTarget = 0;

        for (SessionStats s : sessions) {
            long ts = s.getCreatedAt() > 0 ? s.getCreatedAt() : s.getUpdatedAt();
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(ts);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            activeDays.add(cal.getTimeInMillis());

            if (s.getTargetCount() > 0 && s.getSolvedCount() >= s.getTargetCount()) {
                sessionsMeetingTarget++;
            } else if (s.getTargetCount() == 0 && s.getSolvedCount() > 0) {
                sessionsMeetingTarget++;
            }
        }

        long actualStart = (startTimestamp > 0) ? startTimestamp : sessions.get(0).getCreatedAt();
        long actualEnd = (endTimestamp < Long.MAX_VALUE) ? endTimestamp : System.currentTimeMillis();
        long totalDaysInRange = Math.max(1, TimeUnit.MILLISECONDS.toDays(Math.max(0, actualEnd - actualStart)) + 1);

        double dayFactor = ((double) activeDays.size() / totalDaysInRange) * 60.0;
        double targetFactor = ((double) sessionsMeetingTarget / Math.max(1, sessions.size())) * 40.0;

        double consistency = Math.min(100.0, Math.max(0.0, dayFactor + targetFactor));
        return Math.round(consistency * 10.0) / 10.0;
    }

    public synchronized double calculateFocusScore() {
        return calculateFocusScore(TimeRangeFilter.RANGE_30_DAYS);
    }

    public synchronized double calculateFocusScore(TimeRangeFilter filter) {
        long now = System.currentTimeMillis();
        long start = (filter != null) ? filter.getStartTimestamp(now) : 0L;
        long end = (filter != null) ? filter.getEndTimestamp(now) : Long.MAX_VALUE;
        return calculateFocusScore(start, end);
    }

    public synchronized double calculateFocusScore(long startTimestamp, long endTimestamp) {
        List<SessionStats> sessions = getSessionsWithStatsInRange(startTimestamp, endTimestamp);
        if (sessions.isEmpty()) {
            return 0.0;
        }

        int targetMetCount = 0;
        long totalDuration = 0L;
        int sessionsWithQuestions = 0;

        for (SessionStats s : sessions) {
            totalDuration += s.getTotalDurationMs();
            if (s.getSolvedCount() > 0) sessionsWithQuestions++;
            if ((s.getTargetCount() > 0 && s.getSolvedCount() >= s.getTargetCount()) ||
                (s.getTargetCount() == 0 && s.getSolvedCount() > 0)) {
                targetMetCount++;
            }
        }

        double targetScore = ((double) targetMetCount / sessions.size()) * 40.0;
        double avgDurationMinutes = (totalDuration / (double) sessions.size()) / 60000.0;
        double durationScore = Math.min(35.0, (avgDurationMinutes / 45.0) * 35.0);
        double engagementScore = (sessionsWithQuestions > 0) ? Math.min(25.0, ((double) sessionsWithQuestions / sessions.size()) * 25.0) : 0.0;

        double focusScore = Math.min(100.0, Math.max(0.0, targetScore + durationScore + engagementScore));
        return Math.round(focusScore * 10.0) / 10.0;
    }

    public synchronized int calculateCurrentStreak() {
        return calculateCurrentStreak(System.currentTimeMillis());
    }

    public synchronized int calculateCurrentStreak(long now) {
        if (now <= 0) now = System.currentTimeMillis();

        List<DateSessionGroup> groups = getSessionsGroupedByDate();
        if (groups.isEmpty()) {
            return 0;
        }

        Set<Long> activeMidnights = new HashSet<>();
        for (DateSessionGroup g : groups) {
            if (g.getTotalQuestionsSolved() >= 1 || g.getTotalPracticeDurationMs() >= 600000L) {
                activeMidnights.add(g.getRawDateMillis());
            }
        }

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long todayMidnight = cal.getTimeInMillis();

        cal.add(Calendar.DAY_OF_YEAR, -1);
        long yesterdayMidnight = cal.getTimeInMillis();

        long checkMidnight;
        int streak = 0;

        if (activeMidnights.contains(todayMidnight)) {
            checkMidnight = todayMidnight;
        } else if (activeMidnights.contains(yesterdayMidnight)) {
            checkMidnight = yesterdayMidnight;
        } else {
            return 0;
        }

        cal.setTimeInMillis(checkMidnight);
        while (activeMidnights.contains(cal.getTimeInMillis())) {
            streak++;
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }

        return streak;
    }

    public synchronized int calculateLongestStreak() {
        List<DateSessionGroup> groups = getSessionsGroupedByDate();
        if (groups.isEmpty()) {
            return 0;
        }

        List<Long> activeDays = new ArrayList<>();
        for (DateSessionGroup g : groups) {
            if (g.getTotalQuestionsSolved() >= 1 || g.getTotalPracticeDurationMs() >= 600000L) {
                activeDays.add(g.getRawDateMillis());
            }
        }
        Collections.sort(activeDays);

        if (activeDays.isEmpty()) return 0;

        int longest = 1;
        int current = 1;

        for (int i = 1; i < activeDays.size(); i++) {
            long prev = activeDays.get(i - 1);
            long curr = activeDays.get(i);

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(prev);
            cal.add(Calendar.DAY_OF_YEAR, 1);
            long expectedNext = cal.getTimeInMillis();

            if (curr == expectedNext) {
                current++;
                if (current > longest) {
                    longest = current;
                }
            } else if (curr > expectedNext) {
                current = 1;
            }
        }

        return longest;
    }

    public synchronized double calculateSpeedImprovementPercentage(TimeRangeFilter currentRange, TimeRangeFilter previousRange) {
        TimeRangeKpiSummary cur = getAnalyticsSummary(currentRange);
        TimeRangeKpiSummary prev = getAnalyticsSummary(previousRange);
        return calculateSpeedImprovementPercentage(cur.getOverallAverageTimePerQuestionMs(), prev.getOverallAverageTimePerQuestionMs());
    }

    public double calculateSpeedImprovementPercentage(long currentAvgPaceMs, long previousAvgPaceMs) {
        if (previousAvgPaceMs <= 0 || currentAvgPaceMs <= 0) {
            return 0.0;
        }
        double improvement = (((double) previousAvgPaceMs - currentAvgPaceMs) / previousAvgPaceMs) * 100.0;
        return Math.round(improvement * 10.0) / 10.0;
    }

    public synchronized PeriodReport getWeeklyReport(int offsetWeeks) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        cal.add(Calendar.WEEK_OF_YEAR, -offsetWeeks);

        long startThisWeek = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_YEAR, 7);
        long endThisWeek = cal.getTimeInMillis() - 1;

        cal.setTimeInMillis(startThisWeek);
        cal.add(Calendar.WEEK_OF_YEAR, -1);
        long startPrevWeek = cal.getTimeInMillis();
        long endPrevWeek = startThisWeek - 1;

        return buildPeriodReport(PeriodReport.PeriodType.WEEKLY, startThisWeek, endThisWeek, startPrevWeek, endPrevWeek);
    }

    public synchronized PeriodReport getMonthlyReport(int offsetMonths) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.MONTH, -offsetMonths);

        long startThisMonth = cal.getTimeInMillis();
        cal.add(Calendar.MONTH, 1);
        long endThisMonth = cal.getTimeInMillis() - 1;

        cal.setTimeInMillis(startThisMonth);
        cal.add(Calendar.MONTH, -1);
        long startPrevMonth = cal.getTimeInMillis();
        long endPrevMonth = startThisMonth - 1;

        return buildPeriodReport(PeriodReport.PeriodType.MONTHLY, startThisMonth, endThisMonth, startPrevMonth, endPrevMonth);
    }

    private PeriodReport buildPeriodReport(PeriodReport.PeriodType type, long start, long end, long prevStart, long prevEnd) {
        List<SessionStats> currentSessions = getSessionsWithStatsInRange(start, end);
        List<SessionStats> prevSessions = getSessionsWithStatsInRange(prevStart, prevEnd);

        int sessionCount = currentSessions.size();
        long totalDurationMs = 0L;
        int totalQuestions = 0;

        for (SessionStats s : currentSessions) {
            totalDurationMs += s.getTotalDurationMs();
            totalQuestions += s.getSolvedCount();
        }

        long prevDurationMs = 0L;
        for (SessionStats s : prevSessions) {
            prevDurationMs += s.getTotalDurationMs();
        }

        double comparisonPercentage = 0.0;
        if (prevDurationMs > 0) {
            comparisonPercentage = Math.round((((double) totalDurationMs - prevDurationMs) / prevDurationMs) * 1000.0) / 10.0;
        }

        long avgSessionDuration = sessionCount > 0 ? (totalDurationMs / sessionCount) : 0L;
        long avgPace = totalQuestions > 0 ? (totalDurationMs / totalQuestions) : 0L;

        List<DailyPracticeDataPoint> dailyList = getDailyPracticeTimeData(start, end);

        String bestDayLabel = "N/A";
        double bestDayMinutes = 0.0;
        for (DailyPracticeDataPoint d : dailyList) {
            if (d.getPracticeMinutes() > bestDayMinutes) {
                bestDayMinutes = d.getPracticeMinutes();
                bestDayLabel = d.getDateLabel();
            }
        }

        double consistency = calculateConsistencyScore(start, end);

        SimpleDateFormat sdf = (type == PeriodReport.PeriodType.WEEKLY)
                ? new SimpleDateFormat("MMM d - ", Locale.getDefault())
                : new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

        String periodLabel;
        if (type == PeriodReport.PeriodType.WEEKLY) {
            SimpleDateFormat endSdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            periodLabel = sdf.format(new Date(start)) + endSdf.format(new Date(end));
        } else {
            periodLabel = sdf.format(new Date(start));
        }

        return new PeriodReport(
                type,
                periodLabel,
                start,
                end,
                sessionCount,
                totalDurationMs,
                avgSessionDuration,
                totalQuestions,
                avgPace,
                comparisonPercentage,
                bestDayLabel,
                bestDayMinutes,
                consistency,
                dailyList
        );
    }

    public synchronized List<HeatmapDayData> getMonthlyHeatmapData(int year, int month) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month > 0 ? month - 1 : 0);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        long startMonth = cal.getTimeInMillis();
        int maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        cal.add(Calendar.MONTH, 1);
        long endMonth = cal.getTimeInMillis() - 1;

        List<SessionStats> sessions = getSessionsWithStatsInRange(startMonth, endMonth);
        Map<Integer, long[]> dayStatsMap = new HashMap<>();

        for (SessionStats s : sessions) {
            long ts = s.getCreatedAt() > 0 ? s.getCreatedAt() : s.getUpdatedAt();
            Calendar sessionCal = Calendar.getInstance();
            sessionCal.setTimeInMillis(ts);
            int day = sessionCal.get(Calendar.DAY_OF_MONTH);

            long[] arr = dayStatsMap.computeIfAbsent(day, k -> new long[3]);
            arr[0] += s.getTotalDurationMs();
            arr[1] += 1;
            arr[2] += s.getSolvedCount();
        }

        List<HeatmapDayData> heatmap = new ArrayList<>(maxDays);
        for (int day = 1; day <= maxDays; day++) {
            cal.setTimeInMillis(startMonth);
            cal.set(Calendar.DAY_OF_MONTH, day);
            long dayMillis = cal.getTimeInMillis();

            long[] arr = dayStatsMap.get(day);
            long durationMs = (arr != null) ? arr[0] : 0L;
            int count = (arr != null) ? (int) arr[1] : 0;
            int questions = (arr != null) ? (int) arr[2] : 0;
            double minutes = durationMs / 60000.0;

            heatmap.add(new HeatmapDayData(day, dayMillis, minutes, durationMs, count, questions));
        }

        return heatmap;
    }

    public synchronized List<TimeSlotStats> getTimeSlotStats() {
        return getBestPracticeTimeAnalysis();
    }

    public synchronized List<TimeSlotStats> getBestPracticeTimeAnalysis() {
        List<SessionStats> sessions = getAllSessionsWithStats();
        Map<TimeSlotStats.SlotType, long[]> stats = new LinkedHashMap<>();

        for (TimeSlotStats.SlotType slot : TimeSlotStats.SlotType.values()) {
            stats.put(slot, new long[]{0L, 0L, 0L, Long.MAX_VALUE});
        }

        for (SessionStats s : sessions) {
            long ts = s.getCreatedAt() > 0 ? s.getCreatedAt() : s.getUpdatedAt();
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(ts);
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            TimeSlotStats.SlotType slot = TimeSlotStats.SlotType.fromHour(hour);

            long[] arr = stats.get(slot);
            if (arr != null) {
                arr[0] += 1;
                arr[1] += s.getTotalDurationMs();
                arr[2] += s.getSolvedCount();
                if (s.getSolvedCount() > 0 && s.getAverageDurationMs() < arr[3]) {
                    arr[3] = s.getAverageDurationMs();
                }
            }
        }

        List<TimeSlotStats> result = new ArrayList<>();
        for (Map.Entry<TimeSlotStats.SlotType, long[]> entry : stats.entrySet()) {
            TimeSlotStats.SlotType slot = entry.getKey();
            long[] arr = entry.getValue();
            int count = (int) arr[0];
            long duration = arr[1];
            int questions = (int) arr[2];
            long bestPace = arr[3];
            long avgPace = questions > 0 ? (duration / questions) : 0L;

            result.add(new TimeSlotStats(slot, count, duration, questions, avgPace, bestPace));
        }
        return result;
    }

    public synchronized TimeSlotStats getBestTimeSlot() {
        List<TimeSlotStats> slots = getBestPracticeTimeAnalysis();
        TimeSlotStats best = null;
        long fastestPace = Long.MAX_VALUE;

        for (TimeSlotStats s : slots) {
            if (s.getTotalQuestionsSolved() >= 3 && s.getAveragePaceMs() < fastestPace) {
                fastestPace = s.getAveragePaceMs();
                best = s;
            }
        }

        if (best == null) {
            for (TimeSlotStats s : slots) {
                if (s.getSessionCount() > 0 && (best == null || s.getTotalPracticeTimeMs() > best.getTotalPracticeTimeMs())) {
                    best = s;
                }
            }
        }

        return best != null ? best : (slots.isEmpty() ? null : slots.get(0));
    }

    public synchronized List<MovingAveragePoint> getMovingAveragePoints(int windowSize) {
        return getMovingAverageSeries(windowSize);
    }

    public synchronized List<MovingAveragePoint> getMovingAverageSeries(int windowSize) {
        int window = Math.max(1, windowSize);
        List<SessionStats> sessions = getSessionsWithStatsInRange(0L, Long.MAX_VALUE);
        List<MovingAveragePoint> result = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());

        for (int i = 0; i < sessions.size(); i++) {
            SessionStats cur = sessions.get(i);
            int startIdx = Math.max(0, i - window + 1);
            int count = (i - startIdx + 1);

            double sumPaceSec = 0.0;
            double sumDurationMin = 0.0;
            int pacedCount = 0;

            for (int j = startIdx; j <= i; j++) {
                SessionStats s = sessions.get(j);
                sumDurationMin += (s.getTotalDurationMs() / 60000.0);
                if (s.getSolvedCount() > 0) {
                    sumPaceSec += (s.getAverageDurationMs() / 1000.0);
                    pacedCount++;
                }
            }

            double rollingPace = pacedCount > 0 ? (sumPaceSec / pacedCount) : 0.0;
            double rollingDuration = count > 0 ? (sumDurationMin / count) : 0.0;
            long ts = cur.getCreatedAt() > 0 ? cur.getCreatedAt() : cur.getUpdatedAt();

            result.add(new MovingAveragePoint(
                    i + 1,
                    cur.getSessionId(),
                    cur.getSessionName(),
                    ts,
                    sdf.format(new Date(ts)),
                    Math.round(rollingPace * 10.0) / 10.0,
                    Math.round(rollingDuration * 10.0) / 10.0,
                    window
            ));
        }

        return result;
    }

    public synchronized PersonalRecords getPersonalRecords() {
        List<SessionStats> sessions = getAllSessionsWithStats();
        List<DateSessionGroup> dateGroups = getSessionsGroupedByDate();

        SessionStats longestSession = null;
        SessionStats fastestPaceSession = null;
        long fastestPaceMs = Long.MAX_VALUE;

        for (SessionStats s : sessions) {
            if (longestSession == null || s.getTotalDurationMs() > longestSession.getTotalDurationMs()) {
                longestSession = s;
            }
            if (s.getSolvedCount() >= 3 && s.getAverageDurationMs() < fastestPaceMs) {
                fastestPaceMs = s.getAverageDurationMs();
                fastestPaceSession = s;
            }
        }

        if (fastestPaceSession == null) {
            for (SessionStats s : sessions) {
                if (s.getSolvedCount() > 0 && s.getAverageDurationMs() < fastestPaceMs) {
                    fastestPaceMs = s.getAverageDurationMs();
                    fastestPaceSession = s;
                }
            }
        }

        long mostPracticeMillis = 0L;
        String mostPracticeLabel = "N/A";
        long mostPracticeDuration = 0L;

        long mostSessionsMillis = 0L;
        String mostSessionsLabel = "N/A";
        int mostSessionsCount = 0;

        for (DateSessionGroup g : dateGroups) {
            if (g.getTotalPracticeDurationMs() > mostPracticeDuration) {
                mostPracticeDuration = g.getTotalPracticeDurationMs();
                mostPracticeMillis = g.getRawDateMillis();
                mostPracticeLabel = g.getFormattedDate();
            }
            if (g.getTotalSessionsCount() > mostSessionsCount) {
                mostSessionsCount = g.getTotalSessionsCount();
                mostSessionsMillis = g.getRawDateMillis();
                mostSessionsLabel = g.getFormattedDate();
            }
        }

        int longestStreak = calculateLongestStreak();

        return new PersonalRecords(
                longestSession,
                mostPracticeMillis,
                mostPracticeLabel,
                mostPracticeDuration,
                mostSessionsMillis,
                mostSessionsLabel,
                mostSessionsCount,
                fastestPaceSession,
                longestStreak
        );
    }

    public synchronized PerformanceTrend getPerformanceTrend() {
        List<SessionStats> sessions = getSessionsWithStatsInRange(0L, Long.MAX_VALUE);
        if (sessions.size() < 3) {
            return PerformanceTrend.STABLE;
        }

        long totalDuration = 0L;
        int totalQuestions = 0;
        for (SessionStats s : sessions) {
            totalDuration += s.getTotalDurationMs();
            totalQuestions += s.getSolvedCount();
        }
        if (totalQuestions == 0) return PerformanceTrend.STABLE;
        double overallAvgPaceMs = (double) totalDuration / totalQuestions;

        int window = Math.min(5, sessions.size());
        long recentDuration = 0L;
        int recentQuestions = 0;
        for (int i = sessions.size() - window; i < sessions.size(); i++) {
            SessionStats s = sessions.get(i);
            recentDuration += s.getTotalDurationMs();
            recentQuestions += s.getSolvedCount();
        }
        if (recentQuestions == 0) return PerformanceTrend.STABLE;
        double recentAvgPaceMs = (double) recentDuration / recentQuestions;

        double ratio = recentAvgPaceMs / overallAvgPaceMs;
        if (ratio <= 0.95) {
            return PerformanceTrend.IMPROVING;
        } else if (ratio >= 1.05) {
            return PerformanceTrend.DECLINING;
        } else {
            return PerformanceTrend.STABLE;
        }
    }

    public synchronized List<SubjectPracticeStats> getSubjectPracticeStats() {
        List<SessionStats> sessions = getAllSessionsWithStats();
        List<Subject> subjects = getAllSubjects();
        Map<String, String> colorMap = new HashMap<>();
        for (Subject sub : subjects) {
            colorMap.put(sub.getName().toLowerCase(Locale.ROOT), sub.getColorHex());
        }

        long overallPracticeMs = 0L;
        Map<String, long[]> map = new LinkedHashMap<>();

        for (SessionStats s : sessions) {
            String subj = s.getSubject();
            if (subj == null || subj.trim().isEmpty()) subj = "General";
            overallPracticeMs += s.getTotalDurationMs();

            long[] arr = map.computeIfAbsent(subj, k -> new long[3]);
            arr[0] += s.getTotalDurationMs();
            arr[1] += 1;
            arr[2] += s.getSolvedCount();
        }

        List<SubjectPracticeStats> list = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : map.entrySet()) {
            String subj = entry.getKey();
            long[] arr = entry.getValue();
            long dur = arr[0];
            int count = (int) arr[1];
            int questions = (int) arr[2];
            long avgPace = questions > 0 ? (dur / questions) : 0L;
            double pct = overallPracticeMs > 0 ? Math.round(((double) dur / overallPracticeMs) * 1000.0) / 10.0 : 0.0;
            String color = colorMap.getOrDefault(subj.toLowerCase(Locale.ROOT), "#8E8E93");

            list.add(new SubjectPracticeStats(subj, color, dur, count, questions, avgPace, pct));
        }

        list.sort((a, b) -> Long.compare(b.getPracticeTimeMs(), a.getPracticeTimeMs()));
        return list;
    }

    public synchronized AdaptiveTargetSuggestion getAdaptiveTargetSuggestion() {
        long now = System.currentTimeMillis();
        TimeRangeFilter filter = TimeRangeFilter.RANGE_7_DAYS;
        List<DailyPracticeDataPoint> daily = getDailyPracticeTimeData(filter);

        List<Double> minutesList = new ArrayList<>();
        double totalMin = 0.0;
        for (DailyPracticeDataPoint d : daily) {
            minutesList.add(d.getPracticeMinutes());
            totalMin += d.getPracticeMinutes();
        }

        while (minutesList.size() < 7) {
            minutesList.add(0.0);
        }
        Collections.sort(minutesList);

        double medianMin = minutesList.get(minutesList.size() / 2);
        double meanMin = totalMin / 7.0;
        double baseline = (medianMin > 0.0) ? medianMin : (meanMin > 0.0 ? meanMin : 45.0);

        int suggestedMinutes = (int) Math.round(baseline * 1.10);
        suggestedMinutes = Math.max(30, Math.min(360, suggestedMinutes));

        int targetPaceSec = getTargetTimePerQuestionSec();
        if (targetPaceSec <= 0) targetPaceSec = 120;
        int suggestedQuestions = Math.max(10, (suggestedMinutes * 60) / targetPaceSec);

        String reasoning;
        if (medianMin > 0.0) {
            reasoning = String.format(Locale.US,
                    "Based on your 7-day median daily practice of %.1f mins, applying a gentle +10%% progression goal.", medianMin);
        } else if (meanMin > 0.0) {
            reasoning = String.format(Locale.US,
                    "Based on your recent 7-day daily average of %.1f mins, advancing targets by +10%%.", meanMin);
        } else {
            reasoning = "Welcome to Question Solving Timer! Starting with a balanced 45-60 min daily target.";
        }

        return new AdaptiveTargetSuggestion(
                Math.round(meanMin * 10.0) / 10.0,
                suggestedMinutes,
                suggestedQuestions,
                10.0,
                reasoning
        );
    }

    // =========================================================================
    // Subject, Countdown & Achievement Operations
    // =========================================================================

    public synchronized List<Subject> getAllSubjects() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Subject> list = new ArrayList<>();
        String query = "SELECT " +
                DatabaseContract.SubjectEntry.COLUMN_ALT_ID + ", " +
                DatabaseContract.SubjectEntry.COLUMN_NAME + ", " +
                DatabaseContract.SubjectEntry.COLUMN_COLOR_HEX + ", " +
                DatabaseContract.SubjectEntry.COLUMN_CREATED_AT +
                " FROM " + DatabaseContract.SubjectEntry.TABLE_NAME +
                " ORDER BY " + DatabaseContract.SubjectEntry.COLUMN_NAME + " ASC";

        try (Cursor cursor = db.rawQuery(query, null)) {
            while (cursor.moveToNext()) {
                list.add(new Subject(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getLong(3)
                ));
            }
        }
        return list;
    }

    public synchronized long createSubject(String name, String colorHex) {
        if (name == null || name.trim().isEmpty()) return -1L;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseContract.SubjectEntry.COLUMN_NAME, name.trim());
        cv.put(DatabaseContract.SubjectEntry.COLUMN_COLOR_HEX, (colorHex != null && !colorHex.trim().isEmpty()) ? colorHex.trim() : "#8E8E93");
        cv.put(DatabaseContract.SubjectEntry.COLUMN_CREATED_AT, System.currentTimeMillis());

        return db.insertWithOnConflict(DatabaseContract.SubjectEntry.TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public synchronized Subject getSubjectByName(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT " +
                DatabaseContract.SubjectEntry.COLUMN_ALT_ID + ", " +
                DatabaseContract.SubjectEntry.COLUMN_NAME + ", " +
                DatabaseContract.SubjectEntry.COLUMN_COLOR_HEX + ", " +
                DatabaseContract.SubjectEntry.COLUMN_CREATED_AT +
                " FROM " + DatabaseContract.SubjectEntry.TABLE_NAME +
                " WHERE " + DatabaseContract.SubjectEntry.COLUMN_NAME + " = ? COLLATE NOCASE LIMIT 1";

        try (Cursor cursor = db.rawQuery(query, new String[]{name.trim()})) {
            if (cursor.moveToFirst()) {
                return new Subject(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getLong(3)
                );
            }
        }
        return null;
    }

    public synchronized boolean deleteSubject(long id) {
        if (id <= 0) return false;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DatabaseContract.SubjectEntry.TABLE_NAME,
                DatabaseContract.SubjectEntry.COLUMN_ALT_ID + " = ?",
                new String[]{String.valueOf(id)}) > 0;
    }

    public synchronized List<ExamCountdown> getAllExamCountdowns() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<ExamCountdown> list = new ArrayList<>();
        String query = "SELECT " +
                DatabaseContract.ExamCountdownEntry.COLUMN_ALT_ID + ", " +
                DatabaseContract.ExamCountdownEntry.COLUMN_TITLE + ", " +
                DatabaseContract.ExamCountdownEntry.COLUMN_TARGET_DATE_MILLIS + ", " +
                DatabaseContract.ExamCountdownEntry.COLUMN_DESCRIPTION +
                " FROM " + DatabaseContract.ExamCountdownEntry.TABLE_NAME +
                " ORDER BY " + DatabaseContract.ExamCountdownEntry.COLUMN_TARGET_DATE_MILLIS + " ASC";

        try (Cursor cursor = db.rawQuery(query, null)) {
            while (cursor.moveToNext()) {
                list.add(new ExamCountdown(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getLong(2),
                        cursor.getString(3)
                ));
            }
        }
        return list;
    }

    public synchronized long createExamCountdown(String title, long targetDateMillis, String description) {
        if (title == null || title.trim().isEmpty()) return -1L;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseContract.ExamCountdownEntry.COLUMN_TITLE, title.trim());
        cv.put(DatabaseContract.ExamCountdownEntry.COLUMN_TARGET_DATE_MILLIS, targetDateMillis);
        cv.put(DatabaseContract.ExamCountdownEntry.COLUMN_DESCRIPTION, description != null ? description : "");

        return db.insertOrThrow(DatabaseContract.ExamCountdownEntry.TABLE_NAME, null, cv);
    }

    public synchronized boolean deleteExamCountdown(long id) {
        if (id <= 0) return false;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DatabaseContract.ExamCountdownEntry.TABLE_NAME,
                DatabaseContract.ExamCountdownEntry.COLUMN_ALT_ID + " = ?",
                new String[]{String.valueOf(id)}) > 0;
    }

    public synchronized List<Achievement> getAllAchievements() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Achievement> list = new ArrayList<>();
        String query = "SELECT " +
                DatabaseContract.AchievementEntry.COLUMN_ALT_ID + ", " +
                DatabaseContract.AchievementEntry.COLUMN_KEY + ", " +
                DatabaseContract.AchievementEntry.COLUMN_TITLE + ", " +
                DatabaseContract.AchievementEntry.COLUMN_UNLOCKED_AT +
                " FROM " + DatabaseContract.AchievementEntry.TABLE_NAME +
                " ORDER BY " + DatabaseContract.AchievementEntry.COLUMN_UNLOCKED_AT + " DESC";

        try (Cursor cursor = db.rawQuery(query, null)) {
            while (cursor.moveToNext()) {
                list.add(new Achievement(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getLong(3)
                ));
            }
        }
        return list;
    }

    public synchronized boolean unlockAchievement(String key, String title) {
        if (key == null || key.trim().isEmpty()) return false;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseContract.AchievementEntry.COLUMN_KEY, key.trim());
        cv.put(DatabaseContract.AchievementEntry.COLUMN_TITLE, title != null ? title.trim() : key.trim());
        cv.put(DatabaseContract.AchievementEntry.COLUMN_UNLOCKED_AT, System.currentTimeMillis());

        long row = db.insertWithOnConflict(DatabaseContract.AchievementEntry.TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        return row != -1L;
    }

    public synchronized boolean isAchievementUnlocked(String key) {
        if (key == null || key.trim().isEmpty()) return false;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT 1 FROM " + DatabaseContract.AchievementEntry.TABLE_NAME +
                " WHERE " + DatabaseContract.AchievementEntry.COLUMN_KEY + " = ? LIMIT 1";
        try (Cursor cursor = db.rawQuery(query, new String[]{key.trim()})) {
            return cursor.moveToFirst();
        }
    }

    // =========================================================================
    // Chart Data Series Operations
    // =========================================================================

    public synchronized List<SpeedDataPoint> getSpeedOverTimeData(TimeRangeFilter filter) {
        long now = System.currentTimeMillis();
        long start = (filter != null) ? filter.getStartTimestamp(now) : 0L;
        long end = (filter != null) ? filter.getEndTimestamp(now) : Long.MAX_VALUE;
        return getSpeedOverTimeData(start, end);
    }

    public synchronized List<SpeedDataPoint> getSpeedOverTimeData(long startTimestamp, long endTimestamp) {
        List<SessionStats> sessions = getSessionsWithStatsInRange(startTimestamp, endTimestamp);
        List<SpeedDataPoint> points = new ArrayList<>();
        SimpleDateFormat labelFormat = new SimpleDateFormat("MMM d", Locale.getDefault());

        int index = 1;
        for (SessionStats s : sessions) {
            if (s.getSolvedCount() > 0) {
                long ts = s.getCreatedAt() > 0 ? s.getCreatedAt() : s.getUpdatedAt();
                String label = labelFormat.format(new Date(ts));
                double avgSec = s.getAverageDurationMs() / 1000.0;

                points.add(new SpeedDataPoint(
                        s.getSessionId(),
                        index++,
                        s.getSessionName(),
                        ts,
                        label,
                        avgSec,
                        s.getSolvedCount()
                ));
            }
        }
        return points;
    }

    public synchronized List<DailyPracticeDataPoint> getDailyPracticeTimeData(TimeRangeFilter filter) {
        long now = System.currentTimeMillis();
        long start = (filter != null) ? filter.getStartTimestamp(now) : 0L;
        long end = (filter != null) ? filter.getEndTimestamp(now) : Long.MAX_VALUE;
        return getDailyPracticeTimeData(start, end);
    }

    public synchronized List<DailyPracticeDataPoint> getDailyPracticeTimeData(long startTimestamp, long endTimestamp) {
        List<SessionStats> sessions = getSessionsWithStatsInRange(startTimestamp, endTimestamp);
        Map<Long, DailyPracticeDataPoint> dayMap = new TreeMap<>();
        SimpleDateFormat labelFormat = new SimpleDateFormat("MMM d", Locale.getDefault());

        for (SessionStats s : sessions) {
            long ts = s.getCreatedAt() > 0 ? s.getCreatedAt() : s.getUpdatedAt();
            if (ts <= 0) ts = System.currentTimeMillis();

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(ts);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long midnight = cal.getTimeInMillis();

            DailyPracticeDataPoint point = dayMap.get(midnight);
            if (point == null) {
                point = new DailyPracticeDataPoint(
                        midnight,
                        labelFormat.format(new Date(midnight)),
                        0.0,
                        0L,
                        0,
                        0
                );
                dayMap.put(midnight, point);
            }

            point.setPracticeDurationMs(point.getPracticeDurationMs() + s.getTotalDurationMs());
            point.setPracticeMinutes(point.getPracticeDurationMs() / 60000.0);
            point.setSessionCount(point.getSessionCount() + 1);
            point.setTotalQuestionsSolved(point.getTotalQuestionsSolved() + s.getSolvedCount());
        }

        return new ArrayList<>(dayMap.values());
    }

    // =========================================================================
    // Target Settings & Compliance Operations
    // =========================================================================

    public synchronized TargetSettings getTargetSettings() {
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            int timePerQuestion = prefs.getInt(KEY_TARGET_TIME_PER_QUESTION_SEC, TargetSettings.DEFAULT_TIME_PER_QUESTION_SEC);
            int sessionDuration = prefs.getInt(KEY_TARGET_SESSION_DURATION_MIN, TargetSettings.DEFAULT_SESSION_DURATION_MIN);
            int sessionQuestions = prefs.getInt(KEY_TARGET_SESSION_QUESTIONS, TargetSettings.DEFAULT_SESSION_QUESTIONS);
            return new TargetSettings(timePerQuestion, sessionDuration, sessionQuestions,
                    getTargetDate(), isTwoZoneEnabled(), getWallpaperPreferences(), getWidgetSettings());
        }
        return new TargetSettings(
                inMemoryTargetSettings.getTargetTimePerQuestionSec(),
                inMemoryTargetSettings.getTargetSessionDurationMin(),
                inMemoryTargetSettings.getTargetSessionQuestions(),
                getTargetDate(),
                isTwoZoneEnabled(),
                getWallpaperPreferences(),
                getWidgetSettings()
        );
    }

    public synchronized void saveTargetSettings(TargetSettings settings) {
        if (settings == null) return;
        this.inMemoryTargetSettings = new TargetSettings(
                settings.getTargetTimePerQuestionSec(),
                settings.getTargetSessionDurationMin(),
                settings.getTargetSessionQuestions(),
                settings.getTargetDate(),
                settings.isTwoZoneEnabled(),
                settings.getWallpaperPreferences(),
                settings.getWidgetSettings()
        );

        setTargetDate(settings.getTargetDate());
        setTwoZoneEnabled(settings.isTwoZoneEnabled());
        saveWallpaperPreferences(settings.getWallpaperPreferences());
        saveWidgetSettings(settings.getWidgetSettings());

        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .putInt(KEY_TARGET_TIME_PER_QUESTION_SEC, settings.getTargetTimePerQuestionSec())
                    .putInt(KEY_TARGET_SESSION_DURATION_MIN, settings.getTargetSessionDurationMin())
                    .putInt(KEY_TARGET_SESSION_QUESTIONS, settings.getTargetSessionQuestions())
                    .putString("target_date", settings.getTargetDate())
                    .putBoolean("two_zone_enabled", settings.isTwoZoneEnabled())
                    .apply();
        }
    }

    public synchronized int getTargetTimePerQuestionSec() {
        return getTargetSettings().getTargetTimePerQuestionSec();
    }

    public synchronized void setTargetTimePerQuestionSec(int sec) {
        TargetSettings current = getTargetSettings();
        current.setTargetTimePerQuestionSec(sec);
        saveTargetSettings(current);
    }

    public synchronized int getTargetSessionDurationMin() {
        return getTargetSettings().getTargetSessionDurationMin();
    }

    public synchronized void setTargetSessionDurationMin(int min) {
        TargetSettings current = getTargetSettings();
        current.setTargetSessionDurationMin(min);
        saveTargetSettings(current);
    }

    public synchronized int getTargetSessionQuestions() {
        return getTargetSettings().getTargetSessionQuestions();
    }

    public synchronized void setTargetSessionQuestions(int count) {
        TargetSettings current = getTargetSettings();
        current.setTargetSessionQuestions(count);
        saveTargetSettings(current);
    }

    // =========================================================================
    // Version 4 Settings & Preferences Storage
    // =========================================================================

    public synchronized String getSetting(String key, String defaultValue) {
        if (key == null) return defaultValue;
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String query = "SELECT " + DatabaseContract.SettingEntry.COLUMN_VALUE +
                    " FROM " + DatabaseContract.SettingEntry.TABLE_NAME +
                    " WHERE " + DatabaseContract.SettingEntry.COLUMN_KEY + " = ? LIMIT 1";
            try (Cursor c = db.rawQuery(query, new String[]{key})) {
                if (c != null && c.moveToFirst()) {
                    return c.getString(0);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error querying setting key: " + key, e);
        }
        return defaultValue;
    }

    public synchronized boolean saveSetting(String key, String value) {
        if (key == null || value == null) return false;
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(DatabaseContract.SettingEntry.COLUMN_KEY, key);
            cv.put(DatabaseContract.SettingEntry.COLUMN_VALUE, value);
            db.insertWithOnConflict(DatabaseContract.SettingEntry.TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error saving setting key: " + key, e);
            return false;
        }
    }

    public synchronized Map<String, String> getAllSettings() {
        Map<String, String> map = new HashMap<>();
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String query = "SELECT " + DatabaseContract.SettingEntry.COLUMN_KEY + ", " +
                    DatabaseContract.SettingEntry.COLUMN_VALUE +
                    " FROM " + DatabaseContract.SettingEntry.TABLE_NAME;
            try (Cursor c = db.rawQuery(query, null)) {
                while (c != null && c.moveToNext()) {
                    map.put(c.getString(0), c.getString(1));
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error querying all settings", e);
        }
        return map;
    }

    public synchronized String getTargetDate() {
        String dbVal = getSetting("target_date", null);
        if (dbVal != null && !dbVal.trim().isEmpty()) {
            return dbVal.trim();
        }
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            return prefs.getString("target_date", TargetSettings.DEFAULT_TARGET_DATE);
        }
        return inMemoryTargetSettings.getTargetDate();
    }

    public synchronized void setTargetDate(String date) {
        String val = (date != null && !date.trim().isEmpty()) ? date.trim() : TargetSettings.DEFAULT_TARGET_DATE;
        saveSetting("target_date", val);
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString("target_date", val).apply();
        }
        inMemoryTargetSettings.setTargetDate(val);
    }

    public synchronized boolean isTwoZoneEnabled() {
        String dbVal = getSetting("two_zone_enabled", null);
        if (dbVal != null) {
            return Boolean.parseBoolean(dbVal);
        }
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            return prefs.getBoolean("two_zone_enabled", false);
        }
        return inMemoryTargetSettings.isTwoZoneEnabled();
    }

    public synchronized void setTwoZoneEnabled(boolean enabled) {
        saveSetting("two_zone_enabled", String.valueOf(enabled));
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putBoolean("two_zone_enabled", enabled).apply();
        }
        inMemoryTargetSettings.setTwoZoneEnabled(enabled);
    }

    public synchronized WallpaperPreferences getWallpaperPreferences() {
        String raw = getSetting("wallpaper_preferences", null);
        if (raw != null && !raw.trim().isEmpty()) {
            return WallpaperPreferences.fromJsonString(raw);
        }
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String prefRaw = prefs.getString("wallpaper_preferences", null);
            if (prefRaw != null && !prefRaw.trim().isEmpty()) {
                return WallpaperPreferences.fromJsonString(prefRaw);
            }
        }
        return inMemoryTargetSettings.getWallpaperPreferences();
    }

    public synchronized void saveWallpaperPreferences(WallpaperPreferences wallpaperPrefs) {
        if (wallpaperPrefs == null) wallpaperPrefs = new WallpaperPreferences();
        String jsonStr = wallpaperPrefs.toJson().toString();
        saveSetting("wallpaper_preferences", jsonStr);
        saveSetting("wallpaper_enabled", String.valueOf(wallpaperPrefs.isEnabled()));
        saveSetting("wallpaper_uri", wallpaperPrefs.getUri());
        saveSetting("wallpaper_dim_alpha", String.valueOf(wallpaperPrefs.getDimAlpha()));
        saveSetting("wallpaper_blur_radius", String.valueOf(wallpaperPrefs.getBlurRadius()));
        saveSetting("wallpaper_style", wallpaperPrefs.getStyle());

        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString("wallpaper_preferences", jsonStr).apply();
        }
        inMemoryTargetSettings.setWallpaperPreferences(wallpaperPrefs);
    }

    public synchronized WidgetSettings getWidgetSettings() {
        String raw = getSetting("widget_settings", null);
        if (raw != null && !raw.trim().isEmpty()) {
            return WidgetSettings.fromJsonString(raw);
        }
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String prefRaw = prefs.getString("widget_settings", null);
            if (prefRaw != null && !prefRaw.trim().isEmpty()) {
                return WidgetSettings.fromJsonString(prefRaw);
            }
        }
        return inMemoryTargetSettings.getWidgetSettings();
    }

    public synchronized void saveWidgetSettings(WidgetSettings widgetSettings) {
        if (widgetSettings == null) widgetSettings = new WidgetSettings();
        String jsonStr = widgetSettings.toJson().toString();
        saveSetting("widget_settings", jsonStr);
        saveSetting("widget_theme", widgetSettings.getTheme());
        saveSetting("widget_show_countdown", String.valueOf(widgetSettings.isShowCountdown()));
        saveSetting("widget_show_daily_goal", String.valueOf(widgetSettings.isShowDailyGoal()));
        saveSetting("widget_opacity", String.valueOf(widgetSettings.getOpacity()));
        saveSetting("widget_refresh_interval_minutes", String.valueOf(widgetSettings.getRefreshIntervalMinutes()));

        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString("widget_settings", jsonStr).apply();
        }
        inMemoryTargetSettings.setWidgetSettings(widgetSettings);
    }

    // =========================================================================
    // Version 4 Attempt Audit History Operations
    // =========================================================================

    public synchronized boolean logAttemptCorrection(long questionLogId, String newOutcome, String notes) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String currentOutcome = "SOLVED";
        long sessionId = 0;
        String query = "SELECT " + DatabaseContract.QuestionLogEntry.COLUMN_SESSION_ID + ", " +
                DatabaseContract.QuestionLogEntry.COLUMN_OUTCOME +
                " FROM " + DatabaseContract.QuestionLogEntry.TABLE_NAME +
                " WHERE " + DatabaseContract.QuestionLogEntry.COLUMN_ALT_ID + " = ? LIMIT 1";
        try (Cursor c = db.rawQuery(query, new String[]{String.valueOf(questionLogId)})) {
            if (c != null && c.moveToFirst()) {
                sessionId = c.getLong(0);
                currentOutcome = c.getString(1);
            } else {
                Log.w(TAG, "Question log not found for correction id=" + questionLogId);
                return false;
            }
        }

        return logAttemptCorrection(questionLogId, sessionId, currentOutcome, newOutcome, System.currentTimeMillis(), notes);
    }

    public synchronized boolean logAttemptCorrection(long questionLogId, long sessionId,
                                                     String originalOutcome, String correctedOutcome,
                                                     long timestamp, String notes) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            // 1. Update question_logs record
            ContentValues qCv = new ContentValues();
            qCv.put(DatabaseContract.QuestionLogEntry.COLUMN_OUTCOME, correctedOutcome);
            qCv.put(DatabaseContract.QuestionLogEntry.COLUMN_ORIGINAL_OUTCOME, originalOutcome);
            qCv.put(DatabaseContract.QuestionLogEntry.COLUMN_CORRECTION_TIMESTAMP, timestamp);
            db.update(
                    DatabaseContract.QuestionLogEntry.TABLE_NAME,
                    qCv,
                    DatabaseContract.QuestionLogEntry.COLUMN_ALT_ID + " = ?",
                    new String[]{String.valueOf(questionLogId)}
            );

            // 2. Insert immutable audit history log
            ContentValues aCv = new ContentValues();
            aCv.put(DatabaseContract.AttemptAuditEntry.COLUMN_QUESTION_LOG_ID, questionLogId);
            aCv.put(DatabaseContract.AttemptAuditEntry.COLUMN_SESSION_ID, sessionId);
            aCv.put(DatabaseContract.AttemptAuditEntry.COLUMN_ORIGINAL_OUTCOME, originalOutcome != null ? originalOutcome : "");
            aCv.put(DatabaseContract.AttemptAuditEntry.COLUMN_CORRECTED_OUTCOME, correctedOutcome != null ? correctedOutcome : "");
            aCv.put(DatabaseContract.AttemptAuditEntry.COLUMN_CORRECTION_TIMESTAMP, timestamp);
            aCv.put(DatabaseContract.AttemptAuditEntry.COLUMN_NOTES, notes != null ? notes : "");
            db.insertOrThrow(DatabaseContract.AttemptAuditEntry.TABLE_NAME, null, aCv);

            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error recording attempt audit correction", e);
            return false;
        } finally {
            db.endTransaction();
        }
    }

    public synchronized List<AttemptAuditRecord> getAuditHistoryForQuestion(long questionLogId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<AttemptAuditRecord> records = new ArrayList<>();
        String query = "SELECT * FROM " + DatabaseContract.AttemptAuditEntry.TABLE_NAME +
                " WHERE " + DatabaseContract.AttemptAuditEntry.COLUMN_QUESTION_LOG_ID + " = ?" +
                " ORDER BY " + DatabaseContract.AttemptAuditEntry.COLUMN_CORRECTION_TIMESTAMP + " DESC";
        try (Cursor c = db.rawQuery(query, new String[]{String.valueOf(questionLogId)})) {
            while (c != null && c.moveToNext()) {
                records.add(parseAuditRecordCursor(c));
            }
        }
        return records;
    }

    public synchronized List<AttemptAuditRecord> getAuditHistoryForSession(long sessionId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<AttemptAuditRecord> records = new ArrayList<>();
        String query = "SELECT * FROM " + DatabaseContract.AttemptAuditEntry.TABLE_NAME +
                " WHERE " + DatabaseContract.AttemptAuditEntry.COLUMN_SESSION_ID + " = ?" +
                " ORDER BY " + DatabaseContract.AttemptAuditEntry.COLUMN_CORRECTION_TIMESTAMP + " DESC";
        try (Cursor c = db.rawQuery(query, new String[]{String.valueOf(sessionId)})) {
            while (c != null && c.moveToNext()) {
                records.add(parseAuditRecordCursor(c));
            }
        }
        return records;
    }

    public synchronized List<AttemptAuditRecord> getAllAuditHistory() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<AttemptAuditRecord> records = new ArrayList<>();
        String query = "SELECT * FROM " + DatabaseContract.AttemptAuditEntry.TABLE_NAME +
                " ORDER BY " + DatabaseContract.AttemptAuditEntry.COLUMN_CORRECTION_TIMESTAMP + " DESC";
        try (Cursor c = db.rawQuery(query, null)) {
            while (c != null && c.moveToNext()) {
                records.add(parseAuditRecordCursor(c));
            }
        }
        return records;
    }

    private AttemptAuditRecord parseAuditRecordCursor(Cursor c) {
        int idIdx = c.getColumnIndex(DatabaseContract.AttemptAuditEntry.COLUMN_ALT_ID);
        long id = idIdx != -1 ? c.getLong(idIdx) : c.getLong(0);

        int qIdIdx = c.getColumnIndex(DatabaseContract.AttemptAuditEntry.COLUMN_QUESTION_LOG_ID);
        long qLogId = qIdIdx != -1 ? c.getLong(qIdIdx) : 0;

        int sIdIdx = c.getColumnIndex(DatabaseContract.AttemptAuditEntry.COLUMN_SESSION_ID);
        long sId = sIdIdx != -1 ? c.getLong(sIdIdx) : 0;

        int origIdx = c.getColumnIndex(DatabaseContract.AttemptAuditEntry.COLUMN_ORIGINAL_OUTCOME);
        String origOut = origIdx != -1 ? c.getString(origIdx) : "";

        int corrIdx = c.getColumnIndex(DatabaseContract.AttemptAuditEntry.COLUMN_CORRECTED_OUTCOME);
        String corrOut = corrIdx != -1 ? c.getString(corrIdx) : "";

        int timeIdx = c.getColumnIndex(DatabaseContract.AttemptAuditEntry.COLUMN_CORRECTION_TIMESTAMP);
        long time = timeIdx != -1 ? c.getLong(timeIdx) : 0;

        int notesIdx = c.getColumnIndex(DatabaseContract.AttemptAuditEntry.COLUMN_NOTES);
        String notes = (notesIdx != -1 && !c.isNull(notesIdx)) ? c.getString(notesIdx) : "";

        return new AttemptAuditRecord(id, qLogId, sId, origOut, corrOut, time, notes);
    }

    public TargetCompliance computeTargetCompliance(SessionStats session) {
        if (session == null) {
            return new TargetCompliance(getTargetSettings(), 0L, 0, 0L);
        }
        return new TargetCompliance(
                getTargetSettings(),
                session.getTotalDurationMs(),
                session.getSolvedCount(),
                session.getAverageDurationMs()
        );
    }

    public TargetCompliance computeTargetCompliance(long durationMs, int questionsSolved, long avgTimePerQuestionMs) {
        return new TargetCompliance(getTargetSettings(), durationMs, questionsSolved, avgTimePerQuestionMs);
    }

    public TargetCompliance computeTargetCompliance(TargetSettings settings, long durationMs, int questionsSolved, long avgTimePerQuestionMs) {
        return new TargetCompliance(settings, durationMs, questionsSolved, avgTimePerQuestionMs);
    }

    // =========================================================================
    // CSV Export & Import Operations
    // =========================================================================

    public synchronized String exportSessionsToCsv() {
        List<SessionStats> sessions = getAllSessionsWithStats();
        StringBuilder sb = new StringBuilder();
        sb.append("Session ID,Name,Subject,Date,Duration (s),Questions Solved,Avg Pace (s),Status,Notes,What Went Well,Needs Improvement\r\n");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

        for (SessionStats s : sessions) {
            long ts = s.getCreatedAt() > 0 ? s.getCreatedAt() : s.getUpdatedAt();
            String dateStr = sdf.format(new Date(ts));
            long durationSec = s.getTotalDurationMs() / 1000;
            double avgPaceSec = Math.round((s.getAverageDurationMs() / 1000.0) * 10.0) / 10.0;

            sb.append(s.getSessionId()).append(",");
            sb.append(escapeCsvField(s.getSessionName())).append(",");
            sb.append(escapeCsvField(s.getSubject())).append(",");
            sb.append(escapeCsvField(dateStr)).append(",");
            sb.append(durationSec).append(",");
            sb.append(s.getSolvedCount()).append(",");
            sb.append(avgPaceSec).append(",");
            sb.append(escapeCsvField(s.getStatus())).append(",");
            sb.append(escapeCsvField(s.getNotes())).append(",");
            sb.append(escapeCsvField(s.getWentWell())).append(",");
            sb.append(escapeCsvField(s.getNeedsImprovement())).append("\r\n");
        }

        return sb.toString();
    }

    private String escapeCsvField(String value) {
        if (value == null) return "\"\"";
        boolean needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        if (needsQuotes) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    public synchronized int importSessionsFromCsv(String csvContent) {
        if (csvContent == null || csvContent.trim().isEmpty()) {
            return 0;
        }

        List<List<String>> records = parseCsv(csvContent);
        if (records.isEmpty()) return 0;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int importedCount = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

        db.beginTransaction();
        try {
            boolean isFirst = true;
            for (List<String> row : records) {
                if (row.isEmpty()) continue;
                if (isFirst) {
                    isFirst = false;
                    if (row.get(0).toLowerCase(Locale.ROOT).contains("session") ||
                        row.get(0).toLowerCase(Locale.ROOT).contains("id")) {
                        continue;
                    }
                }

                if (row.size() < 2) continue;

                String name = row.get(1);
                String subject = row.size() > 2 ? row.get(2) : "General";
                String dateStr = row.size() > 3 ? row.get(3) : "";
                long durationSec = (row.size() > 4) ? parseLongSafe(row.get(4), 0L) : 0L;
                int questions = (row.size() > 5) ? parseIntSafe(row.get(5), 0) : 0;
                String status = row.size() > 7 ? row.get(7) : "COMPLETED";
                String notes = row.size() > 8 ? row.get(8) : "";
                String wentWell = row.size() > 9 ? row.get(9) : "";
                String needsImprovement = row.size() > 10 ? row.get(10) : "";

                long createdAt = System.currentTimeMillis();
                if (!dateStr.isEmpty()) {
                    try {
                        Date d = sdf.parse(dateStr);
                        if (d != null) createdAt = d.getTime();
                    } catch (Exception ignored) {
                        try {
                            createdAt = Long.parseLong(dateStr);
                        } catch (Exception ignored2) {}
                    }
                }

                if (isSessionDuplicate(db, name, createdAt)) {
                    continue;
                }

                ContentValues cv = new ContentValues();
                cv.put(DatabaseContract.SessionEntry.COLUMN_NAME, name);
                cv.put(DatabaseContract.SessionEntry.COLUMN_TARGET_COUNT, questions);
                cv.put(DatabaseContract.SessionEntry.COLUMN_CREATED_AT, createdAt);
                cv.put(DatabaseContract.SessionEntry.COLUMN_UPDATED_AT, createdAt + (durationSec * 1000L));
                cv.put(DatabaseContract.SessionEntry.COLUMN_IS_ACTIVE, 0);
                cv.put(DatabaseContract.SessionEntry.COLUMN_SUBJECT, subject);
                cv.put(DatabaseContract.SessionEntry.COLUMN_NOTES, notes);
                cv.put(DatabaseContract.SessionEntry.COLUMN_WENT_WELL, wentWell);
                cv.put(DatabaseContract.SessionEntry.COLUMN_NEEDS_IMPROVEMENT, needsImprovement);
                cv.put(DatabaseContract.SessionEntry.COLUMN_STATUS, status);
                cv.put(DatabaseContract.SessionEntry.COLUMN_PROFILE_ID, "default");

                long newId = db.insertOrThrow(DatabaseContract.SessionEntry.TABLE_NAME, null, cv);

                if (questions > 0 && durationSec > 0) {
                    long perQuestionMs = (durationSec * 1000L) / questions;
                    for (int q = 1; q <= questions; q++) {
                        ContentValues logCv = new ContentValues();
                        logCv.put(DatabaseContract.QuestionLogEntry.COLUMN_SESSION_ID, newId);
                        logCv.put(DatabaseContract.QuestionLogEntry.COLUMN_QUESTION_NUM, q);
                        logCv.put(DatabaseContract.QuestionLogEntry.COLUMN_DURATION_MS, perQuestionMs);
                        logCv.put(DatabaseContract.QuestionLogEntry.COLUMN_COMPLETED_AT, createdAt + (q * perQuestionMs));
                        db.insertOrThrow(DatabaseContract.QuestionLogEntry.TABLE_NAME, null, logCv);
                    }
                }

                importedCount++;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        return importedCount;
    }

    private boolean isSessionDuplicate(SQLiteDatabase db, String name, long createdAt) {
        String query = "SELECT 1 FROM " + DatabaseContract.SessionEntry.TABLE_NAME +
                " WHERE " + DatabaseContract.SessionEntry.COLUMN_NAME + " = ? AND abs(" +
                DatabaseContract.SessionEntry.COLUMN_CREATED_AT + " - ?) < 60000 LIMIT 1";
        try (Cursor c = db.rawQuery(query, new String[]{name, String.valueOf(createdAt)})) {
            return c.moveToFirst();
        }
    }

    private List<List<String>> parseCsv(String csvContent) {
        List<List<String>> records = new ArrayList<>();
        List<String> currentRecord = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        int len = csvContent.length();

        for (int i = 0; i < len; i++) {
            char c = csvContent.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < len && csvContent.charAt(i + 1) == '"') {
                        currentField.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    currentField.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    currentRecord.add(currentField.toString().trim());
                    currentField.setLength(0);
                } else if (c == '\r') {
                    if (i + 1 < len && csvContent.charAt(i + 1) == '\n') {
                        i++;
                    }
                    currentRecord.add(currentField.toString().trim());
                    currentField.setLength(0);
                    records.add(currentRecord);
                    currentRecord = new ArrayList<>();
                } else if (c == '\n') {
                    currentRecord.add(currentField.toString().trim());
                    currentField.setLength(0);
                    records.add(currentRecord);
                    currentRecord = new ArrayList<>();
                } else {
                    currentField.append(c);
                }
            }
        }

        if (currentField.length() > 0 || !currentRecord.isEmpty()) {
            currentRecord.add(currentField.toString().trim());
            records.add(currentRecord);
        }

        return records;
    }

    private long parseLongSafe(String s, long def) {
        try {
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private int parseIntSafe(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    // =========================================================================
    // Auto Session Recovery & Integrity Diagnostics
    // =========================================================================

    public synchronized void saveInterruptedSessionState(TimerSession session) {
        if (session == null || context == null) return;
        try {
            JSONObject obj = new JSONObject();
            obj.put("id", session.getId());
            obj.put("name", session.getName());
            obj.put("target", session.getTarget());
            obj.put("currentQuestion", session.getCurrentQuestion());
            obj.put("status", "INTERRUPTED");
            obj.put("createdAt", session.getCreatedAt());
            obj.put("updatedAt", System.currentTimeMillis());
            obj.put("subject", session.getSubject());
            obj.put("notes", session.getNotes());
            obj.put("whatWentWell", session.getWhatWentWell());
            obj.put("needsImprovement", session.getNeedsImprovement());
            obj.put("profileId", session.getProfileId());
            obj.put("examProfile", session.getExamProfile());
            obj.put("totalStudyDurationMs", session.getTotalStudyDurationMs());
            obj.put("totalBreakDurationMs", session.getTotalBreakDurationMs());
            obj.put("isMockTest", session.isMockTest());
            obj.put("currentBreakStartTime", session.getCurrentBreakStartTime());
            obj.put("activeQuestionElapsedMs", session.getActiveQuestionElapsedMs());

            JSONArray arr = new JSONArray();
            for (com.example.timerapp.model.QuestionLog log : session.getLogs()) {
                JSONObject lObj = new JSONObject();
                lObj.put("id", log.getId());
                lObj.put("sessionId", session.getId());
                lObj.put("questionNumber", log.getQuestionNumber());
                lObj.put("durationMs", log.getDurationMs());
                lObj.put("timestamp", log.getTimestamp());
                lObj.put("label", log.getLabel());
                lObj.put("topic", log.getTopic());
                lObj.put("difficulty", log.getDifficulty());
                lObj.put("outcome", log.getOutcome());
                lObj.put("confidence", log.getConfidence());
                lObj.put("mistakeTag", log.getMistakeTag());
                lObj.put("notes", log.getNotes());
                arr.put(lObj);
            }
            obj.put("logs", arr);

            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_INTERRUPTED_SESSION, obj.toString()).apply();
            Log.d(TAG, "Saved interrupted session state for session id=" + session.getId());
        } catch (Exception e) {
            Log.e(TAG, "Error saving interrupted session state", e);
        }
    }

    public synchronized TimerSession getInterruptedSession() {
        if (context == null) return null;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_INTERRUPTED_SESSION, null);
        if (raw == null || raw.trim().isEmpty()) return null;

        try {
            JSONObject obj = new JSONObject(raw);
            long id = obj.optLong("id", 0L);
            String name = obj.optString("name", "RECOVERED SESSION");
            int target = obj.optInt("target", 0);
            TimerSession session = new TimerSession(id, name, target);
            session.setCurrentQuestion(obj.optInt("currentQuestion", 1));
            session.setStatus(obj.optString("status", "INTERRUPTED"));
            session.setCreatedAt(obj.optLong("createdAt", System.currentTimeMillis()));
            session.setUpdatedAt(obj.optLong("updatedAt", System.currentTimeMillis()));
            session.setSubject(obj.optString("subject", "General"));
            session.setNotes(obj.optString("notes", ""));
            session.setWhatWentWell(obj.optString("whatWentWell", ""));
            session.setNeedsImprovement(obj.optString("needsImprovement", ""));
            session.setProfileId(obj.optString("profileId", "default"));
            session.setExamProfile(obj.optString("examProfile", "PRACTICE"));
            session.setTotalStudyDurationMs(obj.optLong("totalStudyDurationMs", 0L));
            session.setTotalBreakDurationMs(obj.optLong("totalBreakDurationMs", 0L));
            session.setMockTest(obj.optBoolean("isMockTest", false));
            session.setCurrentBreakStartTime(obj.optLong("currentBreakStartTime", 0L));
            session.setActiveQuestionElapsedMs(obj.optLong("activeQuestionElapsedMs", 0L));

            JSONArray arr = obj.optJSONArray("logs");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject lObj = arr.getJSONObject(i);
                    session.addLog(new com.example.timerapp.model.QuestionLog(
                            lObj.optLong("id", System.currentTimeMillis()),
                            id,
                            lObj.optInt("questionNumber", i + 1),
                            lObj.optLong("durationMs", 0L),
                            lObj.optLong("timestamp", System.currentTimeMillis()),
                            lObj.optString("label", ""),
                            lObj.optString("topic", ""),
                            lObj.optString("difficulty", "MEDIUM"),
                            lObj.optString("outcome", "SOLVED"),
                            lObj.optInt("confidence", 3),
                            lObj.optString("mistakeTag", ""),
                            lObj.optString("notes", "")
                    ));
                }
            }
            return session;
        } catch (Exception e) {
            Log.e(TAG, "Error deserializing interrupted session", e);
            return null;
        }
    }

    public synchronized void clearInterruptedSessionState() {
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().remove(KEY_INTERRUPTED_SESSION).apply();
        }
    }

    public synchronized DataIntegrityReport verifyAndRepairDatabaseIntegrity() {
        return verifyAndRepairDataIntegrity();
    }

    public synchronized DataIntegrityReport verifyAndRepairDataIntegrity() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        DataIntegrityReport report = new DataIntegrityReport();

        db.beginTransaction();
        try {
            int orphanedCount = db.delete(
                    DatabaseContract.QuestionLogEntry.TABLE_NAME,
                    DatabaseContract.QuestionLogEntry.COLUMN_SESSION_ID + " NOT IN (SELECT " +
                            DatabaseContract.SessionEntry.COLUMN_ALT_ID + " FROM " +
                            DatabaseContract.SessionEntry.TABLE_NAME + ")",
                    null
            );
            report.setOrphanedLogsRemoved(orphanedCount);
            if (orphanedCount > 0) {
                report.addDiagnosticMessage("Removed " + orphanedCount + " orphaned question logs.");
            }

            ContentValues fixDuration = new ContentValues();
            fixDuration.put(DatabaseContract.QuestionLogEntry.COLUMN_DURATION_MS, 0L);
            int negDurationCount = db.update(
                    DatabaseContract.QuestionLogEntry.TABLE_NAME,
                    fixDuration,
                    DatabaseContract.QuestionLogEntry.COLUMN_DURATION_MS + " < 0",
                    null
            );
            report.setNegativeDurationsFixed(negDurationCount);
            if (negDurationCount > 0) {
                report.addDiagnosticMessage("Fixed " + negDurationCount + " negative duration values to 0.");
            }

            int invalidQNumCount = db.delete(
                    DatabaseContract.QuestionLogEntry.TABLE_NAME,
                    DatabaseContract.QuestionLogEntry.COLUMN_QUESTION_NUM + " <= 0",
                    null
            );
            if (invalidQNumCount > 0) {
                report.addDiagnosticMessage("Removed " + invalidQNumCount + " question logs with invalid question numbers (<= 0).");
            }

            long now = System.currentTimeMillis();
            ContentValues fixTimestamp = new ContentValues();
            fixTimestamp.put(DatabaseContract.SessionEntry.COLUMN_CREATED_AT, now);
            fixTimestamp.put(DatabaseContract.SessionEntry.COLUMN_UPDATED_AT, now);
            int corruptSessions = db.update(
                    DatabaseContract.SessionEntry.TABLE_NAME,
                    fixTimestamp,
                    DatabaseContract.SessionEntry.COLUMN_CREATED_AT + " <= 0 OR " +
                            DatabaseContract.SessionEntry.COLUMN_UPDATED_AT + " <= 0",
                    null
            );
            report.setCorruptSessionsFixed(corruptSessions);
            if (corruptSessions > 0) {
                report.addDiagnosticMessage("Repaired " + corruptSessions + " sessions with corrupted timestamps.");
            }

            ContentValues fixSubject = new ContentValues();
            fixSubject.put(DatabaseContract.SessionEntry.COLUMN_SUBJECT, "General");
            db.update(
                    DatabaseContract.SessionEntry.TABLE_NAME,
                    fixSubject,
                    DatabaseContract.SessionEntry.COLUMN_SUBJECT + " IS NULL OR trim(" +
                            DatabaseContract.SessionEntry.COLUMN_SUBJECT + ") = ''",
                    null
            );

            db.setTransactionSuccessful();
            report.setHealthy(report.getTotalRepairs() == 0);
        } catch (Exception e) {
            Log.e(TAG, "Data integrity verification failed", e);
            report.setHealthy(false);
            report.addDiagnosticMessage("Integrity check encountered exception: " + e.getMessage());
        } finally {
            db.endTransaction();
        }

        return report;
    }

    // =========================================================================
    // Single Session & Question Queries
    // =========================================================================

    public synchronized List<QuestionLog> getQuestionsForSession(long sessionId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<QuestionLog> logs = new ArrayList<>();

        String query = "SELECT * FROM " + DatabaseContract.QuestionLogEntry.TABLE_NAME +
                " WHERE " + DatabaseContract.QuestionLogEntry.COLUMN_SESSION_ID + " = ? " +
                " ORDER BY " + DatabaseContract.QuestionLogEntry.COLUMN_QUESTION_NUM + " ASC";

        try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(sessionId)})) {
            while (cursor.moveToNext()) {
                logs.add(parseQuestionLogCursor(cursor));
            }
        }
        return logs;
    }

    public synchronized List<com.example.timerapp.model.QuestionLog> getUiQuestionLogsForSession(long sessionId) {
        List<QuestionLog> dataLogs = getQuestionsForSession(sessionId);
        List<com.example.timerapp.model.QuestionLog> uiLogs = new ArrayList<>(dataLogs.size());
        for (QuestionLog log : dataLogs) {
            uiLogs.add(log.toUiModel());
        }
        return uiLogs;
    }

    public synchronized Session getSessionById(long sessionId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT * FROM " + DatabaseContract.SessionEntry.TABLE_NAME +
                " WHERE " + DatabaseContract.SessionEntry.COLUMN_ALT_ID + " = ? LIMIT 1";

        try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(sessionId)})) {
            if (cursor.moveToFirst()) {
                return parseSessionCursor(cursor);
            }
        }
        return null;
    }

    public synchronized List<Session> getAllSessions() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Session> sessions = new ArrayList<>();
        String query = "SELECT * FROM " + DatabaseContract.SessionEntry.TABLE_NAME +
                " ORDER BY " + DatabaseContract.SessionEntry.COLUMN_CREATED_AT + " DESC";
        try (Cursor cursor = db.rawQuery(query, null)) {
            while (cursor.moveToNext()) {
                sessions.add(parseSessionCursor(cursor));
            }
        }
        return sessions;
    }

    public synchronized boolean deleteLastQuestionLogForSession(long sessionId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + DatabaseContract.QuestionLogEntry.COLUMN_ALT_ID +
                " FROM " + DatabaseContract.QuestionLogEntry.TABLE_NAME +
                " WHERE " + DatabaseContract.QuestionLogEntry.COLUMN_SESSION_ID + " = ? " +
                " ORDER BY " + DatabaseContract.QuestionLogEntry.COLUMN_QUESTION_NUM + " DESC LIMIT 1",
                new String[]{String.valueOf(sessionId)});
        try {
            if (cursor.moveToFirst()) {
                long logId = cursor.getLong(0);
                int deleted = db.delete(DatabaseContract.QuestionLogEntry.TABLE_NAME,
                        DatabaseContract.QuestionLogEntry.COLUMN_ALT_ID + " = ?", new String[]{String.valueOf(logId)});
                return deleted > 0;
            }
        } finally {
            cursor.close();
        }
        return false;
    }

    public synchronized boolean updateQuestionLogDetails(long logId, String outcome, String difficulty,
                                                         int confidence, String mistakeTag, String notes) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        if (outcome != null) cv.put(DatabaseContract.QuestionLogEntry.COLUMN_OUTCOME, outcome);
        if (difficulty != null) cv.put(DatabaseContract.QuestionLogEntry.COLUMN_DIFFICULTY, difficulty);
        cv.put(DatabaseContract.QuestionLogEntry.COLUMN_CONFIDENCE, confidence);
        if (mistakeTag != null) cv.put(DatabaseContract.QuestionLogEntry.COLUMN_MISTAKE_TAG, mistakeTag);
        if (notes != null) cv.put(DatabaseContract.QuestionLogEntry.COLUMN_NOTES, notes);
        int rows = db.update(DatabaseContract.QuestionLogEntry.TABLE_NAME, cv,
                DatabaseContract.QuestionLogEntry.COLUMN_ALT_ID + " = ?", new String[]{String.valueOf(logId)});
        return rows > 0;
    }

    public synchronized int clearQuestionLogsForSession(long sessionId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.SessionEntry.COLUMN_UPDATED_AT, System.currentTimeMillis());
        db.update(
                DatabaseContract.SessionEntry.TABLE_NAME,
                values,
                DatabaseContract.SessionEntry.COLUMN_ALT_ID + " = ?",
                new String[]{String.valueOf(sessionId)}
        );
        return db.delete(
                DatabaseContract.QuestionLogEntry.TABLE_NAME,
                DatabaseContract.QuestionLogEntry.COLUMN_SESSION_ID + " = ?",
                new String[]{String.valueOf(sessionId)}
        );
    }

    public synchronized boolean deleteSession(long sessionId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete(
                DatabaseContract.SessionEntry.TABLE_NAME,
                DatabaseContract.SessionEntry.COLUMN_ALT_ID + " = ?",
                new String[]{String.valueOf(sessionId)}
        );
        return rows > 0;
    }

    public synchronized void clearAllData() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(DatabaseContract.QuestionLogEntry.TABLE_NAME, null, null);
            db.delete(DatabaseContract.SessionEntry.TABLE_NAME, null, null);
            db.delete(DatabaseContract.SubjectEntry.TABLE_NAME, null, null);
            db.delete(DatabaseContract.ExamCountdownEntry.TABLE_NAME, null, null);
            db.delete(DatabaseContract.AchievementEntry.TABLE_NAME, null, null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // =========================================================================
    // Backup & Restore
    // =========================================================================

    public synchronized String exportBackupToJson() throws JSONException {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return BackupManager.exportBackupToJson(db);
    }

    public synchronized int importBackupFromJson(String jsonString) throws JSONException {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return BackupManager.importBackupFromJson(db, jsonString, true);
    }

    public synchronized int importBackupFromJson(String jsonString, boolean clearExisting) throws JSONException {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return BackupManager.importBackupFromJson(db, jsonString, clearExisting);
    }

    public synchronized void exportBackupToFile(File file) throws IOException, JSONException {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        BackupManager.exportBackupToFile(db, file);
    }

    public synchronized int importBackupFromFile(File file, boolean clearExisting) throws IOException, JSONException {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return BackupManager.importBackupFromFile(db, file, clearExisting);
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private boolean hasAnyActiveSession(SQLiteDatabase db) {
        String query = "SELECT 1 FROM " + DatabaseContract.SessionEntry.TABLE_NAME +
                " WHERE " + DatabaseContract.SessionEntry.COLUMN_IS_ACTIVE + " = 1 LIMIT 1";
        try (Cursor cursor = db.rawQuery(query, null)) {
            return cursor.moveToFirst();
        }
    }

    private Session parseSessionCursor(Cursor cursor) {
        int idIdx = cursor.getColumnIndex(DatabaseContract.SessionEntry.COLUMN_ALT_ID);
        long id = idIdx != -1 ? cursor.getLong(idIdx) : cursor.getLong(0);

        int nameIdx = cursor.getColumnIndex(DatabaseContract.SessionEntry.COLUMN_NAME);
        String name = nameIdx != -1 ? cursor.getString(nameIdx) : cursor.getString(1);

        int targetIdx = cursor.getColumnIndex(DatabaseContract.SessionEntry.COLUMN_TARGET_COUNT);
        int target = targetIdx != -1 ? cursor.getInt(targetIdx) : cursor.getInt(2);

        int createdIdx = cursor.getColumnIndex(DatabaseContract.SessionEntry.COLUMN_CREATED_AT);
        long createdAt = createdIdx != -1 ? cursor.getLong(createdIdx) : cursor.getLong(3);

        int updatedIdx = cursor.getColumnIndex(DatabaseContract.SessionEntry.COLUMN_UPDATED_AT);
        long updatedAt = updatedIdx != -1 ? cursor.getLong(updatedIdx) : cursor.getLong(4);

        int activeIdx = cursor.getColumnIndex(DatabaseContract.SessionEntry.COLUMN_IS_ACTIVE);
        boolean isActive = (activeIdx != -1 ? cursor.getInt(activeIdx) : cursor.getInt(5)) == 1;

        int subjectIdx = cursor.getColumnIndex(DatabaseContract.SessionEntry.COLUMN_SUBJECT);
        String subject = (subjectIdx != -1 && !cursor.isNull(subjectIdx)) ? cursor.getString(subjectIdx) : "General";

        int notesIdx = cursor.getColumnIndex(DatabaseContract.SessionEntry.COLUMN_NOTES);
        String notes = (notesIdx != -1 && !cursor.isNull(notesIdx)) ? cursor.getString(notesIdx) : "";

        int wentWellIdx = cursor.getColumnIndex(DatabaseContract.SessionEntry.COLUMN_WENT_WELL);
        String wentWell = (wentWellIdx != -1 && !cursor.isNull(wentWellIdx)) ? cursor.getString(wentWellIdx) : "";

        int needsImpIdx = cursor.getColumnIndex(DatabaseContract.SessionEntry.COLUMN_NEEDS_IMPROVEMENT);
        String needsImprovement = (needsImpIdx != -1 && !cursor.isNull(needsImpIdx)) ? cursor.getString(needsImpIdx) : "";

        int statusIdx = cursor.getColumnIndex(DatabaseContract.SessionEntry.COLUMN_STATUS);
        String status = (statusIdx != -1 && !cursor.isNull(statusIdx)) ? cursor.getString(statusIdx) : "COMPLETED";

        int profileIdx = cursor.getColumnIndex(DatabaseContract.SessionEntry.COLUMN_PROFILE_ID);
        String profileId = (profileIdx != -1 && !cursor.isNull(profileIdx)) ? cursor.getString(profileIdx) : "default";

        int examProfIdx = cursor.getColumnIndex(DatabaseContract.SessionEntry.COLUMN_EXAM_PROFILE);
        String examProfile = (examProfIdx != -1 && !cursor.isNull(examProfIdx)) ? cursor.getString(examProfIdx) : "PRACTICE";

        int studyDurIdx = cursor.getColumnIndex(DatabaseContract.SessionEntry.COLUMN_TOTAL_STUDY_DURATION_MS);
        long totalStudyMs = (studyDurIdx != -1 && !cursor.isNull(studyDurIdx)) ? cursor.getLong(studyDurIdx) : 0L;

        int breakDurIdx = cursor.getColumnIndex(DatabaseContract.SessionEntry.COLUMN_TOTAL_BREAK_DURATION_MS);
        long totalBreakMs = (breakDurIdx != -1 && !cursor.isNull(breakDurIdx)) ? cursor.getLong(breakDurIdx) : 0L;

        int mockIdx = cursor.getColumnIndex(DatabaseContract.SessionEntry.COLUMN_IS_MOCK_TEST);
        boolean isMock = (mockIdx != -1 && !cursor.isNull(mockIdx)) && cursor.getInt(mockIdx) == 1;

        return new Session(id, name, target, createdAt, updatedAt, isActive,
                subject, notes, wentWell, needsImprovement, status, profileId,
                examProfile, totalStudyMs, totalBreakMs, isMock);
    }

    private QuestionLog parseQuestionLogCursor(Cursor cursor) {
        int idIdx = cursor.getColumnIndex(DatabaseContract.QuestionLogEntry.COLUMN_ALT_ID);
        long id = idIdx != -1 ? cursor.getLong(idIdx) : cursor.getLong(0);

        int sessIdx = cursor.getColumnIndex(DatabaseContract.QuestionLogEntry.COLUMN_SESSION_ID);
        long sessionId = sessIdx != -1 ? cursor.getLong(sessIdx) : cursor.getLong(1);

        int qNumIdx = cursor.getColumnIndex(DatabaseContract.QuestionLogEntry.COLUMN_QUESTION_NUM);
        int questionNum = qNumIdx != -1 ? cursor.getInt(qNumIdx) : cursor.getInt(2);

        int durIdx = cursor.getColumnIndex(DatabaseContract.QuestionLogEntry.COLUMN_DURATION_MS);
        long durationMs = durIdx != -1 ? cursor.getLong(durIdx) : cursor.getLong(3);

        int compIdx = cursor.getColumnIndex(DatabaseContract.QuestionLogEntry.COLUMN_COMPLETED_AT);
        long completedAt = compIdx != -1 ? cursor.getLong(compIdx) : cursor.getLong(4);

        int labelIdx = cursor.getColumnIndex(DatabaseContract.QuestionLogEntry.COLUMN_LABEL);
        String label = (labelIdx != -1 && !cursor.isNull(labelIdx)) ? cursor.getString(labelIdx) : "";

        int topicIdx = cursor.getColumnIndex(DatabaseContract.QuestionLogEntry.COLUMN_TOPIC);
        String topic = (topicIdx != -1 && !cursor.isNull(topicIdx)) ? cursor.getString(topicIdx) : "";

        int diffIdx = cursor.getColumnIndex(DatabaseContract.QuestionLogEntry.COLUMN_DIFFICULTY);
        String difficulty = (diffIdx != -1 && !cursor.isNull(diffIdx)) ? cursor.getString(diffIdx) : "MEDIUM";

        int outIdx = cursor.getColumnIndex(DatabaseContract.QuestionLogEntry.COLUMN_OUTCOME);
        String outcome = (outIdx != -1 && !cursor.isNull(outIdx)) ? cursor.getString(outIdx) : "SOLVED";

        int confIdx = cursor.getColumnIndex(DatabaseContract.QuestionLogEntry.COLUMN_CONFIDENCE);
        int confidence = (confIdx != -1 && !cursor.isNull(confIdx)) ? cursor.getInt(confIdx) : 3;

        int mistakeIdx = cursor.getColumnIndex(DatabaseContract.QuestionLogEntry.COLUMN_MISTAKE_TAG);
        String mistakeTag = (mistakeIdx != -1 && !cursor.isNull(mistakeIdx)) ? cursor.getString(mistakeIdx) : "";

        int notesIdx = cursor.getColumnIndex(DatabaseContract.QuestionLogEntry.COLUMN_NOTES);
        String notes = (notesIdx != -1 && !cursor.isNull(notesIdx)) ? cursor.getString(notesIdx) : "";

        int origOutIdx = cursor.getColumnIndex(DatabaseContract.QuestionLogEntry.COLUMN_ORIGINAL_OUTCOME);
        String originalOutcome = (origOutIdx != -1 && !cursor.isNull(origOutIdx)) ? cursor.getString(origOutIdx) : "";

        int corrTimeIdx = cursor.getColumnIndex(DatabaseContract.QuestionLogEntry.COLUMN_CORRECTION_TIMESTAMP);
        long correctionTimestamp = (corrTimeIdx != -1 && !cursor.isNull(corrTimeIdx)) ? cursor.getLong(corrTimeIdx) : 0L;

        return new QuestionLog(id, sessionId, questionNum, durationMs, completedAt,
                label, topic, difficulty, outcome, confidence, mistakeTag, notes,
                originalOutcome, correctionTimestamp);
    }

    public synchronized List<QuestionLog> getAllQuestions() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<QuestionLog> logs = new ArrayList<>();
        String query = "SELECT * FROM " + DatabaseContract.QuestionLogEntry.TABLE_NAME + " ORDER BY id ASC";
        try (Cursor cursor = db.rawQuery(query, null)) {
            while (cursor.moveToNext()) {
                logs.add(parseQuestionLogCursor(cursor));
            }
        }
        return logs;
    }

    // =========================================================================
    // Dual-Clock & Break Management Operations
    // =========================================================================

    public synchronized long recordBreakStart(long sessionId, long startTime) {
        if (sessionId <= 0) return -1L;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long start = startTime > 0 ? startTime : System.currentTimeMillis();

        ContentValues cv = new ContentValues();
        cv.put(DatabaseContract.BreakEntry.COLUMN_SESSION_ID, sessionId);
        cv.put(DatabaseContract.BreakEntry.COLUMN_START_TIME, start);
        cv.put(DatabaseContract.BreakEntry.COLUMN_END_TIME, 0L);
        cv.put(DatabaseContract.BreakEntry.COLUMN_DURATION_MS, 0L);

        return db.insertOrThrow(DatabaseContract.BreakEntry.TABLE_NAME, null, cv);
    }

    public synchronized long recordBreakEnd(long sessionId, long endTime) {
        if (sessionId <= 0) return 0L;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long end = endTime > 0 ? endTime : System.currentTimeMillis();

        db.beginTransaction();
        try {
            String query = "SELECT " + DatabaseContract.BreakEntry.COLUMN_ALT_ID + ", " +
                    DatabaseContract.BreakEntry.COLUMN_START_TIME +
                    " FROM " + DatabaseContract.BreakEntry.TABLE_NAME +
                    " WHERE " + DatabaseContract.BreakEntry.COLUMN_SESSION_ID + " = ? AND (" +
                    DatabaseContract.BreakEntry.COLUMN_END_TIME + " <= 0 OR " +
                    DatabaseContract.BreakEntry.COLUMN_END_TIME + " < " + DatabaseContract.BreakEntry.COLUMN_START_TIME + ") " +
                    " ORDER BY " + DatabaseContract.BreakEntry.COLUMN_ALT_ID + " DESC LIMIT 1";

            long breakId = -1;
            long startTime = end;

            try (Cursor c = db.rawQuery(query, new String[]{String.valueOf(sessionId)})) {
                if (c.moveToFirst()) {
                    breakId = c.getLong(0);
                    startTime = c.getLong(1);
                }
            }

            long duration = Math.max(0, end - startTime);

            if (breakId > 0) {
                ContentValues cv = new ContentValues();
                cv.put(DatabaseContract.BreakEntry.COLUMN_END_TIME, end);
                cv.put(DatabaseContract.BreakEntry.COLUMN_DURATION_MS, duration);
                db.update(DatabaseContract.BreakEntry.TABLE_NAME, cv,
                        DatabaseContract.BreakEntry.COLUMN_ALT_ID + " = ?",
                        new String[]{String.valueOf(breakId)});

                db.execSQL("UPDATE " + DatabaseContract.SessionEntry.TABLE_NAME +
                                " SET " + DatabaseContract.SessionEntry.COLUMN_TOTAL_BREAK_DURATION_MS + " = " +
                                DatabaseContract.SessionEntry.COLUMN_TOTAL_BREAK_DURATION_MS + " + ?, " +
                                DatabaseContract.SessionEntry.COLUMN_UPDATED_AT + " = ? WHERE " +
                                DatabaseContract.SessionEntry.COLUMN_ALT_ID + " = ?",
                        new Object[]{duration, end, sessionId});
            }

            db.setTransactionSuccessful();
            return duration;
        } finally {
            db.endTransaction();
        }
    }

    public synchronized List<BreakLog> getBreaksForSession(long sessionId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<BreakLog> list = new ArrayList<>();
        String query = "SELECT " +
                DatabaseContract.BreakEntry.COLUMN_ALT_ID + ", " +
                DatabaseContract.BreakEntry.COLUMN_SESSION_ID + ", " +
                DatabaseContract.BreakEntry.COLUMN_START_TIME + ", " +
                DatabaseContract.BreakEntry.COLUMN_END_TIME + ", " +
                DatabaseContract.BreakEntry.COLUMN_DURATION_MS +
                " FROM " + DatabaseContract.BreakEntry.TABLE_NAME +
                " WHERE " + DatabaseContract.BreakEntry.COLUMN_SESSION_ID + " = ? " +
                " ORDER BY " + DatabaseContract.BreakEntry.COLUMN_START_TIME + " ASC";

        try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(sessionId)})) {
            while (cursor.moveToNext()) {
                list.add(new BreakLog(
                        cursor.getLong(0),
                        cursor.getLong(1),
                        cursor.getLong(2),
                        cursor.getLong(3),
                        cursor.getLong(4)
                ));
            }
        }
        return list;
    }

    public synchronized void updateSessionClocks(long sessionId, long totalStudyMs, long totalBreakMs, long activeQuestionElapsedMs) {
        if (sessionId <= 0) return;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseContract.SessionEntry.COLUMN_TOTAL_STUDY_DURATION_MS, Math.max(0, totalStudyMs));
        cv.put(DatabaseContract.SessionEntry.COLUMN_TOTAL_BREAK_DURATION_MS, Math.max(0, totalBreakMs));
        cv.put(DatabaseContract.SessionEntry.COLUMN_UPDATED_AT, System.currentTimeMillis());

        db.update(DatabaseContract.SessionEntry.TABLE_NAME, cv,
                DatabaseContract.SessionEntry.COLUMN_ALT_ID + " = ?",
                new String[]{String.valueOf(sessionId)});
    }

    // =========================================================================
    // Exam Profiles Operations
    // =========================================================================

    public synchronized List<ExamProfile> getAllExamProfiles() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<ExamProfile> list = new ArrayList<>();
        String query = "SELECT " +
                DatabaseContract.ExamProfileEntry.COLUMN_ALT_ID + ", " +
                DatabaseContract.ExamProfileEntry.COLUMN_NAME + ", " +
                DatabaseContract.ExamProfileEntry.COLUMN_TOTAL_DURATION_MINUTES + ", " +
                DatabaseContract.ExamProfileEntry.COLUMN_TOTAL_QUESTIONS + ", " +
                DatabaseContract.ExamProfileEntry.COLUMN_CORRECT_MARKS + ", " +
                DatabaseContract.ExamProfileEntry.COLUMN_NEGATIVE_MARKS + ", " +
                DatabaseContract.ExamProfileEntry.COLUMN_SECTIONS_JSON +
                " FROM " + DatabaseContract.ExamProfileEntry.TABLE_NAME +
                " ORDER BY " + DatabaseContract.ExamProfileEntry.COLUMN_ALT_ID + " ASC";

        try (Cursor cursor = db.rawQuery(query, null)) {
            while (cursor.moveToNext()) {
                list.add(new ExamProfile(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getInt(2),
                        cursor.getInt(3),
                        cursor.getDouble(4),
                        cursor.getDouble(5),
                        cursor.getString(6)
                ));
            }
        }
        return list;
    }

    public synchronized ExamProfile getExamProfileByName(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT " +
                DatabaseContract.ExamProfileEntry.COLUMN_ALT_ID + ", " +
                DatabaseContract.ExamProfileEntry.COLUMN_NAME + ", " +
                DatabaseContract.ExamProfileEntry.COLUMN_TOTAL_DURATION_MINUTES + ", " +
                DatabaseContract.ExamProfileEntry.COLUMN_TOTAL_QUESTIONS + ", " +
                DatabaseContract.ExamProfileEntry.COLUMN_CORRECT_MARKS + ", " +
                DatabaseContract.ExamProfileEntry.COLUMN_NEGATIVE_MARKS + ", " +
                DatabaseContract.ExamProfileEntry.COLUMN_SECTIONS_JSON +
                " FROM " + DatabaseContract.ExamProfileEntry.TABLE_NAME +
                " WHERE " + DatabaseContract.ExamProfileEntry.COLUMN_NAME + " = ? LIMIT 1";

        try (Cursor cursor = db.rawQuery(query, new String[]{name.trim().toUpperCase(Locale.ROOT)})) {
            if (cursor.moveToFirst()) {
                return new ExamProfile(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getInt(2),
                        cursor.getInt(3),
                        cursor.getDouble(4),
                        cursor.getDouble(5),
                        cursor.getString(6)
                );
            }
        }
        return null;
    }

    public synchronized long createOrUpdateExamProfile(ExamProfile profile) {
        if (profile == null || profile.getName() == null || profile.getName().trim().isEmpty()) return -1L;
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(DatabaseContract.ExamProfileEntry.COLUMN_NAME, profile.getName().trim().toUpperCase(Locale.ROOT));
        cv.put(DatabaseContract.ExamProfileEntry.COLUMN_TOTAL_DURATION_MINUTES, profile.getTotalDurationMinutes());
        cv.put(DatabaseContract.ExamProfileEntry.COLUMN_TOTAL_QUESTIONS, profile.getTotalQuestions());
        cv.put(DatabaseContract.ExamProfileEntry.COLUMN_CORRECT_MARKS, profile.getCorrectMarks());
        cv.put(DatabaseContract.ExamProfileEntry.COLUMN_NEGATIVE_MARKS, profile.getNegativeMarks());
        cv.put(DatabaseContract.ExamProfileEntry.COLUMN_SECTIONS_JSON, profile.getSectionsJson());

        return db.insertWithOnConflict(DatabaseContract.ExamProfileEntry.TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public synchronized boolean deleteExamProfile(long id) {
        if (id <= 0) return false;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DatabaseContract.ExamProfileEntry.TABLE_NAME,
                DatabaseContract.ExamProfileEntry.COLUMN_ALT_ID + " = ?",
                new String[]{String.valueOf(id)}) > 0;
    }

    // =========================================================================
    // Study Coach Feedback & Rules Operations
    // =========================================================================

    public synchronized long recordCoachFeedback(String recommendationKey, String feedback, boolean dismissed) {
        if (recommendationKey == null || recommendationKey.trim().isEmpty()) return -1L;
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(DatabaseContract.StudyCoachFeedbackEntry.COLUMN_RECOMMENDATION_KEY, recommendationKey.trim());
        cv.put(DatabaseContract.StudyCoachFeedbackEntry.COLUMN_FEEDBACK, feedback != null ? feedback : "");
        cv.put(DatabaseContract.StudyCoachFeedbackEntry.COLUMN_DISMISSED, dismissed ? 1 : 0);
        cv.put(DatabaseContract.StudyCoachFeedbackEntry.COLUMN_CREATED_AT, System.currentTimeMillis());

        return db.insertOrThrow(DatabaseContract.StudyCoachFeedbackEntry.TABLE_NAME, null, cv);
    }

    public synchronized boolean dismissRecommendation(String recommendationKey) {
        return recordCoachFeedback(recommendationKey, "dismissed", true) > 0;
    }

    public synchronized boolean isRecommendationDismissed(String recommendationKey) {
        if (recommendationKey == null) return false;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT 1 FROM " + DatabaseContract.StudyCoachFeedbackEntry.TABLE_NAME +
                " WHERE " + DatabaseContract.StudyCoachFeedbackEntry.COLUMN_RECOMMENDATION_KEY + " = ? AND " +
                DatabaseContract.StudyCoachFeedbackEntry.COLUMN_DISMISSED + " = 1 LIMIT 1";

        try (Cursor c = db.rawQuery(query, new String[]{recommendationKey.trim()})) {
            return c != null && c.moveToFirst();
        }
    }

    public synchronized List<StudyRecommendation> getStudyRecommendations(int targetPaceSeconds, int targetDailyQuestions) {
        List<SessionStats> sessions = getAllSessionsWithStats();
        List<QuestionLog> allLogs = getAllQuestions();

        List<StudyRecommendation> raw = StudyCoachEngine.evaluateRecommendations(sessions, allLogs, targetPaceSeconds, targetDailyQuestions);
        List<StudyRecommendation> active = new ArrayList<>();

        for (StudyRecommendation rec : raw) {
            if (!isRecommendationDismissed(rec.getKey())) {
                active.add(rec);
            }
        }
        return active;
    }

    public synchronized JeeAnalyticsReport getJeeAnalytics(String profileName, long startMillis, long endMillis) {
        String profileKey = (profileName != null && !profileName.trim().isEmpty())
                ? profileName.trim().toUpperCase(Locale.ROOT)
                : "JEE_MAIN";

        ExamProfile profile = getExamProfileByName(profileKey);
        if (profile == null) {
            profile = new ExamProfile(profileKey, 180, 75, 4.0, 1.0, "[]");
        }

        List<Session> sessions = getAllSessions();
        List<Session> matchingSessions = new ArrayList<>();
        for (Session s : sessions) {
            if (profileKey.equalsIgnoreCase(s.getExamProfile()) || s.isMockTest()) {
                matchingSessions.add(s);
            }
        }

        List<QuestionLog> logs = getAllQuestions();
        return StudyCoachEngine.calculateJeeAnalytics(profileKey, matchingSessions, logs, profile, startMillis, endMillis, 144);
    }

    // =========================================================================
    // SAF Backup & Restore Delegation
    // =========================================================================

    public synchronized void exportSafBackup(OutputStream outputStream, JSONObject preferences) throws IOException, JSONException {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        JSONObject prefs = (preferences != null) ? preferences : new JSONObject();
        if (!prefs.has("target_date")) {
            prefs.put("target_date", getTargetDate());
        }
        if (!prefs.has("two_zone_enabled")) {
            prefs.put("two_zone_enabled", isTwoZoneEnabled());
        }
        if (!prefs.has("wallpaper_settings")) {
            prefs.put("wallpaper_settings", getWallpaperPreferences().toJson());
        }
        if (!prefs.has("widget_settings")) {
            prefs.put("widget_settings", getWidgetSettings().toJson());
        }
        SafBackupEngine.exportBackup(db, outputStream, prefs);
    }

    public synchronized SafBackupEngine.ValidationResult validateSafBackup(InputStream inputStream) {
        return SafBackupEngine.validateBackup(inputStream);
    }

    public synchronized SafBackupEngine.RestoreResult restoreSafBackup(InputStream inputStream, SafBackupEngine.RestoreMode mode) throws IOException, JSONException {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return SafBackupEngine.restoreBackup(db, inputStream, mode);
    }
}

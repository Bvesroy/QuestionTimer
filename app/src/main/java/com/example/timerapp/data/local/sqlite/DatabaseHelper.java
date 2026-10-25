package com.example.timerapp.data.local.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Native Android SQLiteOpenHelper implementation for Question Solving Timer.
 * Manages database lifecycle, table creation, foreign key constraints,
 * schema version upgrades, and performance optimizations such as Write-Ahead Logging (WAL).
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    private static volatile DatabaseHelper sInstance;

    /**
     * Singleton instance provider. Ensures a single connection pool
     * is shared across the entire application process to avoid locking issues.
     *
     * @param context Application or Activity context.
     * @return Shared instance of DatabaseHelper.
     */
    public static DatabaseHelper getInstance(Context context) {
        if (sInstance == null) {
            synchronized (DatabaseHelper.class) {
                if (sInstance == null) {
                    sInstance = new DatabaseHelper(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    /**
     * Constructor is private to enforce Singleton pattern.
     */
    private DatabaseHelper(Context context) {
        super(context, DatabaseContract.DATABASE_NAME, null, DatabaseContract.DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        // Explicitly enforce foreign keys in SQLite (disabled by default in Android)
        db.setForeignKeyConstraintsEnabled(true);
        // Enable Write-Ahead Logging for non-blocking concurrent reads during writes
        db.enableWriteAheadLogging();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "Creating database schema version " + DatabaseContract.DATABASE_VERSION);
        db.beginTransaction();
        try {
            db.execSQL(DatabaseContract.SessionEntry.SQL_CREATE_TABLE);
            db.execSQL(DatabaseContract.SessionEntry.SQL_CREATE_IDX_IS_ACTIVE);
            db.execSQL(DatabaseContract.SessionEntry.SQL_CREATE_IDX_UPDATED_AT);
            db.execSQL(DatabaseContract.SessionEntry.SQL_CREATE_IDX_SUBJECT);
            db.execSQL(DatabaseContract.SessionEntry.SQL_CREATE_IDX_EXAM_PROFILE);

            db.execSQL(DatabaseContract.QuestionLogEntry.SQL_CREATE_TABLE);
            db.execSQL(DatabaseContract.QuestionLogEntry.SQL_CREATE_IDX_SESSION_ID);
            db.execSQL(DatabaseContract.QuestionLogEntry.SQL_CREATE_IDX_SESSION_QUESTION);
            db.execSQL(DatabaseContract.QuestionLogEntry.SQL_CREATE_IDX_TOPIC);
            db.execSQL(DatabaseContract.QuestionLogEntry.SQL_CREATE_IDX_OUTCOME);

            db.execSQL(DatabaseContract.BreakEntry.SQL_CREATE_TABLE);
            db.execSQL(DatabaseContract.BreakEntry.SQL_CREATE_IDX_SESSION_ID);

            db.execSQL(DatabaseContract.StudyCoachFeedbackEntry.SQL_CREATE_TABLE);
            db.execSQL(DatabaseContract.StudyCoachFeedbackEntry.SQL_CREATE_IDX_KEY);

            db.execSQL(DatabaseContract.ExamProfileEntry.SQL_CREATE_TABLE);

            db.execSQL(DatabaseContract.SubjectEntry.SQL_CREATE_TABLE);
            db.execSQL(DatabaseContract.ExamCountdownEntry.SQL_CREATE_TABLE);
            db.execSQL(DatabaseContract.AchievementEntry.SQL_CREATE_TABLE);

            db.execSQL(DatabaseContract.SettingEntry.SQL_CREATE_TABLE);
            db.execSQL(DatabaseContract.SettingEntry.SQL_CREATE_IDX_KEY);

            db.execSQL(DatabaseContract.AttemptAuditEntry.SQL_CREATE_TABLE);
            db.execSQL(DatabaseContract.AttemptAuditEntry.SQL_CREATE_IDX_QUESTION_LOG_ID);
            db.execSQL(DatabaseContract.AttemptAuditEntry.SQL_CREATE_IDX_SESSION_ID);
            db.execSQL(DatabaseContract.AttemptAuditEntry.SQL_CREATE_IDX_TIMESTAMP);

            preseedDefaultSubjects(db);
            preseedDefaultExamProfiles(db);
            preseedDefaultSettings(db);

            db.setTransactionSuccessful();
            Log.i(TAG, "Database schema, tables, indexes, and initial seeds created successfully.");
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        db.beginTransaction();
        try {
            if (oldVersion < 2) {
                // Safely migrate sessions table with v2 columns
                addColumnSafely(db, DatabaseContract.SessionEntry.TABLE_NAME, DatabaseContract.SessionEntry.COLUMN_SUBJECT, "TEXT DEFAULT 'General'");
                addColumnSafely(db, DatabaseContract.SessionEntry.TABLE_NAME, DatabaseContract.SessionEntry.COLUMN_NOTES, "TEXT DEFAULT ''");
                addColumnSafely(db, DatabaseContract.SessionEntry.TABLE_NAME, DatabaseContract.SessionEntry.COLUMN_WENT_WELL, "TEXT DEFAULT ''");
                addColumnSafely(db, DatabaseContract.SessionEntry.TABLE_NAME, DatabaseContract.SessionEntry.COLUMN_NEEDS_IMPROVEMENT, "TEXT DEFAULT ''");
                addColumnSafely(db, DatabaseContract.SessionEntry.TABLE_NAME, DatabaseContract.SessionEntry.COLUMN_STATUS, "TEXT DEFAULT 'COMPLETED'");
                addColumnSafely(db, DatabaseContract.SessionEntry.TABLE_NAME, DatabaseContract.SessionEntry.COLUMN_PROFILE_ID, "TEXT DEFAULT 'default'");

                db.execSQL(DatabaseContract.SessionEntry.SQL_CREATE_IDX_SUBJECT);

                // Create new tables for v2
                db.execSQL(DatabaseContract.SubjectEntry.SQL_CREATE_TABLE);
                db.execSQL(DatabaseContract.ExamCountdownEntry.SQL_CREATE_TABLE);
                db.execSQL(DatabaseContract.AchievementEntry.SQL_CREATE_TABLE);

                preseedDefaultSubjects(db);
                Log.i(TAG, "Upgraded schema to version 2: added subject reflection fields and auxiliary tables.");
            }

            if (oldVersion < 3) {
                // Safely migrate sessions table with v3 columns
                addColumnSafely(db, DatabaseContract.SessionEntry.TABLE_NAME, DatabaseContract.SessionEntry.COLUMN_EXAM_PROFILE, "TEXT DEFAULT 'PRACTICE'");
                addColumnSafely(db, DatabaseContract.SessionEntry.TABLE_NAME, DatabaseContract.SessionEntry.COLUMN_TOTAL_STUDY_DURATION_MS, "INTEGER DEFAULT 0");
                addColumnSafely(db, DatabaseContract.SessionEntry.TABLE_NAME, DatabaseContract.SessionEntry.COLUMN_TOTAL_BREAK_DURATION_MS, "INTEGER DEFAULT 0");
                addColumnSafely(db, DatabaseContract.SessionEntry.TABLE_NAME, DatabaseContract.SessionEntry.COLUMN_IS_MOCK_TEST, "INTEGER DEFAULT 0");

                db.execSQL(DatabaseContract.SessionEntry.SQL_CREATE_IDX_EXAM_PROFILE);

                // Safely migrate question_logs table with v3 columns
                addColumnSafely(db, DatabaseContract.QuestionLogEntry.TABLE_NAME, DatabaseContract.QuestionLogEntry.COLUMN_LABEL, "TEXT DEFAULT ''");
                addColumnSafely(db, DatabaseContract.QuestionLogEntry.TABLE_NAME, DatabaseContract.QuestionLogEntry.COLUMN_TOPIC, "TEXT DEFAULT ''");
                addColumnSafely(db, DatabaseContract.QuestionLogEntry.TABLE_NAME, DatabaseContract.QuestionLogEntry.COLUMN_DIFFICULTY, "TEXT DEFAULT 'MEDIUM'");
                addColumnSafely(db, DatabaseContract.QuestionLogEntry.TABLE_NAME, DatabaseContract.QuestionLogEntry.COLUMN_OUTCOME, "TEXT DEFAULT 'SOLVED'");
                addColumnSafely(db, DatabaseContract.QuestionLogEntry.TABLE_NAME, DatabaseContract.QuestionLogEntry.COLUMN_CONFIDENCE, "INTEGER DEFAULT 3");
                addColumnSafely(db, DatabaseContract.QuestionLogEntry.TABLE_NAME, DatabaseContract.QuestionLogEntry.COLUMN_MISTAKE_TAG, "TEXT DEFAULT ''");
                addColumnSafely(db, DatabaseContract.QuestionLogEntry.TABLE_NAME, DatabaseContract.QuestionLogEntry.COLUMN_NOTES, "TEXT DEFAULT ''");

                db.execSQL(DatabaseContract.QuestionLogEntry.SQL_CREATE_IDX_TOPIC);
                db.execSQL(DatabaseContract.QuestionLogEntry.SQL_CREATE_IDX_OUTCOME);

                // Create new tables for v3
                db.execSQL(DatabaseContract.BreakEntry.SQL_CREATE_TABLE);
                db.execSQL(DatabaseContract.BreakEntry.SQL_CREATE_IDX_SESSION_ID);

                db.execSQL(DatabaseContract.StudyCoachFeedbackEntry.SQL_CREATE_TABLE);
                db.execSQL(DatabaseContract.StudyCoachFeedbackEntry.SQL_CREATE_IDX_KEY);

                db.execSQL(DatabaseContract.ExamProfileEntry.SQL_CREATE_TABLE);

                preseedDefaultExamProfiles(db);
                Log.i(TAG, "Upgraded schema to version 3: added dual clocks, breaks, exam profiles, and study coach feedback.");
            }

            if (oldVersion < 4) {
                // Safely migrate question_logs table with v4 audit columns
                addColumnSafely(db, DatabaseContract.QuestionLogEntry.TABLE_NAME, DatabaseContract.QuestionLogEntry.COLUMN_ORIGINAL_OUTCOME, "TEXT DEFAULT ''");
                addColumnSafely(db, DatabaseContract.QuestionLogEntry.TABLE_NAME, DatabaseContract.QuestionLogEntry.COLUMN_CORRECTION_TIMESTAMP, "INTEGER DEFAULT 0");

                // Create new tables for v4
                db.execSQL(DatabaseContract.SettingEntry.SQL_CREATE_TABLE);
                db.execSQL(DatabaseContract.SettingEntry.SQL_CREATE_IDX_KEY);

                db.execSQL(DatabaseContract.AttemptAuditEntry.SQL_CREATE_TABLE);
                db.execSQL(DatabaseContract.AttemptAuditEntry.SQL_CREATE_IDX_QUESTION_LOG_ID);
                db.execSQL(DatabaseContract.AttemptAuditEntry.SQL_CREATE_IDX_SESSION_ID);
                db.execSQL(DatabaseContract.AttemptAuditEntry.SQL_CREATE_IDX_TIMESTAMP);

                preseedDefaultSettings(db);
                Log.i(TAG, "Upgraded schema to version 4: added settings preferences storage and attempt audit history.");
            }

            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Migration error during database upgrade", e);
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Downgrading database from version " + oldVersion + " to " + newVersion);
        super.onDowngrade(db, oldVersion, newVersion);
    }

    private void addColumnSafely(SQLiteDatabase db, String table, String column, String definition) {
        if (!isColumnExists(db, table, column)) {
            try {
                String sql = "ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition + ";";
                db.execSQL(sql);
                Log.i(TAG, "Added column " + column + " to table " + table);
            } catch (Exception e) {
                Log.w(TAG, "Failed to add column " + column + " to table " + table + ": " + e.getMessage());
            }
        }
    }

    private boolean isColumnExists(SQLiteDatabase db, String table, String column) {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null);
            if (cursor != null) {
                int nameIndex = cursor.getColumnIndex("name");
                while (cursor.moveToNext()) {
                    if (nameIndex != -1 && column.equalsIgnoreCase(cursor.getString(nameIndex))) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error checking column existence for " + column, e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return false;
    }

    private void preseedDefaultSubjects(SQLiteDatabase db) {
        long now = System.currentTimeMillis();
        String[][] defaultSubjects = new String[][]{
                {"Mathematics", "#30D158"},
                {"Physics", "#0A84FF"},
                {"Chemistry", "#FF9F0A"},
                {"Biology", "#BF5AF2"},
                {"General", "#8E8E93"}
        };

        for (String[] sub : defaultSubjects) {
            ContentValues cv = new ContentValues();
            cv.put(DatabaseContract.SubjectEntry.COLUMN_NAME, sub[0]);
            cv.put(DatabaseContract.SubjectEntry.COLUMN_COLOR_HEX, sub[1]);
            cv.put(DatabaseContract.SubjectEntry.COLUMN_CREATED_AT, now);
            db.insertWithOnConflict(DatabaseContract.SubjectEntry.TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    private void preseedDefaultExamProfiles(SQLiteDatabase db) {
        ContentValues cv1 = new ContentValues();
        cv1.put(DatabaseContract.ExamProfileEntry.COLUMN_NAME, "JEE_MAIN");
        cv1.put(DatabaseContract.ExamProfileEntry.COLUMN_TOTAL_DURATION_MINUTES, 180);
        cv1.put(DatabaseContract.ExamProfileEntry.COLUMN_TOTAL_QUESTIONS, 75);
        cv1.put(DatabaseContract.ExamProfileEntry.COLUMN_CORRECT_MARKS, 4.0);
        cv1.put(DatabaseContract.ExamProfileEntry.COLUMN_NEGATIVE_MARKS, 1.0);
        cv1.put(DatabaseContract.ExamProfileEntry.COLUMN_SECTIONS_JSON,
                "[{\"name\":\"Physics\",\"questions\":25},{\"name\":\"Chemistry\",\"questions\":25},{\"name\":\"Mathematics\",\"questions\":25}]");
        db.insertWithOnConflict(DatabaseContract.ExamProfileEntry.TABLE_NAME, null, cv1, SQLiteDatabase.CONFLICT_IGNORE);

        ContentValues cv2 = new ContentValues();
        cv2.put(DatabaseContract.ExamProfileEntry.COLUMN_NAME, "JEE_ADVANCED");
        cv2.put(DatabaseContract.ExamProfileEntry.COLUMN_TOTAL_DURATION_MINUTES, 180);
        cv2.put(DatabaseContract.ExamProfileEntry.COLUMN_TOTAL_QUESTIONS, 54);
        cv2.put(DatabaseContract.ExamProfileEntry.COLUMN_CORRECT_MARKS, 4.0);
        cv2.put(DatabaseContract.ExamProfileEntry.COLUMN_NEGATIVE_MARKS, 2.0);
        cv2.put(DatabaseContract.ExamProfileEntry.COLUMN_SECTIONS_JSON,
                "[{\"name\":\"Physics\",\"questions\":18},{\"name\":\"Chemistry\",\"questions\":18},{\"name\":\"Mathematics\",\"questions\":18}]");
        db.insertWithOnConflict(DatabaseContract.ExamProfileEntry.TABLE_NAME, null, cv2, SQLiteDatabase.CONFLICT_IGNORE);

        ContentValues cv3 = new ContentValues();
        cv3.put(DatabaseContract.ExamProfileEntry.COLUMN_NAME, "PRACTICE");
        cv3.put(DatabaseContract.ExamProfileEntry.COLUMN_TOTAL_DURATION_MINUTES, 180);
        cv3.put(DatabaseContract.ExamProfileEntry.COLUMN_TOTAL_QUESTIONS, 50);
        cv3.put(DatabaseContract.ExamProfileEntry.COLUMN_CORRECT_MARKS, 4.0);
        cv3.put(DatabaseContract.ExamProfileEntry.COLUMN_NEGATIVE_MARKS, 1.0);
        cv3.put(DatabaseContract.ExamProfileEntry.COLUMN_SECTIONS_JSON, "[]");
        db.insertWithOnConflict(DatabaseContract.ExamProfileEntry.TABLE_NAME, null, cv3, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private void preseedDefaultSettings(SQLiteDatabase db) {
        String[][] defaultSettings = new String[][]{
                {"target_date", "2027-01-20"},
                {"two_zone_enabled", "false"},
                {"wallpaper_enabled", "false"},
                {"wallpaper_uri", ""},
                {"wallpaper_dim_alpha", "0.5"},
                {"wallpaper_blur_radius", "0"},
                {"wallpaper_style", "DEFAULT"},
                {"wallpaper_preferences", "{\"enabled\":false,\"uri\":\"\",\"dim_alpha\":0.5,\"blur_radius\":0,\"style\":\"DEFAULT\"}"},
                {"widget_theme", "AMOLED"},
                {"widget_show_countdown", "true"},
                {"widget_show_daily_goal", "true"},
                {"widget_opacity", "1.0"},
                {"widget_refresh_interval_minutes", "15"},
                {"widget_settings", "{\"theme\":\"AMOLED\",\"show_countdown\":true,\"show_daily_goal\":true,\"opacity\":1.0,\"refresh_interval_minutes\":15}"}
        };

        for (String[] setting : defaultSettings) {
            ContentValues cv = new ContentValues();
            cv.put(DatabaseContract.SettingEntry.COLUMN_KEY, setting[0]);
            cv.put(DatabaseContract.SettingEntry.COLUMN_VALUE, setting[1]);
            db.insertWithOnConflict(DatabaseContract.SettingEntry.TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }
}

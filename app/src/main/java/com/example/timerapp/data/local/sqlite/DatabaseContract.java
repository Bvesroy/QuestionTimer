package com.example.timerapp.data.local.sqlite;

import android.provider.BaseColumns;

/**
 * Defines table schema and column definitions for the Question Solving Timer database.
 */
public final class DatabaseContract {

    private DatabaseContract() {
        // Prevent instantiation
    }

    public static final String DATABASE_NAME = "question_timer.db";
    public static final int DATABASE_VERSION = 4;

    /**
     * Session table schema.
     */
    public static final class SessionEntry implements BaseColumns {
        public static final String TABLE_NAME = "sessions";
        public static final String COLUMN_ID = "_id";
        public static final String COLUMN_ALT_ID = "id";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_TARGET_COUNT = "target_count";
        public static final String COLUMN_CREATED_AT = "created_at";
        public static final String COLUMN_UPDATED_AT = "updated_at";
        public static final String COLUMN_IS_ACTIVE = "is_active";

        // Version 2 extended columns
        public static final String COLUMN_SUBJECT = "subject";
        public static final String COLUMN_NOTES = "notes";
        public static final String COLUMN_WENT_WELL = "went_well";
        public static final String COLUMN_NEEDS_IMPROVEMENT = "needs_improvement";
        public static final String COLUMN_STATUS = "status";
        public static final String COLUMN_PROFILE_ID = "profile_id";

        // Version 3 extended columns (Dual-clock, exam profiles & mock test tracking)
        public static final String COLUMN_EXAM_PROFILE = "exam_profile";
        public static final String COLUMN_TOTAL_STUDY_DURATION_MS = "total_study_duration_ms";
        public static final String COLUMN_TOTAL_BREAK_DURATION_MS = "total_break_duration_ms";
        public static final String COLUMN_IS_MOCK_TEST = "is_mock_test";

        // Version 4 extended columns (Wallpaper & widget integration)
        public static final String COLUMN_WALLPAPER_PATH = "wallpaper_path";
        public static final String COLUMN_WALLPAPER_ALPHA = "wallpaper_alpha";
        public static final String COLUMN_WIDGET_CONFIG_ID = "widget_config_id";

        public static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_ALT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_NAME + " TEXT NOT NULL, " +
                        COLUMN_TARGET_COUNT + " INTEGER NOT NULL DEFAULT 0, " +
                        COLUMN_CREATED_AT + " INTEGER NOT NULL, " +
                        COLUMN_UPDATED_AT + " INTEGER NOT NULL, " +
                        COLUMN_IS_ACTIVE + " INTEGER NOT NULL DEFAULT 0, " +
                        COLUMN_SUBJECT + " TEXT DEFAULT 'General', " +
                        COLUMN_NOTES + " TEXT DEFAULT '', " +
                        COLUMN_WENT_WELL + " TEXT DEFAULT '', " +
                        COLUMN_NEEDS_IMPROVEMENT + " TEXT DEFAULT '', " +
                        COLUMN_STATUS + " TEXT DEFAULT 'COMPLETED', " +
                        COLUMN_PROFILE_ID + " TEXT DEFAULT 'default', " +
                        COLUMN_EXAM_PROFILE + " TEXT DEFAULT 'PRACTICE', " +
                        COLUMN_TOTAL_STUDY_DURATION_MS + " INTEGER DEFAULT 0, " +
                        COLUMN_TOTAL_BREAK_DURATION_MS + " INTEGER DEFAULT 0, " +
                        COLUMN_IS_MOCK_TEST + " INTEGER DEFAULT 0, " +
                        COLUMN_WALLPAPER_PATH + " TEXT DEFAULT '', " +
                        COLUMN_WALLPAPER_ALPHA + " REAL DEFAULT 1.0, " +
                        COLUMN_WIDGET_CONFIG_ID + " TEXT DEFAULT ''" +
                        ");";

        public static final String SQL_CREATE_IDX_IS_ACTIVE =
                "CREATE INDEX IF NOT EXISTS idx_sessions_is_active ON " +
                        TABLE_NAME + " (" + COLUMN_IS_ACTIVE + ");";

        public static final String SQL_CREATE_IDX_UPDATED_AT =
                "CREATE INDEX IF NOT EXISTS idx_sessions_updated_at ON " +
                        TABLE_NAME + " (" + COLUMN_UPDATED_AT + " DESC);";

        public static final String SQL_CREATE_IDX_SUBJECT =
                "CREATE INDEX IF NOT EXISTS idx_sessions_subject ON " +
                        TABLE_NAME + " (" + COLUMN_SUBJECT + ");";

        public static final String SQL_CREATE_IDX_EXAM_PROFILE =
                "CREATE INDEX IF NOT EXISTS idx_sessions_exam_profile ON " +
                        TABLE_NAME + " (" + COLUMN_EXAM_PROFILE + ");";

        public static final String SQL_DROP_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
    }

    /**
     * Question logs table schema.
     */
    public static final class QuestionLogEntry implements BaseColumns {
        public static final String TABLE_NAME = "question_logs";
        public static final String COLUMN_ID = "_id";
        public static final String COLUMN_ALT_ID = "id";
        public static final String COLUMN_SESSION_ID = "session_id";
        public static final String COLUMN_QUESTION_NUM = "question_num";
        public static final String COLUMN_DURATION_MS = "duration_ms";
        public static final String COLUMN_COMPLETED_AT = "completed_at";

        // Version 3 extended columns (Analytics & mistake logging)
        public static final String COLUMN_LABEL = "label";
        public static final String COLUMN_TOPIC = "topic";
        public static final String COLUMN_DIFFICULTY = "difficulty";
        public static final String COLUMN_OUTCOME = "outcome";
        public static final String COLUMN_CONFIDENCE = "confidence";
        public static final String COLUMN_MISTAKE_TAG = "mistake_tag";
        public static final String COLUMN_NOTES = "notes";

        // Version 4 extended audit columns
        public static final String COLUMN_ORIGINAL_OUTCOME = "original_outcome";
        public static final String COLUMN_CORRECTION_TIMESTAMP = "correction_timestamp";

        public static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_ALT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_SESSION_ID + " INTEGER NOT NULL, " +
                        COLUMN_QUESTION_NUM + " INTEGER NOT NULL, " +
                        COLUMN_DURATION_MS + " INTEGER NOT NULL, " +
                        COLUMN_COMPLETED_AT + " INTEGER NOT NULL, " +
                        COLUMN_LABEL + " TEXT DEFAULT '', " +
                        COLUMN_TOPIC + " TEXT DEFAULT '', " +
                        COLUMN_DIFFICULTY + " TEXT DEFAULT 'MEDIUM', " +
                        COLUMN_OUTCOME + " TEXT DEFAULT 'SOLVED', " +
                        COLUMN_CONFIDENCE + " INTEGER DEFAULT 3, " +
                        COLUMN_MISTAKE_TAG + " TEXT DEFAULT '', " +
                        COLUMN_NOTES + " TEXT DEFAULT '', " +
                        COLUMN_ORIGINAL_OUTCOME + " TEXT DEFAULT '', " +
                        COLUMN_CORRECTION_TIMESTAMP + " INTEGER DEFAULT 0, " +
                        "FOREIGN KEY(" + COLUMN_SESSION_ID + ") REFERENCES " +
                        SessionEntry.TABLE_NAME + "(" + SessionEntry.COLUMN_ALT_ID + ") ON DELETE CASCADE" +
                        ");";

        public static final String SQL_CREATE_IDX_SESSION_ID =
                "CREATE INDEX IF NOT EXISTS idx_question_logs_session_id ON " +
                        TABLE_NAME + " (" + COLUMN_SESSION_ID + ");";

        public static final String SQL_CREATE_IDX_SESSION_QUESTION =
                "CREATE INDEX IF NOT EXISTS idx_question_logs_session_question ON " +
                        TABLE_NAME + " (" + COLUMN_SESSION_ID + ", " + COLUMN_QUESTION_NUM + ");";

        public static final String SQL_CREATE_IDX_TOPIC =
                "CREATE INDEX IF NOT EXISTS idx_question_logs_topic ON " +
                        TABLE_NAME + " (" + COLUMN_TOPIC + ");";

        public static final String SQL_CREATE_IDX_OUTCOME =
                "CREATE INDEX IF NOT EXISTS idx_question_logs_outcome ON " +
                        TABLE_NAME + " (" + COLUMN_OUTCOME + ");";

        public static final String SQL_DROP_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
    }

    /**
     * Breaks table schema (Dual-clock study interval tracking).
     */
    public static final class BreakEntry implements BaseColumns {
        public static final String TABLE_NAME = "breaks";
        public static final String COLUMN_ID = "_id";
        public static final String COLUMN_ALT_ID = "id";
        public static final String COLUMN_SESSION_ID = "session_id";
        public static final String COLUMN_START_TIME = "start_time";
        public static final String COLUMN_END_TIME = "end_time";
        public static final String COLUMN_DURATION_MS = "duration_ms";

        public static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_ALT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_SESSION_ID + " INTEGER NOT NULL, " +
                        COLUMN_START_TIME + " INTEGER NOT NULL, " +
                        COLUMN_END_TIME + " INTEGER NOT NULL DEFAULT 0, " +
                        COLUMN_DURATION_MS + " INTEGER NOT NULL DEFAULT 0, " +
                        "FOREIGN KEY(" + COLUMN_SESSION_ID + ") REFERENCES " +
                        SessionEntry.TABLE_NAME + "(" + SessionEntry.COLUMN_ALT_ID + ") ON DELETE CASCADE" +
                        ");";

        public static final String SQL_CREATE_IDX_SESSION_ID =
                "CREATE INDEX IF NOT EXISTS idx_breaks_session_id ON " +
                        TABLE_NAME + " (" + COLUMN_SESSION_ID + ");";

        public static final String SQL_DROP_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
    }

    /**
     * Study coach feedback table schema.
     */
    public static final class StudyCoachFeedbackEntry implements BaseColumns {
        public static final String TABLE_NAME = "study_coach_feedback";
        public static final String COLUMN_ID = "_id";
        public static final String COLUMN_ALT_ID = "id";
        public static final String COLUMN_RECOMMENDATION_KEY = "recommendation_key";
        public static final String COLUMN_FEEDBACK = "feedback";
        public static final String COLUMN_DISMISSED = "dismissed";
        public static final String COLUMN_CREATED_AT = "created_at";

        public static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_ALT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_RECOMMENDATION_KEY + " TEXT NOT NULL, " +
                        COLUMN_FEEDBACK + " TEXT DEFAULT '', " +
                        COLUMN_DISMISSED + " INTEGER DEFAULT 0, " +
                        COLUMN_CREATED_AT + " INTEGER NOT NULL" +
                        ");";

        public static final String SQL_CREATE_IDX_KEY =
                "CREATE INDEX IF NOT EXISTS idx_coach_feedback_key ON " +
                        TABLE_NAME + " (" + COLUMN_RECOMMENDATION_KEY + ");";

        public static final String SQL_DROP_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
    }

    /**
     * Exam profiles table schema.
     */
    public static final class ExamProfileEntry implements BaseColumns {
        public static final String TABLE_NAME = "exam_profiles";
        public static final String COLUMN_ID = "_id";
        public static final String COLUMN_ALT_ID = "id";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_TOTAL_DURATION_MINUTES = "total_duration_minutes";
        public static final String COLUMN_TOTAL_QUESTIONS = "total_questions";
        public static final String COLUMN_CORRECT_MARKS = "correct_marks";
        public static final String COLUMN_NEGATIVE_MARKS = "negative_marks";
        public static final String COLUMN_SECTIONS_JSON = "sections_json";

        public static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_ALT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_NAME + " TEXT UNIQUE NOT NULL, " +
                        COLUMN_TOTAL_DURATION_MINUTES + " INTEGER NOT NULL, " +
                        COLUMN_TOTAL_QUESTIONS + " INTEGER NOT NULL, " +
                        COLUMN_CORRECT_MARKS + " REAL NOT NULL DEFAULT 4.0, " +
                        COLUMN_NEGATIVE_MARKS + " REAL NOT NULL DEFAULT 1.0, " +
                        COLUMN_SECTIONS_JSON + " TEXT DEFAULT '[]'" +
                        ");";

        public static final String SQL_DROP_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
    }

    /**
     * Subjects table schema.
     */
    public static final class SubjectEntry implements BaseColumns {
        public static final String TABLE_NAME = "subjects";
        public static final String COLUMN_ID = "_id";
        public static final String COLUMN_ALT_ID = "id";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_COLOR_HEX = "color_hex";
        public static final String COLUMN_CREATED_AT = "created_at";

        public static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_ALT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_NAME + " TEXT UNIQUE NOT NULL, " +
                        COLUMN_COLOR_HEX + " TEXT NOT NULL, " +
                        COLUMN_CREATED_AT + " INTEGER NOT NULL" +
                        ");";

        public static final String SQL_DROP_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
    }

    /**
     * Exam countdowns table schema.
     */
    public static final class ExamCountdownEntry implements BaseColumns {
        public static final String TABLE_NAME = "exam_countdowns";
        public static final String COLUMN_ID = "_id";
        public static final String COLUMN_ALT_ID = "id";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_TARGET_DATE_MILLIS = "target_date_millis";
        public static final String COLUMN_DESCRIPTION = "description";

        public static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_ALT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_TITLE + " TEXT NOT NULL, " +
                        COLUMN_TARGET_DATE_MILLIS + " INTEGER NOT NULL, " +
                        COLUMN_DESCRIPTION + " TEXT" +
                        ");";

        public static final String SQL_DROP_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
    }

    /**
     * Achievements table schema.
     */
    public static final class AchievementEntry implements BaseColumns {
        public static final String TABLE_NAME = "achievements";
        public static final String COLUMN_ID = "_id";
        public static final String COLUMN_ALT_ID = "id";
        public static final String COLUMN_KEY = "key";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_UNLOCKED_AT = "unlocked_at";

        public static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_ALT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_KEY + " TEXT UNIQUE NOT NULL, " +
                        COLUMN_TITLE + " TEXT NOT NULL, " +
                        COLUMN_UNLOCKED_AT + " INTEGER NOT NULL" +
                        ");";

        public static final String SQL_DROP_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
    }

    /**
     * Settings / preferences table schema (Version 4).
     */
    public static final class SettingEntry implements BaseColumns {
        public static final String TABLE_NAME = "settings";
        public static final String COLUMN_ID = "_id";
        public static final String COLUMN_ALT_ID = "id";
        public static final String COLUMN_KEY = "key";
        public static final String COLUMN_VALUE = "value";

        public static final String SQL_CREATE_TABLE =
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                        COLUMN_ALT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_KEY + " TEXT UNIQUE NOT NULL, " +
                        COLUMN_VALUE + " TEXT NOT NULL" +
                        ");";

        public static final String SQL_CREATE_IDX_KEY =
                "CREATE INDEX IF NOT EXISTS idx_settings_key ON " +
                        TABLE_NAME + " (" + COLUMN_KEY + ");";

        public static final String SQL_DROP_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
    }

    /**
     * Attempt audit history table schema (Version 4).
     * Tracks original outcome, corrected outcome, and correction timestamp for question attempts.
     */
    public static final class AttemptAuditEntry implements BaseColumns {
        public static final String TABLE_NAME = "attempt_audit_history";
        public static final String COLUMN_ID = "_id";
        public static final String COLUMN_ALT_ID = "id";
        public static final String COLUMN_QUESTION_LOG_ID = "question_log_id";
        public static final String COLUMN_SESSION_ID = "session_id";
        public static final String COLUMN_ORIGINAL_OUTCOME = "original_outcome";
        public static final String COLUMN_CORRECTED_OUTCOME = "corrected_outcome";
        public static final String COLUMN_CORRECTION_TIMESTAMP = "correction_timestamp";
        public static final String COLUMN_NOTES = "notes";

        public static final String SQL_CREATE_TABLE =
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                        COLUMN_ALT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_QUESTION_LOG_ID + " INTEGER NOT NULL, " +
                        COLUMN_SESSION_ID + " INTEGER NOT NULL, " +
                        COLUMN_ORIGINAL_OUTCOME + " TEXT NOT NULL, " +
                        COLUMN_CORRECTED_OUTCOME + " TEXT NOT NULL, " +
                        COLUMN_CORRECTION_TIMESTAMP + " INTEGER NOT NULL, " +
                        COLUMN_NOTES + " TEXT DEFAULT '', " +
                        "FOREIGN KEY(" + COLUMN_QUESTION_LOG_ID + ") REFERENCES " +
                        QuestionLogEntry.TABLE_NAME + "(" + QuestionLogEntry.COLUMN_ALT_ID + ") ON DELETE CASCADE, " +
                        "FOREIGN KEY(" + COLUMN_SESSION_ID + ") REFERENCES " +
                        SessionEntry.TABLE_NAME + "(" + SessionEntry.COLUMN_ALT_ID + ") ON DELETE CASCADE" +
                        ");";

        public static final String SQL_CREATE_IDX_QUESTION_LOG_ID =
                "CREATE INDEX IF NOT EXISTS idx_audit_question_log_id ON " +
                        TABLE_NAME + " (" + COLUMN_QUESTION_LOG_ID + ");";

        public static final String SQL_CREATE_IDX_SESSION_ID =
                "CREATE INDEX IF NOT EXISTS idx_audit_session_id ON " +
                        TABLE_NAME + " (" + COLUMN_SESSION_ID + ");";

        public static final String SQL_CREATE_IDX_TIMESTAMP =
                "CREATE INDEX IF NOT EXISTS idx_audit_correction_time ON " +
                        TABLE_NAME + " (" + COLUMN_CORRECTION_TIMESTAMP + " DESC);";

        public static final String SQL_DROP_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
    }

    /**
     * Widget configuration table schema (Version 4).
     */
    public static final class WidgetConfigEntry implements BaseColumns {
        public static final String TABLE_NAME = "widget_configs";
        public static final String COLUMN_ID = "_id";
        public static final String COLUMN_ALT_ID = "id";
        public static final String COLUMN_WIDGET_ID = "widget_id";
        public static final String COLUMN_TARGET_EXAM_NAME = "target_exam_name";
        public static final String COLUMN_COUNTDOWN_DAYS = "countdown_days";
        public static final String COLUMN_THEME = "theme";
        public static final String COLUMN_TRANSPARENCY = "transparency";
        public static final String COLUMN_WALLPAPER_URI = "wallpaper_uri";
        public static final String COLUMN_REFRESH_INTERVAL_SEC = "refresh_interval_sec";
        public static final String COLUMN_IS_ACTIVE = "is_active";
        public static final String COLUMN_CREATED_AT = "created_at";
        public static final String COLUMN_UPDATED_AT = "updated_at";

        public static final String SQL_CREATE_TABLE =
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                        COLUMN_ALT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_WIDGET_ID + " TEXT UNIQUE NOT NULL, " +
                        COLUMN_TARGET_EXAM_NAME + " TEXT DEFAULT 'JEE ADVANCED', " +
                        COLUMN_COUNTDOWN_DAYS + " INTEGER DEFAULT 142, " +
                        COLUMN_THEME + " TEXT DEFAULT 'AMOLED_BLACK', " +
                        COLUMN_TRANSPARENCY + " REAL DEFAULT 0.85, " +
                        COLUMN_WALLPAPER_URI + " TEXT DEFAULT '', " +
                        COLUMN_REFRESH_INTERVAL_SEC + " INTEGER DEFAULT 60, " +
                        COLUMN_IS_ACTIVE + " INTEGER DEFAULT 1, " +
                        COLUMN_CREATED_AT + " INTEGER NOT NULL, " +
                        COLUMN_UPDATED_AT + " INTEGER NOT NULL" +
                        ");";

        public static final String SQL_DROP_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
    }
}

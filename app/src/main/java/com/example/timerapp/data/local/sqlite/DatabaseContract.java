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
    public static final int DATABASE_VERSION = 3;

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
                        COLUMN_IS_MOCK_TEST + " INTEGER DEFAULT 0" +
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
}

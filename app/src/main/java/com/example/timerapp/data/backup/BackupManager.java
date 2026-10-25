package com.example.timerapp.data.backup;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.timerapp.data.local.sqlite.DatabaseContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * Handles JSON backup and restore operations for Question Solving Timer.
 * Uses Android's built-in org.json package with zero external dependencies.
 */
public class BackupManager {

    private static final String TAG = "BackupManager";
    public static final int BACKUP_FORMAT_VERSION = 1;
    public static final String APP_IDENTIFIER = "QuestionSolvingTimer";

    // JSON Keys
    private static final String KEY_VERSION = "version";
    private static final String KEY_APP = "app";
    private static final String KEY_EXPORTED_AT = "exported_at";
    private static final String KEY_SESSIONS = "sessions";

    private static final String KEY_SESSION_ID = "id";
    private static final String KEY_SESSION_NAME = "name";
    private static final String KEY_SESSION_TARGET = "target_count";
    private static final String KEY_SESSION_CREATED_AT = "created_at";
    private static final String KEY_SESSION_UPDATED_AT = "updated_at";
    private static final String KEY_SESSION_IS_ACTIVE = "is_active";
    private static final String KEY_SESSION_SUBJECT = "subject";
    private static final String KEY_SESSION_NOTES = "notes";
    private static final String KEY_SESSION_WENT_WELL = "went_well";
    private static final String KEY_SESSION_NEEDS_IMPROVEMENT = "needs_improvement";
    private static final String KEY_SESSION_STATUS = "status";
    private static final String KEY_SESSION_PROFILE_ID = "profile_id";
    private static final String KEY_QUESTION_LOGS = "question_logs";

    private static final String KEY_LOG_ID = "id";
    private static final String KEY_LOG_QUESTION_NUM = "question_num";
    private static final String KEY_LOG_DURATION_MS = "duration_ms";
    private static final String KEY_LOG_COMPLETED_AT = "completed_at";

    /**
     * Serializes all sessions and associated question logs from SQLite database into JSON string.
     *
     * @param db SQLiteDatabase instance with read access.
     * @return Formatted JSON string containing all persistent data.
     * @throws JSONException If JSON building fails.
     */
    public static String exportBackupToJson(SQLiteDatabase db) throws JSONException {
        JSONObject root = new JSONObject();
        root.put(KEY_VERSION, BACKUP_FORMAT_VERSION);
        root.put(KEY_APP, APP_IDENTIFIER);
        root.put(KEY_EXPORTED_AT, System.currentTimeMillis());

        JSONArray sessionsArray = new JSONArray();

        String sessionQuery = "SELECT " +
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
                " ORDER BY " + DatabaseContract.SessionEntry.COLUMN_ALT_ID + " ASC";

        try (Cursor sessionCursor = db.rawQuery(sessionQuery, null)) {
            while (sessionCursor.moveToNext()) {
                long sessionId = sessionCursor.getLong(0);
                String name = sessionCursor.getString(1);
                int targetCount = sessionCursor.getInt(2);
                long createdAt = sessionCursor.getLong(3);
                long updatedAt = sessionCursor.getLong(4);
                int isActive = sessionCursor.getInt(5);
                String subject = sessionCursor.getString(6);
                String notes = sessionCursor.getString(7);
                String wentWell = sessionCursor.getString(8);
                String needsImprovement = sessionCursor.getString(9);
                String status = sessionCursor.getString(10);
                String profileId = sessionCursor.getString(11);

                JSONObject sessionObj = new JSONObject();
                sessionObj.put(KEY_SESSION_ID, sessionId);
                sessionObj.put(KEY_SESSION_NAME, name);
                sessionObj.put(KEY_SESSION_TARGET, targetCount);
                sessionObj.put(KEY_SESSION_CREATED_AT, createdAt);
                sessionObj.put(KEY_SESSION_UPDATED_AT, updatedAt);
                sessionObj.put(KEY_SESSION_IS_ACTIVE, isActive == 1);
                sessionObj.put(KEY_SESSION_SUBJECT, subject != null ? subject : "General");
                sessionObj.put(KEY_SESSION_NOTES, notes != null ? notes : "");
                sessionObj.put(KEY_SESSION_WENT_WELL, wentWell != null ? wentWell : "");
                sessionObj.put(KEY_SESSION_NEEDS_IMPROVEMENT, needsImprovement != null ? needsImprovement : "");
                sessionObj.put(KEY_SESSION_STATUS, status != null ? status : "COMPLETED");
                sessionObj.put(KEY_SESSION_PROFILE_ID, profileId != null ? profileId : "default");

                // Nested question logs
                JSONArray logsArray = new JSONArray();
                String logQuery = "SELECT " +
                        DatabaseContract.QuestionLogEntry.COLUMN_ALT_ID + ", " +
                        DatabaseContract.QuestionLogEntry.COLUMN_QUESTION_NUM + ", " +
                        DatabaseContract.QuestionLogEntry.COLUMN_DURATION_MS + ", " +
                        DatabaseContract.QuestionLogEntry.COLUMN_COMPLETED_AT +
                        " FROM " + DatabaseContract.QuestionLogEntry.TABLE_NAME +
                        " WHERE " + DatabaseContract.QuestionLogEntry.COLUMN_SESSION_ID + " = ? " +
                        " ORDER BY " + DatabaseContract.QuestionLogEntry.COLUMN_QUESTION_NUM + " ASC";

                try (Cursor logCursor = db.rawQuery(logQuery, new String[]{String.valueOf(sessionId)})) {
                    while (logCursor.moveToNext()) {
                        long logId = logCursor.getLong(0);
                        int questionNum = logCursor.getInt(1);
                        long durationMs = logCursor.getLong(2);
                        long completedAt = logCursor.getLong(3);

                        JSONObject logObj = new JSONObject();
                        logObj.put(KEY_LOG_ID, logId);
                        logObj.put(KEY_LOG_QUESTION_NUM, questionNum);
                        logObj.put(KEY_LOG_DURATION_MS, durationMs);
                        logObj.put(KEY_LOG_COMPLETED_AT, completedAt);
                        logsArray.put(logObj);
                    }
                }
                sessionObj.put(KEY_QUESTION_LOGS, logsArray);
                sessionsArray.put(sessionObj);
            }
        }

        root.put(KEY_SESSIONS, sessionsArray);
        return root.toString(2); // Indented 2 spaces for human-readable backup
    }

    /**
     * Imports and restores sessions and question logs from a JSON backup string into the database.
     * The entire restore operation executes within an atomic database transaction.
     * If an error occurs, changes are rolled back completely.
     *
     * @param db            SQLiteDatabase instance with write access.
     * @param jsonString    Backup JSON payload.
     * @param clearExisting If true, purges existing tables before importing;
     *                      if false, appends sessions without deleting existing ones.
     * @return Number of sessions successfully restored.
     * @throws JSONException If JSON parsing or validation fails.
     */
    public static int importBackupFromJson(SQLiteDatabase db, String jsonString, boolean clearExisting)
            throws JSONException {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            throw new IllegalArgumentException("Backup JSON content cannot be null or empty.");
        }

        JSONObject root = new JSONObject(jsonString);

        if (!root.has(KEY_SESSIONS)) {
            throw new JSONException("Invalid backup format: missing 'sessions' root array.");
        }

        JSONArray sessionsArray = root.getJSONArray(KEY_SESSIONS);
        int sessionCount = sessionsArray.length();

        db.beginTransaction();
        try {
            if (clearExisting) {
                db.delete(DatabaseContract.QuestionLogEntry.TABLE_NAME, null, null);
                db.delete(DatabaseContract.SessionEntry.TABLE_NAME, null, null);
            }

            boolean hasActiveSession = false;

            for (int i = 0; i < sessionCount; i++) {
                JSONObject sessionObj = sessionsArray.getJSONObject(i);

                String name = sessionObj.optString(KEY_SESSION_NAME, "Restored Session " + (i + 1));
                int targetCount = sessionObj.optInt(KEY_SESSION_TARGET, 0);
                long createdAt = sessionObj.optLong(KEY_SESSION_CREATED_AT, System.currentTimeMillis());
                long updatedAt = sessionObj.optLong(KEY_SESSION_UPDATED_AT, System.currentTimeMillis());
                boolean isActive = sessionObj.optBoolean(KEY_SESSION_IS_ACTIVE, false);
                String subject = sessionObj.optString(KEY_SESSION_SUBJECT, "General");
                String notes = sessionObj.optString(KEY_SESSION_NOTES, "");
                String wentWell = sessionObj.optString(KEY_SESSION_WENT_WELL, "");
                String needsImprovement = sessionObj.optString(KEY_SESSION_NEEDS_IMPROVEMENT, "");
                String status = sessionObj.optString(KEY_SESSION_STATUS, "COMPLETED");
                String profileId = sessionObj.optString(KEY_SESSION_PROFILE_ID, "default");

                // If another session was already marked active in this import, only keep the first or latest
                int isActiveInt = 0;
                if (isActive && !hasActiveSession) {
                    isActiveInt = 1;
                    hasActiveSession = true;
                }

                ContentValues sessionValues = new ContentValues();
                sessionValues.put(DatabaseContract.SessionEntry.COLUMN_NAME, name);
                sessionValues.put(DatabaseContract.SessionEntry.COLUMN_TARGET_COUNT, targetCount);
                sessionValues.put(DatabaseContract.SessionEntry.COLUMN_CREATED_AT, createdAt);
                sessionValues.put(DatabaseContract.SessionEntry.COLUMN_UPDATED_AT, updatedAt);
                sessionValues.put(DatabaseContract.SessionEntry.COLUMN_IS_ACTIVE, isActiveInt);
                sessionValues.put(DatabaseContract.SessionEntry.COLUMN_SUBJECT, subject);
                sessionValues.put(DatabaseContract.SessionEntry.COLUMN_NOTES, notes);
                sessionValues.put(DatabaseContract.SessionEntry.COLUMN_WENT_WELL, wentWell);
                sessionValues.put(DatabaseContract.SessionEntry.COLUMN_NEEDS_IMPROVEMENT, needsImprovement);
                sessionValues.put(DatabaseContract.SessionEntry.COLUMN_STATUS, status);
                sessionValues.put(DatabaseContract.SessionEntry.COLUMN_PROFILE_ID, profileId);

                long newSessionId = db.insertOrThrow(DatabaseContract.SessionEntry.TABLE_NAME, null, sessionValues);

                if (sessionObj.has(KEY_QUESTION_LOGS)) {
                    JSONArray logsArray = sessionObj.getJSONArray(KEY_QUESTION_LOGS);
                    int logsLength = logsArray.length();

                    for (int j = 0; j < logsLength; j++) {
                        JSONObject logObj = logsArray.getJSONObject(j);
                        int questionNum = logObj.optInt(KEY_LOG_QUESTION_NUM, j + 1);
                        long durationMs = logObj.optLong(KEY_LOG_DURATION_MS, 0L);
                        long completedAt = logObj.optLong(KEY_LOG_COMPLETED_AT, System.currentTimeMillis());

                        ContentValues logValues = new ContentValues();
                        logValues.put(DatabaseContract.QuestionLogEntry.COLUMN_SESSION_ID, newSessionId);
                        logValues.put(DatabaseContract.QuestionLogEntry.COLUMN_QUESTION_NUM, questionNum);
                        logValues.put(DatabaseContract.QuestionLogEntry.COLUMN_DURATION_MS, durationMs);
                        logValues.put(DatabaseContract.QuestionLogEntry.COLUMN_COMPLETED_AT, completedAt);

                        db.insertOrThrow(DatabaseContract.QuestionLogEntry.TABLE_NAME, null, logValues);
                    }
                }
            }

            // Ensure at least one session is active if existing sessions were cleared and none was active
            if (clearExisting && !hasActiveSession && sessionCount > 0) {
                // Activate the most recent session
                String activateLatestSql = "UPDATE " + DatabaseContract.SessionEntry.TABLE_NAME +
                        " SET " + DatabaseContract.SessionEntry.COLUMN_IS_ACTIVE + " = 1 " +
                        " WHERE " + DatabaseContract.SessionEntry.COLUMN_ALT_ID + " = (" +
                        "   SELECT " + DatabaseContract.SessionEntry.COLUMN_ALT_ID +
                        "   FROM " + DatabaseContract.SessionEntry.TABLE_NAME +
                        "   ORDER BY " + DatabaseContract.SessionEntry.COLUMN_UPDATED_AT + " DESC LIMIT 1" +
                        ")";
                db.execSQL(activateLatestSql);
            }

            db.setTransactionSuccessful();
            Log.i(TAG, "Successfully restored " + sessionCount + " sessions from JSON backup.");
            return sessionCount;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Exports backup JSON directly to a destination File.
     */
    public static void exportBackupToFile(SQLiteDatabase db, File file) throws IOException, JSONException {
        if (file == null) {
            throw new IllegalArgumentException("Destination file cannot be null.");
        }
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        String json = exportBackupToJson(db);
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(json);
            writer.flush();
        }
    }

    /**
     * Imports backup directly from a source File.
     */
    public static int importBackupFromFile(SQLiteDatabase db, File file, boolean clearExisting)
            throws IOException, JSONException {
        if (file == null || !file.exists() || !file.canRead()) {
            throw new IOException("Source backup file does not exist or cannot be read.");
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return importBackupFromJson(db, sb.toString(), clearExisting);
    }

    /**
     * Complete SAF stream export including all auxiliary tables and preferences.
     */
    public static void exportBackupToStream(SQLiteDatabase db, java.io.OutputStream out, JSONObject preferences)
            throws IOException, JSONException {
        SafBackupEngine.exportBackup(db, out, preferences);
    }

    /**
     * Complete SAF stream restore.
     */
    public static SafBackupEngine.RestoreResult importBackupFromStream(
            SQLiteDatabase db, java.io.InputStream in, SafBackupEngine.RestoreMode mode)
            throws IOException, JSONException {
        return SafBackupEngine.restoreBackup(db, in, mode);
    }

    /**
     * Validates SAF backup integrity and schema.
     */
    public static SafBackupEngine.ValidationResult validateBackupStream(java.io.InputStream in) {
        return SafBackupEngine.validateBackup(in);
    }
}

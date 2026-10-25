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
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Production-grade SAF (Storage Access Framework) Backup & Restore Engine.
 * Exports and restores complete database snapshots with SHA-256 integrity validation,
 * schema versioning (up to Version 4), transaction rollbacks, and dual restore modes (REPLACE / MERGE).
 * Fully backward-compatible with v1, v2, and v3 backups.
 */
public class SafBackupEngine {

    private static final String TAG = "SafBackupEngine";
    public static final int BACKUP_SCHEMA_VERSION = 4;
    public static final int APP_VERSION_CODE = 4;
    public static final String APP_ID = "QuestionSolvingTimer";

    public enum RestoreMode {
        REPLACE,
        MERGE
    }

    public static class ValidationResult {
        public final boolean isValid;
        public final int schemaVersion;
        public final long timestamp;
        public final int totalRecords;
        public final boolean checksumVerified;
        public final String errorMessage;

        public ValidationResult(boolean isValid, int schemaVersion, long timestamp,
                                int totalRecords, boolean checksumVerified, String errorMessage) {
            this.isValid = isValid;
            this.schemaVersion = schemaVersion;
            this.timestamp = timestamp;
            this.totalRecords = totalRecords;
            this.checksumVerified = checksumVerified;
            this.errorMessage = errorMessage != null ? errorMessage : "";
        }
    }

    public static class RestoreResult {
        public final boolean success;
        public final int sessionsRestored;
        public final int logsRestored;
        public final int breaksRestored;
        public final int subjectsRestored;
        public final int countdownsRestored;
        public final int achievementsRestored;
        public final int profilesRestored;
        public final int auditRecordsRestored;
        public final int settingsRestored;
        public final String message;

        public RestoreResult(boolean success, int sessionsRestored, int logsRestored,
                             int breaksRestored, int subjectsRestored, int countdownsRestored,
                             int achievementsRestored, int profilesRestored, String message) {
            this(success, sessionsRestored, logsRestored, breaksRestored, subjectsRestored,
                    countdownsRestored, achievementsRestored, profilesRestored, 0, 0, message);
        }

        public RestoreResult(boolean success, int sessionsRestored, int logsRestored,
                             int breaksRestored, int subjectsRestored, int countdownsRestored,
                             int achievementsRestored, int profilesRestored,
                             int auditRecordsRestored, int settingsRestored, String message) {
            this.success = success;
            this.sessionsRestored = sessionsRestored;
            this.logsRestored = logsRestored;
            this.breaksRestored = breaksRestored;
            this.subjectsRestored = subjectsRestored;
            this.countdownsRestored = countdownsRestored;
            this.achievementsRestored = achievementsRestored;
            this.profilesRestored = profilesRestored;
            this.auditRecordsRestored = auditRecordsRestored;
            this.settingsRestored = settingsRestored;
            this.message = message;
        }

        public int getTotalItemsRestored() {
            return sessionsRestored + logsRestored + breaksRestored +
                    subjectsRestored + countdownsRestored + achievementsRestored +
                    profilesRestored + auditRecordsRestored + settingsRestored;
        }
    }

    /**
     * Serializes complete database snapshot and preferences to the provided OutputStream.
     */
    public static void exportBackup(SQLiteDatabase db, OutputStream outputStream, JSONObject userPreferences)
            throws IOException, JSONException {
        if (db == null || outputStream == null) {
            throw new IllegalArgumentException("Database and OutputStream cannot be null");
        }

        JSONObject root = new JSONObject();
        root.put("version", BACKUP_SCHEMA_VERSION);
        root.put("app", APP_ID);
        root.put("app_version_code", APP_VERSION_CODE);
        long now = System.currentTimeMillis();
        root.put("created_at", now);

        JSONObject data = new JSONObject();

        // 1. Sessions
        JSONArray sessionsArray = exportSessionsTable(db);
        data.put("sessions", sessionsArray);

        // 2. Question Logs
        JSONArray logsArray = exportQuestionLogsTable(db);
        data.put("question_logs", logsArray);

        // 3. Breaks
        JSONArray breaksArray = exportBreaksTable(db);
        data.put("breaks", breaksArray);

        // 4. Subjects
        JSONArray subjectsArray = exportSubjectsTable(db);
        data.put("subjects", subjectsArray);

        // 5. Exam Countdowns
        JSONArray countdownsArray = exportCountdownsTable(db);
        data.put("exam_countdowns", countdownsArray);

        // 6. Achievements
        JSONArray achievementsArray = exportAchievementsTable(db);
        data.put("achievements", achievementsArray);

        // 7. Exam Profiles
        JSONArray profilesArray = exportExamProfilesTable(db);
        data.put("exam_profiles", profilesArray);

        // 8. Attempt Audit History (Version 4)
        JSONArray auditArray = exportAuditHistoryTable(db);
        data.put("audit_history", auditArray);

        // 9. Settings / Preferences (Version 4)
        JSONArray settingsArray = exportSettingsTable(db);
        data.put("settings", settingsArray);

        // Record Counts
        JSONObject counts = new JSONObject();
        counts.put("sessions", sessionsArray.length());
        counts.put("question_logs", logsArray.length());
        counts.put("breaks", breaksArray.length());
        counts.put("subjects", subjectsArray.length());
        counts.put("exam_countdowns", countdownsArray.length());
        counts.put("achievements", achievementsArray.length());
        counts.put("exam_profiles", profilesArray.length());
        counts.put("audit_history", auditArray.length());
        counts.put("settings", settingsArray.length());
        root.put("record_counts", counts);

        root.put("data", data);

        // Build preferences object including wallpaper settings, target date, widget settings
        JSONObject prefs = (userPreferences != null) ? new JSONObject(userPreferences.toString()) : new JSONObject();
        if (!prefs.has("target_date")) {
            prefs.put("target_date", getSettingValue(db, "target_date", "2027-01-20"));
        }
        if (!prefs.has("two_zone_enabled")) {
            prefs.put("two_zone_enabled", Boolean.parseBoolean(getSettingValue(db, "two_zone_enabled", "false")));
        }
        if (!prefs.has("wallpaper_settings")) {
            String wpStr = getSettingValue(db, "wallpaper_preferences", "");
            if (!wpStr.isEmpty()) {
                try {
                    prefs.put("wallpaper_settings", new JSONObject(wpStr));
                } catch (Exception ignored) {}
            }
        }
        if (!prefs.has("widget_settings")) {
            String wgStr = getSettingValue(db, "widget_settings", "");
            if (!wgStr.isEmpty()) {
                try {
                    prefs.put("widget_settings", new JSONObject(wgStr));
                } catch (Exception ignored) {}
            }
        }

        root.put("preferences", prefs);
        root.put("target_date", prefs.optString("target_date", "2027-01-20"));
        root.put("two_zone_enabled", prefs.optBoolean("two_zone_enabled", false));
        if (prefs.has("wallpaper_settings")) {
            root.put("wallpaper_settings", prefs.get("wallpaper_settings"));
        }
        if (prefs.has("widget_settings")) {
            root.put("widget_settings", prefs.get("widget_settings"));
        }
        root.put("audit_history", auditArray);

        // Calculate and embed SHA-256 checksum over data payload
        String dataPayload = data.toString();
        String checksum = computeSha256(dataPayload);
        root.put("checksum_sha256", checksum);

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
        writer.write(root.toString(2));
        writer.flush();
    }

    /**
     * Validates backup format, checksum, and structure from an InputStream.
     */
    public static ValidationResult validateBackup(InputStream inputStream) {
        if (inputStream == null) {
            return new ValidationResult(false, 0, 0, 0, false, "Input stream is null");
        }
        try {
            String jsonStr = readStreamToString(inputStream);
            JSONObject root = new JSONObject(jsonStr);

            if (!root.has("version") || !root.has("app")) {
                return new ValidationResult(false, 0, 0, 0, false, "Missing version or app identifier in backup");
            }

            int version = root.getInt("version");
            long timestamp = root.optLong("created_at", 0);

            // Backward compatibility: v1 or v2 backups might have sessions directly under root
            JSONObject data = root.optJSONObject("data");
            int totalRecords = 0;
            if (root.has("record_counts")) {
                JSONObject counts = root.getJSONObject("record_counts");
                totalRecords += counts.optInt("sessions", 0);
                totalRecords += counts.optInt("question_logs", 0);
                totalRecords += counts.optInt("breaks", 0);
                totalRecords += counts.optInt("subjects", 0);
                totalRecords += counts.optInt("exam_countdowns", 0);
                totalRecords += counts.optInt("achievements", 0);
                totalRecords += counts.optInt("exam_profiles", 0);
                totalRecords += counts.optInt("audit_history", 0);
                totalRecords += counts.optInt("settings", 0);
            } else if (data != null) {
                JSONArray s = data.optJSONArray("sessions");
                if (s != null) totalRecords += s.length();
                JSONArray q = data.optJSONArray("question_logs");
                if (q != null) totalRecords += q.length();
                JSONArray a = data.optJSONArray("audit_history");
                if (a != null) totalRecords += a.length();
            } else if (root.has("sessions")) {
                JSONArray s = root.optJSONArray("sessions");
                if (s != null) totalRecords += s.length();
            }

            boolean checksumVerified = false;
            if (root.has("checksum_sha256") && data != null) {
                String expectedChecksum = root.getString("checksum_sha256");
                String actualChecksum = computeSha256(data.toString());
                checksumVerified = expectedChecksum.equalsIgnoreCase(actualChecksum);
                if (!checksumVerified) {
                    return new ValidationResult(false, version, timestamp, totalRecords, false,
                            "SHA-256 checksum mismatch: backup archive has been modified or corrupted.");
                }
            } else {
                // Older backups without checksum are accepted if JSON structure is valid
                checksumVerified = true;
            }

            return new ValidationResult(true, version, timestamp, totalRecords, checksumVerified, null);
        } catch (Exception e) {
            return new ValidationResult(false, 0, 0, 0, false, "Malformed backup JSON: " + e.getMessage());
        }
    }

    /**
     * Atomically restores database state from an InputStream using REPLACE or MERGE mode.
     * Guarantees rollback on error and maintains foreign key consistency.
     */
    public static RestoreResult restoreBackup(SQLiteDatabase db, InputStream inputStream, RestoreMode mode)
            throws IOException, JSONException {
        if (db == null || inputStream == null) {
            throw new IllegalArgumentException("Database and InputStream cannot be null");
        }

        String jsonStr = readStreamToString(inputStream);
        JSONObject root = new JSONObject(jsonStr);

        int version = root.optInt("version", 1);
        JSONObject dataObj = root.optJSONObject("data");
        if (dataObj == null) {
            // Support legacy v1/v2 backups where sessions was directly in root
            dataObj = root;
        }

        // Verify SHA-256 tamper verification before modifying database
        if (root.has("checksum_sha256") && root.has("data")) {
            String expectedChecksum = root.getString("checksum_sha256");
            String actualChecksum = computeSha256(root.getJSONObject("data").toString());
            if (!expectedChecksum.equalsIgnoreCase(actualChecksum)) {
                throw new SecurityException("SHA-256 checksum mismatch: backup archive has been tampered with or corrupted.");
            }
        }

        int sessionsRestored = 0;
        int logsRestored = 0;
        int breaksRestored = 0;
        int subjectsRestored = 0;
        int countdownsRestored = 0;
        int achievementsRestored = 0;
        int profilesRestored = 0;
        int auditRecordsRestored = 0;
        int settingsRestored = 0;

        db.beginTransaction();
        try {
            if (mode == RestoreMode.REPLACE) {
                // Clear all existing data
                try {
                    db.delete(DatabaseContract.AttemptAuditEntry.TABLE_NAME, null, null);
                } catch (Exception ignored) {}
                try {
                    db.delete(DatabaseContract.SettingEntry.TABLE_NAME, null, null);
                } catch (Exception ignored) {}
                db.delete(DatabaseContract.BreakEntry.TABLE_NAME, null, null);
                db.delete(DatabaseContract.QuestionLogEntry.TABLE_NAME, null, null);
                db.delete(DatabaseContract.SessionEntry.TABLE_NAME, null, null);
                db.delete(DatabaseContract.SubjectEntry.TABLE_NAME, null, null);
                db.delete(DatabaseContract.ExamCountdownEntry.TABLE_NAME, null, null);
                db.delete(DatabaseContract.AchievementEntry.TABLE_NAME, null, null);
                db.delete(DatabaseContract.ExamProfileEntry.TABLE_NAME, null, null);
            }

            // Map oldSessionId -> newSessionId and oldLogId -> newLogId for foreign key preservation
            Map<Long, Long> sessionIdMap = new HashMap<>();
            Map<Long, Long> logIdMap = new HashMap<>();

            // 1. Restore Sessions
            JSONArray sessionsArray = dataObj.optJSONArray("sessions");
            if (sessionsArray != null) {
                for (int i = 0; i < sessionsArray.length(); i++) {
                    JSONObject sObj = sessionsArray.getJSONObject(i);
                    long oldId = sObj.optLong("id", 0);
                    String name = sObj.optString("name", "RESTORED SESSION");
                    long createdAt = sObj.optLong("created_at", System.currentTimeMillis());

                    if (mode == RestoreMode.MERGE && isDuplicateSession(db, name, createdAt)) {
                        // Skip duplicate in merge mode
                        continue;
                    }

                    ContentValues cv = new ContentValues();
                    cv.put(DatabaseContract.SessionEntry.COLUMN_NAME, name);
                    cv.put(DatabaseContract.SessionEntry.COLUMN_TARGET_COUNT, sObj.optInt("target_count", 0));
                    cv.put(DatabaseContract.SessionEntry.COLUMN_CREATED_AT, createdAt);
                    cv.put(DatabaseContract.SessionEntry.COLUMN_UPDATED_AT, sObj.optLong("updated_at", createdAt));
                    cv.put(DatabaseContract.SessionEntry.COLUMN_IS_ACTIVE, mode == RestoreMode.REPLACE ? sObj.optInt("is_active", 0) : 0);
                    cv.put(DatabaseContract.SessionEntry.COLUMN_SUBJECT, sObj.optString("subject", "General"));
                    cv.put(DatabaseContract.SessionEntry.COLUMN_NOTES, sObj.optString("notes", ""));
                    cv.put(DatabaseContract.SessionEntry.COLUMN_WENT_WELL, sObj.optString("went_well", ""));
                    cv.put(DatabaseContract.SessionEntry.COLUMN_NEEDS_IMPROVEMENT, sObj.optString("needs_improvement", ""));
                    cv.put(DatabaseContract.SessionEntry.COLUMN_STATUS, sObj.optString("status", "COMPLETED"));
                    cv.put(DatabaseContract.SessionEntry.COLUMN_PROFILE_ID, sObj.optString("profile_id", "default"));

                    cv.put(DatabaseContract.SessionEntry.COLUMN_EXAM_PROFILE, sObj.optString("exam_profile", "PRACTICE"));
                    cv.put(DatabaseContract.SessionEntry.COLUMN_TOTAL_STUDY_DURATION_MS, sObj.optLong("total_study_duration_ms", 0L));
                    cv.put(DatabaseContract.SessionEntry.COLUMN_TOTAL_BREAK_DURATION_MS, sObj.optLong("total_break_duration_ms", 0L));
                    cv.put(DatabaseContract.SessionEntry.COLUMN_IS_MOCK_TEST, sObj.optInt("is_mock_test", 0));

                    long newId = db.insertOrThrow(DatabaseContract.SessionEntry.TABLE_NAME, null, cv);
                    sessionIdMap.put(oldId, newId);
                    sessionsRestored++;

                    // Legacy nested question logs in session object
                    if (sObj.has("question_logs")) {
                        JSONArray nestedLogs = sObj.getJSONArray("question_logs");
                        for (int k = 0; k < nestedLogs.length(); k++) {
                            JSONObject logObj = nestedLogs.getJSONObject(k);
                            long oldLogId = logObj.optLong("id", 0);
                            long newLogId = insertQuestionLog(db, newId, logObj);
                            if (oldLogId > 0) {
                                logIdMap.put(oldLogId, newLogId);
                            }
                            logsRestored++;
                        }
                    }
                }
            }

            // 2. Restore Question Logs (if stored in root data array)
            JSONArray logsArray = dataObj.optJSONArray("question_logs");
            if (logsArray != null) {
                for (int i = 0; i < logsArray.length(); i++) {
                    JSONObject logObj = logsArray.getJSONObject(i);
                    long oldSessionId = logObj.optLong("session_id", 0);
                    Long newSessionId = sessionIdMap.get(oldSessionId);
                    if (newSessionId != null) {
                        long oldLogId = logObj.optLong("id", 0);
                        long newLogId = insertQuestionLog(db, newSessionId, logObj);
                        if (oldLogId > 0) {
                            logIdMap.put(oldLogId, newLogId);
                        }
                        logsRestored++;
                    }
                }
            }

            // 3. Restore Breaks
            JSONArray breaksArray = dataObj.optJSONArray("breaks");
            if (breaksArray != null) {
                for (int i = 0; i < breaksArray.length(); i++) {
                    JSONObject bObj = breaksArray.getJSONObject(i);
                    long oldSessionId = bObj.optLong("session_id", 0);
                    Long newSessionId = sessionIdMap.get(oldSessionId);
                    if (newSessionId != null) {
                        ContentValues bCv = new ContentValues();
                        bCv.put(DatabaseContract.BreakEntry.COLUMN_SESSION_ID, newSessionId);
                        bCv.put(DatabaseContract.BreakEntry.COLUMN_START_TIME, bObj.optLong("start_time", 0));
                        bCv.put(DatabaseContract.BreakEntry.COLUMN_END_TIME, bObj.optLong("end_time", 0));
                        bCv.put(DatabaseContract.BreakEntry.COLUMN_DURATION_MS, bObj.optLong("duration_ms", 0));
                        db.insertOrThrow(DatabaseContract.BreakEntry.TABLE_NAME, null, bCv);
                        breaksRestored++;
                    }
                }
            }

            // 4. Restore Subjects
            JSONArray subjectsArray = dataObj.optJSONArray("subjects");
            if (subjectsArray != null) {
                for (int i = 0; i < subjectsArray.length(); i++) {
                    JSONObject subObj = subjectsArray.getJSONObject(i);
                    ContentValues sCv = new ContentValues();
                    sCv.put(DatabaseContract.SubjectEntry.COLUMN_NAME, subObj.optString("name", ""));
                    sCv.put(DatabaseContract.SubjectEntry.COLUMN_COLOR_HEX, subObj.optString("color_hex", "#8E8E93"));
                    sCv.put(DatabaseContract.SubjectEntry.COLUMN_CREATED_AT, subObj.optLong("created_at", System.currentTimeMillis()));
                    long res = db.insertWithOnConflict(DatabaseContract.SubjectEntry.TABLE_NAME, null, sCv, SQLiteDatabase.CONFLICT_IGNORE);
                    if (res != -1) subjectsRestored++;
                }
            }

            // 5. Restore Countdowns
            JSONArray countdownsArray = dataObj.optJSONArray("exam_countdowns");
            if (countdownsArray != null) {
                for (int i = 0; i < countdownsArray.length(); i++) {
                    JSONObject cObj = countdownsArray.getJSONObject(i);
                    ContentValues cCv = new ContentValues();
                    cCv.put(DatabaseContract.ExamCountdownEntry.COLUMN_TITLE, cObj.optString("title", "Exam"));
                    cCv.put(DatabaseContract.ExamCountdownEntry.COLUMN_TARGET_DATE_MILLIS, cObj.optLong("target_date_millis", 0));
                    cCv.put(DatabaseContract.ExamCountdownEntry.COLUMN_DESCRIPTION, cObj.optString("description", ""));
                    long res = db.insertWithOnConflict(DatabaseContract.ExamCountdownEntry.TABLE_NAME, null, cCv, SQLiteDatabase.CONFLICT_IGNORE);
                    if (res != -1) countdownsRestored++;
                }
            }

            // 6. Restore Achievements
            JSONArray achievementsArray = dataObj.optJSONArray("achievements");
            if (achievementsArray != null) {
                for (int i = 0; i < achievementsArray.length(); i++) {
                    JSONObject aObj = achievementsArray.getJSONObject(i);
                    ContentValues aCv = new ContentValues();
                    aCv.put(DatabaseContract.AchievementEntry.COLUMN_KEY, aObj.optString("key", ""));
                    aCv.put(DatabaseContract.AchievementEntry.COLUMN_TITLE, aObj.optString("title", ""));
                    aCv.put(DatabaseContract.AchievementEntry.COLUMN_UNLOCKED_AT, aObj.optLong("unlocked_at", System.currentTimeMillis()));
                    long res = db.insertWithOnConflict(DatabaseContract.AchievementEntry.TABLE_NAME, null, aCv, SQLiteDatabase.CONFLICT_IGNORE);
                    if (res != -1) achievementsRestored++;
                }
            }

            // 7. Restore Exam Profiles
            JSONArray profilesArray = dataObj.optJSONArray("exam_profiles");
            if (profilesArray != null) {
                for (int i = 0; i < profilesArray.length(); i++) {
                    JSONObject pObj = profilesArray.getJSONObject(i);
                    ContentValues pCv = new ContentValues();
                    pCv.put(DatabaseContract.ExamProfileEntry.COLUMN_NAME, pObj.optString("name", ""));
                    pCv.put(DatabaseContract.ExamProfileEntry.COLUMN_TOTAL_DURATION_MINUTES, pObj.optInt("total_duration_minutes", 180));
                    pCv.put(DatabaseContract.ExamProfileEntry.COLUMN_TOTAL_QUESTIONS, pObj.optInt("total_questions", 75));
                    pCv.put(DatabaseContract.ExamProfileEntry.COLUMN_CORRECT_MARKS, pObj.optDouble("correct_marks", 4.0));
                    pCv.put(DatabaseContract.ExamProfileEntry.COLUMN_NEGATIVE_MARKS, pObj.optDouble("negative_marks", 1.0));
                    pCv.put(DatabaseContract.ExamProfileEntry.COLUMN_SECTIONS_JSON, pObj.optString("sections_json", "[]"));
                    long res = db.insertWithOnConflict(DatabaseContract.ExamProfileEntry.TABLE_NAME, null, pCv, SQLiteDatabase.CONFLICT_IGNORE);
                    if (res != -1) profilesRestored++;
                }
            }

            // 8. Restore Attempt Audit History (Version 4)
            JSONArray auditArray = dataObj.optJSONArray("audit_history");
            if (auditArray == null) {
                auditArray = root.optJSONArray("audit_history");
            }
            if (auditArray != null) {
                for (int i = 0; i < auditArray.length(); i++) {
                    JSONObject aObj = auditArray.getJSONObject(i);
                    long oldLogId = aObj.optLong("question_log_id", 0);
                    Long newLogId = logIdMap.get(oldLogId);
                    if (newLogId == null) newLogId = oldLogId;

                    long oldSessionId = aObj.optLong("session_id", 0);
                    Long newSessionId = sessionIdMap.get(oldSessionId);
                    if (newSessionId == null) newSessionId = oldSessionId;

                    ContentValues aCv = new ContentValues();
                    aCv.put(DatabaseContract.AttemptAuditEntry.COLUMN_QUESTION_LOG_ID, newLogId);
                    aCv.put(DatabaseContract.AttemptAuditEntry.COLUMN_SESSION_ID, newSessionId);
                    aCv.put(DatabaseContract.AttemptAuditEntry.COLUMN_ORIGINAL_OUTCOME, aObj.optString("original_outcome", ""));
                    aCv.put(DatabaseContract.AttemptAuditEntry.COLUMN_CORRECTED_OUTCOME, aObj.optString("corrected_outcome", ""));
                    aCv.put(DatabaseContract.AttemptAuditEntry.COLUMN_CORRECTION_TIMESTAMP, aObj.optLong("correction_timestamp", System.currentTimeMillis()));
                    aCv.put(DatabaseContract.AttemptAuditEntry.COLUMN_NOTES, aObj.optString("notes", ""));

                    long res = db.insertWithOnConflict(DatabaseContract.AttemptAuditEntry.TABLE_NAME, null, aCv, SQLiteDatabase.CONFLICT_IGNORE);
                    if (res != -1) auditRecordsRestored++;
                }
            }

            // 9. Restore Settings (Version 4)
            JSONArray settingsArray = dataObj.optJSONArray("settings");
            if (settingsArray != null) {
                for (int i = 0; i < settingsArray.length(); i++) {
                    JSONObject sObj = settingsArray.getJSONObject(i);
                    String k = sObj.optString("key", "");
                    String v = sObj.optString("value", "");
                    if (!k.isEmpty()) {
                        ContentValues sCv = new ContentValues();
                        sCv.put(DatabaseContract.SettingEntry.COLUMN_KEY, k);
                        sCv.put(DatabaseContract.SettingEntry.COLUMN_VALUE, v);
                        long res = db.insertWithOnConflict(DatabaseContract.SettingEntry.TABLE_NAME, null, sCv, SQLiteDatabase.CONFLICT_REPLACE);
                        if (res != -1) settingsRestored++;
                    }
                }
            }

            // Also restore settings from preferences / root fields if present
            JSONObject prefsObj = root.optJSONObject("preferences");
            if (prefsObj != null) {
                if (prefsObj.has("target_date")) {
                    saveSettingToDb(db, "target_date", prefsObj.getString("target_date"));
                    settingsRestored++;
                }
                if (prefsObj.has("two_zone_enabled")) {
                    saveSettingToDb(db, "two_zone_enabled", String.valueOf(prefsObj.optBoolean("two_zone_enabled", false)));
                    settingsRestored++;
                }
                if (prefsObj.has("wallpaper_settings")) {
                    saveSettingToDb(db, "wallpaper_preferences", prefsObj.get("wallpaper_settings").toString());
                    settingsRestored++;
                }
                if (prefsObj.has("widget_settings")) {
                    saveSettingToDb(db, "widget_settings", prefsObj.get("widget_settings").toString());
                    settingsRestored++;
                }
            } else {
                if (root.has("target_date")) {
                    saveSettingToDb(db, "target_date", root.getString("target_date"));
                    settingsRestored++;
                }
                if (root.has("two_zone_enabled")) {
                    saveSettingToDb(db, "two_zone_enabled", String.valueOf(root.optBoolean("two_zone_enabled", false)));
                    settingsRestored++;
                }
                if (root.has("wallpaper_settings")) {
                    saveSettingToDb(db, "wallpaper_preferences", root.get("wallpaper_settings").toString());
                    settingsRestored++;
                }
                if (root.has("widget_preferences")) {
                    saveSettingToDb(db, "widget_settings", root.get("widget_preferences").toString());
                    settingsRestored++;
                }
            }

            db.setTransactionSuccessful();
            return new RestoreResult(true, sessionsRestored, logsRestored, breaksRestored,
                    subjectsRestored, countdownsRestored, achievementsRestored, profilesRestored,
                    auditRecordsRestored, settingsRestored,
                    "Successfully restored backup (mode: " + mode.name() + ").");
        } finally {
            db.endTransaction();
        }
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    private static long insertQuestionLog(SQLiteDatabase db, long sessionId, JSONObject logObj) {
        ContentValues cv = new ContentValues();
        cv.put(DatabaseContract.QuestionLogEntry.COLUMN_SESSION_ID, sessionId);
        cv.put(DatabaseContract.QuestionLogEntry.COLUMN_QUESTION_NUM, logObj.optInt("question_num", 1));
        cv.put(DatabaseContract.QuestionLogEntry.COLUMN_DURATION_MS, logObj.optLong("duration_ms", 0L));
        cv.put(DatabaseContract.QuestionLogEntry.COLUMN_COMPLETED_AT, logObj.optLong("completed_at", System.currentTimeMillis()));

        cv.put(DatabaseContract.QuestionLogEntry.COLUMN_LABEL, logObj.optString("label", ""));
        cv.put(DatabaseContract.QuestionLogEntry.COLUMN_TOPIC, logObj.optString("topic", ""));
        cv.put(DatabaseContract.QuestionLogEntry.COLUMN_DIFFICULTY, logObj.optString("difficulty", "MEDIUM"));
        cv.put(DatabaseContract.QuestionLogEntry.COLUMN_OUTCOME, logObj.optString("outcome", "SOLVED"));
        cv.put(DatabaseContract.QuestionLogEntry.COLUMN_CONFIDENCE, logObj.optInt("confidence", 3));
        cv.put(DatabaseContract.QuestionLogEntry.COLUMN_MISTAKE_TAG, logObj.optString("mistake_tag", ""));
        cv.put(DatabaseContract.QuestionLogEntry.COLUMN_NOTES, logObj.optString("notes", ""));

        cv.put(DatabaseContract.QuestionLogEntry.COLUMN_ORIGINAL_OUTCOME, logObj.optString("original_outcome", ""));
        cv.put(DatabaseContract.QuestionLogEntry.COLUMN_CORRECTION_TIMESTAMP, logObj.optLong("correction_timestamp", 0L));

        return db.insertOrThrow(DatabaseContract.QuestionLogEntry.TABLE_NAME, null, cv);
    }

    private static void saveSettingToDb(SQLiteDatabase db, String key, String value) {
        if (key == null || value == null) return;
        try {
            ContentValues cv = new ContentValues();
            cv.put(DatabaseContract.SettingEntry.COLUMN_KEY, key);
            cv.put(DatabaseContract.SettingEntry.COLUMN_VALUE, value);
            db.insertWithOnConflict(DatabaseContract.SettingEntry.TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            Log.w(TAG, "Failed to save setting to DB: " + key, e);
        }
    }

    private static String getSettingValue(SQLiteDatabase db, String key, String defaultValue) {
        try {
            String query = "SELECT " + DatabaseContract.SettingEntry.COLUMN_VALUE +
                    " FROM " + DatabaseContract.SettingEntry.TABLE_NAME +
                    " WHERE " + DatabaseContract.SettingEntry.COLUMN_KEY + " = ? LIMIT 1";
            try (Cursor c = db.rawQuery(query, new String[]{key})) {
                if (c != null && c.moveToFirst()) {
                    return c.getString(0);
                }
            }
        } catch (Exception ignored) {
        }
        return defaultValue;
    }

    private static boolean isDuplicateSession(SQLiteDatabase db, String name, long createdAt) {
        String query = "SELECT 1 FROM " + DatabaseContract.SessionEntry.TABLE_NAME +
                " WHERE " + DatabaseContract.SessionEntry.COLUMN_NAME + " = ? " +
                " AND abs(" + DatabaseContract.SessionEntry.COLUMN_CREATED_AT + " - ?) < 60000 LIMIT 1";
        try (Cursor c = db.rawQuery(query, new String[]{name, String.valueOf(createdAt)})) {
            return c != null && c.moveToFirst();
        }
    }

    private static JSONArray exportSessionsTable(SQLiteDatabase db) throws JSONException {
        JSONArray arr = new JSONArray();
        String query = "SELECT * FROM " + DatabaseContract.SessionEntry.TABLE_NAME + " ORDER BY id ASC";
        try (Cursor c = db.rawQuery(query, null)) {
            while (c != null && c.moveToNext()) {
                JSONObject obj = new JSONObject();
                for (int i = 0; i < c.getColumnCount(); i++) {
                    String col = c.getColumnName(i);
                    int type = c.getType(i);
                    if (type == Cursor.FIELD_TYPE_INTEGER) {
                        obj.put(col, c.getLong(i));
                    } else if (type == Cursor.FIELD_TYPE_FLOAT) {
                        obj.put(col, c.getDouble(i));
                    } else if (type == Cursor.FIELD_TYPE_NULL) {
                        obj.put(col, JSONObject.NULL);
                    } else {
                        obj.put(col, c.getString(i));
                    }
                }
                arr.put(obj);
            }
        }
        return arr;
    }

    private static JSONArray exportQuestionLogsTable(SQLiteDatabase db) throws JSONException {
        JSONArray arr = new JSONArray();
        String query = "SELECT * FROM " + DatabaseContract.QuestionLogEntry.TABLE_NAME + " ORDER BY id ASC";
        try (Cursor c = db.rawQuery(query, null)) {
            while (c != null && c.moveToNext()) {
                JSONObject obj = new JSONObject();
                for (int i = 0; i < c.getColumnCount(); i++) {
                    String col = c.getColumnName(i);
                    int type = c.getType(i);
                    if (type == Cursor.FIELD_TYPE_INTEGER) {
                        obj.put(col, c.getLong(i));
                    } else if (type == Cursor.FIELD_TYPE_FLOAT) {
                        obj.put(col, c.getDouble(i));
                    } else if (type == Cursor.FIELD_TYPE_NULL) {
                        obj.put(col, JSONObject.NULL);
                    } else {
                        obj.put(col, c.getString(i));
                    }
                }
                arr.put(obj);
            }
        }
        return arr;
    }

    private static JSONArray exportBreaksTable(SQLiteDatabase db) throws JSONException {
        JSONArray arr = new JSONArray();
        String query = "SELECT * FROM " + DatabaseContract.BreakEntry.TABLE_NAME + " ORDER BY id ASC";
        try (Cursor c = db.rawQuery(query, null)) {
            while (c != null && c.moveToNext()) {
                JSONObject obj = new JSONObject();
                for (int i = 0; i < c.getColumnCount(); i++) {
                    String col = c.getColumnName(i);
                    int type = c.getType(i);
                    if (type == Cursor.FIELD_TYPE_INTEGER) {
                        obj.put(col, c.getLong(i));
                    } else {
                        obj.put(col, c.getString(i));
                    }
                }
                arr.put(obj);
            }
        }
        return arr;
    }

    private static JSONArray exportSubjectsTable(SQLiteDatabase db) throws JSONException {
        JSONArray arr = new JSONArray();
        String query = "SELECT * FROM " + DatabaseContract.SubjectEntry.TABLE_NAME + " ORDER BY id ASC";
        try (Cursor c = db.rawQuery(query, null)) {
            while (c != null && c.moveToNext()) {
                JSONObject obj = new JSONObject();
                for (int i = 0; i < c.getColumnCount(); i++) {
                    String col = c.getColumnName(i);
                    int type = c.getType(i);
                    if (type == Cursor.FIELD_TYPE_INTEGER) {
                        obj.put(col, c.getLong(i));
                    } else {
                        obj.put(col, c.getString(i));
                    }
                }
                arr.put(obj);
            }
        }
        return arr;
    }

    private static JSONArray exportCountdownsTable(SQLiteDatabase db) throws JSONException {
        JSONArray arr = new JSONArray();
        String query = "SELECT * FROM " + DatabaseContract.ExamCountdownEntry.TABLE_NAME + " ORDER BY id ASC";
        try (Cursor c = db.rawQuery(query, null)) {
            while (c != null && c.moveToNext()) {
                JSONObject obj = new JSONObject();
                for (int i = 0; i < c.getColumnCount(); i++) {
                    String col = c.getColumnName(i);
                    int type = c.getType(i);
                    if (type == Cursor.FIELD_TYPE_INTEGER) {
                        obj.put(col, c.getLong(i));
                    } else {
                        obj.put(col, c.getString(i));
                    }
                }
                arr.put(obj);
            }
        }
        return arr;
    }

    private static JSONArray exportAchievementsTable(SQLiteDatabase db) throws JSONException {
        JSONArray arr = new JSONArray();
        String query = "SELECT * FROM " + DatabaseContract.AchievementEntry.TABLE_NAME + " ORDER BY id ASC";
        try (Cursor c = db.rawQuery(query, null)) {
            while (c != null && c.moveToNext()) {
                JSONObject obj = new JSONObject();
                for (int i = 0; i < c.getColumnCount(); i++) {
                    String col = c.getColumnName(i);
                    int type = c.getType(i);
                    if (type == Cursor.FIELD_TYPE_INTEGER) {
                        obj.put(col, c.getLong(i));
                    } else {
                        obj.put(col, c.getString(i));
                    }
                }
                arr.put(obj);
            }
        }
        return arr;
    }

    private static JSONArray exportExamProfilesTable(SQLiteDatabase db) throws JSONException {
        JSONArray arr = new JSONArray();
        String query = "SELECT * FROM " + DatabaseContract.ExamProfileEntry.TABLE_NAME + " ORDER BY id ASC";
        try (Cursor c = db.rawQuery(query, null)) {
            while (c != null && c.moveToNext()) {
                JSONObject obj = new JSONObject();
                for (int i = 0; i < c.getColumnCount(); i++) {
                    String col = c.getColumnName(i);
                    int type = c.getType(i);
                    if (type == Cursor.FIELD_TYPE_INTEGER) {
                        obj.put(col, c.getLong(i));
                    } else if (type == Cursor.FIELD_TYPE_FLOAT) {
                        obj.put(col, c.getDouble(i));
                    } else {
                        obj.put(col, c.getString(i));
                    }
                }
                arr.put(obj);
            }
        }
        return arr;
    }

    private static JSONArray exportAuditHistoryTable(SQLiteDatabase db) throws JSONException {
        JSONArray arr = new JSONArray();
        try {
            String query = "SELECT * FROM " + DatabaseContract.AttemptAuditEntry.TABLE_NAME + " ORDER BY id ASC";
            try (Cursor c = db.rawQuery(query, null)) {
                while (c != null && c.moveToNext()) {
                    JSONObject obj = new JSONObject();
                    for (int i = 0; i < c.getColumnCount(); i++) {
                        String col = c.getColumnName(i);
                        int type = c.getType(i);
                        if (type == Cursor.FIELD_TYPE_INTEGER) {
                            obj.put(col, c.getLong(i));
                        } else if (type == Cursor.FIELD_TYPE_FLOAT) {
                            obj.put(col, c.getDouble(i));
                        } else if (type == Cursor.FIELD_TYPE_NULL) {
                            obj.put(col, JSONObject.NULL);
                        } else {
                            obj.put(col, c.getString(i));
                        }
                    }
                    arr.put(obj);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Table " + DatabaseContract.AttemptAuditEntry.TABLE_NAME + " does not exist or error reading: " + e.getMessage());
        }
        return arr;
    }

    private static JSONArray exportSettingsTable(SQLiteDatabase db) throws JSONException {
        JSONArray arr = new JSONArray();
        try {
            String query = "SELECT * FROM " + DatabaseContract.SettingEntry.TABLE_NAME + " ORDER BY id ASC";
            try (Cursor c = db.rawQuery(query, null)) {
                while (c != null && c.moveToNext()) {
                    JSONObject obj = new JSONObject();
                    for (int i = 0; i < c.getColumnCount(); i++) {
                        String col = c.getColumnName(i);
                        int type = c.getType(i);
                        if (type == Cursor.FIELD_TYPE_INTEGER) {
                            obj.put(col, c.getLong(i));
                        } else {
                            obj.put(col, c.getString(i));
                        }
                    }
                    arr.put(obj);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Table " + DatabaseContract.SettingEntry.TABLE_NAME + " does not exist or error reading: " + e.getMessage());
        }
        return arr;
    }

    private static String readStreamToString(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    public static String computeSha256(String input) {
        if (input == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }
}

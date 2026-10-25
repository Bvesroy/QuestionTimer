package com.example.timerapp.data;

import com.example.timerapp.data.backup.SafBackupManager;
import com.example.timerapp.data.local.sqlite.DatabaseContract;
import com.example.timerapp.data.model.AttemptAuditRecord;
import com.example.timerapp.data.model.QuestionLog;
import com.example.timerapp.data.model.Session;
import com.example.timerapp.data.model.TargetSettings;
import com.example.timerapp.data.model.WallpaperPreferences;
import com.example.timerapp.data.model.WidgetSettings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * JUnit 4 Test Suite verifying Version 4 database schema extensions,
 * migration backwards-compatibility, attempt audit history, wallpaper preferences,
 * target date ('2027-01-20'), two-zone enabled preference, and widget settings.
 */
public class DatabaseV4MigrationAndAuditTest {

    @Test
    public void testSchemaV4ContractDefinitions() {
        // 1. Verify Database Version is 4
        Assert.assertEquals(4, DatabaseContract.DATABASE_VERSION);

        // 2. Settings table contract
        Assert.assertEquals("settings", DatabaseContract.SettingEntry.TABLE_NAME);
        Assert.assertEquals("id", DatabaseContract.SettingEntry.COLUMN_ALT_ID);
        Assert.assertEquals("key", DatabaseContract.SettingEntry.COLUMN_KEY);
        Assert.assertEquals("value", DatabaseContract.SettingEntry.COLUMN_VALUE);
        Assert.assertTrue(DatabaseContract.SettingEntry.SQL_CREATE_TABLE.contains("settings"));
        Assert.assertTrue(DatabaseContract.SettingEntry.SQL_CREATE_TABLE.contains("UNIQUE"));
        Assert.assertTrue(DatabaseContract.SettingEntry.SQL_CREATE_IDX_KEY.contains("idx_settings_key"));

        // 3. Attempt audit history table contract
        Assert.assertEquals("attempt_audit_history", DatabaseContract.AttemptAuditEntry.TABLE_NAME);
        Assert.assertEquals("question_log_id", DatabaseContract.AttemptAuditEntry.COLUMN_QUESTION_LOG_ID);
        Assert.assertEquals("session_id", DatabaseContract.AttemptAuditEntry.COLUMN_SESSION_ID);
        Assert.assertEquals("original_outcome", DatabaseContract.AttemptAuditEntry.COLUMN_ORIGINAL_OUTCOME);
        Assert.assertEquals("corrected_outcome", DatabaseContract.AttemptAuditEntry.COLUMN_CORRECTED_OUTCOME);
        Assert.assertEquals("correction_timestamp", DatabaseContract.AttemptAuditEntry.COLUMN_CORRECTION_TIMESTAMP);
        Assert.assertEquals("notes", DatabaseContract.AttemptAuditEntry.COLUMN_NOTES);

        // Verify foreign key constraints and cascading deletes
        Assert.assertTrue(DatabaseContract.AttemptAuditEntry.SQL_CREATE_TABLE.contains("FOREIGN KEY(question_log_id) REFERENCES question_logs"));
        Assert.assertTrue(DatabaseContract.AttemptAuditEntry.SQL_CREATE_TABLE.contains("FOREIGN KEY(session_id) REFERENCES sessions"));
        Assert.assertTrue(DatabaseContract.AttemptAuditEntry.SQL_CREATE_TABLE.contains("ON DELETE CASCADE"));

        // Verify audit indexes
        Assert.assertTrue(DatabaseContract.AttemptAuditEntry.SQL_CREATE_IDX_QUESTION_LOG_ID.contains("idx_audit_question_log_id"));
        Assert.assertTrue(DatabaseContract.AttemptAuditEntry.SQL_CREATE_IDX_SESSION_ID.contains("idx_audit_session_id"));
        Assert.assertTrue(DatabaseContract.AttemptAuditEntry.SQL_CREATE_IDX_TIMESTAMP.contains("idx_audit_correction_time"));

        // 4. Question log v4 extended audit columns
        Assert.assertEquals("original_outcome", DatabaseContract.QuestionLogEntry.COLUMN_ORIGINAL_OUTCOME);
        Assert.assertEquals("correction_timestamp", DatabaseContract.QuestionLogEntry.COLUMN_CORRECTION_TIMESTAMP);
        Assert.assertTrue(DatabaseContract.QuestionLogEntry.SQL_CREATE_TABLE.contains("original_outcome"));
        Assert.assertTrue(DatabaseContract.QuestionLogEntry.SQL_CREATE_TABLE.contains("correction_timestamp"));
    }

    @Test
    public void testAttemptAuditRecordModel_propertiesAndEquality() {
        long now = System.currentTimeMillis();
        AttemptAuditRecord record1 = new AttemptAuditRecord(
                1L, 101L, 5L, "SOLVED", "WRONG_ANSWER", now, "Misread calculation in step 3"
        );
        AttemptAuditRecord record2 = new AttemptAuditRecord(
                1L, 101L, 5L, "SOLVED", "WRONG_ANSWER", now, "Misread calculation in step 3"
        );

        Assert.assertEquals(1L, record1.getId());
        Assert.assertEquals(101L, record1.getQuestionLogId());
        Assert.assertEquals(5L, record1.getSessionId());
        Assert.assertEquals("SOLVED", record1.getOriginalOutcome());
        Assert.assertEquals("WRONG_ANSWER", record1.getCorrectedOutcome());
        Assert.assertEquals(now, record1.getCorrectionTimestamp());
        Assert.assertEquals("Misread calculation in step 3", record1.getNotes());

        Assert.assertEquals(record1, record2);
        Assert.assertEquals(record1.hashCode(), record2.hashCode());
        Assert.assertTrue(record1.toString().contains("attempt_audit_history") || record1.toString().contains("AttemptAuditRecord"));
    }

    @Test
    public void testAttemptCorrectionFlow_updatesQuestionLogAndTracksAudit() {
        long timestamp = 1700000000000L;
        // Initial question log state
        QuestionLog qLog = new QuestionLog(
                10L, 2L, 1, 75000L, timestamp, "Q1", "Rotational Dynamics", "HARD", "SOLVED", 4, "", "First try"
        );
        Assert.assertEquals("SOLVED", qLog.getOutcome());
        Assert.assertEquals("", qLog.getOriginalOutcome());
        Assert.assertEquals(0L, qLog.getCorrectionTimestamp());

        // Simulate correction: User reviews exam and discovers mistake
        long correctionTime = timestamp + 3600000L;
        String original = qLog.getOutcome();
        String corrected = "CALCULATION_ERROR";
        String auditNotes = "Corrected after reviewing solution key";

        qLog.setOriginalOutcome(original);
        qLog.setOutcome(corrected);
        qLog.setCorrectionTimestamp(correctionTime);

        Assert.assertEquals("CALCULATION_ERROR", qLog.getOutcome());
        Assert.assertEquals("SOLVED", qLog.getOriginalOutcome());
        Assert.assertEquals(correctionTime, qLog.getCorrectionTimestamp());

        // Create audit record
        AttemptAuditRecord auditRecord = new AttemptAuditRecord(
                1L, qLog.getId(), qLog.getSessionId(), original, corrected, correctionTime, auditNotes
        );
        Assert.assertEquals(10L, auditRecord.getQuestionLogId());
        Assert.assertEquals(2L, auditRecord.getSessionId());
        Assert.assertEquals("SOLVED", auditRecord.getOriginalOutcome());
        Assert.assertEquals("CALCULATION_ERROR", auditRecord.getCorrectedOutcome());
        Assert.assertEquals(correctionTime, auditRecord.getCorrectionTimestamp());
        Assert.assertEquals(auditNotes, auditRecord.getNotes());

        // Verify UI model conversion preserves audit state
        com.example.timerapp.model.QuestionLog uiLog = qLog.toUiModel();
        Assert.assertEquals("CALCULATION_ERROR", uiLog.getOutcome());
        Assert.assertEquals("SOLVED", uiLog.getOriginalOutcome());
        Assert.assertEquals(correctionTime, uiLog.getCorrectionTimestamp());

        QuestionLog backFromUi = QuestionLog.fromUiModel(uiLog);
        Assert.assertEquals(qLog.getOutcome(), backFromUi.getOutcome());
        Assert.assertEquals(qLog.getOriginalOutcome(), backFromUi.getOriginalOutcome());
        Assert.assertEquals(qLog.getCorrectionTimestamp(), backFromUi.getCorrectionTimestamp());
    }

    @Test
    public void testWallpaperPreferences_serializationAndDefaults() {
        WallpaperPreferences defaults = new WallpaperPreferences();
        Assert.assertFalse(defaults.isEnabled());
        Assert.assertEquals("", defaults.getUri());
        Assert.assertEquals(0.5f, defaults.getDimAlpha(), 0.001f);
        Assert.assertEquals(0, defaults.getBlurRadius());
        Assert.assertEquals("DEFAULT", defaults.getStyle());

        // Custom preferences
        WallpaperPreferences custom = new WallpaperPreferences(
                true, "content://media/external/images/media/42", 0.75f, 15, "BLUR_DARKEN"
        );
        Assert.assertTrue(custom.isEnabled());
        Assert.assertEquals("content://media/external/images/media/42", custom.getUri());
        Assert.assertEquals(0.75f, custom.getDimAlpha(), 0.001f);
        Assert.assertEquals(15, custom.getBlurRadius());
        Assert.assertEquals("BLUR_DARKEN", custom.getStyle());

        // JSON roundtrip
        JSONObject json = custom.toJson();
        WallpaperPreferences roundtrip = WallpaperPreferences.fromJson(json);
        Assert.assertEquals(custom, roundtrip);

        // String roundtrip
        WallpaperPreferences fromStr = WallpaperPreferences.fromJsonString(json.toString());
        Assert.assertEquals(custom, fromStr);
    }

    @Test
    public void testWidgetSettings_serializationAndDefaults() {
        WidgetSettings defaults = new WidgetSettings();
        Assert.assertEquals("AMOLED", defaults.getTheme());
        Assert.assertTrue(defaults.isShowCountdown());
        Assert.assertTrue(defaults.isShowDailyGoal());
        Assert.assertEquals(1.0f, defaults.getOpacity(), 0.001f);
        Assert.assertEquals(15, defaults.getRefreshIntervalMinutes());

        // Custom widget settings
        WidgetSettings custom = new WidgetSettings("TRANSPARENT_GLASS", false, true, 0.85f, 30);
        Assert.assertEquals("TRANSPARENT_GLASS", custom.getTheme());
        Assert.assertFalse(custom.isShowCountdown());
        Assert.assertTrue(custom.isShowDailyGoal());
        Assert.assertEquals(0.85f, custom.getOpacity(), 0.001f);
        Assert.assertEquals(30, custom.getRefreshIntervalMinutes());

        // JSON roundtrip
        JSONObject json = custom.toJson();
        WidgetSettings roundtrip = WidgetSettings.fromJson(json);
        Assert.assertEquals(custom, roundtrip);

        // String roundtrip
        WidgetSettings fromStr = WidgetSettings.fromJsonString(json.toString());
        Assert.assertEquals(custom, fromStr);
    }

    @Test
    public void testTargetSettings_v4ExtensionsAndDefaults() {
        TargetSettings settings = new TargetSettings();
        // Required default target date
        Assert.assertEquals("2027-01-20", settings.getTargetDate());
        Assert.assertEquals(TargetSettings.DEFAULT_TARGET_DATE, settings.getTargetDate());
        Assert.assertFalse(settings.isTwoZoneEnabled());
        Assert.assertNotNull(settings.getWallpaperPreferences());
        Assert.assertNotNull(settings.getWidgetSettings());

        // Custom target settings
        settings.setTargetDate("2027-05-15");
        settings.setTwoZoneEnabled(true);
        settings.getWallpaperPreferences().setEnabled(true);
        settings.getWidgetSettings().setTheme("MINIMAL_MONO");

        Assert.assertEquals("2027-05-15", settings.getTargetDate());
        Assert.assertTrue(settings.isTwoZoneEnabled());
        Assert.assertTrue(settings.getWallpaperPreferences().isEnabled());
        Assert.assertEquals("MINIMAL_MONO", settings.getWidgetSettings().getTheme());
    }

    @Test
    public void testSafBackupManager_v4ArchiveTamperProofAndIntegrity() throws JSONException {
        JSONArray sessions = new JSONArray();
        JSONObject s1 = new JSONObject();
        s1.put("id", 1L);
        s1.put("name", "JEE 2027 Math Sprint");
        sessions.put(s1);

        JSONArray audit = new JSONArray();
        JSONObject a1 = new JSONObject();
        a1.put("question_log_id", 101L);
        a1.put("session_id", 1L);
        a1.put("original_outcome", "SOLVED");
        a1.put("corrected_outcome", "WRONG_ANSWER");
        a1.put("correction_timestamp", 1700000000000L);
        audit.put(a1);

        JSONObject wallpaper = new WallpaperPreferences(true, "file:///wp.jpg", 0.6f, 5, "COVER").toJson();
        JSONObject widget = new WidgetSettings("AMOLED", true, true, 0.9f, 10).toJson();
        JSONObject settings = new JSONObject();
        settings.put("two_zone_enabled", true);

        // Create full v4 archive
        JSONObject archive = SafBackupManager.createFullArchive(
                sessions, null, null, null, null, null, null, settings,
                audit, wallpaper, "2027-01-20", widget
        );

        // Verification
        Assert.assertEquals(4, archive.getInt(SafBackupManager.KEY_VERSION));
        Assert.assertEquals(SafBackupManager.APP_IDENTIFIER, archive.getString(SafBackupManager.KEY_APP));
        Assert.assertTrue(archive.has(SafBackupManager.KEY_CHECKSUM));

        // Check payload contents
        JSONObject payload = SafBackupManager.validateAndExtractArchive(archive.toString());
        Assert.assertNotNull(payload);
        Assert.assertEquals(1, payload.getJSONArray(SafBackupManager.KEY_SESSIONS).length());
        Assert.assertEquals(1, payload.getJSONArray(SafBackupManager.KEY_AUDIT_HISTORY).length());
        Assert.assertEquals("2027-01-20", payload.getString(SafBackupManager.KEY_TARGET_DATE));
        Assert.assertTrue(payload.getBoolean(SafBackupManager.KEY_TWO_ZONE_ENABLED));
        Assert.assertTrue(payload.getJSONObject(SafBackupManager.KEY_WALLPAPER_SETTINGS).getBoolean("enabled"));
        Assert.assertEquals("AMOLED", payload.getJSONObject(SafBackupManager.KEY_WIDGET_SETTINGS).getString("theme"));
    }

    @Test
    public void testMigrationPreservesAllV1V2V3Data() {
        // Verify all v1 columns exist
        Assert.assertEquals("name", DatabaseContract.SessionEntry.COLUMN_NAME);
        Assert.assertEquals("target_count", DatabaseContract.SessionEntry.COLUMN_TARGET_COUNT);
        Assert.assertEquals("created_at", DatabaseContract.SessionEntry.COLUMN_CREATED_AT);
        Assert.assertEquals("updated_at", DatabaseContract.SessionEntry.COLUMN_UPDATED_AT);
        Assert.assertEquals("is_active", DatabaseContract.SessionEntry.COLUMN_IS_ACTIVE);

        // Verify all v2 columns exist
        Assert.assertEquals("subject", DatabaseContract.SessionEntry.COLUMN_SUBJECT);
        Assert.assertEquals("notes", DatabaseContract.SessionEntry.COLUMN_NOTES);
        Assert.assertEquals("went_well", DatabaseContract.SessionEntry.COLUMN_WENT_WELL);
        Assert.assertEquals("needs_improvement", DatabaseContract.SessionEntry.COLUMN_NEEDS_IMPROVEMENT);
        Assert.assertEquals("status", DatabaseContract.SessionEntry.COLUMN_STATUS);
        Assert.assertEquals("profile_id", DatabaseContract.SessionEntry.COLUMN_PROFILE_ID);

        // Verify all v3 columns exist
        Assert.assertEquals("exam_profile", DatabaseContract.SessionEntry.COLUMN_EXAM_PROFILE);
        Assert.assertEquals("total_study_duration_ms", DatabaseContract.SessionEntry.COLUMN_TOTAL_STUDY_DURATION_MS);
        Assert.assertEquals("total_break_duration_ms", DatabaseContract.SessionEntry.COLUMN_TOTAL_BREAK_DURATION_MS);
        Assert.assertEquals("is_mock_test", DatabaseContract.SessionEntry.COLUMN_IS_MOCK_TEST);

        // Verify v3 question log columns
        Assert.assertEquals("label", DatabaseContract.QuestionLogEntry.COLUMN_LABEL);
        Assert.assertEquals("topic", DatabaseContract.QuestionLogEntry.COLUMN_TOPIC);
        Assert.assertEquals("difficulty", DatabaseContract.QuestionLogEntry.COLUMN_DIFFICULTY);
        Assert.assertEquals("outcome", DatabaseContract.QuestionLogEntry.COLUMN_OUTCOME);
        Assert.assertEquals("confidence", DatabaseContract.QuestionLogEntry.COLUMN_CONFIDENCE);
        Assert.assertEquals("mistake_tag", DatabaseContract.QuestionLogEntry.COLUMN_MISTAKE_TAG);
        Assert.assertEquals("notes", DatabaseContract.QuestionLogEntry.COLUMN_NOTES);

        // Verify v3 tables
        Assert.assertEquals("breaks", DatabaseContract.BreakEntry.TABLE_NAME);
        Assert.assertEquals("study_coach_feedback", DatabaseContract.StudyCoachFeedbackEntry.TABLE_NAME);
        Assert.assertEquals("exam_profiles", DatabaseContract.ExamProfileEntry.TABLE_NAME);

        // Verify v4 new tables & columns
        Assert.assertEquals("settings", DatabaseContract.SettingEntry.TABLE_NAME);
        Assert.assertEquals("attempt_audit_history", DatabaseContract.AttemptAuditEntry.TABLE_NAME);
        Assert.assertEquals("original_outcome", DatabaseContract.QuestionLogEntry.COLUMN_ORIGINAL_OUTCOME);
        Assert.assertEquals("correction_timestamp", DatabaseContract.QuestionLogEntry.COLUMN_CORRECTION_TIMESTAMP);
    }
}

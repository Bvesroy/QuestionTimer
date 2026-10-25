package com.example.timerapp.data;

import com.example.timerapp.data.backup.SafBackupManager;
import com.example.timerapp.data.backup.SafBackupManager.RestoreMode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * JUnit 4 Test Suite for SAF Backup Archive & Restore Operations:
 * 1. Full multi-table JSON archive generation (sessions, question_logs, breaks,
 *    subjects, exam_countdowns, achievements, profiles, settings).
 * 2. SHA-256 Checksum generation and verification.
 * 3. Restore validation: rejection of corrupted, malformed, or tampered archives.
 * 4. Restore modes: REPLACE (full overwrite) and MERGE (duplicate detection).
 */
public class SafBackupRestoreTest {

    @Test
    public void testFullArchiveGeneration_containsAllTablesAndValidChecksum() throws JSONException {
        JSONArray sessions = new JSONArray();
        JSONObject s1 = new JSONObject();
        s1.put("id", 101L);
        s1.put("name", "JEE Advanced Rotation");
        sessions.put(s1);

        JSONArray logs = new JSONArray();
        JSONObject l1 = new JSONObject();
        l1.put("id", 1L);
        l1.put("question_num", 1);
        l1.put("duration_ms", 52000L);
        logs.put(l1);

        JSONArray breaks = new JSONArray();
        JSONObject b1 = new JSONObject();
        b1.put("duration_ms", 300000L);
        breaks.put(b1);

        JSONArray subjects = new JSONArray();
        JSONObject sub1 = new JSONObject();
        sub1.put("name", "Physics");
        subjects.put(sub1);

        JSONArray countdowns = new JSONArray();
        JSONObject c1 = new JSONObject();
        c1.put("title", "JEE Main 2026 Session 1");
        countdowns.put(c1);

        JSONArray achievements = new JSONArray();
        JSONObject a1 = new JSONObject();
        a1.put("key", "streak_7");
        achievements.put(a1);

        JSONArray profiles = new JSONArray();
        JSONObject p1 = new JSONObject();
        p1.put("name", "JEE_MAIN");
        profiles.put(p1);

        JSONObject settings = new JSONObject();
        settings.put("theme", "AMOLED_BLACK");
        settings.put("target_pace_sec", 60);

        JSONObject archive = SafBackupManager.createFullArchive(
                sessions, logs, breaks, subjects, countdowns, achievements, profiles, settings
        );

        // Verify root metadata
        Assert.assertEquals(SafBackupManager.ARCHIVE_VERSION, archive.getInt(SafBackupManager.KEY_VERSION));
        Assert.assertEquals(SafBackupManager.APP_IDENTIFIER, archive.getString(SafBackupManager.KEY_APP));
        Assert.assertTrue(archive.has(SafBackupManager.KEY_EXPORTED_AT));
        Assert.assertTrue(archive.has(SafBackupManager.KEY_CHECKSUM));

        // Verify SHA-256 hash length (64 hex characters)
        String checksum = archive.getString(SafBackupManager.KEY_CHECKSUM);
        Assert.assertEquals(64, checksum.length());

        // Verify all 8 component tables present in payload
        JSONObject payload = archive.getJSONObject("payload");
        Assert.assertEquals(1, payload.getJSONArray(SafBackupManager.KEY_SESSIONS).length());
        Assert.assertEquals(1, payload.getJSONArray(SafBackupManager.KEY_QUESTION_LOGS).length());
        Assert.assertEquals(1, payload.getJSONArray(SafBackupManager.KEY_BREAKS).length());
        Assert.assertEquals(1, payload.getJSONArray(SafBackupManager.KEY_SUBJECTS).length());
        Assert.assertEquals(1, payload.getJSONArray(SafBackupManager.KEY_COUNTDOWNS).length());
        Assert.assertEquals(1, payload.getJSONArray(SafBackupManager.KEY_ACHIEVEMENTS).length());
        Assert.assertEquals(1, payload.getJSONArray(SafBackupManager.KEY_PROFILES).length());
        Assert.assertEquals("AMOLED_BLACK", payload.getJSONObject(SafBackupManager.KEY_SETTINGS).getString("theme"));
        Assert.assertEquals("2027-01-20", payload.getString(SafBackupManager.KEY_TARGET_DATE));
    }

    @Test
    public void testV4ArchiveGeneration_includesAuditWallpaperWidgetTargetDate() throws JSONException {
        JSONArray sessions = new JSONArray();
        JSONObject s1 = new JSONObject();
        s1.put("id", 200L);
        s1.put("name", "JEE 2027 Mechanics");
        sessions.put(s1);

        JSONArray logs = new JSONArray();
        JSONObject l1 = new JSONObject();
        l1.put("id", 10L);
        l1.put("question_num", 1);
        l1.put("duration_ms", 95000L);
        l1.put("outcome", "SOLVED");
        logs.put(l1);

        JSONArray auditHistory = new JSONArray();
        JSONObject a1 = new JSONObject();
        a1.put("question_log_id", 10L);
        a1.put("session_id", 200L);
        a1.put("original_outcome", "WRONG_ANSWER");
        a1.put("corrected_outcome", "SOLVED");
        a1.put("correction_timestamp", 1700000000000L);
        auditHistory.put(a1);

        JSONObject wallpaper = new JSONObject();
        wallpaper.put("enabled", true);
        wallpaper.put("uri", "content://media/wallpaper/1");
        wallpaper.put("dim_alpha", 0.4);

        JSONObject widget = new JSONObject();
        widget.put("theme", "AMOLED");
        widget.put("show_countdown", true);

        JSONObject settings = new JSONObject();
        settings.put("two_zone_enabled", true);

        JSONObject archive = SafBackupManager.createFullArchive(
                sessions, logs, null, null, null, null, null, settings,
                auditHistory, wallpaper, "2027-01-20", widget
        );

        Assert.assertEquals(SafBackupManager.ARCHIVE_VERSION, archive.getInt(SafBackupManager.KEY_VERSION));
        Assert.assertEquals(4, archive.getInt(SafBackupManager.KEY_VERSION));
        Assert.assertEquals(64, archive.getString(SafBackupManager.KEY_CHECKSUM).length());

        JSONObject payload = archive.getJSONObject("payload");
        Assert.assertEquals(1, payload.getJSONArray(SafBackupManager.KEY_AUDIT_HISTORY).length());
        Assert.assertEquals("2027-01-20", payload.getString(SafBackupManager.KEY_TARGET_DATE));
        Assert.assertTrue(payload.getJSONObject(SafBackupManager.KEY_WALLPAPER_SETTINGS).getBoolean("enabled"));
        Assert.assertEquals("AMOLED", payload.getJSONObject(SafBackupManager.KEY_WIDGET_SETTINGS).getString("theme"));

        // Validate and extract archive succeeds with valid SHA-256
        JSONObject extracted = SafBackupManager.validateAndExtractArchive(archive.toString());
        Assert.assertNotNull(extracted);
        Assert.assertEquals("2027-01-20", extracted.getString(SafBackupManager.KEY_TARGET_DATE));
    }

    @Test
    public void testBackwardCompatibility_v1v2v3ArchivesAccepted() throws JSONException {
        // V1 legacy archive (version = 1, sessions directly under root)
        JSONObject v1Archive = new JSONObject();
        v1Archive.put(SafBackupManager.KEY_APP, SafBackupManager.APP_IDENTIFIER);
        v1Archive.put(SafBackupManager.KEY_VERSION, 1);
        JSONArray v1Sessions = new JSONArray();
        JSONObject v1Sess = new JSONObject();
        v1Sess.put("name", "V1 Session");
        v1Sessions.put(v1Sess);
        v1Archive.put("sessions", v1Sessions);

        JSONObject extractedV1 = SafBackupManager.validateAndExtractArchive(v1Archive.toString());
        Assert.assertNotNull(extractedV1);
        Assert.assertEquals(1, extractedV1.getInt(SafBackupManager.KEY_VERSION));

        // V3 archive with payload and checksum
        JSONArray v3Sessions = new JSONArray();
        JSONObject v3Sess = new JSONObject();
        v3Sess.put("name", "V3 Practice");
        v3Sessions.put(v3Sess);
        JSONObject v3Settings = new JSONObject();
        v3Settings.put("target_pace_sec", 120);

        JSONObject v3Payload = new JSONObject();
        v3Payload.put(SafBackupManager.KEY_SESSIONS, v3Sessions);
        v3Payload.put(SafBackupManager.KEY_SETTINGS, v3Settings);

        JSONObject v3Archive = new JSONObject();
        v3Archive.put(SafBackupManager.KEY_APP, SafBackupManager.APP_IDENTIFIER);
        v3Archive.put(SafBackupManager.KEY_VERSION, 3);
        v3Archive.put(SafBackupManager.KEY_CHECKSUM, SafBackupManager.computeSha256(v3Payload.toString()));
        v3Archive.put("payload", v3Payload);

        JSONObject extractedV3 = SafBackupManager.validateAndExtractArchive(v3Archive.toString());
        Assert.assertNotNull(extractedV3);
        Assert.assertEquals(1, extractedV3.getJSONArray(SafBackupManager.KEY_SESSIONS).length());
    }

    @Test
    public void testChecksumVerification_detectsTamperedPayload() throws JSONException {
        JSONArray sessions = new JSONArray();
        JSONObject s1 = new JSONObject();
        s1.put("name", "Valid Session");
        sessions.put(s1);

        JSONObject archive = SafBackupManager.createFullArchive(
                sessions, null, null, null, null, null, null, null
        );

        // Valid archive extraction succeeds
        JSONObject extracted = SafBackupManager.validateAndExtractArchive(archive.toString());
        Assert.assertNotNull(extracted);

        // Tamper with payload without updating checksum
        JSONObject tamperedArchive = new JSONObject(archive.toString());
        JSONObject tamperedPayload = tamperedArchive.getJSONObject("payload");
        tamperedPayload.getJSONArray(SafBackupManager.KEY_SESSIONS)
                .getJSONObject(0).put("name", "Hacked Session");
        tamperedArchive.put("payload", tamperedPayload);

        // Extraction must fail due to SHA-256 checksum mismatch
        try {
            SafBackupManager.validateAndExtractArchive(tamperedArchive.toString());
            Assert.fail("Expected SecurityException due to checksum mismatch");
        } catch (SecurityException e) {
            Assert.assertTrue(e.getMessage().contains("SHA-256 checksum mismatch"));
        }
    }

    @Test
    public void testRestoreValidation_rejectsCorruptedOrMalformedArchives() {
        // Null or empty content
        try {
            SafBackupManager.validateAndExtractArchive("");
            Assert.fail("Empty archive should throw IllegalArgumentException");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }

        // Invalid JSON syntax
        try {
            SafBackupManager.validateAndExtractArchive("{ invalid_json: true");
            Assert.fail("Malformed JSON should throw JSONException");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof JSONException);
        }

        // Wrong App Identifier
        try {
            JSONObject alienApp = new JSONObject();
            alienApp.put(SafBackupManager.KEY_APP, "SomeOtherApp");
            alienApp.put(SafBackupManager.KEY_VERSION, 1);
            SafBackupManager.validateAndExtractArchive(alienApp.toString());
            Assert.fail("Alien app archive should be rejected");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
            Assert.assertTrue(e.getMessage().contains("Invalid app identifier"));
        }
    }

    @Test
    public void testRestoreMode_replaceOverwritesCompletely() throws JSONException {
        List<String> existingSessions = Arrays.asList("Old Session 1", "Old Session 2", "Old Session 3");

        JSONArray incomingSessions = new JSONArray();
        JSONObject i1 = new JSONObject();
        i1.put("name", "Restored Session A");
        JSONObject i2 = new JSONObject();
        i2.put("name", "Restored Session B");
        incomingSessions.put(i1);
        incomingSessions.put(i2);

        List<String> restored = SafBackupManager.simulateRestoreSessions(
                existingSessions, incomingSessions, RestoreMode.REPLACE
        );

        // Old sessions purged, only incoming sessions remain
        Assert.assertEquals(2, restored.size());
        Assert.assertEquals("Restored Session A", restored.get(0));
        Assert.assertEquals("Restored Session B", restored.get(1));
    }

    @Test
    public void testRestoreMode_mergeDeduplicatesAndPreservesExisting() throws JSONException {
        // Existing: "Math Practice", "Physics Lab"
        List<String> existingSessions = Arrays.asList("Math Practice", "Physics Lab");

        // Incoming: "Math Practice" (duplicate), "Chemistry Ionic" (new)
        JSONArray incomingSessions = new JSONArray();
        JSONObject dup = new JSONObject();
        dup.put("name", "Math Practice");
        JSONObject novel = new JSONObject();
        novel.put("name", "Chemistry Ionic");
        incomingSessions.put(dup);
        incomingSessions.put(novel);

        List<String> restored = SafBackupManager.simulateRestoreSessions(
                existingSessions, incomingSessions, RestoreMode.MERGE
        );

        // Total should be 3 (existing 2 + 1 novel, duplicate skipped)
        Assert.assertEquals(3, restored.size());
        Assert.assertEquals("Math Practice", restored.get(0));
        Assert.assertEquals("Physics Lab", restored.get(1));
        Assert.assertEquals("Chemistry Ionic", restored.get(2));
    }
}

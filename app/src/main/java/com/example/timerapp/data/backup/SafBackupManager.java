package com.example.timerapp.data.backup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Storage Access Framework (SAF) Compliant JSON Archive Manager.
 * Features:
 * 1. Comprehensive multi-table export (sessions, question_logs, breaks, subjects,
 *    exam_countdowns, achievements, profiles, settings).
 * 2. Cryptographic SHA-256 checksum hashing and verification for tamper detection.
 * 3. Strict structural and cryptographic validation before initiating restoration.
 * 4. Dual restore modes: REPLACE (complete database overwrite) and MERGE (conflict-free duplicate detection).
 */
public class SafBackupManager {

    public static final int ARCHIVE_VERSION = 4;
    public static final String APP_IDENTIFIER = "QuestionSolvingTimer";

    public enum RestoreMode {
        REPLACE,
        MERGE
    }

    public static final String KEY_VERSION = "version";
    public static final String KEY_APP = "app";
    public static final String KEY_EXPORTED_AT = "exported_at";
    public static final String KEY_CHECKSUM = "checksum";

    public static final String KEY_SESSIONS = "sessions";
    public static final String KEY_QUESTION_LOGS = "question_logs";
    public static final String KEY_BREAKS = "breaks";
    public static final String KEY_SUBJECTS = "subjects";
    public static final String KEY_COUNTDOWNS = "exam_countdowns";
    public static final String KEY_ACHIEVEMENTS = "achievements";
    public static final String KEY_PROFILES = "profiles";
    public static final String KEY_SETTINGS = "settings";

    // Version 4 extended keys
    public static final String KEY_AUDIT_HISTORY = "audit_history";
    public static final String KEY_WALLPAPER_SETTINGS = "wallpaper_settings";
    public static final String KEY_TARGET_DATE = "target_date";
    public static final String KEY_WIDGET_SETTINGS = "widget_settings";
    public static final String KEY_TWO_ZONE_ENABLED = "two_zone_enabled";

    /**
     * Constructs a full JSON archive with an embedded SHA-256 checksum computed over all data tables.
     * Preserved for backward-compatibility with v1/v2/v3 callers.
     */
    public static JSONObject createFullArchive(
            JSONArray sessions,
            JSONArray questionLogs,
            JSONArray breaks,
            JSONArray subjects,
            JSONArray countdowns,
            JSONArray achievements,
            JSONArray profiles,
            JSONObject settings) throws JSONException {
        return createFullArchive(sessions, questionLogs, breaks, subjects, countdowns, achievements,
                profiles, settings, new JSONArray(), null, "2027-01-20", null);
    }

    /**
     * Constructs a full Version 4 JSON archive with audit history, wallpaper settings,
     * target date ('2027-01-20'), and widget settings.
     */
    public static JSONObject createFullArchive(
            JSONArray sessions,
            JSONArray questionLogs,
            JSONArray breaks,
            JSONArray subjects,
            JSONArray countdowns,
            JSONArray achievements,
            JSONArray profiles,
            JSONObject settings,
            JSONArray auditHistory,
            JSONObject wallpaperSettings,
            String targetDate,
            JSONObject widgetSettings) throws JSONException {

        JSONObject root = new JSONObject();
        root.put(KEY_VERSION, ARCHIVE_VERSION);
        root.put(KEY_APP, APP_IDENTIFIER);
        root.put(KEY_EXPORTED_AT, System.currentTimeMillis());

        JSONObject dataPayload = new JSONObject();
        dataPayload.put(KEY_SESSIONS, sessions != null ? sessions : new JSONArray());
        dataPayload.put(KEY_QUESTION_LOGS, questionLogs != null ? questionLogs : new JSONArray());
        dataPayload.put(KEY_BREAKS, breaks != null ? breaks : new JSONArray());
        dataPayload.put(KEY_SUBJECTS, subjects != null ? subjects : new JSONArray());
        dataPayload.put(KEY_COUNTDOWNS, countdowns != null ? countdowns : new JSONArray());
        dataPayload.put(KEY_ACHIEVEMENTS, achievements != null ? achievements : new JSONArray());
        dataPayload.put(KEY_PROFILES, profiles != null ? profiles : new JSONArray());
        dataPayload.put(KEY_AUDIT_HISTORY, auditHistory != null ? auditHistory : new JSONArray());

        JSONObject finalSettings = settings != null ? new JSONObject(settings.toString()) : new JSONObject();
        String finalTargetDate = (targetDate != null && !targetDate.trim().isEmpty()) ? targetDate.trim() : "2027-01-20";
        JSONObject finalWallpaper = wallpaperSettings != null ? wallpaperSettings : new JSONObject();
        JSONObject finalWidget = widgetSettings != null ? widgetSettings : new JSONObject();

        if (!finalSettings.has(KEY_TARGET_DATE)) {
            finalSettings.put(KEY_TARGET_DATE, finalTargetDate);
        }
        if (!finalSettings.has(KEY_WALLPAPER_SETTINGS) && finalWallpaper.length() > 0) {
            finalSettings.put(KEY_WALLPAPER_SETTINGS, finalWallpaper);
        }
        if (!finalSettings.has(KEY_WIDGET_SETTINGS) && finalWidget.length() > 0) {
            finalSettings.put(KEY_WIDGET_SETTINGS, finalWidget);
        }
        if (!finalSettings.has(KEY_TWO_ZONE_ENABLED)) {
            finalSettings.put(KEY_TWO_ZONE_ENABLED, false);
        }

        dataPayload.put(KEY_SETTINGS, finalSettings);
        dataPayload.put(KEY_TARGET_DATE, finalTargetDate);
        dataPayload.put(KEY_WALLPAPER_SETTINGS, finalWallpaper);
        dataPayload.put(KEY_WIDGET_SETTINGS, finalWidget);
        dataPayload.put(KEY_TWO_ZONE_ENABLED, finalSettings.optBoolean(KEY_TWO_ZONE_ENABLED, false));

        String payloadString = dataPayload.toString();
        String sha256Checksum = computeSha256(payloadString);

        root.put(KEY_CHECKSUM, sha256Checksum);
        root.put("payload", dataPayload);

        // Also duplicate top-level keys for backward-compatibility with v1/v2/v3 parsers
        root.put(KEY_SESSIONS, dataPayload.get(KEY_SESSIONS));
        root.put(KEY_QUESTION_LOGS, dataPayload.get(KEY_QUESTION_LOGS));
        root.put(KEY_BREAKS, dataPayload.get(KEY_BREAKS));
        root.put(KEY_SUBJECTS, dataPayload.get(KEY_SUBJECTS));
        root.put(KEY_COUNTDOWNS, dataPayload.get(KEY_COUNTDOWNS));
        root.put(KEY_ACHIEVEMENTS, dataPayload.get(KEY_ACHIEVEMENTS));
        root.put(KEY_PROFILES, dataPayload.get(KEY_PROFILES));
        root.put(KEY_SETTINGS, dataPayload.get(KEY_SETTINGS));
        root.put(KEY_AUDIT_HISTORY, dataPayload.get(KEY_AUDIT_HISTORY));
        root.put(KEY_TARGET_DATE, dataPayload.get(KEY_TARGET_DATE));
        root.put(KEY_WALLPAPER_SETTINGS, dataPayload.get(KEY_WALLPAPER_SETTINGS));
        root.put(KEY_WIDGET_SETTINGS, dataPayload.get(KEY_WIDGET_SETTINGS));
        root.put(KEY_TWO_ZONE_ENABLED, dataPayload.get(KEY_TWO_ZONE_ENABLED));

        return root;
    }

    /**
     * Validates JSON archive structure, mandatory keys, and SHA-256 checksum integrity.
     * Throws an IllegalArgumentException or SecurityException if verification fails.
     */
    public static JSONObject validateAndExtractArchive(String archiveJson) throws JSONException {
        if (archiveJson == null || archiveJson.trim().isEmpty()) {
            throw new IllegalArgumentException("Backup archive content cannot be null or empty.");
        }

        JSONObject root = new JSONObject(archiveJson);

        if (!root.has(KEY_APP) || !APP_IDENTIFIER.equals(root.optString(KEY_APP))) {
            throw new IllegalArgumentException("Archive rejected: Invalid app identifier.");
        }

        if (!root.has(KEY_VERSION) || root.getInt(KEY_VERSION) < 1) {
            throw new IllegalArgumentException("Archive rejected: Invalid archive version.");
        }

        if (root.has("payload") && root.has(KEY_CHECKSUM)) {
            String declaredChecksum = root.getString(KEY_CHECKSUM);
            JSONObject payload = root.getJSONObject("payload");
            String actualChecksum = computeSha256(payload.toString());

            if (!declaredChecksum.equalsIgnoreCase(actualChecksum)) {
                throw new SecurityException("Archive rejected: SHA-256 checksum mismatch! Archive has been tampered with or corrupted.");
            }
            return payload;
        }

        return root;
    }

    /**
     * Simulates restoration in either REPLACE or MERGE mode across in-memory collections.
     *
     * @param existingSessionNames List of existing session names.
     * @param incomingSessions     JSONArray of sessions from the backup archive.
     * @param mode                 REPLACE (overwrite) or MERGE (deduplicate).
     * @return List of resulting session names after restore.
     */
    public static List<String> simulateRestoreSessions(
            List<String> existingSessionNames,
            JSONArray incomingSessions,
            RestoreMode mode) throws JSONException {

        List<String> result = new ArrayList<>();
        if (mode == RestoreMode.REPLACE) {
            // Overwrite completely
            for (int i = 0; i < incomingSessions.length(); i++) {
                JSONObject s = incomingSessions.getJSONObject(i);
                result.add(s.getString("name"));
            }
        } else {
            // MERGE mode: preserve existing and add only non-duplicate incoming items
            Set<String> seen = new HashSet<>(existingSessionNames);
            result.addAll(existingSessionNames);

            for (int i = 0; i < incomingSessions.length(); i++) {
                JSONObject s = incomingSessions.getJSONObject(i);
                String name = s.getString("name");
                if (!seen.contains(name)) {
                    seen.add(name);
                    result.add(name);
                }
            }
        }
        return result;
    }

    /**
     * Computes hexadecimal SHA-256 hash string for an input string.
     */
    public static String computeSha256(String input) {
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
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}

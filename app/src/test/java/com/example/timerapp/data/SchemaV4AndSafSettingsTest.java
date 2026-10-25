package com.example.timerapp.data;

import com.example.timerapp.data.backup.SafBackupEngine;
import com.example.timerapp.data.backup.SafBackupManager;
import com.example.timerapp.data.local.sqlite.DatabaseContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

/**
 * Comprehensive Unit Test Suite for Database Schema v4 & SAF Backup/Restore
 * with Wallpaper and Widget settings integration and backward compatibility.
 */
public class SchemaV4AndSafSettingsTest {

    // ==========================================
    // Database Schema v4 Contract Tests
    // ==========================================

    @Test
    public void testSchemaV4ContractDefinitions() {
        // Assert Database Version is 4
        Assert.assertEquals(4, DatabaseContract.DATABASE_VERSION);

        // Verify Schema v4 Session Table Columns
        Assert.assertEquals("wallpaper_path", DatabaseContract.SessionEntry.COLUMN_WALLPAPER_PATH);
        Assert.assertEquals("wallpaper_alpha", DatabaseContract.SessionEntry.COLUMN_WALLPAPER_ALPHA);
        Assert.assertEquals("widget_config_id", DatabaseContract.SessionEntry.COLUMN_WIDGET_CONFIG_ID);

        // Verify SQL Create Table statement includes v4 columns
        String sessionSql = DatabaseContract.SessionEntry.SQL_CREATE_TABLE;
        Assert.assertTrue(sessionSql.contains("wallpaper_path TEXT DEFAULT ''"));
        Assert.assertTrue(sessionSql.contains("wallpaper_alpha REAL DEFAULT 1.0"));
        Assert.assertTrue(sessionSql.contains("widget_config_id TEXT DEFAULT ''"));

        // Verify Widget Config Table Schema
        Assert.assertEquals("widget_configs", DatabaseContract.WidgetConfigEntry.TABLE_NAME);
        Assert.assertEquals("widget_id", DatabaseContract.WidgetConfigEntry.COLUMN_WIDGET_ID);
        Assert.assertEquals("target_exam_name", DatabaseContract.WidgetConfigEntry.COLUMN_TARGET_EXAM_NAME);
        Assert.assertEquals("countdown_days", DatabaseContract.WidgetConfigEntry.COLUMN_COUNTDOWN_DAYS);
        Assert.assertEquals("theme", DatabaseContract.WidgetConfigEntry.COLUMN_THEME);
        Assert.assertEquals("transparency", DatabaseContract.WidgetConfigEntry.COLUMN_TRANSPARENCY);
        Assert.assertEquals("wallpaper_uri", DatabaseContract.WidgetConfigEntry.COLUMN_WALLPAPER_URI);
        Assert.assertEquals("refresh_interval_sec", DatabaseContract.WidgetConfigEntry.COLUMN_REFRESH_INTERVAL_SEC);
        Assert.assertEquals("is_active", DatabaseContract.WidgetConfigEntry.COLUMN_IS_ACTIVE);

        String widgetSql = DatabaseContract.WidgetConfigEntry.SQL_CREATE_TABLE;
        Assert.assertTrue(widgetSql.contains("widget_configs"));
        Assert.assertTrue(widgetSql.contains("widget_id TEXT UNIQUE NOT NULL"));
        Assert.assertTrue(widgetSql.contains("transparency REAL DEFAULT 0.85"));
    }

    // ==========================================
    // SAF Backup/Restore v4 with Wallpaper & Widgets
    // ==========================================

    @Test
    public void testSafBackupManager_createsFullV4ArchiveWithWallpaperAndWidgetSettings() throws JSONException {
        Assert.assertEquals(4, SafBackupManager.ARCHIVE_VERSION);

        JSONArray sessions = new JSONArray();
        JSONObject s1 = new JSONObject();
        s1.put("id", 101L);
        s1.put("name", "JEE Advanced Rotation");
        s1.put("wallpaper_path", "/storage/emulated/0/Pictures/galaxy_amoled.png");
        s1.put("wallpaper_alpha", 0.75);
        sessions.put(s1);

        JSONObject wallpaperSettings = new JSONObject();
        wallpaperSettings.put("wallpaper_uri", "content://media/external/images/media/42");
        wallpaperSettings.put("dim_percentage", 30);
        wallpaperSettings.put("blur_radius", 5);
        wallpaperSettings.put("is_enabled", true);

        JSONObject widgetSettings = new JSONObject();
        widgetSettings.put("widget_id", "countdown_widget_01");
        widgetSettings.put("target_exam", "JEE MAIN 2027");
        widgetSettings.put("target_date", "2027-01-20");
        widgetSettings.put("theme", "AMOLED_BLACK");
        widgetSettings.put("transparency", 0.85);
        widgetSettings.put("refresh_interval_seconds", 60);

        JSONObject settings = new JSONObject();
        settings.put("theme", "AMOLED_BLACK");
        settings.put("timer_size_mode", 2);

        JSONObject archive = SafBackupManager.createFullArchive(
                sessions, null, null, null, null, null, null,
                settings, null, wallpaperSettings, "2027-01-20", widgetSettings
        );

        // Verify root metadata
        Assert.assertEquals(4, archive.getInt(SafBackupManager.KEY_VERSION));
        Assert.assertEquals("QuestionSolvingTimer", archive.getString(SafBackupManager.KEY_APP));
        Assert.assertTrue(archive.has(SafBackupManager.KEY_CHECKSUM));
        Assert.assertEquals(64, archive.getString(SafBackupManager.KEY_CHECKSUM).length());

        // Verify payload contents
        JSONObject payload = archive.getJSONObject("payload");
        Assert.assertEquals(1, payload.getJSONArray(SafBackupManager.KEY_SESSIONS).length());

        // Verify Wallpaper Settings in payload
        Assert.assertTrue(payload.has(SafBackupManager.KEY_WALLPAPER_SETTINGS));
        JSONObject payloadWp = payload.getJSONObject(SafBackupManager.KEY_WALLPAPER_SETTINGS);
        Assert.assertEquals("content://media/external/images/media/42", payloadWp.getString("wallpaper_uri"));
        Assert.assertEquals(30, payloadWp.getInt("dim_percentage"));
        Assert.assertTrue(payloadWp.getBoolean("is_enabled"));

        // Verify Widget Settings in payload
        Assert.assertTrue(payload.has(SafBackupManager.KEY_WIDGET_SETTINGS));
        JSONObject payloadWidget = payload.getJSONObject(SafBackupManager.KEY_WIDGET_SETTINGS);
        Assert.assertEquals("countdown_widget_01", payloadWidget.getString("widget_id"));
        Assert.assertEquals("JEE MAIN 2027", payloadWidget.getString("target_exam"));
        Assert.assertEquals("2027-01-20", payloadWidget.getString("target_date"));
        Assert.assertEquals(0.85, payloadWidget.getDouble("transparency"), 0.001);

        // Verify Target Date field
        Assert.assertTrue(payload.has(SafBackupManager.KEY_TARGET_DATE));
        Assert.assertEquals("2027-01-20", payload.getString(SafBackupManager.KEY_TARGET_DATE));
    }

    @Test
    public void testSafBackupManager_detectsTamperedWallpaperOrWidgetSettings() throws JSONException {
        JSONObject wallpaperSettings = new JSONObject();
        wallpaperSettings.put("wallpaper_uri", "content://media/original.png");
        wallpaperSettings.put("dim_percentage", 40);

        JSONObject widgetSettings = new JSONObject();
        widgetSettings.put("theme", "DARK_SLATE");

        JSONObject archive = SafBackupManager.createFullArchive(
                null, null, null, null, null, null, null,
                null, null, wallpaperSettings, "2027-01-20", widgetSettings
        );

        // Valid archive validates successfully
        JSONObject extracted = SafBackupManager.validateAndExtractArchive(archive.toString());
        Assert.assertNotNull(extracted);

        // Maliciously tamper with wallpaper settings in payload without updating SHA-256
        JSONObject tamperedArchive = new JSONObject(archive.toString());
        JSONObject tamperedPayload = tamperedArchive.getJSONObject("payload");
        JSONObject tamperedWp = tamperedPayload.getJSONObject(SafBackupManager.KEY_WALLPAPER_SETTINGS);
        tamperedWp.put("wallpaper_uri", "content://media/tampered_exploit.png");
        tamperedPayload.put(SafBackupManager.KEY_WALLPAPER_SETTINGS, tamperedWp);
        tamperedArchive.put("payload", tamperedPayload);

        // Validation must reject tampered archive
        try {
            SafBackupManager.validateAndExtractArchive(tamperedArchive.toString());
            Assert.fail("Expected SecurityException due to SHA-256 checksum mismatch on tampered wallpaper settings");
        } catch (SecurityException e) {
            Assert.assertTrue(e.getMessage().contains("SHA-256 checksum mismatch"));
        }
    }

    @Test
    public void testSafBackupManager_backwardCompatibilityWithVersion3Archives() throws JSONException {
        // Construct legacy v3 archive
        JSONObject v3Archive = new JSONObject();
        v3Archive.put(SafBackupManager.KEY_VERSION, 3);
        v3Archive.put(SafBackupManager.KEY_APP, SafBackupManager.APP_IDENTIFIER);
        v3Archive.put(SafBackupManager.KEY_EXPORTED_AT, System.currentTimeMillis());

        JSONArray sessions = new JSONArray();
        JSONObject s = new JSONObject();
        s.put("id", 1L);
        s.put("name", "Legacy V3 Session");
        sessions.put(s);
        v3Archive.put(SafBackupManager.KEY_SESSIONS, sessions);

        // Validation must accept legacy v3 archive cleanly
        JSONObject extracted = SafBackupManager.validateAndExtractArchive(v3Archive.toString());
        Assert.assertNotNull(extracted);
        Assert.assertEquals(3, extracted.getInt(SafBackupManager.KEY_VERSION));
        Assert.assertEquals(1, extracted.getJSONArray(SafBackupManager.KEY_SESSIONS).length());
    }

    @Test
    public void testSafBackupEngine_version4Constants() {
        Assert.assertEquals(4, SafBackupEngine.BACKUP_SCHEMA_VERSION);
        Assert.assertEquals(4, SafBackupEngine.APP_VERSION_CODE);
        Assert.assertEquals("QuestionSolvingTimer", SafBackupEngine.APP_ID);
    }
}

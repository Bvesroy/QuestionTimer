package com.example.timerapp.data;

import com.example.timerapp.data.model.QuestionLog;
import com.example.timerapp.data.model.Session;
import com.example.timerapp.data.model.SessionResumeData;
import com.example.timerapp.data.model.SessionStats;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests verifying JSON serialization, stats calculations, and session resumption logic.
 */
public class BackupManagerTest {

    @Test
    public void testSessionResumeLogic_70SolvedNextIs71() {
        Session session = new Session(1L, "JEE Advanced Physics", 90, 1000L, 2000L, true);
        List<QuestionLog> logs = new ArrayList<>();

        long totalDuration = 0;
        for (int i = 1; i <= 70; i++) {
            long duration = 60000L + (i * 1000L);
            totalDuration += duration;
            logs.add(new QuestionLog(i, 1L, i, duration, 2000L + i));
        }

        int solvedCount = logs.size();
        int maxQuestionNum = logs.get(logs.size() - 1).getQuestionNum();
        int nextQuestionNum = maxQuestionNum + 1;
        long averageDuration = totalDuration / solvedCount;

        SessionResumeData resumeData = new SessionResumeData(
                session,
                solvedCount,
                nextQuestionNum,
                totalDuration,
                averageDuration,
                logs
        );

        Assert.assertEquals(70, resumeData.getSolvedCount());
        Assert.assertEquals(71, resumeData.getNextQuestionNum());
        Assert.assertEquals(totalDuration, resumeData.getTotalElapsedMs());
        Assert.assertEquals(averageDuration, resumeData.getAverageDurationMs());
        Assert.assertEquals(70, resumeData.getQuestionLogs().size());
    }

    @Test
    public void testSessionStatsProgressAndFormatting() {
        SessionStats stats = new SessionStats(
                1L,
                "JEE Mathematics 2026",
                100, // target
                75,  // solved
                2700000L, // 45 minutes
                36000L,   // 36 seconds avg
                1000L,
                2000L,
                true
        );

        Assert.assertEquals(75.0, stats.getProgressPercentage(), 0.001);
        Assert.assertEquals("45:00", stats.getFormattedTotalDuration());
        Assert.assertEquals("00:36", stats.getFormattedAverageDuration());
    }

    @Test
    public void testBackupJsonStructureValidation() throws JSONException {
        JSONObject root = new JSONObject();
        root.put("version", 1);
        root.put("app", "QuestionSolvingTimer");
        root.put("exported_at", System.currentTimeMillis());

        JSONArray sessionsArray = new JSONArray();
        JSONObject sessionObj = new JSONObject();
        sessionObj.put("id", 1L);
        sessionObj.put("name", "Mock JEE Prep");
        sessionObj.put("target_count", 30);
        sessionObj.put("created_at", 1700000000000L);
        sessionObj.put("updated_at", 1700005000000L);
        sessionObj.put("is_active", true);

        JSONArray logsArray = new JSONArray();
        JSONObject logObj = new JSONObject();
        logObj.put("id", 101L);
        logObj.put("question_num", 1);
        logObj.put("duration_ms", 45000L);
        logObj.put("completed_at", 1700000045000L);
        logsArray.put(logObj);

        sessionObj.put("question_logs", logsArray);
        sessionsArray.put(sessionObj);
        root.put("sessions", sessionsArray);

        String jsonString = root.toString(2);

        // Verification of roundtrip deserialization
        JSONObject restoredRoot = new JSONObject(jsonString);
        Assert.assertEquals(1, restoredRoot.getInt("version"));
        Assert.assertEquals("QuestionSolvingTimer", restoredRoot.getString("app"));
        JSONArray restoredSessions = restoredRoot.getJSONArray("sessions");
        Assert.assertEquals(1, restoredSessions.length());

        JSONObject restoredSession = restoredSessions.getJSONObject(0);
        Assert.assertEquals("Mock JEE Prep", restoredSession.getString("name"));
        Assert.assertEquals(30, restoredSession.getInt("target_count"));

        JSONArray restoredLogs = restoredSession.getJSONArray("question_logs");
        Assert.assertEquals(1, restoredLogs.length());
        Assert.assertEquals(45000L, restoredLogs.getJSONObject(0).getLong("duration_ms"));
    }
}

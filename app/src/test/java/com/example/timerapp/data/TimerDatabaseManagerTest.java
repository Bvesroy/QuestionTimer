package com.example.timerapp.data;

import com.example.timerapp.data.local.sqlite.DatabaseContract;
import com.example.timerapp.data.model.QuestionLog;
import com.example.timerapp.data.model.Session;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests verifying database contracts, SQL statements, and schema definitions.
 */
public class TimerDatabaseManagerTest {

    @Test
    public void testDatabaseContractDefinitions() {
        Assert.assertEquals("question_timer.db", DatabaseContract.DATABASE_NAME);
        Assert.assertEquals(4, DatabaseContract.DATABASE_VERSION);

        Assert.assertEquals("sessions", DatabaseContract.SessionEntry.TABLE_NAME);
        Assert.assertEquals("question_logs", DatabaseContract.QuestionLogEntry.TABLE_NAME);
        Assert.assertEquals("subjects", DatabaseContract.SubjectEntry.TABLE_NAME);
        Assert.assertEquals("exam_countdowns", DatabaseContract.ExamCountdownEntry.TABLE_NAME);
        Assert.assertEquals("achievements", DatabaseContract.AchievementEntry.TABLE_NAME);
        Assert.assertEquals("settings", DatabaseContract.SettingEntry.TABLE_NAME);
        Assert.assertEquals("attempt_audit_history", DatabaseContract.AttemptAuditEntry.TABLE_NAME);

        // Verify foreign key constraint clause in SQL
        Assert.assertTrue(DatabaseContract.QuestionLogEntry.SQL_CREATE_TABLE.contains("FOREIGN KEY"));
        Assert.assertTrue(DatabaseContract.QuestionLogEntry.SQL_CREATE_TABLE.contains("ON DELETE CASCADE"));
        Assert.assertTrue(DatabaseContract.AttemptAuditEntry.SQL_CREATE_TABLE.contains("FOREIGN KEY"));
        Assert.assertTrue(DatabaseContract.AttemptAuditEntry.SQL_CREATE_TABLE.contains("ON DELETE CASCADE"));

        // Verify indexes
        Assert.assertTrue(DatabaseContract.SessionEntry.SQL_CREATE_IDX_IS_ACTIVE.contains("idx_sessions_is_active"));
        Assert.assertTrue(DatabaseContract.QuestionLogEntry.SQL_CREATE_IDX_SESSION_ID.contains("idx_question_logs_session_id"));
        Assert.assertTrue(DatabaseContract.SettingEntry.SQL_CREATE_IDX_KEY.contains("idx_settings_key"));
        Assert.assertTrue(DatabaseContract.AttemptAuditEntry.SQL_CREATE_IDX_QUESTION_LOG_ID.contains("idx_audit_question_log_id"));
    }

    @Test
    public void testSessionModelEqualityAndCloning() {
        long now = System.currentTimeMillis();
        Session s1 = new Session(10L, "Chemistry Practice", 45, now, now, true);
        Session s2 = new Session(10L, "Chemistry Practice", 45, now, now, true);

        Assert.assertEquals(s1, s2);
        Assert.assertEquals(s1.hashCode(), s2.hashCode());
        Assert.assertTrue(s1.isActive());
        Assert.assertEquals(45, s1.getTargetCount());
    }

    @Test
    public void testQuestionLogFormatting() {
        QuestionLog log = new QuestionLog(1L, 10L, 5, 125400L, 1000L); // 2m 5.4s
        Assert.assertEquals("02:05.4", log.getFormattedDuration());
        Assert.assertEquals("02:05", log.getFormattedDurationMinutesSeconds());
    }

    @Test
    public void testQuestionLogOutcomeToggleContracts() {
        com.example.timerapp.model.QuestionLog log = new com.example.timerapp.model.QuestionLog(
                1L, 10L, 1, 60000L, 1000L, "TEST", "PHYSICS", "MEDIUM", "INCORRECT", 2, "CALC_ERROR", "Notes"
        );
        Assert.assertTrue(com.example.timerapp.data.analytics.OutcomeClassifier.isWrong(log.getOutcome()));

        // Toggle outcome
        com.example.timerapp.model.QuestionLog updated = log.withOutcome("CORRECT");
        Assert.assertTrue(com.example.timerapp.data.analytics.OutcomeClassifier.isCorrect(updated.getOutcome()));
        Assert.assertFalse(com.example.timerapp.data.analytics.OutcomeClassifier.isWrong(updated.getOutcome()));
    }
}

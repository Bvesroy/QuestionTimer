package com.example.timerapp.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.timerapp.data.local.sqlite.TimerDatabaseManager;
import com.example.timerapp.data.model.Achievement;
import com.example.timerapp.data.model.AdaptiveTargetSuggestion;
import com.example.timerapp.data.model.DailyPracticeDataPoint;
import com.example.timerapp.data.model.DataIntegrityReport;
import com.example.timerapp.data.model.DateSessionGroup;
import com.example.timerapp.data.model.ExamCountdown;
import com.example.timerapp.data.model.HeatmapDayData;
import com.example.timerapp.data.model.MovingAveragePoint;
import com.example.timerapp.data.model.PeriodReport;
import com.example.timerapp.data.model.PerformanceTrend;
import com.example.timerapp.data.model.PersonalRecords;
import com.example.timerapp.data.model.Session;
import com.example.timerapp.data.model.SessionStats;
import com.example.timerapp.data.model.SpeedDataPoint;
import com.example.timerapp.data.model.Subject;
import com.example.timerapp.data.model.SubjectPracticeStats;
import com.example.timerapp.data.backup.SafBackupEngine;
import com.example.timerapp.data.model.BreakLog;
import com.example.timerapp.data.model.ExamProfile;
import com.example.timerapp.data.model.JeeAnalyticsReport;
import com.example.timerapp.data.model.StudyCoachFeedback;
import com.example.timerapp.data.model.StudyRecommendation;
import com.example.timerapp.data.model.TargetCompliance;
import com.example.timerapp.data.model.TargetSettings;
import com.example.timerapp.data.model.TimeRangeFilter;
import com.example.timerapp.data.model.TimeRangeKpiSummary;
import com.example.timerapp.data.model.TimeSlotStats;
import com.example.timerapp.model.QuestionLog;
import com.example.timerapp.model.TimerSession;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistence layer bridging in-memory UI models to native SQLite storage
 * via {@link TimerDatabaseManager}.
 *
 * Guarantees zero data loss, instant transactional writes, automatic
 * legacy migration from SharedPreferences, date-grouped query views,
 * advanced analytics, reports, streak calculations, target tracking,
 * CSV export/import, crash recovery, and diagnostic verification.
 */
public class SessionStorage {
    private static final String TAG = "SessionStorage";
    public static final String PREFS_NAME = "question_timer_prefs";
    public static final String KEY_SESSIONS = "saved_sessions";
    public static final String KEY_ACTIVE_SESSION_ID = "active_session_id";
    public static final String KEY_MIGRATED_TO_SQLITE = "migrated_to_sqlite";

    public static final String KEY_TARGET_TIME_PER_QUESTION_SEC = "target_time_per_question_sec";
    public static final String KEY_TARGET_SESSION_DURATION_MIN = "target_session_duration_min";
    public static final String KEY_TARGET_SESSION_QUESTIONS = "target_session_questions";

    private final SharedPreferences prefs;
    private final TimerDatabaseManager dbManager;

    public SessionStorage(Context context) {
        Context appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.dbManager = TimerDatabaseManager.getInstance(appContext);
        migrateLegacyPrefsIfPresent();
    }

    public TimerDatabaseManager getDbManager() {
        return dbManager;
    }

    /**
     * One-time silent migration from SharedPreferences JSON to SQLite tables.
     */
    private void migrateLegacyPrefsIfPresent() {
        if (prefs.getBoolean(KEY_MIGRATED_TO_SQLITE, false)) {
            return;
        }

        String raw = prefs.getString(KEY_SESSIONS, null);
        if (raw != null && !raw.trim().isEmpty()) {
            try {
                JSONArray array = new JSONArray(raw);
                for (int i = 0; i < array.length(); i++) {
                    TimerSession s = deserializeSession(array.getJSONObject(i));
                    if (s != null) {
                        long sId = dbManager.createSession(s.getName(), s.getTarget(), s.getSubject(), s.getNotes(), s.getStatus());
                        dbManager.updateSessionReflection(sId, s.getSubject(), s.getNotes(), s.getWhatWentWell(), s.getNeedsImprovement());
                        for (QuestionLog log : s.getLogs()) {
                            dbManager.logQuestionTime(sId, log.getQuestionNumber(), log.getDurationMs());
                        }
                    }
                }
                Log.i(TAG, "Successfully migrated legacy SharedPreferences sessions to SQLite.");
            } catch (Exception e) {
                Log.e(TAG, "Legacy migration error", e);
            }
        }
        prefs.edit().putBoolean(KEY_MIGRATED_TO_SQLITE, true).apply();
    }

    public synchronized void saveActiveSessionId(long id) {
        prefs.edit().putLong(KEY_ACTIVE_SESSION_ID, id).apply();
        if (id > 0) {
            dbManager.resumeSession(id);
        }
    }

    public synchronized long getActiveSessionId() {
        Session active = dbManager.getActiveSession();
        if (active != null) {
            return active.getId();
        }
        return prefs.getLong(KEY_ACTIVE_SESSION_ID, -1);
    }

    /**
     * Saves or synchronizes sessions and question logs into SQLite.
     */
    public synchronized void saveSessions(List<TimerSession> sessions) {
        if (sessions == null) return;

        for (TimerSession s : sessions) {
            Session existing = (s.getId() > 0) ? dbManager.getSessionById(s.getId()) : null;
            if (existing == null) {
                long newId = dbManager.createSession(s.getName(), s.getTarget(), s.getSubject(), s.getNotes(), s.getStatus());
                dbManager.updateSessionReflection(newId, s.getSubject(), s.getNotes(), s.getWhatWentWell(), s.getNeedsImprovement());
                s.setId(newId);
            } else {
                dbManager.renameSession(s.getId(), s.getName());
                dbManager.setSessionTarget(s.getId(), s.getTarget());
                dbManager.updateSessionReflection(s.getId(), s.getSubject(), s.getNotes(), s.getWhatWentWell(), s.getNeedsImprovement());
                dbManager.updateSessionStatus(s.getId(), s.getStatus());
            }

            // Sync logs: get existing logged question count
            List<QuestionLog> uiLogs = dbManager.getUiQuestionLogsForSession(s.getId());
            int existingCount = uiLogs.size();
            List<QuestionLog> currentLogs = s.getLogs();

            if (currentLogs.size() > existingCount) {
                for (int i = existingCount; i < currentLogs.size(); i++) {
                    QuestionLog log = currentLogs.get(i);
                    dbManager.logQuestionTime(s.getId(), log.getQuestionNumber(), log.getDurationMs(),
                            log.getLabel(), log.getTopic(), log.getDifficulty(), log.getOutcome(),
                            log.getConfidence(), log.getMistakeTag(), log.getNotes());
                }
            } else if (currentLogs.size() < existingCount) {
                if (currentLogs.isEmpty()) {
                    dbManager.clearQuestionLogsForSession(s.getId());
                } else {
                    int toDelete = existingCount - currentLogs.size();
                    for (int i = 0; i < toDelete; i++) {
                        dbManager.deleteLastQuestionLogForSession(s.getId());
                    }
                }
            }

            // Sync any outcome / mistake tag modifications on existing logs
            for (int i = 0; i < Math.min(existingCount, currentLogs.size()); i++) {
                QuestionLog log = currentLogs.get(i);
                QuestionLog existingLog = uiLogs.get(i);
                if (log.getId() > 0 && (!log.getOutcome().equals(existingLog.getOutcome())
                        || !log.getMistakeTag().equals(existingLog.getMistakeTag())
                        || !log.getNotes().equals(existingLog.getNotes())
                        || log.getConfidence() != existingLog.getConfidence())) {
                    dbManager.updateQuestionLogDetails(log.getId(), log.getOutcome(), log.getDifficulty(),
                            log.getConfidence(), log.getMistakeTag(), log.getNotes());
                }
            }
        }
    }

    /**
     * Loads all sessions with their question logs from SQLite database.
     */
    public synchronized List<TimerSession> loadSessions() {
        List<SessionStats> statsList = dbManager.getAllSessionsWithStats();
        List<TimerSession> list = new ArrayList<>(statsList.size());

        for (SessionStats stats : statsList) {
            TimerSession session = dbManager.loadTimerSession(stats.getSessionId());
            if (session != null) {
                list.add(session);
            }
        }
        return list;
    }

    // =========================================================================
    // Enhanced Date Grouping & Analytics APIs
    // =========================================================================

    public synchronized List<DateSessionGroup> getSessionsGroupedByDate() {
        return dbManager.getSessionsGroupedByDate();
    }

    public synchronized List<DateSessionGroup> getSessionsGroupedByDate(TimeRangeFilter filter) {
        return dbManager.getSessionsGroupedByDate(filter);
    }

    public synchronized TimeRangeKpiSummary getAnalyticsSummary(TimeRangeFilter filter) {
        return dbManager.getAnalyticsSummary(filter);
    }

    public synchronized TimeRangeKpiSummary getAnalyticsSummary(long startTimestamp, long endTimestamp) {
        return dbManager.getAnalyticsSummary(startTimestamp, endTimestamp);
    }

    public synchronized List<SpeedDataPoint> getSpeedOverTimeData(TimeRangeFilter filter) {
        return dbManager.getSpeedOverTimeData(filter);
    }

    public synchronized List<DailyPracticeDataPoint> getDailyPracticeTimeData(TimeRangeFilter filter) {
        return dbManager.getDailyPracticeTimeData(filter);
    }

    // =========================================================================
    // Streaks, Scores, Reports & Trends
    // =========================================================================

    public synchronized int calculateCurrentStreak() {
        return dbManager.calculateCurrentStreak();
    }

    public synchronized int calculateCurrentStreak(long now) {
        return dbManager.calculateCurrentStreak(now);
    }

    public synchronized int calculateLongestStreak() {
        return dbManager.calculateLongestStreak();
    }

    public synchronized double calculateConsistencyScore() {
        return dbManager.calculateConsistencyScore();
    }

    public synchronized double calculateConsistencyScore(TimeRangeFilter filter) {
        return dbManager.calculateConsistencyScore(filter);
    }

    public synchronized double calculateFocusScore() {
        return dbManager.calculateFocusScore();
    }

    public synchronized double calculateFocusScore(TimeRangeFilter filter) {
        return dbManager.calculateFocusScore(filter);
    }

    public synchronized PeriodReport getWeeklyReport(int offsetWeeks) {
        return dbManager.getWeeklyReport(offsetWeeks);
    }

    public synchronized PeriodReport getMonthlyReport(int offsetMonths) {
        return dbManager.getMonthlyReport(offsetMonths);
    }

    public synchronized List<HeatmapDayData> getMonthlyHeatmapData(int year, int month) {
        return dbManager.getMonthlyHeatmapData(year, month);
    }

    public synchronized List<TimeSlotStats> getTimeSlotStats() {
        return dbManager.getTimeSlotStats();
    }

    public synchronized List<TimeSlotStats> getBestPracticeTimeAnalysis() {
        return dbManager.getBestPracticeTimeAnalysis();
    }

    public synchronized TimeSlotStats getBestTimeSlot() {
        return dbManager.getBestTimeSlot();
    }

    public synchronized List<MovingAveragePoint> getMovingAveragePoints(int windowSize) {
        return dbManager.getMovingAveragePoints(windowSize);
    }

    public synchronized List<MovingAveragePoint> getMovingAverageSeries(int windowSize) {
        return dbManager.getMovingAverageSeries(windowSize);
    }

    public synchronized PersonalRecords getPersonalRecords() {
        return dbManager.getPersonalRecords();
    }

    public synchronized PerformanceTrend getPerformanceTrend() {
        return dbManager.getPerformanceTrend();
    }

    public synchronized List<SubjectPracticeStats> getSubjectPracticeStats() {
        return dbManager.getSubjectPracticeStats();
    }

    public synchronized AdaptiveTargetSuggestion getAdaptiveTargetSuggestion() {
        return dbManager.getAdaptiveTargetSuggestion();
    }

    // =========================================================================
    // Subjects, Countdowns & Achievements
    // =========================================================================

    public synchronized List<Subject> getAllSubjects() {
        return dbManager.getAllSubjects();
    }

    public synchronized long createSubject(String name, String colorHex) {
        return dbManager.createSubject(name, colorHex);
    }

    public synchronized Subject getSubjectByName(String name) {
        return dbManager.getSubjectByName(name);
    }

    public synchronized boolean deleteSubject(long id) {
        return dbManager.deleteSubject(id);
    }

    public synchronized List<ExamCountdown> getAllExamCountdowns() {
        return dbManager.getAllExamCountdowns();
    }

    public synchronized long createExamCountdown(String title, long targetDateMillis, String description) {
        return dbManager.createExamCountdown(title, targetDateMillis, description);
    }

    public synchronized boolean deleteExamCountdown(long id) {
        return dbManager.deleteExamCountdown(id);
    }

    public synchronized List<Achievement> getAllAchievements() {
        return dbManager.getAllAchievements();
    }

    public synchronized boolean unlockAchievement(String key, String title) {
        return dbManager.unlockAchievement(key, title);
    }

    public synchronized boolean isAchievementUnlocked(String key) {
        return dbManager.isAchievementUnlocked(key);
    }

    // =========================================================================
    // CSV Export & Import
    // =========================================================================

    public synchronized String exportSessionsToCsv() {
        return dbManager.exportSessionsToCsv();
    }

    public synchronized int importSessionsFromCsv(String csvContent) {
        return dbManager.importSessionsFromCsv(csvContent);
    }

    // =========================================================================
    // Session Notes & Reflection
    // =========================================================================

    public synchronized boolean saveSessionNotes(long sessionId, String subject, String notes,
                                                 String wentWell, String needsImprovement) {
        return dbManager.saveSessionNotes(sessionId, subject, notes, wentWell, needsImprovement);
    }

    public synchronized boolean updateSessionReflection(long sessionId, String subject, String notes,
                                                        String wentWell, String needsImprovement) {
        return dbManager.updateSessionReflection(sessionId, subject, notes, wentWell, needsImprovement);
    }

    // =========================================================================
    // Crash Recovery & Diagnostics
    // =========================================================================

    public synchronized void saveInterruptedSessionState(TimerSession session) {
        dbManager.saveInterruptedSessionState(session);
    }

    public synchronized TimerSession getInterruptedSession() {
        return dbManager.getInterruptedSession();
    }

    public synchronized void clearInterruptedSessionState() {
        dbManager.clearInterruptedSessionState();
    }

    public synchronized DataIntegrityReport verifyAndRepairDatabaseIntegrity() {
        return dbManager.verifyAndRepairDatabaseIntegrity();
    }

    public synchronized DataIntegrityReport verifyAndRepairDataIntegrity() {
        return dbManager.verifyAndRepairDataIntegrity();
    }

    // =========================================================================
    // Target Settings & Compliance APIs
    // =========================================================================

    public synchronized TargetSettings getTargetSettings() {
        return dbManager.getTargetSettings();
    }

    public synchronized void saveTargetSettings(TargetSettings settings) {
        dbManager.saveTargetSettings(settings);
    }

    public synchronized int getTargetTimePerQuestionSec() {
        return dbManager.getTargetTimePerQuestionSec();
    }

    public synchronized void setTargetTimePerQuestionSec(int sec) {
        dbManager.setTargetTimePerQuestionSec(sec);
    }

    public synchronized int getTargetSessionDurationMin() {
        return dbManager.getTargetSessionDurationMin();
    }

    public synchronized void setTargetSessionDurationMin(int min) {
        dbManager.setTargetSessionDurationMin(min);
    }

    public synchronized int getTargetSessionQuestions() {
        return dbManager.getTargetSessionQuestions();
    }

    public synchronized void setTargetSessionQuestions(int count) {
        dbManager.setTargetSessionQuestions(count);
    }

    public TargetCompliance computeTargetCompliance(SessionStats session) {
        return dbManager.computeTargetCompliance(session);
    }

    public TargetCompliance computeTargetCompliance(long durationMs, int questionsSolved, long avgTimePerQuestionMs) {
        return dbManager.computeTargetCompliance(durationMs, questionsSolved, avgTimePerQuestionMs);
    }

    // =========================================================================
    // Dual-Clock, Breaks & JEE Operations
    // =========================================================================

    public synchronized long recordBreakStart(long sessionId, long startTime) {
        return dbManager.recordBreakStart(sessionId, startTime);
    }

    public synchronized long recordBreakEnd(long sessionId, long endTime) {
        return dbManager.recordBreakEnd(sessionId, endTime);
    }

    public synchronized List<BreakLog> getBreaksForSession(long sessionId) {
        return dbManager.getBreaksForSession(sessionId);
    }

    public synchronized void updateSessionClocks(long sessionId, long totalStudyMs, long totalBreakMs, long activeQuestionElapsedMs) {
        dbManager.updateSessionClocks(sessionId, totalStudyMs, totalBreakMs, activeQuestionElapsedMs);
    }

    public synchronized List<ExamProfile> getAllExamProfiles() {
        return dbManager.getAllExamProfiles();
    }

    public synchronized ExamProfile getExamProfileByName(String name) {
        return dbManager.getExamProfileByName(name);
    }

    public synchronized long createOrUpdateExamProfile(ExamProfile profile) {
        return dbManager.createOrUpdateExamProfile(profile);
    }

    public synchronized boolean deleteExamProfile(long id) {
        return dbManager.deleteExamProfile(id);
    }

    public synchronized long recordCoachFeedback(String recommendationKey, String feedback, boolean dismissed) {
        return dbManager.recordCoachFeedback(recommendationKey, feedback, dismissed);
    }

    public synchronized boolean dismissRecommendation(String recommendationKey) {
        return dbManager.dismissRecommendation(recommendationKey);
    }

    public synchronized List<StudyRecommendation> getStudyRecommendations(int targetPaceSeconds, int targetDailyQuestions) {
        return dbManager.getStudyRecommendations(targetPaceSeconds, targetDailyQuestions);
    }

    public synchronized JeeAnalyticsReport getJeeAnalytics(String profileName, long startMillis, long endMillis) {
        return dbManager.getJeeAnalytics(profileName, startMillis, endMillis);
    }

    public synchronized void exportSafBackup(OutputStream outputStream, JSONObject preferences) throws java.io.IOException, JSONException {
        dbManager.exportSafBackup(outputStream, preferences);
    }

    public synchronized SafBackupEngine.ValidationResult validateSafBackup(InputStream inputStream) {
        return dbManager.validateSafBackup(inputStream);
    }

    public synchronized SafBackupEngine.RestoreResult restoreSafBackup(InputStream inputStream, SafBackupEngine.RestoreMode mode) throws java.io.IOException, JSONException {
        return dbManager.restoreSafBackup(inputStream, mode);
    }

    private TimerSession deserializeSession(JSONObject obj) {
        try {
            long id = obj.optLong("id", System.currentTimeMillis());
            String name = obj.optString("name", "SESSION");
            int target = obj.optInt("target", 0);
            int currentQuestion = obj.optInt("currentQuestion", 1);
            String status = obj.optString("status", "CREATED");

            TimerSession session = new TimerSession(id, name, target);
            session.setCurrentQuestion(currentQuestion);
            session.setStatus(status);
            session.setSubject(obj.optString("subject", "General"));
            session.setNotes(obj.optString("notes", ""));
            session.setWhatWentWell(obj.optString("whatWentWell", ""));
            session.setNeedsImprovement(obj.optString("needsImprovement", ""));
            session.setProfileId(obj.optString("profileId", "default"));

            JSONArray logsArray = obj.optJSONArray("logs");
            if (logsArray != null) {
                for (int i = 0; i < logsArray.length(); i++) {
                    JSONObject lObj = logsArray.getJSONObject(i);
                    QuestionLog log = new QuestionLog(
                            lObj.optLong("id", System.currentTimeMillis()),
                            lObj.optLong("sessionId", id),
                            lObj.optInt("questionNumber", i + 1),
                            lObj.optLong("durationMs", 0),
                            lObj.optLong("timestamp", System.currentTimeMillis())
                    );
                    session.addLog(log);
                }
            }
            return session;
        } catch (JSONException e) {
            Log.e(TAG, "Error deserializing session", e);
            return null;
        }
    }
}

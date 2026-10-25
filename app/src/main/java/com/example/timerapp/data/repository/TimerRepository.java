package com.example.timerapp.data.repository;

import android.content.Context;

import com.example.timerapp.data.local.sqlite.TimerDatabaseManager;
import com.example.timerapp.data.model.Achievement;
import com.example.timerapp.data.model.AdaptiveTargetSuggestion;
import com.example.timerapp.data.model.AttemptAuditRecord;
import com.example.timerapp.data.model.DailyPracticeDataPoint;
import com.example.timerapp.data.model.DataIntegrityReport;
import com.example.timerapp.data.model.DateSessionGroup;
import com.example.timerapp.data.model.ExamCountdown;
import com.example.timerapp.data.model.HeatmapDayData;
import com.example.timerapp.data.model.MovingAveragePoint;
import com.example.timerapp.data.model.PeriodReport;
import com.example.timerapp.data.model.PerformanceTrend;
import com.example.timerapp.data.model.PersonalRecords;
import com.example.timerapp.data.model.QuestionLog;
import com.example.timerapp.data.model.Session;
import com.example.timerapp.data.model.SessionResumeData;
import com.example.timerapp.data.model.SessionStats;
import com.example.timerapp.data.model.SpeedDataPoint;
import com.example.timerapp.data.model.Subject;
import com.example.timerapp.data.model.SubjectPracticeStats;
import com.example.timerapp.data.model.TargetCompliance;
import com.example.timerapp.data.model.WallpaperPreferences;
import com.example.timerapp.data.model.WidgetSettings;
import com.example.timerapp.data.backup.SafBackupEngine;
import com.example.timerapp.data.model.BreakLog;
import com.example.timerapp.data.model.ExamProfile;
import com.example.timerapp.data.model.JeeAnalyticsReport;
import com.example.timerapp.data.model.StudyCoachFeedback;
import com.example.timerapp.data.model.StudyRecommendation;
import com.example.timerapp.data.model.TargetSettings;
import com.example.timerapp.data.model.TimeRangeFilter;
import com.example.timerapp.data.model.TimeRangeKpiSummary;
import com.example.timerapp.data.model.TimeSlotStats;
import com.example.timerapp.model.TimerSession;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * Repository providing clean abstraction over local SQLite persistence layer.
 * Thread-safe and ready for ViewModel or Activity consumption.
 */
public class TimerRepository {

    private final TimerDatabaseManager dbManager;

    public TimerRepository(Context context) {
        this.dbManager = TimerDatabaseManager.getInstance(context);
    }

    public TimerRepository(TimerDatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public long createSession(String name, int target) {
        return dbManager.createSession(name, target);
    }

    public long createSession(String name, int target, String subject, String notes, String status) {
        return dbManager.createSession(name, target, subject, notes, status);
    }

    public boolean renameSession(long sessionId, String newName) {
        return dbManager.renameSession(sessionId, newName);
    }

    public boolean setSessionTarget(long sessionId, int target) {
        return dbManager.setSessionTarget(sessionId, target);
    }

    public boolean updateSessionReflection(long sessionId, String subject, String notes,
                                           String wentWell, String needsImprovement) {
        return dbManager.updateSessionReflection(sessionId, subject, notes, wentWell, needsImprovement);
    }

    public boolean saveSessionNotes(long sessionId, String subject, String notes,
                                    String wentWell, String needsImprovement) {
        return dbManager.saveSessionNotes(sessionId, subject, notes, wentWell, needsImprovement);
    }

    public boolean updateSessionStatus(long sessionId, String status) {
        return dbManager.updateSessionStatus(sessionId, status);
    }

    public long logQuestionTime(long sessionId, int questionNum, long durationMs) {
        return dbManager.logQuestionTime(sessionId, questionNum, durationMs);
    }

    public SessionResumeData resumeSession(long sessionId) {
        return dbManager.resumeSession(sessionId);
    }

    public Session getActiveSession() {
        return dbManager.getActiveSession();
    }

    public TimerSession getActiveTimerSession() {
        return dbManager.getActiveTimerSession();
    }

    public TimerSession loadTimerSession(long sessionId) {
        return dbManager.loadTimerSession(sessionId);
    }

    public List<SessionStats> getAllSessionsWithStats() {
        return dbManager.getAllSessionsWithStats();
    }

    public List<QuestionLog> getQuestionsForSession(long sessionId) {
        return dbManager.getQuestionsForSession(sessionId);
    }

    public List<com.example.timerapp.model.QuestionLog> getUiQuestionLogsForSession(long sessionId) {
        return dbManager.getUiQuestionLogsForSession(sessionId);
    }

    public Session getSessionById(long sessionId) {
        return dbManager.getSessionById(sessionId);
    }

    public boolean deleteSession(long sessionId) {
        return dbManager.deleteSession(sessionId);
    }

    public int clearQuestionLogsForSession(long sessionId) {
        return dbManager.clearQuestionLogsForSession(sessionId);
    }

    public void clearAllData() {
        dbManager.clearAllData();
    }

    // =========================================================================
    // Enhanced Date Grouping & Analytics
    // =========================================================================

    public List<DateSessionGroup> getSessionsGroupedByDate() {
        return dbManager.getSessionsGroupedByDate();
    }

    public List<DateSessionGroup> getSessionsGroupedByDate(TimeRangeFilter filter) {
        return dbManager.getSessionsGroupedByDate(filter);
    }

    public TimeRangeKpiSummary getAnalyticsSummary(TimeRangeFilter filter) {
        return dbManager.getAnalyticsSummary(filter);
    }

    public TimeRangeKpiSummary getAnalyticsSummary(long startTimestamp, long endTimestamp) {
        return dbManager.getAnalyticsSummary(startTimestamp, endTimestamp);
    }

    public List<SpeedDataPoint> getSpeedOverTimeData(TimeRangeFilter filter) {
        return dbManager.getSpeedOverTimeData(filter);
    }

    public List<DailyPracticeDataPoint> getDailyPracticeTimeData(TimeRangeFilter filter) {
        return dbManager.getDailyPracticeTimeData(filter);
    }

    // =========================================================================
    // Streaks, Scores, Reports & Trends
    // =========================================================================

    public int calculateCurrentStreak() {
        return dbManager.calculateCurrentStreak();
    }

    public int calculateCurrentStreak(long now) {
        return dbManager.calculateCurrentStreak(now);
    }

    public int calculateLongestStreak() {
        return dbManager.calculateLongestStreak();
    }

    public double calculateConsistencyScore() {
        return dbManager.calculateConsistencyScore();
    }

    public double calculateConsistencyScore(TimeRangeFilter filter) {
        return dbManager.calculateConsistencyScore(filter);
    }

    public double calculateFocusScore() {
        return dbManager.calculateFocusScore();
    }

    public double calculateFocusScore(TimeRangeFilter filter) {
        return dbManager.calculateFocusScore(filter);
    }

    public PeriodReport getWeeklyReport(int offsetWeeks) {
        return dbManager.getWeeklyReport(offsetWeeks);
    }

    public PeriodReport getMonthlyReport(int offsetMonths) {
        return dbManager.getMonthlyReport(offsetMonths);
    }

    public List<HeatmapDayData> getMonthlyHeatmapData(int year, int month) {
        return dbManager.getMonthlyHeatmapData(year, month);
    }

    public List<TimeSlotStats> getTimeSlotStats() {
        return dbManager.getTimeSlotStats();
    }

    public List<TimeSlotStats> getBestPracticeTimeAnalysis() {
        return dbManager.getBestPracticeTimeAnalysis();
    }

    public TimeSlotStats getBestTimeSlot() {
        return dbManager.getBestTimeSlot();
    }

    public List<MovingAveragePoint> getMovingAveragePoints(int windowSize) {
        return dbManager.getMovingAveragePoints(windowSize);
    }

    public List<MovingAveragePoint> getMovingAverageSeries(int windowSize) {
        return dbManager.getMovingAverageSeries(windowSize);
    }

    public PersonalRecords getPersonalRecords() {
        return dbManager.getPersonalRecords();
    }

    public PerformanceTrend getPerformanceTrend() {
        return dbManager.getPerformanceTrend();
    }

    public List<SubjectPracticeStats> getSubjectPracticeStats() {
        return dbManager.getSubjectPracticeStats();
    }

    public AdaptiveTargetSuggestion getAdaptiveTargetSuggestion() {
        return dbManager.getAdaptiveTargetSuggestion();
    }

    // =========================================================================
    // Subjects, Countdowns & Achievements
    // =========================================================================

    public List<Subject> getAllSubjects() {
        return dbManager.getAllSubjects();
    }

    public long createSubject(String name, String colorHex) {
        return dbManager.createSubject(name, colorHex);
    }

    public Subject getSubjectByName(String name) {
        return dbManager.getSubjectByName(name);
    }

    public boolean deleteSubject(long id) {
        return dbManager.deleteSubject(id);
    }

    public List<ExamCountdown> getAllExamCountdowns() {
        return dbManager.getAllExamCountdowns();
    }

    public long createExamCountdown(String title, long targetDateMillis, String description) {
        return dbManager.createExamCountdown(title, targetDateMillis, description);
    }

    public boolean deleteExamCountdown(long id) {
        return dbManager.deleteExamCountdown(id);
    }

    public List<Achievement> getAllAchievements() {
        return dbManager.getAllAchievements();
    }

    public boolean unlockAchievement(String key, String title) {
        return dbManager.unlockAchievement(key, title);
    }

    public boolean isAchievementUnlocked(String key) {
        return dbManager.isAchievementUnlocked(key);
    }

    // =========================================================================
    // CSV Export & Import
    // =========================================================================

    public String exportSessionsToCsv() {
        return dbManager.exportSessionsToCsv();
    }

    public int importSessionsFromCsv(String csvContent) {
        return dbManager.importSessionsFromCsv(csvContent);
    }

    // =========================================================================
    // Crash Recovery & Diagnostics
    // =========================================================================

    public void saveInterruptedSessionState(TimerSession session) {
        dbManager.saveInterruptedSessionState(session);
    }

    public TimerSession getInterruptedSession() {
        return dbManager.getInterruptedSession();
    }

    public void clearInterruptedSessionState() {
        dbManager.clearInterruptedSessionState();
    }

    public DataIntegrityReport verifyAndRepairDatabaseIntegrity() {
        return dbManager.verifyAndRepairDatabaseIntegrity();
    }

    public DataIntegrityReport verifyAndRepairDataIntegrity() {
        return dbManager.verifyAndRepairDataIntegrity();
    }

    // =========================================================================
    // Target Settings & Compliance
    // =========================================================================

    public TargetSettings getTargetSettings() {
        return dbManager.getTargetSettings();
    }

    public void saveTargetSettings(TargetSettings settings) {
        dbManager.saveTargetSettings(settings);
    }

    public int getTargetTimePerQuestionSec() {
        return dbManager.getTargetTimePerQuestionSec();
    }

    public void setTargetTimePerQuestionSec(int sec) {
        dbManager.setTargetTimePerQuestionSec(sec);
    }

    public int getTargetSessionDurationMin() {
        return dbManager.getTargetSessionDurationMin();
    }

    public void setTargetSessionDurationMin(int min) {
        dbManager.setTargetSessionDurationMin(min);
    }

    public int getTargetSessionQuestions() {
        return dbManager.getTargetSessionQuestions();
    }

    public void setTargetSessionQuestions(int count) {
        dbManager.setTargetSessionQuestions(count);
    }

    // =========================================================================
    // Version 4 Settings & Preferences Storage APIs
    // =========================================================================

    public String getTargetDate() {
        return dbManager.getTargetDate();
    }

    public void setTargetDate(String date) {
        dbManager.setTargetDate(date);
    }

    public boolean isTwoZoneEnabled() {
        return dbManager.isTwoZoneEnabled();
    }

    public void setTwoZoneEnabled(boolean enabled) {
        dbManager.setTwoZoneEnabled(enabled);
    }

    public WallpaperPreferences getWallpaperPreferences() {
        return dbManager.getWallpaperPreferences();
    }

    public void saveWallpaperPreferences(WallpaperPreferences prefs) {
        dbManager.saveWallpaperPreferences(prefs);
    }

    public WidgetSettings getWidgetSettings() {
        return dbManager.getWidgetSettings();
    }

    public void saveWidgetSettings(WidgetSettings settings) {
        dbManager.saveWidgetSettings(settings);
    }

    public String getSetting(String key, String defaultValue) {
        return dbManager.getSetting(key, defaultValue);
    }

    public boolean saveSetting(String key, String value) {
        return dbManager.saveSetting(key, value);
    }

    public Map<String, String> getAllSettings() {
        return dbManager.getAllSettings();
    }

    // =========================================================================
    // Version 4 Attempt Audit History APIs
    // =========================================================================

    public boolean logAttemptCorrection(long questionLogId, String newOutcome, String notes) {
        return dbManager.logAttemptCorrection(questionLogId, newOutcome, notes);
    }

    public boolean logAttemptCorrection(long questionLogId, long sessionId,
                                        String originalOutcome, String correctedOutcome,
                                        long timestamp, String notes) {
        return dbManager.logAttemptCorrection(questionLogId, sessionId, originalOutcome, correctedOutcome, timestamp, notes);
    }

    public List<AttemptAuditRecord> getAuditHistoryForQuestion(long questionLogId) {
        return dbManager.getAuditHistoryForQuestion(questionLogId);
    }

    public List<AttemptAuditRecord> getAuditHistoryForSession(long sessionId) {
        return dbManager.getAuditHistoryForSession(sessionId);
    }

    public List<AttemptAuditRecord> getAllAuditHistory() {
        return dbManager.getAllAuditHistory();
    }

    public TargetCompliance computeTargetCompliance(SessionStats session) {
        return dbManager.computeTargetCompliance(session);
    }

    public TargetCompliance computeTargetCompliance(long durationMs, int questionsSolved, long avgTimePerQuestionMs) {
        return dbManager.computeTargetCompliance(durationMs, questionsSolved, avgTimePerQuestionMs);
    }

    // =========================================================================
    // Backup & Restore
    // =========================================================================

    public String exportBackupToJson() throws JSONException {
        return dbManager.exportBackupToJson();
    }

    public int importBackupFromJson(String jsonString) throws JSONException {
        return dbManager.importBackupFromJson(jsonString);
    }

    public int importBackupFromJson(String jsonString, boolean clearExisting) throws JSONException {
        return dbManager.importBackupFromJson(jsonString, clearExisting);
    }

    public void exportBackupToFile(File file) throws IOException, JSONException {
        dbManager.exportBackupToFile(file);
    }

    public int importBackupFromFile(File file, boolean clearExisting) throws IOException, JSONException {
        return dbManager.importBackupFromFile(file, clearExisting);
    }

    // =========================================================================
    // Dual-Clock, Breaks & JEE Operations
    // =========================================================================

    public long recordBreakStart(long sessionId, long startTime) {
        return dbManager.recordBreakStart(sessionId, startTime);
    }

    public long recordBreakEnd(long sessionId, long endTime) {
        return dbManager.recordBreakEnd(sessionId, endTime);
    }

    public List<BreakLog> getBreaksForSession(long sessionId) {
        return dbManager.getBreaksForSession(sessionId);
    }

    public void updateSessionClocks(long sessionId, long totalStudyMs, long totalBreakMs, long activeQuestionElapsedMs) {
        dbManager.updateSessionClocks(sessionId, totalStudyMs, totalBreakMs, activeQuestionElapsedMs);
    }

    public List<ExamProfile> getAllExamProfiles() {
        return dbManager.getAllExamProfiles();
    }

    public ExamProfile getExamProfileByName(String name) {
        return dbManager.getExamProfileByName(name);
    }

    public long createOrUpdateExamProfile(ExamProfile profile) {
        return dbManager.createOrUpdateExamProfile(profile);
    }

    public boolean deleteExamProfile(long id) {
        return dbManager.deleteExamProfile(id);
    }

    public long recordCoachFeedback(String recommendationKey, String feedback, boolean dismissed) {
        return dbManager.recordCoachFeedback(recommendationKey, feedback, dismissed);
    }

    public boolean dismissRecommendation(String recommendationKey) {
        return dbManager.dismissRecommendation(recommendationKey);
    }

    public List<StudyRecommendation> getStudyRecommendations(int targetPaceSeconds, int targetDailyQuestions) {
        return dbManager.getStudyRecommendations(targetPaceSeconds, targetDailyQuestions);
    }

    public JeeAnalyticsReport getJeeAnalytics(String profileName, long startMillis, long endMillis) {
        return dbManager.getJeeAnalytics(profileName, startMillis, endMillis);
    }

    public void exportSafBackup(OutputStream outputStream, JSONObject preferences) throws IOException, JSONException {
        dbManager.exportSafBackup(outputStream, preferences);
    }

    public SafBackupEngine.ValidationResult validateSafBackup(InputStream inputStream) {
        return dbManager.validateSafBackup(inputStream);
    }

    public SafBackupEngine.RestoreResult restoreSafBackup(InputStream inputStream, SafBackupEngine.RestoreMode mode) throws IOException, JSONException {
        return dbManager.restoreSafBackup(inputStream, mode);
    }
}

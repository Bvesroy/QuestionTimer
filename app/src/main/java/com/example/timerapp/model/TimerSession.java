package com.example.timerapp.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Represents a question-solving session with target, state, and question timing logs.
 */
public class TimerSession {
    private long id;
    private String name;
    private int target; // 0 = no target
    private int currentQuestion; // 1-based index currently being answered
    private String status;
    private long createdAt;
    private long updatedAt;
    private String subject = "General";
    private String notes = "";
    private String whatWentWell = "";
    private String needsImprovement = "";
    private String profileId = "default";
    private final List<QuestionLog> logs;

    // Version 3 dual-clock & profile fields
    private String examProfile = "PRACTICE";
    private long totalStudyDurationMs = 0L;
    private long totalBreakDurationMs = 0L;
    private boolean isMockTest = false;
    private long currentBreakStartTime = 0L;
    private long activeQuestionElapsedMs = 0L;
    private final List<com.example.timerapp.data.model.BreakLog> breaks = new ArrayList<>();

    public TimerSession(long id, String name, int target) {
        this.id = id;
        this.name = (name == null || name.trim().isEmpty()) ? "DEFAULT SESSION" : name.toUpperCase(Locale.ROOT);
        this.target = Math.max(0, target);
        this.currentQuestion = 1;
        this.status = "CREATED";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
        this.logs = new ArrayList<>();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = (name == null || name.trim().isEmpty()) ? "UNTITLED SESSION" : name.toUpperCase(Locale.ROOT);
        this.updatedAt = System.currentTimeMillis();
    }

    public int getTarget() {
        return target;
    }

    public void setTarget(int target) {
        this.target = Math.max(0, target);
        this.updatedAt = System.currentTimeMillis();
    }

    public int getCurrentQuestion() {
        return currentQuestion;
    }

    public void setCurrentQuestion(int currentQuestion) {
        this.currentQuestion = Math.max(1, currentQuestion);
        this.updatedAt = System.currentTimeMillis();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getSubject() {
        return (subject == null || subject.trim().isEmpty()) ? "General" : subject;
    }

    public void setSubject(String subject) {
        this.subject = (subject == null || subject.trim().isEmpty()) ? "General" : subject;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getNotes() {
        return (notes == null) ? "" : notes;
    }

    public void setNotes(String notes) {
        this.notes = (notes == null) ? "" : notes;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getWhatWentWell() {
        return (whatWentWell == null) ? "" : whatWentWell;
    }

    public void setWhatWentWell(String whatWentWell) {
        this.whatWentWell = (whatWentWell == null) ? "" : whatWentWell;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getWentWell() {
        return getWhatWentWell();
    }

    public void setWentWell(String wentWell) {
        setWhatWentWell(wentWell);
    }

    public String getNeedsImprovement() {
        return (needsImprovement == null) ? "" : needsImprovement;
    }

    public void setNeedsImprovement(String needsImprovement) {
        this.needsImprovement = (needsImprovement == null) ? "" : needsImprovement;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getProfileId() {
        return (profileId == null || profileId.trim().isEmpty()) ? "default" : profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = (profileId == null || profileId.trim().isEmpty()) ? "default" : profileId;
        this.updatedAt = System.currentTimeMillis();
    }

    public List<QuestionLog> getLogs() {
        return Collections.unmodifiableList(logs);
    }

    public void addLog(QuestionLog log) {
        if (log != null) {
            logs.add(log);
            this.currentQuestion = logs.size() + 1;
            this.updatedAt = System.currentTimeMillis();
        }
    }

    public void replaceLog(int index, QuestionLog newLog) {
        if (index >= 0 && index < logs.size() && newLog != null) {
            logs.set(index, newLog);
            this.updatedAt = System.currentTimeMillis();
        }
    }

    public void clearLogs() {
        logs.clear();
        this.currentQuestion = 1;
        this.updatedAt = System.currentTimeMillis();
    }

    public int getCompletedCount() {
        return logs.size();
    }

    public long getTotalDurationMs() {
        long total = 0;
        for (QuestionLog log : logs) {
            total += log.getDurationMs();
        }
        return total;
    }

    public long getAverageDurationMs() {
        if (logs.isEmpty()) return 0;
        return getTotalDurationMs() / logs.size();
    }

    public QuestionLog getFastestLog() {
        if (logs.isEmpty()) return null;
        QuestionLog fastest = logs.get(0);
        for (QuestionLog log : logs) {
            if (log.getDurationMs() < fastest.getDurationMs()) {
                fastest = log;
            }
        }
        return fastest;
    }

    public QuestionLog getSlowestLog() {
        if (logs.isEmpty()) return null;
        QuestionLog slowest = logs.get(0);
        for (QuestionLog log : logs) {
            if (log.getDurationMs() > slowest.getDurationMs()) {
                slowest = log;
            }
        }
        return slowest;
    }

    /**
     * Formats total duration as H:MM:SS or MM:SS
     */
    public String getFormattedTotalDuration() {
        long totalSeconds = getTotalDurationMs() / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
        }
    }

    /**
     * Formats average duration as MM:SS.s
     */
    public String getFormattedAverageDuration() {
        long avg = getAverageDurationMs();
        long totalSeconds = avg / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long tenths = (avg % 1000) / 100;
        return String.format(Locale.ROOT, "%02d:%02d.%d", minutes, seconds, tenths);
    }

    public String getExamProfile() {
        return (examProfile == null || examProfile.trim().isEmpty()) ? "PRACTICE" : examProfile;
    }

    public void setExamProfile(String examProfile) {
        this.examProfile = (examProfile == null || examProfile.trim().isEmpty()) ? "PRACTICE" : examProfile;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getTotalStudyDurationMs() {
        return totalStudyDurationMs;
    }

    public void setTotalStudyDurationMs(long totalStudyDurationMs) {
        this.totalStudyDurationMs = Math.max(0, totalStudyDurationMs);
        this.updatedAt = System.currentTimeMillis();
    }

    public long getTotalBreakDurationMs() {
        return totalBreakDurationMs;
    }

    public void setTotalBreakDurationMs(long totalBreakDurationMs) {
        this.totalBreakDurationMs = Math.max(0, totalBreakDurationMs);
        this.updatedAt = System.currentTimeMillis();
    }

    public boolean isMockTest() {
        return isMockTest;
    }

    public void setMockTest(boolean mockTest) {
        isMockTest = mockTest;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCurrentBreakStartTime() {
        return currentBreakStartTime;
    }

    public void setCurrentBreakStartTime(long currentBreakStartTime) {
        this.currentBreakStartTime = currentBreakStartTime;
    }

    public boolean isOnBreak() {
        return currentBreakStartTime > 0;
    }

    public long getActiveQuestionElapsedMs() {
        return activeQuestionElapsedMs;
    }

    public void setActiveQuestionElapsedMs(long activeQuestionElapsedMs) {
        this.activeQuestionElapsedMs = Math.max(0, activeQuestionElapsedMs);
    }

    public List<com.example.timerapp.data.model.BreakLog> getBreaks() {
        return Collections.unmodifiableList(breaks);
    }

    public void addBreak(com.example.timerapp.data.model.BreakLog breakLog) {
        if (breakLog != null) {
            breaks.add(breakLog);
            if (breakLog.getDurationMs() > 0) {
                totalBreakDurationMs += breakLog.getDurationMs();
            }
            this.updatedAt = System.currentTimeMillis();
        }
    }

    public void clearBreaks() {
        breaks.clear();
        totalBreakDurationMs = 0L;
        currentBreakStartTime = 0L;
    }

    public String getFormattedStudyDuration() {
        long totalSeconds = totalStudyDurationMs / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
        }
    }

    public String getFormattedBreakDuration() {
        long totalSeconds = totalBreakDurationMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }
}

package com.example.timerapp.data.model;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

/**
 * Model representing an individual solved question entry logged in a session.
 */
public class QuestionLog implements Serializable {
    private static final long serialVersionUID = 2L;

    private long id;
    private long sessionId;
    private int questionNum;
    private long durationMs;
    private long completedAt;

    // Version 3 extended fields
    private String label;
    private String topic;
    private String difficulty;
    private String outcome;
    private int confidence;
    private String mistakeTag;
    private String notes;

    // Version 4 extended audit fields
    private String originalOutcome;
    private long correctionTimestamp;

    public QuestionLog() {
        this.label = "";
        this.topic = "";
        this.difficulty = "MEDIUM";
        this.outcome = "SOLVED";
        this.confidence = 3;
        this.mistakeTag = "";
        this.notes = "";
        this.originalOutcome = "";
        this.correctionTimestamp = 0L;
    }

    public QuestionLog(long id, long sessionId, int questionNum, long durationMs, long completedAt) {
        this(id, sessionId, questionNum, durationMs, completedAt, "", "", "MEDIUM", "SOLVED", 3, "", "");
    }

    public QuestionLog(long id, long sessionId, int questionNum, long durationMs, long completedAt,
                       String label, String topic, String difficulty, String outcome,
                       int confidence, String mistakeTag, String notes) {
        this(id, sessionId, questionNum, durationMs, completedAt, label, topic, difficulty, outcome, confidence, mistakeTag, notes, "", 0L);
    }

    public QuestionLog(long id, long sessionId, int questionNum, long durationMs, long completedAt,
                       String label, String topic, String difficulty, String outcome,
                       int confidence, String mistakeTag, String notes,
                       String originalOutcome, long correctionTimestamp) {
        this.id = id;
        this.sessionId = sessionId;
        this.questionNum = questionNum;
        this.durationMs = durationMs;
        this.completedAt = completedAt;
        this.label = (label != null) ? label : "";
        this.topic = (topic != null) ? topic : "";
        this.difficulty = (difficulty != null) ? difficulty : "MEDIUM";
        this.outcome = (outcome != null) ? outcome : "SOLVED";
        this.confidence = confidence;
        this.mistakeTag = (mistakeTag != null) ? mistakeTag : "";
        this.notes = (notes != null) ? notes : "";
        this.originalOutcome = (originalOutcome != null) ? originalOutcome : "";
        this.correctionTimestamp = correctionTimestamp;
    }

    public QuestionLog(long sessionId, int questionNum, long durationMs) {
        this(0, sessionId, questionNum, durationMs, System.currentTimeMillis());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public int getQuestionNum() {
        return questionNum;
    }

    /**
     * Alias for getQuestionNum() for UI compatibility.
     */
    public int getQuestionNumber() {
        return questionNum;
    }

    public void setQuestionNum(int questionNum) {
        this.questionNum = questionNum;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public long getCompletedAt() {
        return completedAt;
    }

    /**
     * Alias for getCompletedAt() for UI compatibility.
     */
    public long getTimestamp() {
        return completedAt;
    }

    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }

    public String getLabel() {
        return label != null ? label : "";
    }

    public void setLabel(String label) {
        this.label = (label != null) ? label : "";
    }

    public String getTopic() {
        return topic != null ? topic : "";
    }

    public void setTopic(String topic) {
        this.topic = (topic != null) ? topic : "";
    }

    public String getDifficulty() {
        return difficulty != null ? difficulty : "MEDIUM";
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = (difficulty != null) ? difficulty : "MEDIUM";
    }

    public String getOutcome() {
        return outcome != null ? outcome : "SOLVED";
    }

    public void setOutcome(String outcome) {
        this.outcome = (outcome != null) ? outcome : "SOLVED";
    }

    public int getConfidence() {
        return confidence;
    }

    public void setConfidence(int confidence) {
        this.confidence = confidence;
    }

    public String getMistakeTag() {
        return mistakeTag != null ? mistakeTag : "";
    }

    public void setMistakeTag(String mistakeTag) {
        this.mistakeTag = (mistakeTag != null) ? mistakeTag : "";
    }

    public String getNotes() {
        return notes != null ? notes : "";
    }

    public void setNotes(String notes) {
        this.notes = (notes != null) ? notes : "";
    }

    public String getOriginalOutcome() {
        return originalOutcome != null ? originalOutcome : "";
    }

    public void setOriginalOutcome(String originalOutcome) {
        this.originalOutcome = (originalOutcome != null) ? originalOutcome : "";
    }

    public long getCorrectionTimestamp() {
        return correctionTimestamp;
    }

    public void setCorrectionTimestamp(long correctionTimestamp) {
        this.correctionTimestamp = correctionTimestamp;
    }

    /**
     * Formats duration as MM:SS.s (e.g. 02:14.3)
     */
    public String getFormattedDuration() {
        long totalSeconds = durationMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long tenths = (durationMs % 1000) / 100;
        return String.format(Locale.ROOT, "%02d:%02d.%d", minutes, seconds, tenths);
    }

    /**
     * Formats duration as MM:SS (e.g. 02:14)
     */
    public String getFormattedDurationMinutesSeconds() {
        long totalSeconds = durationMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    /**
     * Converts to com.example.timerapp.model.QuestionLog.
     */
    public com.example.timerapp.model.QuestionLog toUiModel() {
        return new com.example.timerapp.model.QuestionLog(
                id, sessionId, questionNum, durationMs, completedAt,
                label, topic, difficulty, outcome, confidence, mistakeTag, notes,
                originalOutcome, correctionTimestamp
        );
    }

    /**
     * Converts from com.example.timerapp.model.QuestionLog.
     */
    public static QuestionLog fromUiModel(com.example.timerapp.model.QuestionLog uiLog) {
        if (uiLog == null) return null;
        return new QuestionLog(
                uiLog.getId(), uiLog.getSessionId(), uiLog.getQuestionNumber(),
                uiLog.getDurationMs(), uiLog.getTimestamp(),
                uiLog.getLabel(), uiLog.getTopic(), uiLog.getDifficulty(),
                uiLog.getOutcome(), uiLog.getConfidence(), uiLog.getMistakeTag(), uiLog.getNotes(),
                uiLog.getOriginalOutcome(), uiLog.getCorrectionTimestamp()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuestionLog that = (QuestionLog) o;
        return id == that.id &&
                sessionId == that.sessionId &&
                questionNum == that.questionNum &&
                durationMs == that.durationMs &&
                completedAt == that.completedAt &&
                confidence == that.confidence &&
                Objects.equals(label, that.label) &&
                Objects.equals(topic, that.topic) &&
                Objects.equals(difficulty, that.difficulty) &&
                Objects.equals(outcome, that.outcome) &&
                Objects.equals(mistakeTag, that.mistakeTag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sessionId, questionNum, durationMs, completedAt, topic, difficulty, outcome);
    }

    @Override
    public String toString() {
        return "QuestionLog{" +
                "id=" + id +
                ", sessionId=" + sessionId +
                ", questionNum=" + questionNum +
                ", durationMs=" + durationMs +
                ", completedAt=" + completedAt +
                ", topic='" + topic + '\'' +
                ", outcome='" + outcome + '\'' +
                '}';
    }
}

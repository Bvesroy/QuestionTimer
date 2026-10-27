package com.example.timerapp.model;

import java.util.Locale;

/**
 * Timing record for a single question attempt with subject, difficulty, outcome, and mistake categorization.
 */
public class QuestionLog {
    private final long id;
    private final long sessionId;
    private final int questionNumber;
    private final long durationMs;
    private final long timestamp;

    // Version 3 extended fields
    private final String label;
    private final String topic;
    private final String difficulty;
    private final String outcome;
    private final int confidence;
    private final String mistakeTag;
    private final String notes;

    // Version 4 extended audit fields
    private final String originalOutcome;
    private final long correctionTimestamp;

    public QuestionLog(long id, long sessionId, int questionNumber, long durationMs, long timestamp) {
        this(id, sessionId, questionNumber, durationMs, timestamp, "", "", "MEDIUM", "SOLVED", 3, "", "");
    }

    public QuestionLog(long id, long sessionId, int questionNumber, long durationMs, long timestamp,
                       String label, String topic, String difficulty, String outcome,
                       int confidence, String mistakeTag, String notes) {
        this(id, sessionId, questionNumber, durationMs, timestamp, label, topic, difficulty, outcome, confidence, mistakeTag, notes, "", 0L);
    }

    public QuestionLog(long id, long sessionId, int questionNumber, long durationMs, long timestamp,
                       String label, String topic, String difficulty, String outcome,
                       int confidence, String mistakeTag, String notes,
                       String originalOutcome, long correctionTimestamp) {
        this.id = id;
        this.sessionId = sessionId;
        this.questionNumber = questionNumber;
        this.durationMs = durationMs;
        this.timestamp = timestamp;
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

    public long getId() {
        return id;
    }

    public long getSessionId() {
        return sessionId;
    }

    public int getQuestionNumber() {
        return questionNumber;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getLabel() {
        return label;
    }

    public String getTopic() {
        return topic;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getOutcome() {
        return outcome;
    }

    public int getConfidence() {
        return confidence;
    }

    public String getMistakeTag() {
        return mistakeTag;
    }

    public String getNotes() {
        return notes;
    }

    public String getOriginalOutcome() {
        return originalOutcome != null ? originalOutcome : "";
    }

    public long getCorrectionTimestamp() {
        return correctionTimestamp;
    }

    public QuestionLog withOutcome(String newOutcome) {
        String orig = (this.originalOutcome != null && !this.originalOutcome.isEmpty())
                ? this.originalOutcome : this.outcome;
        return new QuestionLog(this.id, this.sessionId, this.questionNumber, this.durationMs, this.timestamp,
                this.label, this.topic, this.difficulty, newOutcome, this.confidence, this.mistakeTag, this.notes,
                orig, System.currentTimeMillis());
    }

    public QuestionLog withTags(String outcome, String difficulty, int confidence, String mistakeTag, String notes) {
        return new QuestionLog(this.id, this.sessionId, this.questionNumber, this.durationMs, this.timestamp,
                this.label, this.topic, difficulty, outcome, confidence, mistakeTag, notes,
                this.originalOutcome, this.correctionTimestamp);
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
}

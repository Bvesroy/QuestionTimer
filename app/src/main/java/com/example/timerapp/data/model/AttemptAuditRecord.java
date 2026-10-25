package com.example.timerapp.data.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Encapsulates an audit record for a question attempt outcome correction.
 * Tracks the original outcome, corrected outcome, correction timestamp, and optional notes.
 */
public class AttemptAuditRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private long questionLogId;
    private long sessionId;
    private String originalOutcome;
    private String correctedOutcome;
    private long correctionTimestamp;
    private String notes;

    public AttemptAuditRecord() {
        this.originalOutcome = "";
        this.correctedOutcome = "";
        this.notes = "";
    }

    public AttemptAuditRecord(long id, long questionLogId, long sessionId,
                              String originalOutcome, String correctedOutcome,
                              long correctionTimestamp, String notes) {
        this.id = id;
        this.questionLogId = questionLogId;
        this.sessionId = sessionId;
        this.originalOutcome = (originalOutcome != null) ? originalOutcome : "";
        this.correctedOutcome = (correctedOutcome != null) ? correctedOutcome : "";
        this.correctionTimestamp = correctionTimestamp;
        this.notes = (notes != null) ? notes : "";
    }

    public AttemptAuditRecord(long questionLogId, long sessionId,
                              String originalOutcome, String correctedOutcome,
                              long correctionTimestamp, String notes) {
        this(0L, questionLogId, sessionId, originalOutcome, correctedOutcome, correctionTimestamp, notes);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getQuestionLogId() {
        return questionLogId;
    }

    public void setQuestionLogId(long questionLogId) {
        this.questionLogId = questionLogId;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public String getOriginalOutcome() {
        return originalOutcome != null ? originalOutcome : "";
    }

    public void setOriginalOutcome(String originalOutcome) {
        this.originalOutcome = (originalOutcome != null) ? originalOutcome : "";
    }

    public String getCorrectedOutcome() {
        return correctedOutcome != null ? correctedOutcome : "";
    }

    public void setCorrectedOutcome(String correctedOutcome) {
        this.correctedOutcome = (correctedOutcome != null) ? correctedOutcome : "";
    }

    public long getCorrectionTimestamp() {
        return correctionTimestamp;
    }

    public void setCorrectionTimestamp(long correctionTimestamp) {
        this.correctionTimestamp = correctionTimestamp;
    }

    public String getNotes() {
        return notes != null ? notes : "";
    }

    public void setNotes(String notes) {
        this.notes = (notes != null) ? notes : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttemptAuditRecord that = (AttemptAuditRecord) o;
        return id == that.id &&
                questionLogId == that.questionLogId &&
                sessionId == that.sessionId &&
                correctionTimestamp == that.correctionTimestamp &&
                Objects.equals(originalOutcome, that.originalOutcome) &&
                Objects.equals(correctedOutcome, that.correctedOutcome) &&
                Objects.equals(notes, that.notes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, questionLogId, sessionId, originalOutcome, correctedOutcome, correctionTimestamp, notes);
    }

    @Override
    public String toString() {
        return "AttemptAuditRecord{" +
                "id=" + id +
                ", questionLogId=" + questionLogId +
                ", sessionId=" + sessionId +
                ", originalOutcome='" + originalOutcome + '\'' +
                ", correctedOutcome='" + correctedOutcome + '\'' +
                ", correctionTimestamp=" + correctionTimestamp +
                ", notes='" + notes + '\'' +
                '}';
    }
}

package com.example.timerapp.data.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Model representing a study session.
 * Stores target question count, creation/update timestamps, active status,
 * subject metadata, reflection notes, status, and profile ID.
 */
public class Session implements Serializable {
    private static final long serialVersionUID = 2L;

    private long id;
    private String name;
    private int targetCount;
    private long createdAt;
    private long updatedAt;
    private boolean isActive;

    private String subject;
    private String notes;
    private String wentWell;
    private String needsImprovement;
    private String status;
    private String profileId;

    // Version 3 extended columns
    private String examProfile;
    private long totalStudyDurationMs;
    private long totalBreakDurationMs;
    private boolean isMockTest;

    public Session() {
        this.subject = "General";
        this.notes = "";
        this.wentWell = "";
        this.needsImprovement = "";
        this.status = "COMPLETED";
        this.profileId = "default";
        this.examProfile = "PRACTICE";
        this.totalStudyDurationMs = 0L;
        this.totalBreakDurationMs = 0L;
        this.isMockTest = false;
    }

    public Session(long id, String name, int targetCount, long createdAt, long updatedAt, boolean isActive) {
        this(id, name, targetCount, createdAt, updatedAt, isActive, "General", "", "", "", "COMPLETED", "default", "PRACTICE", 0L, 0L, false);
    }

    public Session(long id, String name, int targetCount, long createdAt, long updatedAt, boolean isActive,
                   String subject, String notes, String wentWell, String needsImprovement, String status, String profileId) {
        this(id, name, targetCount, createdAt, updatedAt, isActive, subject, notes, wentWell, needsImprovement, status, profileId, "PRACTICE", 0L, 0L, false);
    }

    public Session(long id, String name, int targetCount, long createdAt, long updatedAt, boolean isActive,
                   String subject, String notes, String wentWell, String needsImprovement, String status, String profileId,
                   String examProfile, long totalStudyDurationMs, long totalBreakDurationMs, boolean isMockTest) {
        this.id = id;
        this.name = name;
        this.targetCount = targetCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isActive = isActive;
        this.subject = (subject != null && !subject.trim().isEmpty()) ? subject.trim() : "General";
        this.notes = (notes != null) ? notes : "";
        this.wentWell = (wentWell != null) ? wentWell : "";
        this.needsImprovement = (needsImprovement != null) ? needsImprovement : "";
        this.status = (status != null && !status.trim().isEmpty()) ? status.trim() : "COMPLETED";
        this.profileId = (profileId != null && !profileId.trim().isEmpty()) ? profileId.trim() : "default";
        this.examProfile = (examProfile != null && !examProfile.trim().isEmpty()) ? examProfile.trim() : "PRACTICE";
        this.totalStudyDurationMs = totalStudyDurationMs;
        this.totalBreakDurationMs = totalBreakDurationMs;
        this.isMockTest = isMockTest;
    }

    public Session(String name, int targetCount, boolean isActive) {
        this(0, name, targetCount, System.currentTimeMillis(), System.currentTimeMillis(), isActive);
    }

    public Session(String name, int targetCount) {
        this(name, targetCount, false);
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
        this.name = name;
    }

    public int getTargetCount() {
        return targetCount;
    }

    public void setTargetCount(int targetCount) {
        this.targetCount = targetCount;
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

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getSubject() {
        return subject != null ? subject : "General";
    }

    public void setSubject(String subject) {
        this.subject = (subject != null && !subject.trim().isEmpty()) ? subject.trim() : "General";
    }

    public String getNotes() {
        return notes != null ? notes : "";
    }

    public void setNotes(String notes) {
        this.notes = (notes != null) ? notes : "";
    }

    public String getWentWell() {
        return wentWell != null ? wentWell : "";
    }

    public void setWentWell(String wentWell) {
        this.wentWell = (wentWell != null) ? wentWell : "";
    }

    public String getNeedsImprovement() {
        return needsImprovement != null ? needsImprovement : "";
    }

    public void setNeedsImprovement(String needsImprovement) {
        this.needsImprovement = (needsImprovement != null) ? needsImprovement : "";
    }

    public String getStatus() {
        return status != null ? status : "COMPLETED";
    }

    public void setStatus(String status) {
        this.status = (status != null && !status.trim().isEmpty()) ? status.trim() : "COMPLETED";
    }

    public String getProfileId() {
        return profileId != null ? profileId : "default";
    }

    public void setProfileId(String profileId) {
        this.profileId = (profileId != null && !profileId.trim().isEmpty()) ? profileId.trim() : "default";
    }

    public String getExamProfile() {
        return examProfile != null ? examProfile : "PRACTICE";
    }

    public void setExamProfile(String examProfile) {
        this.examProfile = (examProfile != null && !examProfile.trim().isEmpty()) ? examProfile.trim() : "PRACTICE";
    }

    public long getTotalStudyDurationMs() {
        return totalStudyDurationMs;
    }

    public void setTotalStudyDurationMs(long totalStudyDurationMs) {
        this.totalStudyDurationMs = Math.max(0, totalStudyDurationMs);
    }

    public long getTotalBreakDurationMs() {
        return totalBreakDurationMs;
    }

    public void setTotalBreakDurationMs(long totalBreakDurationMs) {
        this.totalBreakDurationMs = Math.max(0, totalBreakDurationMs);
    }

    public boolean isMockTest() {
        return isMockTest;
    }

    public void setMockTest(boolean mockTest) {
        isMockTest = mockTest;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Session session = (Session) o;
        return id == session.id &&
                targetCount == session.targetCount &&
                createdAt == session.createdAt &&
                updatedAt == session.updatedAt &&
                isActive == session.isActive &&
                Objects.equals(name, session.name) &&
                Objects.equals(subject, session.subject) &&
                Objects.equals(status, session.status) &&
                Objects.equals(profileId, session.profileId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, targetCount, createdAt, updatedAt, isActive, subject, status, profileId);
    }

    @Override
    public String toString() {
        return "Session{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", subject='" + subject + '\'' +
                ", targetCount=" + targetCount +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", isActive=" + isActive +
                ", status='" + status + '\'' +
                '}';
    }
}

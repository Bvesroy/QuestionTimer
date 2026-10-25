package com.example.timerapp.data.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Result data payload when resuming a session.
 * Contains session details, progress count, the next question number to solve,
 * cumulative time, average time per question, and the list of previous question timings.
 */
public class SessionResumeData implements Serializable {
    private static final long serialVersionUID = 1L;

    private Session session;
    private int solvedCount;
    private int nextQuestionNum;
    private long totalElapsedMs;
    private long averageDurationMs;
    private List<QuestionLog> questionLogs;

    public SessionResumeData() {
        this.questionLogs = Collections.emptyList();
    }

    public SessionResumeData(Session session, int solvedCount, int nextQuestionNum,
                             long totalElapsedMs, long averageDurationMs,
                             List<QuestionLog> questionLogs) {
        this.session = session;
        this.solvedCount = solvedCount;
        this.nextQuestionNum = nextQuestionNum;
        this.totalElapsedMs = totalElapsedMs;
        this.averageDurationMs = averageDurationMs;
        this.questionLogs = (questionLogs != null) ? questionLogs : Collections.emptyList();
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public int getSolvedCount() {
        return solvedCount;
    }

    public void setSolvedCount(int solvedCount) {
        this.solvedCount = solvedCount;
    }

    public int getNextQuestionNum() {
        return nextQuestionNum;
    }

    public void setNextQuestionNum(int nextQuestionNum) {
        this.nextQuestionNum = nextQuestionNum;
    }

    public long getTotalElapsedMs() {
        return totalElapsedMs;
    }

    public void setTotalElapsedMs(long totalElapsedMs) {
        this.totalElapsedMs = totalElapsedMs;
    }

    public long getAverageDurationMs() {
        return averageDurationMs;
    }

    public void setAverageDurationMs(long averageDurationMs) {
        this.averageDurationMs = averageDurationMs;
    }

    public List<QuestionLog> getQuestionLogs() {
        return questionLogs;
    }

    public void setQuestionLogs(List<QuestionLog> questionLogs) {
        this.questionLogs = (questionLogs != null) ? questionLogs : Collections.emptyList();
    }

    public String getFormattedTotalElapsed() {
        return SessionStats.formatDuration(totalElapsedMs);
    }

    public String getFormattedAverageDuration() {
        return SessionStats.formatDuration(averageDurationMs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SessionResumeData that = (SessionResumeData) o;
        return solvedCount == that.solvedCount &&
                nextQuestionNum == that.nextQuestionNum &&
                totalElapsedMs == that.totalElapsedMs &&
                averageDurationMs == that.averageDurationMs &&
                Objects.equals(session, that.session) &&
                Objects.equals(questionLogs, that.questionLogs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(session, solvedCount, nextQuestionNum, totalElapsedMs, averageDurationMs, questionLogs);
    }

    @Override
    public String toString() {
        return "SessionResumeData{" +
                "session=" + session +
                ", solvedCount=" + solvedCount +
                ", nextQuestionNum=" + nextQuestionNum +
                ", totalElapsedMs=" + totalElapsedMs +
                ", averageDurationMs=" + averageDurationMs +
                ", questionLogsCount=" + (questionLogs != null ? questionLogs.size() : 0) +
                '}';
    }
}

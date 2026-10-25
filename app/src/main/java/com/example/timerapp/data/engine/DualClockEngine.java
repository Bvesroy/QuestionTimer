package com.example.timerapp.data.engine;

import com.example.timerapp.data.model.BreakLog;
import com.example.timerapp.data.model.QuestionLog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pure Java Dual-Clock & Session Timing Engine.
 * Manages:
 * 1. Active Question Clock (tracks current question duration, resets without affecting total study clock).
 * 2. Total Study Clock (continuity across question boundaries, excluding recorded breaks).
 * 3. Accidental Reset Recovery (Undo support preserving discarded elapsed time).
 * 4. Explicit Break Exclusion (preserves active question clock while deducting break time from study time).
 * 5. Drift-free high-precision accumulation for multi-hour sessions.
 */
public class DualClockEngine implements Serializable {
    private static final long serialVersionUID = 1L;

    private long sessionId;
    private long currentQuestionElapsedMs;
    private long lastDiscardedQuestionElapsedMs;
    private boolean canUndoReset;

    private long completedQuestionsTotalDurationMs;
    private long totalBreakDurationMs;

    private final List<QuestionLog> questionLogs = new ArrayList<>();
    private final List<BreakLog> breakLogs = new ArrayList<>();

    private BreakLog activeBreak = null;
    private boolean isPaused = false;

    public DualClockEngine(long sessionId) {
        this.sessionId = sessionId;
        this.currentQuestionElapsedMs = 0L;
        this.lastDiscardedQuestionElapsedMs = 0L;
        this.canUndoReset = false;
        this.completedQuestionsTotalDurationMs = 0L;
        this.totalBreakDurationMs = 0L;
    }

    /**
     * Advances current question clock by deltaMs.
     */
    public synchronized void tickQuestion(long deltaMs) {
        if (!isPaused && activeBreak == null && deltaMs > 0) {
            currentQuestionElapsedMs += deltaMs;
        }
    }

    /**
     * Resets the active question timer to 0 without saving.
     * Preserves previous question logs, session timer, and Total Study Clock.
     * Records discarded time to enable accidental reset undo.
     */
    public synchronized void resetCurrentQuestion() {
        this.lastDiscardedQuestionElapsedMs = this.currentQuestionElapsedMs;
        this.currentQuestionElapsedMs = 0L;
        this.canUndoReset = true;
    }

    /**
     * Restores the question timer after an accidental reset.
     * Returns true if undo was successful.
     */
    public synchronized boolean undoResetCurrentQuestion() {
        if (canUndoReset) {
            this.currentQuestionElapsedMs = this.lastDiscardedQuestionElapsedMs;
            this.lastDiscardedQuestionElapsedMs = 0L;
            this.canUndoReset = false;
            return true;
        }
        return false;
    }

    /**
     * Commits current question solve time to logs, advances completed study duration,
     * and resets question clock to 0 for next question.
     */
    public synchronized QuestionLog commitCurrentQuestion() {
        int nextQNum = questionLogs.size() + 1;
        long duration = currentQuestionElapsedMs;

        QuestionLog log = new QuestionLog(
                (long) nextQNum,
                sessionId,
                nextQNum,
                duration,
                System.currentTimeMillis()
        );

        questionLogs.add(log);
        completedQuestionsTotalDurationMs += duration;

        currentQuestionElapsedMs = 0L;
        lastDiscardedQuestionElapsedMs = 0L;
        canUndoReset = false;

        return log;
    }

    /**
     * Starts an explicit break at given timestamp.
     * Active question timer is preserved in paused state.
     */
    public synchronized void startBreak(long timestamp) {
        if (activeBreak == null) {
            activeBreak = new BreakLog(sessionId, timestamp, 0L);
            isPaused = true;
        }
    }

    /**
     * Ends active break at given timestamp.
     */
    public synchronized void endBreak(long timestamp) {
        if (activeBreak != null) {
            activeBreak.setEndTime(timestamp);
            breakLogs.add(activeBreak);
            totalBreakDurationMs += activeBreak.getDurationMs();
            activeBreak = null;
            isPaused = false;
        }
    }

    /**
     * Total Study Clock (Net Study Time):
     * Cumulative completed questions + active question time.
     * Breaks are explicitly excluded.
     */
    public synchronized long getTotalStudyClockMs() {
        return completedQuestionsTotalDurationMs + currentQuestionElapsedMs;
    }

    /**
     * Gross session time including both study time and breaks.
     */
    public synchronized long getGrossSessionDurationMs() {
        long currentBreakMs = (activeBreak != null && activeBreak.isActive())
                ? (System.currentTimeMillis() - activeBreak.getStartTime())
                : 0L;
        return getTotalStudyClockMs() + totalBreakDurationMs + currentBreakMs;
    }

    public synchronized long getCurrentQuestionElapsedMs() {
        return currentQuestionElapsedMs;
    }

    public synchronized void setCurrentQuestionElapsedMs(long ms) {
        this.currentQuestionElapsedMs = Math.max(0, ms);
    }

    public synchronized long getLastDiscardedQuestionElapsedMs() {
        return lastDiscardedQuestionElapsedMs;
    }

    public synchronized boolean canUndoReset() {
        return canUndoReset;
    }

    public synchronized long getCompletedQuestionsTotalDurationMs() {
        return completedQuestionsTotalDurationMs;
    }

    public synchronized long getTotalBreakDurationMs() {
        return totalBreakDurationMs;
    }

    public synchronized List<QuestionLog> getQuestionLogs() {
        return Collections.unmodifiableList(questionLogs);
    }

    public synchronized List<BreakLog> getBreakLogs() {
        return Collections.unmodifiableList(breakLogs);
    }

    public synchronized boolean hasActiveBreak() {
        return activeBreak != null;
    }

    public synchronized boolean isPaused() {
        return isPaused;
    }

    public synchronized void setPaused(boolean paused) {
        this.isPaused = paused;
    }
}

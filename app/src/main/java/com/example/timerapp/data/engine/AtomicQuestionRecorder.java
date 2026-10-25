package com.example.timerapp.data.engine;

import com.example.timerapp.data.model.QuestionLog;
import com.example.timerapp.model.QuestionOutcome;
import com.example.timerapp.model.SessionState;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Atomic Question Recording and Undo Engine.
 * Guarantees transaction-safe atomic recording of question outcomes and multi-level Undo.
 * Prevents partial state writes or corrupted question counters on errors.
 */
public class AtomicQuestionRecorder implements Serializable {
    private static final long serialVersionUID = 1L;

    public static class RecordingSnapshot implements Serializable {
        private static final long serialVersionUID = 1L;
        public final int questionNum;
        public final long questionDurationMs;
        public final long totalStudyMs;
        public final SessionState previousState;
        public final QuestionLog committedLog;

        public RecordingSnapshot(int questionNum, long questionDurationMs, long totalStudyMs,
                                 SessionState previousState, QuestionLog committedLog) {
            this.questionNum = questionNum;
            this.questionDurationMs = questionDurationMs;
            this.totalStudyMs = totalStudyMs;
            this.previousState = previousState;
            this.committedLog = committedLog;
        }
    }

    private final long sessionId;
    private int currentQuestionIndex = 1;
    private long currentQuestionElapsedMs = 0L;
    private long totalStudyDurationMs = 0L;

    private final List<QuestionLog> recordedLogs = new ArrayList<>();
    private final Deque<RecordingSnapshot> undoStack = new ArrayDeque<>();
    private final int maxUndoHistory;

    public AtomicQuestionRecorder(long sessionId, int maxUndoHistory) {
        this.sessionId = sessionId;
        this.maxUndoHistory = Math.max(1, maxUndoHistory);
    }

    public AtomicQuestionRecorder(long sessionId) {
        this(sessionId, 20);
    }

    public synchronized int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    public synchronized long getCurrentQuestionElapsedMs() {
        return currentQuestionElapsedMs;
    }

    public synchronized void tickQuestion(long deltaMs) {
        if (deltaMs > 0) {
            currentQuestionElapsedMs += deltaMs;
        }
    }

    public synchronized long getTotalStudyDurationMs() {
        return totalStudyDurationMs + currentQuestionElapsedMs;
    }

    public synchronized List<QuestionLog> getRecordedLogs() {
        return Collections.unmodifiableList(new ArrayList<>(recordedLogs));
    }

    public synchronized int getUndoStackDepth() {
        return undoStack.size();
    }

    public synchronized boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /**
     * Atomically records a completed question.
     * Takes a rollback snapshot before mutation. If any exception is thrown, state is restored.
     */
    public synchronized QuestionLog recordQuestionAtomically(
            QuestionOutcome outcome,
            String topic,
            String mistakeTag,
            boolean autoAdvance) {

        if (currentQuestionElapsedMs < 0) {
            throw new IllegalArgumentException("Question duration cannot be negative: " + currentQuestionElapsedMs);
        }

        long durationToCommit = currentQuestionElapsedMs;
        int qNumToCommit = currentQuestionIndex;

        QuestionLog log = new QuestionLog(
                (long) qNumToCommit,
                sessionId,
                qNumToCommit,
                durationToCommit,
                System.currentTimeMillis()
        );
        log.setOutcome(outcome != null ? outcome.name() : QuestionOutcome.CORRECT.name());
        log.setTopic(topic != null ? topic : "");
        log.setMistakeTag(mistakeTag != null ? mistakeTag : "");

        // Create transaction snapshot for undo/rollback
        RecordingSnapshot snapshot = new RecordingSnapshot(
                qNumToCommit,
                durationToCommit,
                totalStudyDurationMs,
                SessionState.RECORDING_OUTCOME,
                log
        );

        try {
            // Apply atomic commit mutations
            recordedLogs.add(log);
            totalStudyDurationMs += durationToCommit;

            if (autoAdvance) {
                currentQuestionIndex++;
                currentQuestionElapsedMs = 0L;
            }

            // Push to undo stack
            while (undoStack.size() >= maxUndoHistory) {
                undoStack.removeLast();
            }
            undoStack.push(snapshot);

            return log;
        } catch (Exception e) {
            // Rollback on any failure
            recordedLogs.remove(log);
            totalStudyDurationMs = snapshot.totalStudyMs;
            currentQuestionIndex = snapshot.questionNum;
            currentQuestionElapsedMs = snapshot.questionDurationMs;
            throw new RuntimeException("Atomic recording failed, transaction rolled back", e);
        }
    }

    /**
     * Undoes the most recently recorded question.
     * Restores previous question index, elapsed duration, and removes the log entry.
     */
    public synchronized boolean undoLastRecord() {
        if (undoStack.isEmpty()) {
            return false;
        }

        RecordingSnapshot snapshot = undoStack.pop();

        // Remove from committed logs
        if (!recordedLogs.isEmpty() && recordedLogs.get(recordedLogs.size() - 1).equals(snapshot.committedLog)) {
            recordedLogs.remove(recordedLogs.size() - 1);
        } else {
            recordedLogs.remove(snapshot.committedLog);
        }

        // Restore state
        this.totalStudyDurationMs = snapshot.totalStudyMs;
        this.currentQuestionIndex = snapshot.questionNum;
        this.currentQuestionElapsedMs = snapshot.questionDurationMs;

        return true;
    }
}

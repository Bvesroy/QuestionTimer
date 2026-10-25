package com.example.timerapp.data.engine;

import com.example.timerapp.data.model.BreakLog;
import com.example.timerapp.data.model.QuestionLog;
import com.example.timerapp.model.QuestionOutcome;
import com.example.timerapp.model.SessionState;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Thread-safe Explicit Session State Machine for Question Solving Timers.
 * Manages deterministic transitions across 7 explicit states:
 * 1. IDLE -> 2. RUNNING_QUESTION -> 3. PAUSED -> 4. EXPLICIT_BREAK ->
 * 5. RECORDING_OUTCOME -> 6. ADVANCING_QUESTION -> 7. SESSION_ENDED.
 */
public class SessionStateMachine implements Serializable {
    private static final long serialVersionUID = 1L;

    public interface OnStateChangeListener {
        void onStateChanged(SessionState oldState, SessionState newState);
    }

    private final long sessionId;
    private final int targetQuestionCount;

    private SessionState currentState = SessionState.IDLE;
    private SessionState stateBeforeBreak = SessionState.PAUSED;

    private int currentQuestionNumber = 1;
    private long currentQuestionElapsedMs = 0L;
    private long lastDiscardedQuestionElapsedMs = 0L;
    private boolean canUndoReset = false;

    private long totalStudyTimeMs = 0L;
    private long totalBreakTimeMs = 0L;

    private final List<QuestionLog> recordedQuestions = new ArrayList<>();
    private final List<BreakLog> recordedBreaks = new ArrayList<>();
    private BreakLog activeBreak = null;

    private QuestionOutcome lastRecordedOutcome = null;
    private double lastRecordedMarks = 0.0;

    private transient List<OnStateChangeListener> listeners = new ArrayList<>();

    public SessionStateMachine(long sessionId, int targetQuestionCount) {
        this.sessionId = sessionId;
        this.targetQuestionCount = Math.max(0, targetQuestionCount);
    }

    public SessionStateMachine(long sessionId) {
        this(sessionId, 0);
    }

    public synchronized void addListener(OnStateChangeListener listener) {
        if (listener == null) return;
        if (listeners == null) {
            listeners = new ArrayList<>();
        }
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public synchronized void removeListener(OnStateChangeListener listener) {
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    private void notifyStateChanged(SessionState oldState, SessionState newState) {
        if (listeners != null) {
            for (OnStateChangeListener l : listeners) {
                l.onStateChanged(oldState, newState);
            }
        }
    }

    private void transitionTo(SessionState newState) {
        SessionState oldState = this.currentState;
        this.currentState = newState;
        notifyStateChanged(oldState, newState);
    }

    public synchronized SessionState getCurrentState() {
        return currentState;
    }

    public synchronized long getSessionId() {
        return sessionId;
    }

    public synchronized int getTargetQuestionCount() {
        return targetQuestionCount;
    }

    public synchronized int getCurrentQuestionNumber() {
        return currentQuestionNumber;
    }

    public synchronized long getCurrentQuestionElapsedMs() {
        return currentQuestionElapsedMs;
    }

    public synchronized long getTotalStudyTimeMs() {
        return totalStudyTimeMs + currentQuestionElapsedMs;
    }

    public synchronized long getTotalBreakTimeMs() {
        long activeMs = (activeBreak != null && activeBreak.isActive())
                ? (System.currentTimeMillis() - activeBreak.getStartTime())
                : 0L;
        return totalBreakTimeMs + activeMs;
    }

    public synchronized List<QuestionLog> getRecordedQuestions() {
        return Collections.unmodifiableList(new ArrayList<>(recordedQuestions));
    }

    public synchronized List<BreakLog> getRecordedBreaks() {
        return Collections.unmodifiableList(new ArrayList<>(recordedBreaks));
    }

    public synchronized QuestionOutcome getLastRecordedOutcome() {
        return lastRecordedOutcome;
    }

    public synchronized double getLastRecordedMarks() {
        return lastRecordedMarks;
    }

    public synchronized boolean canUndoReset() {
        return canUndoReset;
    }

    // ==========================================
    // State Transitions
    // ==========================================

    /**
     * IDLE -> RUNNING_QUESTION: Starts solving first question.
     */
    public synchronized void startSession() {
        if (currentState != SessionState.IDLE) {
            throw new IllegalStateException("Cannot startSession from state " + currentState + "; expected IDLE.");
        }
        transitionTo(SessionState.RUNNING_QUESTION);
    }

    /**
     * Advances timer while actively running.
     */
    public synchronized void tick(long deltaMs) {
        if (currentState == SessionState.RUNNING_QUESTION && deltaMs > 0) {
            currentQuestionElapsedMs += deltaMs;
        }
    }

    /**
     * RUNNING_QUESTION -> PAUSED: Temporarily freezes timer.
     */
    public synchronized void pauseQuestion() {
        if (currentState != SessionState.RUNNING_QUESTION) {
            throw new IllegalStateException("Cannot pause from state " + currentState + "; expected RUNNING_QUESTION.");
        }
        transitionTo(SessionState.PAUSED);
    }

    /**
     * PAUSED -> RUNNING_QUESTION: Resumes timer.
     */
    public synchronized void resumeQuestion() {
        if (currentState != SessionState.PAUSED) {
            throw new IllegalStateException("Cannot resume from state " + currentState + "; expected PAUSED.");
        }
        transitionTo(SessionState.RUNNING_QUESTION);
    }

    /**
     * RUNNING_QUESTION or PAUSED -> EXPLICIT_BREAK: Initiates break interval.
     */
    public synchronized void startExplicitBreak(long timestamp) {
        if (currentState != SessionState.RUNNING_QUESTION && currentState != SessionState.PAUSED) {
            throw new IllegalStateException("Cannot startExplicitBreak from state " + currentState);
        }
        this.stateBeforeBreak = currentState;
        this.activeBreak = new BreakLog(sessionId, timestamp, 0L);
        transitionTo(SessionState.EXPLICIT_BREAK);
    }

    /**
     * EXPLICIT_BREAK -> PAUSED: Concludes break interval.
     */
    public synchronized void endExplicitBreak(long timestamp) {
        if (currentState != SessionState.EXPLICIT_BREAK) {
            throw new IllegalStateException("Cannot endExplicitBreak when not in EXPLICIT_BREAK.");
        }
        if (activeBreak != null) {
            activeBreak.setEndTime(timestamp);
            recordedBreaks.add(activeBreak);
            totalBreakTimeMs += activeBreak.getDurationMs();
            activeBreak = null;
        }
        transitionTo(SessionState.PAUSED);
    }

    /**
     * RUNNING_QUESTION or PAUSED -> RECORDING_OUTCOME: Freezes question timer to capture outcome.
     */
    public synchronized void promptRecordingOutcome() {
        if (currentState != SessionState.RUNNING_QUESTION && currentState != SessionState.PAUSED) {
            throw new IllegalStateException("Cannot promptRecordingOutcome from state " + currentState);
        }
        transitionTo(SessionState.RECORDING_OUTCOME);
    }

    /**
     * RECORDING_OUTCOME -> ADVANCING_QUESTION: Commits outcome and question record.
     */
    public synchronized QuestionLog recordOutcome(QuestionOutcome outcome, double marks, String topic, String mistakeTag) {
        if (currentState != SessionState.RECORDING_OUTCOME) {
            throw new IllegalStateException("Cannot recordOutcome from state " + currentState + "; expected RECORDING_OUTCOME.");
        }

        QuestionLog log = new QuestionLog(
                (long) currentQuestionNumber,
                sessionId,
                currentQuestionNumber,
                currentQuestionElapsedMs,
                System.currentTimeMillis()
        );
        log.setOutcome(outcome != null ? outcome.name() : QuestionOutcome.CORRECT.name());
        log.setTopic(topic != null ? topic : "");
        log.setMistakeTag(mistakeTag != null ? mistakeTag : "");

        recordedQuestions.add(log);
        totalStudyTimeMs += currentQuestionElapsedMs;
        currentQuestionElapsedMs = 0L;

        this.lastRecordedOutcome = outcome;
        this.lastRecordedMarks = marks;
        this.canUndoReset = false;

        transitionTo(SessionState.ADVANCING_QUESTION);
        return log;
    }

    /**
     * ADVANCING_QUESTION -> RUNNING_QUESTION or SESSION_ENDED:
     * Prepares next question or ends session if target reached.
     */
    public synchronized void advanceQuestion() {
        if (currentState != SessionState.ADVANCING_QUESTION) {
            throw new IllegalStateException("Cannot advanceQuestion from state " + currentState + "; expected ADVANCING_QUESTION.");
        }

        if (targetQuestionCount > 0 && currentQuestionNumber >= targetQuestionCount) {
            transitionTo(SessionState.SESSION_ENDED);
        } else {
            currentQuestionNumber++;
            currentQuestionElapsedMs = 0L;
            lastDiscardedQuestionElapsedMs = 0L;
            transitionTo(SessionState.RUNNING_QUESTION);
        }
    }

    /**
     * Concludes session from any active state.
     */
    public synchronized void endSession() {
        if (currentState == SessionState.IDLE || currentState == SessionState.SESSION_ENDED) {
            throw new IllegalStateException("Cannot endSession from state " + currentState);
        }

        if (currentState == SessionState.EXPLICIT_BREAK && activeBreak != null) {
            endExplicitBreak(System.currentTimeMillis());
        }

        transitionTo(SessionState.SESSION_ENDED);
    }

    /**
     * SESSION_ENDED or IDLE -> IDLE: Full reset.
     */
    public synchronized void resetToIdle() {
        if (currentState != SessionState.SESSION_ENDED && currentState != SessionState.IDLE) {
            throw new IllegalStateException("Cannot resetToIdle from state " + currentState);
        }
        currentQuestionNumber = 1;
        currentQuestionElapsedMs = 0L;
        lastDiscardedQuestionElapsedMs = 0L;
        canUndoReset = false;
        totalStudyTimeMs = 0L;
        totalBreakTimeMs = 0L;
        recordedQuestions.clear();
        recordedBreaks.clear();
        activeBreak = null;
        lastRecordedOutcome = null;
        lastRecordedMarks = 0.0;
        transitionTo(SessionState.IDLE);
    }

    // ==========================================
    // Accidental Reset and Undo
    // ==========================================

    public synchronized void resetCurrentQuestion() {
        this.lastDiscardedQuestionElapsedMs = this.currentQuestionElapsedMs;
        this.currentQuestionElapsedMs = 0L;
        this.canUndoReset = true;
    }

    public synchronized boolean undoResetCurrentQuestion() {
        if (canUndoReset) {
            this.currentQuestionElapsedMs = this.lastDiscardedQuestionElapsedMs;
            this.lastDiscardedQuestionElapsedMs = 0L;
            this.canUndoReset = false;
            return true;
        }
        return false;
    }

    public synchronized void forceState(SessionState state) {
        transitionTo(state);
    }
}

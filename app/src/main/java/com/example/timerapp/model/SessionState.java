package com.example.timerapp.model;

/**
 * Explicit 7-State Lifecycle State Machine for Question Solving Timer Sessions:
 * 1. IDLE: Ready to start a new question session.
 * 2. RUNNING_QUESTION: Actively counting solving time for the current question.
 * 3. PAUSED: Question timer halted, holding current duration.
 * 4. EXPLICIT_BREAK: Explicit study break interval active (deducted from net study time).
 * 5. RECORDING_OUTCOME: Question time frozen, capturing outcome (Correct/Incorrect/Skip/Mistake tag).
 * 6. ADVANCING_QUESTION: Outcome committed, preparing next question counter.
 * 7. SESSION_ENDED: Session concluded, stats finalized and saved.
 */
public enum SessionState {
    IDLE("Idle"),
    RUNNING_QUESTION("RunningQuestion"),
    PAUSED("Paused"),
    EXPLICIT_BREAK("ExplicitBreak"),
    RECORDING_OUTCOME("RecordingOutcome"),
    ADVANCING_QUESTION("AdvancingQuestion"),
    SESSION_ENDED("SessionEnded");

    private final String displayName;

    SessionState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isRunning() {
        return this == RUNNING_QUESTION;
    }

    public boolean isBreak() {
        return this == EXPLICIT_BREAK;
    }

    public boolean isPaused() {
        return this == PAUSED;
    }

    public boolean isTerminal() {
        return this == SESSION_ENDED;
    }

    public boolean isActiveSession() {
        return this != IDLE && this != SESSION_ENDED;
    }
}

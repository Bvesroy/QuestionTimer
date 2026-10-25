package com.example.timerapp.model;

/**
 * Lifecycle and interaction state of the Question Solving Timer.
 * Explicit session state machine with idempotent and atomic transitions.
 */
public enum TimerState {
    /** Timer is ready, elapsed is 0, waiting for tap to start. */
    IDLE,

    /** Active question solving state. */
    RUNNING_QUESTION,

    /** Backward-compatible alias for RUNNING_QUESTION. */
    RUNNING,

    /** Timer is paused on current question. */
    PAUSED,

    /** Explicit user break in progress. */
    EXPLICIT_BREAK,

    /** Transient atomic state: outcome recording in progress. */
    RECORDING_OUTCOME,

    /** Transient atomic state: zeroing timer and advancing question number. */
    ADVANCING_QUESTION,

    /** Session has been ended or finalized. */
    SESSION_ENDED;

    /**
     * Checks if the state represents an active running question timer.
     */
    public boolean isRunning() {
        return this == RUNNING_QUESTION || this == RUNNING;
    }

    /**
     * Checks if the session is currently in an explicit break.
     */
    public boolean isBreak() {
        return this == EXPLICIT_BREAK;
    }

    /**
     * Checks if the session is paused.
     */
    public boolean isPaused() {
        return this == PAUSED;
    }

    /**
     * Checks if the session is idle.
     */
    public boolean isIdle() {
        return this == IDLE;
    }

    /**
     * Checks if the session has ended.
     */
    public boolean isEnded() {
        return this == SESSION_ENDED;
    }
}


package com.example.timerapp.model;

/**
 * Lifecycle and interaction state of the Question Solving Timer.
 */
public enum TimerState {
    /** Timer is ready, elapsed is 0, waiting for tap to start. */
    IDLE,

    /** Timer is actively counting solving time. */
    RUNNING,

    /** Timer is paused, holding current question duration, waiting for tap to advance. */
    PAUSED
}

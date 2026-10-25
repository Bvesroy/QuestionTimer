package com.example.timerapp.data.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Encapsulates user practice goals and thresholds:
 * - target_time_per_question_sec (e.g., 120 seconds for JEE Advanced)
 * - target_session_duration_min (e.g., 60 minutes)
 * - target_session_questions (e.g., 50 questions)
 */
public class TargetSettings implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_TIME_PER_QUESTION_SEC = 120; // 2 minutes
    public static final int DEFAULT_SESSION_DURATION_MIN = 60;   // 1 hour
    public static final int DEFAULT_SESSION_QUESTIONS = 50;      // 50 questions

    private int targetTimePerQuestionSec;
    private int targetSessionDurationMin;
    private int targetSessionQuestions;

    public TargetSettings() {
        this.targetTimePerQuestionSec = DEFAULT_TIME_PER_QUESTION_SEC;
        this.targetSessionDurationMin = DEFAULT_SESSION_DURATION_MIN;
        this.targetSessionQuestions = DEFAULT_SESSION_QUESTIONS;
    }

    public TargetSettings(int targetTimePerQuestionSec, int targetSessionDurationMin, int targetSessionQuestions) {
        this.targetTimePerQuestionSec = Math.max(1, targetTimePerQuestionSec);
        this.targetSessionDurationMin = Math.max(1, targetSessionDurationMin);
        this.targetSessionQuestions = Math.max(1, targetSessionQuestions);
    }

    public int getTargetTimePerQuestionSec() {
        return targetTimePerQuestionSec;
    }

    public void setTargetTimePerQuestionSec(int targetTimePerQuestionSec) {
        this.targetTimePerQuestionSec = Math.max(1, targetTimePerQuestionSec);
    }

    public int getTargetSessionDurationMin() {
        return targetSessionDurationMin;
    }

    public void setTargetSessionDurationMin(int targetSessionDurationMin) {
        this.targetSessionDurationMin = Math.max(1, targetSessionDurationMin);
    }

    public int getTargetSessionQuestions() {
        return targetSessionQuestions;
    }

    public void setTargetSessionQuestions(int targetSessionQuestions) {
        this.targetSessionQuestions = Math.max(1, targetSessionQuestions);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TargetSettings that = (TargetSettings) o;
        return targetTimePerQuestionSec == that.targetTimePerQuestionSec &&
                targetSessionDurationMin == that.targetSessionDurationMin &&
                targetSessionQuestions == that.targetSessionQuestions;
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetTimePerQuestionSec, targetSessionDurationMin, targetSessionQuestions);
    }

    @Override
    public String toString() {
        return "TargetSettings{" +
                "targetTimePerQuestionSec=" + targetTimePerQuestionSec +
                ", targetSessionDurationMin=" + targetSessionDurationMin +
                ", targetSessionQuestions=" + targetSessionQuestions +
                '}';
    }
}

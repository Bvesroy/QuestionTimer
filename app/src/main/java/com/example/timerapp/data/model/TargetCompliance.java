package com.example.timerapp.data.model;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

/**
 * Evaluates and compares actual study performance against target settings.
 */
public class TargetCompliance implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum ComplianceStatus {
        MEETING("Meeting Target"),
        ABOVE_TARGET("Above Target"),
        BELOW_TARGET("Below Target");

        private final String label;

        ComplianceStatus(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private TargetSettings targetSettings;

    // Actual performance metrics
    private long actualDurationMs;
    private int actualQuestionsSolved;
    private long actualAvgTimePerQuestionMs;

    // Status classifications
    private ComplianceStatus paceStatus;
    private ComplianceStatus questionCountStatus;
    private ComplianceStatus durationStatus;

    public TargetCompliance() {
    }

    public TargetCompliance(TargetSettings targetSettings,
                            long actualDurationMs,
                            int actualQuestionsSolved,
                            long actualAvgTimePerQuestionMs) {
        this.targetSettings = (targetSettings != null) ? targetSettings : new TargetSettings();
        this.actualDurationMs = actualDurationMs;
        this.actualQuestionsSolved = actualQuestionsSolved;
        this.actualAvgTimePerQuestionMs = actualAvgTimePerQuestionMs;

        evaluateCompliance();
    }

    private void evaluateCompliance() {
        // 1. Pace Evaluation (time per question in seconds)
        // Faster pacing (smaller seconds per question) is superior/above target.
        long targetPaceSec = targetSettings.getTargetTimePerQuestionSec();
        double actualPaceSec = actualAvgTimePerQuestionMs / 1000.0;

        if (actualQuestionsSolved == 0 || actualAvgTimePerQuestionMs <= 0) {
            paceStatus = ComplianceStatus.BELOW_TARGET;
        } else {
            double ratio = (targetPaceSec > 0) ? (actualPaceSec / targetPaceSec) : 1.0;
            if (ratio < 0.95) {
                // At least 5% faster than target pace
                paceStatus = ComplianceStatus.ABOVE_TARGET;
            } else if (ratio <= 1.05) {
                // Within +-5% of target pace
                paceStatus = ComplianceStatus.MEETING;
            } else {
                // Slower than target pace
                paceStatus = ComplianceStatus.BELOW_TARGET;
            }
        }

        // 2. Question Count Evaluation
        int targetQuestions = targetSettings.getTargetSessionQuestions();
        if (actualQuestionsSolved > targetQuestions) {
            questionCountStatus = ComplianceStatus.ABOVE_TARGET;
        } else if (actualQuestionsSolved == targetQuestions && targetQuestions > 0) {
            questionCountStatus = ComplianceStatus.MEETING;
        } else {
            questionCountStatus = ComplianceStatus.BELOW_TARGET;
        }

        // 3. Duration Evaluation
        long targetDurationMs = (long) targetSettings.getTargetSessionDurationMin() * 60L * 1000L;
        if (actualDurationMs > targetDurationMs) {
            durationStatus = ComplianceStatus.ABOVE_TARGET;
        } else if (actualDurationMs >= (targetDurationMs - 60000L) && targetDurationMs > 0) {
            durationStatus = ComplianceStatus.MEETING;
        } else {
            durationStatus = ComplianceStatus.BELOW_TARGET;
        }
    }

    public TargetSettings getTargetSettings() {
        return targetSettings;
    }

    public long getActualDurationMs() {
        return actualDurationMs;
    }

    public int getActualQuestionsSolved() {
        return actualQuestionsSolved;
    }

    public long getActualAvgTimePerQuestionMs() {
        return actualAvgTimePerQuestionMs;
    }

    public ComplianceStatus getPaceStatus() {
        return paceStatus;
    }

    public ComplianceStatus getQuestionCountStatus() {
        return questionCountStatus;
    }

    public ComplianceStatus getDurationStatus() {
        return durationStatus;
    }

    public boolean isPaceCompliant() {
        return paceStatus == ComplianceStatus.MEETING || paceStatus == ComplianceStatus.ABOVE_TARGET;
    }

    public boolean isQuestionsCompliant() {
        return questionCountStatus == ComplianceStatus.MEETING || questionCountStatus == ComplianceStatus.ABOVE_TARGET;
    }

    public boolean isDurationCompliant() {
        return durationStatus == ComplianceStatus.MEETING || durationStatus == ComplianceStatus.ABOVE_TARGET;
    }

    public boolean isOverallCompliant() {
        return isPaceCompliant() && isQuestionsCompliant();
    }

    /**
     * Positive value indicates user was faster than target pace by X seconds.
     */
    public double getPaceAdvantageSec() {
        double actualPaceSec = actualAvgTimePerQuestionMs / 1000.0;
        return targetSettings.getTargetTimePerQuestionSec() - actualPaceSec;
    }

    public int getQuestionsDelta() {
        return actualQuestionsSolved - targetSettings.getTargetSessionQuestions();
    }

    public double getQuestionsProgressPercentage() {
        int target = targetSettings.getTargetSessionQuestions();
        if (target <= 0) return 0.0;
        return ((double) actualQuestionsSolved / target) * 100.0;
    }

    public double getDurationProgressPercentage() {
        long targetMs = (long) targetSettings.getTargetSessionDurationMin() * 60000L;
        if (targetMs <= 0) return 0.0;
        return ((double) actualDurationMs / targetMs) * 100.0;
    }

    public String getSummaryMessage() {
        StringBuilder sb = new StringBuilder();
        if (actualQuestionsSolved == 0) {
            return "No questions completed yet in session.";
        }

        double paceAdv = getPaceAdvantageSec();
        if (paceAdv >= 0) {
            sb.append(String.format(Locale.US, "Pace: %s (%.1fs faster than %ds target). ",
                    paceStatus.getLabel(), paceAdv, targetSettings.getTargetTimePerQuestionSec()));
        } else {
            sb.append(String.format(Locale.US, "Pace: %s (%.1fs slower than %ds target). ",
                    paceStatus.getLabel(), Math.abs(paceAdv), targetSettings.getTargetTimePerQuestionSec()));
        }

        sb.append(String.format(Locale.US, "Questions: %d/%d (%.0f%%).",
                actualQuestionsSolved, targetSettings.getTargetSessionQuestions(), getQuestionsProgressPercentage()));

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TargetCompliance that = (TargetCompliance) o;
        return actualDurationMs == that.actualDurationMs &&
                actualQuestionsSolved == that.actualQuestionsSolved &&
                actualAvgTimePerQuestionMs == that.actualAvgTimePerQuestionMs &&
                Objects.equals(targetSettings, that.targetSettings) &&
                paceStatus == that.paceStatus &&
                questionCountStatus == that.questionCountStatus &&
                durationStatus == that.durationStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetSettings, actualDurationMs, actualQuestionsSolved,
                actualAvgTimePerQuestionMs, paceStatus, questionCountStatus, durationStatus);
    }

    @Override
    public String toString() {
        return "TargetCompliance{" +
                "paceStatus=" + paceStatus +
                ", questionCountStatus=" + questionCountStatus +
                ", durationStatus=" + durationStatus +
                ", summary='" + getSummaryMessage() + '\'' +
                '}';
    }
}

package com.example.timerapp.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Diagnostic results from database verification and integrity repair.
 */
public class DataIntegrityReport implements Serializable {
    private static final long serialVersionUID = 1L;

    private int orphanedLogsRemoved;
    private int negativeDurationsFixed;
    private int corruptSessionsFixed;
    private boolean isHealthy;
    private List<String> diagnosticMessages;

    public DataIntegrityReport() {
        this.diagnosticMessages = new ArrayList<>();
        this.isHealthy = true;
    }

    public DataIntegrityReport(int orphanedLogsRemoved, int negativeDurationsFixed,
                               int corruptSessionsFixed, boolean isHealthy,
                               List<String> diagnosticMessages) {
        this.orphanedLogsRemoved = orphanedLogsRemoved;
        this.negativeDurationsFixed = negativeDurationsFixed;
        this.corruptSessionsFixed = corruptSessionsFixed;
        this.isHealthy = isHealthy;
        this.diagnosticMessages = (diagnosticMessages != null) ? diagnosticMessages : new ArrayList<>();
    }

    public int getOrphanedLogsRemoved() {
        return orphanedLogsRemoved;
    }

    public void setOrphanedLogsRemoved(int orphanedLogsRemoved) {
        this.orphanedLogsRemoved = orphanedLogsRemoved;
    }

    public int getNegativeDurationsFixed() {
        return negativeDurationsFixed;
    }

    public void setNegativeDurationsFixed(int negativeDurationsFixed) {
        this.negativeDurationsFixed = negativeDurationsFixed;
    }

    public int getCorruptSessionsFixed() {
        return corruptSessionsFixed;
    }

    public void setCorruptSessionsFixed(int corruptSessionsFixed) {
        this.corruptSessionsFixed = corruptSessionsFixed;
    }

    public boolean isHealthy() {
        return isHealthy;
    }

    public void setHealthy(boolean healthy) {
        isHealthy = healthy;
    }

    public List<String> getDiagnosticMessages() {
        return Collections.unmodifiableList(diagnosticMessages);
    }

    public void addDiagnosticMessage(String message) {
        if (message != null && !message.isEmpty()) {
            this.diagnosticMessages.add(message);
        }
    }

    public int getTotalRepairs() {
        return orphanedLogsRemoved + negativeDurationsFixed + corruptSessionsFixed;
    }

    public String getSummaryText() {
        if (getTotalRepairs() == 0 && isHealthy) {
            return "Database integrity verified. Zero anomalies detected.";
        }
        return String.format("Repaired %d orphaned logs, %d invalid durations, %d session anomalies.",
                orphanedLogsRemoved, negativeDurationsFixed, corruptSessionsFixed);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataIntegrityReport that = (DataIntegrityReport) o;
        return orphanedLogsRemoved == that.orphanedLogsRemoved &&
                negativeDurationsFixed == that.negativeDurationsFixed &&
                corruptSessionsFixed == that.corruptSessionsFixed &&
                isHealthy == that.isHealthy &&
                Objects.equals(diagnosticMessages, that.diagnosticMessages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orphanedLogsRemoved, negativeDurationsFixed, corruptSessionsFixed, isHealthy, diagnosticMessages);
    }

    @Override
    public String toString() {
        return "DataIntegrityReport{" +
                "isHealthy=" + isHealthy +
                ", totalRepairs=" + getTotalRepairs() +
                ", summary='" + getSummaryText() + '\'' +
                '}';
    }
}

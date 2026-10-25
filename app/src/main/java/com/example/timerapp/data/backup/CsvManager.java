package com.example.timerapp.data.backup;

import com.example.timerapp.data.model.QuestionLog;
import com.example.timerapp.data.model.SessionStats;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Robust RFC 4180 compliant CSV export and import manager.
 * Safely handles escaped double-quotes (""), embedded commas, multiline reflections/notes,
 * and Windows (CRLF) / Unix (LF) newline styles without external dependencies.
 */
public class CsvManager {

    public static final String SESSION_CSV_HEADER = "SessionId,SessionName,TargetCount,SolvedCount,TotalDurationMs,FormattedTotalDuration,AvgDurationMs,FormattedAvgPace,CreatedAt,FormattedDate,Notes";
    public static final String QUESTION_LOG_CSV_HEADER = "SessionId,QuestionNum,DurationMs,FormattedDuration,CompletedAt,FormattedCompletedAt";

    /**
     * Exports a list of session records to RFC 4180 CSV string.
     */
    public static String exportSessionsToCsv(List<SessionStats> sessions) {
        StringBuilder sb = new StringBuilder();
        sb.append(SESSION_CSV_HEADER).append("\r\n");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

        if (sessions != null) {
            for (SessionStats s : sessions) {
                long ts = s.getCreatedAt() > 0 ? s.getCreatedAt() : s.getUpdatedAt();
                String formattedDate = sdf.format(new Date(ts));

                sb.append(s.getSessionId()).append(",");
                sb.append(escapeCsvField(s.getSessionName())).append(",");
                sb.append(s.getTargetCount()).append(",");
                sb.append(s.getSolvedCount()).append(",");
                sb.append(s.getTotalDurationMs()).append(",");
                sb.append(escapeCsvField(s.getFormattedTotalDuration())).append(",");
                sb.append(s.getAverageDurationMs()).append(",");
                sb.append(escapeCsvField(s.getFormattedAverageDuration())).append(",");
                sb.append(ts).append(",");
                sb.append(escapeCsvField(formattedDate)).append(",");
                sb.append(escapeCsvField(s.getNotes() != null ? s.getNotes() : ""));
                sb.append("\r\n");
            }
        }
        return sb.toString();
    }

    /**
     * Exports a list of question logs to RFC 4180 CSV string.
     */
    public static String exportQuestionLogsToCsv(List<QuestionLog> logs) {
        StringBuilder sb = new StringBuilder();
        sb.append(QUESTION_LOG_CSV_HEADER).append("\r\n");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

        if (logs != null) {
            for (QuestionLog l : logs) {
                String formattedCompleted = sdf.format(new Date(l.getCompletedAt()));

                sb.append(l.getSessionId()).append(",");
                sb.append(l.getQuestionNum()).append(",");
                sb.append(l.getDurationMs()).append(",");
                sb.append(escapeCsvField(l.getFormattedDuration())).append(",");
                sb.append(l.getCompletedAt()).append(",");
                sb.append(escapeCsvField(formattedCompleted));
                sb.append("\r\n");
            }
        }
        return sb.toString();
    }

    /**
     * Escapes a field according to RFC 4180:
     * If the field contains comma, quotation mark, or newline, it is enclosed in double quotes.
     * Any double-quote inside is escaped by another double-quote.
     */
    public static String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        boolean mustQuote = field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r");
        if (mustQuote) {
            String escaped = field.replace("\"", "\"\"");
            return "\"" + escaped + "\"";
        }
        return field;
    }

    /**
     * Parses an RFC 4180 compliant CSV string into a 2D list of rows and cell values.
     * Implements a full finite state machine tracking quote state, multiline cells, and delimiter escapes.
     */
    public static List<List<String>> parseCsv(String csvContent) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        if (csvContent == null || csvContent.trim().isEmpty()) {
            return rows;
        }

        List<String> currentRow = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();

        boolean inQuotes = false;
        int length = csvContent.length();

        for (int i = 0; i < length; i++) {
            char c = csvContent.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    // Check if escaped quote ("")
                    if (i + 1 < length && csvContent.charAt(i + 1) == '"') {
                        currentField.append('"');
                        i++; // Skip the second quote
                    } else {
                        // Closing quote
                        inQuotes = false;
                    }
                } else {
                    currentField.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    currentRow.add(currentField.toString());
                    currentField.setLength(0);
                } else if (c == '\r') {
                    if (i + 1 < length && csvContent.charAt(i + 1) == '\n') {
                        i++; // Skip LF following CR
                    }
                    currentRow.add(currentField.toString());
                    currentField.setLength(0);
                    rows.add(currentRow);
                    currentRow = new ArrayList<>();
                } else if (c == '\n') {
                    currentRow.add(currentField.toString());
                    currentField.setLength(0);
                    rows.add(currentRow);
                    currentRow = new ArrayList<>();
                } else {
                    currentField.append(c);
                }
            }
        }

        // Add final field and row if any content remains
        if (currentField.length() > 0 || !currentRow.isEmpty()) {
            currentRow.add(currentField.toString());
            rows.add(currentRow);
        }

        return rows;
    }
}

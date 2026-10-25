package com.example.timerapp.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.timerapp.R;
import com.example.timerapp.model.QuestionLog;
import com.example.timerapp.model.TimerSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Side-by-Side Session Comparison Dialog comparing duration, question counts,
 * pace, subject, reflections, and targets across any two selected sessions.
 */
public class SessionComparisonDialog extends Dialog {

    private final List<TimerSession> allSessions;
    private Spinner spinnerSessionA, spinnerSessionB;
    private TextView tvCompSubjectA, tvCompSubjectB;
    private TextView tvCompQuestionsA, tvCompQuestionsB;
    private TextView tvCompDurationA, tvCompDurationB;
    private TextView tvCompAvgA, tvCompAvgB;
    private TextView tvCompExtremeA, tvCompExtremeB;
    private TextView tvCompNotesA, tvCompNotesB;

    private TimerSession sessionA;
    private TimerSession sessionB;

    public SessionComparisonDialog(Context context, List<TimerSession> sessions) {
        super(context, R.style.Theme_FullScreenTimer_BottomSheet);
        this.allSessions = (sessions != null) ? new ArrayList<>(sessions) : new ArrayList<>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_session_comparison, null);
        setContentView(view);

        Window window = getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        setCanceledOnTouchOutside(true);

        initViews(view);
        setupSpinners();
    }

    private void initViews(View view) {
        ImageButton btnClose = view.findViewById(R.id.btnCloseComparison);
        btnClose.setOnClickListener(v -> dismiss());

        spinnerSessionA = view.findViewById(R.id.spinnerSessionA);
        spinnerSessionB = view.findViewById(R.id.spinnerSessionB);

        tvCompSubjectA = view.findViewById(R.id.tvCompSubjectA);
        tvCompSubjectB = view.findViewById(R.id.tvCompSubjectB);
        tvCompQuestionsA = view.findViewById(R.id.tvCompQuestionsA);
        tvCompQuestionsB = view.findViewById(R.id.tvCompQuestionsB);
        tvCompDurationA = view.findViewById(R.id.tvCompDurationA);
        tvCompDurationB = view.findViewById(R.id.tvCompDurationB);
        tvCompAvgA = view.findViewById(R.id.tvCompAvgA);
        tvCompAvgB = view.findViewById(R.id.tvCompAvgB);
        tvCompExtremeA = view.findViewById(R.id.tvCompExtremeA);
        tvCompExtremeB = view.findViewById(R.id.tvCompExtremeB);
        tvCompNotesA = view.findViewById(R.id.tvCompNotesA);
        tvCompNotesB = view.findViewById(R.id.tvCompNotesB);
    }

    private void setupSpinners() {
        if (allSessions.isEmpty()) return;

        List<String> titles = new ArrayList<>(allSessions.size());
        for (int i = 0; i < allSessions.size(); i++) {
            TimerSession s = allSessions.get(i);
            titles.add((i + 1) + ". " + s.getName() + " (" + s.getCompletedCount() + " Qs)");
        }

        ArrayAdapter<String> adapterA = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, titles);
        ArrayAdapter<String> adapterB = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, titles);

        spinnerSessionA.setAdapter(adapterA);
        spinnerSessionB.setAdapter(adapterB);

        spinnerSessionA.setSelection(0);
        spinnerSessionB.setSelection(Math.min(1, allSessions.size() - 1));

        sessionA = allSessions.get(0);
        sessionB = allSessions.get(Math.min(1, allSessions.size() - 1));

        spinnerSessionA.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sessionA = allSessions.get(position);
                renderComparison();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerSessionB.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sessionB = allSessions.get(position);
                renderComparison();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        renderComparison();
    }

    private void renderComparison() {
        if (sessionA == null || sessionB == null) return;

        tvCompSubjectA.setText(sessionA.getSubject());
        tvCompSubjectB.setText(sessionB.getSubject());

        if (sessionA.getTarget() > 0) {
            tvCompQuestionsA.setText(String.format(Locale.ROOT, "%d / %d Qs", sessionA.getCompletedCount(), sessionA.getTarget()));
        } else {
            tvCompQuestionsA.setText(String.format(Locale.ROOT, "%d Qs", sessionA.getCompletedCount()));
        }

        if (sessionB.getTarget() > 0) {
            tvCompQuestionsB.setText(String.format(Locale.ROOT, "%d / %d Qs", sessionB.getCompletedCount(), sessionB.getTarget()));
        } else {
            tvCompQuestionsB.setText(String.format(Locale.ROOT, "%d Qs", sessionB.getCompletedCount()));
        }

        tvCompDurationA.setText(sessionA.getFormattedTotalDuration());
        tvCompDurationB.setText(sessionB.getFormattedTotalDuration());

        long avgA = sessionA.getAverageDurationMs();
        long avgB = sessionB.getAverageDurationMs();

        tvCompAvgA.setText(sessionA.getCompletedCount() > 0 ? sessionA.getFormattedAverageDuration() : "--:--");
        tvCompAvgB.setText(sessionB.getCompletedCount() > 0 ? sessionB.getFormattedAverageDuration() : "--:--");

        // Highlight faster pace in green
        if (avgA > 0 && avgB > 0) {
            tvCompAvgA.setTextColor(avgA <= avgB ? Color.parseColor("#30D158") : Color.parseColor("#FFFFFF"));
            tvCompAvgB.setTextColor(avgB <= avgA ? Color.parseColor("#30D158") : Color.parseColor("#FFFFFF"));
        }

        // Extreme questions
        QuestionLog fastA = sessionA.getFastestLog();
        QuestionLog slowA = sessionA.getSlowestLog();
        tvCompExtremeA.setText(String.format(Locale.ROOT, "Fast: %s\nSlow: %s",
                (fastA != null ? fastA.getFormattedDuration() : "--:--"),
                (slowA != null ? slowA.getFormattedDuration() : "--:--")));

        QuestionLog fastB = sessionB.getFastestLog();
        QuestionLog slowB = sessionB.getSlowestLog();
        tvCompExtremeB.setText(String.format(Locale.ROOT, "Fast: %s\nSlow: %s",
                (fastB != null ? fastB.getFormattedDuration() : "--:--"),
                (slowB != null ? slowB.getFormattedDuration() : "--:--")));

        // Notes and reflections
        String notesA = sessionA.getNotes();
        if (sessionA.getWhatWentWell() != null && !sessionA.getWhatWentWell().isEmpty()) {
            notesA += "\n+ " + sessionA.getWhatWentWell();
        }
        tvCompNotesA.setText(notesA.trim().isEmpty() ? "No notes recorded" : notesA.trim());

        String notesB = sessionB.getNotes();
        if (sessionB.getWhatWentWell() != null && !sessionB.getWhatWentWell().isEmpty()) {
            notesB += "\n+ " + sessionB.getWhatWentWell();
        }
        tvCompNotesB.setText(notesB.trim().isEmpty() ? "No notes recorded" : notesB.trim());
    }
}

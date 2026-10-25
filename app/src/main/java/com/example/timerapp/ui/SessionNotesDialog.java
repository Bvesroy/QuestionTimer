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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.timerapp.R;
import com.example.timerapp.model.TimerSession;

/**
 * Dialog for capturing Session Completion Notes, Subject Categorization,
 * and Reflective Feedback (What Went Well & Areas for Improvement).
 */
public class SessionNotesDialog extends Dialog {

    public interface OnNotesSavedListener {
        void onNotesSaved(TimerSession session);
    }

    private final TimerSession session;
    private final OnNotesSavedListener listener;

    private EditText etNoteSessionTitle;
    private TextView pillSubMath, pillSubPhysics, pillSubChem, pillSubBio, pillSubGeneral, pillSubCustom;
    private EditText etCustomSubject;
    private EditText etSessionNotes;
    private EditText etWhatWentWell;
    private EditText etNeedsImprovement;

    private String selectedSubject = "General";

    public SessionNotesDialog(Context context, TimerSession session, OnNotesSavedListener listener) {
        super(context, R.style.Theme_FullScreenTimer_BottomSheet);
        this.session = session;
        this.listener = listener;
        if (session != null && session.getSubject() != null && !session.getSubject().isEmpty()) {
            this.selectedSubject = session.getSubject();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_session_notes, null);
        setContentView(view);

        Window window = getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        setCanceledOnTouchOutside(true);

        initViews(view);
        populateData();
    }

    private void initViews(View view) {
        ImageButton btnClose = view.findViewById(R.id.btnCloseNotes);
        btnClose.setOnClickListener(v -> dismiss());

        etNoteSessionTitle = view.findViewById(R.id.etNoteSessionTitle);
        pillSubMath = view.findViewById(R.id.pillSubMath);
        pillSubPhysics = view.findViewById(R.id.pillSubPhysics);
        pillSubChem = view.findViewById(R.id.pillSubChem);
        pillSubBio = view.findViewById(R.id.pillSubBio);
        pillSubGeneral = view.findViewById(R.id.pillSubGeneral);
        pillSubCustom = view.findViewById(R.id.pillSubCustom);
        etCustomSubject = view.findViewById(R.id.etCustomSubject);
        etSessionNotes = view.findViewById(R.id.etSessionNotes);
        etWhatWentWell = view.findViewById(R.id.etWhatWentWell);
        etNeedsImprovement = view.findViewById(R.id.etNeedsImprovement);

        Button btnSkip = view.findViewById(R.id.btnSkipNotes);
        Button btnSave = view.findViewById(R.id.btnSaveNotes);

        pillSubMath.setOnClickListener(v -> selectSubject("Mathematics"));
        pillSubPhysics.setOnClickListener(v -> selectSubject("Physics"));
        pillSubChem.setOnClickListener(v -> selectSubject("Chemistry"));
        pillSubBio.setOnClickListener(v -> selectSubject("Biology"));
        pillSubGeneral.setOnClickListener(v -> selectSubject("General"));
        pillSubCustom.setOnClickListener(v -> selectCustomSubject());

        btnSkip.setOnClickListener(v -> dismiss());
        btnSave.setOnClickListener(v -> saveNotes());
    }

    private void populateData() {
        if (session == null) return;

        etNoteSessionTitle.setText(session.getName());
        etSessionNotes.setText(session.getNotes());
        etWhatWentWell.setText(session.getWhatWentWell());
        etNeedsImprovement.setText(session.getNeedsImprovement());

        selectSubject(selectedSubject);
    }

    private void selectSubject(String subject) {
        this.selectedSubject = subject;
        etCustomSubject.setVisibility(View.GONE);

        setPillState(pillSubMath, "Mathematics".equalsIgnoreCase(subject));
        setPillState(pillSubPhysics, "Physics".equalsIgnoreCase(subject));
        setPillState(pillSubChem, "Chemistry".equalsIgnoreCase(subject));
        setPillState(pillSubBio, "Biology".equalsIgnoreCase(subject));
        setPillState(pillSubGeneral, "General".equalsIgnoreCase(subject));
        setPillState(pillSubCustom, false);

        // If it's a custom subject not in standard list
        if (!"Mathematics".equalsIgnoreCase(subject)
                && !"Physics".equalsIgnoreCase(subject)
                && !"Chemistry".equalsIgnoreCase(subject)
                && !"Biology".equalsIgnoreCase(subject)
                && !"General".equalsIgnoreCase(subject)) {
            setPillState(pillSubCustom, true);
            etCustomSubject.setVisibility(View.VISIBLE);
            etCustomSubject.setText(subject);
        }
    }

    private void selectCustomSubject() {
        setPillState(pillSubMath, false);
        setPillState(pillSubPhysics, false);
        setPillState(pillSubChem, false);
        setPillState(pillSubBio, false);
        setPillState(pillSubGeneral, false);
        setPillState(pillSubCustom, true);

        etCustomSubject.setVisibility(View.VISIBLE);
        etCustomSubject.requestFocus();
    }

    private void setPillState(TextView pill, boolean isSelected) {
        if (pill == null) return;
        pill.setBackgroundResource(isSelected ? R.drawable.bg_pill_selected : R.drawable.bg_pill_unselected);
        pill.setTextColor(getContext().getColor(isSelected ? R.color.accent_highlight : R.color.text_secondary));
    }

    private void saveNotes() {
        if (session != null) {
            String title = etNoteSessionTitle.getText().toString().trim();
            if (!title.isEmpty()) {
                session.setName(title);
            }

            if (etCustomSubject.getVisibility() == View.VISIBLE) {
                String custom = etCustomSubject.getText().toString().trim();
                selectedSubject = custom.isEmpty() ? "General" : custom;
            }
            session.setSubject(selectedSubject);
            session.setNotes(etSessionNotes.getText().toString().trim());
            session.setWhatWentWell(etWhatWentWell.getText().toString().trim());
            session.setNeedsImprovement(etNeedsImprovement.getText().toString().trim());

            if (listener != null) {
                listener.onNotesSaved(session);
            }
        }
        dismiss();
    }
}

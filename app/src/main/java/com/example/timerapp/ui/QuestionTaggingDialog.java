package com.example.timerapp.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.timerapp.R;
import com.example.timerapp.model.QuestionLog;

import java.util.Locale;

/**
 * Question-Level Tagging Modal Dialog.
 * Allows tagging Outcome, Difficulty, Confidence (1-5), Mistake Category, and review notes.
 */
public class QuestionTaggingDialog extends Dialog {

    public interface OnTagSavedListener {
        void onTagSaved(QuestionLog updatedLog);
    }

    private final QuestionLog log;
    private final OnTagSavedListener listener;

    private String selectedOutcome = "SOLVED";
    private String selectedDifficulty = "MEDIUM";
    private int selectedConfidence = 3;
    private String selectedMistake = "";

    // Views
    private TextView tvTaggingTitle;
    private TextView tvQuestionDuration;
    private TextView pillOutcomeSolved, pillOutcomeIncorrect, pillOutcomeSkipped, pillOutcomeNeedsReview, pillOutcomeAbandoned;
    private TextView pillDiffEasy, pillDiffMedium, pillDiffHard;
    private TextView pillConf1, pillConf2, pillConf3, pillConf4, pillConf5;
    private TextView pillMistakeNone, pillMistakeCalc, pillMistakeConceptual, pillMistakeTime, pillMistakeSilly, pillMistakeMisread;
    private EditText etQuestionNotes;
    private Button btnCancelTagging, btnSaveTagging;

    public QuestionTaggingDialog(Context context, QuestionLog log, OnTagSavedListener listener) {
        super(context, R.style.Theme_FullScreenTimer_Dialog);
        this.log = log;
        this.listener = listener;
        if (log != null) {
            this.selectedOutcome = (log.getOutcome() != null && !log.getOutcome().isEmpty()) ? log.getOutcome() : "SOLVED";
            this.selectedDifficulty = (log.getDifficulty() != null && !log.getDifficulty().isEmpty()) ? log.getDifficulty() : "MEDIUM";
            this.selectedConfidence = (log.getConfidence() > 0) ? log.getConfidence() : 3;
            this.selectedMistake = (log.getMistakeTag() != null) ? log.getMistakeTag() : "";
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_question_tagging);

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        initViews();
        bindData();
        setupListeners();
    }

    private void initViews() {
        tvTaggingTitle = findViewById(R.id.tvTaggingTitle);
        tvQuestionDuration = findViewById(R.id.tvQuestionDuration);

        pillOutcomeSolved = findViewById(R.id.pillOutcomeSolved);
        pillOutcomeIncorrect = findViewById(R.id.pillOutcomeIncorrect);
        pillOutcomeSkipped = findViewById(R.id.pillOutcomeSkipped);
        pillOutcomeNeedsReview = findViewById(R.id.pillOutcomeNeedsReview);
        pillOutcomeAbandoned = findViewById(R.id.pillOutcomeAbandoned);

        pillDiffEasy = findViewById(R.id.pillDiffEasy);
        pillDiffMedium = findViewById(R.id.pillDiffMedium);
        pillDiffHard = findViewById(R.id.pillDiffHard);

        pillConf1 = findViewById(R.id.pillConf1);
        pillConf2 = findViewById(R.id.pillConf2);
        pillConf3 = findViewById(R.id.pillConf3);
        pillConf4 = findViewById(R.id.pillConf4);
        pillConf5 = findViewById(R.id.pillConf5);

        pillMistakeNone = findViewById(R.id.pillMistakeNone);
        pillMistakeCalc = findViewById(R.id.pillMistakeCalc);
        pillMistakeConceptual = findViewById(R.id.pillMistakeConceptual);
        pillMistakeTime = findViewById(R.id.pillMistakeTime);
        pillMistakeSilly = findViewById(R.id.pillMistakeSilly);
        pillMistakeMisread = findViewById(R.id.pillMistakeMisread);

        etQuestionNotes = findViewById(R.id.etQuestionNotes);
        btnCancelTagging = findViewById(R.id.btnCancelTagging);
        btnSaveTagging = findViewById(R.id.btnSaveTagging);
    }

    private void bindData() {
        if (log != null) {
            tvTaggingTitle.setText(String.format(Locale.ROOT, "TAG QUESTION #%d", log.getQuestionNumber()));
            tvQuestionDuration.setText(log.getFormattedDuration());
            if (log.getNotes() != null && !log.getNotes().isEmpty()) {
                etQuestionNotes.setText(log.getNotes());
                etQuestionNotes.setSelection(log.getNotes().length());
            }
        }
        updateOutcomePills();
        updateDiffPills();
        updateConfPills();
        updateMistakePills();
    }

    private void setupListeners() {
        pillOutcomeSolved.setOnClickListener(v -> { selectedOutcome = "RIGHT"; updateOutcomePills(); });
        pillOutcomeIncorrect.setOnClickListener(v -> { selectedOutcome = "WRONG"; updateOutcomePills(); });
        pillOutcomeSkipped.setOnClickListener(v -> { selectedOutcome = "SKIPPED"; updateOutcomePills(); });
        pillOutcomeNeedsReview.setOnClickListener(v -> { selectedOutcome = "NEEDS REVIEW"; updateOutcomePills(); });
        pillOutcomeAbandoned.setOnClickListener(v -> { selectedOutcome = "ABANDONED"; updateOutcomePills(); });

        pillDiffEasy.setOnClickListener(v -> { selectedDifficulty = "EASY"; updateDiffPills(); });
        pillDiffMedium.setOnClickListener(v -> { selectedDifficulty = "MEDIUM"; updateDiffPills(); });
        pillDiffHard.setOnClickListener(v -> { selectedDifficulty = "HARD"; updateDiffPills(); });

        pillConf1.setOnClickListener(v -> { selectedConfidence = 1; updateConfPills(); });
        pillConf2.setOnClickListener(v -> { selectedConfidence = 2; updateConfPills(); });
        pillConf3.setOnClickListener(v -> { selectedConfidence = 3; updateConfPills(); });
        pillConf4.setOnClickListener(v -> { selectedConfidence = 4; updateConfPills(); });
        pillConf5.setOnClickListener(v -> { selectedConfidence = 5; updateConfPills(); });

        pillMistakeNone.setOnClickListener(v -> { selectedMistake = ""; updateMistakePills(); });
        pillMistakeCalc.setOnClickListener(v -> { selectedMistake = "CALCULATION"; updateMistakePills(); });
        pillMistakeConceptual.setOnClickListener(v -> { selectedMistake = "CONCEPTUAL"; updateMistakePills(); });
        pillMistakeTime.setOnClickListener(v -> { selectedMistake = "TIME PRESSURE"; updateMistakePills(); });
        pillMistakeSilly.setOnClickListener(v -> { selectedMistake = "CARELESS / SILLY"; updateMistakePills(); });
        pillMistakeMisread.setOnClickListener(v -> { selectedMistake = "MISREAD QUESTION"; updateMistakePills(); });

        btnCancelTagging.setOnClickListener(v -> dismiss());
        btnSaveTagging.setOnClickListener(v -> saveAndDismiss());
    }

    private void updateOutcomePills() {
        boolean isRight = "RIGHT".equalsIgnoreCase(selectedOutcome) || "SOLVED".equalsIgnoreCase(selectedOutcome) || "CORRECT".equalsIgnoreCase(selectedOutcome);
        boolean isWrong = "WRONG".equalsIgnoreCase(selectedOutcome) || "INCORRECT".equalsIgnoreCase(selectedOutcome);
        setPillSelected(pillOutcomeSolved, isRight);
        setPillSelected(pillOutcomeIncorrect, isWrong);
        setPillSelected(pillOutcomeSkipped, "SKIPPED".equalsIgnoreCase(selectedOutcome));
        setPillSelected(pillOutcomeNeedsReview, "NEEDS REVIEW".equalsIgnoreCase(selectedOutcome));
        setPillSelected(pillOutcomeAbandoned, "ABANDONED".equalsIgnoreCase(selectedOutcome));
    }

    private void updateDiffPills() {
        setPillSelected(pillDiffEasy, "EASY".equalsIgnoreCase(selectedDifficulty));
        setPillSelected(pillDiffMedium, "MEDIUM".equalsIgnoreCase(selectedDifficulty));
        setPillSelected(pillDiffHard, "HARD".equalsIgnoreCase(selectedDifficulty));
    }

    private void updateConfPills() {
        setPillSelected(pillConf1, selectedConfidence == 1);
        setPillSelected(pillConf2, selectedConfidence == 2);
        setPillSelected(pillConf3, selectedConfidence == 3);
        setPillSelected(pillConf4, selectedConfidence == 4);
        setPillSelected(pillConf5, selectedConfidence == 5);
    }

    private void updateMistakePills() {
        setPillSelected(pillMistakeNone, selectedMistake.isEmpty() || "NONE".equalsIgnoreCase(selectedMistake));
        setPillSelected(pillMistakeCalc, "CALCULATION".equalsIgnoreCase(selectedMistake));
        setPillSelected(pillMistakeConceptual, "CONCEPTUAL".equalsIgnoreCase(selectedMistake));
        setPillSelected(pillMistakeTime, "TIME PRESSURE".equalsIgnoreCase(selectedMistake));
        setPillSelected(pillMistakeSilly, "CARELESS / SILLY".equalsIgnoreCase(selectedMistake));
        setPillSelected(pillMistakeMisread, "MISREAD QUESTION".equalsIgnoreCase(selectedMistake));
    }

    private void setPillSelected(TextView pill, boolean isSelected) {
        if (pill == null) return;
        pill.setBackgroundResource(isSelected ? R.drawable.bg_pill_selected : R.drawable.bg_pill_unselected);
        pill.setTextColor(getContext().getColor(isSelected ? R.color.accent_highlight : R.color.text_secondary));
    }

    private void saveAndDismiss() {
        if (log != null && listener != null) {
            String notes = etQuestionNotes.getText().toString().trim();
            QuestionLog updated = log.withTags(selectedOutcome, selectedDifficulty, selectedConfidence, selectedMistake, notes);
            listener.onTagSaved(updated);
        }
        dismiss();
    }
}

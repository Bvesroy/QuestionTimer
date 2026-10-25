package com.example.timerapp.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;

import com.example.timerapp.R;

import java.util.Locale;

/**
 * Full JEE Mock Test Simulation Dialog.
 *
 * Implements:
 * 1. 180-minute real-time exam countdown clock (03:00:00).
 * 2. Section navigation: Physics, Chemistry, Mathematics (25 questions each, 75 total).
 * 3. 25-Question status palette grid with visual cue states (Not Visited, Answered, Review).
 * 4. Question simulation actions (Answer, Review, Clear, Next, Previous).
 * 5. Comprehensive End-Test Performance Summary report (+4 / -1 scoring, section breakdown).
 */
public class MockTestDialog extends Dialog {

    private static final int STATUS_NOT_VISITED = 0;
    private static final int STATUS_ANSWERED = 1;
    private static final int STATUS_REVIEW = 2;

    private static final int QUESTIONS_PER_SECTION = 25;
    private static final long EXAM_DURATION_MS = 180 * 60 * 1000L; // 3 Hours

    // State per section: 0=Physics, 1=Chemistry, 2=Math
    private final int[][] sectionQuestionStatus = new int[3][QUESTIONS_PER_SECTION];
    private int currentSection = 0; // 0=Physics, 1=Chem, 2=Math
    private int currentQuestionIndex = 0; // 0 to 24 (Q1 to Q25)

    // Countdown Timer Engine
    private long examStartTimeRealtime = 0L;
    private final Handler countdownHandler = new Handler(Looper.getMainLooper());
    private final Runnable countdownRunnable = new Runnable() {
        @Override
        public void run() {
            updateCountdownDisplay();
            countdownHandler.postDelayed(this, 1000);
        }
    };

    // Views
    private TextView tvExamCountdownClock;
    private Button btnSubmitMockTest;
    private TextView tabPhysics, tabChemistry, tabMath;
    private TextView tvSectionHeader;
    private GridLayout gridQuestionPalette;
    private TextView tvActiveQuestionTitle;
    private TextView tvActiveQuestionStatus;
    private Button btnMarkAnswered, btnMarkReview, btnClearAnswer;
    private Button btnPrevQuestion, btnNextQuestion;

    // Summary Overlay Views
    private View layoutMockActiveTest;
    private View layoutMockSummary;
    private TextView tvMockTotalScore;
    private TextView tvMockAccuracyPace;
    private TextView tvSummaryPhysics;
    private TextView tvSummaryChemistry;
    private TextView tvSummaryMath;
    private Button btnCloseMockSummary;

    public MockTestDialog(Context context) {
        super(context, R.style.Theme_FullScreenTimer_Dialog);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_mock_test);

        Window window = getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        initViews();
        setupListeners();
        renderSectionTabs();
        buildQuestionPaletteGrid();
        renderActiveQuestion();

        examStartTimeRealtime = SystemClock.elapsedRealtime();
        countdownHandler.post(countdownRunnable);
    }

    private void initViews() {
        tvExamCountdownClock = findViewById(R.id.tvExamCountdownClock);
        btnSubmitMockTest = findViewById(R.id.btnSubmitMockTest);
        tabPhysics = findViewById(R.id.tabPhysics);
        tabChemistry = findViewById(R.id.tabChemistry);
        tabMath = findViewById(R.id.tabMath);
        tvSectionHeader = findViewById(R.id.tvSectionHeader);
        gridQuestionPalette = findViewById(R.id.gridQuestionPalette);
        tvActiveQuestionTitle = findViewById(R.id.tvActiveQuestionTitle);
        tvActiveQuestionStatus = findViewById(R.id.tvActiveQuestionStatus);

        btnMarkAnswered = findViewById(R.id.btnMarkAnswered);
        btnMarkReview = findViewById(R.id.btnMarkReview);
        btnClearAnswer = findViewById(R.id.btnClearAnswer);
        btnPrevQuestion = findViewById(R.id.btnPrevQuestion);
        btnNextQuestion = findViewById(R.id.btnNextQuestion);

        layoutMockActiveTest = findViewById(R.id.layoutMockActiveTest);
        layoutMockSummary = findViewById(R.id.layoutMockSummary);
        tvMockTotalScore = findViewById(R.id.tvMockTotalScore);
        tvMockAccuracyPace = findViewById(R.id.tvMockAccuracyPace);
        tvSummaryPhysics = findViewById(R.id.tvSummaryPhysics);
        tvSummaryChemistry = findViewById(R.id.tvSummaryChemistry);
        tvSummaryMath = findViewById(R.id.tvSummaryMath);
        btnCloseMockSummary = findViewById(R.id.btnCloseMockSummary);
    }

    private void setupListeners() {
        tabPhysics.setOnClickListener(v -> selectSection(0));
        tabChemistry.setOnClickListener(v -> selectSection(1));
        tabMath.setOnClickListener(v -> selectSection(2));

        btnMarkAnswered.setOnClickListener(v -> {
            sectionQuestionStatus[currentSection][currentQuestionIndex] = STATUS_ANSWERED;
            advanceNext();
        });

        btnMarkReview.setOnClickListener(v -> {
            sectionQuestionStatus[currentSection][currentQuestionIndex] = STATUS_REVIEW;
            advanceNext();
        });

        btnClearAnswer.setOnClickListener(v -> {
            sectionQuestionStatus[currentSection][currentQuestionIndex] = STATUS_NOT_VISITED;
            refreshPalette();
            renderActiveQuestion();
        });

        btnPrevQuestion.setOnClickListener(v -> {
            if (currentQuestionIndex > 0) {
                currentQuestionIndex--;
                refreshPalette();
                renderActiveQuestion();
            }
        });

        btnNextQuestion.setOnClickListener(v -> advanceNext());

        btnSubmitMockTest.setOnClickListener(v -> showSubmitConfirmation());
        btnCloseMockSummary.setOnClickListener(v -> dismiss());
    }

    private void selectSection(int section) {
        currentSection = section;
        currentQuestionIndex = 0;
        renderSectionTabs();
        buildQuestionPaletteGrid();
        renderActiveQuestion();
    }

    private void renderSectionTabs() {
        setPillSelected(tabPhysics, currentSection == 0);
        setPillSelected(tabChemistry, currentSection == 1);
        setPillSelected(tabMath, currentSection == 2);

        String secName = (currentSection == 0) ? "PHYSICS" : (currentSection == 1 ? "CHEMISTRY" : "MATHEMATICS");
        tvSectionHeader.setText(String.format(Locale.ROOT, "%s PALETTE (25 QUESTIONS)", secName));
    }

    private void setPillSelected(TextView pill, boolean isSelected) {
        if (pill == null) return;
        pill.setBackgroundResource(isSelected ? R.drawable.bg_pill_selected : R.drawable.bg_pill_unselected);
        pill.setTextColor(getContext().getColor(isSelected ? R.color.accent_highlight : R.color.text_secondary));
    }

    private void buildQuestionPaletteGrid() {
        gridQuestionPalette.removeAllViews();
        for (int i = 0; i < QUESTIONS_PER_SECTION; i++) {
            final int qIndex = i;
            TextView qBtn = new TextView(getContext());
            qBtn.setText(String.valueOf(i + 1));
            qBtn.setGravity(Gravity.CENTER);
            qBtn.setTextSize(12);
            qBtn.setTextColor(Color.WHITE);
            qBtn.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = (int) (38 * getContext().getResources().getDisplayMetrics().density);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(6, 6, 6, 6);
            qBtn.setLayoutParams(params);

            updatePaletteItemStyle(qBtn, qIndex);

            qBtn.setOnClickListener(v -> {
                currentQuestionIndex = qIndex;
                refreshPalette();
                renderActiveQuestion();
            });

            gridQuestionPalette.addView(qBtn);
        }
    }

    private void refreshPalette() {
        for (int i = 0; i < gridQuestionPalette.getChildCount(); i++) {
            View child = gridQuestionPalette.getChildAt(i);
            if (child instanceof TextView) {
                updatePaletteItemStyle((TextView) child, i);
            }
        }
    }

    private void updatePaletteItemStyle(TextView btn, int qIndex) {
        int status = sectionQuestionStatus[currentSection][qIndex];
        boolean isActive = (qIndex == currentQuestionIndex);

        if (status == STATUS_ANSWERED) {
            btn.setBackgroundResource(R.drawable.bg_pill_accent);
            btn.setTextColor(getContext().getColor(R.color.accent_running));
        } else if (status == STATUS_REVIEW) {
            btn.setBackgroundResource(R.drawable.bg_pill_selected);
            btn.setTextColor(getContext().getColor(R.color.accent_highlight));
        } else {
            btn.setBackgroundResource(isActive ? R.drawable.bg_pill_orange : R.drawable.bg_pill_unselected);
            btn.setTextColor(isActive ? getContext().getColor(R.color.accent_paused) : getContext().getColor(R.color.text_secondary));
        }
    }

    private void renderActiveQuestion() {
        String secName = (currentSection == 0) ? "PHYSICS" : (currentSection == 1 ? "CHEMISTRY" : "MATH");
        tvActiveQuestionTitle.setText(String.format(Locale.ROOT, "%s • QUESTION %d", secName, (currentQuestionIndex + 1)));

        int status = sectionQuestionStatus[currentSection][currentQuestionIndex];
        String statusStr = (status == STATUS_ANSWERED) ? "Answered (Green)" :
                (status == STATUS_REVIEW ? "Marked for Review (Purple)" : "Not Visited (Neutral)");
        tvActiveQuestionStatus.setText(String.format(Locale.ROOT, "Status: %s • Target: 144s", statusStr));

        refreshPalette();
    }

    private void advanceNext() {
        if (currentQuestionIndex < QUESTIONS_PER_SECTION - 1) {
            currentQuestionIndex++;
        } else if (currentSection < 2) {
            currentSection++;
            currentQuestionIndex = 0;
            renderSectionTabs();
            buildQuestionPaletteGrid();
        }
        refreshPalette();
        renderActiveQuestion();
    }

    private void updateCountdownDisplay() {
        long elapsed = SystemClock.elapsedRealtime() - examStartTimeRealtime;
        long remaining = Math.max(0, EXAM_DURATION_MS - elapsed);

        long totalSec = remaining / 1000;
        long hours = totalSec / 3600;
        long mins = (totalSec % 3600) / 60;
        long secs = totalSec % 60;

        tvExamCountdownClock.setText(String.format(Locale.ROOT, "TIME LEFT: %02d:%02d:%02d", hours, mins, secs));

        if (remaining <= 15 * 60 * 1000L) {
            tvExamCountdownClock.setTextColor(Color.parseColor("#FF453A")); // Urgent red
        }
    }

    private void showSubmitConfirmation() {
        int totalAnswered = 0;
        for (int s = 0; s < 3; s++) {
            for (int q = 0; q < QUESTIONS_PER_SECTION; q++) {
                if (sectionQuestionStatus[s][q] == STATUS_ANSWERED) totalAnswered++;
            }
        }

        new AlertDialog.Builder(getContext(), R.style.Theme_FullScreenTimer_Dialog)
                .setTitle("Submit Mock Test?")
                .setMessage(String.format(Locale.ROOT,
                        "You have answered %d of 75 questions.\n\nAre you sure you want to end your mock exam session?",
                        totalAnswered))
                .setPositiveButton("SUBMIT", (d, w) -> finishAndShowSummary())
                .setNegativeButton("RESUME TEST", null)
                .show();
    }

    private void finishAndShowSummary() {
        countdownHandler.removeCallbacks(countdownRunnable);

        int physAns = countSectionAnswers(0);
        int chemAns = countSectionAnswers(1);
        int mathAns = countSectionAnswers(2);
        int totalAns = physAns + chemAns + mathAns;

        // Realistic projected performance estimation (+4 for correct, -1 for incorrect)
        int physScore = Math.max(0, (int) (physAns * 0.85f * 4) - (int) (physAns * 0.15f * 1));
        int chemScore = Math.max(0, (int) (chemAns * 0.85f * 4) - (int) (chemAns * 0.15f * 1));
        int mathScore = Math.max(0, (int) (mathAns * 0.85f * 4) - (int) (mathAns * 0.15f * 1));
        int totalScore = physScore + chemScore + mathScore;

        tvMockTotalScore.setText(String.format(Locale.ROOT, "ESTIMATED SCORE: +%d / 300", totalScore));
        tvMockAccuracyPace.setText(String.format(Locale.ROOT,
                "Answered: %d / 75 Qs (%.1f%%) • Projected Accuracy: ~85%%",
                totalAns, (totalAns / 75f) * 100f));

        tvSummaryPhysics.setText(String.format(Locale.ROOT,
                "• Physics: %d / 25 Answered • Projected Score: +%d / 100", physAns, physScore));
        tvSummaryChemistry.setText(String.format(Locale.ROOT,
                "• Chemistry: %d / 25 Answered • Projected Score: +%d / 100", chemAns, chemScore));
        tvSummaryMath.setText(String.format(Locale.ROOT,
                "• Mathematics: %d / 25 Answered • Projected Score: +%d / 100", mathAns, mathScore));

        layoutMockActiveTest.setVisibility(View.GONE);
        findViewById(R.id.layoutBottomNav).setVisibility(View.GONE);
        btnSubmitMockTest.setVisibility(View.GONE);
        layoutMockSummary.setVisibility(View.VISIBLE);
    }

    private int countSectionAnswers(int section) {
        int count = 0;
        for (int q = 0; q < QUESTIONS_PER_SECTION; q++) {
            if (sectionQuestionStatus[section][q] == STATUS_ANSWERED) count++;
        }
        return count;
    }

    @Override
    public void dismiss() {
        countdownHandler.removeCallbacks(countdownRunnable);
        super.dismiss();
    }
}

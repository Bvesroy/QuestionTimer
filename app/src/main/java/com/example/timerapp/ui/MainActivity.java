package com.example.timerapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Rational;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.timerapp.R;
import com.example.timerapp.data.SessionStorage;
import com.example.timerapp.model.QuestionLog;
import com.example.timerapp.model.TimerSession;
import com.example.timerapp.model.TimerState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Modern AMOLED Black Fullscreen Immersive Question Solving Timer Activity.
 *
 * Implements:
 * 1. Edge-to-edge sticky immersive mode (hidden status & navigation bars).
 * 2. Uppercase geometric modern session title (clickable to rename).
 * 3. Subtle target display (e.g. "QUESTION 71 / 100", clickable to edit target).
 * 4. Huge tabular monospace timer readout (MM:SS.s or H:MM:SS.s) with dynamic size re-scaling.
 * 5. Fullscreen tap-anywhere gesture loop:
 *      - IDLE -> Tap starts timer
 *      - RUNNING -> Tap pauses timer, records current question duration
 *      - PAUSED -> Tap resets and immediately starts next question (Q1 -> Q2)
 * 6. Auto-hiding control buttons (Play, Discard, History, Settings) during RUNNING state.
 * 7. Hidden popup modal for History, Date-Grouped Sessions, and Dedicated Analytics Dashboard.
 * 8. Configurable target tracking & dynamic font size configuration.
 * 9. Responsive orientation handling: Portrait and Landscape without resetting timer or state.
 */
public class MainActivity extends Activity {

    // Views
    private View rootContainer;
    private View topBarContainer;
    private View bottomControlsContainer;
    private TextView tvSessionTitle;
    private TextView tvTarget;
    private TextView tvExamBanner;
    private ImageView ivEditTitle;
    private TextView tvTimerReadout;
    private TextView tvStateBadge;
    private TextView tvTotalStudyClock;
    private ImageButton btnDiscard;
    private ImageButton btnToggle;
    private ImageButton btnHistory;
    private ImageButton btnSettings;

    // Total Study Clock & Break Management
    private long sessionStudyStartRealtime = 0L;
    private long accumulatedStudyDurationMs = 0L;
    private boolean isBreakActive = false;
    private long breakStartRealtime = 0L;
    private long accumulatedBreakDurationMs = 0L;
    private long lastDiscardedQuestionElapsedMs = 0L;
    private boolean canUndoReset = false;

    // Smart Focus, Inactivity & Feedback
    private Vibrator vibrator;
    private ToneGenerator toneGenerator;
    private long lastInteractionRealtime = 0L;
    private long continuousRunningStartTime = 0L;
    private boolean hasShownIdleToast = false;
    private boolean hasShownBreakToast = false;

    // Timer Engine
    private TimerState currentState = TimerState.IDLE;
    private long startTimeRealtime = 0L;
    private long accumulatedElapsedMs = 0L;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerTicker = new Runnable() {
        @Override
        public void run() {
            if (currentState == TimerState.RUNNING) {
                updateTimerDisplay();
                checkIdleAndBreakReminders();
                timerHandler.postDelayed(this, 40); // ~25 FPS update for smooth tenths of a second
            }
        }
    };

    // UI Configuration & Animation State
    private String currentTimerSizeMode = SettingsDialog.SIZE_MEDIUM;
    private boolean controlsAreVisible = true;

    // Data & Persistence
    private SessionStorage sessionStorage;
    private final List<TimerSession> allSessions = new ArrayList<>();
    private TimerSession currentSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep screen awake during solving sessions
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Display cutout short edges (takes notch into full immersion)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        setContentView(R.layout.activity_main);

        sessionStorage = new SessionStorage(this);

        // Load saved timer size preference
        SharedPreferences prefs = getSharedPreferences(SettingsDialog.PREFS_NAME, Context.MODE_PRIVATE);
        currentTimerSizeMode = prefs.getString(SettingsDialog.KEY_TIMER_SIZE_MODE, SettingsDialog.SIZE_MEDIUM);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        lastInteractionRealtime = SystemClock.elapsedRealtime();

        initViews();
        loadOrCreateSession();
        setupGestureDetector();
        applyImmersiveMode();
        applyTheme();
        updateExamBanner();
        applyTimerSize();
        renderUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyImmersiveMode();
        applyTimerSize();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            applyImmersiveMode();
        }
    }

    /**
     * Enforces AMOLED sticky immersive mode hiding status bar and navigation bars.
     */
    private void applyImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    private void initViews() {
        rootContainer = findViewById(R.id.rootContainer);
        topBarContainer = findViewById(R.id.topBarContainer);
        bottomControlsContainer = findViewById(R.id.bottomControlsContainer);
        tvSessionTitle = findViewById(R.id.tvSessionTitle);
        tvTarget = findViewById(R.id.tvTarget);
        tvExamBanner = findViewById(R.id.tvExamBanner);
        ivEditTitle = findViewById(R.id.ivEditTitle);
        tvTimerReadout = findViewById(R.id.tvTimerReadout);
        tvStateBadge = findViewById(R.id.tvStateBadge);
        tvTotalStudyClock = findViewById(R.id.tvTotalStudyClock);
        if (tvTotalStudyClock != null) {
            tvTotalStudyClock.setOnClickListener(v -> handleStudyClockClick());
        }
        btnDiscard = findViewById(R.id.btnDiscard);
        btnToggle = findViewById(R.id.btnToggle);
        btnHistory = findViewById(R.id.btnHistory);
        btnSettings = findViewById(R.id.btnSettings);

        // Click on session title to rename
        findViewById(R.id.layoutSessionTitle).setOnClickListener(v -> showRenameDialog());

        // Click on target pill to edit target
        tvTarget.setOnClickListener(v -> showEditTargetDialog());

        // Click on exam banner to customize exam countdown
        if (tvExamBanner != null) {
            tvExamBanner.setOnClickListener(v -> showSettingsDialog());
        }

        // Subtle Control Buttons
        btnDiscard.setOnClickListener(v -> handleDiscardCurrentQuestion());
        btnToggle.setOnClickListener(v -> handleManualToggle());
        btnHistory.setOnClickListener(v -> showHistoryBottomSheet());
        btnSettings.setOnClickListener(v -> showSettingsDialog());
    }

    /**
     * Dynamically rescales the main timer readout text according to the user's
     * configured font size preference (Small, Medium, Large, Extra Large) and orientation.
     */
    private void applyTimerSize() {
        if (tvTimerReadout == null) return;

        boolean isLandscape = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
        int dimenResId;

        if (SettingsDialog.SIZE_SMALL.equals(currentTimerSizeMode)) {
            dimenResId = isLandscape ? R.dimen.text_timer_small_landscape : R.dimen.text_timer_small_portrait;
        } else if (SettingsDialog.SIZE_LARGE.equals(currentTimerSizeMode)) {
            dimenResId = isLandscape ? R.dimen.text_timer_large_landscape : R.dimen.text_timer_large_portrait;
        } else if (SettingsDialog.SIZE_XLARGE.equals(currentTimerSizeMode)) {
            dimenResId = isLandscape ? R.dimen.text_timer_xlarge_landscape : R.dimen.text_timer_xlarge_portrait;
        } else {
            // Default MEDIUM
            dimenResId = isLandscape ? R.dimen.text_timer_medium_landscape : R.dimen.text_timer_medium_portrait;
        }

        tvTimerReadout.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(dimenResId));
    }

    /**
     * Configures the full-screen tap-anywhere gesture loop while ignoring touches on interactive controls.
     * When RUNNING, the entire screen acts as an instant pause & record tap target.
     */
    private void setupGestureDetector() {
        rootContainer.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                // If not running and controls are visible, ignore taps on interactive bars
                if (currentState != TimerState.RUNNING) {
                    if (isTouchInsideView(topBarContainer, event) || isTouchInsideView(bottomControlsContainer, event)) {
                        return false;
                    }
                }
                onScreenTapped();
                return true;
            }
            return true;
        });
    }

    private boolean isTouchInsideView(View view, MotionEvent event) {
        if (view == null || view.getVisibility() != View.VISIBLE || view.getAlpha() < 0.1f) return false;
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        float x = event.getRawX();
        float y = event.getRawY();
        return (x >= location[0] && x <= (location[0] + view.getWidth())
                && y >= location[1] && y <= (location[1] + view.getHeight()));
    }

    /**
     * Tap anywhere gesture loop:
     * - IDLE / READY -> Tap starts timer
     * - RUNNING -> Tap pauses timer, records current question duration
     * - PAUSED -> Tap resets and immediately starts timer for next question (e.g. Q1 -> Q2)
     */
    private void onScreenTapped() {
        lastInteractionRealtime = SystemClock.elapsedRealtime();
        hasShownIdleToast = false;
        triggerTapFeedback();

        switch (currentState) {
            case IDLE:
                startTimer();
                break;
            case RUNNING:
                pauseAndRecordQuestion();
                break;
            case PAUSED:
                advanceAndStartNextQuestion();
                break;
        }
    }

    private void startTimer() {
        currentState = TimerState.RUNNING;
        lastInteractionRealtime = SystemClock.elapsedRealtime();
        continuousRunningStartTime = SystemClock.elapsedRealtime();
        if (sessionStudyStartRealtime == 0L && !isBreakActive) {
            sessionStudyStartRealtime = SystemClock.elapsedRealtime();
        }
        hasShownIdleToast = false;
        hasShownBreakToast = false;
        startTimeRealtime = SystemClock.elapsedRealtime() - accumulatedElapsedMs;
        timerHandler.post(timerTicker);
        renderUi();
    }

    private void pauseAndRecordQuestion() {
        currentState = TimerState.PAUSED;
        continuousRunningStartTime = 0L;
        timerHandler.removeCallbacks(timerTicker);

        long currentDurationMs = SystemClock.elapsedRealtime() - startTimeRealtime;
        accumulatedElapsedMs = currentDurationMs;

        // Log this question duration into active session
        int currentQNum = currentSession.getCurrentQuestion();
        QuestionLog log = new QuestionLog(
                System.currentTimeMillis(),
                currentSession.getId(),
                currentQNum,
                currentDurationMs,
                System.currentTimeMillis()
        );
        currentSession.addLog(log);
        persistSessions();

        renderUi();

        // Check if target questions goal is reached
        int target = currentSession.getTarget();
        if (target > 0 && currentSession.getCompletedCount() >= target) {
            Toast.makeText(this, "🎯 TARGET REACHED! (" + target + " questions completed)", Toast.LENGTH_LONG).show();
            promptSessionNotesIfNeeded(currentSession);
        }
    }

    private void advanceAndStartNextQuestion() {
        // Reset timer duration to 0 and immediately start for next question
        accumulatedElapsedMs = 0L;
        currentState = TimerState.RUNNING;
        lastInteractionRealtime = SystemClock.elapsedRealtime();
        continuousRunningStartTime = SystemClock.elapsedRealtime();
        hasShownIdleToast = false;
        hasShownBreakToast = false;
        startTimeRealtime = SystemClock.elapsedRealtime();
        timerHandler.post(timerTicker);
        renderUi();
    }

    private void handleManualToggle() {
        triggerTapFeedback();
        lastInteractionRealtime = SystemClock.elapsedRealtime();
        if (currentState == TimerState.RUNNING) {
            pauseAndRecordQuestion();
        } else if (currentState == TimerState.PAUSED) {
            advanceAndStartNextQuestion();
        } else {
            startTimer();
        }
    }

    private void handleDiscardCurrentQuestion() {
        triggerTapFeedback();
        if (currentState == TimerState.IDLE && accumulatedElapsedMs == 0L) {
            Toast.makeText(this, "Timer is already reset", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this, R.style.Theme_FullScreenTimer_Dialog)
                .setTitle(R.string.action_discard_confirm)
                .setMessage("This will reset current timer back to 0 without saving.")
                .setPositiveButton(R.string.action_confirm, (dialog, which) -> {
                    timerHandler.removeCallbacks(timerTicker);
                    accumulatedElapsedMs = 0L;
                    continuousRunningStartTime = 0L;
                    currentState = TimerState.IDLE;
                    renderUi();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    /**
     * Updates timer readout text and format (monospaced tabular numbers).
     */
    private void updateTimerDisplay() {
        long elapsed = (currentState == TimerState.RUNNING)
                ? (SystemClock.elapsedRealtime() - startTimeRealtime)
                : accumulatedElapsedMs;

        long totalSeconds = elapsed / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long tenths = (elapsed % 1000) / 100;

        if (minutes >= 60) {
            long hours = minutes / 60;
            minutes = minutes % 60;
            tvTimerReadout.setText(String.format(Locale.ROOT, "%d:%02d:%02d.%d", hours, minutes, seconds, tenths));
        } else {
            tvTimerReadout.setText(String.format(Locale.ROOT, "%02d:%02d.%d", minutes, seconds, tenths));
        }
    }

    /**
     * Controls smooth auto-hiding of control buttons when timer runs.
     * When timer transitions to RUNNING:
     *   Fades out all 4 buttons to alpha 0 and marks invisible/disabled.
     * When timer transitions to PAUSED or IDLE:
     *   Immediately restores all 4 buttons with smooth fade-in to alpha 1.
     */
    private void updateControlsVisibility(boolean visible, boolean animate) {
        if (bottomControlsContainer == null) return;
        if (controlsAreVisible == visible) return;

        controlsAreVisible = visible;
        long duration = animate ? 250L : 0L;

        if (visible) {
            bottomControlsContainer.setVisibility(View.VISIBLE);
            bottomControlsContainer.animate()
                    .alpha(1.0f)
                    .setDuration(duration)
                    .start();
        } else {
            bottomControlsContainer.animate()
                    .alpha(0.0f)
                    .setDuration(duration)
                    .withEndAction(() -> {
                        if (!controlsAreVisible) {
                            bottomControlsContainer.setVisibility(View.INVISIBLE);
                        }
                    })
                    .start();
        }

        // Enable/disable touch reception on buttons
        btnDiscard.setEnabled(visible);
        btnToggle.setEnabled(visible);
        btnHistory.setEnabled(visible);
        btnSettings.setEnabled(visible);
    }

    /**
     * Renders UI based on session data and timer state.
     */
    private void renderUi() {
        if (currentSession == null) return;

        // Session title
        tvSessionTitle.setText(currentSession.getName().toUpperCase(Locale.ROOT));

        // Target / Question counter display
        int currentQ = currentSession.getCurrentQuestion();
        int target = currentSession.getTarget();
        if (target > 0) {
            tvTarget.setText(String.format(Locale.ROOT, "QUESTION %d / %d", currentQ, target));
        } else {
            tvTarget.setText(String.format(Locale.ROOT, "QUESTION %d", currentQ));
        }

        // State Badge & Button Tints
        int accentColor = ThemeManager.getAccentColor(this);
        int secondaryColor = ThemeManager.getTextSecondaryColor(this);
        int primaryColor = ThemeManager.getTextPrimaryColor(this);

        switch (currentState) {
            case IDLE:
                tvStateBadge.setText(R.string.badge_ready);
                tvStateBadge.setBackgroundResource(R.drawable.bg_pill);
                tvStateBadge.setTextColor(secondaryColor);
                btnToggle.setImageResource(R.drawable.ic_play);
                btnToggle.setColorFilter(primaryColor);
                break;
            case RUNNING:
                tvStateBadge.setText(R.string.badge_running);
                tvStateBadge.setBackgroundResource(R.drawable.bg_pill_accent);
                tvStateBadge.setTextColor(accentColor);
                btnToggle.setImageResource(R.drawable.ic_pause);
                btnToggle.setColorFilter(accentColor);
                break;
            case PAUSED:
                tvStateBadge.setText(R.string.badge_paused);
                tvStateBadge.setBackgroundResource(R.drawable.bg_pill_orange);
                tvStateBadge.setTextColor(Color.parseColor("#FF9F0A"));
                btnToggle.setImageResource(R.drawable.ic_play);
                btnToggle.setColorFilter(Color.parseColor("#FF9F0A"));
                break;
        }

        // Manage auto-hiding controls based on state
        updateControlsVisibility(currentState != TimerState.RUNNING, true);

        updateTimerDisplay();
    }

    // ==========================================
    // History, Analytics & Session Bottom Sheet
    // ==========================================

    private void showHistoryBottomSheet() {
        HistoryBottomSheet sheet = new HistoryBottomSheet(this, currentSession, allSessions, new HistoryBottomSheet.HistoryActionListener() {
            @Override
            public void onNewSessionRequested() {
                createNewSession();
            }

            @Override
            public void onSwitchSessionRequested() {
                showSessionSwitcherDialog();
            }

            @Override
            public void onClearHistoryRequested() {
                currentSession.clearLogs();
                persistSessions();
                renderUi();
            }

            @Override
            public void onResumeSessionRequested(TimerSession session) {
                switchSession(session);
            }

            @Override
            public void onSessionNotesEdited(TimerSession session) {
                persistSessions();
            }
        });
        sheet.show();
    }

    // ==========================================
    // Settings Dialog
    // ==========================================

    private void showSettingsDialog() {
        SettingsDialog dialog = new SettingsDialog(this, currentSession, new SettingsDialog.OnSettingsChangedListener() {
            @Override
            public void onTimerSizeChanged(String sizeMode) {
                currentTimerSizeMode = sizeMode;
                applyTimerSize();
            }

            @Override
            public void onTargetsChanged(int targetSecPerQ, int targetSessionMin, int targetQuestions) {
                if (currentSession != null && targetQuestions > 0) {
                    currentSession.setTarget(targetQuestions);
                    persistSessions();
                }
                renderUi();
            }

            @Override
            public void onThemeChanged(String theme, String accent) {
                applyTheme();
                renderUi();
            }

            @Override
            public void onFocusControlsChanged() {
                updateExamBanner();
                renderUi();
            }
        });
        dialog.show();
    }

    // ==========================================
    // Dialogs: Rename & Target
    // ==========================================

    private void showRenameDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_title, null);
        EditText etName = dialogView.findViewById(R.id.etSessionName);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelRename);
        Button btnSave = dialogView.findViewById(R.id.btnSaveRename);

        etName.setText(currentSession.getName());
        etName.setSelection(etName.getText().length());

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_FullScreenTimer_Dialog)
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            if (!newName.isEmpty()) {
                currentSession.setName(newName);
                persistSessions();
                renderUi();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void showEditTargetDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_target, null);
        EditText etTarget = dialogView.findViewById(R.id.etTargetNumber);
        Button btnClear = dialogView.findViewById(R.id.btnClearTarget);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelTarget);
        Button btnSave = dialogView.findViewById(R.id.btnSaveTarget);

        if (currentSession.getTarget() > 0) {
            etTarget.setText(String.valueOf(currentSession.getTarget()));
            etTarget.setSelection(etTarget.getText().length());
        }

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_FullScreenTimer_Dialog)
                .setView(dialogView)
                .create();

        btnClear.setOnClickListener(v -> {
            currentSession.setTarget(0);
            persistSessions();
            renderUi();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String val = etTarget.getText().toString().trim();
            try {
                int target = Integer.parseInt(val);
                currentSession.setTarget(target);
                persistSessions();
                renderUi();
                dialog.dismiss();
            } catch (NumberFormatException ignored) {
            }
        });

        dialog.show();
    }

    private void showSessionSwitcherDialog() {
        Dialog switcherDialog = new Dialog(this, R.style.Theme_FullScreenTimer_BottomSheet);
        View view = getLayoutInflater().inflate(R.layout.dialog_session_manager, null);
        switcherDialog.setContentView(view);

        Window window = switcherDialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        switcherDialog.setCanceledOnTouchOutside(true);

        ListView lvSessions = view.findViewById(R.id.lvSessionsList);
        Button btnCreate = view.findViewById(R.id.btnCreateNewSessionInDialog);

        SessionsAdapter adapter = new SessionsAdapter(this, session -> {
            switchSession(session);
            switcherDialog.dismiss();
        });
        adapter.setSessions(allSessions, currentSession.getId());
        lvSessions.setAdapter(adapter);

        btnCreate.setOnClickListener(v -> {
            switcherDialog.dismiss();
            createNewSession();
        });

        switcherDialog.show();
    }

    // ==========================================
    // Session State Management
    // ==========================================

    private void loadOrCreateSession() {
        allSessions.clear();
        allSessions.addAll(sessionStorage.loadSessions());

        long activeId = sessionStorage.getActiveSessionId();
        if (activeId != -1) {
            for (TimerSession s : allSessions) {
                if (s.getId() == activeId) {
                    currentSession = s;
                    break;
                }
            }
        }

        if (currentSession == null) {
            if (!allSessions.isEmpty()) {
                currentSession = allSessions.get(0);
            } else {
                currentSession = new TimerSession(
                        0L,
                        getString(R.string.default_session_name),
                        50
                );
                allSessions.add(currentSession);
                persistSessions();
            }
        }
    }

    private void createNewSession() {
        TimerSession prevSession = currentSession;
        // Pause timer if running
        if (currentState == TimerState.RUNNING) {
            timerHandler.removeCallbacks(timerTicker);
            currentState = TimerState.IDLE;
        }
        accumulatedElapsedMs = 0L;

        TimerSession newSession = new TimerSession(
                0L,
                "SESSION " + (allSessions.size() + 1),
                50
        );
        allSessions.add(0, newSession);
        currentSession = newSession;
        persistSessions();
        renderUi();

        if (prevSession != null && prevSession.getCompletedCount() > 0 &&
                (prevSession.getNotes() == null || prevSession.getNotes().isEmpty())) {
            promptSessionNotesIfNeeded(prevSession);
        } else {
            showRenameDialog();
        }
    }

    private void switchSession(TimerSession session) {
        if (session == null || session.getId() == currentSession.getId()) return;

        if (currentState == TimerState.RUNNING) {
            timerHandler.removeCallbacks(timerTicker);
            currentState = TimerState.IDLE;
        }
        accumulatedElapsedMs = 0L;
        currentSession = session;
        persistSessions();
        renderUi();
    }

    private void persistSessions() {
        sessionStorage.saveSessions(allSessions);
        if (currentSession != null) {
            sessionStorage.saveActiveSessionId(currentSession.getId());
        }
    }

    // ==========================================
    // Orientation Change Handling
    // ==========================================

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Seamlessly re-apply immersive flags on rotation
        applyImmersiveMode();

        // Dynamically adjust timer readout font size and margins for portrait vs landscape
        applyTimerSize();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("TIMER_STATE", currentState);
        outState.putLong("ACCUMULATED_MS", (currentState == TimerState.RUNNING)
                ? (SystemClock.elapsedRealtime() - startTimeRealtime)
                : accumulatedElapsedMs);
        outState.putLong("ACTIVE_SESSION_ID", currentSession != null ? currentSession.getId() : -1);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        currentState = (TimerState) savedInstanceState.getSerializable("TIMER_STATE");
        if (currentState == null) {
            currentState = TimerState.IDLE;
        }
        accumulatedElapsedMs = savedInstanceState.getLong("ACCUMULATED_MS", 0L);
        if (currentState == TimerState.RUNNING) {
            startTimeRealtime = SystemClock.elapsedRealtime() - accumulatedElapsedMs;
            timerHandler.post(timerTicker);
        }
        renderUi();
    }

    // ==========================================
    // Picture-in-Picture (PiP) Integration
    // ==========================================

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        SharedPreferences prefs = getSharedPreferences(SettingsDialog.PREFS_NAME, Context.MODE_PRIVATE);
        boolean autoPip = prefs.getBoolean(SettingsDialog.KEY_AUTO_PIP, true);
        if (autoPip && currentState == TimerState.RUNNING && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPiPMode();
        }
    }

    private void enterPiPMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder();
                builder.setAspectRatio(new Rational(16, 9));
                enterPictureInPictureMode(builder.build());
            } catch (Exception e) {
                try {
                    enterPictureInPictureMode();
                } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        if (isInPictureInPictureMode) {
            if (topBarContainer != null) topBarContainer.setVisibility(View.GONE);
            if (bottomControlsContainer != null) bottomControlsContainer.setVisibility(View.GONE);
            if (tvStateBadge != null) tvStateBadge.setVisibility(View.GONE);
            if (tvTimerReadout != null) {
                tvTimerReadout.setTextSize(TypedValue.COMPLEX_UNIT_SP, 44);
            }
        } else {
            if (topBarContainer != null) topBarContainer.setVisibility(View.VISIBLE);
            if (tvStateBadge != null) tvStateBadge.setVisibility(View.VISIBLE);
            applyTimerSize();
            renderUi();
            applyImmersiveMode();
        }
    }

    // ==========================================
    // Smart Inactivity & Break Reminders
    // ==========================================

    private void checkIdleAndBreakReminders() {
        SharedPreferences prefs = getSharedPreferences(SettingsDialog.PREFS_NAME, Context.MODE_PRIVATE);

        // Break Reminder
        int breakMinutes = prefs.getInt(SettingsDialog.KEY_BREAK_MINUTES, 45);
        if (breakMinutes > 0 && continuousRunningStartTime > 0 && !hasShownBreakToast) {
            long runningMs = SystemClock.elapsedRealtime() - continuousRunningStartTime;
            if (runningMs >= breakMinutes * 60 * 1000L) {
                hasShownBreakToast = true;
                Toast.makeText(MainActivity.this, "Break Time: Continuous solving for " + breakMinutes + "m. Rest your eyes!", Toast.LENGTH_LONG).show();
                triggerTapFeedback();
            }
        }

        // Idle Inactivity Detection
        int idleMinutes = prefs.getInt(SettingsDialog.KEY_IDLE_MINUTES, 10);
        if (idleMinutes > 0 && lastInteractionRealtime > 0 && !hasShownIdleToast) {
            long idleMs = SystemClock.elapsedRealtime() - lastInteractionRealtime;
            if (idleMs >= idleMinutes * 60 * 1000L) {
                hasShownIdleToast = true;
                Toast.makeText(MainActivity.this, "Still solving Question #" + (currentSession != null ? currentSession.getCurrentQuestion() : 1) + "?", Toast.LENGTH_LONG).show();
                triggerTapFeedback();
            }
        }
    }

    // ==========================================
    // Visual Themes & Exam Countdown Banner
    // ==========================================

    private void applyTheme() {
        int bgColor = ThemeManager.getBackgroundColor(this);
        int textColor = ThemeManager.getTextPrimaryColor(this);
        int secondaryColor = ThemeManager.getTextSecondaryColor(this);
        int accentColor = ThemeManager.getAccentColor(this);

        if (rootContainer != null) {
            rootContainer.setBackgroundColor(bgColor);
        }
        if (tvSessionTitle != null) {
            tvSessionTitle.setTextColor(textColor);
        }
        if (ivEditTitle != null) {
            ivEditTitle.setColorFilter(secondaryColor);
        }
        if (tvTarget != null) {
            tvTarget.setTextColor(secondaryColor);
        }
        if (tvExamBanner != null) {
            tvExamBanner.setTextColor(accentColor);
        }
        if (tvTimerReadout != null) {
            tvTimerReadout.setTextColor(textColor);
        }

        // Control buttons tint
        if (btnDiscard != null) btnDiscard.setColorFilter(secondaryColor);
        if (btnHistory != null) btnHistory.setColorFilter(secondaryColor);
        if (btnSettings != null) btnSettings.setColorFilter(secondaryColor);

        // System bar icons adaptation for light themes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                int appearance = ThemeManager.isLightTheme(this)
                        ? (WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
                        : 0;
                controller.setSystemBarsAppearance(appearance,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            }
        }
    }

    private void updateExamBanner() {
        if (tvExamBanner == null) return;
        SharedPreferences prefs = getSharedPreferences(SettingsDialog.PREFS_NAME, Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(SettingsDialog.KEY_EXAM_BANNER_ENABLED, true);
        if (!enabled) {
            tvExamBanner.setVisibility(View.GONE);
            return;
        }

        String examName = prefs.getString(SettingsDialog.KEY_EXAM_NAME, "JEE ADVANCED").toUpperCase(Locale.ROOT);
        int days = prefs.getInt(SettingsDialog.KEY_EXAM_DAYS, 142);
        tvExamBanner.setText(String.format(Locale.ROOT, "%s IN %d DAYS • FOCUS", examName, days));
        tvExamBanner.setTextColor(ThemeManager.getAccentColor(this));
        tvExamBanner.setVisibility(View.VISIBLE);
    }

    // ==========================================
    // Haptic & Sound Tap Feedback
    // ==========================================

    private void triggerTapFeedback() {
        SharedPreferences prefs = getSharedPreferences(SettingsDialog.PREFS_NAME, Context.MODE_PRIVATE);

        // Haptic feedback
        boolean haptic = prefs.getBoolean(SettingsDialog.KEY_HAPTIC_ENABLED, true);
        if (haptic && vibrator != null && vibrator.hasVibrator()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(35);
                }
            } catch (Exception ignored) {}
        }

        // Sound alert
        boolean sound = prefs.getBoolean(SettingsDialog.KEY_SOUND_ENABLED, true);
        if (sound) {
            try {
                if (toneGenerator == null) {
                    toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60);
                }
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 35);
            } catch (Exception ignored) {}
        }
    }

    // ==========================================
    // Session Completion Notes Dialog
    // ==========================================

    private void promptSessionNotesIfNeeded(TimerSession session) {
        if (session == null || isFinishing()) return;
        SessionNotesDialog dialog = new SessionNotesDialog(this, session, updatedSession -> {
            persistSessions();
            renderUi();
        });
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerTicker);
        if (toneGenerator != null) {
            try {
                toneGenerator.release();
            } catch (Exception ignored) {}
            toneGenerator = null;
        }
    }
}

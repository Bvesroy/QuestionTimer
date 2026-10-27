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
import android.content.Intent;
import android.net.Uri;
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
import com.example.timerapp.data.backup.SafBackupEngine;
import com.example.timerapp.data.engine.TwoZoneHitDetector;
import com.example.timerapp.data.engine.TwoZoneHitDetector.TouchZone;
import com.example.timerapp.model.QuestionLog;
import com.example.timerapp.model.TimerSession;
import com.example.timerapp.model.TimerState;
import com.example.timerapp.receiver.DailyWallpaperRefreshReceiver;
import com.example.timerapp.widget.WidgetUpdateHelper;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
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

    // Two-Zone Answer Recording & Undo Views
    private View layoutUndoBanner;
    private TextView tvUndoMessage;
    private Button btnUndoAnswer;
    private TextView tvLiveAnalytics;
    private boolean isTwoZoneRecordingEnabled = false;
    private String currentJeeExamType = SettingsDialog.EXAM_TYPE_JEE_MAIN;
    private final Handler undoBannerHandler = new Handler(Looper.getMainLooper());
    private QuestionLog lastRecordedOutcomeLog = null;

    // Explicit Session State Machine concurrency guard
    private final Object stateLock = new Object();
    private long lastTapRealtime = 0L;

    // Total Study Clock & Break Management
    private long sessionStudyStartRealtime = 0L;
    private long accumulatedStudyDurationMs = 0L;
    private boolean isBreakActive = false;
    private long breakStartRealtime = 0L;
    private long accumulatedBreakDurationMs = 0L;
    private long lastDiscardedQuestionElapsedMs = 0L;
    private boolean canUndoReset = false;

    // SAF Request Codes
    private static final int REQUEST_CODE_SAF_FOLDER = 2001;
    private static final int REQUEST_CODE_SAF_CREATE = 2002;
    private static final int REQUEST_CODE_SAF_OPEN = 2003;

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
    private int widgetUpdateTickCounter = 0;
    private final Runnable timerTicker = new Runnable() {
        @Override
        public void run() {
            if (currentState.isRunning()) {
                updateTimerDisplay();
                checkIdleAndBreakReminders();
                widgetUpdateTickCounter++;
                if (widgetUpdateTickCounter >= 25) { // update widgets approx every second
                    widgetUpdateTickCounter = 0;
                    updateWidgets();
                }
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

        // Load saved timer size preference & Two-Zone Answer setting
        SharedPreferences prefs = getSharedPreferences(SettingsDialog.PREFS_NAME, Context.MODE_PRIVATE);
        currentTimerSizeMode = prefs.getString(SettingsDialog.KEY_TIMER_SIZE_MODE, SettingsDialog.SIZE_MEDIUM);
        isTwoZoneRecordingEnabled = prefs.getBoolean(SettingsDialog.KEY_TWO_ZONE_RECORDING, false);
        currentJeeExamType = prefs.getString(SettingsDialog.KEY_JEE_EXAM_TYPE, SettingsDialog.EXAM_TYPE_JEE_MAIN);

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
        handleWidgetActions(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleWidgetActions(intent);
    }

    private void handleWidgetActions(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (com.example.timerapp.widget.WidgetUpdateHelper.ACTION_WIDGET_TOGGLE.equals(action)) {
            handleManualToggle();
        } else if (com.example.timerapp.widget.WidgetUpdateHelper.ACTION_WIDGET_BREAK.equals(action)) {
            handleStudyClockClick();
        }
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
        tvLiveAnalytics = findViewById(R.id.tvLiveAnalytics);
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

        // Transient Undo Notification Banner
        layoutUndoBanner = findViewById(R.id.layoutUndoBanner);
        tvUndoMessage = findViewById(R.id.tvUndoMessage);
        btnUndoAnswer = findViewById(R.id.btnUndoAnswer);
        if (btnUndoAnswer != null) {
            btnUndoAnswer.setOnClickListener(v -> undoLastAnswerOutcome());
        }
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
    /**
     * Configures full-screen tap gesture loop with Two-Zone Answer Recording support.
     * When Two-Zone Answer Recording is active:
     * - Screen is split into two equal horizontal zones (integer pixel height / 2).
     * - Upper zone = INCORRECT, Lower zone = SOLVED (identical AMOLED background, no dividing lines).
     * - Deliberate taps immediately record authoritative duration, save outcome, and advance to next question
     *   without resetting continuous Total Study Clock.
     * - Shows transient Undo notification.
     * - Prevents unintended answers on controls, rotation, pause, break, or reset.
     */
    private void setupGestureDetector() {
        rootContainer.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (isTouchInsideView(topBarContainer, event) ||
                    isTouchInsideView(bottomControlsContainer, event) ||
                    isTouchInsideView(layoutUndoBanner, event)) {
                    return false;
                }
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                // Ignore touches on interactive top/bottom bars or undo banner
                if (isTouchInsideView(topBarContainer, event) ||
                    isTouchInsideView(bottomControlsContainer, event) ||
                    isTouchInsideView(layoutUndoBanner, event)) {
                    return false;
                }

                // Two-Zone Answer Recording when timer is actively running
                if (isTwoZoneRecordingEnabled && currentState.isRunning()) {
                    int width = Math.max(1, rootContainer.getWidth());
                    int height = Math.max(1, rootContainer.getHeight());
                    TwoZoneHitDetector detector = new TwoZoneHitDetector(width, height);
                    TouchZone zone = detector.detectVerticalZone(event.getX(), event.getY());
                    if (zone == TouchZone.ZONE_UPPER) {
                        recordTwoZoneOutcome("INCORRECT");
                        return true;
                    } else if (zone == TouchZone.ZONE_LOWER) {
                        recordTwoZoneOutcome("SOLVED");
                        return true;
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
     * Thread-safe, atomic, and idempotent state machine transition preventing rapid double taps.
     */
    private boolean transitionTo(TimerState newState) {
        synchronized (stateLock) {
            long now = SystemClock.elapsedRealtime();
            if (now - lastTapRealtime < 180 && (newState == currentState || newState == TimerState.RECORDING_OUTCOME)) {
                return false;
            }
            lastTapRealtime = now;
            currentState = newState;
            return true;
        }
    }

    /**
     * Tap anywhere gesture loop:
     * - IDLE / READY -> Tap starts timer
     * - RUNNING -> Tap pauses timer, records current question duration
     * - PAUSED -> Tap resets and immediately starts timer for next question (e.g. Q1 -> Q2)
     */
    private void onScreenTapped() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastTapRealtime < 180) {
            return;
        }
        lastInteractionRealtime = now;
        hasShownIdleToast = false;
        triggerTapFeedback();

        if (currentState == TimerState.IDLE) {
            startTimer();
        } else if (currentState.isRunning()) {
            pauseAndRecordQuestion();
        } else if (currentState.isPaused()) {
            advanceAndStartNextQuestion();
        } else if (currentState == TimerState.SESSION_ENDED) {
            createNewSession();
        }
    }

    /**
     * Two-Zone Answer Recording:
     * Authoritatively records duration, saves outcome (SOLVED or INCORRECT), advances question count,
     * resets active question clock to 0, preserves Total Study Clock without reset, and shows transient Undo control.
     */
    private void recordTwoZoneOutcome(String outcome) {
        if (!transitionTo(TimerState.RECORDING_OUTCOME)) {
            return;
        }

        long authoritativeDuration = (startTimeRealtime > 0)
                ? (SystemClock.elapsedRealtime() - startTimeRealtime)
                : accumulatedElapsedMs;

        triggerTwoZoneHaptic(outcome);

        int currentQNum = (currentSession != null) ? currentSession.getCurrentQuestion() : 1;
        long sessId = (currentSession != null) ? currentSession.getId() : 1L;
        QuestionLog log = new QuestionLog(
                System.currentTimeMillis(),
                sessId,
                currentQNum,
                authoritativeDuration,
                System.currentTimeMillis(),
                "", "", "MEDIUM", outcome, 3,
                "INCORRECT".equals(outcome) ? "MISTAKE" : "", ""
        );

        if (currentSession != null) {
            currentSession.addLog(log);
            persistSessions();
        }

        showUndoAnswerBanner(log);

        // Pause timer upon recording outcome:
        // Tapping upper pauses and marks Wrong; tapping lower pauses and marks Correct.
        // Next screen tap resumes timing for the subsequent question.
        transitionTo(TimerState.PAUSED);
        continuousRunningStartTime = 0L;
        timerHandler.removeCallbacks(timerTicker);
        accumulatedElapsedMs = authoritativeDuration;

        updateWidgets();
        renderUi();

        // Target check
        if (currentSession != null) {
            int target = currentSession.getTarget();
            if (target > 0 && currentSession.getCompletedCount() >= target) {
                Toast.makeText(this, "🎯 TARGET REACHED! (" + target + " questions completed)", Toast.LENGTH_LONG).show();
                checkAutoApplyWallpaper();
                promptSessionNotesIfNeeded(currentSession);
            }
        }
    }

    private void showUndoAnswerBanner(QuestionLog log) {
        lastRecordedOutcomeLog = log;
        if (layoutUndoBanner == null || tvUndoMessage == null) return;

        undoBannerHandler.removeCallbacksAndMessages(null);
        boolean isIncorrect = "INCORRECT".equals(log.getOutcome()) || "WRONG".equals(log.getOutcome());
        double penalty = "JEE_ADVANCED".equalsIgnoreCase(currentJeeExamType) ? 2.0 : 1.0;
        String markStr = isIncorrect ? ("-" + (int) penalty + " PTS") : "+4 PTS";
        String outcomeStr = isIncorrect ? "WRONG" : "CORRECT";
        tvUndoMessage.setText(String.format(Locale.ROOT, "Q#%d %s (%s, %s)",
                log.getQuestionNumber(), outcomeStr, markStr, log.getFormattedDuration()));
        layoutUndoBanner.setVisibility(View.VISIBLE);
        layoutUndoBanner.setAlpha(1.0f);

        // Transient auto-dismiss in 4.5 seconds
        undoBannerHandler.postDelayed(() -> {
            if (layoutUndoBanner != null) {
                layoutUndoBanner.animate()
                        .alpha(0.0f)
                        .setDuration(250)
                        .withEndAction(() -> layoutUndoBanner.setVisibility(View.GONE))
                        .start();
            }
        }, 4500);
    }

    private void undoLastAnswerOutcome() {
        triggerTapFeedback();
        if (currentSession == null || currentSession.getLogs().isEmpty()) return;

        QuestionLog removed = currentSession.removeLastLog();
        persistSessions();

        if (layoutUndoBanner != null) {
            layoutUndoBanner.setVisibility(View.GONE);
        }

        if (removed != null) {
            accumulatedElapsedMs = removed.getDurationMs();
            List<QuestionLog> remainingLogs = currentSession.getLogs();
            lastRecordedOutcomeLog = remainingLogs.isEmpty() ? null : remainingLogs.get(remainingLogs.size() - 1);
            Toast.makeText(this, "Undid Question #" + removed.getQuestionNumber() + " (" + removed.getOutcome() + ")", Toast.LENGTH_SHORT).show();
        }
        renderUi();
        updateWidgets();
    }

    private void triggerTwoZoneHaptic(String outcome) {
        SharedPreferences prefs = getSharedPreferences(SettingsDialog.PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(SettingsDialog.KEY_HAPTIC_ENABLED, true)) return;
        if (vibrator == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if ("INCORRECT".equals(outcome)) {
                    vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 20, 35, 20}, -1));
                } else {
                    vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
                }
            } else {
                vibrator.vibrate(25);
            }
        } catch (Exception ignored) {}
    }

    private void startTimer() {
        transitionTo(TimerState.RUNNING_QUESTION);
        lastInteractionRealtime = SystemClock.elapsedRealtime();
        continuousRunningStartTime = SystemClock.elapsedRealtime();
        if (sessionStudyStartRealtime == 0L && !isBreakActive) {
            sessionStudyStartRealtime = SystemClock.elapsedRealtime();
        }
        hasShownIdleToast = false;
        hasShownBreakToast = false;
        startTimeRealtime = SystemClock.elapsedRealtime() - accumulatedElapsedMs;
        timerHandler.post(timerTicker);
        updateWidgets();
        renderUi();
    }

    private void pauseAndRecordQuestion() {
        transitionTo(TimerState.PAUSED);
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

        updateWidgets();
        renderUi();

        // Check if target questions goal is reached
        int target = currentSession.getTarget();
        if (target > 0 && currentSession.getCompletedCount() >= target) {
            Toast.makeText(this, "🎯 TARGET REACHED! (" + target + " questions completed)", Toast.LENGTH_LONG).show();
            checkAutoApplyWallpaper();
            promptSessionNotesIfNeeded(currentSession);
        }
    }

    private void advanceAndStartNextQuestion() {
        // Reset timer duration to 0 and immediately start for next question
        accumulatedElapsedMs = 0L;
        transitionTo(TimerState.RUNNING_QUESTION);
        lastInteractionRealtime = SystemClock.elapsedRealtime();
        continuousRunningStartTime = SystemClock.elapsedRealtime();
        if (sessionStudyStartRealtime == 0L && !isBreakActive) {
            sessionStudyStartRealtime = SystemClock.elapsedRealtime();
        }
        hasShownIdleToast = false;
        hasShownBreakToast = false;
        startTimeRealtime = SystemClock.elapsedRealtime();
        timerHandler.post(timerTicker);
        updateWidgets();
        renderUi();
    }

    private void handleManualToggle() {
        triggerTapFeedback();
        lastInteractionRealtime = SystemClock.elapsedRealtime();
        if (currentState.isRunning()) {
            pauseAndRecordQuestion();
        } else if (currentState == TimerState.PAUSED) {
            advanceAndStartNextQuestion();
        } else {
            startTimer();
        }
    }

    private void handleStudyClockClick() {
        triggerTapFeedback();
        if (isBreakActive) {
            long breakMs = (breakStartRealtime > 0) ? (SystemClock.elapsedRealtime() - breakStartRealtime) : 0L;
            new AlertDialog.Builder(this, R.style.Theme_FullScreenTimer_Dialog)
                    .setTitle("Break in Progress")
                    .setMessage(String.format(Locale.ROOT, "Current Break: %s\nTotal Study Time: %s",
                            formatTimeDisplay(breakMs),
                            formatTimeDisplay(accumulatedStudyDurationMs)))
                    .setPositiveButton("End Break & Resume Study", (dialog, which) -> endBreak())
                    .setNegativeButton("Keep Resting", null)
                    .show();
        } else {
            long currentStudyMs = accumulatedStudyDurationMs;
            if (sessionStudyStartRealtime > 0) {
                currentStudyMs += (SystemClock.elapsedRealtime() - sessionStudyStartRealtime);
            }
            final long studyMs = currentStudyMs;
            CharSequence[] options = new CharSequence[]{
                    "☕ Take a Break",
                    "🏷️ Tag Question / Notes",
                    "📝 JEE Mock Test (180 min)",
                    "🏁 Complete & End Session"
            };
            new AlertDialog.Builder(this, R.style.Theme_FullScreenTimer_Dialog)
                    .setTitle(String.format(Locale.ROOT, "Total Study Time: %s", formatTimeDisplay(studyMs)))
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            startBreak();
                        } else if (which == 1) {
                            openQuestionTaggingForCurrent();
                        } else if (which == 2) {
                            new MockTestDialog(MainActivity.this).show();
                        } else if (which == 3) {
                            endStudySession();
                        }
                    })
                    .setNegativeButton("Close", null)
                    .show();
        }
    }

    private void startBreak() {
        if (isBreakActive) return;
        if (currentState.isRunning()) {
            pauseAndRecordQuestion();
        }
        if (sessionStudyStartRealtime > 0) {
            accumulatedStudyDurationMs += (SystemClock.elapsedRealtime() - sessionStudyStartRealtime);
            sessionStudyStartRealtime = 0L;
        }
        isBreakActive = true;
        breakStartRealtime = SystemClock.elapsedRealtime();
        if (currentSession != null) {
            currentSession.setCurrentBreakStartTime(System.currentTimeMillis());
            currentSession.setTotalStudyDurationMs(accumulatedStudyDurationMs);
            persistSessions();
        }
        renderUi();
        Toast.makeText(this, "☕ Break started. Total study clock paused.", Toast.LENGTH_SHORT).show();
    }

    private void endBreak() {
        if (!isBreakActive) return;
        long duration = (breakStartRealtime > 0) ? (SystemClock.elapsedRealtime() - breakStartRealtime) : 0L;
        accumulatedBreakDurationMs += duration;
        if (currentSession != null) {
            long breakEnd = System.currentTimeMillis();
            long breakStart = breakEnd - duration;
            currentSession.addBreak(new com.example.timerapp.data.model.BreakLog(currentSession.getId(), breakStart, breakEnd));
            currentSession.setCurrentBreakStartTime(0L);
            currentSession.setTotalBreakDurationMs(accumulatedBreakDurationMs);
            persistSessions();
        }
        isBreakActive = false;
        breakStartRealtime = 0L;
        sessionStudyStartRealtime = SystemClock.elapsedRealtime();
        renderUi();
        Toast.makeText(this, "Resumed study session! Total study clock running.", Toast.LENGTH_SHORT).show();
    }

    private void endStudySession() {
        if (isBreakActive) {
            endBreak();
        }
        if (sessionStudyStartRealtime > 0) {
            accumulatedStudyDurationMs += (SystemClock.elapsedRealtime() - sessionStudyStartRealtime);
            sessionStudyStartRealtime = 0L;
        }
        if (currentSession != null) {
            currentSession.setTotalStudyDurationMs(accumulatedStudyDurationMs);
            currentSession.setTotalBreakDurationMs(accumulatedBreakDurationMs);
            currentSession.setStatus("COMPLETED");
            persistSessions();
            promptSessionNotesIfNeeded(currentSession);
        }
        renderUi();
    }

    private void openQuestionTaggingForCurrent() {
        if (currentSession == null || currentSession.getLogs().isEmpty()) {
            Toast.makeText(this, "No completed questions yet in this session to tag.", Toast.LENGTH_SHORT).show();
            return;
        }
        List<QuestionLog> logs = currentSession.getLogs();
        int logIndex = logs.size() - 1;
        QuestionLog lastLog = logs.get(logIndex);
        QuestionTaggingDialog dialog = new QuestionTaggingDialog(this, lastLog, updatedLog -> {
            currentSession.replaceLog(logIndex, updatedLog);
            persistSessions();
            Toast.makeText(this, "Tags saved for Question #" + updatedLog.getQuestionNumber(), Toast.LENGTH_SHORT).show();
        });
        dialog.show();
    }

    private void openWallpaperPreviewDialog() {
        int streak = (sessionStorage != null) ? sessionStorage.calculateCurrentStreak() : 1;
        long targetMillis = (sessionStorage != null && !sessionStorage.getAllExamCountdowns().isEmpty())
                ? sessionStorage.getAllExamCountdowns().get(0).getTargetDateMillis()
                : com.example.timerapp.wallpaper.JeeWallpaperGenerator.DEFAULT_JEE_2027_MILLIS;
        new WallpaperPreviewDialog(this, allSessions, targetMillis, streak).show();
    }

    private void openMistakeReviewDialog() {
        new MistakeReviewDialog(this, allSessions, sessionStorage, this::renderUi).show();
    }

    private void handleDiscardCurrentQuestion() {
        triggerTapFeedback();
        long currentElapsed = (currentState.isRunning())
                ? (SystemClock.elapsedRealtime() - startTimeRealtime)
                : accumulatedElapsedMs;
        if (currentState == TimerState.IDLE && currentElapsed == 0L) {
            Toast.makeText(this, "Timer is already at 00:00:00", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this, R.style.Theme_FullScreenTimer_Dialog)
                .setTitle("Reset Current Question?")
                .setMessage("Resets Question #" + (currentSession != null ? currentSession.getCurrentQuestion() : 1) +
                        " timer to 00:00:00. Previous questions, study clock, and session records will NOT be affected.")
                .setPositiveButton("Reset Question", (dialog, which) -> {
                    timerHandler.removeCallbacks(timerTicker);
                    lastDiscardedQuestionElapsedMs = currentElapsed;
                    canUndoReset = true;
                    accumulatedElapsedMs = 0L;
                    continuousRunningStartTime = 0L;
                    currentState = TimerState.IDLE;
                    renderUi();
                    showUndoResetDialog(lastDiscardedQuestionElapsedMs);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showUndoResetDialog(long discardedMs) {
        new AlertDialog.Builder(this, R.style.Theme_FullScreenTimer_Dialog)
                .setTitle("Question Reset")
                .setMessage(String.format(Locale.ROOT, "Reset Question #%d (%s was discarded). Undo?",
                        currentSession != null ? currentSession.getCurrentQuestion() : 1,
                        formatTimeDisplay(discardedMs)))
                .setPositiveButton("Undo", (dialog, which) -> {
                    if (canUndoReset) {
                        accumulatedElapsedMs = lastDiscardedQuestionElapsedMs;
                        lastDiscardedQuestionElapsedMs = 0L;
                        canUndoReset = false;
                        currentState = TimerState.PAUSED;
                        renderUi();
                        Toast.makeText(this, "Question timer restored!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Dismiss", (dialog, which) -> canUndoReset = false)
                .show();
    }

    private void updateStudyClockDisplay() {
        if (tvTotalStudyClock == null || currentSession == null) return;

        long studyMs = accumulatedStudyDurationMs;
        if (sessionStudyStartRealtime > 0 && !isBreakActive) {
            studyMs += (SystemClock.elapsedRealtime() - sessionStudyStartRealtime);
        }

        long breakMs = accumulatedBreakDurationMs;
        if (isBreakActive && breakStartRealtime > 0) {
            breakMs += (SystemClock.elapsedRealtime() - breakStartRealtime);
        }

        long studySec = studyMs / 1000;
        long studyH = studySec / 3600;
        long studyM = (studySec % 3600) / 60;
        long studyS = studySec % 60;

        long breakSec = breakMs / 1000;
        long breakM = breakSec / 60;
        long breakS = breakSec % 60;

        if (isBreakActive) {
            tvTotalStudyClock.setText(String.format(Locale.ROOT, "☕ ON BREAK: %02d:%02d • STUDY: %02d:%02d:%02d", breakM, breakS, studyH, studyM, studyS));
            tvTotalStudyClock.setTextColor(Color.parseColor("#FF9F0A"));
        } else {
            tvTotalStudyClock.setText(String.format(Locale.ROOT, "STUDY: %02d:%02d:%02d • BREAK: %02d:%02d", studyH, studyM, studyS, breakM, breakS));
            tvTotalStudyClock.setTextColor(ThemeManager.getTextSecondaryColor(this));
        }
    }

    private String formatTimeDisplay(long elapsedMs) {
        long totalSeconds = Math.max(0, elapsedMs) / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
        }
    }

    /**
     * Updates timer readout text and format (monospaced tabular numbers).
     */
    private void updateTimerDisplay() {
        long elapsed = (currentState.isRunning())
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
        updateStudyClockDisplay();
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
            case RUNNING_QUESTION:
                if (isTwoZoneRecordingEnabled) {
                    tvStateBadge.setText("UPPER=WRONG • LOWER=CORRECT");
                } else {
                    tvStateBadge.setText(R.string.badge_running);
                }
                tvStateBadge.setBackgroundResource(R.drawable.bg_pill_accent);
                tvStateBadge.setTextColor(accentColor);
                btnToggle.setImageResource(R.drawable.ic_pause);
                btnToggle.setColorFilter(accentColor);
                break;
            case PAUSED:
                if (isTwoZoneRecordingEnabled && lastRecordedOutcomeLog != null) {
                    boolean isWrong = "INCORRECT".equals(lastRecordedOutcomeLog.getOutcome()) || "WRONG".equals(lastRecordedOutcomeLog.getOutcome());
                    double penalty = "JEE_ADVANCED".equalsIgnoreCase(currentJeeExamType) ? 2.0 : 1.0;
                    String scoreDelta = isWrong ? ("-" + (int) penalty) : "+4";
                    String outcomeLabel = isWrong ? "WRONG" : "CORRECT";
                    int nextQ = (currentSession != null) ? currentSession.getCurrentQuestion() : 2;
                    tvStateBadge.setText(String.format(Locale.ROOT, "%s (%s PTS) • TAP FOR Q%d", outcomeLabel, scoreDelta, nextQ));
                    tvStateBadge.setBackgroundResource(isWrong ? R.drawable.bg_pill_orange : R.drawable.bg_pill_accent);
                    tvStateBadge.setTextColor(isWrong ? Color.parseColor("#FF9F0A") : accentColor);
                } else {
                    tvStateBadge.setText(R.string.badge_paused);
                    tvStateBadge.setBackgroundResource(R.drawable.bg_pill_orange);
                    tvStateBadge.setTextColor(Color.parseColor("#FF9F0A"));
                }
                btnToggle.setImageResource(R.drawable.ic_play);
                btnToggle.setColorFilter(Color.parseColor("#FF9F0A"));
                break;
            case EXPLICIT_BREAK:
                tvStateBadge.setText("ON BREAK • TAP TO RESUME");
                tvStateBadge.setBackgroundResource(R.drawable.bg_pill_orange);
                tvStateBadge.setTextColor(Color.parseColor("#FF9F0A"));
                btnToggle.setImageResource(R.drawable.ic_play);
                btnToggle.setColorFilter(Color.parseColor("#FF9F0A"));
                break;
            case SESSION_ENDED:
                tvStateBadge.setText("SESSION ENDED");
                tvStateBadge.setBackgroundResource(R.drawable.bg_pill);
                tvStateBadge.setTextColor(secondaryColor);
                btnToggle.setImageResource(R.drawable.ic_play);
                btnToggle.setColorFilter(secondaryColor);
                break;
            default:
                break;
        }

        // Manage auto-hiding controls based on state
        updateControlsVisibility(!currentState.isRunning(), true);

        updateTimerDisplay();

        // Live Performance Analytics KPI Display
        if (tvLiveAnalytics != null) {
            if (isTwoZoneRecordingEnabled && currentSession != null && !currentSession.getLogs().isEmpty()) {
                com.example.timerapp.data.analytics.EstimatedMarksCalculator.PerformanceAnalytics analytics =
                        com.example.timerapp.data.analytics.EstimatedMarksCalculator.computeAnalytics(
                                currentJeeExamType, currentSession.getLogs(), currentSession.getTarget()
                        );
                tvLiveAnalytics.setText(analytics.getFormattedSummary());
                tvLiveAnalytics.setVisibility(View.VISIBLE);
            } else {
                tvLiveAnalytics.setVisibility(View.GONE);
            }
        }
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

            @Override
            public void onTwoZoneRecordingChanged(boolean enabled) {
                onTwoZoneRecordingChanged(enabled, currentJeeExamType);
            }

            @Override
            public void onTwoZoneRecordingChanged(boolean enabled, String examType) {
                isTwoZoneRecordingEnabled = enabled;
                if (examType != null) {
                    currentJeeExamType = examType;
                }
                renderUi();
            }

            @Override
            public void onOpenWallpaperPreview() {
                openWallpaperPreviewDialog();
            }

            @Override
            public void onOpenMistakeReview() {
                openMistakeReviewDialog();
            }

            @Override
            public void onChooseBackupFolderRequested() {
                chooseSafBackupFolder();
            }

            @Override
            public void onBackupNowRequested() {
                createSafBackup();
            }

            @Override
            public void onRestoreBackupRequested() {
                restoreSafBackup();
            }
        });
        dialog.show();
    }

    private void chooseSafBackupFolder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            try {
                startActivityForResult(intent, REQUEST_CODE_SAF_FOLDER);
            } catch (Exception e) {
                Toast.makeText(this, "Folder picker not available: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "SAF requires Android 5.0+", Toast.LENGTH_SHORT).show();
        }
    }

    private void createSafBackup() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "QuestionTimer_Backup_" + System.currentTimeMillis() + ".json");
        try {
            startActivityForResult(intent, REQUEST_CODE_SAF_CREATE);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open SAF document creator: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void restoreSafBackup() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        try {
            startActivityForResult(intent, REQUEST_CODE_SAF_OPEN);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open file picker: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null) return;

        Uri uri = data.getData();
        if (uri == null) return;

        if (requestCode == REQUEST_CODE_SAF_FOLDER) {
            final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    getContentResolver().takePersistableUriPermission(uri, takeFlags);
                }
            } catch (Exception ignored) {}
            SharedPreferences prefs = getSharedPreferences(SettingsDialog.PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .putString(SettingsDialog.KEY_SAF_BACKUP_URI, uri.toString())
                    .putString(SettingsDialog.KEY_SAF_BACKUP_PATH, uri.getLastPathSegment() != null ? uri.getLastPathSegment() : uri.toString())
                    .apply();
            Toast.makeText(this, "SAF folder selected: " + uri.getLastPathSegment(), Toast.LENGTH_SHORT).show();
        } else if (requestCode == REQUEST_CODE_SAF_CREATE) {
            try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                if (os != null) {
                    JSONObject prefsJson = new JSONObject();
                    SharedPreferences prefs = getSharedPreferences(SettingsDialog.PREFS_NAME, Context.MODE_PRIVATE);
                    prefsJson.put(SettingsDialog.KEY_TIMER_SIZE_MODE, currentTimerSizeMode);
                    sessionStorage.exportSafBackup(os, prefsJson);
                    Toast.makeText(this, "SAF Backup archive created successfully!", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Backup creation failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_CODE_SAF_OPEN) {
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                SafBackupEngine.ValidationResult val = sessionStorage.validateSafBackup(is);
                if (is != null) is.close();

                if (!val.isValid) {
                    Toast.makeText(this, "Invalid backup: " + val.errorMessage, Toast.LENGTH_LONG).show();
                    return;
                }

                new AlertDialog.Builder(this, R.style.Theme_FullScreenTimer_Dialog)
                        .setTitle("Restore Backup (v" + val.schemaVersion + ")")
                        .setMessage(String.format(Locale.ROOT, "Backup archive validated (SHA-256 Verified).\nFound %d records.\n\nChoose Restore Mode:", val.totalRecords))
                        .setPositiveButton("Replace All (Wipe & Restore)", (dialog, which) -> {
                            performSafRestore(uri, SafBackupEngine.RestoreMode.REPLACE);
                        })
                        .setNeutralButton("Merge with Existing", (dialog, which) -> {
                            performSafRestore(uri, SafBackupEngine.RestoreMode.MERGE);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            } catch (Exception e) {
                Toast.makeText(this, "Restore check failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void performSafRestore(Uri uri, SafBackupEngine.RestoreMode mode) {
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is != null) {
                SafBackupEngine.RestoreResult result = sessionStorage.restoreSafBackup(is, mode);
                if (result.success) {
                    loadOrCreateSession();
                    renderUi();
                    Toast.makeText(this, String.format(Locale.ROOT, "Restored %d items! (%d sessions, %d questions)",
                            result.getTotalItemsRestored(), result.sessionsRestored, result.logsRestored), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Restore failed: " + result.message, Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error during restore: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
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

        if (currentSession != null) {
            accumulatedStudyDurationMs = currentSession.getTotalStudyDurationMs();
            accumulatedBreakDurationMs = currentSession.getTotalBreakDurationMs();
            if (currentSession.isOnBreak()) {
                isBreakActive = true;
                breakStartRealtime = SystemClock.elapsedRealtime();
            }
        }
    }

    private void createNewSession() {
        TimerSession prevSession = currentSession;
        // Pause timer if running
        if (currentState.isRunning()) {
            timerHandler.removeCallbacks(timerTicker);
            currentState = TimerState.IDLE;
        }
        accumulatedElapsedMs = 0L;
        accumulatedStudyDurationMs = 0L;
        sessionStudyStartRealtime = 0L;
        isBreakActive = false;
        breakStartRealtime = 0L;
        accumulatedBreakDurationMs = 0L;
        lastDiscardedQuestionElapsedMs = 0L;
        canUndoReset = false;

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

        if (currentState.isRunning()) {
            timerHandler.removeCallbacks(timerTicker);
            currentState = TimerState.IDLE;
        }
        accumulatedElapsedMs = 0L;
        currentSession = session;
        accumulatedStudyDurationMs = session.getTotalStudyDurationMs();
        accumulatedBreakDurationMs = session.getTotalBreakDurationMs();
        isBreakActive = session.isOnBreak();
        sessionStudyStartRealtime = (currentState.isRunning() && !isBreakActive) ? SystemClock.elapsedRealtime() : 0L;
        persistSessions();
        renderUi();
    }

    private void persistSessions() {
        if (currentSession != null) {
            long currentStudyMs = accumulatedStudyDurationMs;
            if (sessionStudyStartRealtime > 0 && !isBreakActive) {
                currentStudyMs += (SystemClock.elapsedRealtime() - sessionStudyStartRealtime);
            }
            currentSession.setTotalStudyDurationMs(currentStudyMs);
            currentSession.setTotalBreakDurationMs(accumulatedBreakDurationMs);
        }
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
        outState.putLong("ACCUMULATED_MS", (currentState.isRunning())
                ? (SystemClock.elapsedRealtime() - startTimeRealtime)
                : accumulatedElapsedMs);
        outState.putLong("ACTIVE_SESSION_ID", currentSession != null ? currentSession.getId() : -1);
        outState.putLong("STUDY_ACCUMULATED_MS", accumulatedStudyDurationMs);
        outState.putLong("STUDY_START_REALTIME", sessionStudyStartRealtime);
        outState.putBoolean("IS_BREAK_ACTIVE", isBreakActive);
        outState.putLong("BREAK_START_REALTIME", breakStartRealtime);
        outState.putLong("BREAK_ACCUMULATED_MS", accumulatedBreakDurationMs);
        outState.putLong("LAST_DISCARDED_MS", lastDiscardedQuestionElapsedMs);
        outState.putBoolean("CAN_UNDO_RESET", canUndoReset);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        currentState = (TimerState) savedInstanceState.getSerializable("TIMER_STATE");
        if (currentState == null) {
            currentState = TimerState.IDLE;
        }
        accumulatedElapsedMs = savedInstanceState.getLong("ACCUMULATED_MS", 0L);
        accumulatedStudyDurationMs = savedInstanceState.getLong("STUDY_ACCUMULATED_MS", 0L);
        sessionStudyStartRealtime = savedInstanceState.getLong("STUDY_START_REALTIME", 0L);
        isBreakActive = savedInstanceState.getBoolean("IS_BREAK_ACTIVE", false);
        breakStartRealtime = savedInstanceState.getLong("BREAK_START_REALTIME", 0L);
        accumulatedBreakDurationMs = savedInstanceState.getLong("BREAK_ACCUMULATED_MS", 0L);
        lastDiscardedQuestionElapsedMs = savedInstanceState.getLong("LAST_DISCARDED_MS", 0L);
        canUndoReset = savedInstanceState.getBoolean("CAN_UNDO_RESET", false);

        if (currentState.isRunning()) {
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
        if (autoPip && currentState.isRunning() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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

    private void updateWidgets() {
        try {
            long currentElapsed = (startTimeRealtime > 0)
                    ? (SystemClock.elapsedRealtime() - startTimeRealtime)
                    : accumulatedElapsedMs;
            long studyMs = accumulatedStudyDurationMs;
            if (sessionStudyStartRealtime > 0 && !isBreakActive) {
                studyMs += (SystemClock.elapsedRealtime() - sessionStudyStartRealtime);
            }
            int completedCount = 0;
            int correctCount = 0;
            if (currentSession != null) {
                List<QuestionLog> logs = currentSession.getLogs();
                if (logs != null) {
                    completedCount = logs.size();
                    for (QuestionLog log : logs) {
                        if ("CORRECT".equalsIgnoreCase(log.getOutcome())) {
                            correctCount++;
                        }
                    }
                }
            }
            int accuracyPct = completedCount > 0 ? (int) Math.round((correctCount * 100.0) / completedCount) : 0;
            int streakDays = 1;
            WidgetUpdateHelper.updateAllWidgets(
                    this,
                    currentState,
                    currentElapsed,
                    studyMs,
                    isBreakActive,
                    completedCount,
                    accuracyPct,
                    streakDays
            );
        } catch (Throwable ignored) {
        }
    }

    private void checkAutoApplyWallpaper() {
        try {
            DailyWallpaperRefreshReceiver.performDailyRefresh(this);
        } catch (Throwable ignored) {
        }
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

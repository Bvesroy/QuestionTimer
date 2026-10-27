package com.example.timerapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
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
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.content.Intent;
import android.net.Uri;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.timerapp.R;
import com.example.timerapp.data.SessionStorage;
import com.example.timerapp.data.analytics.OutcomeClassifier;
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
 *      - RUNNING -> Two-Zone: upper half = WRONG, lower half = RIGHT, then pause
 *                   (tracking off: tap pauses and records the question)
 *      - PAUSED -> Tap resumes a manually paused question (elapsed preserved);
 *                  after a recorded outcome, tap resets and starts the next question
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

    // Two-Zone Answer Recording, Corner Change Tag & Undo Views
    private View layoutUndoBanner;
    private TextView tvUndoMessage;
    private Button btnChangeTag;
    private Button btnUndoAnswer;
    private TextView tvLiveAnalytics;
    private boolean isTwoZoneRecordingEnabled = true;
    private String currentJeeExamType = SettingsDialog.EXAM_TYPE_JEE_MAIN;
    private final Handler undoBannerHandler = new Handler(Looper.getMainLooper());
    private QuestionLog lastRecordedOutcomeLog = null;
    /**
     * True when the current question was paused by the user (or by a break) without
     * recording an answer. The next resume must continue the same question with its
     * preserved elapsed time instead of advancing to a new one.
     */
    private boolean manualPauseActive = false;

    // Two-Zone Visual Feedback Views & Settings
    private View viewUpperFeedback;
    private View viewLowerFeedback;
    private boolean isZoneEffectEnabled = true;
    private int zoneEffectDurationMs = 300;
    private String zoneEffectStyle = SettingsDialog.EFFECT_STYLE_FADE;
    private float touchDownRawX;
    private float touchDownRawY;
    private long touchDownRealtime;

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
        // Force initialize two-zone outcome tracking and click effects if not previously marked active
        if (!prefs.getBoolean(SettingsDialog.KEY_TWO_ZONE_INITIALIZED, false)) {
            prefs.edit()
                    .putBoolean(SettingsDialog.KEY_TWO_ZONE_RECORDING, true)
                    .putBoolean(SettingsDialog.KEY_ZONE_EFFECT_ENABLED, true)
                    .putInt(SettingsDialog.KEY_ZONE_EFFECT_DURATION_MS, 300)
                    .putString(SettingsDialog.KEY_ZONE_EFFECT_STYLE, SettingsDialog.EFFECT_STYLE_FADE)
                    .putBoolean(SettingsDialog.KEY_TWO_ZONE_INITIALIZED, true)
                    .apply();
        }
        currentTimerSizeMode = prefs.getString(SettingsDialog.KEY_TIMER_SIZE_MODE, SettingsDialog.SIZE_MEDIUM);
        isTwoZoneRecordingEnabled = prefs.getBoolean(SettingsDialog.KEY_TWO_ZONE_RECORDING, true);
        currentJeeExamType = prefs.getString(SettingsDialog.KEY_JEE_EXAM_TYPE, SettingsDialog.EXAM_TYPE_JEE_MAIN);
        isZoneEffectEnabled = prefs.getBoolean(SettingsDialog.KEY_ZONE_EFFECT_ENABLED, true);
        zoneEffectDurationMs = prefs.getInt(SettingsDialog.KEY_ZONE_EFFECT_DURATION_MS, 300);
        zoneEffectStyle = prefs.getString(SettingsDialog.KEY_ZONE_EFFECT_STYLE, SettingsDialog.EFFECT_STYLE_FADE);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        lastInteractionRealtime = SystemClock.elapsedRealtime();

        initViews();
        loadOrCreateSession();
        setupGestureDetector();
        applyImmersiveMode();
        applyTheme();
        applyTimerFont();
        applyTimerSize();
        applyStudyTimerPosition();
        updateExamBanner();
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
        applyTimerFont();
        applyTimerSize();
        applyStudyTimerPosition();
        updateExamBanner();
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
        controlsAreVisible = true;
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

        // Two-Zone Click Feedback Views
        viewUpperFeedback = findViewById(R.id.viewUpperFeedback);
        viewLowerFeedback = findViewById(R.id.viewLowerFeedback);
        // Raise the overlay container once; raising an individual weighted child would
        // reorder it and swap the upper/lower halves.
        View zoneFeedbackContainer = findViewById(R.id.layoutZoneFeedback);
        if (zoneFeedbackContainer != null) {
            zoneFeedbackContainer.bringToFront();
        }
        if (viewUpperFeedback != null) {
            viewUpperFeedback.setVisibility(View.GONE);
        }
        if (viewLowerFeedback != null) {
            viewLowerFeedback.setVisibility(View.GONE);
        }

        // Corner Undo & Change Tag Notification Banner
        layoutUndoBanner = findViewById(R.id.layoutUndoBanner);
        tvUndoMessage = findViewById(R.id.tvUndoMessage);
        btnChangeTag = findViewById(R.id.btnChangeTag);
        btnUndoAnswer = findViewById(R.id.btnUndoAnswer);
        if (btnChangeTag != null) {
            btnChangeTag.setOnClickListener(v -> changeLastAnswerTag());
        }
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
     * Applies user-selected font family to the timer countdown readout.
     */
    private void applyTimerFont() {
        if (tvTimerReadout == null) return;
        SharedPreferences prefs = getSharedPreferences(SettingsDialog.PREFS_NAME, Context.MODE_PRIVATE);
        String fontChoice = prefs.getString(SettingsDialog.KEY_TIMER_FONT, SettingsDialog.FONT_MONOSPACE);
        Typeface tf;
        switch (fontChoice) {
            case SettingsDialog.FONT_SANS_SERIF_BOLD:
                tf = Typeface.create("sans-serif", Typeface.BOLD);
                break;
            case SettingsDialog.FONT_CONDENSED:
                tf = Typeface.create("sans-serif-condensed", Typeface.BOLD);
                break;
            case SettingsDialog.FONT_SERIF:
                tf = Typeface.create("serif", Typeface.BOLD);
                break;
            case SettingsDialog.FONT_LIGHT:
                tf = Typeface.create("sans-serif-light", Typeface.NORMAL);
                break;
            case SettingsDialog.FONT_MONOSPACE:
            default:
                tf = Typeface.MONOSPACE;
                break;
        }
        tvTimerReadout.setTypeface(tf);
    }

    /**
     * Dynamically positions the Total Study Clock in the user-selected screen corner
     * (Bottom-Left, Bottom-Right, Top-Left, Top-Right, Center, or Hidden).
     */
    private void applyStudyTimerPosition() {
        if (tvTotalStudyClock == null) return;
        SharedPreferences prefs = getSharedPreferences(SettingsDialog.PREFS_NAME, Context.MODE_PRIVATE);
        String position = prefs.getString(SettingsDialog.KEY_STUDY_TIMER_POSITION, SettingsDialog.POS_BOTTOM_LEFT);

        if (SettingsDialog.POS_HIDDEN.equals(position)) {
            tvTotalStudyClock.setVisibility(View.GONE);
            return;
        } else {
            tvTotalStudyClock.setVisibility(View.VISIBLE);
        }

        boolean isLandscape = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );

        float density = getResources().getDisplayMetrics().density;
        int m16 = (int) (16 * density);
        int m20 = (int) (20 * density);
        int m48 = (int) (48 * density);
        int m96 = (int) (96 * density);

        switch (position) {
            case SettingsDialog.POS_BOTTOM_RIGHT:
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.setMargins(0, 0, m20, isLandscape ? m16 : m96);
                break;
            case SettingsDialog.POS_TOP_LEFT:
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                params.addRule(RelativeLayout.ALIGN_PARENT_START);
                params.setMargins(m20, isLandscape ? m16 : m48, 0, 0);
                break;
            case SettingsDialog.POS_TOP_RIGHT:
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.setMargins(0, isLandscape ? m16 : m48, m20, 0);
                break;
            case SettingsDialog.POS_BOTTOM_CENTER:
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                params.addRule(RelativeLayout.CENTER_HORIZONTAL);
                params.setMargins(0, 0, 0, isLandscape ? m16 : m96);
                break;
            case SettingsDialog.POS_CENTER:
                params.addRule(RelativeLayout.BELOW, R.id.centerContainer);
                params.addRule(RelativeLayout.CENTER_HORIZONTAL);
                params.setMargins(0, (int) (8 * density), 0, 0);
                break;
            case SettingsDialog.POS_BOTTOM_LEFT:
            default:
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                params.addRule(RelativeLayout.ALIGN_PARENT_START);
                params.setMargins(m20, 0, 0, isLandscape ? m16 : m96);
                break;
        }

        tvTotalStudyClock.setLayoutParams(params);
    }

    /**
     * Executes configurable visual effects and transition animations when a question ends or pauses.
     */
    private void triggerEndTimerEffects() {
        SharedPreferences prefs = getSharedPreferences(SettingsDialog.PREFS_NAME, Context.MODE_PRIVATE);
        String effect = prefs.getString(SettingsDialog.KEY_END_TIMER_EFFECT, SettingsDialog.EFFECT_GLOW_PULSE);
        String transition = prefs.getString(SettingsDialog.KEY_END_TIMER_TRANSITION, SettingsDialog.TRANSITION_SMOOTH_FADE);
        int durationMs = prefs.getInt(SettingsDialog.KEY_END_TIMER_DURATION_MS, SettingsDialog.DEFAULT_END_TIMER_DURATION_MS);
        long dur = Math.max(100L, durationMs);

        // Haptic feedback on pause / end timer
        if (vibrator != null && prefs.getBoolean(SettingsDialog.KEY_HAPTIC_ENABLED, true)) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(45);
                }
            } catch (Exception ignored) {}
        }

        // Screen Flash visual effect if selected
        if (SettingsDialog.EFFECT_FLASH.equals(effect) && rootContainer != null) {
            int accentColor = ThemeManager.getAccentColor(this);
            int flashColor = Color.argb(125, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor));
            rootContainer.setBackgroundColor(flashColor);
            rootContainer.postDelayed(() -> {
                if (rootContainer != null) {
                    rootContainer.setBackgroundColor(ThemeManager.getBackgroundColor(this));
                }
            }, Math.min(250L, dur));
        }

        // AMOLED Glow Pulse shadow layer if selected
        if (SettingsDialog.EFFECT_GLOW_PULSE.equals(effect) && tvTimerReadout != null) {
            int accentColor = ThemeManager.getAccentColor(this);
            tvTimerReadout.setShadowLayer(35f, 0, 0, accentColor);
            tvTimerReadout.postDelayed(() -> {
                if (tvTimerReadout != null) {
                    tvTimerReadout.setShadowLayer(0, 0, 0, 0);
                }
            }, dur * 2);
        }

        // Smooth visual cue for tvStateBadge
        if (tvStateBadge != null && tvStateBadge.getVisibility() == View.VISIBLE) {
            tvStateBadge.animate().cancel();
            tvStateBadge.setAlpha(0.25f);
            tvStateBadge.setScaleX(0.92f);
            tvStateBadge.setScaleY(0.92f);
            tvStateBadge.animate()
                    .alpha(1.0f)
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(dur)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        if (tvTimerReadout != null) {
            tvTimerReadout.animate().cancel();

            Runnable onTransitionFinished = () -> {
                // Secondary visual pulse/bounce executed AFTER transition to avoid cancellation
                if (SettingsDialog.EFFECT_GLOW_PULSE.equals(effect) && tvTimerReadout != null) {
                    tvTimerReadout.animate()
                            .scaleX(1.08f)
                            .scaleY(1.08f)
                            .setDuration(dur / 2)
                            .withEndAction(() -> {
                                if (tvTimerReadout != null) {
                                    tvTimerReadout.animate()
                                            .scaleX(1.0f)
                                            .scaleY(1.0f)
                                            .setDuration(dur / 2)
                                            .start();
                                }
                            })
                            .start();
                } else if (SettingsDialog.EFFECT_SUBTLE_BOUNCE.equals(effect) && tvTimerReadout != null) {
                    tvTimerReadout.animate()
                            .scaleX(1.04f)
                            .scaleY(1.04f)
                            .setDuration(dur / 2)
                            .withEndAction(() -> {
                                if (tvTimerReadout != null) {
                                    tvTimerReadout.animate().scaleX(1.0f).scaleY(1.0f).setDuration(dur / 2).start();
                                }
                            })
                            .start();
                }
            };

            switch (transition) {
                case SettingsDialog.TRANSITION_SCALE_POP:
                    tvTimerReadout.setScaleX(0.85f);
                    tvTimerReadout.setScaleY(0.85f);
                    tvTimerReadout.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setInterpolator(new OvershootInterpolator(2.2f))
                            .setDuration(dur)
                            .withEndAction(onTransitionFinished)
                            .start();
                    break;
                case SettingsDialog.TRANSITION_SLIDE_DOWN:
                    tvTimerReadout.setTranslationY(-32f);
                    tvTimerReadout.setAlpha(0.3f);
                    tvTimerReadout.animate()
                            .translationY(0f)
                            .alpha(1.0f)
                            .setInterpolator(new DecelerateInterpolator())
                            .setDuration(dur)
                            .withEndAction(onTransitionFinished)
                            .start();
                    break;
                case SettingsDialog.TRANSITION_INSTANT:
                    tvTimerReadout.setAlpha(1.0f);
                    tvTimerReadout.setScaleX(1.0f);
                    tvTimerReadout.setScaleY(1.0f);
                    tvTimerReadout.setTranslationY(0f);
                    onTransitionFinished.run();
                    break;
                case SettingsDialog.TRANSITION_SMOOTH_FADE:
                default:
                    tvTimerReadout.setAlpha(0.35f);
                    tvTimerReadout.animate()
                            .alpha(1.0f)
                            .setDuration(dur)
                            .withEndAction(onTransitionFinished)
                            .start();
                    break;
            }
        }
    }

    /**
     * Formats the pause cue message according to the user-selected End Timer Text style.
     */
    private String formatEndTimerText(boolean isWrong, String outcomeLabel, String scoreDelta, int nextQ) {
        SharedPreferences prefs = getSharedPreferences(SettingsDialog.PREFS_NAME, Context.MODE_PRIVATE);
        String textStyle = prefs.getString(SettingsDialog.KEY_END_TIMER_TEXT_STYLE, SettingsDialog.TEXT_STYLE_STANDARD);

        switch (textStyle) {
            case SettingsDialog.TEXT_STYLE_JEE_FOCUS:
                if (isTwoZoneRecordingEnabled) {
                    return String.format(Locale.ROOT, "%s (%s PTS) • NEXT Q%d", outcomeLabel, scoreDelta, nextQ);
                }
                return String.format(Locale.ROOT, "QUESTION %d DONE • NEXT Q%d", nextQ - 1, nextQ);

            case SettingsDialog.TEXT_STYLE_MINIMAL:
                if (isTwoZoneRecordingEnabled) {
                    return String.format(Locale.ROOT, "%s • %s PTS", outcomeLabel, scoreDelta);
                }
                return "PAUSED";

            case SettingsDialog.TEXT_STYLE_ENCOURAGING:
                if (isTwoZoneRecordingEnabled) {
                    return isWrong
                            ? String.format(Locale.ROOT, "REVIEW Q%d • KEEP PUSHING!", nextQ - 1)
                            : String.format(Locale.ROOT, "EXCELLENT! +4 PTS • SOLVE Q%d", nextQ);
                }
                return "GREAT PACE • TAP FOR NEXT";

            case SettingsDialog.TEXT_STYLE_STANDARD:
            default:
                if (isTwoZoneRecordingEnabled) {
                    return String.format(Locale.ROOT, "%s (%s PTS) • TAP FOR Q%d", outcomeLabel, scoreDelta, nextQ);
                }
                return getString(R.string.badge_paused);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touchDownRawX = ev.getRawX();
                touchDownRawY = ev.getRawY();
                touchDownRealtime = SystemClock.elapsedRealtime();

                // During active question timing or when resolving a question,
                // capture touch authority-first to guarantee headers, title, or target text
                // cannot steal or intercept the two-zone outcome tap.
                if (currentState.isRunning() || (currentState == TimerState.PAUSED && manualPauseActive)) {
                    return true;
                }

                // If user touched an interactive utility control while paused/idle, let normal dispatch occur
                if (isInteractiveControlTouch(ev)) {
                    return super.dispatchTouchEvent(ev);
                }

                return true;

            case MotionEvent.ACTION_MOVE:
                if (currentState.isRunning() || (currentState == TimerState.PAUSED && manualPauseActive)) {
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
                float dx = Math.abs(ev.getRawX() - touchDownRawX);
                float dy = Math.abs(ev.getRawY() - touchDownRawY);
                long dt = SystemClock.elapsedRealtime() - touchDownRealtime;

                // Generous movement threshold scaled with display density
                // Accommodates thumb rolling & capacitive deflection on physical high-DPI screens
                float density = getResources().getDisplayMetrics().density;
                float maxMovement = Math.max(160f, 48f * density);

                // 1. Authoritative Question Outcome Detection during active question
                // When timer is running or question is active, upper half = WRONG, lower half = RIGHT.
                // It MUST authoritatively and unambiguously register as WRONG (+ red visual flash + appropriate score penalty / mark),
                // regardless of whether they tap on or near headers, target text, or session title, and in BOTH portrait and landscape.
                boolean isResolvingQuestion = currentState.isRunning() ||
                        (currentState == TimerState.PAUSED && manualPauseActive);

                if (isResolvingQuestion) {
                    if (dx > maxMovement || dy > maxMovement || dt > 1500L) {
                        return true;
                    }

                    float midY;
                    if (rootContainer != null && rootContainer.getHeight() > 0) {
                        int[] loc = new int[2];
                        rootContainer.getLocationOnScreen(loc);
                        midY = loc[1] + (rootContainer.getHeight() / 2.0f);
                    } else {
                        midY = getResources().getDisplayMetrics().heightPixels / 2.0f;
                    }

                    boolean isUpper = ev.getRawY() < midY;
                    triggerZoneClickEffect(isUpper);
                    recordTwoZoneOutcome(isUpper ? "WRONG" : "RIGHT");
                    return true;
                }

                // 2. When timer is paused, idle, or ended:
                if (dx > maxMovement || dy > maxMovement || dt > 1500L) {
                    return super.dispatchTouchEvent(ev);
                }

                // If touch hit a registered interactive control button while paused/idle, let normal click dispatch occur
                if (isInteractiveControlTouch(ev)) {
                    return super.dispatchTouchEvent(ev);
                }

                // Standard tap anywhere loop (IDLE -> start, PAUSED -> advance to next question)
                onScreenTapped();
                return true;
        }

        return super.dispatchTouchEvent(ev);
    }

    private boolean isInteractiveControlTouch(MotionEvent ev) {
        // While timer is actively running, or when resolving a question,
        // EVERY tap in the upper half = WRONG, and lower half = RIGHT!
        // No header, title, target, or background button must ever steal the gesture!
        if (currentState.isRunning() || (currentState == TimerState.PAUSED && manualPauseActive)) {
            return false;
        }

        // When paused or idle, allow interactive buttons, undo banner, title, target, exam banner, and study clock
        return isTouchInsideView(btnSettings, ev) ||
               isTouchInsideView(btnHistory, ev) ||
               isTouchInsideView(btnDiscard, ev) ||
               isTouchInsideView(btnToggle, ev) ||
               isTouchInsideView(layoutUndoBanner, ev) ||
               isTouchInsideView(findViewById(R.id.layoutSessionTitle), ev) ||
               isTouchInsideView(tvTarget, ev) ||
               isTouchInsideView(tvExamBanner, ev) ||
               isTouchInsideView(tvTotalStudyClock, ev);
    }

    private void triggerZoneClickEffect(boolean isUpper) {
        if (!isZoneEffectEnabled) return;
        final View targetView = isUpper ? viewUpperFeedback : viewLowerFeedback;
        final View otherView = isUpper ? viewLowerFeedback : viewUpperFeedback;
        if (targetView == null) return;

        if (otherView != null) {
            otherView.animate().cancel();
            otherView.setVisibility(View.GONE);
        }

        targetView.animate().cancel();
        // Upper zone = Vibrant Red (#90FF453A), Lower zone = Vibrant Green (#9030D158)
        // Rich gradient from 160 alpha to 110 alpha across the entire half screen for striking feedback
        int startColor = isUpper ? Color.argb(160, 255, 69, 58) : Color.argb(160, 48, 209, 88);
        int endColor = isUpper ? Color.argb(110, 255, 69, 58) : Color.argb(110, 48, 209, 88);
        GradientDrawable zoneGradient = new GradientDrawable(
                isUpper ? GradientDrawable.Orientation.TOP_BOTTOM : GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{startColor, endColor});
        zoneGradient.setShape(GradientDrawable.RECTANGLE);
        targetView.setBackground(zoneGradient);
        targetView.setAlpha(1.0f);
        targetView.setScaleX(1.0f);
        targetView.setScaleY(1.0f);
        // Do NOT bring the weighted child itself to front: LinearLayout reorders
        // children, which swaps the upper/lower halves. The parent overlay container
        // is raised once in initViews() instead.
        targetView.setVisibility(View.VISIBLE);

        long duration = Math.max(120L, zoneEffectDurationMs);

        if (SettingsDialog.EFFECT_STYLE_SCALE.equalsIgnoreCase(zoneEffectStyle)) {
            targetView.setScaleX(0.92f);
            targetView.setScaleY(0.92f);
            targetView.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .alpha(0.0f)
                    .setDuration(duration)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(() -> {
                        targetView.setVisibility(View.GONE);
                        targetView.setAlpha(1.0f);
                        targetView.setScaleX(1.0f);
                        targetView.setScaleY(1.0f);
                    })
                    .start();
        } else if (SettingsDialog.EFFECT_STYLE_POP.equalsIgnoreCase(zoneEffectStyle)) {
            long holdTime = (long) (duration * 0.5f);
            long fadeTime = duration - holdTime;
            targetView.animate()
                    .alpha(0.0f)
                    .setStartDelay(holdTime)
                    .setDuration(fadeTime)
                    .withEndAction(() -> {
                        targetView.setVisibility(View.GONE);
                        targetView.setAlpha(1.0f);
                    })
                    .start();
        } else {
            // Default: FADE_OUT
            targetView.animate()
                    .alpha(0.0f)
                    .setDuration(duration)
                    .setInterpolator(new AccelerateInterpolator())
                    .withEndAction(() -> {
                        targetView.setVisibility(View.GONE);
                        targetView.setAlpha(1.0f);
                    })
                    .start();
        }
    }

    public void previewZoneClickEffect(boolean isUpper, int durationMs, String style) {
        boolean prevEnabled = isZoneEffectEnabled;
        int prevDur = zoneEffectDurationMs;
        String prevStyle = zoneEffectStyle;

        isZoneEffectEnabled = true;
        zoneEffectDurationMs = durationMs;
        zoneEffectStyle = style;

        triggerZoneClickEffect(isUpper);

        zoneEffectDurationMs = prevDur;
        zoneEffectStyle = prevStyle;
        isZoneEffectEnabled = prevEnabled;
    }

    /**
     * Taps are intercepted authority-first via Activity.dispatchTouchEvent()
     * preventing child views or padding from misclassifying upper/lower zone boundaries.
     */
    private void setupGestureDetector() {
        if (rootContainer != null) {
            rootContainer.setOnTouchListener(null);
        }
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
     * - RUNNING -> Tap pauses timer and records current question duration
     * - PAUSED -> Tap resumes a manual pause, or starts the next question after an outcome
     */
    private void onScreenTapped() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastTapRealtime < 180) {
            return;
        }
        lastInteractionRealtime = now;
        hasShownIdleToast = false;
        triggerTapFeedback();

        if (isBreakActive) {
            showBreakActiveToast();
            return;
        }

        if (currentState == TimerState.IDLE) {
            startTimer();
        } else if (currentState.isRunning()) {
            pauseAndRecordQuestion();
        } else if (currentState.isPaused()) {
            if (manualPauseActive) {
                resumeCurrentQuestion();
            } else {
                advanceAndStartNextQuestion();
            }
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
        // Defense in depth: no outcome may be written while optional tracking is disabled.
        if (!isTwoZoneRecordingEnabled) {
            return;
        }
        if (!transitionTo(TimerState.RECORDING_OUTCOME)) {
            return;
        }

        long authoritativeDuration = (startTimeRealtime > 0)
                ? (SystemClock.elapsedRealtime() - startTimeRealtime)
                : accumulatedElapsedMs;

        triggerTwoZoneHaptic(outcome);

        int currentQNum = (currentSession != null) ? currentSession.getCurrentQuestion() : 1;
        long sessId = (currentSession != null) ? currentSession.getId() : 1L;
        boolean isWrong = OutcomeClassifier.isWrong(outcome);
        QuestionLog log = new QuestionLog(
                System.currentTimeMillis(),
                sessId,
                currentQNum,
                authoritativeDuration,
                System.currentTimeMillis(),
                "", "", "MEDIUM", isWrong ? "WRONG" : "RIGHT", 3,
                isWrong ? "MISTAKE" : "", ""
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
        triggerEndTimerEffects();

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
        boolean isIncorrect = OutcomeClassifier.isWrong(log.getOutcome());
        double penalty = "JEE_ADVANCED".equalsIgnoreCase(currentJeeExamType) ? 2.0 : 1.0;
        String markStr = isIncorrect ? ("-" + (int) penalty + " PTS") : "+4 PTS";
        String outcomeStr = isIncorrect ? "WRONG" : "RIGHT";
        tvUndoMessage.setText(String.format(Locale.ROOT, "Q#%d %s (%s, %s)",
                log.getQuestionNumber(), outcomeStr, markStr, log.getFormattedDuration()));

        if (btnChangeTag != null) {
            if (isIncorrect) {
                btnChangeTag.setText("CHANGE TO RIGHT");
                btnChangeTag.setTextColor(getColor(R.color.accent_running));
                btnChangeTag.setBackgroundResource(R.drawable.bg_pill_accent);
            } else {
                btnChangeTag.setText("CHANGE TO WRONG");
                btnChangeTag.setTextColor(getColor(R.color.accent_paused));
                btnChangeTag.setBackgroundResource(R.drawable.bg_pill_orange);
            }
            btnChangeTag.setVisibility(View.VISIBLE);
        }

        layoutUndoBanner.setVisibility(View.VISIBLE);
        layoutUndoBanner.setAlpha(1.0f);
        // Stays visible in the corner while paused so user can change tags or undo without rushing
    }

    private void changeLastAnswerTag() {
        triggerTapFeedback();
        if (currentSession == null || currentSession.getLogs().isEmpty()) return;

        QuestionLog lastLog = currentSession.getLastLog();
        if (lastLog == null) return;

        boolean wasWrong = OutcomeClassifier.isWrong(lastLog.getOutcome());
        String newOutcome = wasWrong ? "CORRECT" : "INCORRECT";
        QuestionLog updatedLog = currentSession.updateLastLogOutcome(newOutcome);
        if (updatedLog == null) return;

        if (updatedLog.getId() > 0) {
            com.example.timerapp.data.local.sqlite.TimerDatabaseManager.getInstance(this).updateQuestionOutcome(updatedLog.getId(), newOutcome);
        }
        persistSessions();

        triggerTwoZoneHaptic(newOutcome);
        if (wasWrong) {
            triggerZoneClickEffect(false);
            Toast.makeText(this, "Question #" + updatedLog.getQuestionNumber() + " changed to RIGHT (+4 PTS)", Toast.LENGTH_SHORT).show();
        } else {
            triggerZoneClickEffect(true);
            double penalty = "JEE_ADVANCED".equalsIgnoreCase(currentJeeExamType) ? 2.0 : 1.0;
            Toast.makeText(this, "Question #" + updatedLog.getQuestionNumber() + " changed to WRONG (-" + (int) penalty + " PTS)", Toast.LENGTH_SHORT).show();
        }

        showUndoAnswerBanner(updatedLog);
        renderUi();
        updateWidgets();
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
            // After undo the next tap starts the restored question fresh; never show the
            // previous question's outcome/delta as if it belonged to the undone one.
            lastRecordedOutcomeLog = null;
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
            boolean isWrong = OutcomeClassifier.isWrong(outcome);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (isWrong) {
                    // Distinct, strong double buzz for wrong on physical devices
                    long[] timings = new long[]{0, 60, 45, 75};
                    int[] amplitudes = new int[]{0, 255, 0, 255};
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1));
                } else {
                    // Crisp, decisive positive confirmation click
                    vibrator.vibrate(VibrationEffect.createOneShot(55, VibrationEffect.DEFAULT_AMPLITUDE));
                }
            } else {
                vibrator.vibrate(isWrong ? 100 : 50);
            }
        } catch (Exception ignored) {}
    }

    private void startTimer() {
        manualPauseActive = false;
        lastRecordedOutcomeLog = null;
        if (layoutUndoBanner != null) {
            layoutUndoBanner.setVisibility(View.GONE);
        }
        if (viewUpperFeedback != null) {
            viewUpperFeedback.animate().cancel();
            viewUpperFeedback.setVisibility(View.GONE);
        }
        if (viewLowerFeedback != null) {
            viewLowerFeedback.animate().cancel();
            viewLowerFeedback.setVisibility(View.GONE);
        }
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
        if (isTwoZoneRecordingEnabled) {
            // Two-Zone mode: pausing is not an answer. Only tapping the upper (WRONG)
            // or lower (RIGHT) half may create a log or advance the question counter.
            pauseTimerForManualResume();
            return;
        }

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
        triggerEndTimerEffects();

        // Check if target questions goal is reached
        int target = currentSession.getTarget();
        if (target > 0 && currentSession.getCompletedCount() >= target) {
            Toast.makeText(this, "🎯 TARGET REACHED! (" + target + " questions completed)", Toast.LENGTH_LONG).show();
            checkAutoApplyWallpaper();
            promptSessionNotesIfNeeded(currentSession);
        }
    }

    /**
     * Pauses the current question without recording an answer or advancing the question
     * counter. Elapsed time is preserved so {@link #resumeCurrentQuestion()} continues the
     * same question exactly where it stopped.
     */
    private void pauseTimerForManualResume() {
        long elapsedMs = accumulatedElapsedMs;
        if (currentState.isRunning() && startTimeRealtime > 0) {
            elapsedMs = SystemClock.elapsedRealtime() - startTimeRealtime;
        }

        transitionTo(TimerState.PAUSED);
        continuousRunningStartTime = 0L;
        timerHandler.removeCallbacks(timerTicker);
        accumulatedElapsedMs = Math.max(0L, elapsedMs);
        manualPauseActive = true;
        // A manual pause is not an answer: drop any outcome badge left by a prior question.
        lastRecordedOutcomeLog = null;

        updateWidgets();
        renderUi();
        triggerEndTimerEffects();
    }

    /**
     * Resumes the manually paused question with its preserved elapsed time instead of
     * resetting the clock and moving to a fresh question.
     */
    private void resumeCurrentQuestion() {
        if (layoutUndoBanner != null) {
            layoutUndoBanner.setVisibility(View.GONE);
        }
        if (viewUpperFeedback != null) {
            viewUpperFeedback.animate().cancel();
            viewUpperFeedback.setVisibility(View.GONE);
        }
        if (viewLowerFeedback != null) {
            viewLowerFeedback.animate().cancel();
            viewLowerFeedback.setVisibility(View.GONE);
        }

        manualPauseActive = false;
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

    private void advanceAndStartNextQuestion() {
        manualPauseActive = false;
        lastRecordedOutcomeLog = null;
        if (layoutUndoBanner != null) {
            layoutUndoBanner.setVisibility(View.GONE);
        }
        if (viewUpperFeedback != null) {
            viewUpperFeedback.animate().cancel();
            viewUpperFeedback.setVisibility(View.GONE);
        }
        if (viewLowerFeedback != null) {
            viewLowerFeedback.animate().cancel();
            viewLowerFeedback.setVisibility(View.GONE);
        }
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
        if (isBreakActive) {
            showBreakActiveToast();
            return;
        }
        if (currentState.isRunning()) {
            pauseAndRecordQuestion();
        } else if (currentState == TimerState.PAUSED) {
            if (manualPauseActive) {
                resumeCurrentQuestion();
            } else {
                advanceAndStartNextQuestion();
            }
        } else {
            startTimer();
        }
    }

    private void showBreakActiveToast() {
        Toast.makeText(this, "Break in progress — end it from the study clock.", Toast.LENGTH_SHORT).show();
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
            // Preserve the running question's elapsed time; a break is not an answer.
            pauseTimerForManualResume();
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

        SharedPreferences prefs = getSharedPreferences(SettingsDialog.PREFS_NAME, Context.MODE_PRIVATE);
        boolean cleanCountingMode = prefs.getBoolean(SettingsDialog.KEY_CLEAN_COUNTING_MODE, true);

        if (visible) {
            bottomControlsContainer.setVisibility(View.VISIBLE);
            bottomControlsContainer.animate()
                    .alpha(1.0f)
                    .setDuration(duration)
                    .start();

            if (topBarContainer != null && cleanCountingMode) {
                topBarContainer.setVisibility(View.VISIBLE);
                topBarContainer.animate()
                        .alpha(1.0f)
                        .setDuration(duration)
                        .start();
            }
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

            if (topBarContainer != null && cleanCountingMode) {
                topBarContainer.animate()
                        .alpha(0.0f)
                        .setDuration(duration)
                        .withEndAction(() -> {
                            if (!controlsAreVisible && topBarContainer != null) {
                                topBarContainer.setVisibility(View.INVISIBLE);
                            }
                        })
                        .start();
            }
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
                    tvStateBadge.setText("↑ WRONG   ·   ↓ RIGHT");
                } else {
                    tvStateBadge.setText(R.string.badge_running);
                }
                tvStateBadge.setBackgroundResource(R.drawable.bg_pill_accent);
                tvStateBadge.setTextColor(accentColor);
                btnToggle.setImageResource(R.drawable.ic_pause);
                btnToggle.setColorFilter(accentColor);
                break;
            case PAUSED:
                if (manualPauseActive) {
                    // Manual pause: never surface a previous question's outcome or score delta.
                    tvStateBadge.setText(isBreakActive
                            ? "ON BREAK • END FROM STUDY CLOCK"
                            : "PAUSED • TAP TO RESUME");
                    tvStateBadge.setBackgroundResource(R.drawable.bg_pill_orange);
                    tvStateBadge.setTextColor(Color.parseColor("#FF9F0A"));
                } else if (isTwoZoneRecordingEnabled && lastRecordedOutcomeLog != null
                        && OutcomeClassifier.isAttempted(lastRecordedOutcomeLog.getOutcome())) {
                    boolean isWrong = OutcomeClassifier.isWrong(lastRecordedOutcomeLog.getOutcome());
                    double penalty = "JEE_ADVANCED".equalsIgnoreCase(currentJeeExamType) ? 2.0 : 1.0;
                    String scoreDelta = isWrong ? ("-" + (int) penalty) : "+4";
                    String outcomeLabel = isWrong ? "WRONG" : "RIGHT";
                    int nextQ = (currentSession != null) ? currentSession.getCurrentQuestion() : 2;
                    tvStateBadge.setText(formatEndTimerText(isWrong, outcomeLabel, scoreDelta, nextQ));
                    tvStateBadge.setBackgroundResource(isWrong ? R.drawable.bg_pill_orange : R.drawable.bg_pill_accent);
                    tvStateBadge.setTextColor(isWrong ? Color.parseColor("#FF9F0A") : accentColor);
                } else {
                    int nextQ = (currentSession != null) ? (currentSession.getCurrentQuestion() + 1) : 2;
                    tvStateBadge.setText(formatEndTimerText(false, "PAUSED", "0", nextQ));
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

        // Status Hints Visibility
        SharedPreferences prefs = getSharedPreferences(SettingsDialog.PREFS_NAME, Context.MODE_PRIVATE);
        boolean showHints = prefs.getBoolean(SettingsDialog.KEY_SHOW_STATUS_HINTS, true);
        if (tvStateBadge != null) {
            tvStateBadge.setVisibility(showHints ? View.VISIBLE : View.GONE);
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
                renderUi();
                updateWidgets();
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

            @Override
            public void onDisplaySettingsChanged() {
                SharedPreferences prefs = getSharedPreferences(SettingsDialog.PREFS_NAME, Context.MODE_PRIVATE);
                isZoneEffectEnabled = prefs.getBoolean(SettingsDialog.KEY_ZONE_EFFECT_ENABLED, true);
                zoneEffectDurationMs = prefs.getInt(SettingsDialog.KEY_ZONE_EFFECT_DURATION_MS, 300);
                zoneEffectStyle = prefs.getString(SettingsDialog.KEY_ZONE_EFFECT_STYLE, SettingsDialog.EFFECT_STYLE_FADE);

                applyTimerFont();
                applyTimerSize();
                applyStudyTimerPosition();
                updateExamBanner();
                renderUi();
            }

            @Override
            public void onPreviewZoneClickEffect(boolean isUpper, int durationMs, String style) {
                previewZoneClickEffect(isUpper, durationMs, style);
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

        setContentView(R.layout.activity_main);
        initViews();
        setupGestureDetector();
        applyTheme();
        applyTimerFont();
        applyTimerSize();
        applyStudyTimerPosition();
        updateExamBanner();
        renderUi();
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
        outState.putBoolean("MANUAL_PAUSE_ACTIVE", manualPauseActive);
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
        manualPauseActive = savedInstanceState.getBoolean("MANUAL_PAUSE_ACTIVE", false);

        if (currentState.isRunning()) {
            // A running timer cannot be in a manually-paused state; drop any stale flag.
            manualPauseActive = false;
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
            int attemptedCount = 0;
            if (currentSession != null) {
                List<QuestionLog> logs = currentSession.getLogs();
                if (logs != null) {
                    completedCount = logs.size();
                    for (QuestionLog log : logs) {
                        String outcome = log.getOutcome();
                        if (OutcomeClassifier.isAttempted(outcome)) {
                            attemptedCount++;
                            if (OutcomeClassifier.isCorrect(outcome)) {
                                correctCount++;
                            }
                        }
                    }
                }
            }
            int accuracyPct = attemptedCount > 0 ? (int) Math.round((correctCount * 100.0) / attemptedCount) : 0;
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

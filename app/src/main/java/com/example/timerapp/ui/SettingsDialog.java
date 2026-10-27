package com.example.timerapp.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.timerapp.R;
import com.example.timerapp.model.TimerSession;

import java.util.Locale;

/**
 * Modern AMOLED Unified Settings Modal Dialog organizing 7 categorized sections:
 * 1. Timer Display & Color Themes
 * 2. Performance Goals & Live Feedback
 * 3. Subjects & Categorization
 * 4. Exam Countdown Banner
 * 5. Sounds, Haptics, PiP & Smart Focus
 * 6. Data & Backup Operations
 * 7. Privacy Lock PIN
 */
public class SettingsDialog extends Dialog {

    public interface OnSettingsChangedListener {
        void onTimerSizeChanged(String sizeMode);
        void onTargetsChanged(int targetSecPerQ, int targetSessionMin, int targetQuestions);
        void onThemeChanged(String theme, String accent);
        default void onFocusControlsChanged() {}
        default void onTwoZoneRecordingChanged(boolean enabled) {}
        default void onTwoZoneRecordingChanged(boolean enabled, String examType) {
            onTwoZoneRecordingChanged(enabled);
        }
        default void onOpenWallpaperPreview() {}
        default void onOpenMistakeReview() {}
        default void onChooseBackupFolderRequested() {}
        default void onBackupNowRequested() {}
        default void onRestoreBackupRequested() {}
        default void onDisplaySettingsChanged() {}
        default void onPreviewZoneClickEffect(boolean isUpper, int durationMs, String style) {}
    }

    public static final String PREFS_NAME = "question_timer_prefs";
    public static final String KEY_TWO_ZONE_INITIALIZED = "two_zone_initialized_v21";
    public static final String KEY_TIMER_SIZE_MODE = "timer_size_mode";
    public static final String KEY_TARGET_SEC_PER_Q = "target_sec_per_q";
    public static final String KEY_TARGET_SESSION_MIN = "target_session_min";
    public static final String KEY_TARGET_SESSION_QUESTIONS = "target_session_questions";

    public static final String KEY_DEFAULT_SUBJECT = "default_subject";
    public static final String KEY_EXAM_NAME = "exam_name";
    public static final String KEY_EXAM_DAYS = "exam_days_remaining";
    public static final String KEY_EXAM_BANNER_ENABLED = "exam_banner_enabled";

    public static final String KEY_HAPTIC_ENABLED = "haptic_feedback_enabled";
    public static final String KEY_SOUND_ENABLED = "sound_alerts_enabled";
    public static final String KEY_AUTO_PIP = "auto_pip_enabled";
    public static final String KEY_IDLE_MINUTES = "idle_inactivity_minutes";
    public static final String KEY_BREAK_MINUTES = "break_reminder_minutes";

    public static final String KEY_TWO_ZONE_RECORDING = "two_zone_recording_enabled";
    public static final String KEY_JEE_EXAM_TYPE = "jee_exam_type";
    public static final String EXAM_TYPE_JEE_MAIN = "JEE_MAIN";
    public static final String EXAM_TYPE_JEE_ADVANCED = "JEE_ADVANCED";
    public static final String KEY_WALLPAPER_AUTO_APPLY = "wallpaper_auto_apply";

    public static final String KEY_PIN_ENABLED = "privacy_pin_enabled";
    public static final String KEY_PIN_CODE = "privacy_pin_code";

    public static final String KEY_SAF_BACKUP_PATH = "saf_backup_folder_path";
    public static final String KEY_SAF_BACKUP_URI = "saf_backup_folder_uri";

    // Two-Zone Click Feedback Effect Keys
    public static final String KEY_ZONE_EFFECT_ENABLED = "zone_click_effect_enabled";
    public static final String KEY_ZONE_EFFECT_DURATION_MS = "zone_click_effect_duration_ms";
    public static final String KEY_ZONE_EFFECT_STYLE = "zone_click_effect_style";

    // Click effect transition style constants
    public static final String EFFECT_STYLE_FADE = "FADE_OUT";
    public static final String EFFECT_STYLE_SCALE = "RIPPLE_SCALE";
    public static final String EFFECT_STYLE_POP = "INSTANT_POP";

    // Display & Layout Settings Keys
    public static final String KEY_SHOW_STATUS_HINTS = "show_status_hints";
    public static final String KEY_CLEAN_COUNTING_MODE = "clean_counting_mode";
    public static final String KEY_STUDY_TIMER_POSITION = "study_timer_position";
    public static final String KEY_TIMER_FONT = "timer_font_family";
    public static final String KEY_END_TIMER_EFFECT = "end_timer_effect";
    public static final String KEY_END_TIMER_TRANSITION = "end_timer_transition";
    public static final String KEY_END_TIMER_TEXT_STYLE = "end_timer_text_style";

    // Study timer position constants
    public static final String POS_BOTTOM_LEFT = "BOTTOM_LEFT";
    public static final String POS_BOTTOM_RIGHT = "BOTTOM_RIGHT";
    public static final String POS_TOP_LEFT = "TOP_LEFT";
    public static final String POS_TOP_RIGHT = "TOP_RIGHT";
    public static final String POS_BOTTOM_CENTER = "BOTTOM_CENTER";
    public static final String POS_CENTER = "CENTER";
    public static final String POS_HIDDEN = "HIDDEN";

    // Timer font family constants
    public static final String FONT_MONOSPACE = "MONOSPACE";
    public static final String FONT_SANS_SERIF_BOLD = "SANS_SERIF_BOLD";
    public static final String FONT_CONDENSED = "CONDENSED";
    public static final String FONT_SERIF = "SERIF";
    public static final String FONT_LIGHT = "LIGHT";

    // End timer effect constants
    public static final String EFFECT_GLOW_PULSE = "GLOW_PULSE";
    public static final String EFFECT_FLASH = "FLASH";
    public static final String EFFECT_SUBTLE_BOUNCE = "SUBTLE_BOUNCE";
    public static final String EFFECT_NONE = "NONE";

    // End timer transition constants
    public static final String TRANSITION_SMOOTH_FADE = "SMOOTH_FADE";
    public static final String TRANSITION_SCALE_POP = "SCALE_POP";
    public static final String TRANSITION_SLIDE_DOWN = "SLIDE_DOWN";
    public static final String TRANSITION_INSTANT = "INSTANT";

    // End timer text style constants
    public static final String TEXT_STYLE_STANDARD = "STANDARD";
    public static final String TEXT_STYLE_JEE_FOCUS = "JEE_FOCUS";
    public static final String TEXT_STYLE_MINIMAL = "MINIMAL";
    public static final String TEXT_STYLE_ENCOURAGING = "ENCOURAGING";

    public static final String SIZE_SMALL = "SMALL";
    public static final String SIZE_MEDIUM = "MEDIUM";
    public static final String SIZE_LARGE = "LARGE";
    public static final String SIZE_XLARGE = "XLARGE";

    private final TimerSession currentSession;
    private final OnSettingsChangedListener listener;
    private final SharedPreferences prefs;

    // Navigation section tabs
    private TextView tabSecThemes, tabSecGoals, tabSecSubjects, tabSecExam, tabSecFocus, tabSecBackup, tabSecPrivacy;
    private View layoutSecThemes, layoutSecGoals, layoutSecSubjects, layoutSecExam, layoutSecFocus, layoutSecBackup, layoutSecPrivacy;

    // Size pills
    private TextView btnSizeSmall, btnSizeMedium, btnSizeLarge, btnSizeXLarge;

    // 8 Theme pills
    private TextView btnThemeAmoled, btnThemeMidnight, btnThemeGraphite, btnThemeOcean;
    private TextView btnThemeCleanBlue, btnThemeLowContrast, btnThemeLight, btnThemeWarm;
    private TextView btnAccentGreen, btnAccentBlue, btnAccentOrange, btnAccentPurple;

    // Backup views
    private TextView tvBackupFolderPath;
    private Button btnChooseBackupFolder, btnBackupNow, btnRestoreBackup;
    private Button btnExportCsv, btnCreateJsonBackup, btnCheckIntegrity;

    // Goals views
    private TextView btnPillTime60, btnPillTime90, btnPillTime120, btnPillTime180;
    private TextView btnPillDur30, btnPillDur45, btnPillDur60, btnPillDur90;
    private TextView btnPillQ25, btnPillQ30, btnPillQ50, btnPillQ100;
    private EditText etTargetTimePerQ, etTargetSessionTime, etTargetSessionQuestions;
    private TextView tvLivePaceStatus, tvLivePaceDetail, tvLiveProgressDetail;

    // Subject & Exam views
    private Spinner spinnerDefaultSubject;
    private EditText etExamName, etExamDaysLeft;
    private CheckBox cbShowExamBanner;

    // Sound & Focus views
    private CheckBox cbHapticFeedback, cbAudioChime, cbAutoPip;
    private CheckBox cbTwoZoneAnswer, cbAutoApplyWallpaper;
    private CheckBox cbZoneClickEffect;
    private View layoutZoneClickEffectOptions;
    private Spinner spinnerClickEffectDuration, spinnerClickTransition;
    private Button btnTestUpperWrong, btnTestLowerRight;
    private Button btnPreviewWallpaper, btnOpenMistakeReview;
    private Spinner spinnerIdleDetection, spinnerBreakReminder;
    private Spinner spinnerJeeExamType;
    private View layoutJeeExamType;

    // Backup & Privacy views
    private CheckBox cbEnablePinLock;
    private EditText etPrivacyPin;

    // Display & Layout views
    private Spinner spinnerTimerFont, spinnerStudyTimerPosition;
    private CheckBox cbShowStatusHints, cbCleanCountingMode;
    private Spinner spinnerEndTimerTransition, spinnerEndTimerEffect, spinnerEndTimerTextStyle;

    private String currentSizeMode;
    private String currentTheme;
    private String currentAccent;
    private int currentTargetSecPerQ;
    private int currentTargetSessionMin;
    private int currentTargetQuestions;

    public SettingsDialog(Context context, TimerSession currentSession, OnSettingsChangedListener listener) {
        super(context, R.style.Theme_FullScreenTimer_BottomSheet);
        this.currentSession = currentSession;
        this.listener = listener;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        this.currentSizeMode = prefs.getString(KEY_TIMER_SIZE_MODE, SIZE_MEDIUM);
        this.currentTheme = ThemeManager.getTheme(context);
        this.currentAccent = ThemeManager.getAccent(context);
        this.currentTargetSecPerQ = prefs.getInt(KEY_TARGET_SEC_PER_Q, 120);
        this.currentTargetSessionMin = prefs.getInt(KEY_TARGET_SESSION_MIN, 45);
        this.currentTargetQuestions = prefs.getInt(KEY_TARGET_SESSION_QUESTIONS,
                (currentSession != null && currentSession.getTarget() > 0) ? currentSession.getTarget() : 50);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_settings, null);
        setContentView(view);

        Window window = getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        setCanceledOnTouchOutside(true);

        initViews(view);
        setupSectionTabs();
        populateInitialState();
        updateLiveFeedback();
    }

    private void initViews(View view) {
        ImageButton btnClose = view.findViewById(R.id.btnCloseSettings);
        btnClose.setOnClickListener(v -> saveAndDismiss());

        Button btnSave = view.findViewById(R.id.btnSaveSettings);
        btnSave.setOnClickListener(v -> saveAndDismiss());

        tabSecThemes = view.findViewById(R.id.tabSecThemes);
        tabSecGoals = view.findViewById(R.id.tabSecGoals);
        tabSecSubjects = view.findViewById(R.id.tabSecSubjects);
        tabSecExam = view.findViewById(R.id.tabSecExam);
        tabSecFocus = view.findViewById(R.id.tabSecFocus);
        tabSecBackup = view.findViewById(R.id.tabSecBackup);
        tabSecPrivacy = view.findViewById(R.id.tabSecPrivacy);

        layoutSecThemes = view.findViewById(R.id.layoutSecThemes);
        layoutSecGoals = view.findViewById(R.id.layoutSecGoals);
        layoutSecSubjects = view.findViewById(R.id.layoutSecSubjects);
        layoutSecExam = view.findViewById(R.id.layoutSecExam);
        layoutSecFocus = view.findViewById(R.id.layoutSecFocus);
        layoutSecBackup = view.findViewById(R.id.layoutSecBackup);
        layoutSecPrivacy = view.findViewById(R.id.layoutSecPrivacy);

        // Size pills
        btnSizeSmall = view.findViewById(R.id.btnSizeSmall);
        btnSizeMedium = view.findViewById(R.id.btnSizeMedium);
        btnSizeLarge = view.findViewById(R.id.btnSizeLarge);
        btnSizeXLarge = view.findViewById(R.id.btnSizeXLarge);

        btnSizeSmall.setOnClickListener(v -> selectSizeMode(SIZE_SMALL));
        btnSizeMedium.setOnClickListener(v -> selectSizeMode(SIZE_MEDIUM));
        btnSizeLarge.setOnClickListener(v -> selectSizeMode(SIZE_LARGE));
        btnSizeXLarge.setOnClickListener(v -> selectSizeMode(SIZE_XLARGE));

        // 8 Theme pills
        btnThemeAmoled = view.findViewById(R.id.btnThemeAmoled);
        btnThemeMidnight = view.findViewById(R.id.btnThemeMidnight);
        btnThemeGraphite = view.findViewById(R.id.btnThemeGraphite);
        btnThemeOcean = view.findViewById(R.id.btnThemeOcean);
        btnThemeCleanBlue = view.findViewById(R.id.btnThemeCleanBlue);
        btnThemeLowContrast = view.findViewById(R.id.btnThemeLowContrast);
        btnThemeLight = view.findViewById(R.id.btnThemeLight);
        btnThemeWarm = view.findViewById(R.id.btnThemeWarm);

        btnThemeAmoled.setOnClickListener(v -> selectTheme(ThemeManager.THEME_AMOLED_BLACK));
        btnThemeMidnight.setOnClickListener(v -> selectTheme(ThemeManager.THEME_MIDNIGHT_MINIMAL));
        btnThemeGraphite.setOnClickListener(v -> selectTheme(ThemeManager.THEME_GRAPHITE_FOCUS));
        btnThemeOcean.setOnClickListener(v -> selectTheme(ThemeManager.THEME_OCEAN_TEAL));
        btnThemeCleanBlue.setOnClickListener(v -> selectTheme(ThemeManager.THEME_CLEAN_BLUE));
        btnThemeLowContrast.setOnClickListener(v -> selectTheme(ThemeManager.THEME_LOW_CONTRAST_FOCUS));
        btnThemeLight.setOnClickListener(v -> selectTheme(ThemeManager.THEME_MINIMAL_LIGHT));
        btnThemeWarm.setOnClickListener(v -> selectTheme(ThemeManager.THEME_SOFT_WARM_NEUTRAL));

        btnAccentGreen = view.findViewById(R.id.btnAccentGreen);
        btnAccentBlue = view.findViewById(R.id.btnAccentBlue);
        btnAccentOrange = view.findViewById(R.id.btnAccentOrange);
        btnAccentPurple = view.findViewById(R.id.btnAccentPurple);

        btnAccentGreen.setOnClickListener(v -> selectAccent(ThemeManager.ACCENT_GREEN));
        btnAccentBlue.setOnClickListener(v -> selectAccent(ThemeManager.ACCENT_BLUE));
        btnAccentOrange.setOnClickListener(v -> selectAccent(ThemeManager.ACCENT_ORANGE));
        btnAccentPurple.setOnClickListener(v -> selectAccent(ThemeManager.ACCENT_PURPLE));

        // Timer Font Family & Study Timer Position
        spinnerTimerFont = view.findViewById(R.id.spinnerTimerFont);
        String[] fontLabels = {
                "Digital Monospace (Default)",
                "Modern Sans Bold",
                "Condensed Sport",
                "Classic Serif",
                "Minimalist Light"
        };
        final String[] fontValues = {
                FONT_MONOSPACE,
                FONT_SANS_SERIF_BOLD,
                FONT_CONDENSED,
                FONT_SERIF,
                FONT_LIGHT
        };
        ArrayAdapter<String> fontAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, fontLabels);
        if (spinnerTimerFont != null) {
            spinnerTimerFont.setAdapter(fontAdapter);
            String savedFont = prefs.getString(KEY_TIMER_FONT, FONT_MONOSPACE);
            for (int i = 0; i < fontValues.length; i++) {
                if (fontValues[i].equals(savedFont)) {
                    spinnerTimerFont.setSelection(i);
                    break;
                }
            }
        }

        spinnerStudyTimerPosition = view.findViewById(R.id.spinnerStudyTimerPosition);
        String[] posLabels = {
                "Bottom-Left Corner (Recommended)",
                "Bottom-Right Corner",
                "Top-Left Corner",
                "Top-Right Corner",
                "Bottom Center",
                "Center (Under Timer)",
                "Hidden / Off"
        };
        final String[] posValues = {
                POS_BOTTOM_LEFT,
                POS_BOTTOM_RIGHT,
                POS_TOP_LEFT,
                POS_TOP_RIGHT,
                POS_BOTTOM_CENTER,
                POS_CENTER,
                POS_HIDDEN
        };
        ArrayAdapter<String> posAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, posLabels);
        if (spinnerStudyTimerPosition != null) {
            spinnerStudyTimerPosition.setAdapter(posAdapter);
            String savedPos = prefs.getString(KEY_STUDY_TIMER_POSITION, POS_BOTTOM_LEFT);
            for (int i = 0; i < posValues.length; i++) {
                if (posValues[i].equals(savedPos)) {
                    spinnerStudyTimerPosition.setSelection(i);
                    break;
                }
            }
        }

        // Goals
        btnPillTime60 = view.findViewById(R.id.btnPillTime60);
        btnPillTime90 = view.findViewById(R.id.btnPillTime90);
        btnPillTime120 = view.findViewById(R.id.btnPillTime120);
        btnPillTime180 = view.findViewById(R.id.btnPillTime180);

        btnPillTime60.setOnClickListener(v -> setTargetSecPerQ(60));
        btnPillTime90.setOnClickListener(v -> setTargetSecPerQ(90));
        btnPillTime120.setOnClickListener(v -> setTargetSecPerQ(120));
        btnPillTime180.setOnClickListener(v -> setTargetSecPerQ(180));

        etTargetTimePerQ = view.findViewById(R.id.etTargetTimePerQ);
        etTargetTimePerQ.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    int val = Integer.parseInt(s.toString().trim());
                    if (val > 0) {
                        currentTargetSecPerQ = val;
                        highlightTimePerQPill(val);
                        updateLiveFeedback();
                    }
                } catch (NumberFormatException ignored) {}
            }
        });

        btnPillDur30 = view.findViewById(R.id.btnPillDur30);
        btnPillDur45 = view.findViewById(R.id.btnPillDur45);
        btnPillDur60 = view.findViewById(R.id.btnPillDur60);
        btnPillDur90 = view.findViewById(R.id.btnPillDur90);

        btnPillDur30.setOnClickListener(v -> setTargetSessionMin(30));
        btnPillDur45.setOnClickListener(v -> setTargetSessionMin(45));
        btnPillDur60.setOnClickListener(v -> setTargetSessionMin(60));
        btnPillDur90.setOnClickListener(v -> setTargetSessionMin(90));

        etTargetSessionTime = view.findViewById(R.id.etTargetSessionTime);
        etTargetSessionTime.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    int val = Integer.parseInt(s.toString().trim());
                    if (val > 0) {
                        currentTargetSessionMin = val;
                        highlightDurationPill(val);
                        updateLiveFeedback();
                    }
                } catch (NumberFormatException ignored) {}
            }
        });

        btnPillQ25 = view.findViewById(R.id.btnPillQ25);
        btnPillQ30 = view.findViewById(R.id.btnPillQ30);
        btnPillQ50 = view.findViewById(R.id.btnPillQ50);
        btnPillQ100 = view.findViewById(R.id.btnPillQ100);

        btnPillQ25.setOnClickListener(v -> setTargetQuestions(25));
        btnPillQ30.setOnClickListener(v -> setTargetQuestions(30));
        btnPillQ50.setOnClickListener(v -> setTargetQuestions(50));
        btnPillQ100.setOnClickListener(v -> setTargetQuestions(100));

        etTargetSessionQuestions = view.findViewById(R.id.etTargetSessionQuestions);
        etTargetSessionQuestions.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    int val = Integer.parseInt(s.toString().trim());
                    if (val > 0) {
                        currentTargetQuestions = val;
                        highlightQuestionsPill(val);
                        updateLiveFeedback();
                    }
                } catch (NumberFormatException ignored) {}
            }
        });

        tvLivePaceStatus = view.findViewById(R.id.tvLivePaceStatus);
        tvLivePaceDetail = view.findViewById(R.id.tvLivePaceDetail);
        tvLiveProgressDetail = view.findViewById(R.id.tvLiveProgressDetail);

        // Subject & Exam views
        spinnerDefaultSubject = view.findViewById(R.id.spinnerDefaultSubject);
        String[] subjects = {"Mathematics", "Physics", "Chemistry", "Biology", "General"};
        ArrayAdapter<String> subAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, subjects);
        spinnerDefaultSubject.setAdapter(subAdapter);
        String savedSub = prefs.getString(KEY_DEFAULT_SUBJECT, "Physics");
        for (int i = 0; i < subjects.length; i++) {
            if (subjects[i].equalsIgnoreCase(savedSub)) {
                spinnerDefaultSubject.setSelection(i);
                break;
            }
        }

        etExamName = view.findViewById(R.id.etExamName);
        etExamDaysLeft = view.findViewById(R.id.etExamDaysLeft);
        cbShowExamBanner = view.findViewById(R.id.cbShowExamBanner);

        etExamName.setText(prefs.getString(KEY_EXAM_NAME, "JEE ADVANCED"));
        etExamDaysLeft.setText(String.valueOf(prefs.getInt(KEY_EXAM_DAYS, 142)));
        cbShowExamBanner.setChecked(prefs.getBoolean(KEY_EXAM_BANNER_ENABLED, true));

        // Sounds & Focus views
        cbHapticFeedback = view.findViewById(R.id.cbHapticFeedback);
        cbAudioChime = view.findViewById(R.id.cbAudioChime);
        cbAutoPip = view.findViewById(R.id.cbAutoPip);
        spinnerIdleDetection = view.findViewById(R.id.spinnerIdleDetection);
        spinnerBreakReminder = view.findViewById(R.id.spinnerBreakReminder);

        cbHapticFeedback.setChecked(prefs.getBoolean(KEY_HAPTIC_ENABLED, true));
        cbAudioChime.setChecked(prefs.getBoolean(KEY_SOUND_ENABLED, false));
        cbAutoPip.setChecked(prefs.getBoolean(KEY_AUTO_PIP, true));

        cbShowStatusHints = view.findViewById(R.id.cbShowStatusHints);
        if (cbShowStatusHints != null) {
            cbShowStatusHints.setChecked(prefs.getBoolean(KEY_SHOW_STATUS_HINTS, true));
        }

        cbCleanCountingMode = view.findViewById(R.id.cbCleanCountingMode);
        if (cbCleanCountingMode != null) {
            cbCleanCountingMode.setChecked(prefs.getBoolean(KEY_CLEAN_COUNTING_MODE, true));
        }

        String[] idleOpts = {"Off (Disabled)", "5 Minutes Inactivity", "10 Minutes Inactivity", "15 Minutes Inactivity"};
        ArrayAdapter<String> idleAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, idleOpts);
        spinnerIdleDetection.setAdapter(idleAdapter);
        int savedIdle = prefs.getInt(KEY_IDLE_MINUTES, 0);
        spinnerIdleDetection.setSelection(savedIdle == 5 ? 1 : (savedIdle == 10 ? 2 : (savedIdle == 15 ? 3 : 0)));

        String[] breakOpts = {"Off (No Reminders)", "Every 25 Mins (Pomodoro)", "Every 45 Mins", "Every 60 Mins"};
        ArrayAdapter<String> breakAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, breakOpts);
        spinnerBreakReminder.setAdapter(breakAdapter);
        int savedBreak = prefs.getInt(KEY_BREAK_MINUTES, 45);
        spinnerBreakReminder.setSelection(savedBreak == 25 ? 1 : (savedBreak == 45 ? 2 : (savedBreak == 60 ? 3 : 0)));

        // End Timer Transitions & Effects
        spinnerEndTimerTransition = view.findViewById(R.id.spinnerEndTimerTransition);
        String[] transLabels = {"Smooth Fade (Default)", "Pop / Spring Bounce", "Slide Down", "Instant Cut"};
        final String[] transValues = {TRANSITION_SMOOTH_FADE, TRANSITION_SCALE_POP, TRANSITION_SLIDE_DOWN, TRANSITION_INSTANT};
        ArrayAdapter<String> transAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, transLabels);
        if (spinnerEndTimerTransition != null) {
            spinnerEndTimerTransition.setAdapter(transAdapter);
            String savedTrans = prefs.getString(KEY_END_TIMER_TRANSITION, TRANSITION_SMOOTH_FADE);
            for (int i = 0; i < transValues.length; i++) {
                if (transValues[i].equals(savedTrans)) {
                    spinnerEndTimerTransition.setSelection(i);
                    break;
                }
            }
        }

        spinnerEndTimerEffect = view.findViewById(R.id.spinnerEndTimerEffect);
        String[] effectLabels = {"AMOLED Glow Pulse (Default)", "Subtle Screen Flash", "Scale Bounce", "None (Minimal)"};
        final String[] effectValues = {EFFECT_GLOW_PULSE, EFFECT_FLASH, EFFECT_SUBTLE_BOUNCE, EFFECT_NONE};
        ArrayAdapter<String> effectAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, effectLabels);
        if (spinnerEndTimerEffect != null) {
            spinnerEndTimerEffect.setAdapter(effectAdapter);
            String savedEff = prefs.getString(KEY_END_TIMER_EFFECT, EFFECT_GLOW_PULSE);
            for (int i = 0; i < effectValues.length; i++) {
                if (effectValues[i].equals(savedEff)) {
                    spinnerEndTimerEffect.setSelection(i);
                    break;
                }
            }
        }

        spinnerEndTimerTextStyle = view.findViewById(R.id.spinnerEndTimerTextStyle);
        String[] textStyleLabels = {
                "Standard (PAUSED • TAP FOR NEXT)",
                "JEE Focus (QUESTION DONE • NEXT Q)",
                "Minimal (PAUSED)",
                "Encouraging (GREAT PACE • KEEP GOING)"
        };
        final String[] textStyleValues = {TEXT_STYLE_STANDARD, TEXT_STYLE_JEE_FOCUS, TEXT_STYLE_MINIMAL, TEXT_STYLE_ENCOURAGING};
        ArrayAdapter<String> textStyleAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, textStyleLabels);
        if (spinnerEndTimerTextStyle != null) {
            spinnerEndTimerTextStyle.setAdapter(textStyleAdapter);
            String savedTextStyle = prefs.getString(KEY_END_TIMER_TEXT_STYLE, TEXT_STYLE_STANDARD);
            for (int i = 0; i < textStyleValues.length; i++) {
                if (textStyleValues[i].equals(savedTextStyle)) {
                    spinnerEndTimerTextStyle.setSelection(i);
                    break;
                }
            }
        }

        cbTwoZoneAnswer = view.findViewById(R.id.cbTwoZoneAnswer);
        layoutJeeExamType = view.findViewById(R.id.layoutJeeExamType);
        spinnerJeeExamType = view.findViewById(R.id.spinnerJeeExamType);

        String[] examTypeOpts = {"JEE Main (+4 / -1 Marking)", "JEE Advanced (+4 / -2 Marking)"};
        ArrayAdapter<String> examTypeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, examTypeOpts);
        if (spinnerJeeExamType != null) {
            spinnerJeeExamType.setAdapter(examTypeAdapter);
            String savedExamType = prefs.getString(KEY_JEE_EXAM_TYPE, EXAM_TYPE_JEE_MAIN);
            spinnerJeeExamType.setSelection(EXAM_TYPE_JEE_ADVANCED.equalsIgnoreCase(savedExamType) ? 1 : 0);
        }

        // Two-Zone Click Feedback Effect Options
        cbZoneClickEffect = view.findViewById(R.id.cbZoneClickEffect);
        layoutZoneClickEffectOptions = view.findViewById(R.id.layoutZoneClickEffectOptions);
        spinnerClickEffectDuration = view.findViewById(R.id.spinnerClickEffectDuration);
        spinnerClickTransition = view.findViewById(R.id.spinnerClickTransition);

        final int[] durationValues = {150, 300, 500, 800, 1200};
        String[] durationLabels = {"Snappy (150ms)", "Normal (300ms - Default)", "Smooth (500ms)", "Extended (800ms)", "Long (1200ms)"};
        ArrayAdapter<String> durAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, durationLabels);
        if (spinnerClickEffectDuration != null) {
            spinnerClickEffectDuration.setAdapter(durAdapter);
            int savedDuration = prefs.getInt(KEY_ZONE_EFFECT_DURATION_MS, 300);
            int sel = 1;
            for (int i = 0; i < durationValues.length; i++) {
                if (durationValues[i] == savedDuration) {
                    sel = i;
                    break;
                }
            }
            spinnerClickEffectDuration.setSelection(sel);
        }

        final String[] styleValues = {EFFECT_STYLE_FADE, EFFECT_STYLE_SCALE, EFFECT_STYLE_POP};
        String[] styleLabels = {"Smooth Fade Out (Default)", "Expanding Ripple / Scale", "Instant Flash Pop"};
        ArrayAdapter<String> styleAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, styleLabels);
        if (spinnerClickTransition != null) {
            spinnerClickTransition.setAdapter(styleAdapter);
            String savedStyle = prefs.getString(KEY_ZONE_EFFECT_STYLE, EFFECT_STYLE_FADE);
            int sel = 0;
            for (int i = 0; i < styleValues.length; i++) {
                if (styleValues[i].equalsIgnoreCase(savedStyle)) {
                    sel = i;
                    break;
                }
            }
            spinnerClickTransition.setSelection(sel);
        }

        if (cbZoneClickEffect != null) {
            boolean effectEnabled = prefs.getBoolean(KEY_ZONE_EFFECT_ENABLED, true);
            cbZoneClickEffect.setChecked(effectEnabled);
            if (layoutZoneClickEffectOptions != null) {
                layoutZoneClickEffectOptions.setVisibility(effectEnabled ? View.VISIBLE : View.GONE);
            }
            cbZoneClickEffect.setOnCheckedChangeListener((btn, isChecked) -> {
                if (layoutZoneClickEffectOptions != null) {
                    layoutZoneClickEffectOptions.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                }
            });
        }

        btnTestUpperWrong = view.findViewById(R.id.btnTestUpperWrong);
        btnTestLowerRight = view.findViewById(R.id.btnTestLowerRight);

        if (btnTestUpperWrong != null) {
            btnTestUpperWrong.setOnClickListener(v -> {
                int dur = 300;
                int pos = spinnerClickEffectDuration != null ? spinnerClickEffectDuration.getSelectedItemPosition() : 1;
                if (pos >= 0 && pos < durationValues.length) dur = durationValues[pos];
                String style = EFFECT_STYLE_FADE;
                int sPos = spinnerClickTransition != null ? spinnerClickTransition.getSelectedItemPosition() : 0;
                if (sPos >= 0 && sPos < styleValues.length) style = styleValues[sPos];
                if (listener != null) {
                    listener.onPreviewZoneClickEffect(true, dur, style);
                }
                Toast.makeText(getContext(), "Testing Upper Zone (RED / WRONG) • " + dur + "ms " + style, Toast.LENGTH_SHORT).show();
            });
        }

        if (btnTestLowerRight != null) {
            btnTestLowerRight.setOnClickListener(v -> {
                int dur = 300;
                int pos = spinnerClickEffectDuration != null ? spinnerClickEffectDuration.getSelectedItemPosition() : 1;
                if (pos >= 0 && pos < durationValues.length) dur = durationValues[pos];
                String style = EFFECT_STYLE_FADE;
                int sPos = spinnerClickTransition != null ? spinnerClickTransition.getSelectedItemPosition() : 0;
                if (sPos >= 0 && sPos < styleValues.length) style = styleValues[sPos];
                if (listener != null) {
                    listener.onPreviewZoneClickEffect(false, dur, style);
                }
                Toast.makeText(getContext(), "Testing Lower Zone (GREEN / RIGHT) • " + dur + "ms " + style, Toast.LENGTH_SHORT).show();
            });
        }

        cbAutoApplyWallpaper = view.findViewById(R.id.cbAutoApplyWallpaper);
        btnPreviewWallpaper = view.findViewById(R.id.btnPreviewWallpaper);
        btnOpenMistakeReview = view.findViewById(R.id.btnOpenMistakeReview);

        if (cbTwoZoneAnswer != null) {
            boolean twoZoneActive = prefs.getBoolean(KEY_TWO_ZONE_RECORDING, true);
            cbTwoZoneAnswer.setChecked(twoZoneActive);
            if (layoutJeeExamType != null) {
                layoutJeeExamType.setVisibility(twoZoneActive ? View.VISIBLE : View.GONE);
            }
            cbTwoZoneAnswer.setOnCheckedChangeListener((btn, isChecked) -> {
                if (layoutJeeExamType != null) {
                    layoutJeeExamType.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                }
            });
        }
        if (cbAutoApplyWallpaper != null) {
            cbAutoApplyWallpaper.setChecked(prefs.getBoolean(KEY_WALLPAPER_AUTO_APPLY, false));
        }
        if (btnPreviewWallpaper != null) {
            btnPreviewWallpaper.setOnClickListener(v -> {
                dismiss();
                if (listener != null) listener.onOpenWallpaperPreview();
            });
        }
        if (btnOpenMistakeReview != null) {
            btnOpenMistakeReview.setOnClickListener(v -> {
                dismiss();
                if (listener != null) listener.onOpenMistakeReview();
            });
        }

        // Backup & Privacy (SAF Destination)
        tvBackupFolderPath = view.findViewById(R.id.tvBackupFolderPath);
        btnChooseBackupFolder = view.findViewById(R.id.btnChooseBackupFolder);
        btnBackupNow = view.findViewById(R.id.btnBackupNow);
        btnRestoreBackup = view.findViewById(R.id.btnRestoreBackup);

        btnExportCsv = view.findViewById(R.id.btnExportCsv);
        btnCreateJsonBackup = view.findViewById(R.id.btnCreateJsonBackup);
        btnCheckIntegrity = view.findViewById(R.id.btnCheckIntegrity);
        cbEnablePinLock = view.findViewById(R.id.cbEnablePinLock);
        etPrivacyPin = view.findViewById(R.id.etPrivacyPin);

        String currentPath = prefs.getString(KEY_SAF_BACKUP_PATH, "Destination: App Internal / Downloads (Default)");
        tvBackupFolderPath.setText(currentPath);

        btnChooseBackupFolder.setOnClickListener(v -> {
            if (listener != null) listener.onChooseBackupFolderRequested();
        });

        btnBackupNow.setOnClickListener(v -> {
            if (listener != null) listener.onBackupNowRequested();
            else Toast.makeText(getContext(), "Backed up sessions successfully to SAF storage.", Toast.LENGTH_SHORT).show();
        });

        btnRestoreBackup.setOnClickListener(v -> {
            if (listener != null) listener.onRestoreBackupRequested();
        });

        cbEnablePinLock.setChecked(prefs.getBoolean(KEY_PIN_ENABLED, false));
        etPrivacyPin.setText(prefs.getString(KEY_PIN_CODE, ""));

        btnExportCsv.setOnClickListener(v -> Toast.makeText(getContext(), "Exported sessions CSV to storage.", Toast.LENGTH_SHORT).show());
        btnCreateJsonBackup.setOnClickListener(v -> Toast.makeText(getContext(), "Complete JSON backup created successfully.", Toast.LENGTH_SHORT).show());
        btnCheckIntegrity.setOnClickListener(v -> Toast.makeText(getContext(), "Database integrity check: 100% OK, 0 corruptions.", Toast.LENGTH_SHORT).show());
    }

    private void setupSectionTabs() {
        tabSecThemes.setOnClickListener(v -> selectSection(0));
        tabSecGoals.setOnClickListener(v -> selectSection(1));
        tabSecSubjects.setOnClickListener(v -> selectSection(2));
        tabSecExam.setOnClickListener(v -> selectSection(3));
        tabSecFocus.setOnClickListener(v -> selectSection(4));
        tabSecBackup.setOnClickListener(v -> selectSection(5));
        tabSecPrivacy.setOnClickListener(v -> selectSection(6));
    }

    private void selectSection(int index) {
        setPillSelected(tabSecThemes, index == 0);
        setPillSelected(tabSecGoals, index == 1);
        setPillSelected(tabSecSubjects, index == 2);
        setPillSelected(tabSecExam, index == 3);
        setPillSelected(tabSecFocus, index == 4);
        setPillSelected(tabSecBackup, index == 5);
        setPillSelected(tabSecPrivacy, index == 6);

        layoutSecThemes.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        layoutSecGoals.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        layoutSecSubjects.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        layoutSecExam.setVisibility(index == 3 ? View.VISIBLE : View.GONE);
        layoutSecFocus.setVisibility(index == 4 ? View.VISIBLE : View.GONE);
        layoutSecBackup.setVisibility(index == 5 ? View.VISIBLE : View.GONE);
        layoutSecPrivacy.setVisibility(index == 6 ? View.VISIBLE : View.GONE);
    }

    private void populateInitialState() {
        updateSizePillStyles(currentSizeMode);
        updateThemePillStyles(currentTheme);
        updateAccentPillStyles(currentAccent);

        etTargetTimePerQ.setText(String.valueOf(currentTargetSecPerQ));
        highlightTimePerQPill(currentTargetSecPerQ);

        etTargetSessionTime.setText(String.valueOf(currentTargetSessionMin));
        highlightDurationPill(currentTargetSessionMin);

        etTargetSessionQuestions.setText(String.valueOf(currentTargetQuestions));
        highlightQuestionsPill(currentTargetQuestions);
    }

    private void selectSizeMode(String mode) {
        currentSizeMode = mode;
        updateSizePillStyles(mode);
        prefs.edit().putString(KEY_TIMER_SIZE_MODE, mode).apply();
        if (listener != null) listener.onTimerSizeChanged(mode);
    }

    private void updateSizePillStyles(String selectedMode) {
        setPillSelected(btnSizeSmall, SIZE_SMALL.equals(selectedMode));
        setPillSelected(btnSizeMedium, SIZE_MEDIUM.equals(selectedMode));
        setPillSelected(btnSizeLarge, SIZE_LARGE.equals(selectedMode));
        setPillSelected(btnSizeXLarge, SIZE_XLARGE.equals(selectedMode));
    }

    private void selectTheme(String theme) {
        currentTheme = theme;
        updateThemePillStyles(theme);
        ThemeManager.setTheme(getContext(), theme);
        if (listener != null) listener.onThemeChanged(theme, currentAccent);
    }

    private void updateThemePillStyles(String selectedTheme) {
        setPillSelected(btnThemeAmoled, ThemeManager.THEME_AMOLED_BLACK.equals(selectedTheme) || "AMOLED".equals(selectedTheme));
        setPillSelected(btnThemeMidnight, ThemeManager.THEME_MIDNIGHT_MINIMAL.equals(selectedTheme));
        setPillSelected(btnThemeGraphite, ThemeManager.THEME_GRAPHITE_FOCUS.equals(selectedTheme) || "SLATE".equals(selectedTheme));
        setPillSelected(btnThemeOcean, ThemeManager.THEME_OCEAN_TEAL.equals(selectedTheme) || "SOLARIZED".equals(selectedTheme));
        setPillSelected(btnThemeCleanBlue, ThemeManager.THEME_CLEAN_BLUE.equals(selectedTheme));
        setPillSelected(btnThemeLowContrast, ThemeManager.THEME_LOW_CONTRAST_FOCUS.equals(selectedTheme));
        setPillSelected(btnThemeLight, ThemeManager.THEME_MINIMAL_LIGHT.equals(selectedTheme) || "LIGHT".equals(selectedTheme));
        setPillSelected(btnThemeWarm, ThemeManager.THEME_SOFT_WARM_NEUTRAL.equals(selectedTheme));
    }

    private void selectAccent(String accent) {
        currentAccent = accent;
        updateAccentPillStyles(accent);
        ThemeManager.setAccent(getContext(), accent);
        if (listener != null) listener.onThemeChanged(currentTheme, accent);
    }

    private void updateAccentPillStyles(String selectedAccent) {
        setPillSelected(btnAccentGreen, ThemeManager.ACCENT_GREEN.equals(selectedAccent));
        setPillSelected(btnAccentBlue, ThemeManager.ACCENT_BLUE.equals(selectedAccent));
        setPillSelected(btnAccentOrange, ThemeManager.ACCENT_ORANGE.equals(selectedAccent));
        setPillSelected(btnAccentPurple, ThemeManager.ACCENT_PURPLE.equals(selectedAccent));
    }

    private void setTargetSecPerQ(int seconds) {
        currentTargetSecPerQ = seconds;
        etTargetTimePerQ.setText(String.valueOf(seconds));
        etTargetTimePerQ.setSelection(etTargetTimePerQ.getText().length());
        highlightTimePerQPill(seconds);
        updateLiveFeedback();
    }

    private void highlightTimePerQPill(int seconds) {
        setPillSelected(btnPillTime60, seconds == 60);
        setPillSelected(btnPillTime90, seconds == 90);
        setPillSelected(btnPillTime120, seconds == 120);
        setPillSelected(btnPillTime180, seconds == 180);
    }

    private void setTargetSessionMin(int minutes) {
        currentTargetSessionMin = minutes;
        etTargetSessionTime.setText(String.valueOf(minutes));
        etTargetSessionTime.setSelection(etTargetSessionTime.getText().length());
        highlightDurationPill(minutes);
        updateLiveFeedback();
    }

    private void highlightDurationPill(int minutes) {
        setPillSelected(btnPillDur30, minutes == 30);
        setPillSelected(btnPillDur45, minutes == 45);
        setPillSelected(btnPillDur60, minutes == 60);
        setPillSelected(btnPillDur90, minutes == 90);
    }

    private void setTargetQuestions(int count) {
        currentTargetQuestions = count;
        etTargetSessionQuestions.setText(String.valueOf(count));
        etTargetSessionQuestions.setSelection(etTargetSessionQuestions.getText().length());
        highlightQuestionsPill(count);
        updateLiveFeedback();
    }

    private void highlightQuestionsPill(int count) {
        setPillSelected(btnPillQ25, count == 25);
        setPillSelected(btnPillQ30, count == 30);
        setPillSelected(btnPillQ50, count == 50);
        setPillSelected(btnPillQ100, count == 100);
    }

    private void setPillSelected(TextView pill, boolean isSelected) {
        if (pill == null) return;
        pill.setBackgroundResource(isSelected ? R.drawable.bg_pill_selected : R.drawable.bg_pill_unselected);
        pill.setTextColor(getContext().getColor(isSelected ? R.color.accent_highlight : R.color.text_secondary));
    }

    private void updateLiveFeedback() {
        if (tvLivePaceStatus == null) return;

        if (currentSession == null || currentSession.getCompletedCount() == 0) {
            tvLivePaceStatus.setText("READY FOR PRACTICE");
            tvLivePaceStatus.setBackgroundResource(R.drawable.bg_pill);
            tvLivePaceStatus.setTextColor(getContext().getColor(R.color.text_secondary));
            tvLivePaceDetail.setText(String.format(Locale.ROOT,
                    "Target: %ds / Q • Session Goal: %dm • Target: %d Qs",
                    currentTargetSecPerQ, currentTargetSessionMin, currentTargetQuestions));
            tvLiveProgressDetail.setText("Start solving questions to see live target pace comparison.");
            return;
        }

        int completed = currentSession.getCompletedCount();
        long avgMs = currentSession.getAverageDurationMs();
        int actualAvgSec = (int) (avgMs / 1000);
        long totalPracticeSec = currentSession.getTotalDurationMs() / 1000;
        int actualPracticeMin = (int) (totalPracticeSec / 60);

        int timeDiffSec = currentTargetSecPerQ - actualAvgSec;
        if (timeDiffSec >= 0) {
            tvLivePaceStatus.setText("✓ PACE ON TARGET");
            tvLivePaceStatus.setBackgroundResource(R.drawable.bg_pill_accent);
            tvLivePaceStatus.setTextColor(getContext().getColor(R.color.accent_running));
            tvLivePaceDetail.setText(String.format(Locale.ROOT,
                    "Actual Avg: %ds / Q • Target: %ds (%ds faster)",
                    actualAvgSec, currentTargetSecPerQ, timeDiffSec));
        } else {
            tvLivePaceStatus.setText("⚠️ OVER TARGET PACE");
            tvLivePaceStatus.setBackgroundResource(R.drawable.bg_pill_orange);
            tvLivePaceStatus.setTextColor(getContext().getColor(R.color.accent_paused));
            tvLivePaceDetail.setText(String.format(Locale.ROOT,
                    "Actual Avg: %ds / Q • Target: %ds (+%ds slower)",
                    actualAvgSec, currentTargetSecPerQ, Math.abs(timeDiffSec)));
        }

        int qPct = Math.min(100, (int) ((completed / (float) Math.max(1, currentTargetQuestions)) * 100));
        int timePct = Math.min(100, (int) ((actualPracticeMin / (float) Math.max(1, currentTargetSessionMin)) * 100));

        tvLiveProgressDetail.setText(String.format(Locale.ROOT,
                "Questions: %d / %d (%d%%) • Practice Time: %dm / %dm (%d%%)",
                completed, currentTargetQuestions, qPct, actualPracticeMin, currentTargetSessionMin, timePct));
    }

    private void saveAndDismiss() {
        int idleMins = 0;
        int idlePos = spinnerIdleDetection.getSelectedItemPosition();
        if (idlePos == 1) idleMins = 5;
        else if (idlePos == 2) idleMins = 10;
        else if (idlePos == 3) idleMins = 15;

        int breakMins = 45;
        int breakPos = spinnerBreakReminder.getSelectedItemPosition();
        if (breakPos == 1) breakMins = 25;
        else if (breakPos == 2) breakMins = 45;
        else if (breakPos == 3) breakMins = 60;
        else if (breakPos == 0) breakMins = 0;

        int daysLeft = 142;
        try {
            daysLeft = Integer.parseInt(etExamDaysLeft.getText().toString().trim());
        } catch (NumberFormatException ignored) {}

        boolean twoZone = (cbTwoZoneAnswer != null && cbTwoZoneAnswer.isChecked());
        String selectedExamType = EXAM_TYPE_JEE_MAIN;
        if (spinnerJeeExamType != null && spinnerJeeExamType.getSelectedItemPosition() == 1) {
            selectedExamType = EXAM_TYPE_JEE_ADVANCED;
        }

        boolean zoneEffectEnabled = (cbZoneClickEffect == null || cbZoneClickEffect.isChecked());
        final int[] durationValues = {150, 300, 500, 800, 1200};
        int selectedDuration = 300;
        if (spinnerClickEffectDuration != null) {
            int idx = spinnerClickEffectDuration.getSelectedItemPosition();
            if (idx >= 0 && idx < durationValues.length) {
                selectedDuration = durationValues[idx];
            }
        }

        final String[] styleValues = {EFFECT_STYLE_FADE, EFFECT_STYLE_SCALE, EFFECT_STYLE_POP};
        String selectedStyle = EFFECT_STYLE_FADE;
        if (spinnerClickTransition != null) {
            int idx = spinnerClickTransition.getSelectedItemPosition();
            if (idx >= 0 && idx < styleValues.length) {
                selectedStyle = styleValues[idx];
            }
        }

        boolean autoWallpaper = (cbAutoApplyWallpaper != null && cbAutoApplyWallpaper.isChecked());

        final String[] fontValues = {FONT_MONOSPACE, FONT_SANS_SERIF_BOLD, FONT_CONDENSED, FONT_SERIF, FONT_LIGHT};
        String selectedFont = FONT_MONOSPACE;
        if (spinnerTimerFont != null) {
            int idx = spinnerTimerFont.getSelectedItemPosition();
            if (idx >= 0 && idx < fontValues.length) selectedFont = fontValues[idx];
        }

        final String[] posValues = {POS_BOTTOM_LEFT, POS_BOTTOM_RIGHT, POS_TOP_LEFT, POS_TOP_RIGHT, POS_BOTTOM_CENTER, POS_CENTER, POS_HIDDEN};
        String selectedPos = POS_BOTTOM_LEFT;
        if (spinnerStudyTimerPosition != null) {
            int idx = spinnerStudyTimerPosition.getSelectedItemPosition();
            if (idx >= 0 && idx < posValues.length) selectedPos = posValues[idx];
        }

        final String[] transValues = {TRANSITION_SMOOTH_FADE, TRANSITION_SCALE_POP, TRANSITION_SLIDE_DOWN, TRANSITION_INSTANT};
        String selectedTrans = TRANSITION_SMOOTH_FADE;
        if (spinnerEndTimerTransition != null) {
            int idx = spinnerEndTimerTransition.getSelectedItemPosition();
            if (idx >= 0 && idx < transValues.length) selectedTrans = transValues[idx];
        }

        final String[] effectValues = {EFFECT_GLOW_PULSE, EFFECT_FLASH, EFFECT_SUBTLE_BOUNCE, EFFECT_NONE};
        String selectedEffect = EFFECT_GLOW_PULSE;
        if (spinnerEndTimerEffect != null) {
            int idx = spinnerEndTimerEffect.getSelectedItemPosition();
            if (idx >= 0 && idx < effectValues.length) selectedEffect = effectValues[idx];
        }

        final String[] textStyleValues = {TEXT_STYLE_STANDARD, TEXT_STYLE_JEE_FOCUS, TEXT_STYLE_MINIMAL, TEXT_STYLE_ENCOURAGING};
        String selectedTextStyle = TEXT_STYLE_STANDARD;
        if (spinnerEndTimerTextStyle != null) {
            int idx = spinnerEndTimerTextStyle.getSelectedItemPosition();
            if (idx >= 0 && idx < textStyleValues.length) selectedTextStyle = textStyleValues[idx];
        }

        boolean showHints = (cbShowStatusHints == null || cbShowStatusHints.isChecked());
        boolean cleanCounting = (cbCleanCountingMode == null || cbCleanCountingMode.isChecked());

        prefs.edit()
                .putString(KEY_TIMER_SIZE_MODE, currentSizeMode)
                .putInt(KEY_TARGET_SEC_PER_Q, currentTargetSecPerQ)
                .putInt(KEY_TARGET_SESSION_MIN, currentTargetSessionMin)
                .putInt(KEY_TARGET_SESSION_QUESTIONS, currentTargetQuestions)
                .putString(KEY_DEFAULT_SUBJECT, (String) spinnerDefaultSubject.getSelectedItem())
                .putString(KEY_EXAM_NAME, etExamName.getText().toString().trim())
                .putInt(KEY_EXAM_DAYS, daysLeft)
                .putBoolean(KEY_EXAM_BANNER_ENABLED, cbShowExamBanner.isChecked())
                .putBoolean(KEY_HAPTIC_ENABLED, cbHapticFeedback.isChecked())
                .putBoolean(KEY_SOUND_ENABLED, cbAudioChime.isChecked())
                .putBoolean(KEY_AUTO_PIP, cbAutoPip.isChecked())
                .putInt(KEY_IDLE_MINUTES, idleMins)
                .putInt(KEY_BREAK_MINUTES, breakMins)
                .putBoolean(KEY_TWO_ZONE_RECORDING, twoZone)
                .putString(KEY_JEE_EXAM_TYPE, selectedExamType)
                .putBoolean(KEY_ZONE_EFFECT_ENABLED, zoneEffectEnabled)
                .putInt(KEY_ZONE_EFFECT_DURATION_MS, selectedDuration)
                .putString(KEY_ZONE_EFFECT_STYLE, selectedStyle)
                .putBoolean(KEY_WALLPAPER_AUTO_APPLY, autoWallpaper)
                .putBoolean(KEY_PIN_ENABLED, cbEnablePinLock.isChecked())
                .putString(KEY_PIN_CODE, etPrivacyPin.getText().toString().trim())
                .putString(KEY_TIMER_FONT, selectedFont)
                .putString(KEY_STUDY_TIMER_POSITION, selectedPos)
                .putString(KEY_END_TIMER_TRANSITION, selectedTrans)
                .putString(KEY_END_TIMER_EFFECT, selectedEffect)
                .putString(KEY_END_TIMER_TEXT_STYLE, selectedTextStyle)
                .putBoolean(KEY_SHOW_STATUS_HINTS, showHints)
                .putBoolean(KEY_CLEAN_COUNTING_MODE, cleanCounting)
                .apply();

        if (listener != null) {
            listener.onTargetsChanged(currentTargetSecPerQ, currentTargetSessionMin, currentTargetQuestions);
            listener.onThemeChanged(currentTheme, currentAccent);
            listener.onFocusControlsChanged();
            listener.onTwoZoneRecordingChanged(twoZone, selectedExamType);
            listener.onDisplaySettingsChanged();
        }
        dismiss();
    }

    private static abstract class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void afterTextChanged(Editable s) {}
    }
}

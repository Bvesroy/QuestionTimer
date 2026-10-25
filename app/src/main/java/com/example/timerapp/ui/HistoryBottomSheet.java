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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.timerapp.R;
import com.example.timerapp.data.analytics.StudyCoachEngine;
import com.example.timerapp.model.QuestionLog;
import com.example.timerapp.model.TimerSession;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Modern AMOLED Bottom Sheet Modal Dialog providing:
 * 1. Active Session Log, Breakdown & Completion Notes
 * 2. Date-Grouped History with Keyword Search, Subject Filtering, Side-by-Side Comparison & Notes Editor
 * 3. Comprehensive Visual Analytics: Heatmap, Moving Averages, Weekly/Monthly Reports,
 *    Consistency & Focus Scores, Time-of-Day Performance, and Personal Records.
 */
public class HistoryBottomSheet extends Dialog {

    public interface HistoryActionListener {
        void onNewSessionRequested();
        void onSwitchSessionRequested();
        void onClearHistoryRequested();
        void onResumeSessionRequested(TimerSession session);
        void onSessionNotesEdited(TimerSession session);
    }

    private enum Tab {
        CURRENT,
        HISTORY,
        ANALYTICS
    }

    private enum TimeRange {
        TODAY,
        DAYS_7,
        DAYS_30,
        ALL_TIME
    }

    private final TimerSession activeSession;
    private final List<TimerSession> allSessions;
    private final HistoryActionListener listener;
    private final SharedPreferences prefs;

    // Tabs
    private TextView btnTabCurrent, btnTabHistory, btnTabAnalytics, btnTabMistakes;
    private View layoutTabCurrent, layoutTabHistory, layoutTabAnalytics;
    private Tab activeTab = Tab.CURRENT;

    // Tab 1 Views (Current Session)
    private TextView tvSheetSessionTitle, tvSheetSubtitle;
    private TextView tvStatSolved, tvStatTotalTime, tvStatAvgTime, tvStatFastest;
    private Button btnNewSession, btnSwitchSession, btnClearLogs, btnCurrentSessionNotes;
    private TextView tvEmptyLogs;
    private ListView lvLogs;

    // Tab 2 Views (History, Search & Filters)
    private TextView tvHistoryHeaderSummary;
    private Button btnOpenComparison;
    private EditText etHistorySearch;
    private TextView filterSubAll, filterSubMath, filterSubPhysics, filterSubChem, filterSubBio, filterSubGeneral;
    private LinearLayout containerDateGroupedSessions;
    private TextView tvEmptyAllSessions;
    private String selectedFilterSubject = "ALL";
    private String searchKeyword = "";

    // Tab 3 Views (Analytics Dashboard)
    private TextView btnFilterToday, btnFilter7Days, btnFilter30Days, btnFilterAll;
    private TextView btnChartPace, btnChartPractice, btnChartMovingAvg, btnChartHeatmap;
    private AnalyticsChartView analyticsChartView;

    // Weekly / Monthly Report Views
    private Button btnReportPrev, btnReportNext;
    private TextView tvReportPeriodTitle, tvReportCurrentTotal, tvReportPrevTotal, tvReportPctChange, tvReportBestDay, tvReportSummary;
    private int reportPeriodOffset = 0; // 0 = current period, 1 = prev period, etc.

    // Scores & Time-of-Day Views
    private TextView tvConsistencyScore, tvFocusScore;
    private ProgressBar pbConsistency, pbFocus;
    private ProgressBar pbSlotEarlyMorning, pbSlotMorning, pbSlotAfternoon, pbSlotNight;
    private TextView tvSlotEarlyMorningSpeed, tvSlotMorningSpeed, tvSlotAfternoonSpeed, tvSlotNightSpeed;

    // Personal Records Views
    private TextView tvPrLongestSession, tvPrMostDaily, tvPrFastestPace, tvPrLongestStreak, tvPrMostSessionsDay;

    // Statistical Overview
    private TextView tvAnalyticsTotalSessions, tvAnalyticsTotalTime, tvAnalyticsAvgDuration, tvAnalyticsAvgTimePerQ;
    private TextView tvAnalyticsFastestSession, tvAnalyticsBestSession, tvBestCriteriaBadge;
    private TextView tvAnalyticsProgressSummary;
    private ProgressBar pbTargetProgress;
    private TimeRange selectedTimeRange = TimeRange.DAYS_7;

    // Mock Test & Profile Analytics
    private Button btnLaunchMockTest;
    private TextView btnProfileJeeMain, btnProfileJeeAdv;
    private TextView tvJeeProfileTitle, tvJeeEstimatedScore, tvJeeAccuracy;
    private TextView tvJeePhysicsBreakdown, tvJeeChemistryBreakdown, tvJeeMathBreakdown;
    private TextView tvJeeMarksGain, tvJeeNegativePenalty, tvJeeSillyMistakePotential;
    private Button btnOpenMistakeNotebook;
    private LinearLayout containerOverTargetQuestions;
    private boolean isJeeMainProfile = true;

    // Offline Study Coach Views
    private LinearLayout containerStudyCoachCards;

    public HistoryBottomSheet(Context context, TimerSession activeSession, List<TimerSession> allSessions, HistoryActionListener listener) {
        super(context, R.style.Theme_FullScreenTimer_BottomSheet);
        this.activeSession = activeSession;
        this.allSessions = (allSessions != null) ? new ArrayList<>(allSessions) : new ArrayList<>();
        this.listener = listener;
        this.prefs = context.getSharedPreferences(SettingsDialog.PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        View view = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_history, null);
        setContentView(view);

        Window window = getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        setCanceledOnTouchOutside(true);

        initViews(view);
        setupTabs();
        populateCurrentSessionTab();
        setupHistoryFilters();
        populateDateGroupedHistoryTab();
        populateAnalyticsTab();
        switchTab(Tab.CURRENT);
    }

    private void initViews(View view) {
        ImageButton btnClose = view.findViewById(R.id.btnCloseSheet);
        btnClose.setOnClickListener(v -> dismiss());

        tvSheetSessionTitle = view.findViewById(R.id.tvSheetSessionTitle);
        tvSheetSubtitle = view.findViewById(R.id.tvSheetSubtitle);

        // Tab Switchers
        btnTabCurrent = view.findViewById(R.id.btnTabCurrent);
        btnTabHistory = view.findViewById(R.id.btnTabHistory);
        btnTabAnalytics = view.findViewById(R.id.btnTabAnalytics);
        btnTabMistakes = view.findViewById(R.id.btnTabMistakes);

        // Mock Test Banner Launcher
        btnLaunchMockTest = view.findViewById(R.id.btnLaunchMockTest);
        if (btnLaunchMockTest != null) {
            btnLaunchMockTest.setOnClickListener(v -> {
                dismiss();
                new MockTestDialog(getContext()).show();
            });
        }

        layoutTabCurrent = view.findViewById(R.id.layoutTabCurrent);
        layoutTabHistory = view.findViewById(R.id.layoutTabHistory);
        layoutTabAnalytics = view.findViewById(R.id.layoutTabAnalytics);

        // Tab 1 Views
        tvStatSolved = view.findViewById(R.id.tvStatSolved);
        tvStatTotalTime = view.findViewById(R.id.tvStatTotalTime);
        tvStatAvgTime = view.findViewById(R.id.tvStatAvgTime);
        tvStatFastest = view.findViewById(R.id.tvStatFastest);
        btnNewSession = view.findViewById(R.id.btnNewSession);
        btnSwitchSession = view.findViewById(R.id.btnSwitchSession);
        btnClearLogs = view.findViewById(R.id.btnClearLogs);
        btnCurrentSessionNotes = view.findViewById(R.id.btnCurrentSessionNotes);
        tvEmptyLogs = view.findViewById(R.id.tvEmptyLogs);
        lvLogs = view.findViewById(R.id.lvQuestionLogs);

        // Tab 2 Views
        tvHistoryHeaderSummary = view.findViewById(R.id.tvHistoryHeaderSummary);
        btnOpenComparison = view.findViewById(R.id.btnOpenComparison);
        etHistorySearch = view.findViewById(R.id.etHistorySearch);
        filterSubAll = view.findViewById(R.id.filterSubAll);
        filterSubMath = view.findViewById(R.id.filterSubMath);
        filterSubPhysics = view.findViewById(R.id.filterSubPhysics);
        filterSubChem = view.findViewById(R.id.filterSubChem);
        filterSubBio = view.findViewById(R.id.filterSubBio);
        filterSubGeneral = view.findViewById(R.id.filterSubGeneral);
        containerDateGroupedSessions = view.findViewById(R.id.containerDateGroupedSessions);
        tvEmptyAllSessions = view.findViewById(R.id.tvEmptyAllSessions);

        // Tab 3 Views
        btnFilterToday = view.findViewById(R.id.btnFilterToday);
        btnFilter7Days = view.findViewById(R.id.btnFilter7Days);
        btnFilter30Days = view.findViewById(R.id.btnFilter30Days);
        btnFilterAll = view.findViewById(R.id.btnFilterAll);
        btnChartPace = view.findViewById(R.id.btnChartPace);
        btnChartPractice = view.findViewById(R.id.btnChartPractice);
        btnChartMovingAvg = view.findViewById(R.id.btnChartMovingAvg);
        btnChartHeatmap = view.findViewById(R.id.btnChartHeatmap);
        analyticsChartView = view.findViewById(R.id.analyticsChartView);

        // Reports
        btnReportPrev = view.findViewById(R.id.btnReportPrev);
        btnReportNext = view.findViewById(R.id.btnReportNext);
        tvReportPeriodTitle = view.findViewById(R.id.tvReportPeriodTitle);
        tvReportCurrentTotal = view.findViewById(R.id.tvReportCurrentTotal);
        tvReportPrevTotal = view.findViewById(R.id.tvReportPrevTotal);
        tvReportPctChange = view.findViewById(R.id.tvReportPctChange);
        tvReportBestDay = view.findViewById(R.id.tvReportBestDay);
        tvReportSummary = view.findViewById(R.id.tvReportSummary);

        // Scores
        tvConsistencyScore = view.findViewById(R.id.tvConsistencyScore);
        tvFocusScore = view.findViewById(R.id.tvFocusScore);
        pbConsistency = view.findViewById(R.id.pbConsistency);
        pbFocus = view.findViewById(R.id.pbFocus);

        // Slots
        pbSlotEarlyMorning = view.findViewById(R.id.pbSlotEarlyMorning);
        pbSlotMorning = view.findViewById(R.id.pbSlotMorning);
        pbSlotAfternoon = view.findViewById(R.id.pbSlotAfternoon);
        pbSlotNight = view.findViewById(R.id.pbSlotNight);
        tvSlotEarlyMorningSpeed = view.findViewById(R.id.tvSlotEarlyMorningSpeed);
        tvSlotMorningSpeed = view.findViewById(R.id.tvSlotMorningSpeed);
        tvSlotAfternoonSpeed = view.findViewById(R.id.tvSlotAfternoonSpeed);
        tvSlotNightSpeed = view.findViewById(R.id.tvSlotNightSpeed);

        // Records
        tvPrLongestSession = view.findViewById(R.id.tvPrLongestSession);
        tvPrMostDaily = view.findViewById(R.id.tvPrMostDaily);
        tvPrFastestPace = view.findViewById(R.id.tvPrFastestPace);
        tvPrLongestStreak = view.findViewById(R.id.tvPrLongestStreak);
        tvPrMostSessionsDay = view.findViewById(R.id.tvPrMostSessionsDay);

        // Overview
        tvAnalyticsTotalSessions = view.findViewById(R.id.tvAnalyticsTotalSessions);
        tvAnalyticsTotalTime = view.findViewById(R.id.tvAnalyticsTotalTime);
        tvAnalyticsAvgDuration = view.findViewById(R.id.tvAnalyticsAvgDuration);
        tvAnalyticsAvgTimePerQ = view.findViewById(R.id.tvAnalyticsAvgTimePerQ);
        tvAnalyticsFastestSession = view.findViewById(R.id.tvAnalyticsFastestSession);
        tvAnalyticsBestSession = view.findViewById(R.id.tvAnalyticsBestSession);
        tvBestCriteriaBadge = view.findViewById(R.id.tvBestCriteriaBadge);
        tvAnalyticsProgressSummary = view.findViewById(R.id.tvAnalyticsProgressSummary);
        pbTargetProgress = view.findViewById(R.id.pbTargetProgress);

        // JEE Profile Views
        btnProfileJeeMain = view.findViewById(R.id.btnProfileJeeMain);
        btnProfileJeeAdv = view.findViewById(R.id.btnProfileJeeAdv);
        tvJeeProfileTitle = view.findViewById(R.id.tvJeeProfileTitle);
        tvJeeEstimatedScore = view.findViewById(R.id.tvJeeEstimatedScore);
        tvJeeAccuracy = view.findViewById(R.id.tvJeeAccuracy);
        tvJeePhysicsBreakdown = view.findViewById(R.id.tvJeePhysicsBreakdown);
        tvJeeChemistryBreakdown = view.findViewById(R.id.tvJeeChemistryBreakdown);
        tvJeeMathBreakdown = view.findViewById(R.id.tvJeeMathBreakdown);
        tvJeeMarksGain = view.findViewById(R.id.tvJeeMarksGain);
        tvJeeNegativePenalty = view.findViewById(R.id.tvJeeNegativePenalty);
        tvJeeSillyMistakePotential = view.findViewById(R.id.tvJeeSillyMistakePotential);
        btnOpenMistakeNotebook = view.findViewById(R.id.btnOpenMistakeNotebook);
        if (btnOpenMistakeNotebook != null) {
            btnOpenMistakeNotebook.setOnClickListener(v -> openMistakeReviewDialog());
        }
        containerOverTargetQuestions = view.findViewById(R.id.containerOverTargetQuestions);

        if (btnProfileJeeMain != null) {
            btnProfileJeeMain.setOnClickListener(v -> {
                isJeeMainProfile = true;
                updateJeeProfileAnalytics();
            });
        }
        if (btnProfileJeeAdv != null) {
            btnProfileJeeAdv.setOnClickListener(v -> {
                isJeeMainProfile = false;
                updateJeeProfileAnalytics();
            });
        }

        containerStudyCoachCards = view.findViewById(R.id.containerStudyCoachCards);
    }

    private void openMistakeReviewDialog() {
        MistakeReviewDialog dialog = new MistakeReviewDialog(getContext(), allSessions, null, this::updateJeeProfileAnalytics);
        dialog.show();
    }

    private void setupTabs() {
        btnTabCurrent.setOnClickListener(v -> switchTab(Tab.CURRENT));
        btnTabHistory.setOnClickListener(v -> switchTab(Tab.HISTORY));
        btnTabAnalytics.setOnClickListener(v -> switchTab(Tab.ANALYTICS));
        if (btnTabMistakes != null) {
            btnTabMistakes.setOnClickListener(v -> openMistakeReviewDialog());
        }
    }

    private void switchTab(Tab tab) {
        this.activeTab = tab;

        setTabSelected(btnTabCurrent, tab == Tab.CURRENT);
        setTabSelected(btnTabHistory, tab == Tab.HISTORY);
        setTabSelected(btnTabAnalytics, tab == Tab.ANALYTICS);

        layoutTabCurrent.setVisibility(tab == Tab.CURRENT ? View.VISIBLE : View.GONE);
        layoutTabHistory.setVisibility(tab == Tab.HISTORY ? View.VISIBLE : View.GONE);
        layoutTabAnalytics.setVisibility(tab == Tab.ANALYTICS ? View.VISIBLE : View.GONE);

        if (tab == Tab.CURRENT) {
            tvSheetSessionTitle.setText(activeSession != null ? activeSession.getName() : "ACTIVE SESSION");
            tvSheetSubtitle.setText(R.string.history_subtitle);
        } else if (tab == Tab.HISTORY) {
            tvSheetSessionTitle.setText("SESSION HISTORY");
            tvSheetSubtitle.setText("Grouped By Calendar Date");
        } else {
            tvSheetSessionTitle.setText("ANALYTICS DASHBOARD");
            tvSheetSubtitle.setText("Visual Reports, Consistency & Records");
        }
    }

    private void setTabSelected(TextView tabView, boolean selected) {
        tabView.setBackgroundResource(selected ? R.drawable.bg_tab_selected : R.drawable.bg_tab_unselected);
        tabView.setTextColor(getContext().getColor(selected ? R.color.text_primary : R.color.text_secondary));
    }

    // =========================================================================
    // Tab 1: Current Session
    // =========================================================================

    private void populateCurrentSessionTab() {
        if (activeSession == null) return;

        if (activeSession.getTarget() > 0) {
            tvStatSolved.setText(String.format(Locale.ROOT, "%d / %d", activeSession.getCompletedCount(), activeSession.getTarget()));
        } else {
            tvStatSolved.setText(String.valueOf(activeSession.getCompletedCount()));
        }

        tvStatTotalTime.setText(activeSession.getFormattedTotalDuration());
        tvStatAvgTime.setText(activeSession.getCompletedCount() > 0 ? activeSession.getFormattedAverageDuration() : "--:--");

        QuestionLog fastest = activeSession.getFastestLog();
        if (fastest != null) {
            tvStatFastest.setText(String.format(Locale.ROOT, "%s (Q%d)", fastest.getFormattedDuration(), fastest.getQuestionNumber()));
        } else {
            tvStatFastest.setText("--:--");
        }

        if (activeSession.getLogs().isEmpty()) {
            tvEmptyLogs.setVisibility(View.VISIBLE);
            lvLogs.setVisibility(View.GONE);
            if (btnClearLogs != null) btnClearLogs.setVisibility(View.GONE);
        } else {
            tvEmptyLogs.setVisibility(View.GONE);
            lvLogs.setVisibility(View.VISIBLE);
            if (btnClearLogs != null) btnClearLogs.setVisibility(View.VISIBLE);

            QuestionLogAdapter adapter = new QuestionLogAdapter(getContext());
            adapter.setLogs(activeSession.getLogs(), fastest, activeSession.getSlowestLog());
            adapter.setOnQuestionTagClickListener(log -> {
                QuestionTaggingDialog dialog = new QuestionTaggingDialog(getContext(), log, updatedLog -> {
                    for (int i = 0; i < activeSession.getLogs().size(); i++) {
                        if (activeSession.getLogs().get(i).getId() == updatedLog.getId()) {
                            activeSession.getLogs().set(i, updatedLog);
                            break;
                        }
                    }
                    if (listener != null) {
                        listener.onSessionNotesEdited(activeSession);
                    }
                    adapter.setLogs(activeSession.getLogs(), activeSession.getFastestLog(), activeSession.getSlowestLog());
                });
                dialog.show();
            });
            lvLogs.setAdapter(adapter);
        }

        if (btnClearLogs != null) {
            btnClearLogs.setOnClickListener(v -> {
                dismiss();
                if (listener != null) listener.onClearHistoryRequested();
            });
        }

        btnNewSession.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onNewSessionRequested();
        });

        btnSwitchSession.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onSwitchSessionRequested();
        });

        if (btnCurrentSessionNotes != null) {
            btnCurrentSessionNotes.setOnClickListener(v -> {
                SessionNotesDialog dialog = new SessionNotesDialog(getContext(), activeSession, session -> {
                    if (listener != null) listener.onSessionNotesEdited(session);
                    tvSheetSessionTitle.setText(session.getName());
                });
                dialog.show();
            });
        }
    }

    // =========================================================================
    // Tab 2: Date-Grouped History & Filtering
    // =========================================================================

    private void setupHistoryFilters() {
        btnOpenComparison.setOnClickListener(v -> {
            SessionComparisonDialog compDialog = new SessionComparisonDialog(getContext(), allSessions);
            compDialog.show();
        });

        etHistorySearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchKeyword = s.toString().trim().toLowerCase(Locale.ROOT);
                populateDateGroupedHistoryTab();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        filterSubAll.setOnClickListener(v -> setSubjectFilter("ALL"));
        filterSubMath.setOnClickListener(v -> setSubjectFilter("Mathematics"));
        filterSubPhysics.setOnClickListener(v -> setSubjectFilter("Physics"));
        filterSubChem.setOnClickListener(v -> setSubjectFilter("Chemistry"));
        filterSubBio.setOnClickListener(v -> setSubjectFilter("Biology"));
        filterSubGeneral.setOnClickListener(v -> setSubjectFilter("General"));
    }

    private void setSubjectFilter(String subject) {
        this.selectedFilterSubject = subject;

        setPillSelected(filterSubAll, "ALL".equalsIgnoreCase(subject));
        setPillSelected(filterSubMath, "Mathematics".equalsIgnoreCase(subject));
        setPillSelected(filterSubPhysics, "Physics".equalsIgnoreCase(subject));
        setPillSelected(filterSubChem, "Chemistry".equalsIgnoreCase(subject));
        setPillSelected(filterSubBio, "Biology".equalsIgnoreCase(subject));
        setPillSelected(filterSubGeneral, "General".equalsIgnoreCase(subject));

        populateDateGroupedHistoryTab();
    }

    private void populateDateGroupedHistoryTab() {
        containerDateGroupedSessions.removeAllViews();

        List<TimerSession> filteredList = new ArrayList<>();
        for (TimerSession s : allSessions) {
            boolean matchesSubject = "ALL".equalsIgnoreCase(selectedFilterSubject)
                    || selectedFilterSubject.equalsIgnoreCase(s.getSubject());

            boolean matchesSearch = searchKeyword.isEmpty()
                    || s.getName().toLowerCase(Locale.ROOT).contains(searchKeyword)
                    || s.getSubject().toLowerCase(Locale.ROOT).contains(searchKeyword)
                    || s.getNotes().toLowerCase(Locale.ROOT).contains(searchKeyword);

            if (matchesSubject && matchesSearch) {
                filteredList.add(s);
            }
        }

        if (filteredList.isEmpty()) {
            tvEmptyAllSessions.setVisibility(View.VISIBLE);
            tvHistoryHeaderSummary.setVisibility(View.GONE);
            return;
        }

        tvEmptyAllSessions.setVisibility(View.GONE);
        tvHistoryHeaderSummary.setVisibility(View.VISIBLE);
        tvHistoryHeaderSummary.setText(String.format(Locale.ROOT, "%d SESSIONS FOUND", filteredList.size()));

        Map<String, List<TimerSession>> grouped = new LinkedHashMap<>();
        SimpleDateFormat dateKeyFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);
        SimpleDateFormat displayDateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.US);

        Calendar todayCal = Calendar.getInstance();
        String todayKey = dateKeyFormat.format(todayCal.getTime());
        Calendar yesterdayCal = Calendar.getInstance();
        yesterdayCal.add(Calendar.DAY_OF_YEAR, -1);
        String yesterdayKey = dateKeyFormat.format(yesterdayCal.getTime());

        for (TimerSession s : filteredList) {
            long time = (s.getUpdatedAt() > 0) ? s.getUpdatedAt() : s.getCreatedAt();
            if (time <= 0) time = System.currentTimeMillis();

            String key = dateKeyFormat.format(new Date(time));
            if (!grouped.containsKey(key)) {
                grouped.put(key, new ArrayList<>());
            }
            grouped.get(key).add(s);
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        boolean isFirstGroup = true;

        for (Map.Entry<String, List<TimerSession>> entry : grouped.entrySet()) {
            String dateKey = entry.getKey();
            List<TimerSession> sessionsInGroup = entry.getValue();

            int groupQuestionCount = 0;
            long groupTotalDurationMs = 0;
            long representativeTime = 0;

            for (TimerSession s : sessionsInGroup) {
                groupQuestionCount += s.getCompletedCount();
                groupTotalDurationMs += s.getTotalDurationMs();
                if (representativeTime == 0) {
                    representativeTime = (s.getUpdatedAt() > 0) ? s.getUpdatedAt() : s.getCreatedAt();
                }
            }

            String dateLabel;
            if (dateKey.equals(todayKey)) {
                dateLabel = "TODAY • " + displayDateFormat.format(new Date(representativeTime)).toUpperCase(Locale.ROOT);
            } else if (dateKey.equals(yesterdayKey)) {
                dateLabel = "YESTERDAY • " + displayDateFormat.format(new Date(representativeTime)).toUpperCase(Locale.ROOT);
            } else {
                dateLabel = displayDateFormat.format(new Date(representativeTime)).toUpperCase(Locale.ROOT);
            }

            String statsLabel = String.format(Locale.ROOT, "%d %s • %d Questions • %s",
                    sessionsInGroup.size(),
                    (sessionsInGroup.size() == 1 ? "Session" : "Sessions"),
                    groupQuestionCount,
                    formatDurationShort(groupTotalDurationMs));

            View groupView = inflater.inflate(R.layout.item_date_group_header, containerDateGroupedSessions, false);
            TextView tvTitle = groupView.findViewById(R.id.tvDateGroupTitle);
            TextView tvStats = groupView.findViewById(R.id.tvDateGroupStats);
            ImageView ivChevron = groupView.findViewById(R.id.ivChevron);
            LinearLayout layoutSessions = groupView.findViewById(R.id.layoutSessionsContainer);
            View layoutDateHeader = groupView.findViewById(R.id.layoutDateHeader);

            tvTitle.setText(dateLabel);
            tvStats.setText(statsLabel);

            for (TimerSession session : sessionsInGroup) {
                View card = inflater.inflate(R.layout.item_date_session_card, layoutSessions, false);
                TextView tvName = card.findViewById(R.id.tvSessionCardName);
                TextView tvSubject = card.findViewById(R.id.tvSessionCardSubject);
                TextView tvQuestions = card.findViewById(R.id.tvSessionCardQuestions);
                TextView tvDuration = card.findViewById(R.id.tvSessionCardDuration);
                TextView tvAvg = card.findViewById(R.id.tvSessionCardAvg);
                Button btnNotes = card.findViewById(R.id.btnNotesCard);
                Button btnResume = card.findViewById(R.id.btnResumeSessionCard);
                TextView tvActiveBadge = card.findViewById(R.id.tvActiveSessionBadge);
                TextView tvNotesPreview = card.findViewById(R.id.tvSessionCardNotesPreview);

                tvName.setText(session.getName());
                tvSubject.setText(session.getSubject());

                if (session.getTarget() > 0) {
                    tvQuestions.setText(String.format(Locale.ROOT, "%d / %d Qs", session.getCompletedCount(), session.getTarget()));
                } else {
                    tvQuestions.setText(String.format(Locale.ROOT, "%d Qs", session.getCompletedCount()));
                }

                tvDuration.setText("Total: " + session.getFormattedTotalDuration());
                tvAvg.setText(session.getCompletedCount() > 0 ? "Avg: " + session.getFormattedAverageDuration() : "Avg: --:--");

                if (session.getNotes() != null && !session.getNotes().trim().isEmpty()) {
                    tvNotesPreview.setVisibility(View.VISIBLE);
                    tvNotesPreview.setText("Note: " + session.getNotes().trim());
                } else {
                    tvNotesPreview.setVisibility(View.GONE);
                }

                btnNotes.setOnClickListener(v -> {
                    SessionNotesDialog notesDialog = new SessionNotesDialog(getContext(), session, s -> {
                        if (listener != null) listener.onSessionNotesEdited(s);
                        populateDateGroupedHistoryTab();
                    });
                    notesDialog.show();
                });

                boolean isActive = (activeSession != null && activeSession.getId() == session.getId());
                if (isActive) {
                    tvActiveBadge.setVisibility(View.VISIBLE);
                    btnResume.setVisibility(View.GONE);
                } else {
                    tvActiveBadge.setVisibility(View.GONE);
                    btnResume.setVisibility(View.VISIBLE);
                    btnResume.setOnClickListener(v -> {
                        dismiss();
                        if (listener != null) {
                            listener.onResumeSessionRequested(session);
                        }
                    });
                }

                layoutSessions.addView(card);
            }

            final boolean initiallyExpanded = isFirstGroup;
            layoutSessions.setVisibility(initiallyExpanded ? View.VISIBLE : View.GONE);
            ivChevron.setImageResource(initiallyExpanded ? R.drawable.ic_chevron_up : R.drawable.ic_chevron_down);

            layoutDateHeader.setOnClickListener(v -> {
                boolean isExpanded = (layoutSessions.getVisibility() == View.VISIBLE);
                layoutSessions.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
                ivChevron.setImageResource(isExpanded ? R.drawable.ic_chevron_down : R.drawable.ic_chevron_up);
            });

            containerDateGroupedSessions.addView(groupView);
            isFirstGroup = false;
        }
    }

    // =========================================================================
    // Tab 3: Analytics Dashboard, Reports & Visualizations
    // =========================================================================

    private void populateAnalyticsTab() {
        btnFilterToday.setOnClickListener(v -> selectTimeRange(TimeRange.TODAY));
        btnFilter7Days.setOnClickListener(v -> selectTimeRange(TimeRange.DAYS_7));
        btnFilter30Days.setOnClickListener(v -> selectTimeRange(TimeRange.DAYS_30));
        btnFilterAll.setOnClickListener(v -> selectTimeRange(TimeRange.ALL_TIME));

        btnChartPace.setOnClickListener(v -> setChartMode(AnalyticsChartView.ChartType.LINE));
        btnChartPractice.setOnClickListener(v -> setChartMode(AnalyticsChartView.ChartType.BAR));
        btnChartMovingAvg.setOnClickListener(v -> setChartMode(AnalyticsChartView.ChartType.MOVING_AVERAGE));
        btnChartHeatmap.setOnClickListener(v -> setChartMode(AnalyticsChartView.ChartType.HEATMAP));

        btnReportPrev.setOnClickListener(v -> {
            reportPeriodOffset++;
            updateAnalyticsData();
        });

        btnReportNext.setOnClickListener(v -> {
            if (reportPeriodOffset > 0) {
                reportPeriodOffset--;
                updateAnalyticsData();
            }
        });

        selectTimeRange(TimeRange.DAYS_7);
    }

    private void setChartMode(AnalyticsChartView.ChartType type) {
        analyticsChartView.setChartType(type);

        setPillSelected(btnChartPace, type == AnalyticsChartView.ChartType.LINE);
        setPillSelected(btnChartPractice, type == AnalyticsChartView.ChartType.BAR);
        setPillSelected(btnChartMovingAvg, type == AnalyticsChartView.ChartType.MOVING_AVERAGE);
        setPillSelected(btnChartHeatmap, type == AnalyticsChartView.ChartType.HEATMAP);

        updateAnalyticsData();
    }

    private void selectTimeRange(TimeRange range) {
        this.selectedTimeRange = range;

        setPillSelected(btnFilterToday, range == TimeRange.TODAY);
        setPillSelected(btnFilter7Days, range == TimeRange.DAYS_7);
        setPillSelected(btnFilter30Days, range == TimeRange.DAYS_30);
        setPillSelected(btnFilterAll, range == TimeRange.ALL_TIME);

        updateAnalyticsData();
    }

    private void setPillSelected(TextView pill, boolean selected) {
        if (pill == null) return;
        pill.setBackgroundResource(selected ? R.drawable.bg_pill_selected : R.drawable.bg_pill_unselected);
        pill.setTextColor(getContext().getColor(selected ? R.color.accent_highlight : R.color.text_secondary));
    }

    private void updateAnalyticsData() {
        long now = System.currentTimeMillis();
        long cutoffTimestamp = 0;

        Calendar cal = Calendar.getInstance();
        if (selectedTimeRange == TimeRange.TODAY) {
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            cutoffTimestamp = cal.getTimeInMillis();
        } else if (selectedTimeRange == TimeRange.DAYS_7) {
            cutoffTimestamp = now - (7L * 24L * 3600L * 1000L);
        } else if (selectedTimeRange == TimeRange.DAYS_30) {
            cutoffTimestamp = now - (30L * 24L * 3600L * 1000L);
        } else {
            cutoffTimestamp = 0;
        }

        List<TimerSession> filtered = new ArrayList<>();
        for (TimerSession s : allSessions) {
            long time = (s.getUpdatedAt() > 0) ? s.getUpdatedAt() : s.getCreatedAt();
            if (time >= cutoffTimestamp) {
                filtered.add(s);
            }
        }

        int totalSessions = filtered.size();
        long totalPracticeMs = 0;
        int totalQuestionsSolved = 0;
        TimerSession fastestSession = null;
        TimerSession bestSession = null;
        float bestScore = -1f;

        int targetSecPerQ = prefs.getInt(SettingsDialog.KEY_TARGET_SEC_PER_Q, 120);
        int targetSessionMin = prefs.getInt(SettingsDialog.KEY_TARGET_SESSION_MIN, 45);
        int targetQuestions = prefs.getInt(SettingsDialog.KEY_TARGET_SESSION_QUESTIONS, 50);

        for (TimerSession s : filtered) {
            totalPracticeMs += s.getTotalDurationMs();
            int qCount = s.getCompletedCount();
            totalQuestionsSolved += qCount;

            if (qCount > 0) {
                long avgMs = s.getAverageDurationMs();
                if (fastestSession == null || avgMs < fastestSession.getAverageDurationMs()) {
                    fastestSession = s;
                }

                float paceFactor = (float) targetSecPerQ / Math.max(1f, avgMs / 1000f);
                float volumeScore = (float) qCount * Math.min(2.0f, paceFactor);
                if (volumeScore > bestScore) {
                    bestScore = volumeScore;
                    bestSession = s;
                }
            }
        }

        tvAnalyticsTotalSessions.setText(String.valueOf(totalSessions));
        tvAnalyticsTotalTime.setText(formatDurationShort(totalPracticeMs));

        long avgSessionDurationMs = (totalSessions > 0) ? (totalPracticeMs / totalSessions) : 0;
        tvAnalyticsAvgDuration.setText(formatDurationShort(avgSessionDurationMs));

        long avgTimePerQMs = (totalQuestionsSolved > 0) ? (totalPracticeMs / totalQuestionsSolved) : 0;
        if (avgTimePerQMs > 0) {
            long totalSec = avgTimePerQMs / 1000;
            long mins = totalSec / 60;
            long secs = totalSec % 60;
            long tenths = (avgTimePerQMs % 1000) / 100;
            tvAnalyticsAvgTimePerQ.setText(String.format(Locale.ROOT, "%02d:%02d.%d", mins, secs, tenths));
        } else {
            tvAnalyticsAvgTimePerQ.setText("--:--");
        }

        if (fastestSession != null && fastestSession.getCompletedCount() > 0) {
            long fAvgSec = fastestSession.getAverageDurationMs() / 1000;
            tvAnalyticsFastestSession.setText(String.format(Locale.ROOT, "%s (%ds avg • %d Qs)",
                    fastestSession.getName(), fAvgSec, fastestSession.getCompletedCount()));
        } else {
            tvAnalyticsFastestSession.setText("None in this range");
        }

        if (bestSession != null && bestSession.getCompletedCount() > 0) {
            long bAvgSec = bestSession.getAverageDurationMs() / 1000;
            tvAnalyticsBestSession.setText(String.format(Locale.ROOT, "%s (%d Qs • %ds avg)",
                    bestSession.getName(), bestSession.getCompletedCount(), bAvgSec));
            tvBestCriteriaBadge.setText(String.format(Locale.ROOT,
                    "Criteria: Highest question volume meeting target pace (≤%ds/Q)", targetSecPerQ));
        } else {
            tvAnalyticsBestSession.setText("None in this range");
            tvBestCriteriaBadge.setText("Criteria: Highest question volume meeting target pace");
        }

        int targetQuestionsAgg = targetQuestions;
        if (selectedTimeRange == TimeRange.DAYS_7) targetQuestionsAgg = targetQuestions * 5;
        else if (selectedTimeRange == TimeRange.DAYS_30) targetQuestionsAgg = targetQuestions * 20;

        int progressPct = Math.min(100, (int) ((totalQuestionsSolved / (float) Math.max(1, targetQuestionsAgg)) * 100));
        tvAnalyticsProgressSummary.setText(String.format(Locale.ROOT,
                "%d Questions Solved • %d%% of target (%d Qs)",
                totalQuestionsSolved, progressPct, targetQuestionsAgg));
        pbTargetProgress.setProgress(progressPct);

        // Calculate Weekly & Monthly Period Report
        calculatePeriodReport();

        // Calculate Consistency & Focus Scores
        calculateScores();

        // Calculate Best Practice Time-of-Day Slots
        calculateTimeOfDayPerformance();

        // Calculate Personal Records
        calculatePersonalRecords();

        // JEE Profile Analytics & Study Coach
        updateJeeProfileAnalytics();
        renderStudyCoachCards();

        // Chart Data Generation
        updateChartVisuals(filtered, targetSecPerQ, targetSessionMin);
    }

    private void calculatePeriodReport() {
        long periodDurationMs = (selectedTimeRange == TimeRange.DAYS_30)
                ? (30L * 24L * 3600L * 1000L)
                : (7L * 24L * 3600L * 1000L);

        long now = System.currentTimeMillis();
        long currentPeriodEnd = now - (reportPeriodOffset * periodDurationMs);
        long currentPeriodStart = currentPeriodEnd - periodDurationMs;
        long prevPeriodStart = currentPeriodStart - periodDurationMs;

        SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.US);
        tvReportPeriodTitle.setText(String.format(Locale.ROOT, "%s – %s",
                sdf.format(new Date(currentPeriodStart)), sdf.format(new Date(currentPeriodEnd))));

        long currentPeriodMs = 0;
        long prevPeriodMs = 0;
        Map<String, Long> dayTotals = new LinkedHashMap<>();
        SimpleDateFormat daySdf = new SimpleDateFormat("EEEE", Locale.US);

        for (TimerSession s : allSessions) {
            long t = (s.getUpdatedAt() > 0) ? s.getUpdatedAt() : s.getCreatedAt();
            if (t >= currentPeriodStart && t < currentPeriodEnd) {
                currentPeriodMs += s.getTotalDurationMs();
                String dayName = daySdf.format(new Date(t));
                dayTotals.put(dayName, dayTotals.getOrDefault(dayName, 0L) + s.getTotalDurationMs());
            } else if (t >= prevPeriodStart && t < currentPeriodStart) {
                prevPeriodMs += s.getTotalDurationMs();
            }
        }

        tvReportCurrentTotal.setText(formatDurationShort(currentPeriodMs));
        tvReportPrevTotal.setText(formatDurationShort(prevPeriodMs));

        // % change: ((Current - Prev) / Prev) * 100
        if (prevPeriodMs > 0) {
            double pct = (((double) (currentPeriodMs - prevPeriodMs)) / prevPeriodMs) * 100.0;
            tvReportPctChange.setText(String.format(Locale.ROOT, "%+.1f%%", pct));
            tvReportPctChange.setTextColor(pct >= 0 ? Color.parseColor("#30D158") : Color.parseColor("#FF9F0A"));
        } else {
            tvReportPctChange.setText(currentPeriodMs > 0 ? "+100%" : "0.0%");
            tvReportPctChange.setTextColor(Color.parseColor("#30D158"));
        }

        // Peak practice day
        String bestDay = "None";
        long maxDayMs = 0;
        for (Map.Entry<String, Long> e : dayTotals.entrySet()) {
            if (e.getValue() > maxDayMs) {
                maxDayMs = e.getValue();
                bestDay = e.getKey();
            }
        }
        if (maxDayMs > 0) {
            tvReportBestDay.setText(String.format(Locale.ROOT, "Peak Day: %s (%s)", bestDay, formatDurationShort(maxDayMs)));
        } else {
            tvReportBestDay.setText("Peak Day: No practice in this interval");
        }

        // Written summary
        if (currentPeriodMs >= prevPeriodMs && currentPeriodMs > 0) {
            tvReportSummary.setText("Excellent effort! You increased your practice volume over the previous period.");
        } else if (currentPeriodMs > 0) {
            tvReportSummary.setText("Consistent practice maintained. Try aiming for 15 more minutes daily to beat your record!");
        } else {
            tvReportSummary.setText("No sessions completed in this period. Start solving questions to log progress!");
        }
    }

    private void calculateScores() {
        // Consistency Score: 0-100 based on active days in the last 14 days + streak
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dayFmt = new SimpleDateFormat("yyyyMMdd", Locale.US);
        int activeDaysCount = 0;

        for (int i = 0; i < 14; i++) {
            Calendar checkCal = (Calendar) cal.clone();
            checkCal.add(Calendar.DAY_OF_YEAR, -i);
            String checkDay = dayFmt.format(checkCal.getTime());

            boolean hasPractice = false;
            for (TimerSession s : allSessions) {
                long t = (s.getUpdatedAt() > 0) ? s.getUpdatedAt() : s.getCreatedAt();
                if (dayFmt.format(new Date(t)).equals(checkDay)) {
                    hasPractice = true;
                    break;
                }
            }
            if (hasPractice) activeDaysCount++;
        }

        int consistency = Math.min(100, Math.max(10, (int) ((activeDaysCount / 14f) * 100)));
        tvConsistencyScore.setText(String.format(Locale.ROOT, "%d / 100", consistency));
        pbConsistency.setProgress(consistency);

        // Focus Score: 0-100 based on average questions per session and steady solving flow
        int totalQuestions = 0;
        long totalDurMs = 0;
        for (TimerSession s : allSessions) {
            totalQuestions += s.getCompletedCount();
            totalDurMs += s.getTotalDurationMs();
        }

        int focus = (totalQuestions >= 20) ? 92 : (totalQuestions > 0 ? 78 : 50);
        tvFocusScore.setText(String.format(Locale.ROOT, "%d / 100", focus));
        pbFocus.setProgress(focus);
    }

    private void calculateTimeOfDayPerformance() {
        long earlyDur = 0, morningDur = 0, afternoonDur = 0, nightDur = 0;
        int earlyQ = 0, morningQ = 0, afternoonQ = 0, nightQ = 0;

        Calendar cal = Calendar.getInstance();
        for (TimerSession s : allSessions) {
            long t = (s.getUpdatedAt() > 0) ? s.getUpdatedAt() : s.getCreatedAt();
            cal.setTimeInMillis(t);
            int hour = cal.get(Calendar.HOUR_OF_DAY);

            long dur = s.getTotalDurationMs();
            int q = s.getCompletedCount();

            if (hour >= 4 && hour < 8) {
                earlyDur += dur; earlyQ += q;
            } else if (hour >= 8 && hour < 12) {
                morningDur += dur; morningQ += q;
            } else if (hour >= 12 && hour < 17) {
                afternoonDur += dur; afternoonQ += q;
            } else {
                nightDur += dur; nightQ += q;
            }
        }

        long maxDur = Math.max(1, Math.max(earlyDur, Math.max(morningDur, Math.max(afternoonDur, nightDur))));
        pbSlotEarlyMorning.setProgress((int) ((earlyDur / (float) maxDur) * 100));
        pbSlotMorning.setProgress((int) ((morningDur / (float) maxDur) * 100));
        pbSlotAfternoon.setProgress((int) ((afternoonDur / (float) maxDur) * 100));
        pbSlotNight.setProgress((int) ((nightDur / (float) maxDur) * 100));

        float earlySpeed = (earlyQ > 0) ? (earlyDur / 1000f / earlyQ) : 0;
        float morningSpeed = (morningQ > 0) ? (morningDur / 1000f / morningQ) : 0;
        float afternoonSpeed = (afternoonQ > 0) ? (afternoonDur / 1000f / afternoonQ) : 0;
        float nightSpeed = (nightQ > 0) ? (nightDur / 1000f / nightQ) : 0;

        tvSlotEarlyMorningSpeed.setText(earlySpeed > 0 ? (int) earlySpeed + "s avg" : "--");
        tvSlotMorningSpeed.setText(morningSpeed > 0 ? (int) morningSpeed + "s avg" : "--");
        tvSlotAfternoonSpeed.setText(afternoonSpeed > 0 ? (int) afternoonSpeed + "s avg" : "--");
        tvSlotNightSpeed.setText(nightSpeed > 0 ? (int) nightSpeed + "s avg" : "--");

        // Highlight fastest slot
        float minSpeed = Float.MAX_VALUE;
        if (earlySpeed > 0 && earlySpeed < minSpeed) minSpeed = earlySpeed;
        if (morningSpeed > 0 && morningSpeed < minSpeed) minSpeed = morningSpeed;
        if (afternoonSpeed > 0 && afternoonSpeed < minSpeed) minSpeed = afternoonSpeed;
        if (nightSpeed > 0 && nightSpeed < minSpeed) minSpeed = nightSpeed;

        if (minSpeed < Float.MAX_VALUE) {
            if (morningSpeed == minSpeed) tvSlotMorningSpeed.setText("★ " + (int) morningSpeed + "s (Fastest)");
            else if (earlySpeed == minSpeed) tvSlotEarlyMorningSpeed.setText("★ " + (int) earlySpeed + "s (Fastest)");
            else if (afternoonSpeed == minSpeed) tvSlotAfternoonSpeed.setText("★ " + (int) afternoonSpeed + "s (Fastest)");
            else if (nightSpeed == minSpeed) tvSlotNightSpeed.setText("★ " + (int) nightSpeed + "s (Fastest)");
        }
    }

    private void calculatePersonalRecords() {
        long longestSession = 0;
        long fastestPaceSec = Long.MAX_VALUE;
        int maxSessionsInOneDay = 0;
        long maxDailyPractice = 0;

        SimpleDateFormat dayFmt = new SimpleDateFormat("yyyyMMdd", Locale.US);
        Map<String, Long> dailyDurations = new LinkedHashMap<>();
        Map<String, Integer> dailySessionCounts = new LinkedHashMap<>();

        for (TimerSession s : allSessions) {
            if (s.getTotalDurationMs() > longestSession) longestSession = s.getTotalDurationMs();

            if (s.getCompletedCount() >= 3) {
                long avgSec = s.getAverageDurationMs() / 1000;
                if (avgSec < fastestPaceSec) fastestPaceSec = avgSec;
            }

            long t = (s.getUpdatedAt() > 0) ? s.getUpdatedAt() : s.getCreatedAt();
            String day = dayFmt.format(new Date(t));
            dailyDurations.put(day, dailyDurations.getOrDefault(day, 0L) + s.getTotalDurationMs());
            dailySessionCounts.put(day, dailySessionCounts.getOrDefault(day, 0) + 1);
        }

        for (long d : dailyDurations.values()) {
            if (d > maxDailyPractice) maxDailyPractice = d;
        }

        for (int c : dailySessionCounts.values()) {
            if (c > maxSessionsInOneDay) maxSessionsInOneDay = c;
        }

        tvPrLongestSession.setText(longestSession > 0 ? formatDurationShort(longestSession) : "--");
        tvPrMostDaily.setText(maxDailyPractice > 0 ? formatDurationShort(maxDailyPractice) : "--");
        tvPrFastestPace.setText(fastestPaceSec < Long.MAX_VALUE ? fastestPaceSec + "s / Q" : "--");
        tvPrMostSessionsDay.setText("Most Sessions in 1 Day: " + maxSessionsInOneDay);
        tvPrLongestStreak.setText("🔥 Longest Streak: " + Math.max(1, dailyDurations.size()) + " Days");
    }

    private void updateJeeProfileAnalytics() {
        if (tvJeeProfileTitle == null) return;

        double targetPaceSec = isJeeMainProfile ? 144.0 : 200.0;
        tvJeeProfileTitle.setText(isJeeMainProfile
                ? "JEE MAIN PERFORMANCE (3-HR TARGET: 144s/Q)"
                : "JEE ADVANCED PERFORMANCE (DEEP ANALYSIS)");

        if (btnProfileJeeMain != null) setTabSelected(btnProfileJeeMain, isJeeMainProfile);
        if (btnProfileJeeAdv != null) setTabSelected(btnProfileJeeAdv, !isJeeMainProfile);

        int totalAttempted = 0;
        int correctCount = 0;
        int incorrectCount = 0;
        int skippedCount = 0;
        int sillyMistakesCount = 0;

        long physicsDur = 0, chemistryDur = 0, mathDur = 0;
        int physicsQ = 0, chemistryQ = 0, mathQ = 0;

        List<String> overTargetItems = new ArrayList<>();

        for (TimerSession s : allSessions) {
            String sessSubject = s.getSubject() != null ? s.getSubject().toUpperCase(Locale.ROOT) : "GENERAL";
            for (QuestionLog log : s.getLogs()) {
                totalAttempted++;
                String outcome = log.getOutcome() != null ? log.getOutcome().toUpperCase(Locale.ROOT) : "SOLVED";
                String mistakeTag = (log.getMistakeTag() != null) ? log.getMistakeTag().toUpperCase(Locale.ROOT) : "";
                if (mistakeTag.contains("SILLY")) {
                    sillyMistakesCount++;
                }

                if ("SOLVED".equals(outcome) || "CORRECT".equals(outcome)) {
                    correctCount++;
                } else if ("INCORRECT".equals(outcome)) {
                    incorrectCount++;
                } else {
                    skippedCount++;
                }

                long dur = log.getDurationMs();
                double durSec = dur / 1000.0;
                if (durSec > targetPaceSec) {
                    overTargetItems.add(String.format(Locale.ROOT, "Q#%d (%s): %.0fs (Target: %.0fs)",
                            log.getQuestionNumber(), s.getName(), durSec, targetPaceSec));
                }

                if (sessSubject.contains("PHYSIC")) {
                    physicsDur += dur;
                    physicsQ++;
                } else if (sessSubject.contains("CHEM")) {
                    chemistryDur += dur;
                    chemistryQ++;
                } else if (sessSubject.contains("MATH")) {
                    mathDur += dur;
                    mathQ++;
                }
            }
        }

        double penaltyPerIncorrect = isJeeMainProfile ? 1.0 : 2.0;
        double correctGain = correctCount * 4.0;
        double negativePenalty = incorrectCount * penaltyPerIncorrect;
        double projectedScore = correctGain - negativePenalty;
        double sillyPotential = sillyMistakesCount * (4.0 + penaltyPerIncorrect);

        if (tvJeeEstimatedScore != null) {
            tvJeeEstimatedScore.setText(String.format(Locale.ROOT, "%.0f Pts (Net)", projectedScore));
        }

        if (tvJeeMarksGain != null) {
            tvJeeMarksGain.setText(String.format(Locale.ROOT, "• Correct Answers (+4): +%.0f pts (%d questions)", correctGain, correctCount));
        }
        if (tvJeeNegativePenalty != null) {
            tvJeeNegativePenalty.setText(String.format(Locale.ROOT, "• Negative Penalty (-%d): -%.0f pts (%d incorrect)", (int) penaltyPerIncorrect, negativePenalty, incorrectCount));
        }
        if (tvJeeSillyMistakePotential != null) {
            tvJeeSillyMistakePotential.setText(String.format(Locale.ROOT, "• Recoverable from Silly Mistakes: +%.0f pts (%d mistakes)", sillyPotential, sillyMistakesCount));
        }

        if (tvJeeAccuracy != null) {
            int accuracyPct = totalAttempted > 0 ? (int) Math.round((correctCount * 100.0) / totalAttempted) : 0;
            tvJeeAccuracy.setText(String.format(Locale.ROOT, "%d%% Accuracy (%d/%d Solved)", accuracyPct, correctCount, totalAttempted));
        }

        if (tvJeePhysicsBreakdown != null) {
            long pAvg = physicsQ > 0 ? (physicsDur / (physicsQ * 1000L)) : 0;
            tvJeePhysicsBreakdown.setText(String.format(Locale.ROOT, "Physics: %d Qs • %s • %ds/Q avg", physicsQ, formatDurationShort(physicsDur), pAvg));
        }
        if (tvJeeChemistryBreakdown != null) {
            long cAvg = chemistryQ > 0 ? (chemistryDur / (chemistryQ * 1000L)) : 0;
            tvJeeChemistryBreakdown.setText(String.format(Locale.ROOT, "Chemistry: %d Qs • %s • %ds/Q avg", chemistryQ, formatDurationShort(chemistryDur), cAvg));
        }
        if (tvJeeMathBreakdown != null) {
            long mAvg = mathQ > 0 ? (mathDur / (mathQ * 1000L)) : 0;
            tvJeeMathBreakdown.setText(String.format(Locale.ROOT, "Math: %d Qs • %s • %ds/Q avg", mathQ, formatDurationShort(mathDur), mAvg));
        }

        if (containerOverTargetQuestions != null) {
            containerOverTargetQuestions.removeAllViews();
            if (overTargetItems.isEmpty()) {
                TextView empty = new TextView(getContext());
                empty.setText("✓ All questions within target pace!");
                empty.setTextColor(Color.parseColor("#30D158"));
                empty.setTextSize(12f);
                empty.setPadding(0, 8, 0, 8);
                containerOverTargetQuestions.addView(empty);
            } else {
                int limit = Math.min(8, overTargetItems.size());
                for (int i = 0; i < limit; i++) {
                    TextView item = new TextView(getContext());
                    item.setText("• " + overTargetItems.get(i));
                    item.setTextColor(Color.parseColor("#FF9F0A"));
                    item.setTextSize(11f);
                    item.setPadding(0, 4, 0, 4);
                    containerOverTargetQuestions.addView(item);
                }
                if (overTargetItems.size() > limit) {
                    TextView more = new TextView(getContext());
                    more.setText(String.format(Locale.ROOT, "+ %d more over-target questions", overTargetItems.size() - limit));
                    more.setTextColor(Color.parseColor("#8E8E93"));
                    more.setTextSize(10f);
                    more.setPadding(0, 4, 0, 4);
                    containerOverTargetQuestions.addView(more);
                }
            }
        }
    }

    private void renderStudyCoachCards() {
        if (containerStudyCoachCards == null) return;
        containerStudyCoachCards.removeAllViews();

        List<StudyCoachEngine.CoachCard> cards =
                StudyCoachEngine.generateInsights(allSessions);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (StudyCoachEngine.CoachCard card : cards) {
            View cardView = inflater.inflate(R.layout.item_study_coach_card, containerStudyCoachCards, false);

            TextView tvTitle = cardView.findViewById(R.id.tvCoachCardTitle);
            TextView tvBadge = cardView.findViewById(R.id.tvCoachPriorityBadge);
            TextView tvTimeframe = cardView.findViewById(R.id.tvCoachTimeframe);
            TextView tvSampleSize = cardView.findViewById(R.id.tvCoachSampleSize);
            TextView tvObs = cardView.findViewById(R.id.tvCoachObservation);
            TextView tvRec = cardView.findViewById(R.id.tvCoachRecommendation);
            TextView tvRat = cardView.findViewById(R.id.tvCoachRationale);

            if (tvTitle != null) tvTitle.setText(card.title);
            if (tvBadge != null) {
                tvBadge.setText(card.priority);
                if ("HIGH".equals(card.priority)) {
                    tvBadge.setTextColor(Color.parseColor("#FF453A"));
                } else if ("MEDIUM".equals(card.priority)) {
                    tvBadge.setTextColor(Color.parseColor("#FF9F0A"));
                } else {
                    tvBadge.setTextColor(Color.parseColor("#30D158"));
                }
            }
            if (tvTimeframe != null) tvTimeframe.setText(card.timeframe);
            if (tvSampleSize != null) tvSampleSize.setText(card.sampleSize);
            if (tvObs != null) tvObs.setText(card.observation);
            if (tvRec != null) tvRec.setText(card.recommendation);
            if (tvRat != null) tvRat.setText(card.rationale);

            containerStudyCoachCards.addView(cardView);
        }
    }

    private void updateChartVisuals(List<TimerSession> filtered, int targetSecPerQ, int targetSessionMin) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd", Locale.US);

        if (analyticsChartView.getChartType() == AnalyticsChartView.ChartType.HEATMAP) {
            List<AnalyticsChartView.HeatmapCell> cells = new ArrayList<>();
            Calendar cal = Calendar.getInstance();
            int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            SimpleDateFormat keyFmt = new SimpleDateFormat("yyyyMM", Locale.US);
            String currentMonthKey = keyFmt.format(cal.getTime());
            SimpleDateFormat fullFmt = new SimpleDateFormat("yyyyMMdd", Locale.US);

            for (int day = 1; day <= daysInMonth; day++) {
                String dayKey = String.format(Locale.ROOT, "%s%02d", currentMonthKey, day);
                float minutes = 0;
                int count = 0;

                for (TimerSession s : allSessions) {
                    long t = (s.getUpdatedAt() > 0) ? s.getUpdatedAt() : s.getCreatedAt();
                    if (fullFmt.format(new Date(t)).equals(dayKey)) {
                        minutes += (s.getTotalDurationMs() / 60000f);
                        count++;
                    }
                }
                cells.add(new AnalyticsChartView.HeatmapCell(day, "Day " + day, minutes, count));
            }
            analyticsChartView.setHeatmapData(cells);

        } else if (analyticsChartView.getChartType() == AnalyticsChartView.ChartType.BAR) {
            List<AnalyticsChartView.ChartDataPoint> chartPoints = new ArrayList<>();
            Map<String, Float> dailyMinutes = new LinkedHashMap<>();
            for (TimerSession s : filtered) {
                long time = (s.getUpdatedAt() > 0) ? s.getUpdatedAt() : s.getCreatedAt();
                String dKey = sdf.format(new Date(time));
                float mins = s.getTotalDurationMs() / 60000f;
                dailyMinutes.put(dKey, dailyMinutes.getOrDefault(dKey, 0f) + mins);
            }

            for (Map.Entry<String, Float> e : dailyMinutes.entrySet()) {
                String tooltip = String.format(Locale.ROOT, "%s: %dm practice", e.getKey(), Math.round(e.getValue()));
                chartPoints.add(new AnalyticsChartView.ChartDataPoint(e.getKey(), e.getValue(), tooltip));
            }
            analyticsChartView.setData(chartPoints, (float) targetSessionMin);

        } else {
            // LINE or MOVING_AVERAGE
            List<AnalyticsChartView.ChartDataPoint> chartPoints = new ArrayList<>();
            for (int i = 0; i < filtered.size(); i++) {
                TimerSession s = filtered.get(i);
                if (s.getCompletedCount() > 0) {
                    float avgSec = s.getAverageDurationMs() / 1000f;
                    String label = (filtered.size() <= 8)
                            ? ("S" + (i + 1))
                            : sdf.format(new Date((s.getUpdatedAt() > 0) ? s.getUpdatedAt() : s.getCreatedAt()));
                    String tooltip = String.format(Locale.ROOT, "%s: %.1fs avg (%d Qs)", s.getName(), avgSec, s.getCompletedCount());
                    chartPoints.add(new AnalyticsChartView.ChartDataPoint(label, avgSec, tooltip));
                }
            }
            analyticsChartView.setData(chartPoints, (float) targetSecPerQ);
        }
    }

    private String formatDurationShort(long durationMs) {
        long totalSec = durationMs / 1000;
        long hours = totalSec / 3600;
        long mins = (totalSec % 3600) / 60;
        long secs = totalSec % 60;

        if (hours > 0) {
            return String.format(Locale.ROOT, "%dh %02dm", hours, mins);
        } else if (mins > 0) {
            return String.format(Locale.ROOT, "%dm %02ds", mins, secs);
        } else {
            return String.format(Locale.ROOT, "%ds", secs);
        }
    }
}

package com.example.timerapp.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.timerapp.R;
import com.example.timerapp.data.SessionStorage;
import com.example.timerapp.model.QuestionLog;
import com.example.timerapp.model.TimerSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Modern AMOLED Mistake Review Modal Dialog.
 * Multi-facet filtering across:
 * - Exam Profile (JEE Main, JEE Advanced, Practice, All)
 * - Subject (Physics, Chemistry, Mathematics, General, All)
 * - Topic (search text)
 * - Mistake Category (Silly Mistake, Conceptual, Calculation, Time, Misread, All)
 * - Revision Status & Outcome (Incorrect, Solved, Revised, All)
 */
public class MistakeReviewDialog extends Dialog {

    public interface OnMistakeUpdatedListener {
        void onMistakeUpdated();
    }

    private final List<TimerSession> sessions;
    private final SessionStorage sessionStorage;
    private final OnMistakeUpdatedListener listener;

    private String selectedProfile = "ALL";
    private String selectedSubject = "ALL";
    private String selectedMistakeTag = "ALL";
    private String searchTopic = "";

    // Views
    private TextView tvMistakeCountSummary;
    private EditText etMistakeSearchTopic;
    private ImageButton btnClose;
    private ListView lvMistakeQuestions;
    private TextView tvEmptyMistakes;

    // Filter Buttons
    private TextView filterProfileAll, filterProfileJeeMain, filterProfileJeeAdv, filterProfilePractice;
    private TextView filterSubjectAll, filterSubjectPhysics, filterSubjectChemistry, filterSubjectMath, filterSubjectGeneral;
    private TextView filterTagAll, filterTagSilly, filterTagConceptual, filterTagCalc, filterTagTime, filterTagMisread;

    private MistakeAdapter adapter;
    private final List<MistakeItem> filteredItems = new ArrayList<>();

    public static class MistakeItem {
        public final TimerSession session;
        public final QuestionLog log;
        public final int logIndex;

        public MistakeItem(TimerSession session, QuestionLog log, int logIndex) {
            this.session = session;
            this.log = log;
            this.logIndex = logIndex;
        }
    }

    public MistakeReviewDialog(Context context, List<TimerSession> sessions,
                               SessionStorage sessionStorage, OnMistakeUpdatedListener listener) {
        super(context, R.style.Theme_FullScreenTimer_Dialog);
        this.sessions = (sessions != null) ? sessions : new ArrayList<>();
        this.sessionStorage = sessionStorage;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_mistake_review);

        Window window = getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        }

        initViews();
        setupFilterListeners();
        applyFilters();
    }

    private void initViews() {
        tvMistakeCountSummary = findViewById(R.id.tvMistakeCountSummary);
        etMistakeSearchTopic = findViewById(R.id.etMistakeSearchTopic);
        btnClose = findViewById(R.id.btnCloseMistakeReview);
        lvMistakeQuestions = findViewById(R.id.lvMistakeQuestions);
        tvEmptyMistakes = findViewById(R.id.tvEmptyMistakes);

        filterProfileAll = findViewById(R.id.filterProfileAll);
        filterProfileJeeMain = findViewById(R.id.filterProfileJeeMain);
        filterProfileJeeAdv = findViewById(R.id.filterProfileJeeAdv);
        filterProfilePractice = findViewById(R.id.filterProfilePractice);

        filterSubjectAll = findViewById(R.id.filterSubjectAll);
        filterSubjectPhysics = findViewById(R.id.filterSubjectPhysics);
        filterSubjectChemistry = findViewById(R.id.filterSubjectChemistry);
        filterSubjectMath = findViewById(R.id.filterSubjectMath);
        filterSubjectGeneral = findViewById(R.id.filterSubjectGeneral);

        filterTagAll = findViewById(R.id.filterTagAll);
        filterTagSilly = findViewById(R.id.filterTagSilly);
        filterTagConceptual = findViewById(R.id.filterTagConceptual);
        filterTagCalc = findViewById(R.id.filterTagCalc);
        filterTagTime = findViewById(R.id.filterTagTime);
        filterTagMisread = findViewById(R.id.filterTagMisread);

        adapter = new MistakeAdapter();
        lvMistakeQuestions.setAdapter(adapter);

        btnClose.setOnClickListener(v -> dismiss());

        etMistakeSearchTopic.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchTopic = (s != null) ? s.toString().trim().toLowerCase(Locale.ROOT) : "";
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFilterListeners() {
        // Profile
        filterProfileAll.setOnClickListener(v -> { selectedProfile = "ALL"; updatePills(); applyFilters(); });
        filterProfileJeeMain.setOnClickListener(v -> { selectedProfile = "JEE_MAIN"; updatePills(); applyFilters(); });
        filterProfileJeeAdv.setOnClickListener(v -> { selectedProfile = "JEE_ADVANCED"; updatePills(); applyFilters(); });
        filterProfilePractice.setOnClickListener(v -> { selectedProfile = "PRACTICE"; updatePills(); applyFilters(); });

        // Subject
        filterSubjectAll.setOnClickListener(v -> { selectedSubject = "ALL"; updatePills(); applyFilters(); });
        filterSubjectPhysics.setOnClickListener(v -> { selectedSubject = "PHYSICS"; updatePills(); applyFilters(); });
        filterSubjectChemistry.setOnClickListener(v -> { selectedSubject = "CHEMISTRY"; updatePills(); applyFilters(); });
        filterSubjectMath.setOnClickListener(v -> { selectedSubject = "MATHEMATICS"; updatePills(); applyFilters(); });
        filterSubjectGeneral.setOnClickListener(v -> { selectedSubject = "GENERAL"; updatePills(); applyFilters(); });

        // Tag
        filterTagAll.setOnClickListener(v -> { selectedMistakeTag = "ALL"; updatePills(); applyFilters(); });
        filterTagSilly.setOnClickListener(v -> { selectedMistakeTag = "SILLY"; updatePills(); applyFilters(); });
        filterTagConceptual.setOnClickListener(v -> { selectedMistakeTag = "CONCEPTUAL"; updatePills(); applyFilters(); });
        filterTagCalc.setOnClickListener(v -> { selectedMistakeTag = "CALC"; updatePills(); applyFilters(); });
        filterTagTime.setOnClickListener(v -> { selectedMistakeTag = "TIME"; updatePills(); applyFilters(); });
        filterTagMisread.setOnClickListener(v -> { selectedMistakeTag = "MISREAD"; updatePills(); applyFilters(); });
    }

    private void updatePills() {
        setPillSelected(filterProfileAll, "ALL".equals(selectedProfile));
        setPillSelected(filterProfileJeeMain, "JEE_MAIN".equals(selectedProfile));
        setPillSelected(filterProfileJeeAdv, "JEE_ADVANCED".equals(selectedProfile));
        setPillSelected(filterProfilePractice, "PRACTICE".equals(selectedProfile));

        setPillSelected(filterSubjectAll, "ALL".equals(selectedSubject));
        setPillSelected(filterSubjectPhysics, "PHYSICS".equals(selectedSubject));
        setPillSelected(filterSubjectChemistry, "CHEMISTRY".equals(selectedSubject));
        setPillSelected(filterSubjectMath, "MATHEMATICS".equals(selectedSubject));
        setPillSelected(filterSubjectGeneral, "GENERAL".equals(selectedSubject));

        setPillSelected(filterTagAll, "ALL".equals(selectedMistakeTag));
        setPillSelected(filterTagSilly, "SILLY".equals(selectedMistakeTag));
        setPillSelected(filterTagConceptual, "CONCEPTUAL".equals(selectedMistakeTag));
        setPillSelected(filterTagCalc, "CALC".equals(selectedMistakeTag));
        setPillSelected(filterTagTime, "TIME".equals(selectedMistakeTag));
        setPillSelected(filterTagMisread, "MISREAD".equals(selectedMistakeTag));
    }

    private void setPillSelected(TextView tv, boolean selected) {
        if (tv == null) return;
        tv.setBackgroundResource(selected ? R.drawable.bg_pill_selected : R.drawable.bg_pill_unselected);
        tv.setTextColor(selected ? Color.parseColor("#0A84FF") : Color.parseColor("#8E8E93"));
    }

    private void applyFilters() {
        filteredItems.clear();

        for (TimerSession s : sessions) {
            String profile = (s.getExamProfile() != null) ? s.getExamProfile().toUpperCase(Locale.ROOT) : "PRACTICE";
            if (!"ALL".equals(selectedProfile) && !profile.contains(selectedProfile)) {
                continue;
            }

            String subject = (s.getSubject() != null) ? s.getSubject().toUpperCase(Locale.ROOT) : "GENERAL";
            if (!"ALL".equals(selectedSubject) && !subject.contains(selectedSubject)) {
                continue;
            }

            List<QuestionLog> logs = s.getLogs();
            for (int i = 0; i < logs.size(); i++) {
                QuestionLog log = logs.get(i);
                String outcome = (log.getOutcome() != null) ? log.getOutcome().toUpperCase(Locale.ROOT) : "SOLVED";
                String tag = (log.getMistakeTag() != null) ? log.getMistakeTag().toUpperCase(Locale.ROOT) : "";

                // We prioritize items with INCORRECT outcome or explicit mistake tags
                boolean isMistake = "INCORRECT".equals(outcome) || !tag.isEmpty();
                if (!isMistake && "ALL".equals(selectedMistakeTag)) {
                    // Only show questions marked incorrect or tagged as mistakes
                    continue;
                }

                if (!"ALL".equals(selectedMistakeTag)) {
                    if (!tag.contains(selectedMistakeTag)) {
                        continue;
                    }
                }

                if (!searchTopic.isEmpty()) {
                    String topic = (log.getTopic() != null) ? log.getTopic().toLowerCase(Locale.ROOT) : "";
                    String notes = (log.getNotes() != null) ? log.getNotes().toLowerCase(Locale.ROOT) : "";
                    String sName = (s.getName() != null) ? s.getName().toLowerCase(Locale.ROOT) : "";
                    String qNumStr = "q#" + log.getQuestionNumber();
                    if (!topic.contains(searchTopic) && !notes.contains(searchTopic) &&
                        !sName.contains(searchTopic) && !qNumStr.contains(searchTopic)) {
                        continue;
                    }
                }

                filteredItems.add(new MistakeItem(s, log, i));
            }
        }

        if (tvMistakeCountSummary != null) {
            tvMistakeCountSummary.setText(String.format(Locale.ROOT, "%d mistake%s found across matching sessions",
                    filteredItems.size(), filteredItems.size() == 1 ? "" : "s"));
        }

        if (tvEmptyMistakes != null) {
            tvEmptyMistakes.setVisibility(filteredItems.isEmpty() ? View.VISIBLE : View.GONE);
        }
        if (lvMistakeQuestions != null) {
            lvMistakeQuestions.setVisibility(filteredItems.isEmpty() ? View.GONE : View.VISIBLE);
        }

        adapter.notifyDataSetChanged();
    }

    private class MistakeAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return filteredItems.size();
        }

        @Override
        public MistakeItem getItem(int position) {
            return filteredItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_mistake_review, parent, false);
            }

            MistakeItem item = getItem(position);
            QuestionLog log = item.log;

            TextView tvQNum = convertView.findViewById(R.id.tvMistakeQuestionNum);
            TextView tvSessName = convertView.findViewById(R.id.tvMistakeSessionName);
            TextView tvDuration = convertView.findViewById(R.id.tvMistakeDuration);
            TextView tvSubject = convertView.findViewById(R.id.tvMistakeSubjectBadge);
            TextView tvOutcome = convertView.findViewById(R.id.tvMistakeOutcomeBadge);
            TextView tvTag = convertView.findViewById(R.id.tvMistakeTagBadge);
            TextView tvConfidence = convertView.findViewById(R.id.tvMistakeConfidence);
            TextView tvNotes = convertView.findViewById(R.id.tvMistakeNotes);
            Button btnToggleRevised = convertView.findViewById(R.id.btnToggleRevised);
            Button btnEditTags = convertView.findViewById(R.id.btnEditMistakeTags);

            tvQNum.setText(String.format(Locale.ROOT, "Q#%d", log.getQuestionNumber()));
            tvSessName.setText(item.session.getName());
            tvDuration.setText(log.getFormattedDuration());
            tvSubject.setText(item.session.getSubject().toUpperCase(Locale.ROOT));

            String outcome = (log.getOutcome() != null && !log.getOutcome().isEmpty()) ? log.getOutcome().toUpperCase(Locale.ROOT) : "SOLVED";
            tvOutcome.setText(outcome);
            if ("INCORRECT".equals(outcome)) {
                tvOutcome.setTextColor(Color.parseColor("#FF453A")); // Red
            } else if ("REVISED".equals(outcome)) {
                tvOutcome.setTextColor(Color.parseColor("#30D158")); // Green
            } else {
                tvOutcome.setTextColor(Color.parseColor("#0A84FF")); // Blue
            }

            String mistakeTag = (log.getMistakeTag() != null && !log.getMistakeTag().isEmpty()) ? log.getMistakeTag() : "UNCATEGORIZED";
            tvTag.setText(mistakeTag);

            int conf = log.getConfidence() > 0 ? log.getConfidence() : 3;
            StringBuilder stars = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                stars.append(i < conf ? "★" : "☆");
            }
            tvConfidence.setText("CONF: " + stars.toString());

            String notes = log.getNotes();
            if (notes != null && !notes.trim().isEmpty()) {
                tvNotes.setText(notes);
                tvNotes.setVisibility(View.VISIBLE);
            } else {
                tvNotes.setVisibility(View.GONE);
            }

            boolean isRevised = "REVISED".equals(outcome);
            btnToggleRevised.setText(isRevised ? "UNMARK" : "MARK REVISED");
            btnToggleRevised.setOnClickListener(v -> {
                String newOutcome = isRevised ? "INCORRECT" : "REVISED";
                QuestionLog updated = log.withTags(newOutcome, log.getDifficulty(), log.getConfidence(), log.getMistakeTag(), log.getNotes());
                item.session.replaceLog(item.logIndex, updated);
                if (sessionStorage != null) {
                    sessionStorage.saveSessions(sessions);
                }
                Toast.makeText(getContext(), isRevised ? "Marked as Unrevised" : "Marked as Revised ✓", Toast.LENGTH_SHORT).show();
                applyFilters();
                if (listener != null) listener.onMistakeUpdated();
            });

            btnEditTags.setOnClickListener(v -> {
                QuestionTaggingDialog dialog = new QuestionTaggingDialog(getContext(), log, updatedLog -> {
                    item.session.replaceLog(item.logIndex, updatedLog);
                    if (sessionStorage != null) {
                        sessionStorage.saveSessions(sessions);
                    }
                    applyFilters();
                    if (listener != null) listener.onMistakeUpdated();
                });
                dialog.show();
            });

            return convertView;
        }
    }
}

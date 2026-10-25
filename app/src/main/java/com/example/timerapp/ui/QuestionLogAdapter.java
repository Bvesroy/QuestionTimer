package com.example.timerapp.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.timerapp.R;
import com.example.timerapp.model.QuestionLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * High-performance AMOLED ListView adapter for question solve time breakdown.
 * Displays question number, relative duration bar, fastest/slowest badges, and monospaced duration.
 */
public class QuestionLogAdapter extends BaseAdapter {

    public interface OnQuestionTagClickListener {
        void onTagClicked(QuestionLog log);
    }

    private final List<QuestionLog> logs = new ArrayList<>();
    private final LayoutInflater inflater;
    private OnQuestionTagClickListener tagClickListener;
    private long maxDurationMs = 1;
    private long fastestLogId = -1;
    private long slowestLogId = -1;

    public QuestionLogAdapter(Context context) {
        this.inflater = LayoutInflater.from(context);
    }

    public void setOnQuestionTagClickListener(OnQuestionTagClickListener listener) {
        this.tagClickListener = listener;
    }

    public void setLogs(List<QuestionLog> newLogs, QuestionLog fastest, QuestionLog slowest) {
        logs.clear();
        if (newLogs != null) {
            logs.addAll(newLogs);
            Collections.reverse(logs); // Most recent questions at top
        }

        fastestLogId = (fastest != null) ? fastest.getId() : -1;
        slowestLogId = (slowest != null) ? slowest.getId() : -1;

        maxDurationMs = 1;
        for (QuestionLog log : logs) {
            if (log.getDurationMs() > maxDurationMs) {
                maxDurationMs = log.getDurationMs();
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return logs.size();
    }

    @Override
    public QuestionLog getItem(int position) {
        return logs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return logs.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_question_log, parent, false);
            holder = new ViewHolder();
            holder.tvQuestionNum = convertView.findViewById(R.id.tvQuestionNum);
            holder.pbDurationBar = convertView.findViewById(R.id.pbDurationBar);
            holder.tvDurationTag = convertView.findViewById(R.id.tvDurationTag);
            holder.tvDuration = convertView.findViewById(R.id.tvDuration);
            holder.btnTagQuestion = convertView.findViewById(R.id.btnTagQuestion);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        QuestionLog log = getItem(position);
        Context ctx = convertView.getContext();

        holder.tvQuestionNum.setText(String.format(Locale.ROOT, "Q %02d", log.getQuestionNumber()));
        holder.tvDuration.setText(log.getFormattedDuration());

        if (holder.btnTagQuestion != null) {
            String outcome = log.getOutcome();
            if (outcome != null && !outcome.isEmpty() && !"SOLVED".equalsIgnoreCase(outcome)) {
                holder.btnTagQuestion.setText(outcome.substring(0, Math.min(6, outcome.length())));
                holder.btnTagQuestion.setTextColor(ctx.getColor(R.color.accent_paused));
            } else {
                holder.btnTagQuestion.setText("TAG");
                holder.btnTagQuestion.setTextColor(ctx.getColor(R.color.accent_highlight));
            }

            holder.btnTagQuestion.setOnClickListener(v -> {
                if (tagClickListener != null) {
                    tagClickListener.onTagClicked(log);
                }
            });
        }

        int progressPercent = (int) Math.min(100, Math.max(5, (log.getDurationMs() * 100) / maxDurationMs));
        holder.pbDurationBar.setProgress(progressPercent);

        if (logs.size() > 1 && log.getId() == fastestLogId) {
            holder.tvDurationTag.setVisibility(View.VISIBLE);
            holder.tvDurationTag.setText("FAST");
            holder.tvDurationTag.setTextColor(ctx.getColor(R.color.fastest_green));
        } else if (logs.size() > 1 && log.getId() == slowestLogId) {
            holder.tvDurationTag.setVisibility(View.VISIBLE);
            holder.tvDurationTag.setText("SLOW");
            holder.tvDurationTag.setTextColor(ctx.getColor(R.color.slowest_amber));
        } else {
            holder.tvDurationTag.setVisibility(View.GONE);
        }

        return convertView;
    }

    private static class ViewHolder {
        TextView tvQuestionNum;
        ProgressBar pbDurationBar;
        TextView tvDurationTag;
        TextView tvDuration;
        TextView btnTagQuestion;
    }
}

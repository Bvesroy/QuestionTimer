package com.example.timerapp.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.timerapp.R;
import com.example.timerapp.model.TimerSession;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for selecting and resuming past sessions in the session manager modal.
 */
public class SessionsAdapter extends BaseAdapter {

    public interface OnSessionClickListener {
        void onSessionClick(TimerSession session);
    }

    private final List<TimerSession> sessions = new ArrayList<>();
    private final LayoutInflater inflater;
    private long currentActiveSessionId = -1;
    private final OnSessionClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault());

    public SessionsAdapter(Context context, OnSessionClickListener listener) {
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    public void setSessions(List<TimerSession> newSessions, long activeSessionId) {
        sessions.clear();
        if (newSessions != null) {
            sessions.addAll(newSessions);
        }
        this.currentActiveSessionId = activeSessionId;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return sessions.size();
    }

    @Override
    public TimerSession getItem(int position) {
        return sessions.get(position);
    }

    @Override
    public long getItemId(int position) {
        return sessions.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_session_switch, parent, false);
            holder = new ViewHolder();
            holder.tvItemSessionTitle = convertView.findViewById(R.id.tvItemSessionTitle);
            holder.tvItemSessionSubtitle = convertView.findViewById(R.id.tvItemSessionSubtitle);
            holder.tvItemActiveTag = convertView.findViewById(R.id.tvItemActiveTag);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        TimerSession session = getItem(position);
        holder.tvItemSessionTitle.setText(session.getName());

        String targetStr = session.getTarget() > 0 ? " / " + session.getTarget() : "";
        String subtitle = session.getCompletedCount() + targetStr + " Solved • " + dateFormat.format(new Date(session.getCreatedAt()));
        holder.tvItemSessionSubtitle.setText(subtitle);

        holder.tvItemActiveTag.setVisibility(session.getId() == currentActiveSessionId ? View.VISIBLE : View.GONE);

        convertView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSessionClick(session);
            }
        });

        return convertView;
    }

    private static class ViewHolder {
        TextView tvItemSessionTitle;
        TextView tvItemSessionSubtitle;
        TextView tvItemActiveTag;
    }
}

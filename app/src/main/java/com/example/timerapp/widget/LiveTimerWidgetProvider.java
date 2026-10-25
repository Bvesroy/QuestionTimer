package com.example.timerapp.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

import com.example.timerapp.model.TimerState;

/**
 * AppWidgetProvider for Compact Live Timer Widget.
 */
public class LiveTimerWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        WidgetUpdateHelper.updateLiveTimerWidgets(context, appWidgetManager, TimerState.IDLE, 0L, 0L, false);
    }
}

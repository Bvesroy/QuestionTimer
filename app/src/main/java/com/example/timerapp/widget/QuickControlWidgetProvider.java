package com.example.timerapp.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.timerapp.model.TimerState;
import com.example.timerapp.ui.MainActivity;

/**
 * AppWidgetProvider for Quick-Control Widget (Pause/Resume, Break, Open App).
 */
public class QuickControlWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "QuickControlWidget";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        WidgetUpdateHelper.updateQuickControlWidgets(context, appWidgetManager, TimerState.IDLE, 0L, false);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (action == null) return;

        if (WidgetUpdateHelper.ACTION_WIDGET_TOGGLE.equals(action) ||
            WidgetUpdateHelper.ACTION_WIDGET_BREAK.equals(action)) {
            // Forward quick action intent to MainActivity
            Intent forwardIntent = new Intent(context, MainActivity.class);
            forwardIntent.setAction(action);
            forwardIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(forwardIntent);
        }
    }
}

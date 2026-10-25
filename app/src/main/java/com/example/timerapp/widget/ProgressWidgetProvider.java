package com.example.timerapp.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

/**
 * AppWidgetProvider for Study Consistency and Accuracy Progress Widget.
 */
public class ProgressWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        WidgetUpdateHelper.updateProgressWidgets(context, appWidgetManager, 100, 0, 1);
    }
}

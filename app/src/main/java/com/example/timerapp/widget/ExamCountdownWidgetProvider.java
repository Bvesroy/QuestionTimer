package com.example.timerapp.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

/**
 * AppWidgetProvider for Minimal JEE Exam Countdown Widget (Days left to 20 Jan 2027).
 */
public class ExamCountdownWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        WidgetUpdateHelper.updateExamCountdownWidgets(context, appWidgetManager);
    }
}

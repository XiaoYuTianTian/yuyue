package com.yuyue.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.yuyue.app.MainActivity
import com.yuyue.app.R
import com.yuyue.app.ui.CaptureLaunch

/**
 * One-tap desktop widget that opens quick voice capture (not the keyboard).
 * Pinning / Play Console widget catalog still requires a device check.
 */
class QuickCaptureWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_capture)
            val intent = Intent(context, MainActivity::class.java).apply {
                action = CaptureLaunch.QUICK_ACTION
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pending = PendingIntent.getActivity(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pending)
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}

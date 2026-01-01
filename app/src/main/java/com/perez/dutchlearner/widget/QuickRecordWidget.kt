package com.perez.dutchlearner.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.perez.dutchlearner.R
import com.perez.dutchlearner.widget.QuickRecordWidget.Companion.ACTION_RECORD

class QuickRecordWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_RECORD = "com.perez.dutchlearner.widget.ACTION_RECORD"

        /**
         * Actualiza el estado visual del widget
         */
        fun updateWidgetState(context: Context, isRecording: Boolean) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetComponent = ComponentName(context, QuickRecordWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)

            appWidgetIds.forEach { appWidgetId ->
                updateAppWidget(context, appWidgetManager, appWidgetId, isRecording)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, isRecording = false)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_RECORD) {
            // Iniciar servicio de grabación
            val serviceIntent = Intent(context, RecordingService::class.java).apply {
                action = RecordingService.ACTION_START_RECORDING
            }
            context.startForegroundService(serviceIntent)
        }
    }
}

private fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    isRecording: Boolean
) {
    val views = RemoteViews(context.packageName, R.layout.quick_record_widget)

    // Cambiar drawable del background según estado
    if (isRecording) {
        // Estado GRABANDO: Usar fondo rojo
        views.setInt(
            R.id.widget_button,
            "setBackgroundResource",
            R.drawable.widget_background_recording
        )
    } else {
        // Estado IDLE: Fondo morado original
        views.setInt(
            R.id.widget_button,
            "setBackgroundResource",
            R.drawable.widget_background
        )
    }

    // Intent para grabar
    val recordIntent = Intent(context, QuickRecordWidget::class.java).apply {
        action = ACTION_RECORD
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        recordIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_button, pendingIntent)

    appWidgetManager.updateAppWidget(appWidgetId, views)
}
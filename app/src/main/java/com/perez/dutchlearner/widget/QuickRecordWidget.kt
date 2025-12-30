package com.perez.dutchlearner.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.perez.dutchlearner.R

/**
 * Widget de grabación rápida CON Foreground Service
 * Graba en background sin abrir la app
 */
class QuickRecordWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Actualizar todos los widgets
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Widget agregado por primera vez
    }

    override fun onDisabled(context: Context) {
        // Último widget eliminado
    }

    companion object {
        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            // Intent para iniciar el Foreground Service de grabación
            val intent = Intent(context, RecordingForegroundService::class.java).apply {
                action = RecordingForegroundService.ACTION_START_RECORDING
            }

            // PendingIntent para iniciar el servicio
            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PendingIntent.getForegroundService(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getService(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            // Construir layout del widget
            val views = RemoteViews(context.packageName, R.layout.quick_record_widget).apply {
                setOnClickPendingIntent(R.id.widget_button, pendingIntent)
            }

            // Actualizar widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
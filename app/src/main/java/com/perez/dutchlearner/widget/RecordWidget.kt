package com.perez.dutchlearner.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.perez.dutchlearner.R
import com.perez.dutchlearner.MainActivity

/**
 * Widget de grabación rápida
 * Botón circular rojo para grabar desde el escritorio
 */
class RecordWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Actualizar cada instancia del widget
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
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            // Intent para abrir MainActivity en modo grabación
            val intent = Intent(context, MainActivity::class.java).apply {
                action = "com.perez.dutchlearner.ACTION_QUICK_RECORD"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Crear vista del widget
            val views = RemoteViews(context.packageName, R.layout.widget_record).apply {
                setOnClickPendingIntent(R.id.widget_button, pendingIntent)
            }

            // Actualizar widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
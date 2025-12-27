package com.perez.dutchlearner.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.provider.Settings
import java.util.*

class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Programa notificación diaria a una hora específica
     * CORREGIDO: Usa setRepeating para evitar SecurityException
     */
    fun scheduleDailyNotification(hour: Int, minute: Int): Boolean {
        try {
            // Verificar permiso en Android 12+ (opcional, solo para alarmas exactas)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.w("DutchLearner", "No puede programar alarmas exactas, usando inexactas")
                    // Continuar de todos modos con alarma inexacta
                }
            }

            // Crear intent para el receiver
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.perez.dutchlearner.NOTIFICATION_ALARM"
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                NOTIFICATION_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Calcular tiempo para la alarma
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                // Si la hora ya pasó hoy, programar para mañana
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            Log.d("DutchLearner", "Programando alarma para: ${calendar.time}")

            // CORRECCIÓN: Usar setRepeating en vez de setExactAndAllowWhileIdle
            // Esto NO requiere permiso SCHEDULE_EXACT_ALARM
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY, // 24 horas
                pendingIntent
            )

            // Guardar configuración
            saveScheduleConfig(hour, minute)

            Log.d("DutchLearner", "Alarma programada exitosamente")
            return true

        } catch (e: Exception) {
            Log.e("DutchLearner", "Error programando alarma", e)
            return false
        }
    }

    /**
     * Cancela todas las notificaciones programadas
     */
    fun cancelDailyNotifications() {
        try {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.perez.dutchlearner.NOTIFICATION_ALARM"
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                NOTIFICATION_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)
            clearScheduleConfig()

            Log.d("DutchLearner", "Alarmas canceladas")
        } catch (e: Exception) {
            Log.e("DutchLearner", "Error cancelando alarmas", e)
        }
    }

    /**
     * Verifica si hay notificaciones programadas
     */
    fun hasScheduledNotifications(): Boolean {
        val prefs = context.getSharedPreferences("notifications", Context.MODE_PRIVATE)
        return prefs.contains("notification_hour")
    }

    /**
     * Obtiene la hora programada
     */
    fun getScheduledTime(): Pair<Int, Int>? {
        val prefs = context.getSharedPreferences("notifications", Context.MODE_PRIVATE)
        return if (prefs.contains("notification_hour")) {
            Pair(
                prefs.getInt("notification_hour", 9),
                prefs.getInt("notification_minute", 0)
            )
        } else {
            null
        }
    }

    private fun saveScheduleConfig(hour: Int, minute: Int) {
        context.getSharedPreferences("notifications", Context.MODE_PRIVATE)
            .edit()
            .putInt("notification_hour", hour)
            .putInt("notification_minute", minute)
            .putBoolean("notifications_enabled", true)
            .apply()
    }

    private fun clearScheduleConfig() {
        context.getSharedPreferences("notifications", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    companion object {
        private const val NOTIFICATION_REQUEST_CODE = 1001
    }
}
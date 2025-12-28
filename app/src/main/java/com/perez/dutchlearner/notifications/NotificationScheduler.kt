package com.perez.dutchlearner.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.*

class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Programa notificación diaria EXACTA
     */
    fun scheduleDailyNotification(hour: Int, minute: Int): Boolean {
        try {
            // Crear intent
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.perez.dutchlearner.NOTIFICATION_ALARM"
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                NOTIFICATION_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Calcular tiempo
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                // Si ya pasó, programar para mañana
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            Log.d("DutchLearner", "Programando alarma EXACTA para: ${calendar.time}")

            // Programar alarma EXACTA (requiere permisos en Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    // Programar primera alarma exacta
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )

                    // Guardar para reprogramar después
                    saveScheduleConfig(hour, minute)

                    Log.d("DutchLearner", "Alarma exacta programada")
                    return true
                } else {
                    Log.e("DutchLearner", "No tiene permiso para alarmas exactas")
                    // Intentar con alarma inexacta
                    alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        AlarmManager.INTERVAL_DAY,
                        pendingIntent
                    )
                    saveScheduleConfig(hour, minute)
                    return true
                }
            } else {
                // Android < 12: usar setExact sin problemas
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                saveScheduleConfig(hour, minute)
                return true
            }

        } catch (e: Exception) {
            Log.e("DutchLearner", "Error programando alarma", e)
            return false
        }
    }

    /**
     * Reprograma la alarma para el día siguiente (llamar desde AlarmReceiver)
     */
    fun rescheduleNextDay() {
        val time = getScheduledTime()
        if (time != null) {
            scheduleDailyNotification(time.first, time.second)
        }
    }

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

    fun hasScheduledNotifications(): Boolean {
        val prefs = context.getSharedPreferences("notifications", Context.MODE_PRIVATE)
        return prefs.contains("notification_hour")
    }

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
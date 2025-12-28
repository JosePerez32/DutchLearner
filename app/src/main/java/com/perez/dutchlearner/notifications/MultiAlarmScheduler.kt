package com.perez.dutchlearner.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.perez.dutchlearner.database.AlarmEntity
import java.util.*

class MultiAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Programa una alarma específica
     */
    fun scheduleAlarm(alarm: AlarmEntity): Boolean {
        try {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.perez.dutchlearner.NOTIFICATION_ALARM"
                putExtra("alarm_id", alarm.id)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarm.id.toInt(), // Usar ID como request code único
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Calcular próxima ejecución
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                // Si ya pasó hoy, avanzar al próximo día activo
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            Log.d("DutchLearner", "Programando alarma ${alarm.id} para: ${calendar.time}")

            // Programar según versión de Android
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }

            return true

        } catch (e: Exception) {
            Log.e("DutchLearner", "Error programando alarma ${alarm.id}", e)
            return false
        }
    }

    /**
     * Cancela una alarma específica
     */
    fun cancelAlarm(alarmId: Long) {
        try {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.perez.dutchlearner.NOTIFICATION_ALARM"
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)
            Log.d("DutchLearner", "Alarma $alarmId cancelada")

        } catch (e: Exception) {
            Log.e("DutchLearner", "Error cancelando alarma $alarmId", e)
        }
    }

    /**
     * Programa todas las alarmas activas
     */
    fun scheduleAllAlarms(alarms: List<AlarmEntity>) {
        alarms.filter { it.enabled }.forEach { alarm ->
            scheduleAlarm(alarm)
        }
    }

    /**
     * Cancela todas las alarmas
     */
    fun cancelAllAlarms(alarms: List<AlarmEntity>) {
        alarms.forEach { alarm ->
            cancelAlarm(alarm.id)
        }
    }

    /**
     * Reprograma una alarma para mañana (llamar desde AlarmReceiver)
     */
    suspend fun rescheduleAlarm(alarmId: Long, database: com.perez.dutchlearner.database.AppDatabase) {
        val alarm = database.alarmDao().getAlarmById(alarmId)
        if (alarm != null && alarm.enabled) {
            scheduleAlarm(alarm)
        }
    }
}
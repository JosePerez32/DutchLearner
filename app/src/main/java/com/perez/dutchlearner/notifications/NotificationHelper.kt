package com.perez.dutchlearner.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.perez.dutchlearner.MainActivity
import com.perez.dutchlearner.R
import com.perez.dutchlearner.database.PhraseEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationHelper(private val context: Context) {

    fun showPracticeNotification(phrase: PhraseEntity?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                createNotificationChannel()

                val notificationId = System.currentTimeMillis().toInt()
                val builder = buildNotification(phrase, notificationId)

                with(NotificationManagerCompat.from(context)) {
                    if (areNotificationsEnabled()) {
                        notify(notificationId, builder.build())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun showAchievementNotification(title: String, message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                createNotificationChannel()

                val notificationId = System.currentTimeMillis().toInt()
                val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_dutch_flag)
                    .setContentTitle("🏆 $title")
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))

                with(NotificationManagerCompat.from(context)) {
                    if (areNotificationsEnabled()) {
                        notify(notificationId, builder.build())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun buildNotification(phrase: PhraseEntity?, notificationId: Int): NotificationCompat.Builder {
        val defaultTitle = "🇳🇱 ¡Hora de practicar holandés!"
        val defaultText = "Graba una nueva frase o repasa las anteriores."

        return if (phrase != null) {
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_dutch_flag)

                .setContentTitle("📚 Frase del día")
                .setContentText(phrase.dutchText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("🇳🇱 ${phrase.dutchText}\n\n🇪🇸 ${phrase.spanishText}")
                )
                .addAction(
                    R.drawable.mic, // Usar icono genérico o crear uno
                    "Grabar respuesta",
                    getRecordPendingIntent(notificationId)
                )
                .addAction(
                    R.drawable.ic_speaker, // Icono de altavoz
                    "Escuchar",
                    getListenPendingIntent(phrase.dutchText, notificationId)
                )
        } else {
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_dutch_flag)
                .setContentTitle(defaultTitle)
                .setContentText(defaultText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Dutch Learner Notifications"
            val descriptionText = "Recordatorios diarios para practicar holandés"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
            }

            val notificationManager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getRecordPendingIntent(notificationId: Int) =
        android.app.PendingIntent.getActivity(
            context,
            notificationId,
            android.content.Intent(context, MainActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                        android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("action", "record")
            },
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                    android.app.PendingIntent.FLAG_IMMUTABLE
        )

    private fun getListenPendingIntent(text: String, notificationId: Int) =
        android.app.PendingIntent.getBroadcast(
            context,
            notificationId + 1, // ID diferente
            android.content.Intent(context, AlarmReceiver::class.java).apply {
                action = "ACTION_LISTEN"
                putExtra("text_to_speak", text)
                putExtra("notification_id", notificationId)
            },
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                    android.app.PendingIntent.FLAG_IMMUTABLE
        )

    fun areNotificationsEnabled(): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    companion object {
        const val CHANNEL_ID = "dutch_learner_channel"
        const val PRACTICE_CHANNEL_ID = "dutch_practice_channel"
    }
}
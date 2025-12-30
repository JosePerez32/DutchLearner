package com.perez.dutchlearner.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.perez.dutchlearner.MainActivity
import com.perez.dutchlearner.R
import com.perez.dutchlearner.database.PhraseEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "dutch_learner_reminders"
        const val CHANNEL_NAME = "Recordatorios de Dutch Learner"
        const val NOTIFICATION_ID = 1001
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val contentGenerator = NotificationContentGenerator(context)

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones para practicar holandés"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * ⬇️ MÉTODO NUEVO: Muestra notificación con contenido aleatorio inteligente
     */
    fun showSmartNotification() {
        // Generar contenido en coroutine
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val content = contentGenerator.generateContent()
                showNotificationWithContent(content)
            } catch (e: Exception) {
                android.util.Log.e("NotificationHelper", "Error generating content", e)
                // Fallback a notificación genérica
                showPracticeNotification(null)
            }
        }
    }

    /**
     * ⬇️ MÉTODO NUEVO: Muestra notificación con el contenido generado
     */
    private fun showNotificationWithContent(content: NotificationContent) {
        // Intent para abrir app
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent para escuchar (si hay texto de audio)
        val actions = mutableListOf<NotificationCompat.Action>()

        if (content.audioText != null) {
            val listenIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("action", "speak")
                putExtra("text_to_speak", content.audioText)
            }
            val listenPendingIntent = PendingIntent.getActivity(
                context,
                1,
                listenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            actions.add(
                NotificationCompat.Action.Builder(
                    0,
                    "🔊 Escuchar",
                    listenPendingIntent
                ).build()
            )
        }

        // Intent para grabar
        val recordIntent = Intent(context, MainActivity::class.java).apply {
            action = "com.perez.dutchlearner.ACTION_QUICK_RECORD"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val recordPendingIntent = PendingIntent.getActivity(
            context,
            2,
            recordIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        actions.add(
            NotificationCompat.Action.Builder(
                0,
                "🎤 Grabar",
                recordPendingIntent
            ).build()
        )

        // Construir notificación
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(content.title)
            .setContentText(content.text)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(content.text)
                    .setBigContentTitle(content.title)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)

        // Agregar acciones
        actions.forEach { builder.addAction(it) }

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    /**
     * ⬇️ MÉTODO EXISTENTE (para compatibilidad): Muestra notificación de práctica
     */
    fun showPracticeNotification(phrase: PhraseEntity?) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (phrase != null) {
            "💬 Frase del día"
        } else {
            "🇳🇱 Momento de practicar"
        }

        val text = if (phrase != null) {
            "🇪🇸 ${phrase.spanishText}\n🇳🇱 ${phrase.dutchText}"
        } else {
            "Graba una nueva frase o repasa las anteriores"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(text)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
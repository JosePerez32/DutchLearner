package com.perez.dutchlearner.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import com.perez.dutchlearner.notifications.NotificationHelper
import com.perez.dutchlearner.notifications.NotificationContentGenerator


class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
//        if (intent.action == "com.perez.dutchlearner.NOTIFICATION_ALARM") {
//            Log.d("DutchLearner", "Alarma recibida, mostrando notificación")
//
//            // Mostrar notificación simple sin frase específica
//            CoroutineScope(Dispatchers.IO).launch {
//                NotificationHelper(context).showPracticeNotification(null)
//            }
//        }
//        when (intent.action) {
//            "com.perez.dutchlearner.NOTIFICATION_ALARM" -> {
//                showDailyNotification(context)
//            }
//            "ACTION_LISTEN" -> {
//                // Opcional para futuro
//            }
//        }
        Log.d("DutchLearner", "AlarmReceiver triggered")

        try {
            // Usar notificación inteligente en vez de genérica
            val notificationHelper = NotificationContentGenerator(context)
            //notificationHelper.showSmartNotification()
            //notificationHelper.generateContent()
            Log.d("DutchLearner", "Smart notification shown")
        } catch (e: Exception) {
            Log.e("DutchLearner", "Error showing notification", e)
        }
        if (intent.action == "com.perez.dutchlearner.NOTIFICATION_ALARM") {
            Log.d("DutchLearner", "Alarma recibida, mostrando notificación")

            CoroutineScope(Dispatchers.IO).launch {
                NotificationHelper(context).showPracticeNotification(null)

                // Reprogramar para mañana
                NotificationScheduler(context).rescheduleNextDay()
            }
        }
    }

    private fun showDailyNotification(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Obtener frase del día
                val phrase = getTodaysPhrase(context)

                // Usar NotificationHelper
                val notificationHelper = NotificationHelper(context)
                notificationHelper.showPracticeNotification(phrase)

                Log.d("DutchLearner", "Notificación mostrada")
            } catch (e: Exception) {
                Log.e("DutchLearner", "Error en notificación", e)
            }
        }
    }

    private suspend fun getTodaysPhrase(context: Context): com.perez.dutchlearner.database.PhraseEntity? {
        return withContext(Dispatchers.IO) {
            try {
                val database = com.perez.dutchlearner.database.AppDatabase.getDatabase(context)
                val dao = database.phraseDao()

                // Obtener todas las frases sincrónicamente
                val phrases = dao.getAllPhrasesSync()
                Log.d("DutchLearner", "Tipo de phrases: ${phrases::class.java.simpleName}")

                // Si es una lista, puedes usar:
                if (phrases is List<*>) {
                    val phraseList = phrases as List<com.perez.dutchlearner.database.PhraseEntity>
                    if (phraseList.isNotEmpty()) {
                        phraseList.maxByOrNull { it.unknownWordsCount } ?: phraseList.firstOrNull()
                    } else {
                        null
                    }
                } else {
                    // Si no es una lista, retorna null
                    null
                }
            } catch (e: Exception) {
                Log.e("DutchLearner", "Error obteniendo frase", e)
                null
            }
        }
    }
}
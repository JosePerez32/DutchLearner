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

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.perez.dutchlearner.NOTIFICATION_ALARM") {
            Log.d("DutchLearner", "Alarma recibida, mostrando notificación")

            // Mostrar notificación simple sin frase específica
            CoroutineScope(Dispatchers.IO).launch {
                NotificationHelper(context).showPracticeNotification(null)
            }
        }
        when (intent.action) {
            "com.perez.dutchlearner.NOTIFICATION_ALARM" -> {
                showDailyNotification(context)
            }
            "ACTION_LISTEN" -> {
                // Opcional para futuro
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

                if (phrases.isNotEmpty()) {
                    // Buscar la frase con más palabras desconocidas
                    phrases.maxByOrNull { it.unknownWordsCount } ?: phrases.firstOrNull()
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("DutchLearner", "Error obteniendo frase", e)
                null
            }
        }
    }
}
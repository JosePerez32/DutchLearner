package com.perez.dutchlearner.widget

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.perez.dutchlearner.MainActivity
import com.perez.dutchlearner.R
import com.perez.dutchlearner.audio.AudioRecorderHelper
import com.perez.dutchlearner.database.AppDatabase
import com.perez.dutchlearner.database.PhraseEntity
import com.perez.dutchlearner.language.DutchTokenizer
import com.perez.dutchlearner.speech.VoskSpeechRecognizer
import com.perez.dutchlearner.translation.TranslationServiceProvider
import kotlinx.coroutines.*
import java.io.File

class RecordingForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "recording_channel"
        const val NOTIFICATION_ID = 2001
        const val ACTION_START_RECORDING = "START_RECORDING"
        const val ACTION_STOP_RECORDING = "STOP_RECORDING"
        private const val RECORDING_DURATION_MS = 10000L // 10 segundos
    }

    private var audioRecorderHelper: AudioRecorderHelper? = null
    private var audioFile: File? = null
    private var voskRecognizer: VoskSpeechRecognizer? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var recordingJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Inicializar Vosk en background
        serviceScope.launch(Dispatchers.IO) {
            try {
                voskRecognizer = VoskSpeechRecognizer(applicationContext)
                voskRecognizer?.initModel()
                android.util.Log.d("RecordingService", "Vosk initialized")
            } catch (e: Exception) {
                android.util.Log.e("RecordingService", "Vosk init failed", e)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                startForegroundWithNotification("🎤 Grabando...")
                startRecording()
            }
            ACTION_STOP_RECORDING -> {
                stopRecording()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification(message: String) {
        val notification = createNotification(message)
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun updateNotification(message: String) {
        val notification = createNotification(message)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotification(message: String): Notification {
        // Intent para detener grabación
        val stopIntent = Intent(this, RecordingForegroundService::class.java).apply {
            action = ACTION_STOP_RECORDING
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent para abrir app
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Dutch Learner")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(0, "⏹ Detener", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Grabación de voz",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificación durante grabación de frases"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startRecording() {
        recordingJob = serviceScope.launch(Dispatchers.IO) {
            try {
                // Crear archivo temporal
                audioFile = File(cacheDir, "widget_recording_${System.currentTimeMillis()}.wav")
                audioRecorderHelper = AudioRecorderHelper()

                val success = audioRecorderHelper?.startRecording(audioFile!!) ?: false

                if (!success) {
                    withContext(Dispatchers.Main) {
                        showCompletionNotification("❌ Error al iniciar grabación")
                        stopSelf()
                    }
                    return@launch
                }

                android.util.Log.d("RecordingService", "Recording started")

                // Grabar durante 10 segundos
                delay(RECORDING_DURATION_MS)

                // Detener grabación
                audioRecorderHelper?.stopRecording()
                android.util.Log.d("RecordingService", "Recording stopped")

                // Procesar audio
                processAudio()

            } catch (e: Exception) {
                android.util.Log.e("RecordingService", "Recording error", e)
                withContext(Dispatchers.Main) {
                    showCompletionNotification("❌ Error en grabación")
                    stopSelf()
                }
            }
        }
    }

    private fun stopRecording() {
        recordingJob?.cancel()
        audioRecorderHelper?.stopRecording()
        audioRecorderHelper = null
    }

    private suspend fun processAudio() = withContext(Dispatchers.IO) {
        try {
            // Actualizar notificación
            withContext(Dispatchers.Main) {
                updateNotification("📝 Transcribiendo...")
            }

            // Transcribir
            val transcription = voskRecognizer?.transcribeAudio(audioFile!!)

            if (transcription.isNullOrEmpty()) {
                withContext(Dispatchers.Main) {
                    showCompletionNotification("⚠️ No se detectó audio")
                    stopSelf()
                }
                return@withContext
            }

            android.util.Log.d("RecordingService", "Transcription: $transcription")

            // Traducir
            withContext(Dispatchers.Main) {
                updateNotification("🌐 Traduciendo...")
            }

            val translationService = TranslationServiceProvider.getInstance(applicationContext)
            val translationResult = translationService.translateToNL(transcription)

            var dutchText = ""
            translationResult.onSuccess { dutch ->
                dutchText = dutch
            }.onFailure {
                dutchText = "[Sin traducción]"
            }

            // Guardar en base de datos
            withContext(Dispatchers.Main) {
                updateNotification("💾 Guardando...")
            }

            savePhrase(transcription, dutchText)

            // Notificación de éxito
            withContext(Dispatchers.Main) {
                showCompletionNotification("✅ Frase guardada correctamente")
                stopSelf()
            }

        } catch (e: Exception) {
            android.util.Log.e("RecordingService", "Processing error", e)
            withContext(Dispatchers.Main) {
                showCompletionNotification("❌ Error al procesar")
                stopSelf()
            }
        }
    }

    private suspend fun savePhrase(spanish: String, dutch: String) = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getDatabase(applicationContext)
            val tokenizer = DutchTokenizer()

            // Obtener palabras desconocidas
            val unknownWords = database.unknownWordDao().getAllUnknownWordsSync()
                .filter { !it.learned }

            // Analizar texto
            val analysis = tokenizer.analyzeText(dutch, unknownWords)

            // Crear y guardar frase
            val phrase = PhraseEntity(
                spanishText = spanish,
                dutchText = dutch,
                unknownWordsCount = analysis.unknownCount,
                unknownWords = analysis.unknownWords.joinToString(",")
            )

            database.phraseDao().insertPhrase(phrase)

            // Incrementar contador de palabras vistas
            analysis.unknownWords.forEach { word ->
                database.unknownWordDao().incrementWordSeen(word)
            }

            android.util.Log.d("RecordingService", "Phrase saved successfully")

        } catch (e: Exception) {
            android.util.Log.e("RecordingService", "Save error", e)
        }
    }

    private fun showCompletionNotification(message: String) {
        // Detener foreground
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        // Mostrar notificación de resultado
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Dutch Learner")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
        serviceScope.cancel()
        voskRecognizer?.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
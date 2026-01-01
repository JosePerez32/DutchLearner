package com.perez.dutchlearner.widget

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
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

class RecordingService : Service() {

    companion object {
        const val CHANNEL_ID = "recording_channel"
        const val NOTIFICATION_ID = 2001
        const val ACTION_START_RECORDING = "START_RECORDING"
        const val ACTION_STOP_RECORDING = "STOP_RECORDING"

        private const val RECORDING_DURATION_MS = 10_000L // 10 segundos
    }

    private var audioRecorderHelper: AudioRecorderHelper? = null
    private var audioFile: File? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var voskRecognizer: VoskSpeechRecognizer? = null
    private val tokenizer = DutchTokenizer()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Inicializar Vosk en background
        serviceScope.launch(Dispatchers.IO) {
            try {
                voskRecognizer = VoskSpeechRecognizer(this@RecordingService)
                voskRecognizer?.initModel()
                Log.d("RecordingService", "Vosk initialized")
            } catch (e: Exception) {
                Log.e("RecordingService", "Vosk init failed", e)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> startRecording()
            ACTION_STOP_RECORDING -> stopRecordingAndProcess()
        }
        return START_NOT_STICKY
    }

    private fun startRecording() {
        // Mostrar notificación de foreground
        val notification = createRecordingNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Actualizar widget a estado "grabando"
        QuickRecordWidget.updateWidgetState(this, isRecording = true)

        serviceScope.launch(Dispatchers.IO) {
            try {
                audioFile = File(cacheDir, "widget_recording_${System.currentTimeMillis()}.wav")
                audioRecorderHelper = AudioRecorderHelper()

                val success = audioRecorderHelper?.startRecording(audioFile!!) ?: false

                if (success) {
                    Log.d("RecordingService", "Recording started")

                    // Auto-detener después de 10 segundos
                    delay(RECORDING_DURATION_MS)
                    stopRecordingAndProcess()
                } else {
                    Log.e("RecordingService", "Failed to start recording")
                    stopSelf()
                }
            } catch (e: Exception) {
                Log.e("RecordingService", "Recording error", e)
                stopSelf()
            }
        }
    }

    private fun stopRecordingAndProcess() {
        serviceScope.launch(Dispatchers.IO) {
            try {
                // Detener grabación
                audioRecorderHelper?.stopRecording()
                audioRecorderHelper = null

                // Actualizar notificación
                updateNotification("⏳ Procesando audio...")

                // Transcribir
                val spanish = transcribeAudio()

                if (spanish.isEmpty() || spanish.startsWith("Error")) {
                    updateNotification("❌ No se detectó audio")
                    delay(2000)
                    stopServiceAndUpdateWidget()
                    return@launch
                }

                // Traducir
                updateNotification("🌐 Traduciendo...")
                val translationService = TranslationServiceProvider.getInstance(this@RecordingService)
                val result = translationService.translateToNL(spanish)

                var dutch = "[Sin traducción]"
                result.onSuccess { dutch = it }

                // Guardar frase
                savePhraseToDatabase(spanish, dutch)

                // Notificación de éxito
                updateNotification("✅ Frase guardada: \"${spanish.take(50)}...\"")
                delay(3000)

                stopServiceAndUpdateWidget()

            } catch (e: Exception) {
                Log.e("RecordingService", "Processing error", e)
                updateNotification("❌ Error al procesar")
                delay(2000)
                stopServiceAndUpdateWidget()
            }
        }
    }

    private suspend fun transcribeAudio(): String = withContext(Dispatchers.IO) {
        return@withContext try {
            if (voskRecognizer == null) {
                Log.w("RecordingService", "Vosk not initialized")
                ""
            } else {
                audioFile?.let { file ->
                    val transcription = voskRecognizer?.transcribeAudio(file)
                    if (transcription.isNullOrEmpty()) {
                        Log.w("RecordingService", "Vosk returned empty")
                        ""
                    } else {
                        Log.d("RecordingService", "Transcribed: $transcription")
                        transcription
                    }
                } ?: "Error: no audio file"
            }
        } catch (e: Exception) {
            Log.e("RecordingService", "Transcription error", e)
            "Error al transcribir audio"
        }
    }

    private suspend fun savePhraseToDatabase(spanish: String, dutch: String) {
        withContext(Dispatchers.IO) {
            try {
                val database = AppDatabase.getDatabase(this@RecordingService)

                // Obtener palabras DESCONOCIDAS
                val unknownWords = database.unknownWordDao().getAllUnknownWordsSync()
                    .filter { !it.learned }

                // Analizar texto holandés
                val analysis = tokenizer.analyzeText(dutch, unknownWords)

                // Crear entidad de frase
                val phrase = PhraseEntity(
                    spanishText = spanish,
                    dutchText = dutch,
                    unknownWordsCount = analysis.unknownCount,
                    unknownWords = analysis.unknownWords.joinToString(",")
                )

                // Guardar frase
                database.phraseDao().insertPhrase(phrase)

                // Incrementar contador de palabras desconocidas vistas
                analysis.unknownWords.forEach { word ->
                    database.unknownWordDao().incrementWordSeen(word)
                }

                Log.d("RecordingService", "Phrase saved: ${analysis.unknownCount} unknown words")
            } catch (e: Exception) {
                Log.e("RecordingService", "Error saving phrase", e)
            }
        }
    }

    private fun createRecordingNotification(): Notification {
        val stopIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_STOP_RECORDING
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎤 Grabando...")
            .setContentText("Habla en español (10 segundos)")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .addAction(0, "⏹ Detener", stopPendingIntent)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Dutch Learner")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Grabación de audio",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación durante la grabación"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun stopServiceAndUpdateWidget() {
        QuickRecordWidget.updateWidgetState(this, isRecording = false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        voskRecognizer?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
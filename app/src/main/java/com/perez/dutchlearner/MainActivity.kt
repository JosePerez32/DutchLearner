package com.perez.dutchlearner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.perez.dutchlearner.audio.AudioRecorderHelper
import com.perez.dutchlearner.database.AppDatabase
import com.perez.dutchlearner.database.PhraseEntity
import com.perez.dutchlearner.translation.TranslationServiceProvider
import com.perez.dutchlearner.ui.PhrasesScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var audioRecorderHelper: AudioRecorderHelper? = null
    private var audioFile: File? = null
    private var isRecording = false
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    // Vosk
    private var voskRecognizer: com.perez.dutchlearner.speech.VoskSpeechRecognizer? = null
    private var voskInitialized = false

    // Servicios
    private val translationService by lazy {
        TranslationServiceProvider.getInstance(this)
    }
    private val database by lazy {
        try {
            AppDatabase.getDatabase(this)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private val recordPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Permiso de micrófono necesario", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        android.util.Log.d("DutchLearner", "MainActivity onCreate started")

        try {
            // Inicializar TTS
            tts = TextToSpeech(this, this)
            android.util.Log.d("DutchLearner", "TTS initialized")

            // Inicializar Vosk en background
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        voskRecognizer = com.perez.dutchlearner.speech.VoskSpeechRecognizer(this@MainActivity)
                        voskInitialized = voskRecognizer?.initModel() ?: false
                        android.util.Log.d("DutchLearner", "Vosk initialized: $voskInitialized")
                    } catch (e: Exception) {
                        android.util.Log.e("DutchLearner", "Vosk init failed", e)
                    }
                }
            }

            // Solicitar permisos
            checkPermissions()
            android.util.Log.d("DutchLearner", "Permissions checked")

            setContent {
                MaterialTheme {
                    AppNavigation()
                }
            }
            android.util.Log.d("DutchLearner", "UI set successfully")
        } catch (e: Exception) {
            android.util.Log.e("DutchLearner", "Error in onCreate", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    @Composable
    fun AppNavigation() {
        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = "recorder") {
            composable("recorder") {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RecorderScreen(
                        onNavigateToPhrases = {
                            navController.navigate("phrases")
                        }
                    )
                }
            }

            composable("phrases") {
                val phrases by database?.phraseDao()?.getAllPhrasesByDate()?.collectAsState(initial = emptyList())
                    ?: remember { mutableStateOf(emptyList()) }

                PhrasesScreen(
                    phrases = phrases,
                    onNavigateBack = { navController.popBackStack() },
                    onDeletePhrase = { phrase ->
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                database?.phraseDao()?.deletePhrase(phrase)
                            }
                        }
                    },
                    onSpeakDutch = { text -> speakDutch(text) },
                    ttsReady = ttsReady
                )
            }
        }
    }

    @Composable
    fun RecorderScreen(onNavigateToPhrases: () -> Unit) {
        var isRecordingState by remember { mutableStateOf(false) }
        var transcribedText by remember { mutableStateOf("") }
        var translatedText by remember { mutableStateOf("") }
        var isProcessing by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var statusMessage by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header con botón de frases
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🇳🇱 Dutch Learner",
                    style = MaterialTheme.typography.headlineMedium
                )

                IconButton(
                    onClick = onNavigateToPhrases
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Ver frases guardadas",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (statusMessage.isNotEmpty()) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Botón de grabación
            Button(
                onClick = {
                    if (!isRecordingState) {
                        startRecording()
                        isRecordingState = true
                        errorMessage = null
                        statusMessage = "🎤 Grabando..."
                    } else {
                        stopRecording()
                        isRecordingState = false
                        isProcessing = true
                        statusMessage = "⏳ Procesando audio..."

                        // Procesar audio
                        lifecycleScope.launch {
                            try {
                                // Paso 1: Transcribir
                                statusMessage = "📝 Transcribiendo..."
                                val spanish = transcribeAudio()

                                if (spanish.isEmpty() || spanish.startsWith("Error")) {
                                    errorMessage = spanish.ifEmpty { "No se pudo transcribir el audio" }
                                    isProcessing = false
                                    statusMessage = ""
                                    return@launch
                                }

                                transcribedText = spanish

                                // Paso 2: Traducir
                                statusMessage = "🌐 Traduciendo..."
                                val result = translationService.translateToNL(spanish)

                                result.onSuccess { dutch ->
                                    translatedText = dutch
                                    errorMessage = null
                                    statusMessage = "✅ ¡Listo!"
                                }.onFailure { e ->
                                    errorMessage = "Error de traducción: ${e.message}"
                                    statusMessage = ""
                                }

                            } catch (e: Exception) {
                                errorMessage = "Error: ${e.message}"
                                statusMessage = ""
                            } finally {
                                isProcessing = false
                            }
                        }
                    }
                },
                modifier = Modifier.size(120.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecordingState)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                ),
                enabled = !isProcessing
            ) {
                Text(
                    text = if (isRecordingState) "⏹\nDetener" else "🎤\nGrabar",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (isProcessing) {
                CircularProgressIndicator()
            }

            // Mensajes de error
            if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage!!,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Resultados
            if (transcribedText.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🇪🇸 Español:",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = transcribedText,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            if (translatedText.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🇳🇱 Nederlands:",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = translatedText,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { speakDutch(translatedText) },
                            enabled = ttsReady
                        ) {
                            Text("🔊 Escuchar pronunciación")
                        }
                    }
                }

                Button(
                    onClick = {
                        lifecycleScope.launch {
                            savePhraseToDatabase(transcribedText, translatedText)
                            Toast.makeText(
                                this@MainActivity,
                                "✅ Frase guardada",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("💾 Guardar frase")
                }
            }
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startRecording() {
        lifecycleScope.launch {
            try {
                audioFile = File(cacheDir, "recording_${System.currentTimeMillis()}.wav")
                audioRecorderHelper = AudioRecorderHelper()

                val success = audioRecorderHelper?.startRecording(audioFile!!) ?: false

                if (success) {
                    isRecording = true
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            "Error al iniciar grabación",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun stopRecording() {
        try {
            audioRecorderHelper?.stopRecording()
            audioRecorderHelper = null
            isRecording = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun transcribeAudio(): String = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!voskInitialized || voskRecognizer == null) {
                android.util.Log.w("DutchLearner", "Vosk not initialized, using placeholder")
                "Hola, quiero aprender holandés" // Fallback
            } else {
                audioFile?.let { file ->
                    val transcription = voskRecognizer?.transcribeAudio(file)
                    if (transcription.isNullOrEmpty()) {
                        android.util.Log.w("DutchLearner", "Vosk returned empty, using placeholder")
                        "Hola, quiero aprender holandés"
                    } else {
                        android.util.Log.d("DutchLearner", "Transcribed: $transcription")
                        transcription
                    }
                } ?: "Error: no audio file"
            }
        } catch (e: Exception) {
            android.util.Log.e("DutchLearner", "Transcription error", e)
            "Error al transcribir audio"
        }
    }

    private suspend fun savePhraseToDatabase(spanish: String, dutch: String) {
        withContext(Dispatchers.IO) {
            try {
                database?.let { db ->
                    val phrase = PhraseEntity(
                        spanishText = spanish,
                        dutchText = dutch,
                        unknownWordsCount = 0, // TODO: calcular palabras desconocidas
                        unknownWords = ""
                    )
                    db.phraseDao().insertPhrase(phrase)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("nl", "NL"))
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED

            if (!ttsReady) {
                Toast.makeText(
                    this,
                    "Por favor instala voces en holandés:\nConfiguración > Idioma > Texto a voz",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun speakDutch(text: String) {
        if (ttsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            Toast.makeText(
                this,
                "Voces en holandés no disponibles",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        voskRecognizer?.release()
        super.onDestroy()
    }
}
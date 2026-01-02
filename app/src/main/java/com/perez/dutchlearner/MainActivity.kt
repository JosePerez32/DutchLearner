package com.perez.dutchlearner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.perez.dutchlearner.audio.AudioRecorderHelper
import com.perez.dutchlearner.database.AppDatabase
import com.perez.dutchlearner.database.PhraseEntity
import com.perez.dutchlearner.language.DutchTokenizer
import com.perez.dutchlearner.notifications.NotificationScheduler
import com.perez.dutchlearner.translation.TranslationServiceProvider
import com.perez.dutchlearner.ui.PhrasesScreen
import com.perez.dutchlearner.ui.UnknownWordsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    companion object {
        private const val REQUEST_CODE = 100
    }
    private var audioRecorderHelper: AudioRecorderHelper? = null
    private var audioFile: File? = null
    private var isRecording = false
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    // Vosk
    private var voskRecognizer: com.perez.dutchlearner.speech.VoskSpeechRecognizer? = null
    private var voskInitialized = false

    // Tokenizador
    private val tokenizer = DutchTokenizer()

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
            // Manejar grabación rápida desde widget
            if (intent?.action == "com.perez.dutchlearner.ACTION_QUICK_RECORD") {
                // Esperar a que la UI esté lista
                lifecycleScope.launch {
                    kotlinx.coroutines.delay(500)
                    // Auto-iniciar grabación
                    startRecording()
                    Toast.makeText(
                        this@MainActivity,
                        "🎤 Grabación iniciada desde widget",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            intent?.extras?.let { extras ->
                if (extras.getString("action") == "speak") {
                    val textToSpeak = extras.getString("text_to_speak")
                    textToSpeak?.let { text ->
                        // Esperar a que TTS se inicialice
                        lifecycleScope.launch {
                            kotlinx.coroutines.delay(1000) // Esperar 1 segundo
                            speakDutch(text)
                        }
                    }
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
                        onNavigateToPhrases = { navController.navigate("phrases") },
                        onNavigateToVocabulary = { navController.navigate("vocabulary") },
                        onNavigateToSettings = { navController.navigate("settings") }
                    )
                }
            }

            composable("phrases") {
                val phrases by database?.phraseDao()?.getAllPhrasesByDate()
                    ?.collectAsState(initial = emptyList())
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
                    onWordTap = { word ->
                        lifecycleScope.launch {
                            addWordToUnknown(word)
                        }
                    },
                    onRetryTranslation = { phrase ->
                        lifecycleScope.launch {
                            retryTranslation(phrase)
                        }
                    },
                    ttsReady = ttsReady
                )
            }

            composable("vocabulary") {
                val unknownWords by database?.unknownWordDao()?.getAllUnknownWords()
                    ?.collectAsState(initial = emptyList())
                    ?: remember { mutableStateOf(emptyList()) }

                UnknownWordsScreen(
                    unknownWords = unknownWords,
                    onNavigateBack = { navController.popBackStack() },
                    onAddWord = { word ->
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                database?.unknownWordDao()?.insertWord(
                                    com.perez.dutchlearner.database.UnknownWordEntity(word = word)
                                )
                            }
                            Toast.makeText(
                                this@MainActivity,
                                "✅ Palabra agregada a desconocidas",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onMarkAsLearned = { word ->
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                database?.unknownWordDao()?.markAsLearned(word.word)
                            }
                            Toast.makeText(
                                this@MainActivity,
                                "🎉 ¡Palabra aprendida!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onDeleteWord = { word ->
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                database?.unknownWordDao()?.deleteWord(word)
                            }
                        }
                    },
                    onSpeakWord = { word ->  // ⬅️ NUEVO
                        speakDutch(word)  // Usa tu función existente
                        Toast.makeText(
                            this@MainActivity,
                            "🔊 Pronunciando: $word",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }

            composable("settings") {
                com.perez.dutchlearner.ui.SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAlarms = { navController.navigate("alarms") }
                )
            }

            composable("alarms") {
                val alarms by database?.alarmDao()?.getAllAlarms()
                    ?.collectAsState(initial = emptyList())
                    ?: remember { mutableStateOf(emptyList()) }

                com.perez.dutchlearner.ui.AlarmsScreen(
                    alarms = alarms,
                    onNavigateBack = { navController.popBackStack() },
                    onAddAlarm = { hour, minute ->
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                val alarm = com.perez.dutchlearner.database.AlarmEntity(
                                    hour = hour,
                                    minute = minute
                                )
                                val id = database?.alarmDao()?.insertAlarm(alarm)

                                // Programar la alarma
                                id?.let {
                                    val newAlarm = database?.alarmDao()?.getAlarmById(it)
                                    newAlarm?.let { a ->
                                        com.perez.dutchlearner.notifications.MultiAlarmScheduler(
                                            this@MainActivity
                                        )
                                            .scheduleAlarm(a)
                                    }
                                }
                            }
                            Toast.makeText(
                                this@MainActivity,
                                "✅ Alarma agregada",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onToggleAlarm = { alarm, enabled ->
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                database?.alarmDao()?.setAlarmEnabled(alarm.id, enabled)

                                val scheduler =
                                    com.perez.dutchlearner.notifications.MultiAlarmScheduler(this@MainActivity)
                                if (enabled) {
                                    scheduler.scheduleAlarm(alarm.copy(enabled = true))
                                } else {
                                    scheduler.cancelAlarm(alarm.id)
                                }
                            }
                        }
                    },
                    onEditAlarm = { alarm, newHour, newMinute ->  // ⬅️ 3 parámetros!
                        // Implementar edición de alarma
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                // Cancelar alarma vieja
                                com.perez.dutchlearner.notifications.MultiAlarmScheduler(this@MainActivity)
                                    .cancelAlarm(alarm.id)

                                // Actualizar en base de datos
                                val updatedAlarm = alarm.copy(hour = newHour, minute = newMinute)
                                database?.alarmDao()?.updateAlarm(updatedAlarm)

                                // Reprogramar con nueva hora
                                com.perez.dutchlearner.notifications.MultiAlarmScheduler(this@MainActivity)
                                    .scheduleAlarm(updatedAlarm)
                            }

                            // Mostrar confirmación
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "⏰ Alarma actualizada a $newHour:$newMinute",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    onDeleteAlarm = { alarm ->
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                // Cancelar alarma primero
                                com.perez.dutchlearner.notifications.MultiAlarmScheduler(this@MainActivity)
                                    .cancelAlarm(alarm.id)

                                // Eliminar de BD
                                database?.alarmDao()?.deleteAlarm(alarm)
                            }
                            Toast.makeText(
                                this@MainActivity,
                                "🗑️ Alarma eliminada",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            ), REQUEST_CODE)
        }
    }
    @Composable
    fun RecorderScreen(
        onNavigateToPhrases: () -> Unit,
        onNavigateToVocabulary: () -> Unit,
        onNavigateToSettings: () -> Unit
    ) {
        var isRecordingState by remember { mutableStateOf(false) }
        var transcribedText by remember { mutableStateOf("") }
        var translatedText by remember { mutableStateOf("") }
        var isProcessing by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var statusMessage by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
                //.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "🇳🇱 Dutch Learner",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // ⬇️ BOTONES GRANDES (NUEVO)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botón Vocabulario
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp)
                        .clickable { onNavigateToVocabulary() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Palabras",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Botón Frases
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp)
                        .clickable { onNavigateToPhrases() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Frases",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Botón Configuración
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp)
                        .clickable { onNavigateToSettings() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ajustes",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (statusMessage.isNotEmpty()) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                                    // ⬇️ NUEVO: Manejo de error sin internet
                                    if (e.message?.contains("Unable to resolve host") == true) {
                                        errorMessage = "⚠️ Sin internet. Guardando solo en español..."
                                        translatedText = "" // Vacío = no traducido
                                        statusMessage = ""
                                    } else {
                                        errorMessage = "Error de traducción: ${e.message}"
                                        statusMessage = ""
                                    }
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
            } else if (transcribedText.isNotEmpty() && errorMessage?.contains("Sin internet") == true) {
                // ⬇️ NUEVO: Permitir guardar sin traducción
                Button(
                    onClick = {
                        lifecycleScope.launch {
                            savePhraseToDatabase(transcribedText, "[Sin traducción]")
                            Toast.makeText(
                                this@MainActivity,
                                "✅ Frase guardada (sin traducción)",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("💾 Guardar solo en español")
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
                android.util.Log.w("DutchLearner", "Vosk not initialized")
                "" // Devolver vacío en vez de placeholder
            } else {
                audioFile?.let { file ->
                    val transcription = voskRecognizer?.transcribeAudio(file)
                    if (transcription.isNullOrEmpty()) {
                        android.util.Log.w("DutchLearner", "Vosk returned empty")
                        ""
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
                    // Obtener palabras DESCONOCIDAS
                    val unknownWords = db.unknownWordDao().getAllUnknownWords().firstOrNull() ?: emptyList()

                    // Analizar texto holandés (lógica invertida)
                    val analysis = tokenizer.analyzeText(dutch, unknownWords)

                    // Crear entidad de frase
                    val phrase = PhraseEntity(
                        spanishText = spanish,
                        dutchText = dutch,
                        unknownWordsCount = analysis.unknownCount,
                        unknownWords = analysis.unknownWords.joinToString(",")
                    )

                    // Guardar frase
                    db.phraseDao().insertPhrase(phrase)

                    // Incrementar contador de palabras desconocidas vistas
                    analysis.unknownWords.forEach { word ->
                        db.unknownWordDao().incrementWordSeen(word)
                    }

                    android.util.Log.d("DutchLearner",
                        "Phrase saved: ${analysis.unknownCount} unknown words, ${analysis.knownCount} known")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onInit(status: Int) {
//        if (status == TextToSpeech.SUCCESS) {
//            val result = tts?.setLanguage(Locale("nl", "NL"))
//            ttsReady = result != TextToSpeech.LANG_MISSING_DATA &&
//                    result != TextToSpeech.LANG_NOT_SUPPORTED
//
//            if (!ttsReady) {
//                Toast.makeText(
//                    this,
//                    "Por favor instala voces en holandés:\nConfiguración > Idioma > Texto a voz",
//                    Toast.LENGTH_LONG
//                ).show()
//            }
//        }
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("nl", "NL"))
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED

            if (ttsReady) {
                // Ajustar velocidad (0.5 = lento, 1.0 = normal, 1.5 = rápido)
                tts?.setSpeechRate(0.75f) // 25% más lento que normal

                // Ajustar tono (opcional)
                tts?.setPitch(1.0f) // 1.0 = normal
            } else {
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
    private suspend fun addWordToUnknown(word: String) {
        withContext(Dispatchers.IO) {
            try {
                val normalized = word.lowercase().trim()

                if (normalized.length < 2) return@withContext // Ignorar palabras muy cortas

                database?.let { db ->
                    val existing = db.unknownWordDao().getWord(normalized)

                    if (existing != null) {
                        // Ya existe, incrementar contador
                        db.unknownWordDao().incrementWordSeen(normalized)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@MainActivity,
                                "↗️ '$normalized' contador actualizado",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        // Nueva palabra, calcular dificultad
                        val difficulty = when {
                            normalized.length <= 4 -> 0  // Fácil
                            normalized.length <= 8 -> 1  // Media
                            else -> 2                     // Difícil
                        }

                        db.unknownWordDao().insertWord(
                            com.perez.dutchlearner.database.UnknownWordEntity(
                                word = normalized,
                                difficulty = difficulty
                            )
                        )

                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@MainActivity,
                                "✅ '$normalized' agregada a desconocidas",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    // Actualizar contadores de todas las frases
                    refreshPhraseCounts()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Error al agregar palabra",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    private suspend fun refreshPhraseCounts() {
        withContext(Dispatchers.IO) {
            try {
                database?.let { db ->
                    val allPhrases = db.phraseDao().getAllPhrasesSync()
                    val unknownWords = db.unknownWordDao().getAllUnknownWordsSync()
                        .filter { !it.learned }
                        .map { it.word }
                        .toSet()

                    allPhrases.forEach { phrase ->
                        val analysis = tokenizer.analyzeText(phrase.dutchText,
                            unknownWords.map {
                                com.perez.dutchlearner.database.UnknownWordEntity(word = it)
                            }
                        )

                        db.phraseDao().updatePhrase(
                            phrase.copy(
                                unknownWordsCount = analysis.unknownCount,
                                unknownWords = analysis.unknownWords.joinToString(",")
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        voskRecognizer?.release()
        super.onDestroy()
    }

    private suspend fun retryTranslation(phrase: PhraseEntity) {
        try {
            withContext(Dispatchers.IO) {
                // Intentar traducir de nuevo
                val result = translationService.translateToNL(phrase.spanishText)

                result.onSuccess { dutchTranslation ->
                    // Actualizar solo el texto holandés
                    database?.phraseDao()?.updatePhrase(
                        phrase.copy(
                            dutchText = dutchTranslation,
                            unknownWordsCount = 0, // Resetear porque ahora es diferente
                            unknownWords = ""
                        )
                    )

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            "✅ Traducción completada",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }.onFailure { error ->
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            "❌ Error: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}


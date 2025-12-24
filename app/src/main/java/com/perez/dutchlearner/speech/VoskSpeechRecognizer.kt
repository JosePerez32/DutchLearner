package com.perez.dutchlearner.speech

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService
import java.io.File
import java.io.FileInputStream

/**
 * Servicio de reconocimiento de voz offline usando Vosk
 */
class VoskSpeechRecognizer(private val context: Context) {

    private var model: Model? = null
    private var recognizer: Recognizer? = null

    /**
     * Inicializa el modelo de Vosk
     * Debe llamarse antes de usar el reconocedor
     */
    suspend fun initModel(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Ruta donde copiaremos el modelo
            val modelPath = File(context.filesDir, "model-es")

            if (!modelPath.exists()) {
                // Copiar modelo de assets la primera vez (puede tardar)
                copyModelFromAssets(modelPath)
            }

            // Verificar que el modelo se copió correctamente
            if (!File(modelPath, "conf/model.conf").exists()) {
                throw Exception("Modelo no se copió correctamente")
            }

            // Inicializar modelo de Vosk
            model = Model(modelPath.absolutePath)

            // Crear reconocedor (16000 Hz es el estándar para Vosk)
            recognizer = Recognizer(model, 16000.0f)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Transcribe un archivo de audio a texto
     * @param audioFile Archivo de audio en formato WAV (16kHz, mono)
     * @return Texto transcrito o null si falla
     */
    suspend fun transcribeAudio(audioFile: File): String? = withContext(Dispatchers.IO) {
        try {
            if (recognizer == null) {
                throw IllegalStateException("Model not initialized. Call initModel() first.")
            }

            // Convertir audio a formato compatible si es necesario
            val wavFile = convertToWav(audioFile)

            FileInputStream(wavFile).use { audioStream ->
                // Leer audio en chunks
                val buffer = ByteArray(4096)
                var bytesRead: Int

                while (audioStream.read(buffer).also { bytesRead = it } >= 0) {
                    recognizer?.acceptWaveForm(buffer, bytesRead)
                }

                // Obtener resultado final
                val result = recognizer?.finalResult

                // Parsear JSON (Vosk devuelve JSON)
                // Formato: {"text": "transcripción aquí"}
                val text = result?.let { parseVoskResult(it) }

                // Reset recognizer para próximo uso
                recognizer?.reset()

                text
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Convierte audio a formato WAV compatible con Vosk
     * Android MediaRecorder graba en 3GP/AMR, necesitamos convertir
     */
    private fun convertToWav(inputFile: File): File {
        // Por ahora retornamos el mismo archivo
        // TODO: Implementar conversión usando FFmpeg o AudioRecord en lugar de MediaRecorder

        // NOTA: Para producción, es mejor usar AudioRecord directamente
        // para grabar en formato PCM compatible con Vosk
        return inputFile
    }

    /**
     * Parsea el resultado JSON de Vosk
     */
    private fun parseVoskResult(json: String): String? {
        return try {
            // Extraer texto del JSON manualmente (simple)
            val textStart = json.indexOf("\"text\" : \"") + 10
            val textEnd = json.indexOf("\"", textStart)

            if (textStart > 9 && textEnd > textStart) {
                json.substring(textStart, textEnd)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Copia el modelo de Vosk desde assets al almacenamiento interno
     * IMPORTANTE: Asegúrate de que la estructura sea:
     * assets/model-es/am/
     * assets/model-es/conf/
     * etc... (sin carpeta vosk-model-es-0.42 en medio)
     */
    private fun copyModelFromAssets(targetDir: File) {
        targetDir.mkdirs()

        // Copiar todos los archivos y carpetas del modelo recursivamente
        copyAssetFolder("model-es", targetDir)
    }

    /**
     * Copia una carpeta de assets recursivamente
     */
    private fun copyAssetFolder(assetPath: String, targetDir: File) {
        val assetManager = context.assets
        val files = assetManager.list(assetPath) ?: return

        for (filename in files) {
            val assetFilePath = "$assetPath/$filename"
            val targetFile = File(targetDir, filename)

            // Verificar si es carpeta o archivo
            val subFiles = assetManager.list(assetFilePath)
            if (subFiles != null && subFiles.isNotEmpty()) {
                // Es una carpeta, copiar recursivamente
                targetFile.mkdirs()
                copyAssetFolder(assetFilePath, targetFile)
            } else {
                // Es un archivo, copiarlo
                assetManager.open(assetFilePath).use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    /**
     * Libera recursos
     */
    fun release() {
        recognizer?.close()
        model?.close()
        recognizer = null
        model = null
    }
}

/**
 * ALTERNATIVA SIMPLIFICADA PARA EMPEZAR
 * Usa Android SpeechRecognizer (requiere Google Services pero es más simple)
 */
class AndroidSpeechRecognizer(private val context: Context) {

    suspend fun transcribe(audioFile: File): String {
        // Por ahora retornamos un placeholder
        // En la próxima iteración implementaremos esto o Vosk
        return "Texto transcrito de ejemplo"
    }
}
package com.perez.dutchlearner.notifications

import android.content.Context
import com.perez.dutchlearner.database.AppDatabase
import com.perez.dutchlearner.database.PhraseEntity
import com.perez.dutchlearner.database.UnknownWordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Generador de contenido aleatorio para notificaciones
 * - 50% Palabras individuales (3 random)
 * - 50% Frase completa con palabras desconocidas
 */
class NotificationContentGenerator(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)

    /**
     * Genera contenido aleatorio para la notificación
     * @return NotificationContent con título, texto y data
     */
    suspend fun generateContent(): NotificationContent = withContext(Dispatchers.IO) {
        val unknownWords = database.unknownWordDao().getAllUnknownWordsSync()
            .filter { !it.learned }

        val phrases = database.phraseDao().getAllPhrasesSync()
            .filter { it.unknownWordsCount > 0 }

        // Si no hay datos, contenido por defecto
        if (unknownWords.isEmpty() && phrases.isEmpty()) {
            return@withContext NotificationContent(
                title = "🇳🇱 Momento de practicar",
                text = "Graba una nueva frase para empezar a aprender vocabulario",
                type = ContentType.DEFAULT,
                audioText = null
            )
        }

        // Decidir aleatoriamente qué tipo de contenido mostrar
        val useWords = Random.nextBoolean()

        if (useWords && unknownWords.isNotEmpty()) {
            generateWordsContent(unknownWords)
        } else if (phrases.isNotEmpty()) {
            generatePhraseContent(phrases, unknownWords)
        } else if (unknownWords.isNotEmpty()) {
            // Fallback: si no hay frases pero sí palabras
            generateWordsContent(unknownWords)
        } else {
            // Fallback final
            NotificationContent(
                title = "🇳🇱 Momento de practicar",
                text = "Graba una nueva frase para continuar aprendiendo",
                type = ContentType.DEFAULT,
                audioText = null
            )
        }
    }

    /**
     * Genera contenido con 3 palabras aleatorias + traducción
     */
    private suspend fun generateWordsContent(
        unknownWords: List<UnknownWordEntity>
    ): NotificationContent = withContext(Dispatchers.IO) {
        // Seleccionar 3 palabras aleatorias (o menos si no hay suficientes)
        val selectedWords = unknownWords
            .shuffled()
            .take(3)

        // Traducir cada palabra usando DeepL
        val translationService = com.perez.dutchlearner.translation.DeepLTranslationService(context)

        val wordsWithTranslations = mutableListOf<Pair<String, String>>()

        selectedWords.forEach { word ->
            try {
                val result = translationService.translateToNL(word.word)
                result.onSuccess { translation ->
                    wordsWithTranslations.add(word.word to translation)
                }.onFailure {
                    // Si falla, usar placeholder
                    wordsWithTranslations.add(word.word to "[sin traducción]")
                }
            } catch (e: Exception) {
                wordsWithTranslations.add(word.word to "[error]")
            }
        }

        // Formatear texto
        val formattedText = wordsWithTranslations.joinToString("\n") { (dutch, spanish) ->
            "🇳🇱 $dutch  →  🇪🇸 $spanish"
        }

        // Texto para TTS (solo palabras en holandés)
        val audioText = wordsWithTranslations.joinToString(", ") { it.first }

        NotificationContent(
            title = "📚 Palabras para repasar (${selectedWords.size})",
            text = formattedText,
            type = ContentType.WORDS,
            audioText = audioText
        )
    }

    /**
     * Genera contenido con 1 frase completa
     */
    private suspend fun generatePhraseContent(
        phrases: List<PhraseEntity>,
        unknownWords: List<UnknownWordEntity>
    ): NotificationContent = withContext(Dispatchers.IO) {
        // Seleccionar frase aleatoria con palabras desconocidas
        val phrase = phrases
            .sortedByDescending { it.unknownWordsCount }
            .take(10) // Top 10 más difíciles
            .random()

        // Obtener palabras desconocidas de esta frase
        val unknownSet = unknownWords.map { it.word.lowercase() }.toSet()
        val wordsInPhrase = phrase.unknownWords.split(",").filter { it.isNotBlank() }

        // Resaltar palabras desconocidas
        val highlightedDutch = phrase.dutchText
        val unknownCount = wordsInPhrase.size

        val formattedText = buildString {
            appendLine("🇪🇸 ${phrase.spanishText}")
            appendLine()
            appendLine("🇳🇱 $highlightedDutch")
            appendLine()
            if (unknownCount > 0) {
                appendLine("⚠️ $unknownCount ${if (unknownCount == 1) "palabra" else "palabras"} nuevas:")
                appendLine(wordsInPhrase.take(5).joinToString(", "))
            }
        }

        NotificationContent(
            title = "💬 Frase para practicar",
            text = formattedText,
            type = ContentType.PHRASE,
            audioText = phrase.dutchText
        )
    }
}

/**
 * Contenedor de contenido para notificación
 */
data class NotificationContent(
    val title: String,
    val text: String,
    val type: ContentType,
    val audioText: String? // Texto para reproducir con TTS (si hay botón escuchar)
)

enum class ContentType {
    DEFAULT,   // Mensaje genérico
    WORDS,     // 3 palabras + traducción
    PHRASE     // Frase completa
}
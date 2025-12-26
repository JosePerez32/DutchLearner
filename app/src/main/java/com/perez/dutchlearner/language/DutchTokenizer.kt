package com.perez.dutchlearner.language

import com.perez.dutchlearner.database.KnownWordEntity

/**
 * Tokenizador para texto en holandés
 * Separa palabras y las normaliza
 */
class DutchTokenizer {

    /**
     * Tokeniza un texto en holandés en palabras individuales
     *
     * @param text Texto en holandés
     * @return Lista de palabras normalizadas
     */
    fun tokenize(text: String): List<String> {
        return text
            .lowercase()
            // Mantener letras, números, guiones y apóstrofes
            .replace(Regex("[^a-zá-ÿ0-9\\s'-]"), " ")
            // Separar por espacios
            .split(Regex("\\s+"))
            // Filtrar palabras vacías o muy cortas
            .filter { it.length >= 2 }
            // Remover guiones/apóstrofes al inicio y final
            .map { it.trim('-', '\'') }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    /**
     * Analiza qué palabras de un texto son desconocidas
     *
     * @param text Texto en holandés
     * @param knownWords Lista de palabras que ya conoces
     * @return Par: (palabras desconocidas, palabras conocidas)
     */
    fun analyzeText(text: String, knownWords: List<KnownWordEntity>): WordAnalysis {
        val tokens = tokenize(text)
        val knownSet = knownWords.map { it.word.lowercase() }.toSet()

        val unknown = mutableListOf<String>()
        val known = mutableListOf<String>()

        for (word in tokens) {
            if (word in knownSet) {
                known.add(word)
            } else {
                // Verificar si es una variación de una palabra conocida
                if (!isVariationOfKnownWord(word, knownSet)) {
                    unknown.add(word)
                } else {
                    known.add(word)
                }
            }
        }

        return WordAnalysis(
            unknownWords = unknown,
            knownWords = known,
            totalWords = tokens.size
        )
    }

    /**
     * Verifica si una palabra es una variación de una conocida
     * Ejemplo: "goede" es variación de "goed"
     */
    private fun isVariationOfKnownWord(word: String, knownWords: Set<String>): Boolean {
        // Reglas simples de derivación en holandés

        // Plurales: +en, +s
        if (word.endsWith("en") && word.length > 3) {
            val base = word.dropLast(2)
            if (base in knownWords) return true
        }

        if (word.endsWith("s") && word.length > 2) {
            val base = word.dropLast(1)
            if (base in knownWords) return true
        }

        // Adjetivos: +e
        if (word.endsWith("e") && word.length > 2) {
            val base = word.dropLast(1)
            if (base in knownWords) return true
        }

        // Diminutivos: +je, +tje, +pje
        listOf("je", "tje", "pje", "etje").forEach { suffix ->
            if (word.endsWith(suffix) && word.length > suffix.length + 1) {
                val base = word.dropLast(suffix.length)
                if (base in knownWords) return true
            }
        }

        // Verbos: +t, +d (tercera persona)
        if ((word.endsWith("t") || word.endsWith("d")) && word.length > 2) {
            val base = word.dropLast(1)
            if (base in knownWords) return true
        }

        return false
    }

    /**
     * Obtiene estadísticas del texto
     */
    fun getTextStats(text: String): TextStats {
        val tokens = tokenize(text)
        val uniqueWords = tokens.distinct()

        return TextStats(
            totalWords = tokens.size,
            uniqueWords = uniqueWords.size,
            averageWordLength = tokens.map { it.length }.average()
        )
    }
}

/**
 * Resultado del análisis de palabras
 */
data class WordAnalysis(
    val unknownWords: List<String>,
    val knownWords: List<String>,
    val totalWords: Int
) {
    val unknownCount: Int get() = unknownWords.size
    val knownCount: Int get() = knownWords.size
    val knownPercentage: Float get() = if (totalWords > 0) (knownCount.toFloat() / totalWords) * 100 else 0f
}

/**
 * Estadísticas de un texto
 */
data class TextStats(
    val totalWords: Int,
    val uniqueWords: Int,
    val averageWordLength: Double
)
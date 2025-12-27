package com.perez.dutchlearner.language

import com.perez.dutchlearner.database.UnknownWordEntity

/**
 * Tokenizador para texto en holandés
 * ADAPTADO PARA NIVEL AVANZADO: Detecta palabras desconocidas
 */
class DutchTokenizer {

    /**
     * Tokeniza un texto en holandés en palabras individuales
     */
    fun tokenize(text: String): List<String> {
        return text
            .lowercase()
            // Mantener letras, números, guiones y apóstrofes
            .replace(Regex("[^a-záéíóúüñ0-9\\s'-]"), " ")
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
     * LÓGICA INVERTIDA: Ahora buscamos en la lista de DESCONOCIDAS
     *
     * @param text Texto en holandés
     * @param unknownWords Lista de palabras que NO conoces
     * @return Análisis de palabras
     */
    fun analyzeText(text: String, unknownWords: List<UnknownWordEntity>): WordAnalysis {
        val tokens = tokenize(text)

        // Solo considerar palabras que aún no se han aprendido
        val unknownSet = unknownWords
            .filter { !it.learned }
            .map { it.word.lowercase() }
            .toSet()

        val unknown = mutableListOf<String>()
        val known = mutableListOf<String>()

        for (word in tokens) {
            if (word in unknownSet) {
                // Está en la lista de desconocidas
                unknown.add(word)
            } else if (isVariationOfUnknownWord(word, unknownSet)) {
                // Es una variación de una palabra desconocida
                unknown.add(word)
            } else {
                // No está en la lista = la conoces
                known.add(word)
            }
        }

        return WordAnalysis(
            unknownWords = unknown,
            knownWords = known,
            totalWords = tokens.size,
            newWords = unknown.filter { it !in unknownSet } // Palabras que deberías agregar
        )
    }

    /**
     * Verifica si una palabra es una variación de una desconocida
     */
    private fun isVariationOfUnknownWord(word: String, unknownWords: Set<String>): Boolean {
        // Reglas de derivación en holandés

        // Plurales: +en, +s
        if (word.endsWith("en") && word.length > 3) {
            val base = word.dropLast(2)
            if (base in unknownWords) return true
        }

        if (word.endsWith("s") && word.length > 2) {
            val base = word.dropLast(1)
            if (base in unknownWords) return true
        }

        // Adjetivos: +e
        if (word.endsWith("e") && word.length > 2) {
            val base = word.dropLast(1)
            if (base in unknownWords) return true
        }

        // Diminutivos: +je, +tje, +pje
        listOf("je", "tje", "pje", "etje").forEach { suffix ->
            if (word.endsWith(suffix) && word.length > suffix.length + 1) {
                val base = word.dropLast(suffix.length)
                if (base in unknownWords) return true
            }
        }

        // Verbos: +t, +d (tercera persona)
        if ((word.endsWith("t") || word.endsWith("d")) && word.length > 2) {
            val base = word.dropLast(1)
            if (base in unknownWords) return true
        }

        return false
    }

    /**
     * Sugiere palabras nuevas para agregar a la lista de desconocidas
     * Basado en frecuencia de aparición
     */
    fun suggestWordsToAdd(
        recentPhrases: List<String>,
        currentUnknown: List<UnknownWordEntity>
    ): List<WordSuggestion> {
        val allTokens = recentPhrases.flatMap { tokenize(it) }
        val unknownSet = currentUnknown.map { it.word }.toSet()

        // Contar frecuencia de palabras que NO están en tu lista
        val frequencyMap = allTokens
            .filter { it !in unknownSet }
            .groupingBy { it }
            .eachCount()

        // Sugerir las que aparecen varias veces (probablemente complejas)
        return frequencyMap
            .filter { it.value >= 2 } // Aparece 2+ veces
            .map { WordSuggestion(it.key, it.value) }
            .sortedByDescending { it.frequency }
    }

    /**
     * Estadísticas del texto
     */
    fun getTextStats(text: String): TextStats {
        val tokens = tokenize(text)
        val uniqueWords = tokens.distinct()

        return TextStats(
            totalWords = tokens.size,
            uniqueWords = uniqueWords.size,
            averageWordLength = if (tokens.isNotEmpty()) {
                tokens.map { it.length }.average()
            } else 0.0
        )
    }
}

/**
 * Resultado del análisis de palabras
 */
data class WordAnalysis(
    val unknownWords: List<String>, // Palabras que NO conoces (en tu lista)
    val knownWords: List<String>,   // Palabras que SÍ conoces (no en tu lista)
    val totalWords: Int,
    val newWords: List<String> = emptyList() // Palabras nuevas para agregar
) {
    val unknownCount: Int get() = unknownWords.size
    val knownCount: Int get() = knownWords.size
    val knownPercentage: Float get() =
        if (totalWords > 0) (knownCount.toFloat() / totalWords) * 100 else 100f
    val difficulty: Difficulty get() = when {
        knownPercentage >= 95 -> Difficulty.EASY
        knownPercentage >= 80 -> Difficulty.MEDIUM
        else -> Difficulty.HARD
    }
}

enum class Difficulty {
    EASY, MEDIUM, HARD
}

/**
 * Sugerencia de palabra para agregar
 */
data class WordSuggestion(
    val word: String,
    val frequency: Int
)

/**
 * Estadísticas de un texto
 */
data class TextStats(
    val totalWords: Int,
    val uniqueWords: Int,
    val averageWordLength: Double
)
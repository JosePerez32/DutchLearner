package com.perez.dutchlearner.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Refresh
import androidx.lifecycle.lifecycleScope
import com.perez.dutchlearner.database.PhraseEntity
import com.perez.dutchlearner.language.DutchTokenizer
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhrasesScreen(
    phrases: List<PhraseEntity>,
    onNavigateBack: () -> Unit,
    onDeletePhrase: (PhraseEntity) -> Unit,
    onSpeakDutch: (String) -> Unit,
    onWordTap: (String) -> Unit,
    onRetryTranslation: (PhraseEntity) -> Unit,
    ttsReady: Boolean
) {
    var expandedPhraseId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis frases guardadas") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Estadísticas
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondaryContainer,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatItem(
                        label = "Total de frases",
                        value = phrases.size.toString()
                    )
                    StatItem(
                        label = "Esta semana",
                        value = phrases.count {
                            it.createdAt > System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
                        }.toString()
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Lista de frases
            if (phrases.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(phrases, key = { it.id }) { phrase ->
                        PhraseCard(
                            phrase = phrase,
                            isExpanded = expandedPhraseId == phrase.id,
                            onExpandToggle = {
                                expandedPhraseId = if (expandedPhraseId == phrase.id) null else phrase.id
                            },
                            onDelete = { onDeletePhrase(phrase) },
                            onSpeak = { onSpeakDutch(phrase.dutchText) },
                            onWordTap = onWordTap,
                            //onRetryTranslation = { phraseEntity ->  // ⬅️ Te falta pasar esto
                                // Aquí va tu lógica de reintento
                                // Por ahora vacío o llama a una función
                            //},
                            onRetryTranslation = onRetryTranslation,
                            ttsReady = ttsReady
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "📝",
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = "No hay frases guardadas",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Graba y guarda tu primera frase\npara empezar a aprender",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PhraseCard(
    phrase: PhraseEntity,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onDelete: () -> Unit,
    onSpeak: () -> Unit,
    onWordTap: (String) -> Unit,
    onRetryTranslation: (PhraseEntity) -> Unit,
    ttsReady: Boolean
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSnackbar by remember { mutableStateOf(false) }
    var lastAddedWord by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Fecha
            Text(
                text = formatDate(phrase.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Español
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "🇪🇸 Español",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = phrase.spanishText,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // Holandés
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "🇳🇱 Nederlands",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                    if (phrase.dutchText.contains("[Sin traducción]") ||
                        phrase.dutchText.contains("[error]") ||
                        phrase.dutchText.contains("[sin traducción]")) {

                        // Botón para reintentar traducción
                        Button(
                            onClick = { onRetryTranslation(phrase) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Refresh,  // Icono de recargar
                                contentDescription = "Reintentar traducción"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("🔄 Traducir automáticamente")
                        }

                        // Mostrar el texto de error original
                        Text(
                            text = phrase.dutchText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        // Texto normal con palabras tappables
                        if (isExpanded) {
                            // Mostrar palabras clickeables
                            ClickableDutchText(
                                text = phrase.dutchText,
                                onWordClick = { word ->
                                    onWordTap(word)
                                    lastAddedWord = word
                                    showSnackbar = true
                                }
                            )
                        } else {
                            // Mostrar texto normal
                            Text(
                                text = phrase.dutchText,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }


            }

            // Palabras desconocidas (si hay)
            if (phrase.unknownWordsCount > 0) {
                Text(
                    text = "📚 ${phrase.unknownWordsCount} palabras nuevas",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // Estadísticas
            if (phrase.timesReviewed > 0) {
                Text(
                    text = "👁️ Revisada ${phrase.timesReviewed} ${if (phrase.timesReviewed == 1) "vez" else "veces"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Botones de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSpeak,
                    enabled = ttsReady,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🔊")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Escuchar")
                }

                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Botón expandir/contraer
            TextButton(
                onClick = onExpandToggle,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(if (isExpanded) "Contraer" else "Toca palabras para agregar ➕")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }


    // Diálogo de confirmación para eliminar
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Eliminar frase?") },
            text = { Text("Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Snackbar de confirmación
    if (showSnackbar) {
        LaunchedEffect(lastAddedWord) {
            kotlinx.coroutines.delay(2000)
            showSnackbar = false
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Snackbar {
                Text("✅ '$lastAddedWord' agregada a desconocidas")
            }
        }
    }
}

@Composable
private fun ClickableDutchText(
    text: String,
    onWordClick: (String) -> Unit
) {
    val tokenizer = remember { DutchTokenizer() }
    val words = remember(text) { tokenizer.tokenize(text) }

    Column {
        Text(
            "👆 Toca una palabra para agregarla:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalSpacing = 4.dp,
            verticalSpacing = 4.dp
        ) {
            words.forEach { word ->
                WordChip(
                    word = word,
                    onClick = { onWordClick(word) }
                )
            }
        }
    }
}

@Composable
private fun WordChip(
    word: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 2.dp
    ) {
        Text(
            text = word,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    verticalSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }

        var xPosition = 0
        var yPosition = 0
        var maxHeight = 0

        val positions = mutableListOf<Pair<Int, Int>>()

        placeables.forEach { placeable ->
            if (xPosition + placeable.width > constraints.maxWidth && xPosition > 0) {
                xPosition = 0
                yPosition += maxHeight + verticalSpacing.roundToPx()
                maxHeight = 0
            }

            positions.add(Pair(xPosition, yPosition))
            xPosition += placeable.width + horizontalSpacing.roundToPx()
            maxHeight = maxOf(maxHeight, placeable.height)
        }

        val height = yPosition + maxHeight

        layout(constraints.maxWidth, height) {
            placeables.forEachIndexed { index, placeable ->
                val (x, y) = positions[index]
                placeable.place(x, y)
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Hace un momento"
        diff < 3600_000 -> "Hace ${diff / 60_000} minutos"
        diff < 86400_000 -> "Hace ${diff / 3600_000} horas"
        diff < 604800_000 -> {
            val days = diff / 86400_000
            "Hace $days ${if (days == 1L) "día" else "días"}"
        }
        else -> {
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            formatter.format(Date(timestamp))
        }
    }
}
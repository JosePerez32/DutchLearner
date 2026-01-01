package com.perez.dutchlearner.ui

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.PlayArrow
import com.perez.dutchlearner.database.UnknownWordEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnknownWordsScreen(
    unknownWords: List<UnknownWordEntity>,
    onNavigateBack: () -> Unit,
    onAddWord: (String) -> Unit,
    onMarkAsLearned: (UnknownWordEntity) -> Unit,
    onDeleteWord: (UnknownWordEntity) -> Unit,
    onSpeakWord: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showFilter by remember { mutableStateOf(false) } // Mostrar aprendidas

    // Filtrar según estado
    val displayWords = if (showFilter) {
        unknownWords // Mostrar todas (incluyendo aprendidas)
    } else {
        unknownWords.filter { !it.learned } // Solo desconocidas
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Palabras por aprender") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Agregar palabra"
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
            UnknownWordsStats(
                totalUnknown = unknownWords.count { !it.learned },
                totalLearned = unknownWords.count { it.learned }
            )

            Divider()

            // Filtro
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showFilter) "Mostrando todas" else "Solo desconocidas",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = showFilter,
                    onCheckedChange = { showFilter = it }
                )
            }

            Divider()

            // Lista de palabras
            if (displayWords.isEmpty()) {
                EmptyUnknownWordsState(showingAll = showFilter)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayWords, key = { it.word }) { word ->
                        UnknownWordCard(
                            word = word,
                            onMarkAsLearned = { onMarkAsLearned(word) },
                            onDelete = { onDeleteWord(word) },
                            onSpeak = { onSpeakWord(word.word) }
                        )
                    }
                }
            }
        }
    }

    // Diálogo para agregar palabra
    if (showAddDialog) {
        AddUnknownWordDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { newWord ->
                onAddWord(newWord)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun UnknownWordsStats(totalUnknown: Int, totalLearned: Int) {
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
                label = "Por aprender",
                value = totalUnknown.toString(),
                icon = "📚"
            )
            StatItem(
                label = "Aprendidas",
                value = totalLearned.toString(),
                icon = "✅"
            )
            StatItem(
                label = "Progreso",
                value = if (totalUnknown + totalLearned > 0) {
                    "${(totalLearned * 100 / (totalUnknown + totalLearned))}%"
                } else "0%",
                icon = "📈"
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, icon: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = icon,
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
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
private fun EmptyUnknownWordsState(showingAll: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = if (showingAll) "🎉" else "📖",
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = if (showingAll) "¡Ninguna palabra!" else "¡Todo aprendido!",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = if (showingAll) {
                    "Agrega palabras que encuentres difíciles\nal traducir frases"
                } else {
                    "No tienes palabras pendientes\n¡Sigue así!"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UnknownWordCard(
    word: UnknownWordEntity,
    onMarkAsLearned: () -> Unit,
    onDelete: () -> Unit,
    onSpeak: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (word.learned) 1.dp else 2.dp
        ),
        colors = if (word.learned) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = word.word,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (word.learned) {
                        Text(
                            text = "✅ Aprendida",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Vista ${word.timesSeen} ${if (word.timesSeen == 1) "vez" else "veces"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Agregada ${formatDate(word.addedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!word.learned) {
                    IconButton(
                        onClick = onMarkAsLearned,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Marcar como aprendida"
                        )
                    }
                }

                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    // Diálogo de confirmación
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Eliminar palabra?") },
            text = { Text("\"${word.word}\" será eliminada de tu lista.") },
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
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        // NUEVO BOTÓN DE ESCUCHAR (altavoz)
        IconButton(
            onClick = onSpeak,
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,  // Icono de parlante
                contentDescription = "Escuchar pronunciación"
            )
        }

        if (!word.learned) {
            IconButton(
                onClick = onMarkAsLearned,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Marcar como aprendida"
                )
            }
        }

        IconButton(onClick = { showDeleteDialog = true }) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Eliminar",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun AddUnknownWordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var word by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar palabra desconocida") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Escribe una palabra en holandés que NO conozcas:",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = word,
                    onValueChange = {
                        word = it.lowercase().trim()
                        error = null
                    },
                    label = { Text("Palabra en holandés") },
                    placeholder = { Text("Ejemplo: verantwoordelijk, onafhankelijk") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } }
                )

                Text(
                    text = "💡 Consejo: Agrega palabras que encuentres en tus traducciones",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        word.isBlank() -> error = "Escribe una palabra"
                        word.length < 2 -> error = "Palabra muy corta"
                        word.contains(" ") -> error = "Solo una palabra"
                        else -> onConfirm(word)
                    }
                }
            ) {
                Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private fun formatDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 86400_000 -> "hoy"
        diff < 172800_000 -> "ayer"
        diff < 604800_000 -> {
            val days = diff / 86400_000
            "hace $days días"
        }
        else -> {
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            formatter.format(Date(timestamp))
        }
    }
}
// REEMPLAZAR COMPLETAMENTE AlarmsScreen.kt con este código

package com.perez.dutchlearner.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.perez.dutchlearner.database.AlarmEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(
    alarms: List<AlarmEntity>,
    onNavigateBack: () -> Unit,
    onAddAlarm: (Int, Int) -> Unit,
    onToggleAlarm: (AlarmEntity, Boolean) -> Unit,
    onEditAlarm: (AlarmEntity, Int, Int) -> Unit, // ⬅️ NUEVO
    onDeleteAlarm: (AlarmEntity) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⏰ Alarmas") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    if (alarms.size < 5) {
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Agregar alarma"
                            )
                        }
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
            if (alarms.isEmpty()) {
                EmptyAlarmsState()
            } else {
                // Contador
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 2.dp
                ) {
                    Text(
                        text = "${alarms.size} / 5 alarmas configuradas",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                // Lista de alarmas
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmCard(
                            alarm = alarm,
                            onToggle = { enabled -> onToggleAlarm(alarm, enabled) },
                            onDelete = { onDeleteAlarm(alarm) },
                            onEdit = { hour, minute -> onEditAlarm(alarm, hour, minute) } // ⬅️ NUEVO
                        )
                    }
                }
            }
        }
    }

    // Diálogo agregar alarma
    if (showAddDialog) {
        AddAlarmDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { hour, minute ->
                onAddAlarm(hour, minute)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun EmptyAlarmsState() {
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
                text = "⏰",
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = "No hay alarmas",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Toca + para agregar tu primera alarma",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AlarmCard(
    alarm: AlarmEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onEdit: (Int, Int) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hora clickeable
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showEditDialog = true }
            ) {
                Text(
                    text = alarm.getTimeString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Toca para editar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(
                    checked = alarm.enabled,
                    onCheckedChange = onToggle
                )

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

    // Diálogo de eliminación
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Eliminar alarma?") },
            text = { Text("La alarma de ${alarm.getTimeString()} será eliminada.") },
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

    // Diálogo de edición
    if (showEditDialog) {
        var selectedHour by remember { mutableStateOf(alarm.hour) }
        var selectedMinute by remember { mutableStateOf(alarm.minute) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Editar alarma") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Selecciona la nueva hora:")

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = String.format("%02d", selectedHour),
                            onValueChange = {
                                it.toIntOrNull()?.let { hour ->
                                    if (hour in 0..23) selectedHour = hour
                                }
                            },
                            label = { Text("Hora") },
                            modifier = Modifier.width(80.dp),
                            singleLine = true
                        )

                        Text(":", style = MaterialTheme.typography.headlineMedium)

                        OutlinedTextField(
                            value = String.format("%02d", selectedMinute),
                            onValueChange = {
                                it.toIntOrNull()?.let { minute ->
                                    if (minute in 0..59) selectedMinute = minute
                                }
                            },
                            label = { Text("Min") },
                            modifier = Modifier.width(80.dp),
                            singleLine = true
                        )
                    }

                    Text(
                        text = "Nueva hora: ${String.format("%02d:%02d", selectedHour, selectedMinute)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEdit(selectedHour, selectedMinute)
                        showEditDialog = false
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun AddAlarmDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var selectedHour by remember { mutableStateOf(8) }
    var selectedMinute by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva alarma") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Selecciona la hora:")

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = String.format("%02d", selectedHour),
                        onValueChange = {
                            it.toIntOrNull()?.let { hour ->
                                if (hour in 0..23) selectedHour = hour
                            }
                        },
                        label = { Text("Hora") },
                        modifier = Modifier.width(80.dp),
                        singleLine = true
                    )

                    Text(":", style = MaterialTheme.typography.headlineMedium)

                    OutlinedTextField(
                        value = String.format("%02d", selectedMinute),
                        onValueChange = {
                            it.toIntOrNull()?.let { minute ->
                                if (minute in 0..59) selectedMinute = minute
                            }
                        },
                        label = { Text("Min") },
                        modifier = Modifier.width(80.dp),
                        singleLine = true
                    )
                }

                Text(
                    text = String.format("%02d:%02d", selectedHour, selectedMinute),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedHour, selectedMinute) }) {
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
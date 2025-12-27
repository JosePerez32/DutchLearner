package com.perez.dutchlearner.ui

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onScheduleNotification: (hour: Int, minute: Int) -> Unit,
    onCancelNotifications: () -> Unit
) {
    var selectedHour by remember { mutableStateOf(9) }
    var selectedMinute by remember { mutableStateOf(0) }
    var showTimePicker by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(false) }
    var showPermissionWarning by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sección de notificaciones
            Text(
                text = "🔔 Recordatorios diarios",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "Recibe una notificación diaria para practicar holandés",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Switch de notificaciones
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Activar recordatorios",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (notificationsEnabled) {
                            Text(
                                text = "Programado para las ${selectedHour.toString().padStart(2, '0')}:${selectedMinute.toString().padStart(2, '0')}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                showTimePicker = true
                            } else {
                                onCancelNotifications()
                                notificationsEnabled = false
                            }
                        }
                    )
                }
            }

            // Botón para cambiar hora
            if (notificationsEnabled) {
                Button(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("⏰ Cambiar hora")
                }
            }

            // Advertencia de permisos en Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && showPermissionWarning) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "⚠️ Permiso requerido",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Android 12+ requiere que permitas \"Alarmas y recordatorios\" manualmente en Configuración.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Información
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ℹ️ Cómo funciona",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "• Recibirás una notificación diaria a la hora seleccionada\n" +
                                "• La notificación mostrará una frase para practicar\n" +
                                "• Puedes escucharla directamente desde la notificación",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Time Picker Dialog
        if (showTimePicker) {
            TimePickerDialog(
                initialHour = selectedHour,
                initialMinute = selectedMinute,
                onDismiss = { showTimePicker = false },
                onConfirm = { hour, minute ->
                    selectedHour = hour
                    selectedMinute = minute
                    onScheduleNotification(hour, minute)  // No capturar resultado
                    showTimePicker = false
                }
//                onConfirm = { hour, minute ->
//                    selectedHour = hour
//                    selectedMinute = minute

                    //val success = onScheduleNotification(hour, minute)

//                    if (success) {
//                        notificationsEnabled = true
//                        showPermissionWarning = false
//                    } else {
//                        // Mostrar advertencia si falló
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                            showPermissionWarning = true
//                        }
//                        notificationsEnabled = false
//                    }

//                    showTimePicker = false
                //}
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Selecciona la hora") },
        text = {
            TimePicker(state = timePickerState)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(timePickerState.hour, timePickerState.minute)
                }
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
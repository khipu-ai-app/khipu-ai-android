package pe.khipuai.app.ui.screens.profile

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Assuming we extend uiState to have notification settings or we use local state that is later saved.
    // For now, let's use local state to represent the toggles.
    var reviewReminders by remember { mutableStateOf(true) }
    var processingComplete by remember { mutableStateOf(true) }
    var achievements by remember { mutableStateOf(true) }
    var reminderHour by remember { mutableFloatStateOf(18f) } // 18:00

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Ajusta qué notificaciones deseas recibir para mantener tu ritmo de estudio.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column {
                        NotificationToggleItem(
                            title = "Recordatorios de repaso",
                            description = "Te avisamos cuando tengas conceptos listos para repasar.",
                            checked = reviewReminders,
                            onCheckedChange = { reviewReminders = it }
                        )
                        HorizontalDivider()
                        NotificationToggleItem(
                            title = "Procesamiento completado",
                            description = "Aviso cuando tus documentos o audios hayan sido procesados.",
                            checked = processingComplete,
                            onCheckedChange = { processingComplete = it }
                        )
                        HorizontalDivider()
                        NotificationToggleItem(
                            title = "Logros y metas",
                            description = "Avisos sobre rachas y metas cumplidas.",
                            checked = achievements,
                            onCheckedChange = { achievements = it }
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Hora preferida para recordatorios",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val hourInt = reminderHour.toInt()
                        val amPm = if (hourInt < 12) "AM" else "PM"
                        val displayHour = if (hourInt == 0) 12 else if (hourInt > 12) hourInt - 12 else hourInt
                        Text(
                            text = "$displayHour:00 $amPm",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Slider(
                            value = reminderHour,
                            onValueChange = { reminderHour = it },
                            valueRange = 6f..22f, // De 6am a 10pm
                            steps = 15
                        )
                        Text(
                            text = "Te enviaremos las notificaciones principales alrededor de esta hora.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            item {
                Button(
                    onClick = {
                        val prefs = pe.khipuai.app.data.remote.dto.NotificationPreferencesDto(
                            enabled = true,
                            reviewReminders = reviewReminders,
                            processingComplete = processingComplete,
                            achievements = achievements,
                            reminderHour = reminderHour.toInt()
                        )
                        viewModel.updateProfile(
                            notificationPreferences = prefs
                        )
                        onNavigateBack()
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Text("Guardar preferencias")
                }
            }
        }
    }
}

@Composable
private fun NotificationToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

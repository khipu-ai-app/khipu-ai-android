package pe.khipuai.app.ui.screens.planner

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import pe.khipuai.app.data.remote.dto.ScheduleDayResponse
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleNoteScreen(
    onNavigateBack: () -> Unit,
    viewModel: ScheduleNoteViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onNavigateBack()
        }
    }

    if (uiState.errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            title = { Text("Error") },
            text = { Text(uiState.errorMessage!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissError() }) {
                    Text("Aceptar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agendar Repaso") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Nota: ${uiState.noteTitle}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Selecciona una fecha para repasar los conceptos de esta nota. Te mostramos tu carga planificada de los próximos 7 días para que elijas el mejor momento.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Generar lista de próximos 30 días
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val displayDayFormat = SimpleDateFormat("EEE", Locale("es", "ES"))
                    val displayDateFormat = SimpleDateFormat("d", Locale.getDefault())
                    val calendar = Calendar.getInstance()

                    val next30Days = (0..29).map { i ->
                        if (i > 0) calendar.add(Calendar.DAY_OF_YEAR, 1)
                        val dateStr = sdf.format(calendar.time)
                        val dayOfWeek = displayDayFormat.format(calendar.time).take(3).uppercase()
                        val dayOfMonth = displayDateFormat.format(calendar.time)
                        
                        val load = uiState.weeklySchedule.find { it.date == dateStr }?.count ?: 0
                        Triple(dateStr, dayOfWeek, dayOfMonth)
                    }

                    Text(
                        text = "Panorama Mensual",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(next30Days) { (dateStr, dayOfWeek, dayOfMonth) ->
                            val load = uiState.weeklySchedule.find { it.date == dateStr }?.count ?: 0
                            val isSelected = uiState.selectedDate == dateStr
                            
                            DayGridCell(
                                dateStr = dateStr,
                                dayOfWeek = dayOfWeek,
                                dayOfMonth = dayOfMonth,
                                loadCount = load,
                                isSelected = isSelected,
                                onClick = { viewModel.selectDate(dateStr) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Próximos 7 días",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val displaySdf = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "ES"))
                    val listCalendar = Calendar.getInstance()
                    val next7Days = (0..6).map { i ->
                        if (i > 0) listCalendar.add(Calendar.DAY_OF_YEAR, 1)
                        val dateStr = sdf.format(listCalendar.time)
                        val displayStr = if (i == 0) "Hoy" else if (i == 1) "Mañana" else displaySdf.format(listCalendar.time).replaceFirstChar { it.uppercase() }
                        val load = uiState.weeklySchedule.find { it.date == dateStr }?.count ?: 0
                        Pair(dateStr, Pair(displayStr, load))
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(next7Days) { (dateStr, info) ->
                            val (displayStr, load) = info
                            val isSelected = uiState.selectedDate == dateStr
                            
                            DayScheduleCard(
                                dateStr = dateStr,
                                displayStr = displayStr,
                                loadCount = load,
                                isSelected = isSelected,
                                onClick = { viewModel.selectDate(dateStr) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mental Load Tip
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    val selectedLoad = uiState.totalLoadSelectedDate
                    val message = if (selectedLoad >= 4) {
                        "Ese día ya tienes una carga alta ($selectedLoad conceptos). Te sugerimos elegir un día más despejado."
                    } else if (selectedLoad > 0) {
                        "Carga media ($selectedLoad conceptos). Es un buen día para agregar algo más."
                    } else {
                        "¡Día libre! Perfecto para agendar repasos nuevos."
                    }
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.confirmSchedule() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !uiState.isScheduling && uiState.selectedDate.isNotEmpty()
            ) {
                if (uiState.isScheduling) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Agendar Repaso", style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DayGridCell(
    dateStr: String,
    dayOfWeek: String,
    dayOfMonth: String,
    loadCount: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val loadColor = when {
        loadCount >= 4 -> Color(0xFFD32F2F)
        loadCount >= 2 -> Color(0xFFF57C00)
        loadCount > 0 -> Color(0xFF388E3C)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val contentColor = if (loadCount > 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderWidth = if (isSelected) 2.dp else 0.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = dayOfWeek,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(loadColor)
                .background(Color.Black.copy(alpha = if (isSelected) 0.1f else 0f))
                .padding(borderWidth),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dayOfMonth,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Transparent, RoundedCornerShape(8.dp))
                        .padding(1.dp)
                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

@Composable
private fun DayScheduleCard(
    dateStr: String,
    displayStr: String,
    loadCount: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = displayStr,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (loadCount == 0) "Día libre" else "$loadCount conceptos programados",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Seleccionado",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                // Indicador visual de carga
                val loadColor = when {
                    loadCount >= 4 -> Color(0xFFD32F2F)
                    loadCount >= 2 -> Color(0xFFF57C00)
                    loadCount > 0 -> Color(0xFF388E3C)
                    else -> Color.Transparent
                }
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(loadColor)
                )
            }
        }
    }
}

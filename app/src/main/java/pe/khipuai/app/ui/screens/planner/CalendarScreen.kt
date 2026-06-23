package pe.khipuai.app.ui.screens.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.khipuai.app.data.remote.dto.ManualScheduleItem
import pe.khipuai.app.data.remote.dto.PlannerStatsResponse
import pe.khipuai.app.data.remote.dto.ScheduleDayResponse
import pe.khipuai.app.data.repository.PlannerRepository
import pe.khipuai.app.ui.components.BottomNavigationBar
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

// ─── ViewModel ────────────────────────────────────────────────────────────

data class CalendarUiState(
    val weekSchedule: List<ScheduleDayResponse> = emptyList(),
    val manualSchedules: List<ManualScheduleItem> = emptyList(),
    val stats: PlannerStatsResponse? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val plannerRepository: PlannerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState(isLoading = true))
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val today = LocalDate.now()
            val toDate = today.plusDays(30)

            val scheduleResult = plannerRepository.fetchWeeklySchedule()
            val statsResult = plannerRepository.fetchStats()
            // Pedimos los próximos 30 días de repasos agendados manualmente
            val manualResult = plannerRepository.fetchManualSchedules(
                fromIso = today.toString(),
                toIso = toDate.toString()
            )

            val schedule = scheduleResult.getOrNull() ?: emptyList()
            val stats = statsResult.getOrNull()
            val manual = manualResult.getOrNull() ?: emptyList()
            val error = scheduleResult.exceptionOrNull()?.localizedMessage
                ?: statsResult.exceptionOrNull()?.localizedMessage

            _uiState.value = CalendarUiState(
                weekSchedule = schedule,
                manualSchedules = manual,
                stats = stats,
                isLoading = false,
                errorMessage = if (schedule.isEmpty() && stats == null) error else null
            )
        }
    }
}

// ─── Screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateToTab: (Int) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Calendario SM-2",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recargar",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(selectedTab = 2, onTabSelected = onNavigateToTab)
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ── Estadísticas de racha y dominio ──────────────────────
                uiState.stats?.let { stats ->
                    StatsHeader(stats = stats)
                }

                // ── Calendario semanal de repasos ─────────────────────────
                WeeklyCalendar(weekSchedule = uiState.weekSchedule)

                // ── Repasos agendados manualmente (próximos 30 días) ────
                if (uiState.manualSchedules.isNotEmpty()) {
                    ManualSchedulesList(schedules = uiState.manualSchedules)
                }

                // ── Mensaje de error si aplica ─────────────────────────────
                uiState.errorMessage?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsHeader(stats: PlannerStatsResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Resumen de Aprendizaje",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatPill(
                    icon = Icons.Default.Whatshot,
                    value = "${stats.streakDays}",
                    label = "días racha",
                    color = Color(0xFFF57C00)
                )
                StatPill(
                    icon = Icons.Default.Star,
                    value = "${stats.masteryPercentage}%",
                    label = "dominados",
                    color = Color(0xFF7B1FA2)
                )
                StatPill(
                    icon = Icons.Default.AutoGraph,
                    value = "${stats.dominatedConcepts}/${stats.totalConcepts}",
                    label = "conceptos",
                    color = Color(0xFF1976D2)
                )
            }

            if (stats.courseDistribution.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Distribución por Materia",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                stats.courseDistribution.take(4).forEach { course ->
                    CourseDistributionRow(
                        courseName = course.courseName,
                        count = course.count,
                        total = stats.totalConcepts
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CourseDistributionRow(courseName: String, count: Int, total: Int) {
    val fraction = if (total > 0) count.toFloat() / total else 0f
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = courseName.take(18),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(120.dp)
        )
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outlineVariant
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(24.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun WeeklyCalendar(weekSchedule: List<ScheduleDayResponse>) {
    val today = LocalDate.now()
    val maxCount = weekSchedule.maxOfOrNull { it.count } ?: 1

    Column {
        Text(
            text = "Próximos 7 días",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(weekSchedule) { day ->
                val date = runCatching {
                    LocalDate.parse(day.date, DateTimeFormatter.ISO_LOCAL_DATE)
                }.getOrElse { today }

                val isToday = date == today
                val barFraction = if (maxCount > 0) day.count.toFloat() / maxCount else 0f

                DayColumn(
                    dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es")).take(3),
                    dayNumber = date.dayOfMonth.toString(),
                    reviewCount = day.count,
                    barFraction = barFraction,
                    isToday = isToday
                )
            }
        }

        if (weekSchedule.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sin datos de agenda. Sube apuntes para comenzar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ManualSchedulesList(schedules: List<ManualScheduleItem>) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es")) }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Repasos agendados",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        schedules.forEach { entry ->
            val date = runCatching {
                LocalDate.parse(entry.scheduledDate, DateTimeFormatter.ISO_LOCAL_DATE)
            }.getOrNull() ?: LocalDate.now()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.noteTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = date.format(dateFormatter),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayColumn(
    dayName: String,
    dayNumber: String,
    reviewCount: Int,
    barFraction: Float,
    isToday: Boolean
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(52.dp)
    ) {
        // Barra de carga
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isToday) primaryColor.copy(alpha = 0.15f)
                    else surfaceVariant.copy(alpha = 0.5f)
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (reviewCount > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(barFraction.coerceIn(0.05f, 1f))
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isToday) primaryColor
                            else primaryColor.copy(alpha = 0.55f)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Número del día
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (isToday) primaryColor
                    else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dayNumber,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isToday) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Nombre del día (Lun, Mar, ...)
        Text(
            text = dayName.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = if (isToday) primaryColor
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            fontSize = 9.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Contador de repasos
        if (reviewCount > 0) {
            Surface(
                color = primaryColor.copy(alpha = 0.12f),
                shape = CircleShape
            ) {
                Text(
                    text = "$reviewCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = primaryColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 9.sp
                )
            }
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

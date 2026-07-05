package pe.khipuai.app.ui.screens.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pe.khipuai.app.ui.theme.parseCourseColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Progreso", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        // Section 1: Racha y Meta Diaria
                        item {
                            SectionTitle("Racha y Meta Diaria")
                            StreakCard(
                                streakDays = uiState.streakDays,
                                dailyGoal = uiState.dailyGoalConcepts,
                                dailyCompleted = uiState.dailyCompletedConcepts
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        // Section 2: Progreso de esta semana
                        item {
                            SectionTitle("Progreso de esta semana")
                            WeeklyProgressCard(schedule = uiState.weeklySchedule)
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        // Section 3: Dominio por materia
                        item {
                            SectionTitle("Dominio por materia")
                            MasteryCard(
                                masteryPercentage = uiState.masteryPercentage,
                                totalConcepts = uiState.totalConcepts,
                                courseDistribution = uiState.courseDistribution,
                                courseDistributionWithColor = uiState.courseDistributionWithColor,
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        // Section 4: Conceptos más difíciles
                        if (uiState.hardestConcepts.isNotEmpty()) {
                            item {
                                SectionTitle("Conceptos más difíciles")
                                HardestConceptsCard(concepts = uiState.hardestConcepts)
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }

                        // Section 5: Historial de rachas
                        item {
                            SectionTitle("Historial de rachas")
                            StreakHistoryCard(currentStreak = uiState.streakDays)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun StreakCard(streakDays: Int, dailyGoal: Int, dailyCompleted: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Racha actual: $streakDays días",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Meta de hoy: Repasar $dailyGoal conceptos",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            val progress = if (dailyGoal > 0) dailyCompleted.toFloat() / dailyGoal else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Completados: $dailyCompleted / $dailyGoal",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WeeklyProgressCard(schedule: List<pe.khipuai.app.data.remote.dto.ScheduleDayResponse>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (schedule.isEmpty()) {
                Text("No hay datos para esta semana.", style = MaterialTheme.typography.bodyMedium)
            } else {
                val maxCount = schedule.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    schedule.take(7).forEach { day ->
                        val parsedDate = try {
                            LocalDate.parse(day.date)
                        } catch (e: Exception) {
                            null
                        }
                        val dayName = parsedDate?.format(DateTimeFormatter.ofPattern("E")) ?: "?"
                        val barHeight = (day.count.toFloat() / maxCount) * 100f
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(100.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(barHeight.dp.coerceAtLeast(4.dp))
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (day.count > 0) MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = dayName.take(3),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MasteryCard(
    masteryPercentage: Int,
    totalConcepts: Int,
    courseDistribution: List<pe.khipuai.app.data.remote.dto.CourseDistributionItem>,
    // T-16: distribución con colores para teñir cada barra con el
    // color del curso. Si está vacía, caemos al color primary.
    courseDistributionWithColor: List<pe.khipuai.app.ui.screens.statistics.CourseDistributionWithColor> = emptyList(),
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Dominio Global: $masteryPercentage%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (courseDistribution.isEmpty()) {
                Text("Sin datos por curso aún.", style = MaterialTheme.typography.bodyMedium)
            } else {
                courseDistribution.forEach { item ->
                    val coursePercentage = if (totalConcepts > 0) ((item.count.toFloat() / totalConcepts) * 100).toInt() else 0
                    // T-16: buscar el color del curso en la lista paralela.
                    val colorHex = courseDistributionWithColor
                        .firstOrNull { it.item.courseName == item.courseName }
                        ?.colorHex
                    val courseColor = remember(colorHex) { parseCourseColor(colorHex) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // T-16: dot del color del curso antes del nombre
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(courseColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.courseName,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1.5f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(coursePercentage / 100f)
                                    .fillMaxHeight()
                                    // T-16: la barra usa el color del
                                    // curso en lugar del primary genérico.
                                    .background(courseColor)
                            )
                        }
                        Text(
                            text = "$coursePercentage% (${item.count})",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .weight(0.8f)
                                .padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HardestConceptsCard(concepts: List<pe.khipuai.app.data.remote.dto.DueConceptResponse>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Los que más te cuestan:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            concepts.forEachIndexed { index, concept ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${index + 1}. ${concept.conceptName}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "ease: ${String.format("%.1f", concept.easeFactor)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StreakHistoryCard(currentStreak: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tu mejor racha:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "$currentStreak días",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Racha actual:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "$currentStreak días",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

package pe.khipuai.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import pe.khipuai.app.ui.components.BottomNavigationBar
import pe.khipuai.app.ui.components.CourseCard
import pe.khipuai.app.ui.components.RecentFileItem
import pe.khipuai.app.ui.components.SuggestionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToTab: (Int) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    // Corregido: Se añaden los métodos delegados correctos mediante los imports de Compose runtime
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Khipu AI",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { /* TODO: Open drawer */ }) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Perfil",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Notifications */ }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notificaciones",
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
            BottomNavigationBar(
                selectedTab = 0,
                onTabSelected = onNavigateToTab
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToTab(1) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar"
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                GreetingSection()
            }

            item {
                DailyGoalCard(
                    progress = uiState.dailyProgress,
                    streak = uiState.streak
                )
            }

            item {
                SuggestionCard(
                    title = "Hoy Khipu recomienda repasar: Teoría de Cuerdas",
                    subtitle = "Basado en tu última sesión de Física",
                    description = "Avanzada, este concepto reforzará tu comprensión.",
                    onStartReview = { /* TODO: Start review */ }
                )
            }

            item {
                SectionHeader(
                    title = "Tus Cursos",
                    actionText = "Ver todos",
                    onActionClick = { /* TODO: View all courses */ }
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (uiState.courses.isEmpty() && !uiState.isLoading) {
                        Text(
                            text = "No tienes cursos creados. ¡Sube un apunte para empezar!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        uiState.courses.forEach { course ->
                            val composeColor = try {
                                Color(android.graphics.Color.parseColor(course.color))
                            } catch (_: Exception) {
                                MaterialTheme.colorScheme.primary
                            }

                            CourseCard(
                                name = course.name,
                                progress = course.progress,
                                filesCount = course.filesCount,
                                icon = Icons.Default.MenuBook,
                                color = composeColor
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Archivos Recientes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (uiState.recentFiles.isEmpty() && !uiState.isLoading) {
                        Text(
                            text = "Aún no hay documentos digitalizados.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        uiState.recentFiles.forEach { file ->
                            RecentFileItem(
                                title = file.title,
                                subject = file.subject,
                                timeAgo = file.timeAgo,
                                icon = if (file.type == FileType.AUDIO) Icons.Default.Mic else Icons.Default.Description,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun GreetingSection() {
    Column {
        Text(
            text = "Resumen de tu aprendizaje",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Hola, Estudiante",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Composable
private fun DailyGoalCard(
    progress: Float,
    streak: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Meta diaria",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Racha de $streak días",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionText: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        TextButton(onClick = onActionClick) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
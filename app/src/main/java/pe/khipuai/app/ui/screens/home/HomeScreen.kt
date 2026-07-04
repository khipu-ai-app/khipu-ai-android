package pe.khipuai.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
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
    onNavigateToCourses: () -> Unit = {},
    onNavigateToCourseDetail: (String) -> Unit = {},
    onNavigateToCreateCourse: () -> Unit = {},
    onNavigateToFileViewer: (String) -> Unit = {},
    onNavigateToNoteDetail: (String) -> Unit = {},
    onNavigateToSubscription: (String?) -> Unit = {},
    onNavigateToTutorHistory: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
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
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToTutorHistory) {
                        Icon(
                            imageVector = Icons.Default.ChatBubble,
                            contentDescription = "Tutor AI",
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

            // FREEMIUM-07: Banner "X capturas restantes"
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            if (uiState.isPro) {
                                Text("Plan Pro", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("Capturas ilimitadas.", style = MaterialTheme.typography.bodyMedium)
                            } else {
                                val isExceeded = uiState.capturesUsed >= uiState.capturesLimit
                                val titleColor = if (isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onTertiaryContainer
                                
                                Text("Plan Gratuito", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = titleColor)
                                
                                if (isExceeded) {
                                    Text("Hazte Pro para seguir subiendo notas", style = MaterialTheme.typography.bodyMedium, color = titleColor)
                                } else {
                                    Text("${uiState.capturesUsed} de ${uiState.capturesLimit} capturas usadas este mes", style = MaterialTheme.typography.bodyMedium, color = titleColor)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { if (uiState.capturesLimit > 0) uiState.capturesUsed.toFloat() / uiState.capturesLimit.toFloat() else 0f },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }
                        if (!uiState.isPro) {
                            Button(
                                onClick = { onNavigateToSubscription(null) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Ser Pro")
                            }
                        }
                    }
                }
            }

            item {
                GreetingSection(userName = uiState.userName)
            }

            item {
                DailyGoalCard(
                    progress = uiState.dailyProgress,
                    streak = uiState.streak
                )
            }

            uiState.suggestion?.let { sug ->
                item {
                    SuggestionCard(
                        title = "Hoy Khipu recomienda repasar: ${sug.conceptName}",
                        subtitle = "Basado en tu última sesión de ${sug.courseName}",
                        description = "${sug.label}, este concepto reforzará tu comprensión.",
                        onStartReview = { onNavigateToTab(2) }
                    )
                }
            }

            item {
                SectionHeader(
                    title = "Tus Cursos",
                    actionText = "Ver todos",
                    onActionClick = onNavigateToCourses
                )
            }

            if (uiState.courses.isEmpty() && !uiState.isLoading) {
                item {
                    Column(
                        modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No tienes cursos creados. Crea tu primera materia para organizar tus apuntes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = onNavigateToCreateCourse) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Nueva materia")
                        }
                    }
                }
            } else {
                items(uiState.courses, key = { it.id }) { course ->
                    val fallbackColor = MaterialTheme.colorScheme.primary
                    val composeColor = remember(course.color) {
                        try {
                            Color(android.graphics.Color.parseColor(course.color))
                        } catch (_: Exception) {
                            null
                        }
                    } ?: fallbackColor
                    CourseCard(
                        name = course.name,
                        progress = course.progress,
                        filesCount = course.filesCount,
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        color = composeColor,
                        onClick = { onNavigateToCourseDetail(course.id) }
                    )
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

            if (uiState.recentFiles.isEmpty() && !uiState.isLoading) {
                item {
                    Text(
                        text = "Aún no hay documentos digitalizados.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(uiState.recentFiles, key = { it.id }) { file ->
                    var menuExpanded by remember { androidx.compose.runtime.mutableStateOf(false) }
                    var renameDialogExpanded by remember { androidx.compose.runtime.mutableStateOf(false) }
                    var renameText by remember { androidx.compose.runtime.mutableStateOf(file.title) }
                    var deleteDialogExpanded by remember { androidx.compose.runtime.mutableStateOf(false) }

                    if (renameDialogExpanded) {
                        AlertDialog(
                            onDismissRequest = { renameDialogExpanded = false },
                            title = { Text("Renombrar Apunte") },
                            text = {
                                OutlinedTextField(
                                    value = renameText,
                                    onValueChange = { renameText = it },
                                    label = { Text("Título") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        if (renameText.isNotBlank()) {
                                            viewModel.renameNote(file.id, renameText, file.courseId)
                                        }
                                        renameDialogExpanded = false
                                    }
                                ) {
                                    Text("Guardar")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { renameDialogExpanded = false }) {
                                    Text("Cancelar")
                                }
                            }
                        )
                    }

                    if (deleteDialogExpanded) {
                        AlertDialog(
                            onDismissRequest = { deleteDialogExpanded = false },
                            title = { Text("Eliminar Apunte") },
                            text = { Text("¿Estás seguro de que deseas eliminar permanentemente el apunte '${file.title}'? Esta acción no se puede deshacer.") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        viewModel.deleteNote(file.id)
                                        deleteDialogExpanded = false
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Eliminar")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { deleteDialogExpanded = false }) {
                                    Text("Cancelar")
                                }
                            }
                        )
                    }

                    Box {
                        RecentFileItem(
                            title = file.title,
                            subject = file.subject,
                            timeAgo = file.timeAgo,
                            icon = if (file.type == FileType.AUDIO) Icons.Default.Mic else Icons.Default.Description,
                            color = MaterialTheme.colorScheme.secondary,
                            onClick = {
                                if (!file.uploadId.isNullOrEmpty()) {
                                    onNavigateToFileViewer(java.net.URLEncoder.encode(file.uploadId, "UTF-8"))
                                } else {
                                    onNavigateToNoteDetail(file.id)
                                }
                            },
                            onMenuClick = { menuExpanded = true }
                        )

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Renombrar") },
                                onClick = {
                                    renameText = file.title
                                    renameDialogExpanded = true
                                    menuExpanded = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Eliminar permanentemente", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    deleteDialogExpanded = true
                                    menuExpanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DeleteForever,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
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
private fun GreetingSection(userName: String) {
    val currentHour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greeting = when (currentHour) {
        in 5..11 -> "Buenos días"
        in 12..18 -> "Buenas tardes"
        else -> "Buenas noches"
    }

    Column {
        Text(
            text = "Resumen de tu aprendizaje",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$greeting, $userName",
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



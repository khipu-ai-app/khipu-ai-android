package pe.khipuai.app.ui.screens.coursedetail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.khipuai.app.ui.theme.KhipuAITheme
import pe.khipuai.app.ui.theme.parseCourseColor

// =========================================================================================
//  Top-level entry point
// =========================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    onBackClick: () -> Unit,
    onNoteClick: (String) -> Unit,
    onExpandMapClick: () -> Unit,
    onNavigateToCapture: (String) -> Unit = {},
    onNavigateToTutor: (String) -> Unit = {},
    onNavigateToStudy: (route: String) -> Unit = {},
    onNavigateToExam: (courseId: String, courseName: String) -> Unit = { _: String, _: String -> },
    onNavigateToReview: (String) -> Unit = {},
    viewModel: CourseDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val courseColor = remember(uiState.courseColor) { parseCourseColor(uiState.courseColor) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(courseColor)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = uiState.courseName.ifBlank { "Curso" },
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToTutor(uiState.courseId) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = "Tutor del curso"
                        )
                    }
                    // C-03: Modo Examen
                    IconButton(onClick = { onNavigateToExam(uiState.courseId, uiState.courseName) }) {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = "Modo Examen"
                        )
                    }
                }
            )
        }
    ) { padding ->
        CourseDetailContent(
            state = uiState,
            courseColor = courseColor,
            padding = padding,
            onBackClick = onBackClick,
            onAskTutor = { onNavigateToTutor(uiState.courseId) },
            onAddNote = { onNavigateToCapture(uiState.courseId) },
            onNoteClick = onNoteClick,
            onToggleShowAll = viewModel::toggleShowAllNotes,
            onRename = viewModel::renameNote,
            onDelete = viewModel::deleteNote,
            onReassociate = viewModel::reassociateNote,
            onExpandMap = onExpandMapClick,
            onStudySingle = { conceptTitle ->
                onNavigateToStudy(
                    onNavigateToStudyWithConcepts(uiState.courseId, listOf(conceptTitle))
                )
            },
            onStudyMultiple = { conceptTitles ->
                onNavigateToStudy(
                    onNavigateToStudyWithConcepts(uiState.courseId, conceptTitles)
                )
            },
            onSetExamDate = { date -> viewModel.setExamDate(date) },
            onClearExamDate = { viewModel.clearExamDate() },
            onReviewCourse = { cid ->
                val firstNoteId = uiState.notes.firstOrNull()?.id
                if (firstNoteId != null) onNavigateToReview(firstNoteId)
            },
        )
    }
}

@Composable
private fun CourseDetailContent(
    state: CourseDetailUiState,
    courseColor: Color,
    padding: PaddingValues,
    onBackClick: () -> Unit,
    onAskTutor: () -> Unit,
    onAddNote: () -> Unit,
    onNoteClick: (String) -> Unit,
    onToggleShowAll: () -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
            onReassociate: (String, String?) -> Unit,
            onExpandMap: () -> Unit,
            onStudySingle: (conceptTitle: String) -> Unit,
            onStudyMultiple: (List<String>) -> Unit,
            onSetExamDate: (String) -> Unit = {},
            onClearExamDate: () -> Unit = {},
            onReviewCourse: (String) -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            CourseHeaderCard(
                courseName = state.courseName,
                categoryName = state.categoryName,
                progress = state.courseProgress,
                accent = courseColor,
                onAskTutor = onAskTutor
            )
        }

        // CE-03: Repasar curso
        item {
            val hasNotes = state.notes.isNotEmpty()
            OutlinedButton(
                onClick = {
                    val firstNoteId = state.notes.firstOrNull()?.id
                    if (firstNoteId != null) onReviewCourse(firstNoteId)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = hasNotes,
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (hasNotes) "Repasar este curso" else "Sin notas para repasar")
            }
        }

        // C-04: info del examen
        item {
            ExamDateCard(
                examDate = state.examDate,
                isRescheduling = state.isRescheduling,
                onDatePicked = onSetExamDate,
                onClear = onClearExamDate,
            )
        }

        item {
            NotesSection(
                notes = state.notes,
                totalCount = state.totalNotesCount,
                showAll = state.showAllNotes,
                onToggleShowAll = onToggleShowAll,
                onAdd = onAddNote,
                onNoteClick = onNoteClick,
                onRename = onRename,
                onDelete = onDelete,
                onReassociate = onReassociate,
                availableCourses = state.availableCourses,
                currentCourseId = state.courseId
            )
        }

        item {
            KnowledgeGraphSection(
                nodes = state.previewNodes,
                accent = courseColor,
                onExpand = onExpandMap
            )
        }

        item {
            UpcomingReviewsSection(
                reviews = state.upcomingReviews,
                isLoading = state.isLoading,
                onStudySingle = onStudySingle,
                onStudyMultiple = onStudyMultiple
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

/**
 * Helper para construir la URL del tutor con conceptos pre-cargados.
 * Centraliza el encoding y formato de query params.
 */
internal fun onNavigateToStudyWithConcepts(
    courseId: String,
    conceptTitles: List<String>
): String {
    val conceptParam = conceptTitles
        .joinToString("|") { java.net.URLEncoder.encode(it, "UTF-8") }
    return "tutor/new_session?contextType=course&contextId=$courseId&initialConcepts=$conceptParam"
}

// =========================================================================================
//  Section 1 — Header
// =========================================================================================

@Composable
private fun CourseHeaderCard(
    courseName: String,
    categoryName: String,
    progress: Int,
    accent: Color,
    onAskTutor: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MasteryIndicator(progress = progress, accent = accent)

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Solo mostramos la categoría si parece texto (no hex como "#4A90E2")
                    if (categoryName.isNotBlank() && !categoryName.startsWith("#")) {
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text(categoryName.uppercase()) },
                            colors = AssistChipDefaults.assistChipColors(
                                disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                disabledLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            border = null
                        )
                    }

                    Text(
                        text = courseName.ifBlank { "Sin nombre" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = if (progress == 0) "Aún no has dominado ningún concepto"
                        else "$progress% de los conceptos dominados",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (progress in 1..100) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = accent,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round
                )
            }

            FilledTonalButton(
                onClick = onAskTutor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Chatear con el tutor del curso")
            }
        }
    }
}

@Composable
private fun MasteryIndicator(progress: Int, accent: Color) {
    Box(
        modifier = Modifier.size(72.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.size(72.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeWidth = 6.dp,
            strokeCap = StrokeCap.Round
        )
        CircularProgressIndicator(
            progress = { (progress / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.size(72.dp),
            color = accent,
            strokeWidth = 6.dp,
            strokeCap = StrokeCap.Round
        )
        Text(
            text = "$progress%",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── C-04: fecha de examen ──────────────────────────────────────────────────

@Composable
private fun ExamDateCard(
    examDate: String?,
    isRescheduling: Boolean,
    onDatePicked: (String) -> Unit,
    onClear: () -> Unit,
) {
    var showQuickPicker by remember { mutableStateOf(false) }

    if (showQuickPicker) {
        AlertDialog(
            onDismissRequest = { showQuickPicker = false },
            title = { Text("Seleccionar fecha del examen") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3 to "En 3 días", 7 to "En 1 semana", 14 to "En 2 semanas", 30 to "En 1 mes").forEach { (days, label) ->
                        TextButton(
                            onClick = {
                                val date = java.time.LocalDate.now().plusDays(days.toLong()).toString()
                                onDatePicked(date)
                                showQuickPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(label) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showQuickPicker = false }) { Text("Cerrar") } },
            dismissButton = { TextButton(onClick = { showQuickPicker = false }) { Text("Cancelar") } }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (examDate != null) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                tint = if (examDate != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (examDate != null) {
                    val daysUntil = try {
                        val exam = java.time.LocalDate.parse(examDate)
                        val today = java.time.LocalDate.now()
                        java.time.temporal.ChronoUnit.DAYS.between(today, exam)
                    } catch (_: Exception) { null }
                    Text(text = "Examen: ${examDate}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (daysUntil != null) {
                        val text = when {
                            daysUntil < 0 -> "Hace ${-daysUntil} días"
                            daysUntil == 0L -> "¡Hoy!"
                            daysUntil == 1L -> "Mañana"
                            else -> "En $daysUntil días"
                        }
                        Text(text, style = MaterialTheme.typography.bodySmall,
                             color = if (daysUntil <= 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Text("Sin fecha de examen", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Toca + para establecer la fecha.", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (isRescheduling) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                IconButton(onClick = { showQuickPicker = true }) {
                    Icon(Icons.Default.Add, contentDescription = examDate?.let { "Cambiar" } ?: "Fijar fecha", modifier = Modifier.size(20.dp))
                }
                if (examDate != null) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Close, contentDescription = "Quitar fecha", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

// =========================================================================================
//  Section 2 — Notes
// =========================================================================================

@Composable
private fun NotesSection(
    notes: List<CompactNoteUiModel>,
    totalCount: Int,
    showAll: Boolean,
    onToggleShowAll: () -> Unit,
    onAdd: () -> Unit,
    onNoteClick: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onReassociate: (String, String?) -> Unit,
    availableCourses: List<pe.khipuai.app.data.local.entity.CourseEntity>,
    currentCourseId: String
) {
    SectionHeader(
        title = "Tus notas",
        subtitle = if (totalCount == 0) null else "$totalCount en total",
        actionIcon = Icons.Default.Add,
        actionDescription = "Agregar nota",
        onAction = onAdd
    )

    if (notes.isEmpty()) {
        NotesEmptyState(onAdd = onAdd)
        return
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column {
            val displayed = if (showAll) notes else notes.take(5)
            displayed.forEachIndexed { index, note ->
                NoteRow(
                    note = note,
                    onClick = { onNoteClick(note.id) },
                    onRename = onRename,
                    onDelete = onDelete,
                    onReassociate = onReassociate,
                    availableCourses = availableCourses,
                    currentCourseId = currentCourseId
                )
                if (index < displayed.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }
            }

            if (notes.size > 5) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
                TextButton(
                    onClick = onToggleShowAll,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(if (showAll) "Ver menos" else "Ver todas (${notes.size})")
                }
            }
        }
    }
}

@Composable
private fun NoteRow(
    note: CompactNoteUiModel,
    onClick: () -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onReassociate: (String, String?) -> Unit,
    availableCourses: List<pe.khipuai.app.data.local.entity.CourseEntity>,
    currentCourseId: String
) {
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    var renameDialogExpanded by rememberSaveable { mutableStateOf(false) }
    var renameText by rememberSaveable(note.id) { mutableStateOf(note.title) }
    var deleteDialogExpanded by rememberSaveable { mutableStateOf(false) }
    var reassociateDialogExpanded by rememberSaveable { mutableStateOf(false) }

    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        },
        headlineContent = {
            Text(
                text = note.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (note.snippet.isNotBlank()) {
                    Text(
                        text = note.snippet,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Text(
                            text = note.subCategory,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "· ${note.dateTag}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        trailingContent = {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opciones de la nota",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Renombrar") },
                        onClick = {
                            renameText = note.title
                            renameDialogExpanded = true
                            menuExpanded = false
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Cambiar materia") },
                        onClick = {
                            reassociateDialogExpanded = true
                            menuExpanded = false
                        },
                        leadingIcon = { Icon(Icons.Default.Class, contentDescription = null) }
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
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
    )

    if (renameDialogExpanded) {
        AlertDialog(
            onDismissRequest = { renameDialogExpanded = false },
            title = { Text("Renombrar apunte") },
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
                        if (renameText.isNotBlank()) onRename(note.id, renameText)
                        renameDialogExpanded = false
                    }
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { renameDialogExpanded = false }) { Text("Cancelar") }
            }
        )
    }

    if (deleteDialogExpanded) {
        AlertDialog(
            onDismissRequest = { deleteDialogExpanded = false },
            icon = {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Eliminar apunte") },
            text = { Text("¿Eliminar permanentemente '${note.title}'? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(note.id)
                        deleteDialogExpanded = false
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { deleteDialogExpanded = false }) { Text("Cancelar") }
            }
        )
    }

    if (reassociateDialogExpanded) {
        ReassociateDialog(
            currentCourseId = currentCourseId,
            availableCourses = availableCourses,
            onDismiss = { reassociateDialogExpanded = false },
            onConfirm = { newCourseId ->
                onReassociate(note.id, newCourseId)
                reassociateDialogExpanded = false
            }
        )
    }
}

@Composable
private fun NotesEmptyState(onAdd: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Aún no tienes notas en este curso",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Sube tu primer apunte y Khipu lo analizará para extraer conceptos clave.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            FilledTonalButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Subir primer apunte")
            }
        }
    }
}

@Composable
private fun ReassociateDialog(
    currentCourseId: String,
    availableCourses: List<pe.khipuai.app.data.local.entity.CourseEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(currentCourseId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Asociar a materia") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                RadioRow(
                    label = "Ninguno (General)",
                    selected = selectedId == null,
                    onClick = { selectedId = null }
                )
                HorizontalDivider()
                availableCourses.forEach { course ->
                    RadioRow(
                        label = course.name,
                        selected = selectedId == course.id,
                        onClick = { selectedId = course.id }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedId) }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

// =========================================================================================
//  Section 3 — Knowledge graph
// =========================================================================================

@Composable
private fun KnowledgeGraphSection(
    nodes: List<GraphNodeUiModel>,
    accent: Color,
    onExpand: () -> Unit
) {
    SectionHeader(
        title = "Mapa del curso",
        subtitle = "Vista previa · toca para expandir",
        onAction = null,
        actionIcon = null,
        actionDescription = null
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clickable(onClick = onExpand),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        if (nodes.isEmpty()) {
            GraphEmptyState()
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                // Líneas conectoras entre los nodos consecutivos
                if (nodes.size > 1) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val outlineColor = MaterialTheme.colorScheme.outlineVariant
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            for (i in 0 until nodes.size - 1) {
                                val start = Offset(
                                    size.width * nodes[i].xOffsetFraction,
                                    size.height * nodes[i].yOffsetFraction
                                )
                                val end = Offset(
                                    size.width * nodes[i + 1].xOffsetFraction,
                                    size.height * nodes[i + 1].yOffsetFraction
                                )
                                drawLine(
                                    color = outlineColor,
                                    start = start,
                                    end = end,
                                    strokeWidth = 2.5f
                                )
                            }
                        }
                    }
                }

                // Nodos
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val widthDp = maxWidth
                    val heightDp = maxHeight
                    nodes.forEach { node ->
                        val xPos = widthDp * node.xOffsetFraction - 28.dp
                        val yPos = heightDp * node.yOffsetFraction - 16.dp
                        GraphNodeBubble(
                            node = node,
                            accent = accent,
                            modifier = Modifier.offset(x = xPos, y = yPos)
                        )
                    }
                }

                // CTA flotante
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    tonalElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Expandir grafo", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun GraphNodeBubble(node: GraphNodeUiModel, accent: Color, modifier: Modifier = Modifier) {
    val (bg, fg) = when (node.status) {
        NodeStatus.DOMINADO -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        NodeStatus.EN_PROGRESO -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        NodeStatus.BLOQUEADO -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val icon: ImageVector = when (node.status) {
        NodeStatus.DOMINADO -> Icons.Default.Check
        NodeStatus.EN_PROGRESO -> Icons.Default.AutoAwesome
        NodeStatus.BLOQUEADO -> Icons.Default.Lock
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = bg,
            modifier = Modifier.size(40.dp),
            tonalElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            tonalElevation = 1.dp
        ) {
            Text(
                text = node.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun GraphEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Hub,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Aún no hay conceptos mapeados",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Sube notas para que Khipu conecte tus conceptos.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// =========================================================================================
//  Section 4 — Upcoming reviews (Conceptos a dominar)
// =========================================================================================

@Composable
private fun UpcomingReviewsSection(
    reviews: List<ReviewItemUiModel>,
    isLoading: Boolean,
    onStudySingle: (conceptTitle: String) -> Unit,
    onStudyMultiple: (List<String>) -> Unit
) {
    // Dos estados independientes:
    // - selectionMode: si el usuario entró a modo selección (tap en el botón "Seleccionar")
    // - selectedIds: qué conceptos marcó
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedIds by rememberSaveable { mutableStateOf(setOf<String>()) }

    // Si el usuario sale del modo selección, limpiamos la selección
    val exitSelection = {
        selectionMode = false
        selectedIds = emptySet()
    }

    val allSelected = reviews.isNotEmpty() && selectedIds.size == reviews.size

    SectionHeader(
        title = "Conceptos a dominar",
        subtitle = when {
            isLoading -> "Cargando…"
            reviews.isEmpty() -> "Estás al día con este curso 🎉"
            selectionMode && selectedIds.isNotEmpty() ->
                "${selectedIds.size} seleccionado${if (selectedIds.size == 1) "" else "s"}"
            selectionMode -> "Toca los conceptos a estudiar"
            else -> "${reviews.size} ${if (reviews.size == 1) "concepto" else "conceptos"} pendientes"
        },
        actionIcon = when {
            selectionMode -> Icons.Default.Close
            reviews.isNotEmpty() -> Icons.Default.Checklist
            else -> null
        },
        actionDescription = when {
            selectionMode -> "Salir de selección"
            reviews.isNotEmpty() -> "Seleccionar varios para estudiar con el tutor"
            else -> null
        },
        onAction = when {
            selectionMode -> { { exitSelection() } }
            reviews.isNotEmpty() -> { { selectionMode = true } }
            else -> null
        }
    )

    when {
        isLoading && reviews.isEmpty() -> ReviewsLoadingState()

        reviews.isEmpty() -> Spacer(modifier = Modifier.height(8.dp))

        else -> {
            val urgent = reviews.filter { it.isUrgent }
            val upcoming = reviews.filterNot { it.isUrgent }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (selectionMode) {
                    SelectionToolbar(
                        allSelected = allSelected,
                        selectedCount = selectedIds.size,
                        totalCount = reviews.size,
                        onSelectAll = {
                            selectedIds = if (allSelected) emptySet()
                            else reviews.map { it.id }.toSet()
                        },
                        onClear = { selectedIds = emptySet() }
                    )
                }

                if (urgent.isNotEmpty()) {
                    UrgencyGroupHeader(
                        label = "Vencen hoy",
                        count = urgent.size,
                        tone = UrgencyTone.URGENT
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Column {
                            urgent.forEachIndexed { index, task ->
                                ReviewRow(
                                    task = task,
                                    isSelected = task.id in selectedIds,
                                    isSelectionMode = selectionMode,
                                    onToggleSelection = {
                                        selectedIds = if (task.id in selectedIds)
                                            selectedIds - task.id
                                        else
                                            selectedIds + task.id
                                    },
                                    onStudy = { onStudySingle(task.title) }
                                )
                                if (index < urgent.lastIndex) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }
                }

                if (upcoming.isNotEmpty()) {
                    UrgencyGroupHeader(
                        label = "Próximos",
                        count = upcoming.size,
                        tone = UrgencyTone.NORMAL
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Column {
                            upcoming.forEachIndexed { index, task ->
                                ReviewRow(
                                    task = task,
                                    isSelected = task.id in selectedIds,
                                    isSelectionMode = selectionMode,
                                    onToggleSelection = {
                                        selectedIds = if (task.id in selectedIds)
                                            selectedIds - task.id
                                        else
                                            selectedIds + task.id
                                    },
                                    onStudy = { onStudySingle(task.title) }
                                )
                                if (index < upcoming.lastIndex) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Barra inferior con el CTA de "Estudiar N"
            androidx.compose.animation.AnimatedVisibility(
                visible = selectionMode && selectedIds.isNotEmpty(),
                enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                BulkActionBar(
                    selectedCount = selectedIds.size,
                    selectedTitles = reviews.filter { it.id in selectedIds }.map { it.title },
                    onConfirm = onStudyMultiple,
                    onCancel = exitSelection
                )
            }
        }
    }
}

@Composable
private fun SelectionToolbar(
    allSelected: Boolean,
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (allSelected) "Todos seleccionados"
                else "$selectedCount de $totalCount",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Medium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (selectedCount > 0) {
                    TextButton(onClick = onClear) {
                        Text("Limpiar")
                    }
                }
                TextButton(onClick = onSelectAll) {
                    Icon(
                        imageVector = if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (allSelected) "Quitar todos" else "Seleccionar todos")
                }
            }
        }
    }
}

private enum class UrgencyTone { URGENT, NORMAL }

@Composable
private fun UrgencyGroupHeader(label: String, count: Int, tone: UrgencyTone) {
    val color = when (tone) {
        UrgencyTone.URGENT -> MaterialTheme.colorScheme.error
        UrgencyTone.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier.padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
        Text(
            text = "· $count",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReviewRow(
    task: ReviewItemUiModel,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onStudy: () -> Unit
) {
    val containerColor = when (task.difficulty) {
        ConceptDifficulty.HARD -> MaterialTheme.colorScheme.errorContainer
        ConceptDifficulty.MEDIUM -> MaterialTheme.colorScheme.tertiaryContainer
        ConceptDifficulty.EASY -> MaterialTheme.colorScheme.secondaryContainer
        ConceptDifficulty.UNKNOWN -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val onContainerColor = when (task.difficulty) {
        ConceptDifficulty.HARD -> MaterialTheme.colorScheme.onErrorContainer
        ConceptDifficulty.MEDIUM -> MaterialTheme.colorScheme.onTertiaryContainer
        ConceptDifficulty.EASY -> MaterialTheme.colorScheme.onSecondaryContainer
        ConceptDifficulty.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val difficultyLabel = when (task.difficulty) {
        ConceptDifficulty.HARD -> "Difícil"
        ConceptDifficulty.MEDIUM -> "Medio"
        ConceptDifficulty.EASY -> "Fácil"
        ConceptDifficulty.UNKNOWN -> "Nuevo"
    }

    val backgroundColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    else
        MaterialTheme.colorScheme.surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable {
                if (isSelectionMode) onToggleSelection() else onStudy()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Leading: checkbox (en modo selección) o ícono de estado (en modo normal)
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() }
            )
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = difficultyLabel.first().toString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = onContainerColor
                )
            }
        }

        // Contenido: nombre + meta info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DifficultyChip(label = difficultyLabel, tone = task.difficulty)
                MetaDot()
                Text(
                    text = task.dueLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (task.isUrgent)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (task.isUrgent) FontWeight.SemiBold else FontWeight.Normal
                )
                if (task.repetitions > 0) {
                    MetaDot()
                    Text(
                        text = "${task.repetitions} repasos",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Trailing: chevron en modo normal
        if (!isSelectionMode) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = "Estudiar con el tutor",
                tint = if (task.isUrgent)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun MetaDot() {
    Box(
        modifier = Modifier
            .size(3.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
private fun DifficultyChip(label: String, tone: ConceptDifficulty) {
    val (bg, fg) = when (tone) {
        ConceptDifficulty.HARD -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        ConceptDifficulty.MEDIUM -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        ConceptDifficulty.EASY -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        ConceptDifficulty.UNKNOWN -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = bg
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun BulkActionBar(
    selectedCount: Int,
    selectedTitles: List<String>,
    onConfirm: (List<String>) -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Estudiar $selectedCount concepto${if (selectedCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Abriré el chat del curso con todo como contexto",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
            TextButton(
                onClick = onCancel,
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) { Text("Cancelar") }
            FilledTonalButton(
                onClick = { onConfirm(selectedTitles) },
                colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Iniciar")
            }
        }
    }
}

@Composable
private fun ReviewsLoadingState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.5.dp
            )
            Text(
                text = "Buscando repasos pendientes…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// =========================================================================================
//  Shared components
// =========================================================================================

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionIcon: ImageVector? = null,
    actionDescription: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (actionIcon != null && onAction != null) {
            IconButton(
                onClick = onAction,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = actionDescription,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// Wrapper privado de Scaffold para que el Preview no requiera hiltViewModel()
// (definido solo a nivel de archivo para no contaminar la API pública)
@Composable
private fun Scaffold(
    containerColor: Color = MaterialTheme.colorScheme.background,
    topBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    androidx.compose.material3.Scaffold(
        containerColor = containerColor,
        topBar = topBar,
        content = content
    )
}

// =========================================================================================
//  Helpers
// =========================================================================================

// parseCourseColor movido a pe.khipuai.app.ui.theme.CourseColor (T-16: helper compartido).

// =========================================================================================
//  Previews
// =========================================================================================

private fun previewNotes(): List<CompactNoteUiModel> = listOf(
    CompactNoteUiModel(
        id = "n1",
        title = "Cálculo Diferencial - Cap 3",
        snippet = "La derivada parcial mide la tasa de cambio de una función respecto a una variable…",
        dateTag = "15 Jun",
        subCategory = "Teoría"
    ),
    CompactNoteUiModel(
        id = "n2",
        title = "Regla de la cadena",
        snippet = "La regla de la cadena permite derivar funciones compuestas f(g(x))…",
        dateTag = "12 Jun",
        subCategory = "Importante"
    ),
    CompactNoteUiModel(
        id = "n3",
        title = "Ejercicios resueltos",
        snippet = "12 problemas de derivadas con solución paso a paso…",
        dateTag = "10 Jun",
        subCategory = "Práctica"
    )
)

private fun previewReviews(): List<ReviewItemUiModel> = listOf(
    ReviewItemUiModel(
        id = "r1",
        title = "Derivada Parcial",
        courseName = "Cálculo Diferencial",
        dueLabel = "Vence hoy",
        isUrgent = true,
        difficulty = ConceptDifficulty.HARD,
        repetitions = 2,
        easeFactor = 1.7f
    ),
    ReviewItemUiModel(
        id = "r2",
        title = "Regla de la Cadena",
        courseName = "Cálculo Diferencial",
        dueLabel = "Vence hoy",
        isUrgent = true,
        difficulty = ConceptDifficulty.MEDIUM,
        repetitions = 3,
        easeFactor = 2.0f
    ),
    ReviewItemUiModel(
        id = "r3",
        title = "Integral Definida",
        courseName = "Cálculo Diferencial",
        dueLabel = "En 2 días",
        isUrgent = false,
        difficulty = ConceptDifficulty.MEDIUM,
        repetitions = 4,
        easeFactor = 2.2f
    ),
    ReviewItemUiModel(
        id = "r4",
        title = "Límites laterales",
        courseName = "Cálculo Diferencial",
        dueLabel = "En 5 días",
        isUrgent = false,
        difficulty = ConceptDifficulty.EASY,
        repetitions = 8,
        easeFactor = 2.5f
    )
)

private fun previewNodes(): List<GraphNodeUiModel> = listOf(
    GraphNodeUiModel("Derivada", "check", NodeStatus.DOMINADO, 0.5f, 0.2f),
    GraphNodeUiModel("Límite", "check", NodeStatus.DOMINADO, 0.3f, 0.5f),
    GraphNodeUiModel("Integral", "circle", NodeStatus.EN_PROGRESO, 0.7f, 0.5f),
    GraphNodeUiModel("Serie", "lock", NodeStatus.BLOQUEADO, 0.2f, 0.8f),
    GraphNodeUiModel("Continuidad", "circle", NodeStatus.EN_PROGRESO, 0.8f, 0.8f)
)

@Preview(showBackground = true, widthDp = 380, heightDp = 1500, name = "Estado con datos")
@Composable
private fun CourseDetailScreenPreview() {
    KhipuAITheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            CourseDetailContent(
                state = CourseDetailUiState(
                    courseId = "c1",
                    courseName = "Cálculo Diferencial",
                    categoryName = "Ingeniería",
                    courseColor = "#4A90E2",
                    courseProgress = 78,
                    notes = previewNotes(),
                    totalNotesCount = 3,
                    showAllNotes = false,
                    upcomingReviews = previewReviews(),
                    previewNodes = previewNodes(),
                    isLoading = false
                ),
                courseColor = Color(0xFF4A90E2),
                padding = PaddingValues(0.dp),
                onBackClick = {},
                onAskTutor = {},
                onAddNote = {},
                onNoteClick = {},
                onToggleShowAll = {},
                onRename = { _, _ -> },
                onDelete = {},
                onReassociate = { _, _ -> },
                onExpandMap = {},
                onStudySingle = {},
                onStudyMultiple = {}
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 800, name = "Estado vacío")
@Composable
private fun CourseDetailScreenEmptyPreview() {
    KhipuAITheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            CourseDetailContent(
                state = CourseDetailUiState(
                    courseId = "c1",
                    courseName = "Cálculo Diferencial",
                    categoryName = "",
                    courseColor = "#4A90E2",
                    courseProgress = 0,
                    notes = emptyList(),
                    totalNotesCount = 0,
                    showAllNotes = false,
                    upcomingReviews = emptyList(),
                    previewNodes = emptyList(),
                    isLoading = false
                ),
                courseColor = Color(0xFF4A90E2),
                padding = PaddingValues(0.dp),
                onBackClick = {},
                onAskTutor = {},
                onAddNote = {},
                onNoteClick = {},
                onToggleShowAll = {},
                onRename = { _, _ -> },
                onDelete = {},
                onReassociate = { _, _ -> },
                onExpandMap = {},
                onStudySingle = {},
                onStudyMultiple = {}
            )
        }
    }
}


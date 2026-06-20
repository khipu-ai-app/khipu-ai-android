package pe.khipuai.app.ui.screens.coursedetail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    onBackClick: () -> Unit,
    onNoteClick: (String) -> Unit,
    onExpandMapClick: () -> Unit,
    onNavigateToTutor: (String) -> Unit = {},
    viewModel: CourseDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.courseName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToTutor(uiState.courseId) }) {
                        Icon(imageVector = Icons.Default.ChatBubble, contentDescription = "Tutor del Curso")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Iniciar Flujo de Captura / Nueva Nota */ },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Nota", modifier = Modifier.size(28.dp))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // SECCIÓN 1: Banner Header del Curso
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), RoundedCornerShape(99.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(uiState.categoryName.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = uiState.courseName, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(uiState.courseId.take(8), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Widget de Progreso Compacto Interno
                        Card(
                            modifier = Modifier.width(140.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Progreso", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${uiState.courseProgress}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { uiState.courseProgress / 100f },
                                    modifier = Modifier.fillMaxWidth().clip(CircleShape),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }

            // SECCIÓN 2: Panel Dual (Notas + Mapa de Conocimiento Lateral simulado)
            item {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {

                    // Sub-Bloque: Tus Notas
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Tus Notas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = { /* Ver todas */ }) { Text("Ver todas") }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (uiState.notes.isEmpty()) {
                            Text(
                                text = "Aún no tienes notas en este curso.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                uiState.notes.forEach { note ->
                                    var menuExpanded by remember { mutableStateOf(false) }
                                    var renameDialogExpanded by remember { mutableStateOf(false) }
                                    var renameText by remember { mutableStateOf(note.title) }
                                    var deleteDialogExpanded by remember { mutableStateOf(false) }
                                    var reassociateDialogExpanded by remember { mutableStateOf(false) }

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
                                                            viewModel.renameNote(note.id, renameText)
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
                                            text = { Text("¿Estás seguro de que deseas eliminar permanentemente el apunte '${note.title}'? Esta acción no se puede deshacer.") },
                                            confirmButton = {
                                                TextButton(
                                                    onClick = {
                                                        viewModel.deleteNote(note.id)
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

                                    if (reassociateDialogExpanded) {
                                        AlertDialog(
                                            onDismissRequest = { reassociateDialogExpanded = false },
                                            title = { Text("Asociar a Materia / Curso") },
                                            text = {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(max = 300.dp)
                                                        .verticalScroll(rememberScrollState())
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                viewModel.reassociateNote(note.id, null)
                                                                reassociateDialogExpanded = false
                                                            }
                                                            .padding(vertical = 12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        RadioButton(
                                                            selected = uiState.courseId.isBlank(),
                                                            onClick = {
                                                                viewModel.reassociateNote(note.id, null)
                                                                reassociateDialogExpanded = false
                                                            }
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("Ninguno (General)")
                                                    }
                                                    
                                                    HorizontalDivider()

                                                    uiState.availableCourses.forEach { course ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable {
                                                                    viewModel.reassociateNote(note.id, course.id)
                                                                    reassociateDialogExpanded = false
                                                                }
                                                                .padding(vertical = 12.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            RadioButton(
                                                                selected = uiState.courseId == course.id,
                                                                onClick = {
                                                                    viewModel.reassociateNote(note.id, course.id)
                                                                    reassociateDialogExpanded = false
                                                                }
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(course.name)
                                                        }
                                                    }
                                                }
                                            },
                                            confirmButton = {},
                                            dismissButton = {
                                                TextButton(onClick = { reassociateDialogExpanded = false }) {
                                                    Text("Cancelar")
                                                }
                                            }
                                        )
                                    }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onNoteClick(note.id) },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Description,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = note.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = note.snippet,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(note.subCategory, style = MaterialTheme.typography.labelSmall)
                                                    }
                                                    Text(
                                                        text = note.dateTag,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
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
                                                        text = { Text("Cambiar Materia") },
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
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Sub-Bloque: Mini Mapa Mental de Neo4j
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Hub, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Mapa del Curso Semántico", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Lienzo del Grafo con Desenfoque y Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        ) {

                            // Renderizado de las relaciones de red (Líneas vectoriales de fondo)
                            val outlineColor = MaterialTheme.colorScheme.outlineVariant
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawLine(color = outlineColor, start = Offset(size.width * 0.5f, size.height * 0.2f), end = Offset(size.width * 0.3f, size.height * 0.5f), strokeWidth = 3f)
                                drawLine(color = outlineColor, start = Offset(size.width * 0.5f, size.height * 0.2f), end = Offset(size.width * 0.7f, size.height * 0.5f), strokeWidth = 3f)
                                drawLine(color = outlineColor, start = Offset(size.width * 0.3f, size.height * 0.5f), end = Offset(size.width * 0.5f, size.height * 0.8f), strokeWidth = 3f)
                                drawLine(color = outlineColor, start = Offset(size.width * 0.7f, size.height * 0.5f), end = Offset(size.width * 0.5f, size.height * 0.8f), strokeWidth = 3f)
                            }

                            // Posicionamiento absoluto matemático de los Nodos del Grafo
                            uiState.previewNodes.forEach { node ->
                                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                    val xPos = maxWidth * node.xOffsetFraction - 36.dp
                                    val yPos = maxHeight * node.yOffsetFraction - 24.dp

                                    Column(
                                        modifier = Modifier.offset(x = xPos, y = yPos),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(
                                                    color = when(node.status) {
                                                        NodeStatus.DOMINADO -> MaterialTheme.colorScheme.tertiaryContainer
                                                        NodeStatus.EN_PROGRESO -> MaterialTheme.colorScheme.secondaryContainer
                                                        NodeStatus.BLOQUEADO -> MaterialTheme.colorScheme.surfaceVariant
                                                    },
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when(node.iconName) {
                                                    "check" -> Icons.Default.Check
                                                    "lock" -> Icons.Default.Lock
                                                    "circle" -> Icons.Default.Circle
                                                    else -> Icons.Default.Functions
                                                },
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(node.label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp))
                                    }
                                }
                            }

                            // Capa de Acción de Vidrio Esmerilado (Glass Overlay)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.1f))
                                    .clickable { onExpandMapClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Button(
                                    onClick = onExpandMapClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.OpenInFull, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Expandir Grafo Completo")
                                }
                            }
                        }
                    }
                }
            }

            // SECCIÓN 3: Próximos Repasos (Spaced Repetition Scheduler)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.EventAvailable, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Próximos Repasos adaptativos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    ) {
                        Column {
                            uiState.upcomingReviews.forEachIndexed { index, task ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(
                                                    if (task.isUrgent) MaterialTheme.colorScheme.errorContainer
                                                    else MaterialTheme.colorScheme.surfaceVariant,
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (task.isUrgent) Icons.Default.PriorityHigh else Icons.AutoMirrored.Filled.MenuBook,
                                                contentDescription = null,
                                                tint = if (task.isUrgent) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Column {
                                            Text(task.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            Text(task.scheduleText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    IconButton(
                                        onClick = { viewModel.completeReviewTask(task.id) },
                                        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape).size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Hecho", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                                    }
                                }
                                if (index < uiState.upcomingReviews.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}
package pe.khipuai.app.ui.screens.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import pe.khipuai.app.ui.components.BottomNavigationBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    onNavigateToTab: (Int) -> Unit,
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToNote: (String) -> Unit = {},
    onNavigateToConcept: (String) -> Unit = {},  // T-10: navegar al grafo del concepto
    onNavigateToDailyDeck: () -> Unit = {},
    onStartCourseReview: (String) -> Unit = {},  // T-10: noteId de la primera nota del bloque
    viewModel: PlannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Mostrar snackbar cuando hay mensaje
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // Diálogo de confirmación para "Marcar todo como completado"
    uiState.confirmCompleteBlockId?.let { blockId ->
        val block = uiState.studyBlocks.find { it.id == blockId }
        AlertDialog(
            onDismissRequest = { viewModel.dismissConfirmMarkAll() },
            title = { Text("Marcar como completado") },
            text = {
                Text(
                    "Esto marcará ${block?.tasks?.size ?: 0} conceptos como recordados (calidad 4). " +
                    "El algoritmo SM-2 los reprogramará automáticamente. ¿Continuar?"
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmMarkAllCompleted() }) {
                    Text("Confirmar", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissConfirmMarkAll() }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Khipu AI",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.userName.firstOrNull()?.toString()?.uppercase() ?: "U",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
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
                selectedTab = 2,
                onTabSelected = onNavigateToTab
            )
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
        } else if (uiState.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.errorMessage ?: "Error al cargar la agenda.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            val totalDue = uiState.studyBlocks.sumOf { it.tasks.filter { !it.isCompleted }.size }
            // T-13: contamos cuántos conceptos están marcados como
            // repasados hoy (el feedback "¡X repasados hoy!" del empty
            // state). Si el usuario no tiene repasados hoy, el mensaje
            // queda neutro.
            val totalCompletedToday = uiState.studyBlocks.sumOf { it.tasks.filter { it.isCompleted }.size }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                item { 
                    DailyAgendaHeader(onCalendarClick = onNavigateToCalendar) 
                }

                if (totalDue == 0) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "¡Sin repasos pendientes hoy!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (totalCompletedToday > 0) {
                                        "Ya repasaste $totalCompletedToday ${if (totalCompletedToday == 1) "concepto" else "conceptos"} hoy. " +
                                        "El algoritmo SM-2 agendará nuevos cuando sea el momento óptimo."
                                    } else {
                                        "El algoritmo SM-2 agendará nuevos conceptos cuando sea el momento óptimo."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    item {
                        StudyDeckLauncherCard(
                            totalDue = totalDue,
                            onStartDeck = onNavigateToDailyDeck
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Desglose por cursos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(uiState.studyBlocks) { block ->
                        CourseBreakdownCard(
                            block = block,
                            onTaskClick = { task ->
                                if (task.noteId != null) {
                                    onNavigateToNote(task.noteId)
                                } else {
                                    // El concepto existe pero aún no está
                                    // en ninguna nota. Mostramos el grafo del
                                    // concepto para que el usuario explore
                                    // (o confirme que el concepto no está
                                    // conectado a nada todavía).
                                    onNavigateToConcept(task.title)
                                }
                            },
                            onSubmitRating = { conceptId, rating ->
                                viewModel.submitRating(block.id, conceptId, rating)
                            },
                            onPostpone = { viewModel.postponeBlock(block.id) },
                            onMarkAllCompleted = { viewModel.requestMarkAllCompleted(block.id) },
                            onStartReview = { _ ->
                                // T-10: navega a ReviewSession usando la
                                // primera nota del bloque que tenga noteId.
                                // Si NINGÚN concepto del bloque tiene nota
                                // (caso raro, ej. conceptos huérfanos en el
                                // grafo), igual abrimos la ReviewSession sin
                                // nota: el backend devolverá un set vacío o
                                // un error 404 manejable. En ese caso el
                                // usuario puede cerrar la pantalla.
                                val firstNoteId = block.tasks.mapNotNull { it.noteId }.firstOrNull()
                                if (firstNoteId != null) {
                                    onStartCourseReview(firstNoteId)
                                } else {
                                    viewModel.setSnackbarMessage(
                                        "Este bloque no tiene notas asociadas todavía. Sube apuntes primero."
                                    )
                                }
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun DailyAgendaHeader(
    onCalendarClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tu Agenda Diaria",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(
                    onClick = onCalendarClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Ver Calendario",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Calendario",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = Color(0xFFE8F5E8),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "🔋 SM-2",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Repasos priorizados por el algoritmo de memoria espaciada SuperMemo-2. Toca cada concepto para autoevaluarte.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun StudyDeckLauncherCard(
    totalDue: Int,
    onStartDeck: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Style, // Represents a deck of cards
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Tu Mazo de Estudio",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tienes $totalDue conceptos programados para repasar hoy con SM-2.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth(0.8f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onStartDeck,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Comenzar Repaso Diario",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CourseBreakdownCard(
    block: StudyBlock,
    onTaskClick: (Task) -> Unit,
    onSubmitRating: (conceptId: String, rating: Int) -> Unit,
    onPostpone: () -> Unit,
    onMarkAllCompleted: () -> Unit,
    onStartReview: (noteId: String?) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cabecera del bloque
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(block.color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = block.time,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    if (block.isAISuggestion) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "SM-2 IA",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = block.duration,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opciones",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Posponer al mañana") },
                            onClick = {
                                menuExpanded = false
                                onPostpone()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Schedule, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Marcar todos como completados") },
                            onClick = {
                                menuExpanded = false
                                onMarkAllCompleted()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.DoneAll, contentDescription = null)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = block.subject,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // T-10/T-13: lista de conceptos con autoevaluación inline [0-5] y
            // long-press para "Ver nota donde aparece". Separamos
            // visualmente los pendientes (arriba) de los repasados hoy
            // (abajo) con un Divider con texto.
            val pending = block.tasks.filter { !it.isCompleted }
            val completed = block.tasks.filter { it.isCompleted }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                pending.forEach { task ->
                    ConceptTaskItem(
                        task = task,
                        onTap = { onTaskClick(task) },
                        onSubmitRating = { rating -> onSubmitRating(task.id, rating) }
                    )
                }
                if (completed.isNotEmpty() && pending.isNotEmpty()) {
                    // T-13: separador visual. Solo aparece si hay
                    // AMBOS grupos (sino no aporta nada).
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Repasados hoy",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                completed.forEach { task ->
                    ConceptTaskItem(
                        task = task,
                        onTap = { onTaskClick(task) },
                        onSubmitRating = { rating -> onSubmitRating(task.id, rating) }
                    )
                }
            }

            // T-10: botón "Iniciar repaso →" al final de cada bloque de curso.
            // Diferencia con "Comenzar Repaso Diario": este es SOLO los
            // conceptos pendientes de ESE bloque, no del mazo completo.
            // El botón aparece si hay tareas pendientes. El noteId se
            // resuelve dentro del callback (puede ser null si el bloque
            // es de conceptos huérfanos, en cuyo caso se muestra un
            // snackbar al usuario).
            if (block.tasks.any { !it.isCompleted }) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onStartReview(block.noteId) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Iniciar repaso",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("→")
                }
            }
        }
    }
}

/**
 * T-10: un concepto de un bloque de curso. Autoevaluación inline con
 * botones [0][1][2][3][4][5] (toca un número = SM-2 rating). El
 * long-press muestra un menu con "Ver nota donde aparece este concepto".
 *
 * Los botones se muestran SIEMPRE (incluso si está completado) para
 * permitir corregir la calificación. El último rating queda
 * visualmente resaltado con un fondo más fuerte.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ConceptTaskItem(
    task: Task,
    onTap: () -> Unit,
    onSubmitRating: (Int) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (task.isCompleted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                else Color.Transparent
            )
            .combinedClickable(
                onClick = onTap,
                onLongClick = { menuExpanded = true }
            )
            .padding(vertical = 6.dp, horizontal = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Checkbox de completado
            Icon(
                imageVector = if (task.isCompleted) Icons.Default.CheckCircle
                              else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (task.isCompleted) "Completado" else "Pendiente",
                tint = if (task.isCompleted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            // T-13: badge "X/5" cuando el concepto está completado y
            // tenemos el rating. Color verde si fue ≥3 (recordado),
            // rojo si fue <3 (olvidado). Esto refuerza el feedback del
            // SM-2 sin tener que abrir la nota.
            if (task.isCompleted && task.lastRating != null) {
                Spacer(modifier = Modifier.width(8.dp))
                val rating = task.lastRating!!
                val isRemembered = rating >= 3
                val badgeColor = if (isRemembered) Color(0xFF2E7D32) else Color(0xFFC62828)
                Surface(
                    color = badgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isRemembered) Icons.Default.ThumbUp
                                          else Icons.Default.ThumbDown,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "$rating/5",
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Botones de autoevaluación [0-5] SM-2. SIEMPRE visibles para
        // permitir re-calificar (ej. el usuario tap [4] por error y
        // quiere cambiar a [2]). El botón del último rating queda
        // más oscuro para feedback visual.
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(0, 1, 2, 3, 4, 5).forEach { rating ->
                val color = when (rating) {
                    0, 1 -> MaterialTheme.colorScheme.error
                    2 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                }
                // Si el último rating guardado es este, lo resaltamos
                val isLast = task.lastRating == rating
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .padding(horizontal = 2.dp)
                        .clickable { onSubmitRating(rating) },
                    color = if (isLast) color.copy(alpha = 0.45f) else color.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$rating",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isLast) FontWeight.ExtraBold else FontWeight.Bold,
                            color = if (isLast) MaterialTheme.colorScheme.onPrimary
                                    else color
                        )
                    }
                }
            }
        }
    }

    // Long-press menu: "Ver nota donde aparece"
    DropdownMenu(
        expanded = menuExpanded,
        onDismissRequest = { menuExpanded = false }
    ) {
        DropdownMenuItem(
            text = { Text("Ver nota donde aparece") },
            onClick = {
                menuExpanded = false
                onTap()
            },
            leadingIcon = {
                Icon(Icons.Default.Article, contentDescription = null)
            }
        )
    }
}

@Composable
private fun MentalLoadIndicator(
    label: String,
    level: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            color = color.copy(alpha = 0.2f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = level,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(
                                when (level) {
                                    "Alta" -> 0.8f
                                    "Media" -> 0.5f
                                    else -> 0.3f
                                }
                            )
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(color)
                    )
                }
            }
        }
    }
}
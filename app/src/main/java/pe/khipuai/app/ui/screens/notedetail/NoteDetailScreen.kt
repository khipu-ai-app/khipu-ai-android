package pe.khipuai.app.ui.screens.notedetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import pe.khipuai.app.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteDetailScreen(
    onBackClick: () -> Unit,
    onReviewClick: () -> Unit,
    onAskTutorClick: (concept: String?, courseId: String?, noteId: String, noteTitle: String) -> Unit,
    onStudyGuideClick: () -> Unit = {},
    onNavigateToQuizCreation: () -> Unit = {},
    onViewOriginalClick: (String) -> Unit = {},
    onScheduleClick: (String, String) -> Unit,
    viewModel: NoteDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    var menuExpanded by remember { mutableStateOf(false) }
    var renameDialogExpanded by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(uiState.title) }
    var deleteDialogExpanded by remember { mutableStateOf(false) }
    var reassociateDialogExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.title) {
        renameText = uiState.title
    }

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
                            viewModel.renameNote(renameText)
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
            text = { Text("¿Estás seguro de que deseas eliminar permanentemente el apunte '${uiState.title}'? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteNote(onBackClick)
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
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    // Opción "General" (Ningún curso)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.reassociateCourse(null)
                                reassociateDialogExpanded = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.courseId == null,
                            onClick = {
                                viewModel.reassociateCourse(null)
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
                                    viewModel.reassociateCourse(course.id)
                                    reassociateDialogExpanded = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.courseId == course.id,
                                onClick = {
                                    viewModel.reassociateCourse(course.id)
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

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(uiState.title, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(0.7f), maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.action_back))
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = stringResource(id = R.string.action_options))
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Renombrar") },
                                onClick = {
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
                            DropdownMenuItem(
                                text = { Text("Agendar repaso manual") },
                                onClick = {
                                    menuExpanded = false
                                    onScheduleClick(uiState.noteId, uiState.title)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null
                                    )
                                }
                            )
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
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // SECCIÓN: Acciones Principales
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onReviewClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Repasar", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onAskTutorClick(null, uiState.courseId, uiState.noteId, uiState.title) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Chat Tutor", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // SECCIÓN: Acciones Secundarias
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = { onViewOriginalClick(java.net.URLEncoder.encode(uiState.uploadId, "UTF-8")) }) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ver Archivo")
                    }
                    TextButton(onClick = onStudyGuideClick) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Guía")
                    }
                    TextButton(onClick = onNavigateToQuizCreation) {
                        Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Quiz")
                    }
                }
            }

            // SECCIÓN: Metadatos
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(id = R.string.label_captured_date, uiState.capturedDate), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(uiState.courseName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // SECCIÓN: Resumen Ejecutivo Khipu (IA)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Acento de borde izquierdo semántico
                        Box(modifier = Modifier.width(4.dp).height(120.dp).background(MaterialTheme.colorScheme.secondaryContainer).align(Alignment.CenterStart))

                        Column(modifier = Modifier.padding(16.dp).padding(start = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                                Text(stringResource(id = R.string.title_khipu_summary), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.aiSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            // SECCIÓN: Burbujas de Conceptos Clave (Eliminamos texto extraído y lo hacemos interactivo)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
                            Text(stringResource(id = R.string.title_key_concepts), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.keyConcepts.forEach { concept ->
                                SuggestionChip(
                                    onClick = { onAskTutorClick(concept, uiState.courseId, uiState.noteId, uiState.title) },
                                    label = { Text(concept) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // SECCIÓN: Mini Mapa de Conocimiento Local
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Hub, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                Text("Mapa de Conceptos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            if (uiState.showLocalGraph) {
                                if (uiState.isGraphLoading) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(40.dp))
                                    }
                                } else {
                                    val nodesJson = uiState.d3NodesJson
                                    val edgesJson = uiState.d3EdgesJson
                                    androidx.compose.ui.viewinterop.AndroidView(
                                        factory = { ctx ->
                                            android.webkit.WebView(ctx).apply {
                                                layoutParams = android.view.ViewGroup.LayoutParams(
                                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                                )
                                                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                                                settings.javaScriptEnabled = true
                                                settings.domStorageEnabled = true
                                                settings.allowFileAccessFromFileURLs = true
                                                settings.allowUniversalAccessFromFileURLs = true
                                                settings.setSupportZoom(true)
                                                settings.builtInZoomControls = true
                                                settings.displayZoomControls = false

                                                webViewClient = object : android.webkit.WebViewClient() {
                                                    override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                                        super.onPageFinished(view, url)
                                                        val nb64 = android.util.Base64.encodeToString(
                                                            nodesJson.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP
                                                        )
                                                        val eb64 = android.util.Base64.encodeToString(
                                                            edgesJson.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP
                                                        )
                                                        view?.evaluateJavascript("loadGraph('$nb64', '$eb64')", null)
                                                    }
                                                }
                                                loadUrl("file:///android_asset/mindmap.html")
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Button(
                                        onClick = { viewModel.toggleLocalGraph() },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                                    ) {
                                        Icon(Icons.Default.AccountTree, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Cargar Grafo de Conceptos")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECCIÓN: Historial de Repaso (Línea de Tiempo Vectorial Nativa)
            // T-06: usa datos reales de GET /notes/{id}/review-history agrupados
            // por fecha. Verde si avg ≥ 3, naranja si < 3.
            item {
                ReviewHistoryCard(
                    sessions = uiState.reviewSessions,
                    isLoading = uiState.isReviewHistoryLoading,
                    errorMessage = uiState.reviewHistoryError,
                    onRetry = { viewModel.retryReviewHistory() }
                )
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

/**
 * T-06: tarjeta con la línea de tiempo de repasos. Tres estados diferenciados:
 *  - [sessions] no vacía: timeline con conectores verticales
 *  - [isLoading]: skeleton/spinner
 *  - [errorMessage]: card de error con botón "Reintentar"
 *  - empty (sessions vacía y no loading ni error): mensaje "Aún no has repasado"
 *
 * Color del nodo: verde si [ReviewSessionUiModel.isPositive] (avg ≥ 3),
 * naranja si no.
 */
@Composable
private fun ReviewHistoryCard(
    sessions: List<ReviewSessionUiModel>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = stringResource(id = R.string.title_review_history),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            when {
                isLoading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                    }
                }
                errorMessage != null -> {
                    ReviewHistoryError(errorMessage = errorMessage, onRetry = onRetry)
                }
                sessions.isEmpty() -> {
                    Text(
                        text = "Aún no has repasado conceptos de esta nota. " +
                            "Toca el botón de repaso en la nota para empezar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    ReviewHistoryTimeline(sessions = sessions)
                }
            }
        }
    }
}

@Composable
private fun ReviewHistoryTimeline(sessions: List<ReviewSessionUiModel>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        sessions.forEachIndexed { index, session ->
            ReviewHistoryTimelineItem(
                session = session,
                isLast = index == sessions.lastIndex
            )
        }
    }
}

@Composable
private fun ReviewHistoryTimelineItem(
    session: ReviewSessionUiModel,
    isLast: Boolean
) {
    val nodeColor = if (session.isPositive) {
        MaterialTheme.colorScheme.primary  // verde para avg ≥ 3
    } else {
        MaterialTheme.colorScheme.error     // naranja/rojo para avg < 3
    }
    val nodeContentColor = if (session.isPositive) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onError
    }
    val lineColor = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Columna del nodo + línea conectora
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(IntrinsicSize.Min),
            contentAlignment = Alignment.TopCenter
        ) {
            if (!isLast) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawLine(
                        color = lineColor,
                        start = Offset(size.width / 2, 24.dp.toPx()),
                        end = Offset(size.width / 2, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(nodeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = nodeContentColor
                )
            }
        }

        // Contenido de la sesión
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            Text(
                text = session.date,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            val avg = String.format("%.1f", session.averageRating)
            Text(
                text = "$avg / 5 — ${session.conceptsReviewed} " +
                    if (session.conceptsReviewed == 1) "concepto" else "conceptos",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // T-06: lista compacta de los conceptos repasados
            if (session.concepts.isNotEmpty()) {
                val preview = session.concepts.take(3)
                val remaining = session.concepts.size - preview.size
                val conceptsText = preview.joinToString(", ") { "${it.name} (${it.rating}/5)" } +
                    if (remaining > 0) " y $remaining más" else ""
                Text(
                    text = conceptsText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // T-06: próxima fecha de repaso programada
            if (session.nextReviewDate != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Próximo repaso: " + formatNextReviewDate(session.nextReviewDate),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatNextReviewDate(iso: String): String {
    // iso viene como "YYYY-MM-DD" del backend
    val parts = iso.take(10).split("-")
    if (parts.size != 3) return iso
    val months = mapOf(
        1 to "Ene", 2 to "Feb", 3 to "Mar", 4 to "Abr",
        5 to "May", 6 to "Jun", 7 to "Jul", 8 to "Ago",
        9 to "Sep", 10 to "Oct", 11 to "Nov", 12 to "Dic"
    )
    return "${parts[2]} ${months[parts[1].toInt()] ?: ""}"
}

@Composable
private fun ReviewHistoryError(errorMessage: String, onRetry: () -> Unit) {
    Column {
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reintentar")
            }
        }
    }
}
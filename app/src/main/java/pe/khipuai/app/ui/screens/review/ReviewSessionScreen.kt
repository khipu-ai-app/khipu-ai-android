package pe.khipuai.app.ui.screens.review

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import pe.khipuai.app.data.remote.dto.ReviewConceptResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewSessionScreen(
    onBackClick: () -> Unit,
    onComplete: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    onViewNote: (String) -> Unit = {},
    viewModel: ReviewSessionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    var showExitDialog by remember { mutableStateOf(false) }
    var showConceptList by remember { mutableStateOf(false) }
    var showTutorSheet by remember { mutableStateOf(false) }
    var tutorQuestion by remember { mutableStateOf("") }
    var tutorAnswers by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isTutorLoading by remember { mutableStateOf(false) }
    var previousFlashcardIndex by remember { mutableStateOf(uiState.currentIndex) }
    LaunchedEffect(uiState.currentIndex) {
        if (uiState.currentIndex != previousFlashcardIndex) {
            tutorAnswers = emptyMap()
            previousFlashcardIndex = uiState.currentIndex
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("¿Seguro que quieres salir?") },
            text = { Text("Tu progreso se guardará hasta el concepto actual.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onBackClick()
                }) { Text("Salir") }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showConceptList && !uiState.isComplete) {
        AlertDialog(
            onDismissRequest = { showConceptList = false },
            title = { Text("Conceptos del mazo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    uiState.concepts.forEachIndexed { index, concept ->
                        val status = when {
                            index < uiState.currentIndex -> "✅"
                            index == uiState.currentIndex -> "◀"
                            else -> "○"
                        }
                        Text(
                            text = "$status ${concept.label}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                index < uiState.currentIndex -> MaterialTheme.colorScheme.onSurfaceVariant
                                index == uiState.currentIndex -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showConceptList = false }) { Text("Cerrar") }
            }
        )
    }

    if (uiState.errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            title = { Text("Error") },
            text = { Text(uiState.errorMessage ?: "") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissError()
                    viewModel.loadReviewSession()
                }) { Text("Reintentar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.noteTitle,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        if (uiState.courseName != null) {
                            Text(
                                text = uiState.courseName!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showExitDialog = true }) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHome) {
                        Icon(Icons.Default.Home, contentDescription = "Inicio")
                    }
                    if (!uiState.isComplete && uiState.concepts.isNotEmpty()) {
                        IconButton(onClick = { showConceptList = true }) {
                            Icon(Icons.Default.List, contentDescription = "Lista de conceptos")
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Preparando tu repaso...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                uiState.concepts.isEmpty() && !uiState.isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "No se encontraron conceptos",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when (uiState.entryPoint) {
                                ReviewEntryPoint.DAILY_DECK -> "No hay conceptos pendientes de repaso hoy."
                                ReviewEntryPoint.COURSE -> "Este curso no tiene conceptos pendientes."
                                ReviewEntryPoint.NOTE -> "Esta nota no tiene conceptos para repasar."
                                ReviewEntryPoint.CONCEPT -> "No se encontró el concepto."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = onBackClick,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Volver") }
                    }
                }

                uiState.isComplete -> {
                    if (uiState.resultsSummary != null) {
                        ReviewResultContent(
                            summary = uiState.resultsSummary!!,
                            entryPoint = uiState.entryPoint,
                            onComplete = onComplete,
                            onNavigateToHome = onNavigateToHome,
                            onRestartDifficult = { viewModel.restartWithDifficult() }
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "¡Estás al día!",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No tienes conceptos pendientes ahora.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = onBackClick,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Volver") }
                        }
                    }
                }

                else -> {
                    ReviewActiveContent(
                        uiState = uiState,
                        onToggleFlip = { viewModel.toggleFlip() },
                        onRate = { rating ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.submitRating(rating)
                        },
                        onSkip = { viewModel.skipConcept() },
                        onGoBack = { viewModel.goBack() },
                        onViewNote = onViewNote,
                        onAskTutor = { concept ->
                            val existing = tutorAnswers[concept.label]
                            showTutorSheet = true
                            tutorQuestion = ""
                            if (existing == null) {
                                isTutorLoading = false
                            }
                        },
                    )
                }
            }
        }
    }

    if (showTutorSheet) {
        val currentConcept = uiState.currentConcept
        if (currentConcept != null) {
            val existingAnswer = tutorAnswers[currentConcept.label]
            val context = LocalContext.current

            ModalBottomSheet(
                onDismissRequest = { showTutorSheet = false },
                dragHandle = { BottomSheetDefaults.DragHandle() },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp),
                ) {
                    Text(
                        text = currentConcept.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (existingAnswer == null) {
                        OutlinedTextField(
                            value = tutorQuestion,
                            onValueChange = { tutorQuestion = it },
                            label = { Text("¿Qué quieres saber sobre este concepto?") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4,
                            enabled = !isTutorLoading,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (tutorQuestion.isNotBlank()) {
                                    isTutorLoading = true
                                    viewModel.askAboutConcept(
                                        conceptName = currentConcept.label,
                                        definition = currentConcept.definition,
                                        noteTitle = currentConcept.sourceNoteTitle,
                                        question = tutorQuestion,
                                        onResult = { result ->
                                            isTutorLoading = false
                                            val answer = result.getOrElse { "Error al consultar al tutor." }
                                            tutorAnswers = tutorAnswers + (currentConcept.label to answer)
                                        },
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isTutorLoading && tutorQuestion.isNotBlank(),
                        ) {
                            if (isTutorLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                Text("Preguntar")
                            }
                        }
                    } else {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState()).fillMaxWidth()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text(
                                    text = existingAnswer
                                        .replace(Regex("###?\\s*"), "")
                                        .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
                                        .replace(Regex("\\*(.+?)\\*"), "$1")
                                        .replace(Regex("`(.+?)`"), "$1"),
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(onClick = { showTutorSheet = false }) {
                                Text("Cerrar")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun markdownToHtml(md: String): String {
    return md
        .replace(Regex("### (.+)"), "<h3>$1</h3>")
        .replace(Regex("\\*\\*(.+?)\\*\\*"), "<b>$1</b>")
        .replace(Regex("\\*(.+?)\\*"), "<i>$1</i>")
        .replace(Regex("`(.+?)`"), "<code>$1</code>")
        .replace(Regex("^[-*] (.+)", RegexOption.MULTILINE), "• $1")
        .replace(Regex("\\[([^]]+)]\\(([^)]+)\\)"), "<a href='$2'>$1</a>")
        .replace("\n", "<br>")
}

@Composable
private fun ReviewActiveContent(
    uiState: ReviewSessionUiState,
    onToggleFlip: () -> Unit,
    onRate: (Int) -> Unit,
    onSkip: () -> Unit,
    onGoBack: () -> Unit,
    onViewNote: (String) -> Unit,
    onAskTutor: (ReviewConceptResponse) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            progress = { uiState.progressPercent },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Text(
            text = uiState.progress,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Start).padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedContent(
            targetState = uiState.currentIndex,
            transitionSpec = {
                slideInHorizontally(
                    animationSpec = tween(400),
                    initialOffsetX = { fullWidth -> fullWidth }
                ) togetherWith slideOutHorizontally(
                    animationSpec = tween(400),
                    targetOffsetX = { fullWidth -> -fullWidth }
                )
            },
            label = "Flashcard Transition"
        ) { index ->
            val concept = uiState.concepts[index]
            FlashcardView(
                concept = concept,
                isFlipped = uiState.isFlipped,
                onToggleFlip = onToggleFlip,
                onViewNote = { noteId -> onViewNote(noteId) },
                onAskTutor = { onAskTutor(concept) },
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.isFlipped) {
            RatingSection(
                isSubmitting = uiState.isSubmitting,
                onRate = onRate,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (uiState.canGoBack) {
                TextButton(onClick = onGoBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Anterior", style = MaterialTheme.typography.labelMedium)
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            if (uiState.canSkip) {
                TextButton(onClick = onSkip) {
                    Text("Saltar", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun FlashcardView(
    concept: ReviewConceptResponse,
    isFlipped: Boolean,
    onToggleFlip: () -> Unit,
    onViewNote: (String) -> Unit = {},
    onAskTutor: () -> Unit = {},
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "Flip Animation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 300.dp)
            .padding(horizontal = 20.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable(onClick = onToggleFlip),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (rotation <= 90f) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        if (rotation <= 90f) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = concept.label,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Icon(
                        imageVector = Icons.Default.SmartButton,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Toca para ver la respuesta",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp)
                    .graphicsLayer { rotationY = 180f }
            ) {
                Text(
                    text = concept.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))

                val hasDefinition = concept.definition != null && concept.definition != "Definición no disponible." && concept.definition != ""
                val hasSnippet = concept.sourceSnippet != null && concept.sourceSnippet != ""

                when {
                    hasDefinition -> {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = concept.definition!!,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Start,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    hasSnippet -> {
                        Text(
                            text = "Contexto de la nota",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "\"${concept.sourceSnippet}\"",
                                modifier = Modifier.padding(14.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Start,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    else -> {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "La IA identificó este concepto en tu mapa cognitivo, pero no generó una definición en el glosario principal. Usa el botón 'Preguntar a Khipu' abajo para explorarlo.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (concept.noteId != null) {
                            OutlinedButton(
                                onClick = { onViewNote(concept.noteId) },
                                shape = RoundedCornerShape(99.dp),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ver en la nota", fontWeight = FontWeight.Medium)
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onAskTutor,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(99.dp),
                ) {
                    Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Preguntar al tutor", fontWeight = FontWeight.Medium)
                }

                if (concept.sourceNoteTitle != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "📖 ${concept.sourceNoteTitle}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun RatingSection(
    isSubmitting: Boolean,
    onRate: (Int) -> Unit
) {
    val colors = listOf(
        Color(0xFF8B0000),
        Color(0xFFF44336),
        Color(0xFFFF9800),
        Color(0xFFFFC107),
        Color(0xFF8BC34A),
        Color(0xFF4CAF50),
    )
    val labels = listOf("Nada", "Casi nada", "Poco", "Esfuerzo", "Bien", "Perfecto")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isSubmitting) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        } else {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Evalúa tu recuerdo",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (i in 0..2) {
                    RatingButton(i, labels[i], colors[i], Modifier.weight(1f)) { onRate(i) }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (i in 3..5) {
                    RatingButton(i, labels[i], colors[i], Modifier.weight(1f)) { onRate(i) }
                }
            }
        }
    }
}

@Composable
fun RatingButton(
    rating: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "$rating", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
            Text(text = label, fontSize = 10.sp, color = Color.White)
        }
    }
}

@Composable
private fun ReviewResultContent(
    summary: ReviewResultsSummary,
    entryPoint: ReviewEntryPoint,
    onComplete: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    onRestartDifficult: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val percent = if (summary.total > 0) ((summary.remembered.toFloat() / summary.total) * 100).toInt() else 100

        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { percent / 100f },
                modifier = Modifier.size(120.dp),
                strokeWidth = 12.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "¡Repaso completado!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "${summary.remembered} conceptos recordados bien",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF4CAF50),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "${summary.forgotten} conceptos a repasar pronto",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFFFF9800),
            fontWeight = FontWeight.Medium
        )

        if (summary.nextReviewDate.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Próximo repaso programado para: ${summary.nextReviewDate.take(10)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Detalle por concepto",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        summary.results.forEach { result ->
            val remembered = result.rating >= 3
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                Text(
                    text = if (remembered) "✅" else "❌",
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = result.conceptName,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${result.rating}/5",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (remembered) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = if (entryPoint == ReviewEntryPoint.DAILY_DECK) onNavigateToHome else onComplete,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(99.dp),
        ) {
            Text(
                text = when (entryPoint) {
                    ReviewEntryPoint.DAILY_DECK -> "Volver al Planner"
                    ReviewEntryPoint.COURSE -> "Volver al curso"
                    ReviewEntryPoint.NOTE -> "Volver a la nota"
                    ReviewEntryPoint.CONCEPT -> "Volver"
                },
                fontWeight = FontWeight.Bold
            )
        }

        if (summary.forgotten > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onRestartDifficult,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(99.dp),
            ) {
                Text("Repasar los difíciles de nuevo")
            }
        }
    }
}

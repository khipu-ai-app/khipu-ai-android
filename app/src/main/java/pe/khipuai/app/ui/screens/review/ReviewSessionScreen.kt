package pe.khipuai.app.ui.screens.review

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import pe.khipuai.app.data.remote.dto.ReviewConceptResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewSessionScreen(
    onBackClick: () -> Unit,
    onComplete: () -> Unit,
    viewModel: ReviewSessionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("khipu_prefs", Context.MODE_PRIVATE)

    var showExitDialog by remember { mutableStateOf(false) }
    var showTutorial by remember {
        mutableStateOf(!prefs.getBoolean("review_session_tutorial_shown", false))
    }

    if (showTutorial && !uiState.isLoading && !uiState.isComplete && uiState.concepts.isNotEmpty()) {
        LaunchedEffect(Unit) {
            delay(2000)
            prefs.edit().putBoolean("review_session_tutorial_shown", true).apply()
            showTutorial = false
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
                }) {
                    Text("Salir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancelar")
                }
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
                }) {
                    Text("Reintentar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Repasando: ${uiState.noteTitle}",
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
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                        )
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
                            text = "Esta nota aún no tiene conceptos extraídos para repasar.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = onBackClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Volver")
                        }
                    }
                }

                uiState.isComplete -> {
                    if (uiState.resultsSummary != null) {
                        ReviewResultContent(
                            summary = uiState.resultsSummary!!,
                            onComplete = onComplete,
                            onRestartDifficult = { viewModel.restartWithDifficult() }
                        )
                    } else {
                        // Empty state (no concepts due)
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
                                text = "No tienes conceptos pendientes de esta nota. Tu próximo repaso es pronto.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = onBackClick,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Volver")
                            }
                        }
                    }
                }
                
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Progress
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

                        // Flashcard
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
                                onToggleFlip = { viewModel.toggleFlip() }
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Rating buttons
                        if (uiState.isFlipped) {
                            RatingSection(
                                isSubmitting = uiState.isSubmitting,
                                onRate = { viewModel.submitRating(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
            
            // Tutorial overlay
            if (showTutorial && !uiState.isLoading && !uiState.isComplete && uiState.concepts.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(32.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Auto-evaluación",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Evalúa qué tan bien recuerdas el concepto en una escala de 0 a 5. Esto nos ayuda a programar el próximo repaso.",
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FlashcardView(
    concept: ReviewConceptResponse,
    isFlipped: Boolean,
    onToggleFlip: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "Flip Animation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(horizontal = 24.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable(onClick = onToggleFlip),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        if (rotation <= 90f) {
            // Front
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = concept.label,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "¿Recuerdas este concepto?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(32.dp))
                OutlinedButton(onClick = onToggleFlip) {
                    Text("Ver definición")
                }
            }
        } else {
            // Back (Rotated by -180 internally so text is readable)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .graphicsLayer { rotationY = 180f },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = concept.label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = concept.definition ?: concept.sourceSnippet ?: "Definición no disponible.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                if (concept.sourceNoteTitle != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "De: ${concept.sourceNoteTitle}",
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
    // 0 No recuerdo nada (Rojo oscuro)
    // 1 Casi nada (Rojo)
    // 2 Lo recuerdo poco (Naranja)
    // 3 Lo recuerdo con esfuerzo (Amarillo)
    // 4 Lo recuerdo bien (Verde claro)
    // 5 Lo recuerdo perfecto (Verde)
    
    val colors = listOf(
        Color(0xFF8B0000), // Rojo oscuro
        Color(0xFFF44336), // Rojo
        Color(0xFFFF9800), // Naranja
        Color(0xFFFFC107), // Amarillo
        Color(0xFF8BC34A), // Verde claro
        Color(0xFF4CAF50)  // Verde
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
            Text(
                "Evalúa tu recuerdo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 0..2) {
                    RatingButton(i, labels[i], colors[i], Modifier.weight(1f)) { onRate(i) }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 3..5) {
                    RatingButton(i, labels[i], colors[i], Modifier.weight(1f)) { onRate(i) }
                    if (i < 5) Spacer(modifier = Modifier.width(8.dp))
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
    onComplete: () -> Unit,
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

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(99.dp),
        ) {
            Text("Volver a la nota", fontWeight = FontWeight.Bold)
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

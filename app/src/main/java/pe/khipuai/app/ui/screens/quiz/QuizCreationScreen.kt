package pe.khipuai.app.ui.screens.quiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuizCreationScreen(
    onNavigateBack: () -> Unit,
    viewModel: QuizCreationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.quizTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (uiState.isDashboardMode) {
                            onNavigateBack()
                        } else {
                            viewModel.exitQuiz()
                        }
                    }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        bottomBar = {
            if (!uiState.isDashboardMode && !uiState.isLoading && uiState.questions.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 8.dp,
                    shadowElevation = 16.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!uiState.isSubmitted) {
                            val allAnswered = uiState.questions.all { it.selectedOptionIndex != null }
                            Button(
                                onClick = { viewModel.submitQuiz() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                enabled = allAnswered
                            ) {
                                Icon(imageVector = Icons.Default.Send, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (allAnswered) "ENVIAR RESPUESTAS" else "COMPLETA TODAS LAS PREGUNTAS")
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.resetQuiz() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("REINTENTAR")
                                }
                                Button(
                                    onClick = { viewModel.exitQuiz() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("VOLVER AL DASHBOARD")
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(64.dp), strokeWidth = 6.dp)
                Spacer(modifier = Modifier.height(24.dp))
                Text("Analizando tus apuntes...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Generando preguntas de nivel ${uiState.selectedDifficulty}...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (uiState.isDashboardMode) {
            QuizDashboardContent(
                uiState = uiState,
                viewModel = viewModel,
                onNewQuizClick = { viewModel.toggleAddQuestionsDialog(true) },
                onQuizClick = { viewModel.openQuiz(it) },
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                if (uiState.isSubmitted) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Resultados",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "${uiState.score} / ${uiState.questions.size}",
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "preguntas correctas",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                itemsIndexed(uiState.questions) { index, question ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(text = question.question, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                question.options.forEachIndexed { optIndex, option ->
                                    val isSelected = question.selectedOptionIndex == optIndex
                                    val isCorrect = question.correctOptionIndex == optIndex
                                    val isSubmitted = uiState.isSubmitted
                                    
                                    val backgroundColor = when {
                                        !isSubmitted -> if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent
                                        isCorrect -> Color(0xFFE8F5E9) // Verde suave
                                        isSelected && !isCorrect -> Color(0xFFFFEBEE) // Rojo suave
                                        else -> Color.Transparent
                                    }

                                    val borderColor = when {
                                        !isSubmitted -> if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                        isCorrect -> Color(0xFF4CAF50)
                                        isSelected && !isCorrect -> Color(0xFFF44336)
                                        else -> Color.Transparent
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(backgroundColor)
                                            .clickable(enabled = !isSubmitted) {
                                                viewModel.selectOption(question.id, optIndex)
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = null,
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = if (isSubmitted && isSelected && !isCorrect) Color(0xFFF44336) else MaterialTheme.colorScheme.primary
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = option,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected || (isSubmitted && isCorrect)) FontWeight.Medium else FontWeight.Normal,
                                            color = if (isSubmitted && isCorrect) Color(0xFF2E7D32) 
                                                    else if (isSubmitted && isSelected && !isCorrect) Color(0xFFC62828)
                                                    else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isSubmitted) {
                                            Spacer(modifier = Modifier.weight(1f))
                                            if (isCorrect) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Correcto",
                                                    tint = Color(0xFF4CAF50),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            } else if (isSelected && !isCorrect) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Incorrecto",
                                                    tint = Color(0xFFF44336),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                if (uiState.isSubmitted && question.explanation.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Explicación: " + question.explanation,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
    
    if (uiState.isAddQuestionsDialogOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.toggleAddQuestionsDialog(false) },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
                    // Solucionamos problemas de scroll de LazyColumn anidado usando verticalScroll
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Configurar Cuestionario",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Personaliza las preguntas para evaluar tu conocimiento sobre este apunte.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Topics Selection
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Selecciona los temas a evaluar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        uiState.noteTopics.forEach { topic ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleTopicSelection(topic.name) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = topic.isSelected,
                                    onCheckedChange = { viewModel.toggleTopicSelection(topic.name) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = topic.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (topic != uiState.noteTopics.last()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
                
                // Difficulty
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Nivel de dificultad",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Principiante", "Intermedio", "Avanzado").forEach { diff ->
                            val isSelected = uiState.selectedDifficulty == diff
                            Surface(
                                selected = isSelected,
                                onClick = { viewModel.updateDifficulty(diff) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = diff,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
                
                // Questions Count
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Cantidad de preguntas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uiState.sliderQuestionsCount.toInt()}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = uiState.sliderQuestionsCount,
                        onValueChange = { viewModel.updateSliderValue(it) },
                        valueRange = 5f..15f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Submit Action
                Button(
                    onClick = { 
                        viewModel.generateNewQuiz() 
                        viewModel.toggleAddQuestionsDialog(false)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = uiState.noteTopics.any { it.isSelected },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "GENERAR CUESTIONARIO",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun QuizDashboardContent(
    uiState: QuizCreationUiState,
    viewModel: QuizCreationViewModel,
    onNewQuizClick: () -> Unit,
    onQuizClick: (pe.khipuai.app.data.remote.dto.StandaloneQuizResponse) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.missingTopics.isNotEmpty()) {
            item {
                Text(
                    text = "Temas pendientes de repasar:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.missingTopics.joinToString(" • "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (uiState.pastQuizzes.isNotEmpty()) {
            item {
                Text(
                    text = "¡Felicidades! Has cubierto todos los temas de esta nota en tus cuestionarios.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
        
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Tus cuestionarios",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(onClick = onNewQuizClick) {
                    Text("Generar Quiz")
                }
            }
        }
        
        if (uiState.pastQuizzes.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Aún no has generado ningún cuestionario para esta nota.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            itemsIndexed(uiState.pastQuizzes.reversed()) { _, quiz ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onQuizClick(quiz) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = "Puntaje: ${quiz.score ?: 0}/${quiz.questions.size}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(text = "Dificultad: ${quiz.difficulty}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = "Temas: ${if (quiz.topics.isNotEmpty()) quiz.topics.joinToString(", ") else "General"}", style = MaterialTheme.typography.bodySmall)
                                    if (quiz.createdAt != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "Fecha: ${quiz.createdAt.take(10)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                IconButton(onClick = { quiz.id?.let { viewModel.deleteQuiz(it) } }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar Quiz", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
            }
        }
    }
}
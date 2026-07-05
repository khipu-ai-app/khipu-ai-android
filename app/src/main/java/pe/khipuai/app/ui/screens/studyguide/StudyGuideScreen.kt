package pe.khipuai.app.ui.screens.studyguide

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyGuideScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToReview: (String) -> Unit,
    viewModel: StudyGuideViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Guía de Estudio", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHome) {
                        Icon(Icons.Default.Home, contentDescription = "Inicio")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (uiState.errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp), contentAlignment = Alignment.Center) {
                Text(text = uiState.errorMessage ?: "Error", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                // Título y fecha
                item {
                    Text(text = uiState.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "Generado por IA • ${uiState.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Sección 1 - Resumen Ejecutivo
                item {
                    StudySection(icon = Icons.Default.Description, title = "Resumen ejecutivo", content = uiState.executiveSummary)
                }

                // Sección 2 - Glosario
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Book, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Glosario Clave", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            uiState.glossary.forEach { term ->
                                Text(text = term.term, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(text = term.definition, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 8.dp))
                            }
                        }
                    }
                }

                // Sección 3 - Quiz de Práctica
                if (uiState.questions.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Quiz, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Quiz de Práctica", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                
                                val totalQuestions = uiState.questions.size
                                val answeredCount = uiState.questions.count { it.selectedOptionIndex != null }
                                val allAnswered = answeredCount == totalQuestions
                                
                                Text("Has respondido $answeredCount de $totalQuestions preguntas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(16.dp))

                                uiState.questions.forEachIndexed { qIdx, question ->
                                    Text(text = "Pregunta ${qIdx + 1} de $totalQuestions:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                    Text(text = question.question, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    question.options.forEachIndexed { oIdx, option ->
                                        val isSelected = question.selectedOptionIndex == oIdx
                                        val isCorrect = question.correctIndex == oIdx
                                        
                                        val containerColor = if (uiState.isQuizSubmitted) {
                                            when {
                                                isCorrect -> Color(0xFFD4EDDA) // Verde
                                                isSelected && !isCorrect -> Color(0xFFF8D7DA) // Rojo
                                                else -> Color.Transparent
                                            }
                                        } else Color.Transparent

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(containerColor, RoundedCornerShape(8.dp))
                                                .clickable(enabled = !uiState.isQuizSubmitted) { 
                                                    viewModel.selectQuizOption(question.id, oIdx) 
                                                }
                                                .padding(vertical = 4.dp, horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { if (!uiState.isQuizSubmitted) viewModel.selectQuizOption(question.id, oIdx) }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = option, style = MaterialTheme.typography.bodyMedium)
                                            
                                            if (uiState.isQuizSubmitted) {
                                                Spacer(modifier = Modifier.weight(1f))
                                                if (isCorrect) Icon(Icons.Default.Check, "Correcta", tint = Color(0xFF155724))
                                                else if (isSelected) Icon(Icons.Default.Close, "Incorrecta", tint = Color(0xFF721C24))
                                            }
                                        }
                                    }
                                    
                                    if (uiState.isQuizSubmitted) {
                                        Text(text = "Explicación: ${question.explanation}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, start = 8.dp))
                                    }
                                    
                                    Spacer(modifier = Modifier.height(24.dp))
                                }
                                
                                if (!uiState.isQuizSubmitted) {
                                    Button(
                                        onClick = { viewModel.submitQuiz() },
                                        enabled = allAnswered,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Enviar respuestas")
                                    }
                                } else {
                                    val correctCount = uiState.questions.count { it.selectedOptionIndex == it.correctIndex }
                                    val pct = ((correctCount.toFloat() / totalQuestions) * 100).toInt()
                                    
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Obtuviste $correctCount de $totalQuestions correctas ($pct%)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Button(
                                                onClick = { 
                                                    viewModel.noteId?.let { onNavigateToReview(it) }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Repasar respuestas incorrectas")
                                            }
                                            OutlinedButton(
                                                onClick = { viewModel.resetQuiz() },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Volver a intentar")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun StudySection(icon: ImageVector, title: String, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

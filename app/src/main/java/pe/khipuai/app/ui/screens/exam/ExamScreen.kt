package pe.khipuai.app.ui.screens.exam

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScreen(
    onBack: () -> Unit,
    viewModel: ExamViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val questions = state.questions
    val currentIndex = state.currentIndex
    val q = questions.getOrNull(currentIndex)

    var showConfirmFinish by remember { mutableStateOf(false) }

    val minutes = state.remainingSeconds / 60
    val seconds = state.remainingSeconds % 60
    val timeText = "%02d:%02d".format(minutes, seconds)
    val timerColor = when {
        state.remainingSeconds <= 60 -> MaterialTheme.colorScheme.error
        state.remainingSeconds < 300 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurface
    }

    // Interceptar botón atrás: si hay examen en curso, preguntar
    var showExitExamDialog by remember { mutableStateOf(false) }
    BackHandler(enabled = true) {
        showExitExamDialog = true
    }
    if (showExitExamDialog) {
        AlertDialog(
            onDismissRequest = { showExitExamDialog = false },
            title = { Text("Salir del examen?") },
            text = { Text("Perderás el progreso actual y el examen se cancelará.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitExamDialog = false
                    viewModel.cancelExam()
                    onBack()
                }) {
                    Text("Salir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitExamDialog = false }) {
                    Text("Seguir en el examen")
                }
            }
        )
    }

    if (showConfirmFinish) {
        val unanswered = questions.count { state.answers[it.id] == null }
        AlertDialog(
            onDismissRequest = { showConfirmFinish = false },
            title = { Text("Finalizar examen?") },
            text = {
                Text(if (unanswered > 0) "Quedan $unanswered pregunta(s) sin responder."
                     else "Has respondido todas las preguntas.")
            },
            confirmButton = {
                TextButton(onClick = { showConfirmFinish = false; viewModel.finishExam() }) {
                    Text("Finalizar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showConfirmFinish = false }) { Text("Seguir") } }
        )
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    if (q == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Cargando examen...", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Examen: ${state.courseName}", fontWeight = FontWeight.Bold) },
                actions = {
                    Text(timeText, color = timerColor, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                         modifier = Modifier.padding(end = 8.dp))
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            LinearProgressIndicator(
                progress = { (currentIndex + 1).toFloat() / questions.size },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
            )
            Spacer(Modifier.height(8.dp))
            Text("Pregunta ${currentIndex + 1} de ${questions.size}", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(16.dp))

            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                    Text(q.question, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                         lineHeight = 22.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(q.topic, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(20.dp))
                    q.options.forEachIndexed { idx, option ->
                        val selected = state.answers[q.id] == idx
                        val letter = 'A' + idx
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                .clickable { viewModel.selectAnswer(currentIndex, idx) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                                                 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(28.dp).clip(CircleShape).background(
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ), contentAlignment = Alignment.Center) {
                                    Text("$letter", color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(option, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                questions.indices.forEach { idx ->
                    val answered = state.answers[questions[idx].id] != null
                    Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp))
                        .background(if (answered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(onClick = { viewModel.goToQuestion(currentIndex - 1) }, enabled = currentIndex > 0) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = null)
                    Spacer(Modifier.width(4.dp)); Text("Anterior")
                }
                Button(onClick = {
                    if (currentIndex + 1 >= questions.size) { showConfirmFinish = true }
                    else { viewModel.goToQuestion(currentIndex + 1) }
                }) {
                    Text(if (currentIndex + 1 >= questions.size) "Finalizar" else "Siguiente")
                    if (currentIndex + 1 < questions.size) { Spacer(Modifier.width(4.dp)); Icon(Icons.Default.ChevronRight, contentDescription = null) }
                }
            }
        }
    }
}

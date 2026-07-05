package pe.khipuai.app.ui.screens.exam

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamResultScreen(
    onBackToCourse: () -> Unit,
    viewModel: ExamViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val result = state.result ?: return

    val percent = result.percentage.toInt()
    val isPassing = percent >= 60
    val scoreColor = if (isPassing) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
    val emoji = if (isPassing) "🎉" else "💪"
    val message = if (isPassing) "¡Buen trabajo!" else "Sigue practicando"

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Resultados", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState()),
               horizontalAlignment = Alignment.CenterHorizontally) {

            // Score circle
            Box(Modifier.size(140.dp).clip(CircleShape).background(scoreColor.copy(alpha = 0.1f)),
                 contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$percent%", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = scoreColor)
                    Text("${result.score}/${result.total}", style = MaterialTheme.typography.titleMedium, color = scoreColor)
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("$emoji $message", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            val minutes = result.timeSpentSeconds / 60
            val secs = result.timeSpentSeconds % 60
            Text("Tiempo: ${minutes}:${"%02d".format(secs)}", style = MaterialTheme.typography.bodyMedium,
                 color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(24.dp))

            // Topic breakdown
            if (result.topicBreakdown.isNotEmpty()) {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Desglose por tema", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        result.topicBreakdown.forEach { (topic, stats) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(topic, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                Text("${stats.correct}/${stats.total}", fontWeight = FontWeight.Bold,
                                     color = if (stats.correct >= stats.total / 2) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Weak concepts
            if (result.weakConcepts.isNotEmpty()) {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Conceptos a repasar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                             color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        result.weakConcepts.take(5).forEach { weak ->
                            Row(Modifier.padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("•", color = MaterialTheme.colorScheme.error)
                                Text(weak, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(onClick = onBackToCourse, modifier = Modifier.fillMaxWidth().height(52.dp),
                   shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Volver al curso", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(onClick = { viewModel.backToConfig() }, modifier = Modifier.fillMaxWidth().height(52.dp),
                           shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Intentar de nuevo", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}


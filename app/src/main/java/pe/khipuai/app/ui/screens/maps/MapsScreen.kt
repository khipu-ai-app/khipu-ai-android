package pe.khipuai.app.ui.screens.maps

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import pe.khipuai.app.ui.components.BottomNavigationBar
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Paint
import android.graphics.Color as AndroidColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapsScreen(
    onNavigateToTab: (Int) -> Unit,
    viewModel: MapsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
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
                            text = "U",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Notifications */ }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notificaciones",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            BottomNavigationBar(selectedTab = 3, onTabSelected = onNavigateToTab)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column {
                FilterSection(
                    selectedCourse = uiState.selectedCourse,
                    selectedDifficulty = uiState.selectedDifficulty,
                    onCourseChange = viewModel::updateCourse,
                    onDifficultyChange = viewModel::updateDifficulty
                )

                // Contenedor adaptativo según el estado asíncrono del backend
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    } else if (uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage ?: "Error",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    } else {
                        MindMapCanvas(
                            concepts = uiState.concepts,
                            selectedConcept = uiState.selectedConcept,
                            onConceptClick = viewModel::selectConcept
                        )
                    }
                }
            }

            uiState.selectedConcept?.let { selectedConcept ->
                ConceptBottomSheet(
                    concept = selectedConcept,
                    onDismiss = { viewModel.selectConcept(null) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSection(
    selectedCourse: String,
    selectedDifficulty: String,
    onCourseChange: (String) -> Unit,
    onDifficultyChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        var courseExpanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = courseExpanded,
            onExpandedChange = { courseExpanded = !courseExpanded },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = selectedCourse,
                onValueChange = { },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                label = { Text("Curso") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseExpanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(12.dp)
            )

            ExposedDropdownMenu(
                expanded = courseExpanded,
                onDismissRequest = { courseExpanded = false }
            ) {
                listOf("Anatomía Humana", "Microeconomía Avanzada", "Álgebra Lineal").forEach { course ->
                    DropdownMenuItem(
                        text = { Text(course) },
                        onClick = {
                            onCourseChange(course)
                            courseExpanded = false
                        }
                    )
                }
            }
        }

        var difficultyExpanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = difficultyExpanded,
            onExpandedChange = { difficultyExpanded = !difficultyExpanded },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = selectedDifficulty,
                onValueChange = { },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                label = { Text("Dificultad") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = difficultyExpanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(12.dp)
            )

            ExposedDropdownMenu(
                expanded = difficultyExpanded,
                onDismissRequest = { difficultyExpanded = false }
            ) {
                listOf("Todas", "Básica", "Intermedia", "Avanzada").forEach { difficulty ->
                    DropdownMenuItem(
                        text = { Text(difficulty) },
                        onClick = {
                            onDifficultyChange(difficulty)
                            difficultyExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MindMapCanvas(
    concepts: List<Concept>,
    selectedConcept: Concept?,
    onConceptClick: (Concept?) -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onConceptClick(null) }
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2

        // 1. Dibujar las líneas de conexión primero (detrás)
        concepts.forEach { concept ->
            concept.connections.forEach { connectionId ->
                val connectedConcept = concepts.find { it.id == connectionId }
                if (connectedConcept != null) {
                    drawConnection(
                        from = Offset(centerX + concept.position.x * size.width, centerY + concept.position.y * size.height),
                        to = Offset(centerX + connectedConcept.position.x * size.width, centerY + connectedConcept.position.y * size.height)
                    )
                }
            }
        }

        // 2. Dibujar los nodos y sus nombres
        concepts.forEach { concept ->
            val position = Offset(centerX + concept.position.x * size.width, centerY + concept.position.y * size.height)

            // Dibujamos el círculo
            drawConceptNode(concept = concept, position = position, isSelected = concept == selectedConcept)

            // ✨ NUEVO: Pintar el texto explicativo del nodo usando el nativeCanvas de Android
            val radius = when (concept.importance) {
                ConceptImportance.HIGH -> 40.dp.toPx()
                ConceptImportance.MEDIUM -> 30.dp.toPx()
                ConceptImportance.LOW -> 25.dp.toPx()
            }

            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    concept.title,
                    position.x,
                    position.y + radius + 16.dp.toPx(), // Posicionado estratégicamente abajo del círculo
                    Paint().apply {
                        color = AndroidColor.DKGRAY
                        textSize = 12.sp.toPx()
                        textAlign = Paint.Align.CENTER
                        isFakeBoldText = concept.importance == ConceptImportance.HIGH
                        isAntiAlias = true
                    }
                )
            }
        }
    }
}

private fun DrawScope.drawConnection(from: Offset, to: Offset) {
    drawLine(color = Color(0xFFDCDCDC), start = from, end = to, strokeWidth = 2.dp.toPx())
}

private fun DrawScope.drawConceptNode(concept: Concept, position: Offset, isSelected: Boolean) {
    val radius = when (concept.importance) {
        ConceptImportance.HIGH -> 40.dp.toPx()
        ConceptImportance.MEDIUM -> 30.dp.toPx()
        ConceptImportance.LOW -> 25.dp.toPx()
    }

    if (isSelected) {
        drawCircle(color = concept.color.copy(alpha = 0.3f), radius = radius + 8.dp.toPx(), center = position)
    }

    drawCircle(color = concept.color, radius = radius, center = position)
    drawCircle(color = Color.White, radius = radius * 0.5f, center = position) // Anillo interior estilizado
}

@Composable
private fun ConceptBottomSheet(concept: Concept, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Text(text = "CONCEPTO SELECCIONADO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = concept.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = concept.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    StatItem(icon = Icons.Default.Description, label = "${concept.filesCount} Archivos")
                    StatItem(icon = Icons.Default.School, label = "Módulo ${concept.lessonNumber}")
                }
            }
        }
    }
}

@Composable
private fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
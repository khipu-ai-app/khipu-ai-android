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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import pe.khipuai.app.ui.components.BottomNavigationBar
import kotlin.math.*

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
                    // Profile picture placeholder
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(
                selectedTab = 3, // Maps tab
                onTabSelected = onNavigateToTab
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column {
                // Filter section
                FilterSection(
                    selectedCourse = uiState.selectedCourse,
                    selectedDifficulty = uiState.selectedDifficulty,
                    onCourseChange = viewModel::updateCourse,
                    onDifficultyChange = viewModel::updateDifficulty
                )
                
                // Mind map area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    MindMapCanvas(
                        concepts = uiState.concepts,
                        selectedConcept = uiState.selectedConcept,
                        onConceptClick = viewModel::selectConcept
                    )
                }
            }
            
            // Bottom sheet for selected concept
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
        // Course filter
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
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseExpanded)
                },
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
                listOf(
                    "Inteligencia Artificial",
                    "Matemáticas",
                    "Historia",
                    "Psicología"
                ).forEach { course ->
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
        
        // Difficulty filter
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
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = difficultyExpanded)
                },
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
            .clickable { /* Handle canvas click */ }
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        
        // Draw connections first (behind nodes)
        concepts.forEach { concept ->
            concept.connections.forEach { connectionId ->
                val connectedConcept = concepts.find { it.id == connectionId }
                if (connectedConcept != null) {
                    drawConnection(
                        from = Offset(
                            centerX + concept.position.x * size.width,
                            centerY + concept.position.y * size.height
                        ),
                        to = Offset(
                            centerX + connectedConcept.position.x * size.width,
                            centerY + connectedConcept.position.y * size.height
                        )
                    )
                }
            }
        }
        
        // Draw concept nodes
        concepts.forEach { concept ->
            val position = Offset(
                centerX + concept.position.x * size.width,
                centerY + concept.position.y * size.height
            )
            
            drawConceptNode(
                concept = concept,
                position = position,
                isSelected = concept == selectedConcept
            )
        }
    }
}

private fun DrawScope.drawConnection(from: Offset, to: Offset) {
    drawLine(
        color = Color(0xFFE0E0E0),
        start = from,
        end = to,
        strokeWidth = 2.dp.toPx()
    )
}

private fun DrawScope.drawConceptNode(
    concept: Concept,
    position: Offset,
    isSelected: Boolean
) {
    val radius = when (concept.importance) {
        ConceptImportance.HIGH -> 40.dp.toPx()
        ConceptImportance.MEDIUM -> 30.dp.toPx()
        ConceptImportance.LOW -> 25.dp.toPx()
    }
    
    // Draw outer ring if selected
    if (isSelected) {
        drawCircle(
            color = concept.color.copy(alpha = 0.3f),
            radius = radius + 8.dp.toPx(),
            center = position
        )
    }
    
    // Draw main circle
    drawCircle(
        color = concept.color,
        radius = radius,
        center = position
    )
    
    // Draw icon (simplified)
    drawCircle(
        color = Color.White,
        radius = radius * 0.6f,
        center = position
    )
}

@Composable
private fun ConceptBottomSheet(
    concept: Concept,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "CONCEPTO SELECCIONADO",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Concept title
                Text(
                    text = concept.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Description
                Text(
                    text = concept.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Stats
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    StatItem(
                        icon = Icons.Default.Description,
                        label = "${concept.filesCount} Archivos"
                    )
                    
                    StatItem(
                        icon = Icons.Default.School,
                        label = "Lección ${concept.lessonNumber}"
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(6.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
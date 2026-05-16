package pe.khipuai.app.ui.screens.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import pe.khipuai.app.ui.components.BottomNavigationBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    onNavigateToTab: (Int) -> Unit,
    viewModel: PlannerViewModel = hiltViewModel()
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
                selectedTab = 2, // Planner tab
                onTabSelected = onNavigateToTab
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Header section
            item {
                DailyAgendaHeader()
            }
            
            // Study blocks
            items(uiState.studyBlocks) { block ->
                StudyBlockCard(
                    block = block,
                    onTaskToggle = { taskId ->
                        viewModel.toggleTask(block.id, taskId)
                    },
                    onMenuClick = { /* TODO: Show menu */ }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DailyAgendaHeader() {
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
            
            Surface(
                color = Color(0xFFE8F5E8),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "🔋 CARGA ÓPTIMA",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "2 bloques de enfoque profundo sugeridos hoy basados en tus próximas fechas de examen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun StudyBlockCard(
    block: StudyBlock,
    onTaskToggle: (String) -> Unit,
    onMenuClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with time and menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Time indicator
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(block.color),
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = block.time,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // AI suggestion badge
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
                                    contentDescription = "IA",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sugerencia IA",
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
                
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Más opciones",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Subject title
            Text(
                text = block.subject,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Tasks
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                block.tasks.forEach { task ->
                    TaskItem(
                        task = task,
                        onToggle = { onTaskToggle(task.id) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mental load indicator
            MentalLoadIndicator(
                label = "Carga Mental Sugerida",
                level = block.mentalLoadLevel,
                color = block.mentalLoadColor
            )
        }
    }
}

@Composable
private fun TaskItem(
    task: Task,
    onToggle: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary
            )
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (task.isCompleted) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textDecoration = if (task.isCompleted) {
                TextDecoration.LineThrough
            } else {
                TextDecoration.None
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
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = "Carga mental",
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
                
                // Progress bar
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
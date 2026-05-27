package pe.khipuai.app.ui.screens.maps

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import pe.khipuai.app.ui.components.BottomNavigationBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapsScreen(
    onNavigateToTab: (Int) -> Unit,
    viewModel: MapsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Referencia estable a la WebView para poder inyectarle datos vía evaluateJavascript
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    val pageLoaded = remember { mutableStateOf(false) }

    // Cuando llegan nuevos datos del backend Y la página ya está lista, actualizamos el grafo
    LaunchedEffect(uiState.d3NodesJson, uiState.d3EdgesJson) {
        if (pageLoaded.value && webViewRef.value != null) {
            val nodesEscaped = uiState.d3NodesJson
                .replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("$", "\\$")
            val edgesEscaped = uiState.d3EdgesJson
                .replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("$", "\\$")
            webViewRef.value?.evaluateJavascript(
                "updateGraphData(`$nodesEscaped`, `$edgesEscaped`)",
                null
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mapa Cognitivo",
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
                            text = "K",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
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
            BottomNavigationBar(selectedTab = 3, onTabSelected = onNavigateToTab)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Filtros de curso y retención
                FilterSection(
                    selectedCourse = uiState.selectedCourse,
                    selectedDifficulty = uiState.selectedDifficulty,
                    onCourseChange = viewModel::updateCourse,
                    onDifficultyChange = viewModel::updateDifficulty
                )

                // Área principal del grafo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // La WebView siempre está compuesta para no recrearla en cada recomposición.
                    // Se muestra/oculta con alpha según el estado de carga.
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.allowFileAccess = true
                                settings.setSupportZoom(true)
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false

                                addJavascriptInterface(
                                    GraphWebViewBridge { conceptId ->
                                        viewModel.selectConceptById(conceptId)
                                    },
                                    "AndroidBridge"
                                )

                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        pageLoaded.value = true
                                        // Si ya hay datos, inyectarlos inmediatamente
                                        val nodes = uiState.d3NodesJson
                                        val edges = uiState.d3EdgesJson
                                        if (nodes != "[]") {
                                            view?.evaluateJavascript(
                                                "updateGraphData(`${nodes.replace("`", "\\`").replace("$", "\\$")}`, `${edges.replace("`", "\\`").replace("$", "\\$")}`)",
                                                null
                                            )
                                        }
                                    }
                                }

                                loadUrl("file:///android_asset/mindmap.html")
                                webViewRef.value = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Overlay de carga encima del WebView
                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Cargando grafo cognitivo…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Overlay de error
                    if (uiState.errorMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = uiState.errorMessage ?: "Error desconocido",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedButton(onClick = { viewModel.updateCourse(uiState.selectedCourse) }) {
                                    Text("Reintentar")
                                }
                            }
                        }
                    }
                }
            }

            // BottomSheet nativo de Compose para el concepto seleccionado
            uiState.selectedConcept?.let { concept ->
                ConceptBottomSheet(
                    concept = concept,
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
                onValueChange = {},
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
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                label = { Text("Retención") },
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
                listOf("Todas", "Básica", "Intermedia", "Avanzada").forEach { diff ->
                    DropdownMenuItem(
                        text = { Text(diff) },
                        onClick = {
                            onDifficultyChange(diff)
                            difficultyExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConceptBottomSheet(concept: Concept, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CONCEPTO SELECCIONADO",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = concept.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Indicador de retención SM-2
                val retentionColor = if (concept.color == Color(0xFFD32F2F))
                    Color(0xFFEF5350)
                else
                    Color(0xFF4CAF50)

                Surface(
                    color = retentionColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (concept.color == Color(0xFFD32F2F))
                                Icons.Default.Warning
                            else
                                Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = retentionColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (concept.color == Color(0xFFD32F2F))
                                "Repaso pendiente · Prioridad alta"
                            else
                                "Retención óptima · SM-2 activo",
                            style = MaterialTheme.typography.labelSmall,
                            color = retentionColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = concept.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    ConceptStatItem(
                        icon = Icons.Default.Description,
                        label = "${concept.filesCount} Archivos"
                    )
                    ConceptStatItem(
                        icon = Icons.Default.School,
                        label = "Módulo ${concept.lessonNumber}"
                    )
                    ConceptStatItem(
                        icon = Icons.Default.TrendingUp,
                        label = when (concept.difficulty) {
                            ConceptDifficulty.BASIC -> "Básico"
                            ConceptDifficulty.INTERMEDIATE -> "Intermedio"
                            ConceptDifficulty.ADVANCED -> "Avanzado"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConceptStatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
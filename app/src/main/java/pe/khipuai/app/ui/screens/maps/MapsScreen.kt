package pe.khipuai.app.ui.screens.maps

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
    preselectedCourseId: String? = null,
    highlightConcept: String? = null,
    onNoteClick: (String) -> Unit = {},
    onStartReview: (conceptName: String) -> Unit = {},
    onAskTutor: (conceptName: String, courseId: String?) -> Unit = { _, _ -> },
    viewModel: MapsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Referencia estable a la WebView para poder inyectarle datos vía evaluateJavascript
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    val pageLoaded = remember { mutableStateOf(false) }

    // Control Estricto de Inyección: Delegamos a onPageFinished recargando el WebView
    LaunchedEffect(uiState.d3NodesJson, uiState.d3EdgesJson) {
        if (pageLoaded.value && webViewRef.value != null) {
            pageLoaded.value = false
            webViewRef.value?.reload()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = pe.khipuai.app.R.mipmap.ic_launcher_foreground),
                            contentDescription = "Logo Khipu AI",
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mapa Cognitivo",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
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
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Área principal del grafo
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                    // La WebView siempre está compuesta para no recrearla en cada recomposición.
                    // Se muestra/oculta con alpha según el estado de carga.
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                layoutParams = android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.allowFileAccess = true
                                settings.allowContentAccess = true
                                settings.allowFileAccessFromFileURLs = true
                                settings.allowUniversalAccessFromFileURLs = true
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
                                            val nodesB64 = Base64.encodeToString(nodes.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                                            val edgesB64 = Base64.encodeToString(edges.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                                            view?.evaluateJavascript(
                                                "loadGraph('$nodesB64', '$edgesB64', $isDark)",
                                                null
                                            )
                                            // Si la navegación vino con un concepto a resaltar,
                                            // pedimos al WebView que lo centre y destaque
                                            highlightConcept?.let { concept ->
                                                view?.evaluateJavascript(
                                                    "highlightNode('${java.net.URLDecoder.decode(concept, "UTF-8").replace("'", "\\'")}')",
                                                    null
                                                )
                                            }
                                        }
                                    }
                                }

                                loadUrl("file:///android_asset/mindmap.html")
                                webViewRef.value = this
                            }
                        },
                        update = { view ->
                            // Cuando cambia isDark o hay recomposición y la página ya cargó,
                            // actualizamos el tema inyectando JS.
                            if (pageLoaded.value) {
                                view.evaluateJavascript("setTheme($isDark)", null)
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
                                OutlinedButton(onClick = { viewModel.updateCourse(uiState.selectedCourseId, uiState.selectedCourseName) }) {
                                    Text("Reintentar")
                                }
                            }
                        }
                    }
                }

            // Filtros y buscador flotantes superpuestos en la parte superior
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Filtro de curso adaptado (peso 1)
                FilterSection(
                    selectedCourse = uiState.selectedCourseName,
                    courses = uiState.courses,
                    onCourseChange = viewModel::updateCourse,
                    modifier = Modifier.weight(1f)
                )

                // Buscador de nodos
                var searchQuery by remember { mutableStateOf("") }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    tonalElevation = 4.dp,
                    shadowElevation = 2.dp
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { 
                            searchQuery = it
                            webViewRef.value?.evaluateJavascript("searchNode('$it')", null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Buscar...", style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        trailingIcon = { 
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { 
                                    searchQuery = ""
                                    webViewRef.value?.evaluateJavascript("searchNode('')", null)
                                }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                }
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // BottomSheet nativo de Compose para el concepto seleccionado
            uiState.selectedConcept?.let { concept ->
                ConceptBottomSheet(
                    concept = concept,
                    detail = uiState.selectedConceptDetail,
                    isLoadingDetail = uiState.isLoadingDetail,
                    onDismiss = { viewModel.selectConcept(null) },
                    onGoToNote = { noteId -> onNoteClick(noteId) },
                    onStartReview = { onStartReview(concept.title) },
                    onAskTutor = { onAskTutor(concept.title, uiState.selectedCourseId.takeIf { it.isNotBlank() && it != "all" }) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSection(
    selectedCourse: String,
    courses: List<CourseOption>,
    onCourseChange: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var courseExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 4.dp,
        shadowElevation = 2.dp
    ) {
        ExposedDropdownMenuBox(
            expanded = courseExpanded,
            onExpandedChange = { courseExpanded = !courseExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedCourse,
                onValueChange = { },
                readOnly = true,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(
                        type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                        enabled = true
                    ),
                label = null,
                placeholder = { Text("Curso", style = MaterialTheme.typography.bodyMedium) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseExpanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = RoundedCornerShape(24.dp)
            )
            ExposedDropdownMenu(
                expanded = courseExpanded,
                onDismissRequest = { courseExpanded = false }
            ) {
                courses.forEach { course ->
                    DropdownMenuItem(
                        text = { Text(course.name) },
                        onClick = {
                            onCourseChange(course.id, course.name)
                            courseExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConceptBottomSheet(
    concept: Concept,
    detail: pe.khipuai.app.data.remote.dto.ConceptDetailResponse?,
    isLoadingDetail: Boolean,
    onDismiss: () -> Unit,
    onGoToNote: (String) -> Unit,
    onStartReview: () -> Unit,
    onAskTutor: () -> Unit
) {
    var showNotePicker by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 48.dp)) {
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        label = when (concept.difficulty) {
                            ConceptDifficulty.BASIC -> "Básico"
                            ConceptDifficulty.INTERMEDIATE -> "Intermedio"
                            ConceptDifficulty.ADVANCED -> "Avanzado"
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── 3 acciones del bottom sheet (F-09) ─────────────────
                ConceptActions(
                    isLoadingDetail = isLoadingDetail,
                    notesCount = detail?.notes?.size ?: 0,
                    onGoToNote = {
                        val notes = detail?.notes.orEmpty()
                        when {
                            notes.isEmpty() -> { /* no-op, botón deshabilitado */ }
                            notes.size == 1 -> onGoToNote(notes.first().id)
                            else -> showNotePicker = true
                        }
                    },
                    onStartReview = onStartReview,
                    onAskTutor = onAskTutor
                )
            }
        }

    // Picker de notas cuando el concepto está en varias
    if (showNotePicker && detail != null && detail.notes.size > 1) {
        AlertDialog(
            onDismissRequest = { showNotePicker = false },
            title = { Text("¿A qué nota quieres ir?") },
            text = {
                Column {
                    detail.notes.forEach { note ->
                        TextButton(
                            onClick = {
                                showNotePicker = false
                                onGoToNote(note.id)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(note.title, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotePicker = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun ConceptActions(
    isLoadingDetail: Boolean,
    notesCount: Int,
    onGoToNote: () -> Unit,
    onStartReview: () -> Unit,
    onAskTutor: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 1. Ir a la nota — solo si el concepto tiene al menos 1 nota
        FilledTonalButton(
            onClick = onGoToNote,
            enabled = !isLoadingDetail && notesCount > 0,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (notesCount > 1) "Ir a la nota ($notesCount opciones)" else "Ir a la nota",
                fontWeight = FontWeight.SemiBold
            )
        }

        // 2. Preguntar a Khipu — siempre visible
        OutlinedButton(
            onClick = onAskTutor,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Chat,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Preguntar a Khipu", fontWeight = FontWeight.SemiBold)
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

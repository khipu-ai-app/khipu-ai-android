package pe.khipuai.app.ui.screens.notedetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import pe.khipuai.app.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteDetailScreen(
    onBackClick: () -> Unit,
    onReviewClick: () -> Unit,
    onAskTutorClick: (String?) -> Unit,
    onStudyGuideClick: () -> Unit = {},
    onNavigateToQuizCreation: () -> Unit = {},
    onViewOriginalClick: (String) -> Unit = {},
    viewModel: NoteDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var menuExpanded by remember { mutableStateOf(false) }
    var renameDialogExpanded by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(uiState.title) }
    var deleteDialogExpanded by remember { mutableStateOf(false) }
    var reassociateDialogExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.title) {
        renameText = uiState.title
    }

    if (renameDialogExpanded) {
        AlertDialog(
            onDismissRequest = { renameDialogExpanded = false },
            title = { Text("Renombrar Apunte") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            viewModel.renameNote(renameText)
                        }
                        renameDialogExpanded = false
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameDialogExpanded = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (deleteDialogExpanded) {
        AlertDialog(
            onDismissRequest = { deleteDialogExpanded = false },
            title = { Text("Eliminar Apunte") },
            text = { Text("¿Estás seguro de que deseas eliminar permanentemente el apunte '${uiState.title}'? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteNote(onBackClick)
                        deleteDialogExpanded = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteDialogExpanded = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (reassociateDialogExpanded) {
        AlertDialog(
            onDismissRequest = { reassociateDialogExpanded = false },
            title = { Text("Asociar a Materia / Curso") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    // Opción "General" (Ningún curso)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.reassociateCourse(null)
                                reassociateDialogExpanded = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.courseId == null,
                            onClick = {
                                viewModel.reassociateCourse(null)
                                reassociateDialogExpanded = false
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ninguno (General)")
                    }
                    
                    HorizontalDivider()

                    uiState.availableCourses.forEach { course ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.reassociateCourse(course.id)
                                    reassociateDialogExpanded = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.courseId == course.id,
                                onClick = {
                                    viewModel.reassociateCourse(course.id)
                                    reassociateDialogExpanded = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(course.name)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { reassociateDialogExpanded = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.title, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(0.7f), maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.action_back))
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = stringResource(id = R.string.action_options))
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Renombrar") },
                                onClick = {
                                    renameDialogExpanded = true
                                    menuExpanded = false
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Cambiar Materia") },
                                onClick = {
                                    reassociateDialogExpanded = true
                                    menuExpanded = false
                                },
                                leadingIcon = { Icon(Icons.Default.Class, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Eliminar permanentemente", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    deleteDialogExpanded = true
                                    menuExpanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DeleteForever,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp,
                shadowElevation = 16.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onReviewClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(99.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.School, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(id = R.string.action_review_now), fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { onAskTutorClick(uiState.courseId) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(99.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Forum, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(id = R.string.action_ask_khipu), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // SECCIÓN 1: Mini Mapa de Conocimiento Local
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Hub, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                Text("Mapa de Conocimiento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            
                            // Botón Ver Original
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(99.dp))
                                    .clickable { onViewOriginalClick(java.net.URLEncoder.encode(uiState.uploadId, "UTF-8")) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text("Ver Documento", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            if (uiState.showLocalGraph) {
                                if (uiState.isGraphLoading) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(40.dp))
                                    }
                                } else {
                                    val nodesJson = uiState.d3NodesJson
                                    val edgesJson = uiState.d3EdgesJson
                                    androidx.compose.ui.viewinterop.AndroidView(
                                        factory = { ctx ->
                                            android.webkit.WebView(ctx).apply {
                                                layoutParams = android.view.ViewGroup.LayoutParams(
                                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                                )
                                                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                                                settings.javaScriptEnabled = true
                                                settings.domStorageEnabled = true
                                                settings.allowFileAccessFromFileURLs = true
                                                settings.allowUniversalAccessFromFileURLs = true
                                                settings.setSupportZoom(true)
                                                settings.builtInZoomControls = true
                                                settings.displayZoomControls = false

                                                webViewClient = object : android.webkit.WebViewClient() {
                                                    override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                                        super.onPageFinished(view, url)
                                                        val nb64 = android.util.Base64.encodeToString(
                                                            nodesJson.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP
                                                        )
                                                        val eb64 = android.util.Base64.encodeToString(
                                                            edgesJson.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP
                                                        )
                                                        view?.evaluateJavascript("loadGraph('$nb64', '$eb64')", null)
                                                    }
                                                }
                                                loadUrl("file:///android_asset/mindmap.html")
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Button(
                                        onClick = { viewModel.toggleLocalGraph() },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                                    ) {
                                        Icon(Icons.Default.AccountTree, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Cargar Grafo de Conceptos")
                                    }
                                }
                            }
                        }

                        // Footer
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(stringResource(id = R.string.label_captured_date, uiState.capturedDate), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(uiState.courseName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // SECCIÓN 2: Resumen Ejecutivo Khipu (IA)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Acento de borde izquierdo semántico del HTML
                        Box(modifier = Modifier.width(4.dp).height(120.dp).background(MaterialTheme.colorScheme.secondaryContainer).align(Alignment.CenterStart))

                        Column(modifier = Modifier.padding(16.dp).padding(start = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                                Text(stringResource(id = R.string.title_khipu_summary), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.aiSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = onStudyGuideClick,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Guía de Estudio")
                                }
                                
                                Button(
                                    onClick = onNavigateToQuizCreation,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                ) {
                                    Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Crear Quiz")
                                }
                            }
                        }
                    }
                }
            }

            // SECCIÓN 3: Burbujas de Conceptos Clave (Eliminamos texto extraído y lo hacemos interactivo)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
                            Text(stringResource(id = R.string.title_key_concepts), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.keyConcepts.forEachIndexed { index, concept ->
                                val chipColor = when(index) {
                                    0 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    1 -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                                Box(
                                    modifier = Modifier
                                        .background(chipColor, RoundedCornerShape(99.dp))
                                        .clickable { onAskTutorClick(concept) } // Mandamos el concepto como contexto especial
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface)
                                        Text(text = concept, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECCIÓN 4: Historial de Repaso (Línea de Tiempo Vectorial Nativa)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
                            Text(stringResource(id = R.string.title_review_history), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Column(modifier = Modifier.fillMaxWidth()) {
                            uiState.historyTimeline.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    // Dibujo Vectorial del Nodo y la Línea Conectora
                                    val lineColor = MaterialTheme.colorScheme.surfaceVariant
                                    Box(
                                        modifier = Modifier
                                            .width(24.dp)
                                            .height(IntrinsicSize.Min),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        if (index < uiState.historyTimeline.lastIndex) {
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                drawLine(
                                                    color = lineColor,
                                                    start = Offset(size.width / 2, 24.dp.toPx()),
                                                    end = Offset(size.width / 2, size.height),
                                                    strokeWidth = 2.dp.toPx()
                                                )
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(
                                                    if (item.type == HistoryItemType.REPASO_COMPLETADO) MaterialTheme.colorScheme.tertiaryContainer
                                                    else MaterialTheme.colorScheme.surfaceVariant,
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (item.type == HistoryItemType.REPASO_COMPLETADO) Icons.Default.Check else Icons.Default.EditNote,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }

                                    // Contenido del hito
                                    Column(modifier = Modifier.padding(bottom = 12.dp)) {
                                        Text(text = item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text(text = item.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}
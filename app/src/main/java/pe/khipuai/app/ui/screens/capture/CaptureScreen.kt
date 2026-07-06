package pe.khipuai.app.ui.screens.capture

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import android.app.Activity
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pe.khipuai.app.ui.components.BottomNavigationBar
import java.io.File

/**
 * T-05: CaptureScreen con 3 modos seleccionables desde una tab bar.
 *
 *  - CAMERA  → PreviewView de CameraX + botón de captura + flash + switch
 *  - UPLOAD  → Photo picker (Galería) — ya existía
 *  - PDF     → Open document (PDF) — ya existía
 *
 * El banner de uso y el destino de la nota son transversales a los 3 modos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    onNavigateToTab: (Int) -> Unit,
    onNavigateToProcessing: (String) -> Unit = {},
    onNavigateToSubscription: (String?) -> Unit = {},
    // T-17: navegar a la nota existente al hacer "Ver nota existente"
    // en el dialog de duplicado.
    onNavigateToNoteDetail: (String) -> Unit = {},
    viewModel: CaptureViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // T-02: escuchar el evento de "límite alcanzado" y navegar a Subscription
    LaunchedEffect(Unit) {
        viewModel.limitReachedEvents.collect {
            onNavigateToSubscription("limit_reached")
        }
    }

    // T-17: escuchar el evento de "upload exitoso" para iniciar el
    // polling. Se consume tanto para el flujo normal como para el
    // reintento forzado desde el dialog de duplicado.
    LaunchedEffect(Unit) {
        viewModel.uploadedEvents.collect { id ->
            onNavigateToProcessing(id)
        }
    }

    // T-13 combine: cuando el combine termina exitosamente, navegamos
    // a NoteDetail con el noteId.
    LaunchedEffect(Unit) {
        viewModel.combineUploadedEvents.collect { noteId ->
            onNavigateToNoteDetail(noteId)
        }
    }

    // T-13 evolution: cuando el usuario está agregando un archivo a
    // una nota existente, al éxito del upload navegamos de vuelta a
    // esa nota (no a Processing, porque la nota ya existía).
    // (El VM ya tiene preselectedNoteId, lo usamos desde el banner.)

    // T-02: recargar usage al volver al frente
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadUsage()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // T-05: el modo default es CAMERA según el plan de T-05
    var mode by remember { mutableStateOf(CaptureMode.CAMERA) }

    // Photo picker para el modo UPLOAD (múltiple)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val files = uris.mapNotNull { uriToFile(context, it, ".jpg") }
            if (files.isNotEmpty()) {
                viewModel.processFiles(files)
            }
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val files = uris.mapNotNull { uriToFile(context, it, ".pdf") }
            if (files.isNotEmpty()) {
                viewModel.processFiles(files)
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                            text = "Khipu AI",
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
            BottomNavigationBar(
                selectedTab = 1,
                onTabSelected = onNavigateToTab
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
    // Banner de uso Freemium (T-02) — visible en los 3 modos
            UsageBanner(
                capturesUsed = uiState.capturesUsed,
                capturesLimit = uiState.capturesLimit,
                isPro = uiState.isPro,
                onUpgradeClick = onNavigateToSubscription
            )

            // T-05: tab bar con los 3 modos de captura
            CaptureModeTabs(
                current = mode,
                onChange = { mode = it }
            )

            // Cuerpo del modo seleccionado
            when (mode) {
                CaptureMode.CAMERA -> CameraModeBody(
                    modifier = Modifier.weight(1f),
                    onProcessFiles = { files ->
                        viewModel.processFiles(files)
                    }
                )
                CaptureMode.UPLOAD -> UploadModeBody(
                    modifier = Modifier.weight(1f),
                    combineMode = uiState.combineMode,
                    onPickImage = {
                        imagePickerLauncher.launch("image/*")
                    }
                )
                CaptureMode.PDF -> PdfModeBody(
                    modifier = Modifier.weight(1f),
                    combineMode = uiState.combineMode,
                    onPickPdf = {
                        pdfPickerLauncher.launch(arrayOf("application/pdf"))
                    }
                )
            }

            // Destination section — común a los 3 modos
            DestinationSection(
                selectedDestination = uiState.selectedDestination,
                courses = uiState.courses,
                onDestinationChange = viewModel::updateDestination
            )

            // T-13 combine: toggle para activar modo combinación
            CombineToggleRow(
                isActive = uiState.combineMode,
                onToggle = viewModel::toggleCombineMode,
            )
        }
    }

    // T-17: dialog de "Documento duplicado". Se renderiza encima del
    // Scaffold cuando el backend rechaza un upload con 409. Ofrece 3
    // acciones al usuario:
    //   - Ver nota existente: navega a NoteDetail de la nota que ya
    //     tiene este archivo.
    //   - Subir de todas formas: reintenta con X-Force-Upload: true
    //     (el usuario confirma que es una nota distinta).
    //   - Cancelar: descarta el dialog sin hacer nada.
    uiState.duplicateDialog?.let { dialog ->
        val existingNoteId = dialog.info.existingNoteId
        val existingNoteTitle = dialog.info.existingNoteTitle
        val canNavigateToExisting = !existingNoteId.isNullOrBlank()

        AlertDialog(
            onDismissRequest = { viewModel.dismissDuplicateDialog() },
            icon = {
                Icon(
                    imageVector = Icons.Default.FileCopy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = "Documento duplicado",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    if (canNavigateToExisting) {
                        Text(
                            text = "Este archivo ya fue subido antes. Ya existe una nota con este contenido:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        // Card con la info de la nota existente
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = existingNoteTitle ?: "(sin título)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        // Caso edge: el upload anterior aún está siendo
                        // procesado por la IA. La nota todavía no existe,
                        // así que no podemos navegar a ella.
                        Text(
                            text = "Este archivo ya fue subido antes y está siendo procesado por la IA. Espera unos minutos a que termine.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                if (canNavigateToExisting) {
                    TextButton(onClick = {
                        viewModel.dismissDuplicateDialog()
                        onNavigateToNoteDetail(existingNoteId)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ver nota existente")
                    }
                } else {
                    // Sin nota existente, el confirmButton es solo cerrar
                    TextButton(onClick = { viewModel.dismissDuplicateDialog() }) {
                        Text("Entendido")
                    }
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { viewModel.dismissDuplicateDialog() }) {
                        Text("Cancelar")
                    }
                    TextButton(
                        onClick = { viewModel.forceUploadDuplicate() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Subir de todas formas", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        )
    }
}

// ── T-05: cuerpo del modo cámara ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraModeBody(
    modifier: Modifier = Modifier,
    onProcessFiles: (List<File>) -> Unit
) {
    val activity = LocalContext.current as Activity
    val context = LocalContext.current
    
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val files = scanResult?.pages?.mapNotNull { page ->
                uriToFile(context, page.imageUri, ".jpg")
            } ?: emptyList()
            if (files.isNotEmpty()) {
                onProcessFiles(files)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Usa el escáner inteligente para tomar tus apuntes.\nRecortará los bordes automáticamente.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    val options = GmsDocumentScannerOptions.Builder()
                        .setGalleryImportAllowed(false)
                        .setPageLimit(30)
                        .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                        .build()
                    val scanner = GmsDocumentScanning.getClient(options)
                    scanner.getStartScanIntent(activity).addOnSuccessListener { intentSender ->
                        scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                    }.addOnFailureListener { e ->
                        Log.e("CameraModeBody", "Scanner failed to start", e)
                    }
                },
                modifier = Modifier.size(height = 56.dp, width = 240.dp)
            ) {
                Icon(Icons.Default.DocumentScanner, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Escanear", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// ── Cuerpos de los otros modos (reutilizan la UI previa) ────────────────────

@Composable
private fun UploadModeBody(modifier: Modifier = Modifier, combineMode: Boolean, onPickImage: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (combineMode) "Selecciona varias imágenes para combinar" else "Sube imágenes desde tu galería",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onPickImage) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (combineMode) "Elegir imágenes" else "Elegir imagen")
            }
        }
    }
}

@Composable
private fun PdfModeBody(modifier: Modifier = Modifier, combineMode: Boolean, onPickPdf: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (combineMode) "Selecciona varios PDFs para combinar" else "Sube PDFs desde tu dispositivo",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onPickPdf) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (combineMode) "Elegir PDFs" else "Elegir PDF")
            }
        }
    }
}

// ── Tab bar de 3 modos ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaptureModeTabs(
    current: CaptureMode,
    onChange: (CaptureMode) -> Unit
) {
    val modes = listOf(
        CaptureMode.CAMERA to "Cámara",
        CaptureMode.UPLOAD to "Galería",
        CaptureMode.PDF to "PDF"
    )
    TabRow(
        selectedTabIndex = modes.indexOfFirst { it.first == current },
        modifier = Modifier.padding(horizontal = 24.dp)
    ) {
        modes.forEach { (mode, label) ->
            Tab(
                selected = current == mode,
                onClick = { onChange(mode) },
                text = { Text(label) }
            )
        }
    }
}

// ── Destination section (movido del screen anterior) ───────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DestinationSection(
    selectedDestination: String,
    courses: List<CourseOption>,
    onDestinationChange: (String, String?) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = "DESTINO",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(12.dp))
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedDestination,
                onValueChange = { },
                readOnly = true,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(
                        type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                        enabled = true
                    ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "IA",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Autoclasificar con IA") },
                    onClick = { onDestinationChange("Autoclasificar con IA", null); expanded = false }
                )
                courses.forEach { course ->
                    DropdownMenuItem(
                        text = { Text(course.name) },
                        onClick = { onDestinationChange(course.name, course.id); expanded = false }
                    )
                }
            }
        }
    }
}

// ── T-13 evolution: banner "Agregando a nota existente" ────────────────────

/**
 * Banner que se muestra en la parte superior de CaptureScreen cuando
 * el usuario llegó aquí desde NoteDetail con `preselectedNoteId`. Le
 * indica que los archivos que suba se agregarán a la nota existente
 * (no crearán una nueva).
 */
@Composable
private fun AddToNoteBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Agregando a nota existente",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Los archivos que subas se acumularán en esta nota. El título y resumen no se modificarán.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}



// ── T-13 combine: toggle + banner ────────────────────────────────────────

@Composable
private fun CombineToggleRow(
    isActive: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Combinar en una sola nota",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (isActive) "Los archivos se agruparán en 1 nota."
                       else "Cada archivo genera una nota separada.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = isActive,
            onCheckedChange = { onToggle() },
        )
    }
}



// ── Usage banner ────────────────────────────────────────────────────────────

@Composable
private fun UsageBanner(
    capturesUsed: Int,
    capturesLimit: Int,
    isPro: Boolean,
    onUpgradeClick: (String?) -> Unit
) {
    // No mostrar banner para usuarios Pro
    if (isPro) return

    val remaining = capturesLimit - capturesUsed
    val showBanner = remaining <= 2 || remaining <= 0
    if (!showBanner) return

    val atLimit = remaining <= 0
    val containerColor = if (atLimit) MaterialTheme.colorScheme.errorContainer
                         else MaterialTheme.colorScheme.tertiaryContainer
    val contentColor = if (atLimit) MaterialTheme.colorScheme.onErrorContainer
                       else MaterialTheme.colorScheme.onTertiaryContainer

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (atLimit) "Límite de capturas alcanzado"
                    else "$remaining de $capturesLimit capturas este mes",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor
                )
                if (atLimit) {
                    Text("Hazte Pro para seguir subiendo.", style = MaterialTheme.typography.bodySmall, color = contentColor)
                }
            }
            if (atLimit) {
                Button(
                    onClick = { onUpgradeClick(null) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("Ser Pro") }
            }
        }
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────

private fun uriToFile(context: android.content.Context, uri: android.net.Uri, extensionSuffix: String): java.io.File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = java.io.File(context.cacheDir, "khipu_ingest_${System.currentTimeMillis()}$extensionSuffix")
        tempFile.outputStream().use { inputStream.copyTo(it) }
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


package pe.khipuai.app.ui.screens.capture

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import pe.khipuai.app.core.camera.CameraPermissionState
import pe.khipuai.app.core.camera.rememberCameraPermissionState
import pe.khipuai.app.core.camera.rememberCameraPermissionRequest
import pe.khipuai.app.ui.components.BottomNavigationBar
import pe.khipuai.app.ui.components.camera.CameraPreview
import pe.khipuai.app.ui.components.camera.CameraState
import pe.khipuai.app.ui.components.camera.rememberCameraCaptureController
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
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val files = uris.mapNotNull { uriToFile(context, it, ".jpg") }
            if (files.isNotEmpty()) {
                if (uiState.combineMode) {
                    files.forEach { viewModel.addFileToCombineBuffer(it) }
                } else {
                    var lastId: String? = null
                    files.forEachIndexed { index, file ->
                        viewModel.processAndUploadImage(file) { id ->
                            if (id != null) {
                                lastId = id
                                if (index == files.lastIndex) {
                                    onNavigateToProcessing(id)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val files = uris.mapNotNull { uriToFile(context, it, ".pdf") }
            if (files.isNotEmpty()) {
                if (uiState.combineMode) {
                    files.forEach { viewModel.addFileToCombineBuffer(it) }
                } else {
                    var lastId: String? = null
                    files.forEachIndexed { index, file ->
                        viewModel.processAndUploadDocument(file, "application/pdf") { id ->
                            if (id != null) {
                                lastId = id
                                if (index == files.lastIndex) {
                                    onNavigateToProcessing(id)
                                }
                            }
                        }
                    }
                }
            }
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
                    onProcessFile = { file ->
                        if (uiState.combineMode) {
                            viewModel.addFileToCombineBuffer(file)
                        } else {
                            viewModel.processAndUploadImage(file) { id ->
                                if (id != null) onNavigateToProcessing(id)
                            }
                        }
                    }
                )
                CaptureMode.UPLOAD -> UploadModeBody(
                    combineMode = uiState.combineMode,
                    onPickImage = {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
                CaptureMode.PDF -> PdfModeBody(
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

            // T-13 combine: indicador de archivos pendientes (solo visible
            // cuando hay al menos 2 archivos en el buffer).
            if (uiState.combineMode && uiState.pendingFileCount > 0) {
                CombinePendingBanner(
                    count = uiState.pendingFileCount,
                    onCombine = viewModel::combineAndUpload,
                )
            }
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
    onProcessFile: (File) -> Unit
) {
    val context = LocalContext.current
    val cameraPermissionState by rememberCameraPermissionState()
    val requestPermission = rememberCameraPermissionRequest()
    val cameraController = rememberCameraCaptureController()
    val cameraState by cameraController.cameraState

    // Flash: 0=OFF, 1=ON, 2=AUTO. Default OFF.
    var flashMode by remember { mutableIntStateOf(androidx.camera.core.ImageCapture.FLASH_MODE_OFF) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
    ) {
        when (val perm = cameraPermissionState) {
            CameraPermissionState.Granted -> {
                when (val state = cameraState) {
                    is CameraState.Ready -> {
                        CameraPreview(
                            controller = cameraController,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Overlay: rectángulo guía + controles
                        CameraGuideOverlay()
                        CameraControls(
                            hasFlash = state.hasFlash,
                            flashMode = flashMode,
                            onFlashChange = {
                                flashMode = it
                                cameraController.setFlashMode(it)
                            },
                            onSwitchLens = { cameraController.switchLens() },
                            onCapture = {
                                val outputFile = File(
                                    context.cacheDir,
                                    "khipu_camera_${System.currentTimeMillis()}.jpg"
                                )
                                cameraController.captureToFile(
                                    outputFile = outputFile,
                                    onSuccess = { file -> onProcessFile(file) },
                                    onError = { e ->
                                        Log.e("CameraModeBody", "Capture failed", e)
                                    }
                                )
                            }
                        )
                    }
                    is CameraState.Initializing -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                    is CameraState.Error -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = state.message,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            CameraPermissionState.Denied -> {
                CameraPermissionRationale(
                    onAllow = { requestPermission() }
                )
            }
            CameraPermissionState.PermanentlyDenied -> {
                CameraPermissionPermanentlyDenied(
                    onOpenSettings = { openAppSettings(context) }
                )
            }
            CameraPermissionState.NotRequested -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Toca el botón para activar la cámara",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraGuideOverlay() {
    // Rectángulo guía: indica al usuario dónde poner el apunte
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp)
                )
        )
    }
}

@Composable
private fun CameraControls(
    hasFlash: Boolean,
    flashMode: Int,
    onFlashChange: (Int) -> Unit,
    onSwitchLens: () -> Unit,
    onCapture: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top-right: flash + switch lens
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (hasFlash) {
                IconButton(
                    onClick = {
                        // Ciclar OFF → ON → AUTO → OFF
                        val next = when (flashMode) {
                            androidx.camera.core.ImageCapture.FLASH_MODE_OFF ->
                                androidx.camera.core.ImageCapture.FLASH_MODE_ON
                            androidx.camera.core.ImageCapture.FLASH_MODE_ON ->
                                androidx.camera.core.ImageCapture.FLASH_MODE_AUTO
                            else -> androidx.camera.core.ImageCapture.FLASH_MODE_OFF
                        }
                        onFlashChange(next)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = when (flashMode) {
                            androidx.camera.core.ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                            androidx.camera.core.ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                            else -> Icons.Default.FlashOff
                        },
                        contentDescription = "Flash",
                        tint = Color.White
                    )
                }
            }
            IconButton(
                onClick = onSwitchLens,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Cambiar cámara",
                    tint = Color.White
                )
            }
        }

        // Bottom-center: botón de captura grande
        IconButton(
            onClick = onCapture,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .size(72.dp)
                .background(Color.White, CircleShape)
                .border(4.dp, Color.White.copy(alpha = 0.4f), CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

@Composable
private fun CameraPermissionRationale(onAllow: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.White
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Khipu necesita acceso a la cámara para capturar tus apuntes.",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAllow) {
                Text("Permitir cámara")
            }
        }
    }
}

@Composable
private fun CameraPermissionPermanentlyDenied(onOpenSettings: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Block,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.White
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Has denegado el permiso de cámara. Khipu no puede capturar apuntes sin él.",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onOpenSettings) {
                Text("Abrir configuración")
            }
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = android.content.Intent(
        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        android.net.Uri.fromParts("package", context.packageName, null)
    )
    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

// ── Cuerpos de los otros modos (reutilizan la UI previa) ────────────────────

@Composable
private fun UploadModeBody(combineMode: Boolean, onPickImage: () -> Unit) {
    Box(
        modifier = Modifier
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
private fun PdfModeBody(combineMode: Boolean, onPickPdf: () -> Unit) {
    Box(
        modifier = Modifier
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

@Composable
private fun CombinePendingBanner(
    count: Int,
    onCombine: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Layers,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$count archivo(s) listo(s) para combinar",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "Se procesarán como una sola nota.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            FilledTonalButton(
                onClick = onCombine,
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Combinar", fontWeight = FontWeight.Bold)
            }
        }
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


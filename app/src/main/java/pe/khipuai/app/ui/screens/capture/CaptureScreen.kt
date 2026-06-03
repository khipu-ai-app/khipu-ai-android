package pe.khipuai.app.ui.screens.capture

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pe.khipuai.app.ui.components.BottomNavigationBar
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    onNavigateToTab: (Int) -> Unit,
    onNavigateToProcessing: (String) -> Unit = {},
    onNavigateToSubscription: () -> Unit = {},
    viewModel: CaptureViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 📸 CONTRACT 1: Selector Nativo de Imágenes (Photo Picker)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { safeUri ->
            // Convertimos el Uri de la galería en un archivo físico en la caché
            val file = uriToFile(context, safeUri, ".jpg")
            if (file != null) {
                viewModel.processAndUploadImage(file) { id ->
                    if (id != null) onNavigateToProcessing(id)
                }
            }
        }
    }

    // 📄 CONTRACT 2: Selector Nativo de Documentos (PDF)
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { safeUri ->
            // Convertimos el Uri del explorador en un archivo físico en la caché
            val file = uriToFile(context, safeUri, ".pdf")
            if (file != null) {
                viewModel.processAndUploadDocument(file, "application/pdf") { id ->
                    if (id != null) onNavigateToProcessing(id)
                }
            }
        }
    }

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
                    IconButton(onClick = { /* TODO: Open drawer */ }) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Perfil",
                            tint = MaterialTheme.colorScheme.onSurface
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
                selectedTab = 1,
                onTabSelected = onNavigateToTab
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // FREEMIUM-07: Banner "X capturas restantes"
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Plan Gratuito", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Te quedan 5 capturas este mes.", style = MaterialTheme.typography.bodyMedium)
                    }
                    Button(
                        onClick = onNavigateToSubscription,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Ser Pro")
                    }
                }
            }

            // Destination section
            DestinationSection(
                selectedDestination = uiState.selectedDestination,
                onDestinationChange = viewModel::updateDestination
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Capture options vinculadas a los contratos de Android
            CaptureOptions(
                onCameraClick = {
                    // Lanza el photo picker restringido solo a imágenes
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onUploadClick = {
                    // Lanza el selector de archivos pidiendo explícitamente PDFs
                    pdfPickerLauncher.launch(arrayOf("application/pdf"))
                },
                onPdfModeClick = {
                    // Modo PDF secundario mapeado también al contract de documentos
                    pdfPickerLauncher.launch(arrayOf("application/pdf"))
                }
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DestinationSection(
    selectedDestination: String,
    onDestinationChange: (String) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.fillMaxWidth()
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
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
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
                listOf(
                    "Autoclasificar con IA",
                    "Matemáticas",
                    "Historia",
                    "Psicología",
                    "Física",
                    "Química"
                ).forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onDestinationChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CaptureOptions(
    onCameraClick: () -> Unit,
    onUploadClick: () -> Unit,
    onPdfModeClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CaptureOptionButton(
            icon = Icons.Default.Upload,
            label = "Subir Archivo",
            onClick = onUploadClick,
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onCameraClick,
                modifier = Modifier.size(80.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Tomar foto",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        CaptureOptionButton(
            icon = Icons.Default.PictureAsPdf,
            label = "Modo PDF",
            onClick = onPdfModeClick,
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CaptureOptionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    backgroundColor: Color,
    iconColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onClick,
                modifier = Modifier.size(60.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Abre un InputStream desde el ContentResolver de Android, clona los bytes en un archivo
 * temporal dentro del directorio de caché de la app y devuelve el puntero File.
 */
private fun uriToFile(context: Context, uri: Uri, extensionSuffix: String): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        // Creamos un archivo temporal único dentro del almacenamiento de caché privado de la app
        val tempFile = File(
            context.cacheDir,
            "khipu_ingest_${System.currentTimeMillis()}$extensionSuffix"
        )
        tempFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
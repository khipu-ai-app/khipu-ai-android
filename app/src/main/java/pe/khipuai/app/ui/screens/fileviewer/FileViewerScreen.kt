package pe.khipuai.app.ui.screens.fileviewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerScreen(
    uploadId: String,
    onNavigateBack: () -> Unit,
    viewModel: FileViewerViewModel = hiltViewModel()
) {
    LaunchedEffect(uploadId) {
        viewModel.setUploadId(uploadId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current
    var pdfFileUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // T-12: cuando el usuario intenta abrir un PDF y no hay app,
    // mostramos Snackbar con acción "Instalar" que abre Play Store.
    var showNoPdfAppSnackbar by remember { mutableStateOf(false) }

    LaunchedEffect(showNoPdfAppSnackbar) {
        if (showNoPdfAppSnackbar) {
            val result = snackbarHostState.showSnackbar(
                message = "No tienes una app para abrir PDF",
                actionLabel = "Instalar",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                openPlayStoreForPdfViewer(context)
            }
            showNoPdfAppSnackbar = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Visor de Archivos RAW", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                },
                // T-12: botón Compartir en el TopBar. Solo visible si
                // tenemos un fileUrl (o sea, el upload terminó OK).
                actions = {
                    if (!uiState.fileUrl.isNullOrEmpty()) {
                        IconButton(
                            onClick = {
                                if (pdfFileUri == null) {
                                    // Descargar el archivo y abrir el share sheet
                                    viewModel.downloadAndGetUri(
                                        context = context,
                                        onReady = { uri ->
                                            pdfFileUri = uri
                                            shareFile(context, uri, uiState.filename)
                                        },
                                        onError = { error ->
                                            android.widget.Toast.makeText(
                                                context,
                                                "Error al preparar para compartir: $error",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    )
                                } else {
                                    shareFile(context, pdfFileUri!!, uiState.filename)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartir"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Contenido RAW
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage ?: "Error desconocido",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                val isImage = when (uiState.fileType?.lowercase()) {
                    "png", "jpg", "jpeg", "webp" -> true
                    else -> uiState.fileUrl?.let { url ->
                        val cleanUrl = url.substringBefore("?")
                        cleanUrl.endsWith(".png") || cleanUrl.endsWith(".jpg") ||
                            cleanUrl.endsWith(".jpeg") || cleanUrl.endsWith(".webp")
                    } ?: false
                }

                if (isImage) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = uiState.fileUrl,
                            contentDescription = uiState.filename ?: "Imagen original",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    }
                } else {
                    var isDownloading by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = uiState.filename ?: "Documento PDF",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Este archivo se descargará en segundo plano y se abrirá automáticamente en tu lector de PDF preferido.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    isDownloading = true
                                    viewModel.downloadAndGetUri(
                                        context = context,
                                        onReady = { uri ->
                                            isDownloading = false
                                            pdfFileUri = uri
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, "application/pdf")
                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            try {
                                                context.startActivity(intent)
                                            } catch (e: android.content.ActivityNotFoundException) {
                                                // T-12: no hay app de PDF → Snackbar
                                                // con acción "Instalar" que abre Play Store.
                                                showNoPdfAppSnackbar = true
                                            } catch (e: Exception) {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "No se pudo abrir el PDF: ${e.message}",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        },
                                        onError = { error ->
                                            isDownloading = false
                                            android.widget.Toast.makeText(
                                                context,
                                                "Error al abrir PDF: $error",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    )
                                },
                                enabled = !isDownloading,
                                shape = RoundedCornerShape(99.dp)
                            ) {
                                if (isDownloading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isDownloading) "Abriendo..." else "Abrir en Visualizador de PDF")
                            }
                        }
                    }
                }
            }

            // Banner Flotante de IA Analizando
            AnimatedVisibility(
                visible = uiState.isPipelineActive,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Khipu AI está analizando este documento...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

/**
 * T-12: abre el Share Sheet de Android con el archivo como adjunto.
 * Usa `Intent.ACTION_SEND` con el URI devuelto por el FileProvider
 * (vía `downloadAndGetUri`).
 */
private fun shareFile(
    context: android.content.Context,
    uri: android.net.Uri,
    filename: String?
) {
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(android.content.Intent.EXTRA_STREAM, uri)
        putExtra(android.content.Intent.EXTRA_SUBJECT, filename ?: "Documento de Khipu")
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val chooser = android.content.Intent.createChooser(intent, "Compartir PDF")
    chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(chooser)
    } catch (e: Exception) {
        android.widget.Toast.makeText(
            context,
            "No hay app para compartir: ${e.message}",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }
}

/**
 * T-12: abre Play Store buscando "PDF viewer". Si no hay Play Store
 * (ej. device sin GMS), abre el browser como fallback.
 */
private fun openPlayStoreForPdfViewer(context: android.content.Context) {
    val playStoreIntent = android.content.Intent(
        android.content.Intent.ACTION_VIEW,
        android.net.Uri.parse("market://search?q=PDF%20viewer")
    ).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
    try {
        context.startActivity(playStoreIntent)
    } catch (_: Exception) {
        val browserIntent = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse("https://play.google.com/store/search?q=PDF%20viewer")
        ).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
        try {
            context.startActivity(browserIntent)
        } catch (_: Exception) {
            android.widget.Toast.makeText(
                context,
                "No se pudo abrir Play Store. Busca 'PDF viewer' manualmente.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
}

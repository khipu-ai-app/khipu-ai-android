package pe.khipuai.app.ui.screens.fileviewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.ui.platform.LocalUriHandler
import coil.compose.AsyncImage
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerScreen(
    uploadId: String,
    onNavigateBack: () -> Unit,
    viewModel: FileViewerViewModel = hiltViewModel()
) {
    androidx.compose.runtime.LaunchedEffect(uploadId) {
        viewModel.setUploadId(uploadId)
    }

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visor de Archivos RAW", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
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
            // Contenido RAW simulado (Lienzo)
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
                    val uriHandler = LocalUriHandler.current
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
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Este archivo es un documento PDF y se abrirá a través del visor externo del sistema.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { uiState.fileUrl?.let { uriHandler.openUri(it) } },
                                shape = RoundedCornerShape(99.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Abrir PDF en el Navegador")
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

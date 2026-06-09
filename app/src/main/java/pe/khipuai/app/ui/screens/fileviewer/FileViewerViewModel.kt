package pe.khipuai.app.ui.screens.fileviewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import pe.khipuai.app.BuildConfig
import pe.khipuai.app.core.datastore.SessionDataStore
import pe.khipuai.app.data.repository.UploadRepository
import javax.inject.Inject

data class FileViewerUiState(
    val fileUrl: String? = null,
    val fileType: String? = null,
    val filename: String? = null,
    val isPipelineActive: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class FileViewerViewModel @Inject constructor(
    private val uploadRepository: UploadRepository,
    private val sessionDataStore: SessionDataStore,
    private val okHttpClient: OkHttpClient,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private var uploadId: String = ""

    private val _uiState = MutableStateFlow(FileViewerUiState())
    val uiState: StateFlow<FileViewerUiState> = _uiState.asStateFlow()

    private fun loadFileDetails() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // Obtener el estado del upload para saber el nombre y tipo del archivo
                val status = uploadRepository.checkProcessingStatus(uploadId).getOrThrow()
                val token = sessionDataStore.tokenFlow.first() ?: ""
                val fileUrl = "${BuildConfig.BASE_URL}v1/uploads/$uploadId/file?token=$token"
                _uiState.value = _uiState.value.copy(
                    fileUrl = fileUrl,
                    fileType = status.fileType,
                    filename = status.filename,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error al cargar el archivo: ${e.message}"
                )
            }
        }
    }

    fun downloadAndGetUri(
        context: android.content.Context,
        onReady: (android.net.Uri) -> Unit,
        onError: (String) -> Unit
    ) {
        val url = _uiState.value.fileUrl ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("Error del servidor: ${response.code}")
                }

                val body = response.body ?: throw IOException("Cuerpo de respuesta vacío")

                // Crear un archivo temporal en el cache
                val filename = _uiState.value.filename ?: "documento.pdf"
                val tempFile = File(context.cacheDir, filename)

                body.byteStream().use { inputStream ->
                    tempFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                // Obtener la URI usando FileProvider
                val authority = "${context.packageName}.fileprovider"
                val contentUri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    authority,
                    tempFile
                )

                withContext(Dispatchers.Main) {
                    onReady(contentUri)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Error desconocido")
                }
            }
        }
    }

    private fun startPipelinePolling() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPipelineActive = true)
            var attempts = 0
            while (attempts < 10) {
                delay(2000)
                try {
                    val status = uploadRepository.checkProcessingStatus(uploadId).getOrNull()
                    if (status != null && status.status == "completed") {
                        _uiState.value = _uiState.value.copy(isPipelineActive = false)
                        break
                    }
                } catch (e: Exception) {
                    // ignorar
                }
                attempts++
            }
            // Fallback si no termina
            _uiState.value = _uiState.value.copy(isPipelineActive = false)
        }
    }

    fun setUploadId(id: String) {
        if (uploadId.isEmpty() && id.isNotEmpty()) {
            uploadId = id
            loadFileDetails()
            startPipelinePolling()
        }
    }
}

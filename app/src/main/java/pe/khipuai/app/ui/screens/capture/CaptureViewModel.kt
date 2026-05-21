package pe.khipuai.app.ui.screens.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.khipuai.app.data.repository.UploadRepository
import java.io.File
import javax.inject.Inject

data class CaptureUiState(
    val selectedDestination: String = "Autoclasificar con IA",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val captureMode: CaptureMode = CaptureMode.CAMERA,
    val uploadedId: String? = null
)

enum class CaptureMode {
    CAMERA, UPLOAD, PDF
}

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val uploadRepository: UploadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    fun updateDestination(destination: String) {
        _uiState.value = _uiState.value.copy(selectedDestination = destination)
    }

    // Procesa el envío de archivos de imagen capturados por la cámara del celular
    fun processAndUploadImage(file: File, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                captureMode = CaptureMode.CAMERA
            )

            // Invocamos la subida Multipart asíncrona hacia Docker
            val result = uploadRepository.uploadFile(file, mimeType = "image/jpeg")

            result.onSuccess { response ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    uploadedId = response.id
                )
                onResult(response.id)
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Fallo de ingesta de imagen: ${exception.localizedMessage ?: "Servidor caído"}"
                )
                onResult(null)
            }
        }
    }

    // Procesa la carga de archivos locales (PDFs o Imágenes de la Galería)
    fun processAndUploadDocument(file: File, mimeType: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                captureMode = CaptureMode.UPLOAD
            )

            val result = uploadRepository.uploadFile(file, mimeType)

            result.onSuccess { response ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    uploadedId = response.id
                )
                onResult(response.id)
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error al subir documento: ${exception.localizedMessage}"
                )
                onResult(null)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
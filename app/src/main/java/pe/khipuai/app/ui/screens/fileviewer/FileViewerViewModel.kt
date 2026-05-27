package pe.khipuai.app.ui.screens.fileviewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.khipuai.app.data.repository.UploadRepository
import javax.inject.Inject

data class FileViewerUiState(
    val fileUrl: String? = null,
    val isPipelineActive: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class FileViewerViewModel @Inject constructor(
    private val uploadRepository: UploadRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Extraemos el ID del archivo o nota subida desde la navegación
    private val uploadId: String = checkNotNull(savedStateHandle["uploadId"])

    private val _uiState = MutableStateFlow(FileViewerUiState())
    val uiState: StateFlow<FileViewerUiState> = _uiState.asStateFlow()

    init {
        loadFileDetails()
        startPipelinePolling()
    }

    private fun loadFileDetails() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // Simulamos obtener la URL raw del archivo según el ID subido
            try {
                // val result = uploadRepository.getUploadStatus(uploadId).getOrThrow()
                // _uiState.value = _uiState.value.copy(fileUrl = result.fileUrl, isLoading = false)
                
                // MOCK hasta que haya endpoint raw
                delay(800)
                _uiState.value = _uiState.value.copy(
                    fileUrl = "https://example.com/raw_document_$uploadId.pdf",
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
}

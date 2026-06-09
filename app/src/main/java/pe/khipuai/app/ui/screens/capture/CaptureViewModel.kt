package pe.khipuai.app.ui.screens.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.khipuai.app.data.repository.CourseRepository
import pe.khipuai.app.data.repository.UploadRepository
import java.io.File
import javax.inject.Inject

data class CourseOption(
    val id: String,
    val name: String
)

data class CaptureUiState(
    val selectedDestination: String = "Autoclasificar con IA",
    val selectedDestinationId: String? = null,
    val courses: List<CourseOption> = emptyList(),
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
    private val uploadRepository: UploadRepository,
    private val courseRepository: CourseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            courseRepository.observeAll().collect { localCourses ->
                val activeCourses = localCourses.filter { it.isActive }.map {
                    CourseOption(id = it.id, name = it.name)
                }.sortedBy { it.name }
                _uiState.value = _uiState.value.copy(courses = activeCourses)
            }
        }
        viewModelScope.launch {
            courseRepository.fetchMyCourses()
        }
    }

    fun updateDestination(destinationName: String, destinationId: String?) {
        _uiState.value = _uiState.value.copy(
            selectedDestination = destinationName,
            selectedDestinationId = destinationId
        )
    }

    // Procesa el envío de archivos de imagen capturados por la cámara del celular
    fun processAndUploadImage(file: File, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                captureMode = CaptureMode.CAMERA
            )

            val currentCourseId = _uiState.value.selectedDestinationId
            val result = uploadRepository.uploadFile(file, mimeType = "image/jpeg", courseId = currentCourseId)

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

            val currentCourseId = _uiState.value.selectedDestinationId
            val result = uploadRepository.uploadFile(file, mimeType, courseId = currentCourseId)

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
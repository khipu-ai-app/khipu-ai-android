package pe.khipuai.app.ui.screens.courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.khipuai.app.data.repository.CourseRepository
import javax.inject.Inject

// Paleta de colores Hex oficiales que tu frontend bento sabe pintar
data class ColorPaletteItem(
    val hexCode: String,
    val colorDisplay: androidx.compose.ui.graphics.Color
)

data class CreateCourseUiState(
    val courseName: String = "",
    val selectedColorHex: String = "#7B41B3", // Color morado por defecto (surface-tint)
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val createdSuccessfully: Boolean = false
)

@HiltViewModel
class CreateCourseViewModel @Inject constructor(
    private val courseRepository: CourseRepository
) : ViewModel() {


    private val _uiState = MutableStateFlow(CreateCourseUiState())
    val uiState: StateFlow<CreateCourseUiState> = _uiState.asStateFlow()

    val availableColors = listOf(
        ColorPaletteItem("#7B41B3", androidx.compose.ui.graphics.Color(0xFF7B41B3)), // Morado Khipu
        ColorPaletteItem("#4C56AF", androidx.compose.ui.graphics.Color(0xFF4C56AF)), // Azul Secundario
        ColorPaletteItem("#88D982", androidx.compose.ui.graphics.Color(0xFF88D982)), // Verde Certiario
        ColorPaletteItem("#BA1A1A", androidx.compose.ui.graphics.Color(0xFFBA1A1A)), // Rojo Alerta
        ColorPaletteItem("#2E0052", androidx.compose.ui.graphics.Color(0xFF2E0052))  // Oscuro profundo
    )

    fun onNameChanged(newName: String) {
        _uiState.value = _uiState.value.copy(courseName = newName, errorMessage = null)
    }

    fun onColorSelected(hexCode: String) {
        _uiState.value = _uiState.value.copy(selectedColorHex = hexCode)
    }

    fun submitCourse() {
        if (_uiState.value.courseName.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "El nombre del curso es obligatorio.")
            return
        }
        _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
        viewModelScope.launch {
            courseRepository.createCourse(
                name = _uiState.value.courseName,
                color = _uiState.value.selectedColorHex
            ).onSuccess {
                _uiState.value = _uiState.value.copy(isSubmitting = false, createdSuccessfully = true)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = pe.khipuai.app.core.network.NetworkErrorMapper.from(error).message
                )
            }
        }
    }

    fun resetSuccess() {
        _uiState.value = _uiState.value.copy(createdSuccessfully = false)
    }
}

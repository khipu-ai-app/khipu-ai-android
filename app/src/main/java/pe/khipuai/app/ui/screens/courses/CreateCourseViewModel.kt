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

// T-16: Paleta ampliada a 10 colores para mejor personalización
data class ColorPaletteItem(
    val hexCode: String,
    val colorDisplay: androidx.compose.ui.graphics.Color
)

data class CreateCourseUiState(
    val courseName: String = "",
    val courseDescription: String = "",
    val selectedColorHex: String = "#7B41B3",
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

    // T-16: Paleta ampliada con 10 colores curados
    val availableColors = listOf(
        ColorPaletteItem("#7B41B3", androidx.compose.ui.graphics.Color(0xFF7B41B3)), // Morado Khipu
        ColorPaletteItem("#4C56AF", androidx.compose.ui.graphics.Color(0xFF4C56AF)), // Azul Índigo
        ColorPaletteItem("#0288D1", androidx.compose.ui.graphics.Color(0xFF0288D1)), // Azul Cielo
        ColorPaletteItem("#00897B", androidx.compose.ui.graphics.Color(0xFF00897B)), // Verde Teal
        ColorPaletteItem("#88D982", androidx.compose.ui.graphics.Color(0xFF88D982)), // Verde Claro
        ColorPaletteItem("#F9A825", androidx.compose.ui.graphics.Color(0xFFF9A825)), // Ámbar
        ColorPaletteItem("#EF6C00", androidx.compose.ui.graphics.Color(0xFFEF6C00)), // Naranja
        ColorPaletteItem("#BA1A1A", androidx.compose.ui.graphics.Color(0xFFBA1A1A)), // Rojo Alerta
        ColorPaletteItem("#C2185B", androidx.compose.ui.graphics.Color(0xFFC2185B)), // Rosa Oscuro
        ColorPaletteItem("#37474F", androidx.compose.ui.graphics.Color(0xFF37474F)), // Gris Pizarra
    )

    fun onNameChanged(newName: String) {
        _uiState.value = _uiState.value.copy(courseName = newName, errorMessage = null)
    }

    fun onDescriptionChanged(newDesc: String) {
        _uiState.value = _uiState.value.copy(courseDescription = newDesc)
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
                description = _uiState.value.courseDescription.takeIf { it.isNotBlank() },
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

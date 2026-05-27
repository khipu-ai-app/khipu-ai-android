package pe.khipuai.app.ui.screens.courses

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Enumeración estricta para el control de pestañas organizacionales
enum class CourseFilter { TODOS, ACTIVOS, COMPLETADOS, ARCHIVADOS }

// Molde de UI para inyectar datos reales en las tarjetas Bento
data class CourseUiModel(
    val id: String,
    val name: String,
    val description: String,
    val categoryTag: String,
    val semesterTag: String,
    val priorityTag: String? = null,
    val progressPercentage: Int,
    val masteredCount: Int,
    val pendingCount: Int,
    val iconName: String
)

data class CoursesUiState(
    val selectedFilter: CourseFilter = CourseFilter.ACTIVOS,
    val courses: List<CourseUiModel> = emptyList(),
    val isLoading: Boolean = false
)

class CoursesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CoursesUiState(isLoading = true))
    val uiState: StateFlow<CoursesUiState> = _uiState.asStateFlow()

    init {
        loadMockCourses()
    }

    private fun loadMockCourses() {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            courses = listOf(
                CourseUiModel(
                    id = "1",
                    name = "Historia Universal Contemporánea",
                    description = "Análisis de los movimientos sociopolíticos del siglo XX y su impacto en la geopolítica actual.",
                    categoryTag = "Historia",
                    semesterTag = "Semestre 4",
                    priorityTag = "Intensivo",
                    progressPercentage = 68,
                    masteredCount = 24,
                    pendingCount = 12,
                    iconName = "history_edu"
                ),
                CourseUiModel(
                    id = "2",
                    name = "Introducción a la Inteligencia Artificial",
                    description = "Fundamentos de machine learning, redes neuronales y procesamiento de lenguaje natural.",
                    categoryTag = "Tecnología",
                    semesterTag = "IA",
                    priorityTag = "Prioridad Alta",
                    progressPercentage = 32,
                    masteredCount = 15,
                    pendingCount = 32,
                    iconName = "memory"
                ),
                CourseUiModel(
                    id = "3",
                    name = "Psicología Cognitiva",
                    description = "Estudio de los procesos mentales: percepción, memoria, pensamiento y resolución de problemas.",
                    categoryTag = "Ciencias Sociales",
                    semesterTag = "Investigación",
                    priorityTag = null,
                    progressPercentage = 85,
                    masteredCount = 42,
                    pendingCount = 8,
                    iconName = "psychology"
                )
            )
        )
    }

    fun changeFilter(filter: CourseFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }
}
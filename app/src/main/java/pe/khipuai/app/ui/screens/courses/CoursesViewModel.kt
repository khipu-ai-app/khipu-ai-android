package pe.khipuai.app.ui.screens.courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import pe.khipuai.app.data.repository.CourseRepository
import javax.inject.Inject

enum class CourseFilter { TODOS, ACTIVOS, COMPLETADOS, ARCHIVADOS }

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
    val iconName: String,
    val color: String,
)

data class CoursesUiState(
    val selectedFilter: CourseFilter = CourseFilter.ACTIVOS,
    val courses: List<CourseUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class CoursesViewModel @Inject constructor(
    private val courseRepository: CourseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoursesUiState(isLoading = true))
    val uiState: StateFlow<CoursesUiState> = _uiState.asStateFlow()

    init {
        // 1. Observamos Room como fuente de verdad reactiva (offline-first)
        courseRepository.observeAll()
            .onEach { entities ->
                val mapped = entities.map { entity ->
                    CourseUiModel(
                        id = entity.id,
                        name = entity.name,
                        description = "",       // El backend no devuelve descripción en la lista
                        categoryTag = "Curso",
                        semesterTag = "",
                        priorityTag = null,
                        progressPercentage = 0, // Sin endpoint de progreso aún
                        masteredCount = 0,
                        pendingCount = 0,
                        iconName = "menu_book",
                        color = entity.color,
                    )
                }
                _uiState.value = _uiState.value.copy(courses = mapped, isLoading = false)
            }
            .catch { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error al cargar cursos: ${e.localizedMessage}"
                )
            }
            .launchIn(viewModelScope)

        // 2. Sincronizamos con la API para actualizar Room (sin bloquear la UI)
        syncWithNetwork()
    }

    fun syncWithNetwork() {
        viewModelScope.launch {
            courseRepository.fetchMyCourses()
                .onFailure { e ->
                    // Solo mostramos error si Room también está vacío
                    if (_uiState.value.courses.isEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Sin conexión. Verifica tu red."
                        )
                    }
                }
        }
    }

    fun archiveCourse(courseId: String) {
        viewModelScope.launch {
            courseRepository.archiveCourse(courseId)
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "No se pudo archivar el curso: ${e.localizedMessage}"
                    )
                }
        }
    }

    fun changeFilter(filter: CourseFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

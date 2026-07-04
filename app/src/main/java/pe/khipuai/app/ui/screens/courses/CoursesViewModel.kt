package pe.khipuai.app.ui.screens.courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
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
    val isActive: Boolean,
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

    private val _selectedFilter = MutableStateFlow(CourseFilter.ACTIVOS)
    private val _uiState = MutableStateFlow(CoursesUiState(isLoading = true))
    val uiState: StateFlow<CoursesUiState> = _uiState.asStateFlow()

    init {
        // Observamos Room como fuente de verdad reactiva (offline-first) combinada con el filtro seleccionado
        combine(
            courseRepository.observeAll(),
            _selectedFilter
        ) { entities, filter ->
            val mapped = entities.map { entity ->
                CourseUiModel(
                    id = entity.id,
                    name = entity.name,
                    description = "",
                    categoryTag = "",
                    semesterTag = "",
                    priorityTag = null,
                    progressPercentage = 0,
                    masteredCount = 0,
                    pendingCount = 0,
                    iconName = "menu_book",
                    color = entity.color,
                    isActive = entity.isActive
                )
            }.filter { uiModel ->
                when (filter) {
                    CourseFilter.TODOS -> true
                    CourseFilter.ACTIVOS -> uiModel.isActive && uiModel.progressPercentage < 100
                    CourseFilter.COMPLETADOS -> uiModel.isActive && uiModel.progressPercentage == 100
                    CourseFilter.ARCHIVADOS -> !uiModel.isActive
                }
            }
            _uiState.value = _uiState.value.copy(
                courses = mapped,
                selectedFilter = filter,
                isLoading = false
            )
        }.catch { e ->
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = pe.khipuai.app.core.network.NetworkErrorMapper.from(e).message
            )
        }.launchIn(viewModelScope)

        // Sincronizamos con la API para actualizar Room (sin bloquear la UI)
        syncWithNetwork()
    }

    fun syncWithNetwork() {
        viewModelScope.launch {
            courseRepository.fetchMyCourses()
                .onFailure { e ->
                    // Solo mostramos error si la lista actual está vacía
                    if (_uiState.value.courses.isEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = pe.khipuai.app.core.network.NetworkErrorMapper.from(e).message
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
                        errorMessage = pe.khipuai.app.core.network.NetworkErrorMapper.from(e).message
                    )
                }
        }
    }

    fun restoreCourse(courseId: String) {
        viewModelScope.launch {
            courseRepository.restoreCourse(courseId)
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = pe.khipuai.app.core.network.NetworkErrorMapper.from(e).message
                    )
                }
        }
    }

    fun deleteCoursePermanently(courseId: String) {
        viewModelScope.launch {
            courseRepository.deleteCoursePermanently(courseId)
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = pe.khipuai.app.core.network.NetworkErrorMapper.from(e).message
                    )
                }
        }
    }

    fun renameCourse(courseId: String, newName: String) {
        viewModelScope.launch {
            courseRepository.updateCourse(courseId, newName, null)
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = pe.khipuai.app.core.network.NetworkErrorMapper.from(e).message
                    )
                }
        }
    }

    fun changeFilter(filter: CourseFilter) {
        _selectedFilter.value = filter
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

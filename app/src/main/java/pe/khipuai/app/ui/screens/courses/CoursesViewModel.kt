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
import pe.khipuai.app.data.repository.PlannerRepository
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
    private val courseRepository: CourseRepository,
    private val plannerRepository: PlannerRepository,
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(CourseFilter.ACTIVOS)
    private val _conceptsByCourse = MutableStateFlow<Map<String, List<pe.khipuai.app.data.remote.dto.DueConceptResponse>>>(emptyMap())
    private val _uiState = MutableStateFlow(CoursesUiState(isLoading = true))
    val uiState: StateFlow<CoursesUiState> = _uiState.asStateFlow()

    init {
        // Cargar datos del planner de forma reactiva (StateFlow)
        loadPlannerData()

        // Observamos Room, filtro y planner reactivamente
        combine(
            courseRepository.observeAll(),
            _selectedFilter,
            _conceptsByCourse
        ) { entities, filter, conceptsByCourse ->
            val mapped = entities.map { entity ->
                val key = entity.name.lowercase()
                val courseConcepts = conceptsByCourse[key] ?: emptyList()
                val total = courseConcepts.size
                val mastered = courseConcepts.count { it.reviewedToday || !it.isDue }
                val pending = total - mastered

                CourseUiModel(
                    id = entity.id,
                    name = entity.name,
                    description = "",
                    categoryTag = categorize(entity.name),
                    semesterTag = "",
                    priorityTag = null,
                    progressPercentage = if (total > 0) (mastered * 100 / total).coerceIn(0, 100) else 0,
                    masteredCount = mastered.coerceAtLeast(0),
                    pendingCount = pending.coerceAtLeast(0),
                    iconName = courseIcon(entity.name),
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
        viewModelScope.launch { syncWithNetwork() }
    }

    private fun loadPlannerData() {
        viewModelScope.launch {
            plannerRepository.fetchDailyAgenda().onSuccess { agenda ->
                _conceptsByCourse.value = agenda.groupBy { it.courseName.lowercase() }
            }
        }
    }

    private suspend fun syncWithNetwork() {
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
                .onSuccess {
                    // Refrescar la caché local después de eliminar
                    courseRepository.fetchMyCourses()
                    loadPlannerData()
                }
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

    private fun categorize(name: String): String = when {
        name.contains("Historia", ignoreCase = true) -> "Historia"
        name.contains("Cálculo", ignoreCase = true) || name.contains("Álgebra", ignoreCase = true) || name.contains("Estadística", ignoreCase = true) -> "Matemáticas"
        name.contains("Física", ignoreCase = true) || name.contains("Química", ignoreCase = true) -> "Ciencias"
        name.contains("Programación", ignoreCase = true) || name.contains("Software", ignoreCase = true) || name.contains("Datos", ignoreCase = true) || name.contains("Sistemas", ignoreCase = true) || name.contains("Redes", ignoreCase = true) || name.contains("IA", ignoreCase = true) -> "Tecnología"
        else -> "General"
    }

    private fun courseIcon(name: String): String = when {
        name.contains("Historia", ignoreCase = true) -> "history"
        name.contains("Cálculo", ignoreCase = true) || name.contains("Álgebra", ignoreCase = true) -> "calculate"
        name.contains("Física", ignoreCase = true) || name.contains("Química", ignoreCase = true) -> "science"
        name.contains("Programación", ignoreCase = true) || name.contains("Software", ignoreCase = true) || name.contains("Datos", ignoreCase = true) || name.contains("Sistemas", ignoreCase = true) -> "computer"
        else -> "book"
    }
}

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
                // Deterministic mock data based on name/id hash to create a polished and realistic feel
                val hash = Math.abs(entity.id.hashCode())
                val progress = when {
                    // Force a deterministic 100% progress for every 6th course to populate the "Completed" tab
                    hash % 6 == 0 -> 100
                    else -> (hash % 60) + 20 // progress between 20% and 80%
                }
                
                val mastered = when (progress) {
                    100 -> (hash % 6) + 12
                    else -> (progress * 0.15).toInt() + 1
                }
                
                val pending = when (progress) {
                    100 -> 0
                    else -> (hash % 5) + 2
                }
                
                val desc = when {
                    entity.name.contains("Historia", ignoreCase = true) -> 
                        "Estudio exhaustivo de los procesos históricos y evolución social."
                    entity.name.contains("Cálculo", ignoreCase = true) -> 
                        "Límites, derivadas, integración y optimización de funciones matemáticas."
                    entity.name.contains("Física", ignoreCase = true) -> 
                        "Mecánica clásica, cinemática, electromagnetismo y leyes dinámicas."
                    entity.name.contains("Programación", ignoreCase = true) -> 
                        "Conceptos de algoritmos, estructuras de control y resolución de problemas."
                    entity.name.contains("Base de Datos", ignoreCase = true) -> 
                        "Diseño de esquemas, consultas SQL avanzadas y normalización relacional."
                    entity.name.contains("Redes", ignoreCase = true) -> 
                        "Protocolos TCP/IP, topologías de red y transferencia de datos."
                    entity.name.contains("Sistemas Operativos", ignoreCase = true) -> 
                        "Gestión de memoria, procesos, sistemas de archivos y concurrencia."
                    else -> "Análisis sistemático de los conceptos y aplicaciones clave de ${entity.name}."
                }
                
                val category = when {
                    entity.name.contains("Historia", ignoreCase = true) -> "Historia"
                    entity.name.contains("Cálculo", ignoreCase = true) || entity.name.contains("Álgebra", ignoreCase = true) || entity.name.contains("Estadística", ignoreCase = true) -> "Matemáticas"
                    entity.name.contains("Física", ignoreCase = true) || entity.name.contains("Química", ignoreCase = true) -> "Física"
                    entity.name.contains("Programación", ignoreCase = true) || entity.name.contains("Software", ignoreCase = true) || entity.name.contains("Web", ignoreCase = true) || entity.name.contains("Datos", ignoreCase = true) || entity.name.contains("Redes", ignoreCase = true) || entity.name.contains("Sistemas", ignoreCase = true) || entity.name.contains("IA", ignoreCase = true) -> "Sistemas"
                    else -> "Ciencias"
                }
                
                val semester = "Semestre ${ (hash % 4) + 1 }"
                
                val priority = when {
                    hash % 3 == 0 -> "Prioridad Alta"
                    hash % 4 == 0 -> "Intensivo"
                    else -> null
                }
                
                val icon = when {
                    entity.name.contains("Historia", ignoreCase = true) -> "history"
                    entity.name.contains("Cálculo", ignoreCase = true) || entity.name.contains("Álgebra", ignoreCase = true) -> "calculate"
                    entity.name.contains("Física", ignoreCase = true) || entity.name.contains("Química", ignoreCase = true) -> "science"
                    entity.name.contains("Programación", ignoreCase = true) || entity.name.contains("Software", ignoreCase = true) || entity.name.contains("Datos", ignoreCase = true) || entity.name.contains("Sistemas", ignoreCase = true) -> "computer"
                    else -> "menu_book"
                }

                CourseUiModel(
                    id = entity.id,
                    name = entity.name,
                    description = desc,
                    categoryTag = category,
                    semesterTag = semester,
                    priorityTag = priority,
                    progressPercentage = progress,
                    masteredCount = mastered,
                    pendingCount = pending,
                    iconName = icon,
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
                errorMessage = "Error al cargar cursos: ${e.localizedMessage}"
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
        _selectedFilter.value = filter
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

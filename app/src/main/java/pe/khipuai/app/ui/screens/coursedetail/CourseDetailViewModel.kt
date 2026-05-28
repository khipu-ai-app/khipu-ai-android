package pe.khipuai.app.ui.screens.coursedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import pe.khipuai.app.data.local.entity.NoteEntity
import pe.khipuai.app.data.repository.CourseRepository
import pe.khipuai.app.data.repository.OfflineFirstNoteRepository
import javax.inject.Inject

data class CompactNoteUiModel(
    val id: String,
    val title: String,
    val snippet: String,
    val dateTag: String,
    val subCategory: String
)

data class ReviewItemUiModel(
    val id: String,
    val title: String,
    val scheduleText: String,
    val isUrgent: Boolean
)

data class GraphNodeUiModel(
    val label: String,
    val iconName: String,
    val status: NodeStatus,
    val xOffsetFraction: Float,
    val yOffsetFraction: Float
)

enum class NodeStatus { DOMINADO, EN_PROGRESO, BLOQUEADO }

data class CourseDetailUiState(
    // Valores vacíos por defecto — nunca hay datos inventados
    val courseId: String = "",
    val courseName: String = "",
    val categoryName: String = "",
    val courseColor: String = "#7B1FA2",
    val courseProgress: Int = 0,
    val notes: List<CompactNoteUiModel> = emptyList(),
    val upcomingReviews: List<ReviewItemUiModel> = emptyList(),
    // El mini-mapa muestra estado vacío hasta que el grafo real responda
    val previewNodes: List<GraphNodeUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class CourseDetailViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val offlineFirstNoteRepository: OfflineFirstNoteRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Extrae courseId de la ruta de navegación: coursedetail/{courseId}
    private val courseId: String = savedStateHandle.get<String>("courseId") ?: ""

    private val _uiState = MutableStateFlow(CourseDetailUiState(isLoading = true))
    val uiState: StateFlow<CourseDetailUiState> = _uiState.asStateFlow()

    init {
        loadCourseData()
    }

    private fun loadCourseData() {
        if (courseId.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "No se proporcionó un ID de curso válido."
            )
            return
        }

        // Actualizar el ID en el estado inmediatamente
        _uiState.value = _uiState.value.copy(courseId = courseId, isLoading = true)

        viewModelScope.launch {
            // 1. Cargar datos del curso desde Room
            val course = courseRepository.getById(courseId)
            if (course != null) {
                _uiState.value = _uiState.value.copy(
                    courseName = course.name,
                    categoryName = course.color, // El backend no retorna categoría aún
                    courseColor = course.color
                )
            }

            // 2. Si no hay datos en Room, sincronizar con la API
            if (course == null) {
                courseRepository.fetchMyCourses()
                val freshCourse = courseRepository.getById(courseId)
                if (freshCourse != null) {
                    _uiState.value = _uiState.value.copy(
                        courseName = freshCourse.name,
                        courseColor = freshCourse.color
                    )
                }
            }

            // 3. Sincronizar notas desde la API hacia Room
            offlineFirstNoteRepository.syncFromNetwork()
        }

        // 4. Observar notas de Room reactivamente — se actualiza cuando la sync termina
        offlineFirstNoteRepository.observeByCourse(courseId)
            .onEach { noteEntities ->
                _uiState.value = _uiState.value.copy(
                    notes = noteEntities.take(4).map { it.toUiModel() },
                    isLoading = false
                )
            }
            .launchIn(viewModelScope)
    }

    fun completeReviewTask(taskId: String) {
        // TODO Sprint 7: Llamar al PlannerRepository con el rating real
    }

    private fun NoteEntity.toUiModel(): CompactNoteUiModel {
        val displayDate = try {
            // Formato simple: "2026-05-27T..." → "27 May"
            val parts = createdAt.take(10).split("-")
            if (parts.size == 3) "${parts[2]} ${monthName(parts[1].toInt())}" else createdAt.take(10)
        } catch (_: Exception) {
            createdAt.take(10)
        }

        return CompactNoteUiModel(
            id = id,
            title = title,
            snippet = summary.take(100).ifEmpty { "Nota capturada y procesada por Khipu AI" },
            dateTag = displayDate,
            subCategory = difficultyLevel.replaceFirstChar { it.uppercase() }
        )
    }

    private fun monthName(month: Int): String = when (month) {
        1 -> "Ene"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Abr"; 5 -> "May"; 6 -> "Jun"
        7 -> "Jul"; 8 -> "Ago"; 9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; else -> "Dic"
    }
}
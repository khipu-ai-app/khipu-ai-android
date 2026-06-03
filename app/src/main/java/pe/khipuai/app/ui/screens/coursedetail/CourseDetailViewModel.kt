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
import pe.khipuai.app.data.repository.GraphRepository
import pe.khipuai.app.data.repository.OfflineFirstNoteRepository
import pe.khipuai.app.data.repository.PlannerRepository
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
    val errorMessage: String? = null,
    val availableCourses: List<pe.khipuai.app.data.local.entity.CourseEntity> = emptyList()
)

@HiltViewModel
class CourseDetailViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val offlineFirstNoteRepository: OfflineFirstNoteRepository,
    private val plannerRepository: PlannerRepository,  // Sprint 7: reviews SM-2
    private val graphRepository: GraphRepository,       // Sprint 7: grafo Neo4j
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Extrae courseId de la ruta de navegación: coursedetail/{courseId}
    private val courseId: String = savedStateHandle.get<String>("courseId") ?: ""

    private val _uiState = MutableStateFlow(CourseDetailUiState(isLoading = true))
    val uiState: StateFlow<CourseDetailUiState> = _uiState.asStateFlow()

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

        // Cargar cursos disponibles reactivamente
        viewModelScope.launch {
            courseRepository.observeAll().collect { courses ->
                _uiState.value = _uiState.value.copy(availableCourses = courses)
            }
        }

        viewModelScope.launch {
            // 1. Cargar datos del curso desde Room
            var resolvedCourseName = ""
            val course = courseRepository.getById(courseId)
            if (course != null) {
                resolvedCourseName = course.name
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
                    resolvedCourseName = freshCourse.name
                    _uiState.value = _uiState.value.copy(
                        courseName = freshCourse.name,
                        courseColor = freshCourse.color
                    )
                }
            }

            // 3. Sincronizar notas desde la API hacia Room
            offlineFirstNoteRepository.syncFromNetwork()

            // 4. Cargar reviews y grafo en paralelo; errores son silenciosos (listas vacías)
            launch { loadUpcomingReviews(resolvedCourseName) }
            launch { loadGraphPreview() }
        }

        // 5. Observar notas de Room reactivamente — se actualiza cuando la sync termina
        offlineFirstNoteRepository.observeByCourse(courseId)
            .onEach { noteEntities ->
                _uiState.value = _uiState.value.copy(
                    notes = noteEntities.take(4).map { it.toUiModel() },
                    isLoading = false
                )
            }
            .launchIn(viewModelScope)
    }

    /**
     * Obtiene la agenda diaria SM-2 y filtra por el nombre del curso actual.
     * Falla de forma silenciosa: si el endpoint falla, [upcomingReviews] queda vacío.
     */
    private suspend fun loadUpcomingReviews(courseName: String) {
        if (courseName.isBlank()) return

        plannerRepository.fetchDailyAgenda()
            .onSuccess { concepts ->
                val reviews = concepts
                    .filter { it.courseName.equals(courseName, ignoreCase = true) }
                    .map { due ->
                        ReviewItemUiModel(
                            id = due.conceptId,
                            title = due.conceptName,
                            scheduleText = buildScheduleText(due.nextReviewDate, due.interval),
                            // Urgente si el concepto es nuevo (interval ≤ 1) o el ease factor es bajo
                            isUrgent = due.interval <= 1 || due.easeFactor < 2.0f
                        )
                    }
                _uiState.value = _uiState.value.copy(upcomingReviews = reviews)
            }
            // onFailure: lista queda vacía, la pantalla no crashea
    }

    /**
     * Obtiene el grafo del curso y toma los primeros 5 nodos de tipo "concept"
     * para renderizar en el mini-mapa. Falla de forma silenciosa.
     */
    private suspend fun loadGraphPreview() {
        // Posiciones fijas que coinciden con las líneas del Canvas dibujado en CourseDetailScreen
        val positions = listOf(
            0.5f to 0.2f,
            0.3f to 0.5f,
            0.7f to 0.5f,
            0.3f to 0.8f,
            0.7f to 0.8f
        )

        graphRepository.fetchCourseGraph(courseId)
            .onSuccess { graph ->
                val nodes = graph.nodes
                    .filter { it.type == "concept" }
                    .take(5)
                    .mapIndexed { index, node ->
                        val (x, y) = positions[index]
                        val status = when {
                            node.reviewPending == true -> NodeStatus.EN_PROGRESO
                            (node.easeFactor ?: 0f) >= 2.5f -> NodeStatus.DOMINADO
                            else -> NodeStatus.BLOQUEADO
                        }
                        GraphNodeUiModel(
                            label = node.label,
                            iconName = when (status) {
                                NodeStatus.DOMINADO -> "check"
                                NodeStatus.EN_PROGRESO -> "circle"
                                NodeStatus.BLOQUEADO -> "lock"
                            },
                            status = status,
                            xOffsetFraction = x,
                            yOffsetFraction = y
                        )
                    }
                _uiState.value = _uiState.value.copy(previewNodes = nodes)
            }
            // onFailure: mini-mapa queda vacío, la pantalla no crashea
    }

    /**
     * Marca un concepto como repasado con rating 4 ("Fácil, recordado con esfuerzo")
     * y lo remueve optimistamente de la lista local al confirmar el servidor.
     */
    fun completeReviewTask(taskId: String) {
        viewModelScope.launch {
            plannerRepository.submitReviewRating(taskId, rating = 4)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        upcomingReviews = _uiState.value.upcomingReviews.filter { it.id != taskId }
                    )
                }
            // onFailure: el concepto permanece en lista, el usuario puede reintentar
        }
    }

    fun renameNote(noteId: String, newTitle: String) {
        viewModelScope.launch {
            offlineFirstNoteRepository.updateNote(noteId, newTitle, courseId)
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            offlineFirstNoteRepository.deleteNote(noteId)
        }
    }

    fun reassociateNote(noteId: String, newCourseId: String?) {
        viewModelScope.launch {
            val noteTitle = _uiState.value.notes.find { it.id == noteId }?.title
            offlineFirstNoteRepository.updateNote(noteId, noteTitle ?: "Apunte sin título", newCourseId)
        }
    }

    /** Genera el texto descriptivo de cuándo vence el próximo repaso. */
    private fun buildScheduleText(nextReviewDate: String, interval: Int): String {
        return when (interval) {
            0 -> "Nuevo concepto • Por aprender"
            1 -> "Vence hoy • Repaso urgente"
            else -> "En $interval días • ${nextReviewDate.take(10)}"
        }
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

    init {
        loadCourseData()
    }
}

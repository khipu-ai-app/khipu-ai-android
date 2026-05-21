package pe.khipuai.app.ui.screens.maps

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.khipuai.app.data.remote.dto.CourseResponse
import pe.khipuai.app.data.remote.dto.GraphResponse
import pe.khipuai.app.data.repository.CourseRepository
import pe.khipuai.app.data.repository.GraphRepository
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin

data class MapsUiState(
    val selectedCourse: String = "Anatomía Humana",
    val selectedDifficulty: String = "Todas",
    val concepts: List<Concept> = emptyList(),
    val selectedConcept: Concept? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class Concept(
    val id: String,
    val title: String,
    val description: String,
    val position: Offset,
    val color: Color,
    val importance: ConceptImportance,
    val connections: List<String>,
    val filesCount: Int,
    val lessonNumber: Int,
    val difficulty: ConceptDifficulty
)

enum class ConceptImportance { HIGH, MEDIUM, LOW }
enum class ConceptDifficulty { BASIC, INTERMEDIATE, ADVANCED }

@HiltViewModel
class MapsViewModel @Inject constructor(
    private val graphRepository: GraphRepository,
    private val courseRepository: CourseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapsUiState())
    val uiState: StateFlow<MapsUiState> = _uiState.asStateFlow()

    init {
        loadGraphForCourseName(_uiState.value.selectedCourse)
    }

    fun updateCourse(courseName: String) {
        _uiState.value = _uiState.value.copy(selectedCourse = courseName)
        loadGraphForCourseName(courseName)
    }

    fun updateDifficulty(difficulty: String) {
        _uiState.value = _uiState.value.copy(selectedDifficulty = difficulty)
        loadGraphForCourseName(_uiState.value.selectedCourse)
    }

    fun selectConcept(concept: Concept?) {
        _uiState.value = _uiState.value.copy(selectedConcept = concept)
    }

    private fun loadGraphForCourseName(name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            // 1. Llamamos al método real de tu repositorio: fetchMyCourses()
            courseRepository.fetchMyCourses()
                .onSuccess { remoteCourses: List<CourseResponse> ->
                    val targetCourse = remoteCourses.find { it.name.equals(name, ignoreCase = true) }
                    val courseId = targetCourse?.id ?: "c1"

                    // 2. Invocamos al nuevo GraphRepository real
                    graphRepository.fetchCourseGraph(courseId)
                        .onSuccess { response: GraphResponse ->
                            val difficultyFilter = _uiState.value.selectedDifficulty
                            val totalNodes = response.nodes.size

                            val mappedConcepts = response.nodes.mapIndexed { index, node ->
                                val angle = index * (2 * Math.PI / if (totalNodes > 1) totalNodes else 1)
                                val radius = when (node.type) {
                                    "course" -> 0.0f
                                    "note" -> 0.22f
                                    else -> 0.42f
                                }

                                val computedPosition = if (node.type == "course") {
                                    Offset(0f, 0f)
                                } else {
                                    Offset(
                                        x = (cos(angle) * radius).toFloat(),
                                        y = (sin(angle) * radius).toFloat()
                                    )
                                }

                                val nodeConnections = response.edges
                                    .filter { it.source == node.id }
                                    .map { it.target }

                                Concept(
                                    id = node.id,
                                    title = node.label,
                                    description = "Nodo clasificado por Khipu AI como [${node.type.uppercase()}]. Retención SM-2: ${node.easeFactor ?: 2.5f}.",
                                    position = computedPosition,
                                    color = when (node.type) {
                                        "course" -> Color(0xFF7B1FA2)
                                        "note" -> Color(0xFF1976D2)
                                        else -> if (node.reviewPending == true) Color(0xFFD32F2F) else Color(0xFF757575)
                                    },
                                    importance = when (node.type) {
                                        "course" -> ConceptImportance.HIGH
                                        "note" -> ConceptImportance.MEDIUM
                                        else -> ConceptImportance.LOW
                                    },
                                    connections = nodeConnections,
                                    filesCount = if (node.type == "note") 1 else 3,
                                    lessonNumber = index + 1,
                                    difficulty = when {
                                        (node.easeFactor ?: 2.5f) < 1.8f -> ConceptDifficulty.ADVANCED
                                        (node.easeFactor ?: 2.5f) < 2.3f -> ConceptDifficulty.INTERMEDIATE
                                        else -> ConceptDifficulty.BASIC
                                    }
                                )
                            }

                            val finalConcepts = if (difficultyFilter == "Todas") {
                                mappedConcepts
                            } else {
                                mappedConcepts.filter {
                                    when (difficultyFilter) {
                                        "Básica" -> it.difficulty == ConceptDifficulty.BASIC
                                        "Intermedia" -> it.difficulty == ConceptDifficulty.INTERMEDIATE
                                        "Avanzada" -> it.difficulty == ConceptDifficulty.ADVANCED
                                        else -> true
                                    }
                                }
                            }

                            _uiState.value = _uiState.value.copy(concepts = finalConcepts, isLoading = false)
                        }
                        .onFailure { e ->
                            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Error en Neo4j: ${e.localizedMessage}")
                        }
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Error al mapear cursos: ${e.localizedMessage}")
                }
        }
    }
}
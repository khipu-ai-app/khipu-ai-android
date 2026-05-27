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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pe.khipuai.app.data.remote.dto.CourseResponse
import pe.khipuai.app.data.remote.dto.GraphResponse
import pe.khipuai.app.data.repository.CourseRepository
import pe.khipuai.app.data.repository.GraphRepository
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin

// ─── UI State ─────────────────────────────────────────────────────────────

data class MapsUiState(
    val selectedCourse: String = "Anatomía Humana",
    val selectedDifficulty: String = "Todas",
    val concepts: List<Concept> = emptyList(),
    val selectedConcept: Concept? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** JSON serializado para inyectar en la WebView D3.js */
    val d3NodesJson: String = "[]",
    val d3EdgesJson: String = "[]"
)

// ─── Domain models ────────────────────────────────────────────────────────

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

// ─── ViewModel ────────────────────────────────────────────────────────────

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

    /** Selección desde el BottomSheet (dismiss o selección manual en lista) */
    fun selectConcept(concept: Concept?) {
        _uiState.value = _uiState.value.copy(selectedConcept = concept)
    }

    /** Selección desde el puente JavaScript → Kotlin de la WebView */
    fun selectConceptById(conceptId: String) {
        val concept = _uiState.value.concepts.find { it.id == conceptId }
        _uiState.value = _uiState.value.copy(selectedConcept = concept)
    }

    private fun loadGraphForCourseName(name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            // 1. Resolver nombre de curso → course_id
            val courseIdResult = courseRepository.fetchMyCourses()
            val courseId = courseIdResult.getOrNull()
                ?.find { it.name.equals(name, ignoreCase = true) }
                ?.id
                ?: run {
                    // Si no hay cursos remotos, usamos un ID de desarrollo
                    "c1"
                }

            // 2. Obtener el grafo de Neo4j
            graphRepository.fetchCourseGraph(courseId)
                .onSuccess { response: GraphResponse ->
                    val difficultyFilter = _uiState.value.selectedDifficulty

                    // Mapear nodos a modelos de dominio
                    val mappedConcepts = response.nodes.mapIndexed { index, node ->
                        val totalNodes = response.nodes.size
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
                            description = buildConceptDescription(node.type, node.easeFactor),
                            position = computedPosition,
                            color = buildConceptColor(node.type, node.reviewPending),
                            importance = buildConceptImportance(node.type),
                            connections = nodeConnections,
                            filesCount = if (node.type == "note") 1 else 3,
                            lessonNumber = index + 1,
                            difficulty = buildConceptDifficulty(node.easeFactor)
                        )
                    }

                    // Aplicar filtro de dificultad/retención
                    val finalConcepts = applyDifficultyFilter(mappedConcepts, difficultyFilter)

                    // Serializar nodos y aristas filtrados para la WebView D3.js
                    val filteredNodes = response.nodes.filter { node ->
                        difficultyFilter == "Todas" ||
                            buildConceptDifficultyLabel(node.easeFactor) == difficultyFilter
                    }
                    val filteredEdges = response.edges.filter { edge ->
                        filteredNodes.any { it.id == edge.source } &&
                            filteredNodes.any { it.id == edge.target }
                    }

                    _uiState.value = _uiState.value.copy(
                        concepts = finalConcepts,
                        d3NodesJson = Json.encodeToString(filteredNodes),
                        d3EdgesJson = Json.encodeToString(filteredEdges),
                        isLoading = false
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Error al cargar el grafo: ${e.localizedMessage}"
                    )
                }
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private fun buildConceptDescription(type: String, easeFactor: Float?): String {
        val ef = easeFactor ?: 2.5f
        return "Tipo: ${type.uppercase()} · Factor de facilidad SM-2: ${"%.2f".format(ef)}"
    }

    private fun buildConceptColor(type: String, reviewPending: Boolean?): Color = when (type) {
        "course" -> Color(0xFF7B1FA2)
        "note" -> Color(0xFF1976D2)
        else -> if (reviewPending == true) Color(0xFFD32F2F) else Color(0xFF388E3C)
    }

    private fun buildConceptImportance(type: String): ConceptImportance = when (type) {
        "course" -> ConceptImportance.HIGH
        "note" -> ConceptImportance.MEDIUM
        else -> ConceptImportance.LOW
    }

    private fun buildConceptDifficulty(easeFactor: Float?): ConceptDifficulty {
        val ef = easeFactor ?: 2.5f
        return when {
            ef < 1.8f -> ConceptDifficulty.ADVANCED
            ef < 2.3f -> ConceptDifficulty.INTERMEDIATE
            else -> ConceptDifficulty.BASIC
        }
    }

    private fun buildConceptDifficultyLabel(easeFactor: Float?): String {
        val ef = easeFactor ?: 2.5f
        return when {
            ef < 1.8f -> "Avanzada"
            ef < 2.3f -> "Intermedia"
            else -> "Básica"
        }
    }

    private fun applyDifficultyFilter(concepts: List<Concept>, filter: String): List<Concept> {
        if (filter == "Todas") return concepts
        return concepts.filter {
            when (filter) {
                "Básica" -> it.difficulty == ConceptDifficulty.BASIC
                "Intermedia" -> it.difficulty == ConceptDifficulty.INTERMEDIATE
                "Avanzada" -> it.difficulty == ConceptDifficulty.ADVANCED
                else -> true
            }
        }
    }
}
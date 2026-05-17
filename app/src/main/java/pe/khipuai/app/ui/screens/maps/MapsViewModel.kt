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
import javax.inject.Inject

data class MapsUiState(
    val selectedCourse: String = "Inteligencia Artificial",
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
    val position: Offset, // Relative position (-0.5 to 0.5)
    val color: Color,
    val importance: ConceptImportance,
    val connections: List<String>, // IDs of connected concepts
    val filesCount: Int,
    val lessonNumber: Int,
    val difficulty: ConceptDifficulty
)

enum class ConceptImportance {
    HIGH, MEDIUM, LOW
}

enum class ConceptDifficulty {
    BASIC, INTERMEDIATE, ADVANCED
}

@HiltViewModel
class MapsViewModel @Inject constructor(
    // TODO: Inject MapsRepository when implemented
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(
        MapsUiState(
            concepts = getSampleConcepts()
        )
    )
    val uiState: StateFlow<MapsUiState> = _uiState.asStateFlow()
    
    fun updateCourse(course: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectedCourse = course,
                concepts = getConceptsForCourse(course)
            )
        }
    }
    
    fun updateDifficulty(difficulty: String) {
        viewModelScope.launch {
            val filteredConcepts = if (difficulty == "Todas") {
                getSampleConcepts()
            } else {
                getSampleConcepts().filter { concept ->
                    when (difficulty) {
                        "Básica" -> concept.difficulty == ConceptDifficulty.BASIC
                        "Intermedia" -> concept.difficulty == ConceptDifficulty.INTERMEDIATE
                        "Avanzada" -> concept.difficulty == ConceptDifficulty.ADVANCED
                        else -> true
                    }
                }
            }
            
            _uiState.value = _uiState.value.copy(
                selectedDifficulty = difficulty,
                concepts = filteredConcepts
            )
        }
    }
    
    fun selectConcept(concept: Concept?) {
        _uiState.value = _uiState.value.copy(selectedConcept = concept)
    }
    
    private fun getSampleConcepts(): List<Concept> {
        return listOf(
            Concept(
                id = "1",
                title = "Redes Neuronales",
                description = "La unidad fundamental de las redes neuronales artificiales, inspirada en las neuronas biológicas.",
                position = Offset(0f, 0f), // Center
                color = Color(0xFF7B1FA2),
                importance = ConceptImportance.HIGH,
                connections = listOf("2", "3", "4", "5"),
                filesCount = 3,
                lessonNumber = 4,
                difficulty = ConceptDifficulty.ADVANCED
            ),
            Concept(
                id = "2",
                title = "Python",
                description = "Lenguaje de programación utilizado para implementar algoritmos de IA.",
                position = Offset(0.25f, -0.3f), // Top right
                color = Color(0xFF1976D2),
                importance = ConceptImportance.MEDIUM,
                connections = listOf("1"),
                filesCount = 5,
                lessonNumber = 1,
                difficulty = ConceptDifficulty.BASIC
            ),
            Concept(
                id = "3",
                title = "Matemáticas",
                description = "Fundamentos matemáticos necesarios para entender la IA.",
                position = Offset(-0.3f, -0.1f), // Top left
                color = Color(0xFF1976D2),
                importance = ConceptImportance.MEDIUM,
                connections = listOf("1"),
                filesCount = 8,
                lessonNumber = 2,
                difficulty = ConceptDifficulty.INTERMEDIATE
            ),
            Concept(
                id = "4",
                title = "Historia AI",
                description = "Evolución histórica de la inteligencia artificial.",
                position = Offset(-0.35f, 0.25f), // Bottom left
                color = Color(0xFF757575),
                importance = ConceptImportance.LOW,
                connections = listOf("1"),
                filesCount = 2,
                lessonNumber = 1,
                difficulty = ConceptDifficulty.BASIC
            ),
            Concept(
                id = "5",
                title = "Algoritmos",
                description = "Algoritmos fundamentales utilizados en IA y machine learning.",
                position = Offset(0.1f, 0.35f), // Bottom center
                color = Color(0xFF757575),
                importance = ConceptImportance.LOW,
                connections = listOf("1"),
                filesCount = 4,
                lessonNumber = 3,
                difficulty = ConceptDifficulty.INTERMEDIATE
            )
        )
    }
    
    private fun getConceptsForCourse(course: String): List<Concept> {
        // TODO: Implement course-specific concepts
        return when (course) {
            "Inteligencia Artificial" -> getSampleConcepts()
            "Matemáticas" -> getMathConcepts()
            "Historia" -> getHistoryConcepts()
            "Psicología" -> getPsychologyConcepts()
            else -> getSampleConcepts()
        }
    }
    
    private fun getMathConcepts(): List<Concept> {
        return listOf(
            Concept(
                id = "math1",
                title = "Álgebra Lineal",
                description = "Fundamentos de vectores, matrices y transformaciones lineales.",
                position = Offset(0f, 0f),
                color = Color(0xFF4B00B2),
                importance = ConceptImportance.HIGH,
                connections = listOf("math2", "math3"),
                filesCount = 6,
                lessonNumber = 1,
                difficulty = ConceptDifficulty.INTERMEDIATE
            ),
            Concept(
                id = "math2",
                title = "Cálculo",
                description = "Derivadas, integrales y sus aplicaciones.",
                position = Offset(0.3f, -0.2f),
                color = Color(0xFF1976D2),
                importance = ConceptImportance.MEDIUM,
                connections = listOf("math1"),
                filesCount = 4,
                lessonNumber = 2,
                difficulty = ConceptDifficulty.ADVANCED
            ),
            Concept(
                id = "math3",
                title = "Estadística",
                description = "Probabilidad y análisis estadístico.",
                position = Offset(-0.3f, 0.2f),
                color = Color(0xFF2E7D32),
                importance = ConceptImportance.MEDIUM,
                connections = listOf("math1"),
                filesCount = 5,
                lessonNumber = 3,
                difficulty = ConceptDifficulty.INTERMEDIATE
            )
        )
    }
    
    private fun getHistoryConcepts(): List<Concept> {
        return listOf(
            Concept(
                id = "hist1",
                title = "Revolución Industrial",
                description = "Transformación económica y social de los siglos XVIII-XIX.",
                position = Offset(0f, 0f),
                color = Color(0xFF2E7D32),
                importance = ConceptImportance.HIGH,
                connections = listOf("hist2", "hist3"),
                filesCount = 8,
                lessonNumber = 5,
                difficulty = ConceptDifficulty.INTERMEDIATE
            ),
            Concept(
                id = "hist2",
                title = "Capitalismo",
                description = "Sistema económico basado en la propiedad privada.",
                position = Offset(0.25f, -0.25f),
                color = Color(0xFF1976D2),
                importance = ConceptImportance.MEDIUM,
                connections = listOf("hist1"),
                filesCount = 3,
                lessonNumber = 6,
                difficulty = ConceptDifficulty.BASIC
            ),
            Concept(
                id = "hist3",
                title = "Urbanización",
                description = "Proceso de crecimiento de las ciudades.",
                position = Offset(-0.25f, 0.25f),
                color = Color(0xFF757575),
                importance = ConceptImportance.LOW,
                connections = listOf("hist1"),
                filesCount = 2,
                lessonNumber = 7,
                difficulty = ConceptDifficulty.BASIC
            )
        )
    }
    
    private fun getPsychologyConcepts(): List<Concept> {
        return listOf(
            Concept(
                id = "psy1",
                title = "Psicoanálisis",
                description = "Teoría y método terapéutico desarrollado por Freud.",
                position = Offset(0f, 0f),
                color = Color(0xFFD32F2F),
                importance = ConceptImportance.HIGH,
                connections = listOf("psy2", "psy3"),
                filesCount = 7,
                lessonNumber = 3,
                difficulty = ConceptDifficulty.ADVANCED
            ),
            Concept(
                id = "psy2",
                title = "Inconsciente",
                description = "Parte de la mente que contiene pensamientos reprimidos.",
                position = Offset(0.3f, -0.2f),
                color = Color(0xFF7B1FA2),
                importance = ConceptImportance.MEDIUM,
                connections = listOf("psy1"),
                filesCount = 4,
                lessonNumber = 4,
                difficulty = ConceptDifficulty.INTERMEDIATE
            ),
            Concept(
                id = "psy3",
                title = "Transferencia",
                description = "Proceso donde el paciente proyecta sentimientos al terapeuta.",
                position = Offset(-0.3f, 0.2f),
                color = Color(0xFF1976D2),
                importance = ConceptImportance.MEDIUM,
                connections = listOf("psy1"),
                filesCount = 2,
                lessonNumber = 5,
                difficulty = ConceptDifficulty.ADVANCED
            )
        )
    }
}
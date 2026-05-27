package pe.khipuai.app.ui.screens.planner

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.khipuai.app.data.repository.PlannerRepository
import javax.inject.Inject

data class PlannerUiState(
    val studyBlocks: List<StudyBlock> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

// Mantenemos tus modelos originales de UI intactos para no romper tu Screen
data class StudyBlock(
    val id: String,
    val time: String,
    val duration: String,
    val subject: String,
    val tasks: List<Task>,
    val isAISuggestion: Boolean,
    val mentalLoadLevel: String,
    val mentalLoadColor: Color,
    val color: Color,
    val type: StudyBlockType
)

data class Task(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false
)

enum class StudyBlockType {
    FOCUS, REVIEW, BREAK
}

@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val plannerRepository: PlannerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlannerUiState(isLoading = true))
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

    init {
        loadRemotePlanner()
    }

    fun loadRemotePlanner() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            plannerRepository.fetchDailyAgenda()
                .onSuccess { networkConcepts ->
                    if (networkConcepts.isEmpty()) {
                        _uiState.value = _uiState.value.copy(studyBlocks = emptyList(), isLoading = false)
                        return@onSuccess
                    }

                    // 🧠 MAPEO HÍBRIDO: Agrupamos los conceptos del backend por nombre de curso
                    val conceptsByCourse = networkConcepts.groupBy { it.courseName }

                    val mappedBlocks = conceptsByCourse.entries.mapIndexed { index, entry ->
                        val courseName = entry.key
                        val concepts = entry.value

                        // Calculamos una carga mental dinámica basada en la cantidad de conceptos a repasar
                        val (loadLevel, loadColor) = when {
                            concepts.size >= 4 -> "Alta" to Color(0xFFD32F2F)
                            concepts.size >= 2 -> "Media" to Color(0xFFF57C00)
                            else -> "Baja" to Color(0xFF388E3C)
                        }

                        StudyBlock(
                            id = courseName, // Usamos el nombre del curso como ID del bloque
                            time = "${8 + index * 2}:00 AM", // Simulamos un bloque horario elegante
                            duration = "${concepts.size * 10} min", // 10 minutos sugeridos por concepto
                            subject = courseName,
                            tasks = concepts.map { Task(id = it.conceptId, title = it.label, isCompleted = false) },
                            isAISuggestion = true, // Es sugerencia de la IA porque viene del algoritmo SM-2
                            mentalLoadLevel = loadLevel,
                            mentalLoadColor = loadColor,
                            color = Color(0xFF7B1FA2), // Color base para la geometría
                            type = StudyBlockType.REVIEW
                        )
                    }

                    _uiState.value = _uiState.value.copy(studyBlocks = mappedBlocks, isLoading = false)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Error al sincronizar tu agenda con la IA: ${exception.localizedMessage}"
                    )
                }
        }
    }

    fun toggleTask(blockId: String, conceptId: String) {
        viewModelScope.launch {
            val currentBlocks = _uiState.value.studyBlocks
            var finalIsCompleted = false

            // 1. Actualización en memoria local para una respuesta táctil instantánea
            val updatedBlocks = currentBlocks.map { block ->
                if (block.id == blockId) {
                    val updatedTasks = block.tasks.map { task ->
                        if (task.id == conceptId) {
                            finalIsCompleted = !task.isCompleted
                            task.copy(isCompleted = finalIsCompleted)
                        } else {
                            task
                        }
                    }
                    block.copy(tasks = updatedTasks)
                } else {
                    block
                }
            }
            _uiState.value = _uiState.value.copy(studyBlocks = updatedBlocks)

            // 2. Persistencia algorítmica: Si el usuario marca el check, le mandamos un 5 (Recuerdo perfecto)
            // de lo contrario un 1 (Olvido temporal) para que el algoritmo SM-2 de Neo4j haga su magia
            val score = if (finalIsCompleted) 5 else 1

            plannerRepository.submitReviewRating(conceptId, score)
                .onFailure {
                    // Si la red falla, revertimos el estado de manera segura
                    loadRemotePlanner()
                }
        }
    }

    fun submitRating(blockId: String, conceptId: String, rating: Int) {
        viewModelScope.launch {
            val currentBlocks = _uiState.value.studyBlocks
            val updatedBlocks = currentBlocks.map { block ->
                if (block.id == blockId) {
                    val updatedTasks = block.tasks.map { task ->
                        if (task.id == conceptId) {
                            task.copy(isCompleted = true)
                        } else {
                            task
                        }
                    }
                    block.copy(tasks = updatedTasks)
                } else {
                    block
                }
            }
            _uiState.value = _uiState.value.copy(studyBlocks = updatedBlocks)

            plannerRepository.submitReviewRating(conceptId, rating)
                .onFailure {
                    loadRemotePlanner()
                }
        }
    }
}
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
                .onSuccess { networkBlocks ->
                    // 🧠 MAPEO INTELIGENTE: Traducimos contratos de red a objetos gráficos de Compose
                    val mappedBlocks = networkBlocks.map { blockDto ->
                        StudyBlock(
                            id = blockDto.id,
                            time = blockDto.time,
                            duration = blockDto.duration,
                            subject = blockDto.subject,
                            tasks = blockDto.tasks.map { Task(it.id, it.title, it.isCompleted) },
                            isAISuggestion = blockDto.isAISuggestion,
                            mentalLoadLevel = blockDto.mentalLoadLevel,
                            mentalLoadColor = safeParseColor(blockDto.mentalLoadColor),
                            color = safeParseColor(blockDto.color),
                            type = when (blockDto.type) {
                                "FOCUS" -> StudyBlockType.FOCUS
                                "BREAK" -> StudyBlockType.BREAK
                                else -> StudyBlockType.REVIEW
                            }
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

    fun toggleTask(blockId: String, taskId: String) {
        viewModelScope.launch {
            val currentBlocks = _uiState.value.studyBlocks
            var targetIsCompleted = false

            // 1. Clonación y actualización optimista en memoria local para dar respuesta instantánea en la UI
            val updatedBlocks = currentBlocks.map { block ->
                if (block.id == blockId) {
                    val updatedTasks = block.tasks.map { task ->
                        if (task.id == taskId) {
                            targetIsCompleted = !task.isCompleted
                            task.copy(isCompleted = targetIsCompleted)
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

            // 2. Persistencia en caliente en la base de datos relacional de Docker
            plannerRepository.updateTaskCompletion(blockId, taskId, targetIsCompleted)
                .onFailure {
                    // Fallback: Si el servidor falla, revertimos el check para no engañar al estudiante
                    loadRemotePlanner()
                }
        }
    }

    // Helper seguro para evitar caídas si el Hex del backend viene corrupto o vacío
    private fun safeParseColor(hexString: String): Color {
        return try {
            if (hexString.isBlank()) return Color.Gray
            Color(android.graphics.Color.parseColor(hexString))
        } catch (_: Exception) {
            Color(0xFF1976D2) // Azul corporativo por defecto
        }
    }
}
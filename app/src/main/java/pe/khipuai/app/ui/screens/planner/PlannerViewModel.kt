package pe.khipuai.app.ui.screens.planner

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    // TODO: Inject PlannerRepository when implemented
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(
        PlannerUiState(
            studyBlocks = getSampleStudyBlocks()
        )
    )
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()
    
    fun toggleTask(blockId: String, taskId: String) {
        viewModelScope.launch {
            val currentBlocks = _uiState.value.studyBlocks
            val updatedBlocks = currentBlocks.map { block ->
                if (block.id == blockId) {
                    val updatedTasks = block.tasks.map { task ->
                        if (task.id == taskId) {
                            task.copy(isCompleted = !task.isCompleted)
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
        }
    }
    
    private fun getSampleStudyBlocks(): List<StudyBlock> {
        return listOf(
            StudyBlock(
                id = "1",
                time = "00",
                duration = "2h Enfoque",
                subject = "Anatomía: Sistema Nervioso",
                tasks = listOf(
                    Task(
                        id = "1-1",
                        title = "Repasar Flashcards de Anatomía (Tronco Encefálico)",
                        isCompleted = false
                    ),
                    Task(
                        id = "1-2",
                        title = "Leer resumen del Capítulo 4",
                        isCompleted = false
                    )
                ),
                isAISuggestion = true,
                mentalLoadLevel = "Alta",
                mentalLoadColor = Color(0xFF7B1FA2),
                color = Color(0xFF7B1FA2),
                type = StudyBlockType.FOCUS
            ),
            StudyBlock(
                id = "2",
                time = "00",
                duration = "Descanso Recomendado (30 min)",
                subject = "",
                tasks = emptyList(),
                isAISuggestion = false,
                mentalLoadLevel = "",
                mentalLoadColor = Color.Gray,
                color = Color.Gray,
                type = StudyBlockType.BREAK
            ),
            StudyBlock(
                id = "3",
                time = "30",
                duration = "1.5h Enfoque",
                subject = "Microeconomía: Curvas de Demanda",
                tasks = listOf(
                    Task(
                        id = "3-1",
                        title = "Leer resumen de Microeconomía",
                        isCompleted = true
                    ),
                    Task(
                        id = "3-2",
                        title = "Resolver set de problemas 2",
                        isCompleted = false
                    )
                ),
                isAISuggestion = false,
                mentalLoadLevel = "Media",
                mentalLoadColor = Color(0xFF2E7D32),
                color = Color(0xFF1976D2),
                type = StudyBlockType.REVIEW
            )
        )
    }
}
package pe.khipuai.app.ui.screens.coursedetail

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.khipuai.app.data.repository.CourseRepository
import pe.khipuai.app.data.repository.NoteRepository
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
    val xOffsetFraction: Float, // Posición relativa en la caja (0.0 a 1.0)
    val yOffsetFraction: Float
)

enum class NodeStatus { DOMINADO, EN_PROGRESO, BLOQUEADO }

data class CourseDetailUiState(
    val courseName: String = "Matemáticas",
    val categoryName: String = "Ciencias Exactas",
    val professorName: String = "Elena Rojas", // ✨ REFACTORIZADO: quitado "Prof." del mock para cumplir la UI real
    val courseProgress: Int = 45,
    val notes: List<CompactNoteUiModel> = emptyList(),
    val upcomingReviews: List<ReviewItemUiModel> = emptyList(),
    val previewNodes: List<GraphNodeUiModel> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class CourseDetailViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val noteRepository: NoteRepository
) : ViewModel() {


    private val _uiState = MutableStateFlow(CourseDetailUiState(isLoading = true))
    val uiState: StateFlow<CourseDetailUiState> = _uiState.asStateFlow()

    init {
        loadCourseDetail()
    }

    private fun loadCourseDetail() {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            notes = listOf(
                CompactNoteUiModel("1", "Derivadas Parciales", "Resumen de la clase sobre derivadas parciales de segundo orden y el Teorema de Clairaut.", "Hace 2 días", "Cálculo II"),
                CompactNoteUiModel("2", "Integrales Múltiples", "Ejercicios resueltos de integrales dobles y triples usando coordenadas polares y cilíndricas.", "Hace 1 semana", "Ejercicios")
            ),
            upcomingReviews = listOf(
                ReviewItemUiModel("r1", "Examen: Cálculo Vectorial", "Mañana, 10:00 AM", isUrgent = true),
                ReviewItemUiModel("r2", "Repasar Serie de Taylor", "Jueves, 14 Feb", isUrgent = false)
            ),
            previewNodes = listOf(
                GraphNodeUiModel("Cálculo", "functions", NodeStatus.EN_PROGRESO, 0.5f, 0.2f),
                GraphNodeUiModel("Derivadas", "check", NodeStatus.DOMINADO, 0.3f, 0.5f),
                GraphNodeUiModel("Integrales", "circle", NodeStatus.EN_PROGRESO, 0.7f, 0.5f),
                GraphNodeUiModel("Vectores", "lock", NodeStatus.BLOQUEADO, 0.5f, 0.8f)
            )
        )
    }

    fun completeReviewTask(taskId: String) {
        // Remueve o marca la tarea asíncrona en el backend
    }
}
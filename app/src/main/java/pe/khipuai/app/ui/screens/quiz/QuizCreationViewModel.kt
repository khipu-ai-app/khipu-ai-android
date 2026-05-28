package pe.khipuai.app.ui.screens.quiz

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.khipuai.app.data.repository.NoteRepository
import javax.inject.Inject

// Modelos locales ligeros para renderizado visual
data class QuestionUiModel(
    val id: Int,
    val question: String,
    val options: List<String>,
    val selectedOptionIndex: Int? = null,
    val correctOptionIndex: Int = 1 // Mock de respuesta correcta
)

data class QuizCreationUiState(
    val courseName: String = "Introducción a la Psicología",
    val quizTitle: String = "Cuestionario de Práctica",
    val questions: List<QuestionUiModel> = emptyList(),
    val isAddQuestionsDialogOpen: Boolean = false,
    val sliderQuestionsCount: Float = 5f,
    val isSaving: Boolean = false
)

@HiltViewModel
class QuizCreationViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {


    private val _uiState = MutableStateFlow(QuizCreationUiState())
    val uiState: StateFlow<QuizCreationUiState> = _uiState.asStateFlow()

    init {
        // Inicializar sin datos falsos, dependiendo enteramente de la IA o el Backend
        _uiState.value = _uiState.value.copy(questions = emptyList())
    }

    fun selectOption(questionId: Int, optionIndex: Int) {
        val updatedQuestions = _uiState.value.questions.map { question ->
            if (question.id == questionId) {
                question.copy(selectedOptionIndex = optionIndex)
            } else question
        }
        _uiState.value = _uiState.value.copy(questions = updatedQuestions)
    }

    fun toggleAddQuestionsDialog(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isAddQuestionsDialogOpen = isOpen)
    }

    fun updateSliderValue(value: Float) {
        _uiState.value = _uiState.value.copy(sliderQuestionsCount = value)
    }

    fun saveQuiz() {
        _uiState.value = _uiState.value.copy(isSaving = true)
        // Aquí se conectará el pipeline de persistencia relacional con Postgres
    }
}
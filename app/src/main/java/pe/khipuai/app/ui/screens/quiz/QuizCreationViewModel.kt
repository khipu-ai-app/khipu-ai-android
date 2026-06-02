package pe.khipuai.app.ui.screens.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.khipuai.app.data.repository.NoteRepository
import javax.inject.Inject

data class QuestionUiModel(
    val id: Int,
    val question: String,
    val options: List<String>,
    val selectedOptionIndex: Int? = null,
    val correctOptionIndex: Int = 0
)

data class QuizCreationUiState(
    val courseName: String = "",
    val quizTitle: String = "Cuestionario de Práctica",
    val questions: List<QuestionUiModel> = emptyList(),
    val isAddQuestionsDialogOpen: Boolean = false,
    val sliderQuestionsCount: Float = 5f,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class QuizCreationViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val noteId: String = savedStateHandle["noteId"] ?: ""

    private val _uiState = MutableStateFlow(QuizCreationUiState())
    val uiState: StateFlow<QuizCreationUiState> = _uiState.asStateFlow()

    init {
        loadStudyGuide()
    }

    private fun loadStudyGuide() {
        if (noteId.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            noteRepository.fetchStudyGuide(noteId)
                .onSuccess { guide ->
                    val questions = guide.questions.map { dto ->
                        QuestionUiModel(
                            id = dto.id,
                            question = dto.question,
                            options = dto.options
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        quizTitle = guide.title,
                        questions = questions,
                        isLoading = false
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.localizedMessage
                    )
                }
        }
    }

    fun selectOption(questionId: Int, optionIndex: Int) {
        val updatedQuestions = _uiState.value.questions.map { question ->
            if (question.id == questionId) question.copy(selectedOptionIndex = optionIndex)
            else question
        }
        _uiState.value = _uiState.value.copy(questions = updatedQuestions)
    }

    fun toggleAddQuestionsDialog(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isAddQuestionsDialogOpen = isOpen)
    }

    fun updateSliderValue(value: Float) {
        _uiState.value = _uiState.value.copy(sliderQuestionsCount = value)
    }

    // El quiz se persiste implícitamente en el backend al generar la guía de estudio.
    // Este método marca el guardado como completado localmente.
    fun saveQuiz() {
        _uiState.value = _uiState.value.copy(isSaving = false)
    }
}

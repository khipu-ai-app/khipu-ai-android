package pe.khipuai.app.ui.screens.studyguide

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.khipuai.app.data.remote.dto.GlossaryTermDto
import pe.khipuai.app.data.remote.dto.QuizResultRequest
import pe.khipuai.app.data.repository.NoteRepository
import javax.inject.Inject
import kotlin.math.roundToInt

data class FlashcardUiModel(
    val question: String,
    val answer: String,
    val isRevealed: Boolean = false // Permite controlar el volteo individual de la tarjeta
)

data class QuestionUiModel(
    val id: Int,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val selectedOptionIndex: Int? = null
)

data class StudyGuideUiState(
    val title: String = "Cargando guía...",
    val date: String = "",
    val executiveSummary: String = "",
    val glossary: List<GlossaryTermDto> = emptyList(),
    val flashcards: List<FlashcardUiModel> = emptyList(),
    val questions: List<QuestionUiModel> = emptyList(),
    val currentFlashcardIndex: Int = 0,
    val isQuizSubmitted: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class StudyGuideViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val noteId: String? = savedStateHandle["noteId"]

    private val _uiState = MutableStateFlow(StudyGuideUiState(isLoading = true))
    val uiState: StateFlow<StudyGuideUiState> = _uiState.asStateFlow()

    fun loadStudyGuideContent() {
        val id = noteId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            noteRepository.fetchStudyGuide(id)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        title = response.title,
                        date = response.date,
                        executiveSummary = response.executiveSummary,
                        glossary = response.glossary,
                        flashcards = response.flashcards.map { FlashcardUiModel(it.question, it.answer) },
                        questions = response.questions.map { QuestionUiModel(it.id, it.question, it.options, it.correctIndex, it.explanation) }
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Error al conectar con la factoría de guías: ${exception.localizedMessage}"
                    )
                }
        }
    }

    fun flipCurrentFlashcard() {
        val index = _uiState.value.currentFlashcardIndex
        val list = _uiState.value.flashcards.toMutableList()
        if (index in list.indices) {
            list[index] = list[index].copy(isRevealed = !list[index].isRevealed)
            _uiState.value = _uiState.value.copy(flashcards = list)
        }
    }

    fun previousFlashcard() {
        if (_uiState.value.currentFlashcardIndex > 0) {
            _uiState.value = _uiState.value.copy(
                currentFlashcardIndex = _uiState.value.currentFlashcardIndex - 1
            )
            resetFlashcardFlipState(_uiState.value.currentFlashcardIndex)
        }
    }

    fun nextFlashcard() {
        if (_uiState.value.currentFlashcardIndex < _uiState.value.flashcards.size - 1) {
            _uiState.value = _uiState.value.copy(
                currentFlashcardIndex = _uiState.value.currentFlashcardIndex + 1
            )
            resetFlashcardFlipState(_uiState.value.currentFlashcardIndex)
        }
    }

    private fun resetFlashcardFlipState(index: Int) {
        val list = _uiState.value.flashcards.toMutableList()
        if (index in list.indices) {
            list[index] = list[index].copy(isRevealed = false)
            _uiState.value = _uiState.value.copy(flashcards = list)
        }
    }

    fun selectQuizOption(questionId: Int, optionIndex: Int) {
        if (_uiState.value.isQuizSubmitted) return
        val updatedQuestions = _uiState.value.questions.map { q ->
            if (q.id == questionId) q.copy(selectedOptionIndex = optionIndex) else q
        }
        _uiState.value = _uiState.value.copy(questions = updatedQuestions)
    }

    fun submitQuiz() {
        if (_uiState.value.isQuizSubmitted) return
        
        val questions = _uiState.value.questions
        val correctCount = questions.count { it.selectedOptionIndex == it.correctIndex }
        val percentage = if (questions.isNotEmpty()) (correctCount.toFloat() / questions.size) * 100 else 0f
        
        _uiState.value = _uiState.value.copy(isQuizSubmitted = true)
        
        // POST to backend
        val id = noteId ?: return
        viewModelScope.launch {
            noteRepository.submitQuizResult(
                id, 
                QuizResultRequest(score = correctCount, total = questions.size, percentage = percentage)
            )
        }
    }

    fun resetQuiz() {
        val resetQuestions = _uiState.value.questions.map { it.copy(selectedOptionIndex = null) }
        _uiState.value = _uiState.value.copy(
            isQuizSubmitted = false,
            questions = resetQuestions
        )
    }

    init {
        loadStudyGuideContent()
    }
}
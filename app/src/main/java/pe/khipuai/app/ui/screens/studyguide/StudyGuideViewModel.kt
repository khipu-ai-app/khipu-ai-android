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
    val questions: List<QuestionUiModel> = emptyList(),
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
                        questions = response.questions.map { QuestionUiModel(it.id, it.question, it.options, it.correctIndex, it.explanation) }
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = pe.khipuai.app.core.network.NetworkErrorMapper.from(exception).message
                    )
                }
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
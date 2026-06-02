package pe.khipuai.app.ui.screens.studyguide

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

data class FlashcardUiModel(
    val question: String,
    val answer: String,
    val isRevealed: Boolean = false // Permite controlar el volteo individual de la tarjeta
)

data class QuestionUiModel(
    val id: Int,
    val question: String,
    val options: List<String>,
    val selectedOptionIndex: Int? = null
)

data class StudyGuideUiState(
    val title: String = "Cargando guía...",
    val date: String = "",
    val executiveSummary: String = "",
    val glossary: String = "",
    val flashcards: List<FlashcardUiModel> = emptyList(),
    val questions: List<QuestionUiModel> = emptyList(),
    val currentFlashcardIndex: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class StudyGuideViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val noteId: String? = savedStateHandle["noteId"]

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
                        questions = response.questions.map { QuestionUiModel(it.id, it.question, it.options) }
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

    fun selectQuizOption(questionId: Int, optionIndex: Int) {
        val updatedQuestions = _uiState.value.questions.map { q ->
            if (q.id == questionId) q.copy(selectedOptionIndex = optionIndex) else q
        }
        _uiState.value = _uiState.value.copy(questions = updatedQuestions)
    }

    init {
        loadStudyGuideContent()
    }
}
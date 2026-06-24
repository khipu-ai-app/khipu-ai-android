package pe.khipuai.app.ui.screens.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.khipuai.app.data.remote.dto.StandaloneQuestionDto
import pe.khipuai.app.data.remote.dto.StandaloneQuizResponse

import pe.khipuai.app.data.repository.NoteRepository
import javax.inject.Inject

data class QuestionUiModel(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctOptionIndex: Int = 0,
    val selectedOptionIndex: Int? = null,
    val explanation: String = ""
)

data class TopicCoverage(
    val name: String,
    val isCovered: Boolean,
    val isSelected: Boolean,
    val coveredDifficulties: Set<String> = emptySet()
)

data class QuizCreationUiState(
    val quizTitle: String = "Cuestionario de Práctica",
    val questions: List<QuestionUiModel> = emptyList(),
    val isDashboardMode: Boolean = true,
    val currentQuizId: String? = null,
    val pastQuizzes: List<StandaloneQuizResponse> = emptyList(),
    val missingTopics: List<String> = emptyList(),
    val noteTopics: List<TopicCoverage> = emptyList(),
    val isSubmitted: Boolean = false,
    val score: Int = 0,
    val isAddQuestionsDialogOpen: Boolean = false,
    val sliderQuestionsCount: Float = 5f,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSavedSuccessfully: Boolean = false,
    val isAppending: Boolean = false,
    val error: String? = null,
    val selectedDifficulty: String = "Intermedio"
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
        loadInitialQuiz()
    }

    fun loadInitialQuiz() {
        if (noteId.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val noteDetailRes = noteRepository.getNoteDetail(noteId)
            val quizzesRes = noteRepository.getSavedQuizzes(noteId)
            
            val noteTopicsNames = noteDetailRes.getOrNull()?.topics?.map { it.name } ?: emptyList()
            val pastQuizzes = quizzesRes.getOrNull() ?: emptyList()
            
            val topicDifficulties = mutableMapOf<String, MutableSet<String>>()
            pastQuizzes.forEach { quiz ->
                quiz.topics.forEach { topic ->
                    val normTopic = topic.lowercase().trim()
                    val difficulties = topicDifficulties.getOrPut(normTopic) { mutableSetOf() }
                    quiz.difficulty.let { diff ->
                        if (diff.isNotBlank()) difficulties.add(diff)
                    }
                }
            }
            
            val coveredTopics = topicDifficulties.keys
            val missingTopics = noteTopicsNames.filter { !coveredTopics.contains(it.lowercase().trim()) }
            
            val noteTopics = noteTopicsNames.map { name ->
                val normName = name.lowercase().trim()
                TopicCoverage(
                    name = name,
                    isCovered = coveredTopics.contains(normName),
                    isSelected = !coveredTopics.contains(normName),
                    coveredDifficulties = topicDifficulties[normName] ?: emptySet()
                )
            }
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                pastQuizzes = pastQuizzes,
                missingTopics = missingTopics,
                noteTopics = noteTopics,
                isDashboardMode = true
            )
        }
    }

    fun openQuiz(quiz: StandaloneQuizResponse) {
        _uiState.value = _uiState.value.copy(
            isDashboardMode = false,
            isSubmitted = false,
            questions = quiz.questions.map { it.toUiModel().copy(selectedOptionIndex = null) },
            score = 0,
            selectedDifficulty = quiz.difficulty,
            currentQuizId = quiz.id
        )
    }

    fun exitQuiz() {
        _uiState.value = _uiState.value.copy(
            isDashboardMode = true,
            isSubmitted = false,
            score = 0,
            questions = emptyList(),
            currentQuizId = null
        )
        loadInitialQuiz()
    }

    fun deleteQuiz(quizId: String) {
        viewModelScope.launch {
            noteRepository.deleteQuiz(noteId, quizId).onSuccess {
                loadInitialQuiz()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = pe.khipuai.app.core.network.NetworkErrorMapper.from(e).message)
            }
        }
    }

    fun generateMoreQuestions() {
        val count = _uiState.value.sliderQuestionsCount.toInt()
        generateQuiz(count = count, isAppending = true)
        toggleAddQuestionsDialog(false)
    }

    private fun generateQuiz(count: Int, isAppending: Boolean) {
        viewModelScope.launch {
            if (isAppending) {
                _uiState.value = _uiState.value.copy(isAppending = true, error = null)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }

            val difficulty = _uiState.value.selectedDifficulty
            val topicsList = _uiState.value.noteTopics.filter { it.isSelected }.map { it.name }
            noteRepository.generateStandaloneQuiz(noteId, count, difficulty, topicsList)
                .onSuccess { response ->
                    val newQuestions = response.questions.map { it.toUiModel() }
                    val currentQuestions = if (isAppending) _uiState.value.questions else emptyList()
                    _uiState.value = _uiState.value.copy(
                        questions = currentQuestions + newQuestions,
                        currentQuizId = response.id,
                        isAppending = false,
                        isLoading = false,
                        isDashboardMode = false
                    )
                    saveQuiz()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAppending = false,
                        error = pe.khipuai.app.core.network.NetworkErrorMapper.from(e).message
                    )
                }
        }
    }

    fun saveQuiz() {
        if (noteId.isBlank() || _uiState.value.questions.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val dto = StandaloneQuizResponse(
                id = _uiState.value.currentQuizId,
                questions = _uiState.value.questions.map { it.toDto() },
                topics = _uiState.value.noteTopics.filter { it.isSelected }.map { it.name },
                difficulty = _uiState.value.selectedDifficulty,
                score = _uiState.value.score
            )
            noteRepository.saveQuiz(noteId, dto)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        isSavedSuccessfully = true
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = pe.khipuai.app.core.network.NetworkErrorMapper.from(e).message
                    )
                }
        }
    }

    fun updateQuestion(updatedQuestion: QuestionUiModel) {
        val updatedQuestions = _uiState.value.questions.map {
            if (it.id == updatedQuestion.id) updatedQuestion else it
        }
        _uiState.value = _uiState.value.copy(questions = updatedQuestions)
    }

    fun deleteQuestion(questionId: String) {
        val updatedQuestions = _uiState.value.questions.filterNot { it.id == questionId }
        _uiState.value = _uiState.value.copy(questions = updatedQuestions)
    }

    fun toggleAddQuestionsDialog(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isAddQuestionsDialogOpen = isOpen)
    }

    fun updateSliderValue(value: Float) {
        _uiState.value = _uiState.value.copy(sliderQuestionsCount = value)
    }

    fun updateDifficulty(difficulty: String) {
        _uiState.value = _uiState.value.copy(selectedDifficulty = difficulty)
    }

    fun toggleTopicSelection(topicName: String) {
        val updatedTopics = _uiState.value.noteTopics.map {
            if (it.name == topicName) it.copy(isSelected = !it.isSelected) else it
        }
        _uiState.value = _uiState.value.copy(noteTopics = updatedTopics)
    }
    
    fun selectOption(questionId: String, optionIndex: Int) {
        if (_uiState.value.isSubmitted) return
        val currentQuestions = _uiState.value.questions.toMutableList()
        val index = currentQuestions.indexOfFirst { it.id == questionId }
        if (index != -1) {
            currentQuestions[index] = currentQuestions[index].copy(selectedOptionIndex = optionIndex)
            _uiState.value = _uiState.value.copy(questions = currentQuestions)
        }
    }

    fun submitQuiz() {
        val questions = _uiState.value.questions
        val score = questions.count { it.selectedOptionIndex == it.correctOptionIndex }
        _uiState.value = _uiState.value.copy(
            isSubmitted = true,
            score = score

        )
        // Auto save on submit
        saveQuiz()
    }

    fun resetQuiz() {
        val resetQuestions = _uiState.value.questions.map { it.copy(selectedOptionIndex = null) }
        _uiState.value = _uiState.value.copy(
            isSubmitted = false,
            score = 0,
            questions = resetQuestions
        )
    }

    fun generateNewQuiz() {
        // Genera un quiz completamente nuevo descartando el actual
        _uiState.value = _uiState.value.copy(isSubmitted = false, score = 0)
        val count = _uiState.value.sliderQuestionsCount.toInt()
        generateQuiz(count = count, isAppending = false)
    }
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun StandaloneQuestionDto.toUiModel() = QuestionUiModel(
        id = id,
        question = question,
        options = options,
        correctOptionIndex = correctIndex,
        explanation = explanation
    )

    private fun QuestionUiModel.toDto() = StandaloneQuestionDto(
        id = id,
        question = question,
        options = options,
        correctIndex = correctOptionIndex,
        explanation = explanation
    )
}

package pe.khipuai.app.ui.screens.exam

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.khipuai.app.core.network.NetworkErrorMapper
import pe.khipuai.app.data.remote.dto.*
import pe.khipuai.app.data.repository.ExamRepository
import pe.khipuai.app.data.repository.PlannerRepository
import javax.inject.Inject

data class ExamUiState(
    val step: ExamStep = ExamStep.CONFIG,
    val courseId: String = "",
    val courseName: String = "",
    val questionCount: Int = 10,
    val durationMinutes: Int = 15,
    val difficulty: String = "mixed",
    val isLoading: Boolean = false,
    val error: String? = null,
    // Exam in progress
    val examId: String? = null,
    val questions: List<ExamQuestionResponse> = emptyList(),
    val currentIndex: Int = 0,
    val answers: MutableMap<String, Int> = mutableMapOf(),
    val remainingSeconds: Int = 0,
    // Results
    val result: ExamResultResponse? = null,
)

enum class ExamStep { CONFIG, EXAM, RESULTS }

@HiltViewModel
class ExamViewModel @Inject constructor(
    private val examRepository: ExamRepository,
    private val plannerRepository: PlannerRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(ExamUiState())
    val state: StateFlow<ExamUiState> = _state.asStateFlow()
    private var timerJob: Job? = null

    init {
        val courseId = savedStateHandle.get<String>("courseId") ?: ""
        val courseName = savedStateHandle.get<String>("courseName") ?: ""
        _state.value = _state.value.copy(courseId = courseId, courseName = courseName)
    }

    fun updateQuestionCount(n: Int) { _state.value = _state.value.copy(questionCount = n) }
    fun updateDuration(m: Int) { _state.value = _state.value.copy(durationMinutes = m) }
    fun updateDifficulty(d: String) { _state.value = _state.value.copy(difficulty = d) }

    fun generateExam() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val req = ExamGenerateRequest(
                questionCount = _state.value.questionCount,
                durationMinutes = _state.value.durationMinutes,
                difficulty = _state.value.difficulty,
            )
            examRepository.generateExam(_state.value.courseId, req)
                .onSuccess { resp ->
                    _state.value = _state.value.copy(
                        isLoading = false, step = ExamStep.EXAM,
                        examId = resp.examId, questions = resp.questions,
                        currentIndex = 0, answers = mutableMapOf(),
                        remainingSeconds = resp.durationMinutes * 60,
                    )
                    startTimer()
                }
                .onFailure { e ->
                    val errorMessage = NetworkErrorMapper.from(e).message
                    _state.value = _state.value.copy(isLoading = false, error = errorMessage)
                }
        }
    }

    fun selectAnswer(questionIndex: Int, optionIndex: Int) {
        val q = _state.value.questions.getOrNull(questionIndex) ?: return
        _state.value.answers[q.id] = optionIndex
    }

    fun goToQuestion(index: Int) {
        if (index in _state.value.questions.indices)
            _state.value = _state.value.copy(currentIndex = index)
    }

    fun finishExam() {
        timerJob?.cancel()
        submitAnswers()
    }

    fun cancelExam() {
        timerJob?.cancel()
        backToConfig()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_state.value.remainingSeconds > 0) {
                delay(1000)
                _state.value = _state.value.copy(remainingSeconds = _state.value.remainingSeconds - 1)
            }
            // Time's up!
            submitAnswers()
        }
    }

    private fun submitAnswers() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val answers = _state.value.questions.map { q ->
                ExamAnswer(q.id, _state.value.answers[q.id] ?: -1)
            }
            examRepository.submitExam(_state.value.examId ?: "", answers)
                .onSuccess { result ->
                    _state.value = _state.value.copy(isLoading = false, step = ExamStep.RESULTS, result = result)
                    // Sumar 10 minutos por simulacro completado
                    plannerRepository.recordStudySession(10)
                }
                .onFailure { e ->
                    val errorMessage = NetworkErrorMapper.from(e).message
                    _state.value = _state.value.copy(isLoading = false, error = errorMessage)
                }
        }
    }

    fun backToConfig() {
        timerJob?.cancel()
        _state.value = _state.value.copy(step = ExamStep.CONFIG, examId = null, questions = emptyList(), result = null, remainingSeconds = 0)
    }
}

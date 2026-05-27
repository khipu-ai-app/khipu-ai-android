package pe.khipuai.app.ui.screens.quiz

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

class QuizCreationViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(QuizCreationUiState())
    val uiState: StateFlow<QuizCreationUiState> = _uiState.asStateFlow()

    init {
        loadMockQuestions()
    }

    private fun loadMockQuestions() {
        _uiState.value = _uiState.value.copy(
            questions = listOf(
                QuestionUiModel(
                    id = 1,
                    question = "¿Quién es considerado el padre del psicoanálisis?",
                    options = listOf("A. B.F. Skinner", "B. Sigmund Freud", "C. Carl Jung", "D. Ivan Pavlov"),
                    selectedOptionIndex = 1
                ),
                QuestionUiModel(
                    id = 2,
                    question = "¿Qué perspectiva psicológica se enfoca en el comportamiento observable en lugar de los procesos mentales internos?",
                    options = listOf("A. Cognitivismo", "B. Psicoanálisis", "C. Conductismo", "D. Humanismo"),
                    selectedOptionIndex = 2
                ),
                QuestionUiModel(
                    id = 3,
                    question = "¿Cuál es la función principal de la amígdala en el cerebro humano?",
                    options = listOf(
                        "A. Procesamiento de emociones, especialmente el miedo.",
                        "B. Control motor fino y equilibrio.",
                        "C. Regulación del ciclo sueño-vigilia.",
                        "D. Procesamiento del lenguaje hablado."
                    ),
                    selectedOptionIndex = 0
                )
            )
        )
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
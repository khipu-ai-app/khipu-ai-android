package pe.khipuai.app.ui.screens.analysis

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class AnalysisUiState(
    val title: String = "Introducción a la Psicología",
    val course: String = "Psicología",
    val summary: String = "El texto describe los principios fundamentales de la psicología de la Gestalt, enfocándose en cómo el cerebro humano organiza las percepciones visuales en patrones unificados y complejos, en lugar de elementos aislados. Se destaca la \"Ley del Cierre\" y la \"Figura-Fondo\".",
    val keyConcepts: List<String> = listOf("Gestalt", "Percepción", "Ley del Cierre", "Figura-Fondo"),
    val difficultyLevel: String = "Intermedio",
    val difficultyProgress: Float = 0.6f,
    val aiSuggestion: String = "He notado que el texto profundiza en la Ley de Gestalt. ¿Quieres que te explique este concepto con ejemplos prácticos y cómo aplicarlo a tu diseño de estudios?",
    val isLoading: Boolean = false
)

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    // TODO: Inject AnalysisRepository when implemented
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()
    
    // TODO: Implement methods for:
    // - generateStudyGuide()
    // - createQuestions()
    // - addToCalendar()
    // - explainConcept()
}
package pe.khipuai.app.ui.screens.studyguide

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class StudyGuideViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(StudyGuideUiState())
    val uiState: StateFlow<StudyGuideUiState> = _uiState.asStateFlow()
    
}

data class StudyGuideUiState(
    val title: String = "Redes Neuronales Profundas",
    val date: String = "3 de Junio",
    val executiveSummary: String = """
        Las redes neuronales profundas son modelos de aprendizaje automático inspirados en el cerebro humano. 
        Utilizan múltiples capas de neuronas artificiales para procesar información y aprender patrones complejos. 
        Son fundamentales en aplicaciones de visión por computadora, procesamiento de lenguaje natural y más.
    """.trimIndent(),
    val glossary: String = """
        • Neurona Artificial: Unidad básica de procesamiento que recibe entradas, las procesa y genera una salida.
        • Función de Activación: Función matemática que determina la salida de una neurona (ReLU, Sigmoid, Tanh).
        • Backpropagation: Algoritmo para entrenar redes neuronales ajustando los pesos basándose en el error.
        • Learning Rate: Parámetro que controla qué tan rápido aprende el modelo durante el entrenamiento.
        • Overfitting: Cuando el modelo aprende demasiado bien los datos de entrenamiento pero falla en datos nuevos.
    """.trimIndent()
)

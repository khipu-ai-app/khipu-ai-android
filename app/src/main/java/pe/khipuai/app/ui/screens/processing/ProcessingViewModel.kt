package pe.khipuai.app.ui.screens.processing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProcessingUiState(
    val progress: Float = 0f,
    val currentStep: ProcessingStep = ProcessingStep.IMAGE_OPTIMIZATION,
    val imageOptimized: Boolean = false,
    val ocrCompleted: Boolean = false,
    val classificationCompleted: Boolean = false,
    val summaryGenerated: Boolean = false,
    val detectedCourse: String = "Física",
    val studyTip: String = "Revisar tus apuntes clasificados por Khipu durante 15 minutos al día mejora la retención a largo plazo en un 40%.",
    val isComplete: Boolean = false
)

enum class ProcessingStep {
    IMAGE_OPTIMIZATION,
    OCR,
    CLASSIFICATION,
    SUMMARY,
    COMPLETE
}

@HiltViewModel
class ProcessingViewModel @Inject constructor(
    // TODO: Inject ProcessingRepository when implemented
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProcessingUiState())
    val uiState: StateFlow<ProcessingUiState> = _uiState.asStateFlow()
    
    init {
        startProcessing()
    }
    
    private fun startProcessing() {
        viewModelScope.launch {
            // Step 1: Image Optimization (0-25%)
            _uiState.value = _uiState.value.copy(
                currentStep = ProcessingStep.IMAGE_OPTIMIZATION
            )
            animateProgress(0f, 0.25f, 1500)
            _uiState.value = _uiState.value.copy(imageOptimized = true)
            delay(300)
            
            // Step 2: OCR (25-50%)
            _uiState.value = _uiState.value.copy(
                currentStep = ProcessingStep.OCR
            )
            animateProgress(0.25f, 0.50f, 2000)
            _uiState.value = _uiState.value.copy(ocrCompleted = true)
            delay(300)
            
            // Step 3: Classification (50-75%)
            _uiState.value = _uiState.value.copy(
                currentStep = ProcessingStep.CLASSIFICATION
            )
            animateProgress(0.50f, 0.75f, 1500)
            _uiState.value = _uiState.value.copy(classificationCompleted = true)
            delay(300)
            
            // Step 4: Summary Generation (75-100%)
            _uiState.value = _uiState.value.copy(
                currentStep = ProcessingStep.SUMMARY
            )
            animateProgress(0.75f, 1.0f, 2000)
            _uiState.value = _uiState.value.copy(
                summaryGenerated = true,
                currentStep = ProcessingStep.COMPLETE,
                isComplete = true
            )
        }
    }
    
    private suspend fun animateProgress(from: Float, to: Float, durationMs: Long) {
        val steps = 20
        val stepDuration = durationMs / steps
        val increment = (to - from) / steps
        
        repeat(steps) {
            delay(stepDuration)
            _uiState.value = _uiState.value.copy(
                progress = from + (increment * (it + 1))
            )
        }
    }
}
package pe.khipuai.app.ui.screens.processing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.khipuai.app.data.repository.UploadRepository
import javax.inject.Inject

data class ProcessingUiState(
    val progress: Float = 0f,
    val currentStep: ProcessingStep = ProcessingStep.IMAGE_OPTIMIZATION,
    val imageOptimized: Boolean = false,
    val ocrCompleted: Boolean = false,
    val classificationCompleted: Boolean = false,
    val summaryGenerated: Boolean = false,
    val detectedCourse: String = "Detectando...",
    val studyTip: String = "Revisar tus apuntes clasificados por Khipu durante 15 minutos al día mejora la retención a largo plazo en un 40%.",
    val isComplete: Boolean = false,
    val errorMessage: String? = null,
    val isError: Boolean = false,
    val noteId: String? = null
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
    private val uploadRepository: UploadRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Extraemos el uploadId enviado por la pantalla de captura
    private val uploadId: String? = savedStateHandle["uploadId"]

    private val _uiState = MutableStateFlow(ProcessingUiState())
    val uiState: StateFlow<ProcessingUiState> = _uiState.asStateFlow()

    init {
        if (uploadId != null) {
            startRealBackendPolling(uploadId)
        } else {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Error crítico: No se encontró el identificador de carga.",
                isError = true
            )
        }
    }

    private fun startRealBackendPolling(id: String) {
        viewModelScope.launch {
            try {
                var isProcessingFinished = false
                var pollingAttempts = 0

                while (!isProcessingFinished && pollingAttempts < 30) { // Límite de 1 minuto de resiliencia
                    delay(2000) // Consultamos al backend cada 2 segundos
                    pollingAttempts++

                    val result = uploadRepository.checkProcessingStatus(id)

                    result.onSuccess { statusDto ->
                        val step = statusDto.pipelineStep ?: "pending"

                        when (step) {
                            "optimizing" -> {
                                _uiState.value = _uiState.value.copy(
                                    progress = 0.25f,
                                    currentStep = ProcessingStep.IMAGE_OPTIMIZATION,
                                    imageOptimized = true
                                )
                            }
                            "ocr_processing" -> {
                                _uiState.value = _uiState.value.copy(
                                    progress = 0.50f,
                                    currentStep = ProcessingStep.OCR,
                                    imageOptimized = true,
                                    ocrCompleted = true
                                )
                            }
                            "classifying" -> {
                                _uiState.value = _uiState.value.copy(
                                    progress = 0.75f,
                                    currentStep = ProcessingStep.CLASSIFICATION,
                                    imageOptimized = true,
                                    ocrCompleted = true,
                                    classificationCompleted = true,
                                    detectedCourse = "Clasificando..."
                                )
                            }
                            "summarizing" -> {
                                _uiState.value = _uiState.value.copy(
                                    progress = 0.90f,
                                    currentStep = ProcessingStep.SUMMARY,
                                    imageOptimized = true,
                                    ocrCompleted = true,
                                    classificationCompleted = true,
                                    summaryGenerated = true,
                                    detectedCourse = "Analizando..."
                                )
                            }
                            "completed" -> {
                                isProcessingFinished = true
                                _uiState.value = _uiState.value.copy(
                                    progress = 1.0f,
                                    currentStep = ProcessingStep.COMPLETE,
                                    imageOptimized = true,
                                    ocrCompleted = true,
                                    classificationCompleted = true,
                                    summaryGenerated = true,
                                    isComplete = true,
                                    noteId = statusDto.noteId,
                                    detectedCourse = "Completado con éxito"
                                )
                            }
                            "partial_failure" -> {
                                isProcessingFinished = true
                                _uiState.value = _uiState.value.copy(
                                    progress = 1.0f,
                                    currentStep = ProcessingStep.COMPLETE,
                                    imageOptimized = true,
                                    ocrCompleted = true,
                                    classificationCompleted = true,
                                    summaryGenerated = true,
                                    isComplete = true,
                                    noteId = statusDto.noteId,
                                    detectedCourse = "Completado con advertencias",
                                    errorMessage = "Nota procesada. Sin embargo, falló la indexación en el grafo conceptual."
                                )
                            }
                            "failed" -> {
                                isProcessingFinished = true
                                _uiState.value = _uiState.value.copy(
                                    currentStep = ProcessingStep.COMPLETE,
                                    errorMessage = "Fallo total en pipeline: la IA no pudo extraer la información.",
                                    isError = true
                                )
                            }
                        }
                    }.onFailure {
                        _uiState.value = _uiState.value.copy(detectedCourse = "Reconectando...")
                    }
                }

                if (!isProcessingFinished) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Tiempo de espera agotado. Verifica el estado en tu pantalla de inicio.",
                        isError = true
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Fallo en el hilo del procesador: ${e.message}", isError = true)
            }
        }
    }

}
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
                errorMessage = "Error crítico: No se encontró el identificador de carga."
            )
        }
    }

    private fun startRealBackendPolling(id: String) {
        viewModelScope.launch {
            try {
                // Fase 1: Optimización Visual (Progreso visual inicial)
                _uiState.value = _uiState.value.copy(currentStep = ProcessingStep.IMAGE_OPTIMIZATION)
                animateFakeProgress(0f, 0.25f, 1000)
                _uiState.value = _uiState.value.copy(imageOptimized = true, currentStep = ProcessingStep.OCR)

                // Fase 2: Iniciar bucle de consulta activa al servidor Docker
                var isProcessingFinished = false
                var pollingAttempts = 0

                while (!isProcessingFinished && pollingAttempts < 30) { // Límite de 1 minuto de resiliencia
                    delay(2000) // Consultamos al backend cada 2 segundos para no saturar el socket
                    pollingAttempts++

                    val result = uploadRepository.checkProcessingStatus(id)

                    result.onSuccess { statusDto ->
                        when (statusDto.status) {
                            "processing" -> {
                                // Avanzamos el OCR de forma fluida mientras el worker trabaja en segundo plano
                                if (!_uiState.value.ocrCompleted && _uiState.value.progress < 0.50f) {
                                    animateFakeProgress(_uiState.value.progress, 0.50f, 800)
                                    _uiState.value = _uiState.value.copy(ocrCompleted = true, currentStep = ProcessingStep.CLASSIFICATION)
                                }
                            }
                            "completed" -> {
                                isProcessingFinished = true

                                // Forzamos la activación secuencial de los checks para mantener el impacto visual
                                if (!_uiState.value.ocrCompleted) _uiState.value = _uiState.value.copy(ocrCompleted = true)

                                _uiState.value = _uiState.value.copy(
                                    classificationCompleted = true,
                                    currentStep = ProcessingStep.SUMMARY,
                                    detectedCourse = "Verificado con éxito"
                                )

                                // Fase final: Completar la barra al 100% e inyectar el noteId
                                animateFakeProgress(_uiState.value.progress, 1.0f, 1200)

                                _uiState.value = _uiState.value.copy(
                                    summaryGenerated = true,
                                    currentStep = ProcessingStep.COMPLETE,
                                    isComplete = true,
                                    noteId = statusDto.noteId
                                )
                            }
                            "failed" -> {
                                isProcessingFinished = true
                                _uiState.value = _uiState.value.copy(
                                    errorMessage = "La Inteligencia Artificial no pudo procesar este documento de manera legible."
                                )
                            }
                        }
                    }.onFailure {
                        // Si hay un micro-corte de red local, mantenemos el bucle vivo esperando reconexión
                        _uiState.value = _uiState.value.copy(detectedCourse = "Reconectando...")
                    }
                }

                if (!isProcessingFinished) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Tiempo de espera agotado. Verifica el estado en tu pantalla de inicio."
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Fallo en el hilo del procesador: ${e.message}")
            }
        }
    }

    private suspend fun animateFakeProgress(from: Float, to: Float, durationMs: Long) {
        val steps = 10
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
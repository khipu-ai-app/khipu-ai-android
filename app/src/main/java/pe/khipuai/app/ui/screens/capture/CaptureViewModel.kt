package pe.khipuai.app.ui.screens.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.khipuai.app.data.repository.AuthRepository
import pe.khipuai.app.data.repository.CourseRepository
import pe.khipuai.app.data.repository.UploadRepository
import java.io.File
import javax.inject.Inject

data class CourseOption(
    val id: String,
    val name: String
)

data class CaptureUiState(
    val selectedDestination: String = "Autoclasificar con IA",
    val selectedDestinationId: String? = null,
    val courses: List<CourseOption> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val captureMode: CaptureMode = CaptureMode.CAMERA,
    val uploadedId: String? = null,
    val capturesUsed: Int = 0,
    val capturesLimit: Int = 5,
    val isPro: Boolean = false
)

enum class CaptureMode {
    CAMERA, UPLOAD, PDF
}

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val uploadRepository: UploadRepository,
    private val courseRepository: CourseRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    private val preselectedCourseId: String? = savedStateHandle.get<String>("preselectedCourseId")

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    // Evento one-shot: cuando el usuario intenta subir y está al límite,
    // emitimos Unit aquí. La Screen lo colecta y navega a Subscription.
    // Usamos un Channel para que el evento se consuma una sola vez y
    // no se reactive en recomposiciones.
    private val _limitReachedEvents = Channel<Unit>(Channel.BUFFERED)
    val limitReachedEvents = _limitReachedEvents.receiveAsFlow()

    init {
        viewModelScope.launch {
            courseRepository.observeAll().collect { localCourses ->
                val activeCourses = localCourses.filter { it.isActive }.map {
                    CourseOption(id = it.id, name = it.name)
                }.sortedBy { it.name }

                var defaultDestName = "Autoclasificar con IA"
                var defaultDestId: String? = null

                if (preselectedCourseId != null) {
                    val preselected = activeCourses.find { it.id == preselectedCourseId }
                    if (preselected != null) {
                        defaultDestName = preselected.name
                        defaultDestId = preselected.id
                    }
                }

                _uiState.value = _uiState.value.copy(
                    courses = activeCourses,
                    selectedDestination = if (_uiState.value.selectedDestinationId == null && _uiState.value.selectedDestination == "Autoclasificar con IA") defaultDestName else _uiState.value.selectedDestination,
                    selectedDestinationId = if (_uiState.value.selectedDestinationId == null && _uiState.value.selectedDestination == "Autoclasificar con IA") defaultDestId else _uiState.value.selectedDestinationId
                )
            }
        }
        viewModelScope.launch {
            courseRepository.fetchMyCourses()
        }
        loadUsage()
    }

    /**
     * T-02: recarga el contador de capturas. Llamar al volver de
     * SubscriptionScreen después de un upgrade para refrescar la UI.
     */
    fun loadUsage() {
        viewModelScope.launch {
            authRepository.fetchUsage()
                .onSuccess { usage ->
                    _uiState.update {
                        it.copy(
                            capturesUsed = usage.capturesUsed,
                            capturesLimit = usage.capturesLimit,
                            isPro = usage.isPro
                        )
                    }
                }
                // Si falla, mantenemos los valores por defecto (no bloqueamos
                // la pantalla por un error de red en la carga de usage)
        }
    }

    fun updateDestination(destinationName: String, destinationId: String?) {
        _uiState.value = _uiState.value.copy(
            selectedDestination = destinationName,
            selectedDestinationId = destinationId
        )
    }

    // Procesa el envío de archivos de imagen capturados por la cámara del celular
    fun processAndUploadImage(file: File, onResult: (String?) -> Unit) {
        // T-02: chequeo local ANTES de gastar batería subiendo el archivo.
        // La fuente de verdad es el backend (que retorna 402 si está al
        // límite), pero validamos acá para evitar el viaje redondo.
        if (isAtLimit()) {
            _limitReachedEvents.trySend(Unit)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                captureMode = CaptureMode.CAMERA
            )

            val currentCourseId = _uiState.value.selectedDestinationId
            val result = uploadRepository.uploadFile(file, mimeType = "image/jpeg", courseId = currentCourseId)

            result.onSuccess { response ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    uploadedId = response.id
                )
                // Refrescar usage después de cada upload exitoso
                loadUsage()
                onResult(response.id)
            }.onFailure { exception ->
                // Si el backend retorna 402 (Payment Required) por el límite,
                // también disparamos el evento de paywall en vez de mostrar
                // un error genérico.
                if (isPaymentRequiredError(exception)) {
                    loadUsage() // sincronizar contador
                    _limitReachedEvents.trySend(Unit)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = pe.khipuai.app.core.network.NetworkErrorMapper.from(exception).message
                    )
                }
                onResult(null)
            }
        }
    }

    // Procesa la carga de archivos locales (PDFs o Imágenes de la Galería)
    fun processAndUploadDocument(file: File, mimeType: String, onResult: (String?) -> Unit) {
        // T-02: idem al caso de cámara
        if (isAtLimit()) {
            _limitReachedEvents.trySend(Unit)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                captureMode = CaptureMode.UPLOAD
            )

            val currentCourseId = _uiState.value.selectedDestinationId
            val result = uploadRepository.uploadFile(file, mimeType, courseId = currentCourseId)

            result.onSuccess { response ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    uploadedId = response.id
                )
                loadUsage()
                onResult(response.id)
            }.onFailure { exception ->
                if (isPaymentRequiredError(exception)) {
                    loadUsage()
                    _limitReachedEvents.trySend(Unit)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = pe.khipuai.app.core.network.NetworkErrorMapper.from(exception).message
                    )
                }
                onResult(null)
            }
        }
    }

    private fun isAtLimit(): Boolean {
        val s = _uiState.value
        return !s.isPro && s.capturesLimit > 0 && s.capturesUsed >= s.capturesLimit
    }

    private fun isPaymentRequiredError(e: Throwable): Boolean {
        return e is retrofit2.HttpException && e.code() == 402
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

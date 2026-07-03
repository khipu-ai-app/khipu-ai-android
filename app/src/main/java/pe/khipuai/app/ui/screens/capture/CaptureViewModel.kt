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
import pe.khipuai.app.core.network.DuplicateNoteException
import pe.khipuai.app.core.network.NetworkErrorMapper
import pe.khipuai.app.data.remote.dto.DuplicateNoteInfo
import pe.khipuai.app.data.repository.AuthRepository
import pe.khipuai.app.data.repository.CourseRepository
import pe.khipuai.app.data.repository.UploadRepository
import retrofit2.HttpException
import java.io.File
import javax.inject.Inject

data class CourseOption(
    val id: String,
    val name: String
)

internal sealed class UploadOutcome {
    data class Success(val uploadId: String) : UploadOutcome()
    data object LimitReached : UploadOutcome()
    data class Duplicate(val exception: DuplicateNoteException) : UploadOutcome()
    data class GenericError(val message: String) : UploadOutcome()
}

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
    val isPro: Boolean = false,
    val combineMode: Boolean = false,
    val pendingFiles: List<File> = emptyList(),
    val pendingFileCount: Int = 0,
    val duplicateDialog: DuplicateDialogState? = null,
)

data class DuplicateDialogState(
    val info: DuplicateNoteInfo,
    val file: File,
    val mimeType: String,
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

    private val _limitReachedEvents = Channel<Unit>(Channel.BUFFERED)
    val limitReachedEvents = _limitReachedEvents.receiveAsFlow()

    private val _uploadedEvents = Channel<String>(Channel.BUFFERED)
    val uploadedEvents = _uploadedEvents.receiveAsFlow()

    private val _combineUploadedEvents = Channel<String>(Channel.BUFFERED)
    val combineUploadedEvents = _combineUploadedEvents.receiveAsFlow()

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
                    selectedDestinationId = if (_uiState.value.selectedDestinationId == null && _uiState.value.selectedDestination == "Autoclasificar con IA") defaultDestId else _uiState.value.selectedDestinationId,
                )
            }
        }
        viewModelScope.launch {
            courseRepository.fetchMyCourses()
        }
        loadUsage()
    }

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
        }
    }

    fun updateDestination(destinationName: String, destinationId: String?) {
        _uiState.value = _uiState.value.copy(
            selectedDestination = destinationName,
            selectedDestinationId = destinationId
        )
    }

    fun processAndUploadImage(file: File, onResult: (String?) -> Unit) {
        uploadInternal(file, "image/jpeg", CaptureMode.CAMERA, forceUpload = false, onResult)
    }

    fun processAndUploadDocument(file: File, mimeType: String, onResult: (String?) -> Unit) {
        uploadInternal(file, mimeType, CaptureMode.UPLOAD, forceUpload = false, onResult)
    }

    fun forceUploadDuplicate() {
        val dialog = _uiState.value.duplicateDialog ?: return
        _uiState.value = _uiState.value.copy(duplicateDialog = null)
        uploadInternal(dialog.file, dialog.mimeType, _uiState.value.captureMode, forceUpload = true) {}
    }

    fun dismissDuplicateDialog() {
        _uiState.value = _uiState.value.copy(duplicateDialog = null)
    }

    // ─── Combine ──────────────────────────────────────────────────────────

    fun toggleCombineMode() {
        val newMode = !_uiState.value.combineMode
        _uiState.value = _uiState.value.copy(
            combineMode = newMode,
            pendingFiles = if (!newMode) emptyList() else _uiState.value.pendingFiles,
            pendingFileCount = if (!newMode) 0 else _uiState.value.pendingFileCount,
        )
    }

    fun addFileToCombineBuffer(file: File) {
        val current = _uiState.value
        _uiState.value = current.copy(
            pendingFiles = current.pendingFiles + file,
            pendingFileCount = current.pendingFileCount + 1,
        )
    }

    fun combineAndUpload() {
        val files = _uiState.value.pendingFiles
        if (files.size < 2) {
            _uiState.value = _uiState.value.copy(errorMessage = "Necesitas al menos 2 archivos.")
            return
        }
        if (isAtLimit()) {
            _limitReachedEvents.trySend(Unit)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val mimeTypes = List(files.size) { "image/jpeg" }
            val courseId = _uiState.value.selectedDestinationId
            uploadRepository.combineFiles(files, mimeTypes, courseId)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false, pendingFiles = emptyList(), pendingFileCount = 0,
                    )
                    loadUsage()
                    _combineUploadedEvents.trySend(response.noteId)
                }
                .onFailure { e ->
                    val msg = when {
                        isPaymentRequiredError(e) -> { loadUsage(); _limitReachedEvents.trySend(Unit); return@launch }
                        NetworkErrorMapper.parseDuplicate(e) != null -> "'${NetworkErrorMapper.parseDuplicate(e)!!.info.existingNoteTitle}' ya existe."
                        else -> NetworkErrorMapper.from(e).message
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = msg)
                }
        }
    }

    // ─── Upload interno (single) ──────────────────────────────────────────

    private fun uploadInternal(file: File, mimeType: String, captureMode: CaptureMode, forceUpload: Boolean, onResult: (String?) -> Unit) {
        if (isAtLimit()) { _limitReachedEvents.trySend(Unit); return }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, captureMode = captureMode)
            val result = uploadRepository.uploadFile(file, mimeType, _uiState.value.selectedDestinationId, forceUpload)
            val outcome = result.fold(onSuccess = { UploadOutcome.Success(it.id) }, onFailure = { classifyUploadError(it) })
            applyOutcome(outcome, file, mimeType, onResult)
        }
    }

    internal fun classifyUploadError(exception: Throwable): UploadOutcome {
        if (isPaymentRequiredError(exception)) return UploadOutcome.LimitReached
        val duplicate = NetworkErrorMapper.parseDuplicate(exception)
        if (duplicate != null) return UploadOutcome.Duplicate(duplicate)
        return UploadOutcome.GenericError(NetworkErrorMapper.from(exception).message)
    }

    private fun applyOutcome(outcome: UploadOutcome, file: File, mimeType: String, onResult: (String?) -> Unit) {
        when (outcome) {
            is UploadOutcome.Success -> {
                _uiState.value = _uiState.value.copy(isLoading = false, uploadedId = outcome.uploadId, duplicateDialog = null)
                loadUsage(); _uploadedEvents.trySend(outcome.uploadId); onResult(outcome.uploadId)
            }
            is UploadOutcome.LimitReached -> {
                loadUsage(); _limitReachedEvents.trySend(Unit)
                _uiState.value = _uiState.value.copy(isLoading = false); onResult(null)
            }
            is UploadOutcome.Duplicate -> {
                _uiState.value = _uiState.value.copy(isLoading = false, duplicateDialog = DuplicateDialogState(outcome.exception.info, file, mimeType))
                onResult(null)
            }
            is UploadOutcome.GenericError -> {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = outcome.message)
                onResult(null)
            }
        }
    }

    private fun isAtLimit(): Boolean {
        val s = _uiState.value
        return !s.isPro && s.capturesLimit > 0 && s.capturesUsed >= s.capturesLimit
    }

    private fun isPaymentRequiredError(e: Throwable): Boolean = e is HttpException && e.code() == 402

    fun clearError() { _uiState.value = _uiState.value.copy(errorMessage = null) }
}

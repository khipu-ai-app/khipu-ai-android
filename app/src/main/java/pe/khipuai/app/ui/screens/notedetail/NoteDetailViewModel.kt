package pe.khipuai.app.ui.screens.notedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.khipuai.app.data.repository.NoteRepository
import pe.khipuai.app.data.remote.dto.NoteResponse
import javax.inject.Inject

enum class HistoryItemType { REPASO_COMPLETADO, NOTA_CREADA }

data class HistoryItemUiModel(
    val id: String,
    val title: String,
    val description: String,
    val type: HistoryItemType
)

/**
 * T-06: Una sesión de repasos agrupada por fecha.
 *
 * El backend devuelve sesiones ya agrupadas (T-06: el endpoint agrupa en
 * el server). Esta UiModel es 1-a-1 con la respuesta del DTO.
 */
data class ReviewSessionUiModel(
    val sessionKey: String,           // YYYY-MM-DD — clave de agrupación
    val date: String,                  // formato "15 Jun, 2026"
    val averageRating: Double,         // 0.0 - 5.0
    val conceptsReviewed: Int,         // cantidad de items
    val nextReviewDate: String?,       // próxima fecha de repaso (la más lejana)
    val concepts: List<ReviewConceptUiModel>
) {
    /** Verde si promedio >= 3, naranja si < 3. Umbral del plan T-06. */
    val isPositive: Boolean get() = averageRating >= 3.0
}

data class ReviewConceptUiModel(
    val name: String,
    val rating: Int,
    val nextReviewDate: String?
)

data class NoteDetailUiState(
    val noteId: String = "",
    val uploadId: String = "",
    val title: String = "Cargando nota...",
    val capturedDate: String = "",
    val courseId: String? = null,
    val courseName: String = "",
    // T-16: hex del color del curso (formato "#RRGGBB"). Se usa para
    // teñir el dot en los metadatos y el acento lateral del Card de
    // "Resumen Ejecutivo Khipu". Null si la nota no tiene curso o el
    // curso no tiene color.
    val courseColorHex: String? = null,
    val aiSummary: String = "",
    val keyConcepts: List<String> = emptyList(),
    val historyTimeline: List<HistoryItemUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val availableCourses: List<pe.khipuai.app.data.local.entity.CourseEntity> = emptyList(),
    // Grafo local

    val snackbarMessage: String? = null,
    // T-06: historial de repasos por nota
    val reviewSessions: List<ReviewSessionUiModel> = emptyList(),
    val isReviewHistoryLoading: Boolean = false,
    val reviewHistoryError: String? = null,
    // T-13 evolution: archivos adjuntos a la nota. Vacío mientras
    // se carga. Se actualiza también cuando el usuario agrega
    // archivos desde la pantalla.
    val files: List<pe.khipuai.app.data.remote.dto.NoteFileResponse> = emptyList(),
    val isFilesLoading: Boolean = false,
)

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val noteRepository: pe.khipuai.app.data.repository.NoteRepository,
    private val offlineFirstNoteRepository: pe.khipuai.app.data.repository.OfflineFirstNoteRepository,
    private val courseRepository: pe.khipuai.app.data.repository.CourseRepository,
    private val plannerRepository: pe.khipuai.app.data.repository.PlannerRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val noteId: String = checkNotNull(savedStateHandle["noteId"])

    private val _uiState = MutableStateFlow(NoteDetailUiState(isLoading = true, noteId = noteId))
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()

    private fun loadNoteDetails() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            // Cargar cursos disponibles para el diálogo de reasociación
            launch {
                courseRepository.observeAll().collect { courses ->
                    val current = _uiState.value
                    // T-16: re-resolver el color del curso si la nota ya
                    // cargó pero los cursos aún no estaban en cache
                    // (orden de carga: el Flow puede emitir después de
                    // getNoteDetail). Si el `courseId` ya está seteado y
                    // el color aún no, lo copiamos.
                    val resolvedColor = current.courseColorHex
                        ?: current.courseId?.let { id ->
                            courses.firstOrNull { it.id == id }?.color
                        }
                    _uiState.value = current.copy(
                        availableCourses = courses,
                        courseColorHex = resolvedColor,
                    )
                }
            }

            // Sync de cursos en red de forma pasiva
            launch {
                try {
                    courseRepository.fetchMyCourses()
                } catch (_: Exception) {}
            }

            noteRepository.getNoteDetail(noteId)
                .onSuccess { detail ->
                    val timeline = mutableListOf(
                        HistoryItemUiModel(
                            id = "nota_creada_${detail.id}",
                            title = "Nota Creada",
                            description = formatDate(detail.createdAt),
                            type = HistoryItemType.NOTA_CREADA
                        )
                    )
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        noteId = detail.id,
                        uploadId = detail.uploadId ?: "",
                        title = detail.title,
                        capturedDate = formatDate(detail.createdAt),
                        courseId = detail.courseId,
                        courseName = detail.courseName ?: "General",
                        // T-16: resolver el color del curso desde la
                        // cache local (Room). Si el curso existe en la
                        // lista observada, copiamos su color; si no, null.
                        courseColorHex = detail.courseId?.let { id ->
                            _uiState.value.availableCourses.firstOrNull { it.id == id }?.color
                        },
                        aiSummary = detail.summary,
                        keyConcepts = detail.topics.map { it.name },
                        historyTimeline = timeline
                    )
                    loadReviewHistory()
                    // T-13 evolution: cargar los archivos de la nota.
                    loadFiles()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = pe.khipuai.app.core.network.NetworkErrorMapper.from(error).message
                    )
                }
        }
    }

    /**
     * T-06: carga el historial de repasos del backend. El endpoint retorna
     * sesiones YA agrupadas por fecha con promedio, conteo y próxima fecha
     * de repaso. Mapeo directo del DTO a la UiModel.
     */
    private fun loadReviewHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isReviewHistoryLoading = true,
                reviewHistoryError = null
            )
            noteRepository.getNoteReviewHistory(noteId)
                .onSuccess { sessions ->
                    _uiState.value = _uiState.value.copy(
                        isReviewHistoryLoading = false,
                        reviewSessions = sessions.map { it.toUiModel() }
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isReviewHistoryLoading = false,
                        reviewHistoryError = pe.khipuai.app.core.network.NetworkErrorMapper.from(e).message
                    )
                }
        }
    }

    fun retryReviewHistory() {
        loadReviewHistory()
    }

    /** DTO → UiModel. */
    private fun pe.khipuai.app.data.remote.dto.ReviewSessionResponseDto.toUiModel() =
        ReviewSessionUiModel(
            sessionKey = sessionKey,
            date = dateLabel,
            averageRating = averageRating,
            conceptsReviewed = conceptsReviewed,
            nextReviewDate = nextReviewDate,
            concepts = concepts.map {
                ReviewConceptUiModel(
                    name = it.conceptName,
                    rating = it.rating,
                    nextReviewDate = it.nextReviewDate
                )
            }
        )

    fun deleteNote(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = offlineFirstNoteRepository.deleteNote(noteId)
            if (result.isSuccess) {
                onSuccess()
            }
        }
    }

    fun renameNote(newTitle: String) {
        viewModelScope.launch {
            val result = offlineFirstNoteRepository.updateNote(noteId, newTitle, _uiState.value.courseId)
            if (result.isSuccess) {
                val response = result.getOrNull()
                if (response != null) {
                    _uiState.value = _uiState.value.copy(title = response.title)
                }
            }
        }
    }

    fun reassociateCourse(newCourseId: String?) {
        viewModelScope.launch {
            val result = offlineFirstNoteRepository.updateNote(noteId, _uiState.value.title, newCourseId)
            if (result.isSuccess) {
                val response = result.getOrNull()
                if (response != null) {
                    val courseName = if (newCourseId == null) {
                        "General"
                    } else {
                        val course = courseRepository.getById(newCourseId)
                        course?.name ?: "General"
                    }
                    _uiState.value = _uiState.value.copy(
                        courseId = response.courseId,
                        courseName = courseName
                    )
                }
            }
        }
    }

    fun scheduleNote(dateStr: String) {
        viewModelScope.launch {
            val result = plannerRepository.createManualSchedule(noteId, dateStr)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(snackbarMessage = "Nota programada para el $dateStr")
            } else {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = pe.khipuai.app.core.network.NetworkErrorMapper.from(result.exceptionOrNull() ?: Exception()).message
                )
            }
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    /**
     * T-13 evolution: carga la lista de archivos adjuntos a esta nota
     * desde el backend. Actualiza `uiState.files`.
     */
    private fun loadFiles() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFilesLoading = true)
            noteRepository.getNoteFiles(noteId)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        isFilesLoading = false,
                        files = response.files
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isFilesLoading = false
                    )
                }
        }
    }

    /** Convierte ISO 8601 (ej: "2024-10-24T12:00:00") → "24 Oct, 2024". */
    private fun formatDate(isoDate: String): String {
        return try {
            val parts = isoDate.take(10).split("-")
            if (parts.size == 3) {
                val monthName = when (parts[1].toInt()) {
                    1 -> "Ene"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Abr"
                    5 -> "May"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Ago"
                    9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; else -> "Dic"
                }
                "${parts[2]} $monthName, ${parts[0]}"
            } else {
                isoDate.take(10)
            }
        } catch (_: Exception) {
            isoDate.take(10)
        }
    }

    init {
        loadNoteDetails()
    }
}

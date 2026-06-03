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
import javax.inject.Inject

enum class HistoryItemType { REPASO_COMPLETADO, NOTA_CREADA }

data class HistoryItemUiModel(
    val id: String,
    val title: String,
    val description: String,
    val type: HistoryItemType
)

data class NoteDetailUiState(
    val noteId: String = "",
    val uploadId: String = "",
    val title: String = "Cargando nota...",
    val capturedDate: String = "",
    val courseName: String = "",
    val aiSummary: String = "",
    val extractedText: String = "",
    val keyConcepts: List<String> = emptyList(),
    val historyTimeline: List<HistoryItemUiModel> = emptyList(),
    val isBookmarked: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val noteId: String = checkNotNull(savedStateHandle["noteId"])

    private val _uiState = MutableStateFlow(NoteDetailUiState(isLoading = true, noteId = noteId))
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()

    private fun loadNoteDetails() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            noteRepository.getNoteDetail(noteId)
                .onSuccess { detail ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        noteId = detail.id,
                        // El id de la carga (PDF original) se usa como referencia para visualizarlo
                        uploadId = detail.uploadId ?: "",
                        title = detail.title,
                        capturedDate = formatDate(detail.createdAt),
                        courseName = detail.courseName ?: "",
                        aiSummary = detail.summary,
                        // El endpoint /v1/notes/{id} no devuelve texto OCR crudo en este sprint
                        extractedText = "",
                        keyConcepts = detail.topics.map { it.name },
                        historyTimeline = listOf(
                            HistoryItemUiModel(
                                id = "nota_creada_${detail.id}",
                                title = "Nota Creada",
                                description = formatDate(detail.createdAt),
                                type = HistoryItemType.NOTA_CREADA
                            )
                        )
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Error al cargar la nota"
                    )
                }
        }
    }

    fun toggleBookmark() {
        _uiState.value = _uiState.value.copy(isBookmarked = !_uiState.value.isBookmarked)
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

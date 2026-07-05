package pe.khipuai.app.ui.screens.analysis

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pe.khipuai.app.data.remote.dto.ConceptNodeDto
import pe.khipuai.app.data.repository.NoteRepository
import javax.inject.Inject

data class AnalysisUiState(
    val isLoading: Boolean = false,
    val title: String = "",
    val summary: String = "",
    val topics: List<ConceptNodeDto> = emptyList(),
    val keyConcepts: List<String> = emptyList(),
    val difficultyLevel: String = "Intermedio",
    val difficultyProgress: Float = 0.5f,
    val errorMessage: String? = null,
    val d3NodesJson: String = "[]",
    val d3EdgesJson: String = "[]",
    val snackbarMessage: String? = null,
)

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val noteId: String? = savedStateHandle["noteId"]

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = pe.khipuai.app.core.network.NetworkErrorMapper.from(exception).message
        )
    }

    private val _uiState = MutableStateFlow(AnalysisUiState(isLoading = true))
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    fun loadNoteDetail() {
        val id = noteId ?: return

        viewModelScope.launch(exceptionHandler) {
            _uiState.value = _uiState.value.copy(isLoading = true)

            noteRepository.getNoteDetail(id)
                .onSuccess { detail ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        title = detail.title,
                        summary = detail.summary,
                        topics = detail.topics,
                        keyConcepts = detail.topics.map { it.name },
                        difficultyLevel = detail.difficultyLevel,
                        difficultyProgress = detail.difficultyProgress,
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = pe.khipuai.app.core.network.NetworkErrorMapper.from(e).message
                    )
                }

            noteRepository.getNoteLocalGraph(id)
                .onSuccess { graph ->
                    _uiState.value = _uiState.value.copy(
                        d3NodesJson = Json.encodeToString(graph.nodes),
                        d3EdgesJson = Json.encodeToString(graph.edges)
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        d3NodesJson = "[]",
                        d3EdgesJson = "[]"
                    )
                }
        }
    }

    fun consumeSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    init {
        loadNoteDetail()
    }
}

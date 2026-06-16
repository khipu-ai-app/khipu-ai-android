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
import pe.khipuai.app.data.repository.NoteRepository
import javax.inject.Inject

data class AnalysisUiState(
    val isLoading: Boolean = false,
    val title: String = "",
    val summary: String = "",
    val keyConcepts: List<String> = emptyList(),
    val difficultyLevel: String = "Intermedio",
    val difficultyProgress: Float = 0.5f,
    val aiSuggestion: String = "He preparado este material para ti. ¿Hay alguna parte específica que te gustaría que desglosemos?",
    val errorMessage: String? = null,
    val d3NodesJson: String = "[]",
    val d3EdgesJson: String = "[]",
    val showLocalGraph: Boolean = false
)

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val noteId: String? = savedStateHandle["noteId"]

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = "Error de red: ${exception.localizedMessage}"
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
                        keyConcepts = detail.topics.map { it.name },
                        difficultyLevel = detail.difficultyLevel,
                        difficultyProgress = detail.difficultyProgress,
                        aiSuggestion = "Detecté que este contenido es clave para tu examen de la UNSA. ¿Quieres que te lo explique con un ejemplo práctico de la región?"
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No se pudo cargar el análisis: ${e.localizedMessage}"
                    )
                }

            // Fetch Local Graph in parallel or right after
            noteRepository.getNoteLocalGraph(id)
                .onSuccess { graph ->
                    _uiState.value = _uiState.value.copy(
                        d3NodesJson = Json.encodeToString(graph.nodes),
                        d3EdgesJson = Json.encodeToString(graph.edges)
                    )
                }
                .onFailure { e ->
                    // Optionally log error but don't crash the whole screen
                    _uiState.value = _uiState.value.copy(
                        d3NodesJson = "[]",
                        d3EdgesJson = "[]"
                    )
                }
        }
    }

    fun toggleLocalGraph() {
        _uiState.value = _uiState.value.copy(showLocalGraph = !_uiState.value.showLocalGraph)
    }

    init {
        loadNoteDetail()
    }
}
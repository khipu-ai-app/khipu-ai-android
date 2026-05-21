package pe.khipuai.app.ui.screens.analysis

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

data class AnalysisUiState(
    val isLoading: Boolean = false,
    val title: String = "",
    val summary: String = "",
    val keyConcepts: List<String> = emptyList(),
    val difficultyLevel: String = "Intermedio",
    val difficultyProgress: Float = 0.5f,
    val aiSuggestion: String = "He preparado este material para ti. ¿Hay alguna parte específica que te gustaría que desglosemos?",
    val errorMessage: String? = null
)

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val noteId: String? = savedStateHandle["noteId"]

    private val _uiState = MutableStateFlow(AnalysisUiState(isLoading = true))
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    init {
        loadNoteDetail()
    }

    fun loadNoteDetail() {
        val id = noteId ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            noteRepository.getNoteDetail(id)
                .onSuccess { detail ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        title = detail.title,
                        summary = detail.summary,
                        keyConcepts = detail.topics, // Mapeamos los topics de red a tus conceptos clave
                        // Valores calculados por defecto adaptables
                        difficultyLevel = "Intermedio",
                        difficultyProgress = 0.6f,
                        aiSuggestion = "Detecté que este contenido es clave para tu examen de la UNSA. ¿Quieres que te lo explique con un ejemplo práctico de la región?"
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No se pudo cargar el análisis: ${e.localizedMessage}"
                    )
                }
        }
    }
}
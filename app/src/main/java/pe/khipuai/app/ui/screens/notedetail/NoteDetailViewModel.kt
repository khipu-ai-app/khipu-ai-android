package pe.khipuai.app.ui.screens.notedetail

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val title: String = "Cargando nota...",
    val capturedDate: String = "",
    val courseName: String = "",
    val aiSummary: String = "",
    val extractedText: String = "",
    val keyConcepts: List<String> = emptyList(),
    val historyTimeline: List<HistoryItemUiModel> = emptyList(),
    val isBookmarked: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {


    private val _uiState = MutableStateFlow(NoteDetailUiState(isLoading = true))
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()

    init {
        loadNoteDetails()
    }

    private fun loadNoteDetails() {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            title = "Teoría de Cuerdas",
            capturedDate = "24 Oct, 2023",
            courseName = "Física Cuántica",
            aiSummary = "La teoría de cuerdas propone que las partículas fundamentales no son puntos sin dimensión, sino filamentos unidimensionales vibrantes llamados \"cuerdas\". Esta teoría busca unificar la mecánica cuántica con la relatividad general, intentando explicar todas las fuerzas fundamentales de la naturaleza en un solo marco teórico coherente, requiriendo dimensiones espaciales adicionales más allá de las tres perceptibles.",
            extractedText = "...el modelo estándar de la física de partículas es incompleto. Las ecuaciones de la relatividad general de Einstein fallan a nivel subatómico. Aquí es donde la teoría de cuerdas ofrece una solución elegante. Si asumimos que los quarks y electrones están formados por cuerdas minúsculas, la gravedad surge naturalmente de las vibraciones de estas cuerdas (gravitones). Sin embargo, las matemáticas requieren 10 u 11 dimensiones para funcionar sin anomalías...",
            keyConcepts = listOf("Física Cuántica", "Relatividad", "Dimensiones Extras", "Gravitones", "Unificación"),
            historyTimeline = listOf(
                HistoryItemUiModel("h1", "Repaso Guiado", "Hace 2 días • 85% retención", HistoryItemType.REPASO_COMPLETADO),
                HistoryItemUiModel("h2", "Nota Creada", "24 Oct, 2023", HistoryItemType.NOTA_CREADA)
            )
        )
    }

    fun toggleBookmark() {
        _uiState.value = _uiState.value.copy(isBookmarked = !_uiState.value.isBookmarked)
    }
}
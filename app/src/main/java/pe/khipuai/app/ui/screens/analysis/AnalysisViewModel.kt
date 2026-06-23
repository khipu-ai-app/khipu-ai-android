package pe.khipuai.app.ui.screens.analysis

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pe.khipuai.app.data.local.entity.NoteEntity
import pe.khipuai.app.data.notification.ManualScheduleScheduler
import pe.khipuai.app.data.notification.ReminderNotificationHelper
import pe.khipuai.app.data.remote.dto.ConceptNodeDto
import pe.khipuai.app.data.repository.NoteRepository
import pe.khipuai.app.data.repository.PlannerRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class AnalysisUiState(
    val isLoading: Boolean = false,
    val title: String = "",
    val summary: String = "",
    val topics: List<ConceptNodeDto> = emptyList(),
    val keyConcepts: List<String> = emptyList(),
    val difficultyLevel: String = "Intermedio",
    val difficultyProgress: Float = 0.5f,
    val aiSuggestion: String = "He preparado este material para ti. ¿Hay alguna parte específica que te gustaría que desglosemos?",
    val errorMessage: String? = null,
    val d3NodesJson: String = "[]",
    val d3EdgesJson: String = "[]",
    val showLocalGraph: Boolean = false,
    val showAiSuggestion: Boolean = true,
    val showCalendarDialog: Boolean = false,
    val isScheduling: Boolean = false,
    val snackbarMessage: String? = null
)

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val noteRepository: NoteRepository,
    private val plannerRepository: PlannerRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val noteId: String? = savedStateHandle["noteId"]
    private val prefs: SharedPreferences
        get() = context.getSharedPreferences("khipu_analysis", Context.MODE_PRIVATE)

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

        // Hidratar el estado de la AI suggestion desde SharedPreferences antes de cargar
        val dismissed = prefs.getBoolean("dismissed_ai_$id", false)

        viewModelScope.launch(exceptionHandler) {
            _uiState.value = _uiState.value.copy(isLoading = true, showAiSuggestion = !dismissed)

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

    fun dismissAiSuggestion() {
        val id = noteId ?: return
        prefs.edit().putBoolean("dismissed_ai_$id", true).apply()
        _uiState.value = _uiState.value.copy(showAiSuggestion = false)
    }

    fun openCalendarDialog() {
        _uiState.value = _uiState.value.copy(showCalendarDialog = true)
    }

    fun closeCalendarDialog() {
        _uiState.value = _uiState.value.copy(showCalendarDialog = false)
    }

    fun consumeSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    fun scheduleManually(dateIso: String) {
        val id = noteId ?: return
        viewModelScope.launch(exceptionHandler) {
            _uiState.value = _uiState.value.copy(isScheduling = true)
            val result = plannerRepository.createManualSchedule(id, dateIso)
            if (result.isSuccess) {
                // Programar la notificación local para esa fecha
                runCatching {
                    val target = LocalDate.parse(dateIso, DateTimeFormatter.ISO_LOCAL_DATE)
                    ManualScheduleScheduler.schedule(
                        context = context,
                        noteId = id,
                        noteTitle = _uiState.value.title.ifBlank { "Tu apunte" },
                        scheduledDate = target
                    )
                }
            }
            _uiState.value = _uiState.value.copy(
                isScheduling = false,
                showCalendarDialog = false,
                snackbarMessage = if (result.isSuccess) {
                    "Agregado a tu calendario de repaso para el $dateIso"
                } else {
                    "No se pudo agregar. Intenta de nuevo."
                }
            )
        }
    }

    init {
        loadNoteDetail()
        // Nos aseguramos de que el canal de notificaciones exista
        ReminderNotificationHelper.ensureChannel(context)
    }
}

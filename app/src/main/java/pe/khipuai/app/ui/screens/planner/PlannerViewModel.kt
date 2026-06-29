package pe.khipuai.app.ui.screens.planner

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.khipuai.app.data.remote.dto.ScheduleDayResponse
import pe.khipuai.app.data.repository.PlannerRepository
import javax.inject.Inject

data class PlannerUiState(
    val studyBlocks: List<StudyBlock> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val streakDays: Int = 0,
    val masteryPercentage: Int = 0,
    val totalConcepts: Int = 0,
    val weeklySchedule: List<ScheduleDayResponse> = emptyList(),
    val userName: String = "Estudiante",
    val snackbarMessage: String? = null,
    // Diálogo de confirmación para "Marcar todo como completado"
    val confirmCompleteBlockId: String? = null
)

data class StudyBlock(
    val id: String,
    val time: String,
    val duration: String,
    val subject: String,
    val tasks: List<Task>,
    val isAISuggestion: Boolean,
    val mentalLoadLevel: String,
    val mentalLoadColor: Color,
    val color: Color,
    val type: StudyBlockType,
    val noteId: String? = null   // F-10: para "Ver nota relacionada"
)

data class Task(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false,
    val noteId: String? = null,
    val lastRating: Int? = null   // T-10: último rating SM-2 (0-5) para resaltar el botón
)

enum class StudyBlockType {
    FOCUS, REVIEW, BREAK
}

@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val plannerRepository: PlannerRepository,
    private val authRepository: pe.khipuai.app.data.repository.AuthRepository
) : ViewModel() {

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = pe.khipuai.app.core.network.NetworkErrorMapper.from(exception).message
        )
    }

    private val _uiState = MutableStateFlow(PlannerUiState(isLoading = true))
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

    fun setSnackbarMessage(message: String) {
        _uiState.value = _uiState.value.copy(snackbarMessage = message)
    }
    fun loadRemotePlanner() {
        viewModelScope.launch(exceptionHandler) {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            // Cargar perfil en paralelo o silenciosamente
            launch {
                authRepository.fetchMyProfile().onSuccess { profile ->
                    val firstName = profile.fullName?.split(" ")?.firstOrNull() ?: "Estudiante"
                    _uiState.value = _uiState.value.copy(userName = firstName)
                }
            }

            plannerRepository.fetchDailyAgenda()
                .onSuccess { networkConcepts ->
                    if (networkConcepts.isEmpty()) {
                        _uiState.value = _uiState.value.copy(studyBlocks = emptyList(), isLoading = false)
                        return@onSuccess
                    }

                    // T-13: filtrar conceptos que NO son relevantes para la
                    // agenda de HOY. El backend ahora devuelve los 3 estados:
                    //   - is_due=true, reviewed_today=false → pendiente
                    //   - reviewed_today=true → completado hoy
                    //   - is_due=false, reviewed_today=false → futuro lejano
                    //     (no los mostramos, pero los recibimos para que el
                    //      stats endpoint pueda contarlos si quiere)
                    val todayConcepts = networkConcepts.filter {
                        it.isDue || it.reviewedToday
                    }

                    if (todayConcepts.isEmpty()) {
                        _uiState.value = _uiState.value.copy(studyBlocks = emptyList(), isLoading = false)
                        return@onSuccess
                    }

                    // 🧠 MAPEO HÍBRIDO: Agrupamos los conceptos del backend por nombre de curso
                    val conceptsByCourse = todayConcepts.groupBy { it.courseName }

                    val mappedBlocks = conceptsByCourse.entries.mapIndexed { index, entry ->
                        val courseName = entry.key
                        val concepts = entry.value

                        // Calculamos una carga mental dinámica basada en la cantidad de conceptos a repasar
                        val (loadLevel, loadColor) = when {
                            concepts.size >= 4 -> "Alta" to Color(0xFFD32F2F)
                            concepts.size >= 2 -> "Media" to Color(0xFFF57C00)
                            else -> "Baja" to Color(0xFF388E3C)
                        }

                        // noteId del primer concepto del bloque (todos pertenecen al mismo curso)
                        val firstNoteId = concepts.firstOrNull()?.noteId

                        // T-13: orden estable. El backend ya ordena por
                        // (pendientes primero, completados al final) PERO
                        // por las dudas ordenamos también en cliente.
                        val orderedConcepts = concepts.sortedWith(
                            compareBy<pe.khipuai.app.data.remote.dto.DueConceptResponse> { it.reviewedToday }
                                .thenBy { it.label }
                        )

                        StudyBlock(
                            id = courseName,
                            time = "${8 + index * 2}:00 AM",
                            duration = "${concepts.size * 10} min",
                            subject = courseName,
                            tasks = orderedConcepts.map {
                                Task(
                                    id = it.conceptId,
                                    title = it.label,
                                    // T-13: si el backend ya lo marcó como
                                    // repasado hoy, pre-marcamos el Task
                                    // para que la UI lo muestre como
                                    // completado al cargar.
                                    isCompleted = it.reviewedToday,
                                    noteId = it.noteId,
                                    lastRating = it.lastRating,
                                )
                            },
                            isAISuggestion = true,
                            mentalLoadLevel = loadLevel,
                            mentalLoadColor = loadColor,
                            color = Color(0xFF7B1FA2),
                            type = StudyBlockType.REVIEW,
                            noteId = firstNoteId
                        )
                    }

                    _uiState.value = _uiState.value.copy(studyBlocks = mappedBlocks, isLoading = false)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = pe.khipuai.app.core.network.NetworkErrorMapper.from(exception).message
                    )
                    return@launch
                }

            // Estadísticas de progreso (fallo silencioso para no bloquear la agenda)
            plannerRepository.fetchStats()
                .onSuccess { stats ->
                    _uiState.value = _uiState.value.copy(
                        streakDays = stats.streakDays,
                        masteryPercentage = stats.masteryPercentage,
                        totalConcepts = stats.totalConcepts
                    )
                }

            // Calendario semanal de repasos (fallo silencioso)
            plannerRepository.fetchWeeklySchedule()
                .onSuccess { schedule ->
                    _uiState.value = _uiState.value.copy(weeklySchedule = schedule)
                }
        }
    }

    fun toggleTask(blockId: String, conceptId: String) {
        viewModelScope.launch(exceptionHandler) {
            val currentBlocks = _uiState.value.studyBlocks
            var finalIsCompleted = false

            val updatedBlocks = currentBlocks.map { block ->
                if (block.id == blockId) {
                    val updatedTasks = block.tasks.map { task ->
                        if (task.id == conceptId) {
                            finalIsCompleted = !task.isCompleted
                            task.copy(isCompleted = finalIsCompleted)
                        } else {
                            task
                        }
                    }
                    block.copy(tasks = updatedTasks)
                } else {
                    block
                }
            }
            _uiState.value = _uiState.value.copy(studyBlocks = updatedBlocks)

            val score = if (finalIsCompleted) 5 else 1

            plannerRepository.submitReviewRating(conceptId, score)
                .onFailure {
                    loadRemotePlanner()
                }
        }
    }

    fun submitRating(blockId: String, conceptId: String, rating: Int) {
        viewModelScope.launch(exceptionHandler) {
            val currentBlocks = _uiState.value.studyBlocks
            val updatedBlocks = currentBlocks.map { block ->
                if (block.id == blockId) {
                    val updatedTasks = block.tasks.map { task ->
                        if (task.id == conceptId) {
                            // Optimista: marcamos completado y guardamos
                            // el rating. T-10: NO ocultamos los botones
                            // (la UI los sigue mostrando) para permitir
                            // re-calificar.
                            task.copy(isCompleted = true, lastRating = rating)
                        } else {
                            task
                        }
                    }
                    block.copy(tasks = updatedTasks)
                } else {
                    block
                }
            }
            _uiState.value = _uiState.value.copy(studyBlocks = updatedBlocks)

            plannerRepository.submitReviewRating(conceptId, rating)
                .onFailure {
                    loadRemotePlanner()
                }
        }
    }

    // ── F-10: Posponer al mañana ─────────────────────────────────────────────
    fun postponeBlock(blockId: String) {
        viewModelScope.launch(exceptionHandler) {
            val block = _uiState.value.studyBlocks.find { it.id == blockId } ?: return@launch
            val conceptIds = block.tasks.map { it.id }

            plannerRepository.postponeConcepts(conceptIds, days = 1)
                .onSuccess {
                    // Eliminar el bloque de la lista actual
                    val updatedBlocks = _uiState.value.studyBlocks.filter { it.id != blockId }
                    _uiState.value = _uiState.value.copy(
                        studyBlocks = updatedBlocks,
                        snackbarMessage = "Repaso movido a mañana"
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        snackbarMessage = "No se pudo posponer. Intenta de nuevo."
                    )
                }
        }
    }

    // ── F-10: Solicitar confirmación para marcar todo como completado ────────
    fun requestMarkAllCompleted(blockId: String) {
        _uiState.value = _uiState.value.copy(confirmCompleteBlockId = blockId)
    }

    fun dismissConfirmMarkAll() {
        _uiState.value = _uiState.value.copy(confirmCompleteBlockId = null)
    }

    // ── F-10: Confirmar marcar todos los conceptos del bloque como rating 4 ──
    fun confirmMarkAllCompleted() {
        val blockId = _uiState.value.confirmCompleteBlockId ?: return
        _uiState.value = _uiState.value.copy(confirmCompleteBlockId = null)

        viewModelScope.launch(exceptionHandler) {
            val block = _uiState.value.studyBlocks.find { it.id == blockId } ?: return@launch

            // Marcar todos visualmente como completados
            val updatedBlocks = _uiState.value.studyBlocks.map { b ->
                if (b.id == blockId) b.copy(tasks = b.tasks.map { it.copy(isCompleted = true) })
                else b
            }
            _uiState.value = _uiState.value.copy(studyBlocks = updatedBlocks)

            // Enviar rating 4 para cada concepto del bloque
            block.tasks.forEach { task ->
                plannerRepository.submitReviewRating(task.id, 4)
            }

            // Dar feedback al usuario
            _uiState.value = _uiState.value.copy(
                snackbarMessage = "${block.tasks.size} conceptos marcados como recordados"
            )
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    init {
        loadRemotePlanner()
    }
}

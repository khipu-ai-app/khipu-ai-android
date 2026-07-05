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
import pe.khipuai.app.data.repository.CourseRepository
import pe.khipuai.app.data.repository.PlannerRepository
import pe.khipuai.app.ui.theme.parseCourseColor
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
    val snackbarMessage: String? = null
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
    private val authRepository: pe.khipuai.app.data.repository.AuthRepository,
    // T-16: inyectamos el repo de cursos para mapear `courseName` →
    // color hex. La lista se mantiene sincronizada vía Flow de Room.
    private val courseRepository: CourseRepository,
) : ViewModel() {

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = pe.khipuai.app.core.network.NetworkErrorMapper.from(exception).message
        )
    }

    private val _uiState = MutableStateFlow(PlannerUiState(isLoading = true))
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

    // T-16: cache de "course name → color" para no re-parsear en cada
    // recomposición. Se actualiza cuando Room emite una nueva lista.
    private var courseColorByName: Map<String, Color> = emptyMap()
    // T-16: guardamos la última respuesta cruda del backend para poder
    // re-renderizar cuando el cache de colores cambie (ej. el usuario
    // edita el color de un curso y la agenda se actualiza).
    private var lastNetworkConcepts: List<pe.khipuai.app.data.remote.dto.DueConceptResponse> = emptyList()

    fun setSnackbarMessage(message: String) {
        _uiState.value = _uiState.value.copy(snackbarMessage = message)
    }
    /**
     * T-16: agrupa conceptos por curso y construye los `StudyBlock`
     * correspondientes. Usado por `loadRemotePlanner` después de la
     * respuesta del backend y re-utilizado por el Flow de Room cuando
     * el cache de colores de cursos cambia.
     *
     * El `block.color` se resuelve desde el cache `courseColorByName`
     * (courseName → Color). Si no está en el cache, usa el morado
     * neutro de fallback (`#7B1FA2`).
     */
    private fun mapConceptsToBlocks(
        networkConcepts: List<pe.khipuai.app.data.remote.dto.DueConceptResponse>
    ): List<StudyBlock> {
        val conceptsByCourse = networkConcepts.groupBy { it.courseName }

        return conceptsByCourse.entries.mapIndexed { index, entry ->
            val courseName = entry.key
            val concepts = entry.value

            // Carga mental dinámica basada en la cantidad de conceptos
            val (loadLevel, loadColor) = when {
                concepts.size >= 4 -> "Alta" to Color(0xFFD32F2F)
                concepts.size >= 2 -> "Media" to Color(0xFFF57C00)
                else -> "Baja" to Color(0xFF388E3C)
            }

            val firstNoteId = concepts.firstOrNull()?.noteId

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
                        isCompleted = it.reviewedToday,
                        noteId = it.noteId,
                        lastRating = it.lastRating,
                    )
                },
                isAISuggestion = true,
                mentalLoadLevel = loadLevel,
                mentalLoadColor = loadColor,
                // T-16: color real del curso (de Room), no el morado
                // hardcodeado de antes.
                color = courseColorByName[courseName] ?: Color(0xFF7B1FA2),
                type = StudyBlockType.REVIEW,
                noteId = firstNoteId
            )
        }
    }

    fun loadRemotePlanner() {
        viewModelScope.launch(exceptionHandler) {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            // T-16: sincronizar el cache de colores de cursos antes de
            // procesar la agenda, para que el `block.color` se resuelva
            // con el color real del curso.
            launch {
                courseRepository.fetchMyCourses()
                courseRepository.observeAll().collect { courses ->
                    courseColorByName = courses.associate { it.name to parseCourseColor(it.color) }
                    // Re-renderizar con los colores actualizados
                    if (lastNetworkConcepts.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            studyBlocks = mapConceptsToBlocks(lastNetworkConcepts)
                        )
                    }
                }
            }

            // Cargar perfil en paralelo o silenciosamente
            launch {
                authRepository.fetchMyProfile().onSuccess { profile ->
                    val firstName = profile.fullName?.split(" ")?.firstOrNull() ?: "Estudiante"
                    _uiState.value = _uiState.value.copy(userName = firstName)
                }
            }

            plannerRepository.fetchDailyAgenda()
                .onSuccess { networkConcepts ->
                    lastNetworkConcepts = networkConcepts
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

                    _uiState.value = _uiState.value.copy(
                        studyBlocks = mapConceptsToBlocks(todayConcepts),
                        isLoading = false
                    )
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

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    init {
        loadRemotePlanner()
    }
}

package pe.khipuai.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.khipuai.app.data.repository.CourseRepository
import pe.khipuai.app.data.repository.NoteRepository
import pe.khipuai.app.data.repository.PlannerRepository
import javax.inject.Inject

data class Suggestion(
    val conceptId: String,
    val conceptName: String,
    val courseName: String,
    val label: String
)

data class HomeUiState(
    val userName: String = "Estudiante",
    val dailyProgress: Float = 0f,
    val streak: Int = 0,
    val courses: List<Course> = emptyList(),
    val recentFiles: List<RecentFile> = emptyList(),
    val suggestion: Suggestion? = null,
    val isLoading: Boolean = false
)

data class Course(
    val id: String,
    val name: String,
    val progress: Float,
    val filesCount: Int,
    val color: String,
    val icon: String
)

data class RecentFile(
    val id: String,
    val uploadId: String?,
    val title: String,
    val subject: String,
    val timeAgo: String,
    val type: FileType,
    val courseId: String? = null
)

enum class FileType {
    DOCUMENT, AUDIO, IMAGE, VIDEO
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val offlineFirstNoteRepository: pe.khipuai.app.data.repository.OfflineFirstNoteRepository,
    private val plannerRepository: PlannerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadDashboardContent() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // Lanzar las llamadas en paralelo incluyendo la agenda diaria para sugerencias
                val coursesDeferred = async { courseRepository.fetchMyCourses() }
                val notesDeferred = async { offlineFirstNoteRepository.syncFromNetwork() }
                val statsDeferred = async { plannerRepository.fetchStats() }
                val agendaDeferred = async { plannerRepository.fetchDailyAgenda() }

                coursesDeferred.await()
                notesDeferred.await()
                val statsResult = statsDeferred.await()
                val agendaResult = agendaDeferred.await()

                var streak = 0
                var dailyProgress = 0f
                var suggestion: Suggestion? = null

                // Si la llamada de stats falla, streak y dailyProgress quedan en sus defaults (0)
                statsResult.onSuccess { stats ->
                    streak = stats.streakDays
                    dailyProgress = stats.masteryPercentage / 100f
                }

                // Extraer la primera sugerencia de repaso si hay conceptos pendientes
                agendaResult.onSuccess { agenda ->
                    if (agenda.isNotEmpty()) {
                        val firstDue = agenda.first()
                        suggestion = Suggestion(
                            conceptId = firstDue.conceptId,
                            conceptName = firstDue.conceptName,
                            courseName = firstDue.courseName,
                            label = firstDue.label
                        )
                    }
                }

                _uiState.value = _uiState.value.copy(
                    streak = streak,
                    dailyProgress = dailyProgress,
                    suggestion = suggestion,
                    isLoading = false
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    init {
        // Observar cursos y notas de Room reactivamente para mantener la UI sincronizada
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                courseRepository.observeAll(),
                offlineFirstNoteRepository.observeAll()
            ) { localCourses, localNotes ->
                val coursesMapped = localCourses.filter { it.isActive }.map { entity ->
                    Course(
                        id = entity.id,
                        name = entity.name,
                        progress = 0.0f,
                        filesCount = localNotes.count { it.courseId == entity.id },
                        color = entity.color.ifBlank { "#4B00B2" },
                        icon = "calculate"
                    )
                }.sortedWith(
                    compareByDescending<Course> { it.filesCount }
                        .thenBy { it.name }
                ).take(3)


                val courseMap = localCourses.associateBy { it.id }
                val filesMapped = localNotes.map { entity ->
                    RecentFile(
                        id = entity.id,
                        uploadId = entity.uploadId,
                        title = entity.title,
                        subject = entity.courseId?.let { courseMap[it]?.name } ?: "General",
                        timeAgo = "Añadido recientemente",
                        type = FileType.DOCUMENT,
                        courseId = entity.courseId
                    )
                }

                _uiState.value = _uiState.value.copy(
                    courses = coursesMapped,
                    recentFiles = filesMapped
                )
            }.collect {}
        }

        loadDashboardContent()
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            offlineFirstNoteRepository.deleteNote(noteId)
        }
    }

    fun renameNote(noteId: String, newTitle: String, currentCourseId: String?) {
        viewModelScope.launch {
            offlineFirstNoteRepository.updateNote(noteId, newTitle, currentCourseId)
        }
    }
}

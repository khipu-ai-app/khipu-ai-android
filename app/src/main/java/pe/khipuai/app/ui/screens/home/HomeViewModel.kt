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

data class HomeUiState(
    val userName: String = "Estudiante",
    val dailyProgress: Float = 0f,
    val streak: Int = 0,
    val courses: List<Course> = emptyList(),
    val recentFiles: List<RecentFile> = emptyList(),
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
    val title: String,
    val subject: String,
    val timeAgo: String,
    val type: FileType
)

enum class FileType {
    DOCUMENT, AUDIO, IMAGE, VIDEO
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val noteRepository: NoteRepository,
    private val plannerRepository: PlannerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadDashboardContent() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // Lanzar las tres llamadas en paralelo
                val coursesDeferred = async { courseRepository.fetchMyCourses() }
                val notesDeferred = async { noteRepository.fetchMyNotes() }
                val statsDeferred = async { plannerRepository.fetchStats() }

                val coursesResult = coursesDeferred.await()
                val notesResult = notesDeferred.await()
                val statsResult = statsDeferred.await()

                var fetchedCourses = emptyList<Course>()
                var fetchedFiles = emptyList<RecentFile>()
                var streak = 0
                var dailyProgress = 0f

                coursesResult.onSuccess { list ->
                    fetchedCourses = list.map { dto ->
                        Course(
                            id = dto.id,
                            name = dto.name,
                            progress = 0.0f,
                            filesCount = 0,
                            color = dto.color.ifBlank { "#4B00B2" },
                            icon = "calculate"
                        )
                    }
                }

                notesResult.onSuccess { list ->
                    fetchedFiles = list.map { dto ->
                        RecentFile(
                            id = dto.id,
                            title = dto.title,
                            subject = "General",
                            timeAgo = "Añadido recientemente",
                            type = FileType.DOCUMENT
                        )
                    }
                }

                // Si la llamada de stats falla, streak y dailyProgress quedan en sus defaults (0)
                statsResult.onSuccess { stats ->
                    streak = stats.streakDays
                    dailyProgress = stats.masteryPercentage / 100f
                }

                _uiState.value = _uiState.value.copy(
                    courses = fetchedCourses,
                    recentFiles = fetchedFiles,
                    streak = streak,
                    dailyProgress = dailyProgress,
                    isLoading = false
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    init {
        loadDashboardContent()
    }
}

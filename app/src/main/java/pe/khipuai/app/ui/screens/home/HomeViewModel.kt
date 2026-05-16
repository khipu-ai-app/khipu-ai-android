package pe.khipuai.app.ui.screens.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class HomeUiState(
    val userName: String = "Estudiante",
    val dailyProgress: Float = 0.75f,
    val streak: Int = 5,
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
    // TODO: Inject repositories when implemented
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(
        HomeUiState(
            courses = getSampleCourses(),
            recentFiles = getSampleRecentFiles()
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private fun getSampleCourses(): List<Course> {
        return listOf(
            Course(
                id = "1",
                name = "Matemáticas",
                progress = 0.45f,
                filesCount = 12,
                color = "#4B00B2",
                icon = "calculate"
            ),
            Course(
                id = "2",
                name = "Historia",
                progress = 0.80f,
                filesCount = 8,
                color = "#2E7D32",
                icon = "book"
            ),
            Course(
                id = "3",
                name = "Psicología",
                progress = 0.15f,
                filesCount = 24,
                color = "#D32F2F",
                icon = "psychology"
            )
        )
    }
    
    private fun getSampleRecentFiles(): List<RecentFile> {
        return listOf(
            RecentFile(
                id = "1",
                title = "Apuntes_Revoluci...",
                subject = "Historia",
                timeAgo = "Añadido hace 2h",
                type = FileType.DOCUMENT
            ),
            RecentFile(
                id = "2",
                title = "Esquema_Derivad...",
                subject = "Matemáticas",
                timeAgo = "Añadido ayer",
                type = FileType.DOCUMENT
            ),
            RecentFile(
                id = "3",
                title = "Clase_Psicoanalisi...",
                subject = "Psicología",
                timeAgo = "Hace 3 días",
                type = FileType.AUDIO
            )
        )
    }
}
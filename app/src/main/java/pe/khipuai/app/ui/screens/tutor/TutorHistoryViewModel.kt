package pe.khipuai.app.ui.screens.tutor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.khipuai.app.data.remote.dto.ChatSessionResponse
import pe.khipuai.app.data.repository.CourseRepository
import pe.khipuai.app.data.repository.TutorRepository
import javax.inject.Inject

data class TutorHistoryUiState(
    val sessions: List<ChatSessionResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val screenTitle: String = "Conversaciones con Khipu",
    val courseColorHex: String? = null
)

@HiltViewModel
class TutorHistoryViewModel @Inject constructor(
    private val tutorRepository: TutorRepository,
    private val courseRepository: CourseRepository,
    savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    private val contextTypeArg: String? = savedStateHandle["contextType"]
    private val contextIdArg: String? = savedStateHandle["contextId"]

    private val _uiState = MutableStateFlow(TutorHistoryUiState())
    val uiState: StateFlow<TutorHistoryUiState> = _uiState.asStateFlow()

    init {
        resolveScreenTitle()
        loadSessions()
    }

    private fun resolveScreenTitle() {
        val type = contextTypeArg ?: "general"
        val id = contextIdArg
        when (type) {
            "course" -> {
                if (id != null) {
                    viewModelScope.launch {
                        val course = courseRepository.getById(id)
                        if (course != null) {
                            _uiState.value = _uiState.value.copy(
                                screenTitle = "Chat de ${course.name}",
                                courseColorHex = course.color
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                screenTitle = "Chat de curso"
                            )
                        }
                    }
                } else {
                    _uiState.value = _uiState.value.copy(screenTitle = "Chat de curso")
                }
            }
            "note" -> _uiState.value = _uiState.value.copy(screenTitle = "Chats de la nota")
            "concept" -> _uiState.value = _uiState.value.copy(screenTitle = "Chats del concepto")
            else -> _uiState.value = _uiState.value.copy(screenTitle = "Chat global")
        }
    }

    fun loadSessions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val cType = contextTypeArg ?: "general"
            val cId = contextIdArg
            tutorRepository.getSessions(cType, cId)
                .onSuccess { sessions ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        sessions = sessions.sortedByDescending { it.lastMessageAt ?: it.createdAt }
                    )
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = pe.khipuai.app.core.network.NetworkErrorMapper.from(err).message
                    )
                }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            tutorRepository.deleteSession(sessionId)
                .onSuccess {
                    loadSessions()
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(error = pe.khipuai.app.core.network.NetworkErrorMapper.from(err).message)
                }
        }
    }
}

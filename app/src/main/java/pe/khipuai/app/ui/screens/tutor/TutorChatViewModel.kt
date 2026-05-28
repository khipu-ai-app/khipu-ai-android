package pe.khipuai.app.ui.screens.tutor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.khipuai.app.data.repository.TutorRepository
import pe.khipuai.app.data.repository.TutorStreamEvent
import javax.inject.Inject

// Identificador del emisor del mensaje
enum class ChatSender { USER, AI }

// Modelo para inyectar referencias a notas o PDFs reales dentro del chat
data class KnowledgeNodeRef(
    val id: String,
    val title: String,
    val snippet: String
)

data class MessageUiModel(
    val id: String,
    val sender: ChatSender,
    val content: String,
    val timestamp: String,
    val referenceNodes: List<KnowledgeNodeRef> = emptyList()
)

data class TutorChatUiState(
    val sessionId: String? = null,
    val courseName: String = "Tutor Inteligente",
    val messages: List<MessageUiModel> = emptyList(),
    val inputText: String = "",
    val quickActions: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class TutorChatViewModel @Inject constructor(
    private val tutorRepository: TutorRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(TutorChatUiState())
    val uiState: StateFlow<TutorChatUiState> = _uiState.asStateFlow()

    private val sessionIdArg: String? = savedStateHandle["sessionId"]
    private val courseIdArg: String? = savedStateHandle["courseId"]

    init {
        _uiState.value = _uiState.value.copy(
            quickActions = listOf("Explícame más", "Dame un ejemplo", "Hazme una pregunta")
        )
        if (sessionIdArg != null && sessionIdArg != "new" && sessionIdArg != "new_session") {
            _uiState.value = _uiState.value.copy(sessionId = sessionIdArg)
            loadMessages(sessionIdArg)
        } else {
            // Crear sesión de chat nueva de forma dinámica llamando a Postgres
            initializeNewSession()
        }
    }

    private fun initializeNewSession() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            tutorRepository.createSession("Conversación con Khipu Tutor")
                .onSuccess { session ->
                    _uiState.value = _uiState.value.copy(
                        sessionId = session.id,
                        isLoading = false
                    )
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Error al iniciar tutoría: ${err.localizedMessage}"
                    )
                }
        }
    }

    fun loadMessages(sessionId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            tutorRepository.getMessages(sessionId)
                .onSuccess { list ->
                    val mapped = list.map { dto ->
                        MessageUiModel(
                            id = dto.id,
                            sender = if (dto.sender.lowercase() == "user") ChatSender.USER else ChatSender.AI,
                            content = dto.content,
                            timestamp = "Reciente"
                        )
                    }
                    _uiState.value = _uiState.value.copy(messages = mapped, isLoading = false)
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Error al cargar historial: ${err.localizedMessage}"
                    )
                }
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendMessage() {
        val query = _uiState.value.inputText
        val sessionId = _uiState.value.sessionId
        if (query.isBlank() || sessionId == null || _uiState.value.isStreaming) return

        // 1. Añadir el mensaje del usuario de forma reactiva a la lista
        val userMessage = MessageUiModel(
            id = System.currentTimeMillis().toString(),
            sender = ChatSender.USER,
            content = query,
            timestamp = "Ahora"
        )
        
        // Añadimos también un mensaje vacío de la IA que se irá completando vía streaming
        val aiPlaceholderId = (System.currentTimeMillis() + 1).toString()
        val aiMessagePlaceholder = MessageUiModel(
            id = aiPlaceholderId,
            sender = ChatSender.AI,
            content = "",
            timestamp = "Generando..."
        )

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMessage + aiMessagePlaceholder,
            inputText = "",
            isStreaming = true,
            errorMessage = null
        )

        viewModelScope.launch {
            var fullTextAccumulated = ""
            
            tutorRepository.streamChatMessages(sessionId, query, courseIdArg)
                .collect { event ->
                    when (event) {
                        is TutorStreamEvent.Chunk -> {
                            fullTextAccumulated += event.text
                            updateAiMessageContent(aiPlaceholderId, fullTextAccumulated)
                        }
                        is TutorStreamEvent.Done -> {
                            _uiState.value = _uiState.value.copy(isStreaming = false)
                            // Al terminar, actualizamos el mensaje final con las referencias
                            val refs = event.references.map {
                                KnowledgeNodeRef(it.noteId, it.noteTitle, it.snippet)
                            }
                            updateAiMessageFinal(aiPlaceholderId, fullTextAccumulated, refs)
                        }
                        is TutorStreamEvent.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isStreaming = false,
                                errorMessage = event.message
                            )
                        }
                    }
                }
        }
    }

    private fun updateAiMessageContent(messageId: String, content: String) {
        val updated = _uiState.value.messages.map { msg ->
            if (msg.id == messageId) {
                msg.copy(content = content)
            } else msg
        }
        _uiState.value = _uiState.value.copy(messages = updated)
    }

    private fun updateAiMessageFinal(messageId: String, content: String, refs: List<KnowledgeNodeRef>) {
        val updated = _uiState.value.messages.map { msg ->
            if (msg.id == messageId) {
                msg.copy(content = content, referenceNodes = refs)
            } else msg
        }
        _uiState.value = _uiState.value.copy(messages = updated)
    }
}
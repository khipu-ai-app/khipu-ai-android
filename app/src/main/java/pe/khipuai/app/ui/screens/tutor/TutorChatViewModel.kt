package pe.khipuai.app.ui.screens.tutor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
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

    private val sessionIdArg: String? = savedStateHandle["sessionId"]
    private val courseIdArg: String? = savedStateHandle["courseId"]
    private val contextTypeArg: String? = savedStateHandle["contextType"]
    private val contextIdArg: String? = savedStateHandle["contextId"]
    private val initialConceptsArg: String? = savedStateHandle["initialConcepts"]
    private val noteContextArg: String? = savedStateHandle["noteContext"]
    private val noteTitleArg: String? = savedStateHandle["noteTitle"]

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isStreaming = false,
            errorMessage = pe.khipuai.app.core.network.NetworkErrorMapper.from(exception).message
        )
    }

    private val _uiState = MutableStateFlow(TutorChatUiState())
    val uiState: StateFlow<TutorChatUiState> = _uiState.asStateFlow()

    fun loadMessages(sessionId: String) {
        viewModelScope.launch(exceptionHandler) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            tutorRepository.getMessages(sessionId)
                .onSuccess { list ->
                    val mapped = list.map { dto ->
                        MessageUiModel(
                            id = dto.id,
                            sender = if (dto.sender.lowercase() == "user") ChatSender.USER else ChatSender.AI,
                            content = dto.content,
                            timestamp = "Reciente",
                            referenceNodes = dto.referenceNodes?.map { ref ->
                                KnowledgeNodeRef(
                                    id = ref.noteId,
                                    title = ref.noteTitle,
                                    snippet = ref.snippet
                                )
                            } ?: emptyList()
                        )
                    }
                    _uiState.value = _uiState.value.copy(messages = mapped, isLoading = false)
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = pe.khipuai.app.core.network.NetworkErrorMapper.from(err).message
                    )
                }
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendMessage() {
        val query = _uiState.value.inputText.trim()
        if (query.isBlank() || _uiState.value.isStreaming) return

        val currentSessionId = _uiState.value.sessionId
        val isNewSession = currentSessionId.isNullOrBlank() ||
            currentSessionId in listOf("new", "new_session")

        // 1. Render optimista: añadimos el mensaje del usuario + placeholder de la IA
        val userMessage = MessageUiModel(
            id = "user_${System.currentTimeMillis()}",
            sender = ChatSender.USER,
            content = query,
            timestamp = "Ahora"
        )
        val aiPlaceholderId = "ai_${System.currentTimeMillis() + 1}"
        val aiPlaceholder = MessageUiModel(
            id = aiPlaceholderId,
            sender = ChatSender.AI,
            content = "",
            timestamp = "Generando..."
        )

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMessage + aiPlaceholder,
            inputText = "",
            isStreaming = true,
            errorMessage = null
        )

        if (isNewSession) {
            // Solo creamos la sesión en el backend cuando el usuario efectivamente
            // envía el primer mensaje. Si navega fuera sin enviar, no queda
            // ningún chat fantasma en la lista.
            createSessionAndStream(query, aiPlaceholderId)
        } else {
            streamResponse(currentSessionId!!, query, aiPlaceholderId)
        }
    }

    private fun createSessionAndStream(query: String, aiPlaceholderId: String) {
        viewModelScope.launch(exceptionHandler) {
            val cType = if (courseIdArg != null) "course" else (contextTypeArg ?: "general")
            val cId = courseIdArg ?: contextIdArg

            tutorRepository.createSession(cType, cId)
                .onSuccess { session ->
                    _uiState.value = _uiState.value.copy(
                        sessionId = session.id,
                        courseName = session.title
                    )
                    val realCType = if (courseIdArg != null) "course" else (contextTypeArg ?: "general")
                    val realCId = courseIdArg ?: contextIdArg
                    streamResponseInternal(session.id, query, aiPlaceholderId, realCType, realCId)
                }
                .onFailure { err ->
                    // Si falla la creación, limpiamos los placeholders para no dejar
                    // el chat en un estado inconsistente.
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages.filterNot {
                            it.id == aiPlaceholderId || it.id.startsWith("user_") &&
                                it.content == query
                        },
                        inputText = query,  // Devolvemos lo que escribió para que no pierda el texto
                        isStreaming = false,
                        errorMessage = pe.khipuai.app.core.network.NetworkErrorMapper.from(err).message
                    )
                }
        }
    }

    private fun streamResponse(
        sessionId: String,
        query: String,
        aiPlaceholderId: String
    ) {
        val cType = if (courseIdArg != null) "course" else (contextTypeArg ?: "general")
        val cId = courseIdArg ?: contextIdArg
        streamResponseInternal(sessionId, query, aiPlaceholderId, cType, cId)
    }

    private fun streamResponseInternal(
        sessionId: String,
        query: String,
        aiPlaceholderId: String,
        cType: String,
        cId: String?
    ) {
        viewModelScope.launch(exceptionHandler) {
            var fullTextAccumulated = ""
            tutorRepository.streamChatMessages(sessionId, query, cType, cId)
                .collect { event ->
                    when (event) {
                        is TutorStreamEvent.Chunk -> {
                            fullTextAccumulated += event.text
                            updateAiMessageContent(aiPlaceholderId, fullTextAccumulated)
                        }
                        is TutorStreamEvent.Done -> {
                            _uiState.value = _uiState.value.copy(isStreaming = false)
                            val refs = event.references.map {
                                KnowledgeNodeRef(it.noteId, it.noteTitle, it.snippet)
                            }
                            updateAiMessageFinal(aiPlaceholderId, fullTextAccumulated, refs)
                        }
                        is TutorStreamEvent.Error -> {
                            updateAiMessageContent(aiPlaceholderId, "❌ " + event.message)
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

    init {
        _uiState.value = _uiState.value.copy(
            quickActions = listOf("Explícame más", "Dame un ejemplo", "Hazme una pregunta")
        )

        if (sessionIdArg != null && sessionIdArg != "new" && sessionIdArg != "new_session") {
            // Sesión existente: solo cargamos mensajes. NO pre-rellenamos.
            _uiState.value = _uiState.value.copy(sessionId = sessionIdArg)
            loadMessages(sessionIdArg)
        } else {
            // Sesión nueva: pre-rellenamos el input si viene contexto, pero
            // NO creamos la sesión todavía. Se creará cuando el usuario envíe
            // el primer mensaje (en sendMessage → createSessionAndStream).
            val prefillText = buildPrefillText(
                initialConcepts = initialConceptsArg,
                noteTitle = noteTitleArg
            )
            if (prefillText != null) {
                _uiState.value = _uiState.value.copy(inputText = prefillText)
            }
            // sessionId se queda en null — el envío lo creará on-demand.
        }
    }

    /**
     * Construye un texto de pre-rellenado para el input cuando se crea una sesión nueva.
     * Prioriza la lista de conceptos si viene; si no, usa el título de la nota.
     */
    private fun buildPrefillText(
        initialConcepts: String?,
        noteTitle: String?
    ): String? {
        if (!initialConcepts.isNullOrBlank()) {
            val titles = initialConcepts.split("|")
                .mapNotNull { java.net.URLDecoder.decode(it, "UTF-8").trim().takeIf(String::isNotEmpty) }
            if (titles.isNotEmpty()) {
                return if (titles.size == 1) {
                    "Explícame el concepto «${titles.first()}» usando mis apuntes."
                } else {
                    val list = titles.joinToString(", ") { "«$it»" }
                    "Hoy voy a estudiar: $list. Empecemos con «${titles.first()}». Explícamelo con ejemplos."
                }
            }
        }
        if (!noteTitle.isNullOrBlank()) {
            val decoded = java.net.URLDecoder.decode(noteTitle, "UTF-8")
            return "Tengo una pregunta sobre la nota «$decoded»: «»"
        }
        return null
    }
}
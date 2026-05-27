package pe.khipuai.app.ui.screens.tutor

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.khipuai.app.data.repository.NoteRepository
import javax.inject.Inject

// Identificador del emisor del mensaje
enum class ChatSender { USER, AI }

// Modelo para inyectar referencias a notas o PDFs reales dentro del chat
data class KnowledgeNodeRef(
    val title: String,
    val snippet: String
)

data class MessageUiModel(
    val id: String,
    val sender: ChatSender,
    val content: String,
    val timestamp: String,
    val referenceNode: KnowledgeNodeRef? = null
)

data class TutorChatUiState(
    val courseName: String = "Introducción a la Psicología",
    val messages: List<MessageUiModel> = emptyList(),
    val inputText: String = "",
    val quickActions: List<String> = emptyList()
)

@HiltViewModel
class TutorChatViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {


    private val _uiState = MutableStateFlow(TutorChatUiState())
    val uiState: StateFlow<TutorChatUiState> = _uiState.asStateFlow()

    init {
        loadMockConversation()
    }

    private fun loadMockConversation() {
        _uiState.value = _uiState.value.copy(
            quickActions = listOf("Explícame más", "Dame un ejemplo", "Hazme una pregunta"),
            messages = listOf(
                MessageUiModel(
                    id = "1",
                    sender = ChatSender.USER,
                    content = "¿Me puedes explicar cómo se relaciona la 'Plasticidad Neuronal' con mis notas sobre el aprendizaje de idiomas?",
                    timestamp = "Hoy, 10:42 AM"
                ),
                MessageUiModel(
                    id = "2",
                    sender = ChatSender.AI,
                    content = "¡Claro! La plasticidad neuronal es la capacidad de tu cerebro para reorganizarse físicamente. Según tus apuntes recientes sobre lingüística, esto ocurre activamente cuando adquieres un nuevo idioma.",
                    timestamp = "Hoy, 10:42 AM",
                    referenceNode = KnowledgeNodeRef(
                        title = "Adquisición de Segunda Lengua (L2)",
                        snippet = "El hipocampo y la corteza cerebral aumentan su densidad de materia gris durante los primeros meses de estudio intensivo, demostrando cambios estructurales..."
                    )
                )
            )
        )
    }

    fun onInputTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendMessage() {
        if (_uiState.value.inputText.isBlank()) return

        val newUserMessage = MessageUiModel(
            id = System.currentTimeMillis().toString(),
            sender = ChatSender.USER,
            content = _uiState.value.inputText,
            timestamp = "Hoy, 10:43 AM"
        )

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + newUserMessage,
            inputText = "" // Limpiar caja
        )
    }
}
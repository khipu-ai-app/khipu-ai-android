package pe.khipuai.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatSessionCreateRequest(
    @SerialName("context_type") val contextType: String,
    @SerialName("context_id") val contextId: String? = null
)

@Serializable
data class ChatSessionResponse(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("title") val title: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("initial_message") val initialMessage: String? = null,
    @SerialName("last_message_preview") val lastMessagePreview: String? = null,
    @SerialName("message_count") val messageCount: Int = 0,
    @SerialName("last_message_at") val lastMessageAt: String? = null
)

@Serializable
data class ChatMessageRequest(
    @SerialName("message") val message: String,
    @SerialName("context_type") val contextType: String = "general",
    @SerialName("context_id") val contextId: String? = null
)

@Serializable
data class ReferenceNodeDto(
    @SerialName("note_id") val noteId: String,
    @SerialName("note_title") val noteTitle: String,
    @SerialName("snippet") val snippet: String
)

@Serializable
data class ChatMessageResponse(
    @SerialName("id") val id: String,
    @SerialName("session_id") val sessionId: String,
    @SerialName("sender") val sender: String,
    @SerialName("content") val content: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("reference_nodes") val referenceNodes: List<ReferenceNodeDto>? = null
)

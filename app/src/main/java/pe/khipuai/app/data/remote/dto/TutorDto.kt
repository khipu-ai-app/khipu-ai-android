package pe.khipuai.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatSessionCreateRequest(
    @SerialName("title") val title: String
)

@Serializable
data class ChatSessionResponse(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("title") val title: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class ChatMessageRequest(
    @SerialName("message") val message: String,
    @SerialName("course_id") val courseId: String? = null
)

@Serializable
data class ChatMessageResponse(
    @SerialName("id") val id: String,
    @SerialName("session_id") val sessionId: String,
    @SerialName("sender") val sender: String,
    @SerialName("content") val content: String,
    @SerialName("created_at") val createdAt: String
)

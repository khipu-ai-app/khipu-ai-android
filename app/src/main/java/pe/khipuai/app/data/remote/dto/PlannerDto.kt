package pe.khipuai.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkTask(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("is_completed") val isCompleted: Boolean
)

@Serializable
data class StudyBlockResponse(
    @SerialName("id") val id: String,
    @SerialName("time") val time: String,
    @SerialName("duration") val duration: String,
    @SerialName("subject") val subject: String,
    @SerialName("tasks") val tasks: List<NetworkTask>,
    @SerialName("is_ai_suggestion") val isAISuggestion: Boolean,
    @SerialName("mental_load_level") val mentalLoadLevel: String,
    @SerialName("mental_load_color") val mentalLoadColor: String, // Recibe Hex string: "#7B1FA2"
    @SerialName("color") val color: String,                     // Recibe Hex string: "#7B1FA2"
    @SerialName("type") val type: String                        // "FOCUS", "REVIEW", "BREAK"
)

@Serializable
data class TaskToggleRequest(
    @SerialName("is_completed") val isCompleted: Boolean
)
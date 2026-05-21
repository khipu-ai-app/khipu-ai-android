package pe.khipuai.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NoteResponse(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("course_id") val courseId: String?,
    @SerialName("created_at") val createdAt: String
)
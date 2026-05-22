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

@Serializable
data class NoteDetailResponse(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("summary") val summary: String,       // Estricto y no-nulo (Garantizado por el Backend)
    @SerialName("topics") val topics: List<String>,   // Estricto y no-nulo (Garantizado por el Backend)
    @SerialName("created_at") val createdAt: String, // Estricto y no-nulo (Garantizado por el Backend)
    @SerialName("course_id") val courseId: String?
)
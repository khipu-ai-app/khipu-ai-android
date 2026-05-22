package pe.khipuai.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UploadResponse(
    @SerialName("id") val id: String,
    @SerialName("filename") val filename: String,
    @SerialName("file_type") val fileType: String,
    @SerialName("status") val status: String
)

@Serializable
data class UploadStatusResponse(
    @SerialName("id") val id: String,
    @SerialName("status") val status: String,
    @SerialName("note_id") val noteId: String? = null
)
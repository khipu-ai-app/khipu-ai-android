package pe.khipuai.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UploadResponse(
    @SerialName("id") val id: String,
    @SerialName("status") val status: String,
    @SerialName("filename") val filename: String
)

@Serializable
data class UploadStatusResponse(
    @SerialName("id") val id: String,
    @SerialName("status") val status: String, // pending, processing, completed, failed
    @SerialName("note_id") val noteId: String?
)
package pe.khipuai.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * T-13 evolution: un archivo adjunto a una nota multi-archivo.
 *
 * Cada `Note` puede tener N `Upload`s asociados. El primero se
 * guarda en `Note.upload_id` (legacy), los siguientes en
 * `Upload.note_id` (T-13). El cliente lista todos juntos vía el
 * endpoint `GET /v1/notes/{note_id}/files`.
 */
@Serializable
data class NoteFileResponse(
    @SerialName("id") val id: String,
    @SerialName("filename") val filename: String,
    @SerialName("file_type") val fileType: String,
    @SerialName("storage_path") val storagePath: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class NoteFilesListResponse(
    @SerialName("note_id") val noteId: String,
    @SerialName("total_files") val totalFiles: Int,
    @SerialName("files") val files: List<NoteFileResponse>,
)

@Serializable
data class AddFilesResponse(
    @SerialName("note_id") val noteId: String,
    @SerialName("upload_ids") val uploadIds: List<String>,
    @SerialName("skipped_duplicates") val skippedDuplicates: List<String> = emptyList(),
)

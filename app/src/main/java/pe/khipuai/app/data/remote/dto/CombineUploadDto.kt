package pe.khipuai.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * T-13 combine: respuesta de `POST /v1/uploads/combine`.
 * El cliente recibe el `noteId` y la lista de `uploadIds`.
 */
@Serializable
data class CombineUploadResponse(
    @SerialName("note_id") val noteId: String,
    @SerialName("upload_ids") val uploadIds: List<String>,
)

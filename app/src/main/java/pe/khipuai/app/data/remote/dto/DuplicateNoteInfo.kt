package pe.khipuai.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * T-17: estructura del `detail` que el backend retorna en el body del
 * HTTP 409 cuando el usuario intenta subir un archivo que ya existe
 * (mismo hash SHA-256). Se usa para mostrar el dialog de "documento
 * duplicado" con la nota que ya tiene ese archivo.
 *
 * `existingNoteId` y `existingNoteTitle` pueden ser null si el upload
 * anterior aún está siendo procesado por la IA (la nota todavía no
 * existe). En ese caso, la UI muestra un dialog genérico.
 *
 * Ejemplo de body 409:
 * {
 *   "detail": {
 *     "code": "duplicate",
 *     "existing_note_id": "b14e1ebb-...",
 *     "existing_note_title": "Arquitectura de Concurrencia..."
 *   }
 * }
 */
@Serializable
data class DuplicateNoteInfo(
    @SerialName("code") val code: String,
    @SerialName("existing_note_id") val existingNoteId: String? = null,
    @SerialName("existing_note_title") val existingNoteTitle: String? = null,
)

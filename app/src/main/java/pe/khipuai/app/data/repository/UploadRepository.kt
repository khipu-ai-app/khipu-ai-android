package pe.khipuai.app.data.repository

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import pe.khipuai.app.data.remote.KhipuApiService
import pe.khipuai.app.data.remote.dto.CombineUploadResponse
import pe.khipuai.app.data.remote.dto.UploadResponse
import pe.khipuai.app.data.remote.dto.UploadStatusResponse
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadRepository @Inject constructor(
    private val apiService: KhipuApiService
) {
    /**
     * T-17: [forceUpload] reintenta el upload ignorando el chequeo de
     * hash. Usar SOLO cuando el usuario confirmó en el dialog que
     * quiere subir el archivo de todas formas.
     */
    /**
     * T-13 combine: sube [files] como una sola nota combinada.
     * El backend corre OCR + IA sobre el texto combinado.
     */
    suspend fun combineFiles(
        files: List<File>,
        mimeTypes: List<String>,
        courseId: String?,
    ): Result<CombineUploadResponse> {
        if (files.size < 2) {
            return Result.failure(IllegalArgumentException("Se necesitan al menos 2 archivos para combinar"))
        }
        return try {
            val parts = files.zip(mimeTypes).map { (file, mime) ->
                val requestFile = file.asRequestBody(mime.toMediaTypeOrNull())
                MultipartBody.Part.createFormData("files", file.name, requestFile)
            }
            val coursePart = courseId?.toRequestBody("text/plain".toMediaTypeOrNull())
            Result.success(apiService.combineUpload(parts, coursePart))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadFile(
        file: File,
        mimeType: String,
        courseId: String?,
        forceUpload: Boolean = false,
    ): Result<UploadResponse> {
        return try {
            val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

            val coursePart = courseId?.toRequestBody("text/plain".toMediaTypeOrNull())

            Result.success(apiService.uploadDocument(body, coursePart, forceUpload))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkProcessingStatus(uploadId: String): Result<UploadStatusResponse> {
        return try {
            Result.success(apiService.getUploadStatus(uploadId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelProcessing(uploadId: String): Result<Unit> {
        return try {
            apiService.deleteUpload(uploadId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
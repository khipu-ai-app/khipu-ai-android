package pe.khipuai.app.data.repository

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import pe.khipuai.app.data.remote.KhipuApiService
import pe.khipuai.app.data.remote.dto.UploadResponse
import pe.khipuai.app.data.remote.dto.UploadStatusResponse
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadRepository @Inject constructor(
    private val apiService: KhipuApiService
) {
    suspend fun uploadFile(file: File, mimeType: String): Result<UploadResponse> {
        return try {
            val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
            // Creamos la parte multipart usando la clave "file" que exige FastAPI
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            Result.success(apiService.uploadDocument(body))
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
}
package pe.khipuai.app.data.remote

import okhttp3.MultipartBody
import pe.khipuai.app.data.remote.dto.*
import retrofit2.http.*

interface KhipuApiService {

    @POST("v1/auth/google")
    suspend fun googleAuth(@Body request: AuthRequest): AuthResponse

    @GET("v1/courses")
    suspend fun getMyCourses(): List<CourseResponse>

    @GET("v1/notes")
    suspend fun getMyNotes(): List<NoteResponse>

    @Multipart
    @POST("v1/uploads")
    suspend fun uploadDocument(
        @Part file: MultipartBody.Part
    ): UploadResponse

    @GET("v1/uploads/{upload_id}")
    suspend fun getUploadStatus(
        @Path("upload_id") uploadId: String
    ): UploadStatusResponse
}
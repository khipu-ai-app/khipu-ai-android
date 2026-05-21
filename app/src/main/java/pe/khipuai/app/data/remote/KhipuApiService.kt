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

    @GET("v1/notes/{note_id}")
    suspend fun getNoteDetail(
        @Path("note_id") noteId: String
    ): NoteDetailResponse

    @GET("v1/notes/{note_id}/study-guide")
    suspend fun getStudyGuide(
        @Path("note_id") noteId: String
    ): StudyGuideResponse

    @GET("v1/planner/today")
    suspend fun getTodayPlanner(): List<StudyBlockResponse>

    @PUT("v1/planner/blocks/{block_id}/tasks/{task_id}/toggle")
    suspend fun toggleTaskStatus(
        @Path("block_id") blockId: String,
        @Path("task_id") taskId: String,
        @Body request: TaskToggleRequest
    ): Unit
}
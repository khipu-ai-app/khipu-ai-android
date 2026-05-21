package pe.khipuai.app.data.remote

import pe.khipuai.app.data.remote.dto.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface KhipuApiService {

    @POST("v1/auth/google")
    suspend fun googleAuth(@Body request: AuthRequest): AuthResponse

    @GET("v1/courses")
    suspend fun getMyCourses(): List<CourseResponse>

    @GET("v1/notes")
    suspend fun getMyNotes(): List<NoteResponse>
}
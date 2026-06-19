package pe.khipuai.app.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import pe.khipuai.app.data.remote.dto.*
import retrofit2.http.*

interface KhipuApiService {

    @POST("v1/auth/google")
    suspend fun googleAuth(@Body request: AuthRequest): AuthResponse

    @POST("v1/auth/register")
    suspend fun registerTraditional(@Body request: UserRegisterRequest): AuthResponse

    @POST("v1/auth/login")
    suspend fun loginTraditional(@Body request: UserLoginRequest): AuthResponse

    @GET("v1/courses/catalog")
    suspend fun getCatalogByProfile(
        @Query("profile_type") profileType: String
    ): List<String>

    @POST("v1/users/me/onboarding")
    suspend fun completeOnboarding(
        @Body request: OnboardingRequest
    ): UserProfileResponse

    @GET("v1/courses")
    suspend fun getMyCourses(): List<CourseResponse>

    @GET("v1/notes")
    suspend fun getMyNotes(): List<NoteResponse>

    @Multipart
    @POST("v1/uploads")
    suspend fun uploadDocument(
        @Part file: MultipartBody.Part,
        @Part("course_id") courseId: RequestBody?
    ): UploadResponse

    @GET("v1/uploads/{upload_id}")
    suspend fun getUploadStatus(
        @Path("upload_id") uploadId: String
    ): UploadStatusResponse

    @GET("v1/notes/{note_id}")
    suspend fun getNoteDetail(
        @Path("note_id") noteId: String
    ): NoteDetailResponse

    @GET("v1/notes/{note_id}/graph")
    suspend fun getNoteLocalGraph(
        @Path("note_id") noteId: String
    ): GraphResponse

    @GET("v1/notes/{note_id}/study-guide")
    suspend fun getStudyGuide(@Path("note_id") noteId: String): StudyGuideResponse

    @GET("v1/notes/{note_id}/review-session")
    suspend fun getNoteReviewSession(@Path("note_id") noteId: String): ReviewSessionResponse

    @GET("v1/notes/{note_id}/review-history")
    suspend fun getNoteReviewHistory(@Path("note_id") noteId: String): List<ReviewHistoryItemResponse>

    @GET("v1/planner/today")
    suspend fun getTodayPlanner(): List<DueConceptResponse>

    @POST("v1/planner/review")
    suspend fun submitConceptReview(
        @Body request: ReviewRequest
    )

    @GET("v1/planner/schedule")
    suspend fun getWeeklySchedule(): List<ScheduleDayResponse>

    @GET("v1/planner/stats")
    suspend fun getPlannerStats(): PlannerStatsResponse

    @GET("v1/graph/course/{course_id}")
    suspend fun getCourseGraph(
        @Path("course_id") courseId: String
    ): GraphResponse

    @GET("v1/graph/concept/{concept_name}")
    suspend fun getConceptDetail(
        @Path("concept_name") conceptName: String
    ): ConceptDetailResponse

    @GET("v1/users/me")
    suspend fun getMyProfile(): UserProfileResponse

    @DELETE("v1/notes/{note_id}")
    suspend fun deleteNote(@Path("note_id") noteId: String)

    @PATCH("v1/notes/{note_id}")
    suspend fun updateNote(
        @Path("note_id") noteId: String,
        @Body request: NoteUpdateRequest
    ): NoteResponse


    @POST("v1/courses")
    suspend fun createCourse(@Body request: CourseCreateRequest): CourseResponse

    @PATCH("v1/courses/{course_id}")
    suspend fun updateCourse(
        @Path("course_id") courseId: String,
        @Body request: CourseUpdateRequest
    ): CourseResponse

    @DELETE("v1/courses/{course_id}")
    suspend fun deleteCourse(@Path("course_id") courseId: String)

    @DELETE("v1/courses/{course_id}/permanent")
    suspend fun deleteCoursePermanently(@Path("course_id") courseId: String)


    @POST("v1/tutor/sessions")
    suspend fun createChatSession(@Body request: ChatSessionCreateRequest): ChatSessionResponse

    @GET("v1/tutor/sessions")
    suspend fun getChatSessions(): List<ChatSessionResponse>

    @GET("v1/tutor/sessions/{session_id}/messages")
    suspend fun getChatMessages(@Path("session_id") sessionId: String): List<ChatMessageResponse>
}
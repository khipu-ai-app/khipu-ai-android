package pe.khipuai.app.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import pe.khipuai.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface KhipuApiService {

    @POST("v1/auth/google")
    suspend fun googleAuth(@Body request: AuthRequest): AuthResponse

    @POST("v1/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): retrofit2.Response<AuthResponse>

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
        @Part("course_id") courseId: RequestBody?,
        // T-17: si es true, el backend ignora el chequeo de hash y
        // permite subir el archivo aunque ya exista una nota con el
        // mismo contenido. El cliente lo envía SOLO cuando el usuario
        // confirma en el dialog de "Documento duplicado" que quiere
        // crear una nueva nota con el mismo archivo.
        @Header("X-Force-Upload") forceUpload: Boolean = false,
    ): UploadResponse

    /**
     * T-18: sube N archivos para crear una nueva nota multi-página.
     */
    /**
     * T-13 evolution: lista los archivos adjuntos a una nota
     * (incluye el primer upload legacy + los adicionales).
     */
    @GET("v1/notes/{note_id}/files")
    suspend fun getNoteFiles(
        @Path("note_id") noteId: String,
    ): NoteFilesListResponse

    @POST("v1/exams/courses/{course_id}/generate")
    suspend fun generateExam(
        @Path("course_id") courseId: String,
        @Body request: ExamGenerateRequest,
    ): ExamGenerateResponse

    @POST("v1/exams/{exam_id}/submit")
    suspend fun submitExam(
        @Path("exam_id") examId: String,
        @Body request: ExamSubmitRequest,
    ): ExamResultResponse

    /**
     * T-13 combine: sube N archivos como una sola nota combinada.
     * El backend corre OCR en cada archivo, pasa el texto combinado
     * a la IA, y crea UNA nota con todos los archivos asociados.
     */
    @Multipart
    @POST("v1/uploads/combine")
    suspend fun combineUpload(
        @Part files: List<MultipartBody.Part>,
        @Part("course_id") courseId: okhttp3.RequestBody?,
    ): CombineUploadResponse

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

    @POST("v1/notes/{note_id}/quiz-result")
    suspend fun submitQuizResult(
        @Path("note_id") noteId: String,
        @Body request: QuizResultRequest
    )

    @POST("v1/notes/{note_id}/generate-quiz")
    suspend fun generateQuiz(
        @Path("note_id") noteId: String,
        @Body request: QuizGenerateRequest
    ): StandaloneQuizResponse

    @GET("v1/notes/{note_id}/quizzes")
    suspend fun getSavedQuizzes(@Path("note_id") noteId: String): List<StandaloneQuizResponse>

    @POST("v1/notes/{note_id}/quizzes")
    suspend fun saveQuiz(
        @Path("note_id") noteId: String,
        @Body request: StandaloneQuizResponse
    )

    @DELETE("v1/notes/{note_id}/quizzes/{quiz_id}")
    suspend fun deleteQuiz(
        @Path("note_id") noteId: String,
        @Path("quiz_id") quizId: String
    )

    @GET("v1/notes/{note_id}/review-session")
    suspend fun getNoteReviewSession(@Path("note_id") noteId: String): ReviewSessionResponse

    @GET("v1/planner/review-session")
    suspend fun getConceptReviewSession(
        @Query("concept_name") conceptName: String
    ): List<ReviewConceptResponse>

    @GET("v1/notes/{note_id}/review-history")
    suspend fun getNoteReviewHistory(@Path("note_id") noteId: String): List<ReviewSessionResponseDto>

    @GET("v1/planner/today")
    suspend fun getTodayPlanner(): List<DueConceptResponse>

    @GET("v1/planner/daily-deck")
    suspend fun getDailyDeckSession(): List<ReviewConceptResponse>

    @POST("v1/planner/review")
    suspend fun submitConceptReview(
        @Body request: ReviewRequest
    )

    @PATCH("v1/planner/concepts/postpone")
    suspend fun postponeConcepts(@Body request: PostponeRequest)

    @GET("v1/planner/schedule")
    suspend fun getWeeklySchedule(): List<ScheduleDayResponse>

    @GET("v1/planner/stats")
    suspend fun getPlannerStats(): PlannerStatsResponse

    // T-11: conceptos agendados/completados para una fecha específica.
    @GET("v1/planner/day")
    suspend fun getDayConcepts(@Query("date") date: String): List<DayConceptResponse>

    @POST("v1/planner/manual-schedule")
    suspend fun createManualSchedule(@Body request: ManualScheduleRequest)

    @GET("v1/planner/manual-schedules")
    suspend fun getManualSchedules(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): List<ManualScheduleItem>

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

    @GET("v1/users/usage")
    suspend fun getUsage(): UsageResponse

    @GET("v1/users/subscription")
    suspend fun getMySubscription(): SubscriptionResponse

    @POST("v1/users/subscription")
    suspend fun updateMySubscription(@Body request: UpdatePlanRequest): SubscriptionResponse

    @PATCH("v1/users/me")
    suspend fun updateMyProfile(@Body request: UserUpdateRequest): UserProfileResponse

    @DELETE("v1/users/me")
    suspend fun deleteMyAccount(@Body request: UserDeleteRequest)

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
    suspend fun getChatSessions(
        @Query("context_type") contextType: String? = null,
        @Query("context_id") contextId: String? = null
    ): List<ChatSessionResponse>

    @GET("v1/tutor/sessions/{session_id}/messages")
    suspend fun getChatMessages(@Path("session_id") sessionId: String): List<ChatMessageResponse>

    @DELETE("v1/tutor/sessions/{session_id}")
    suspend fun deleteChatSession(@Path("session_id") sessionId: String)
    // Search
    @GET("v1/search")
    suspend fun searchGlobal(
        @Query("q") query: String,
        @Query("limit") limit: Int = 10
    ): retrofit2.Response<SearchResponse>
    // Achievements
    @POST("v1/achievements/check")
    suspend fun checkAchievements(): List<AchievementResponse>

    @GET("v1/achievements")
    suspend fun getAchievements(): List<AchievementResponse>

    @DELETE("v1/uploads/{upload_id}")
    suspend fun deleteUpload(@Path("upload_id") uploadId: String)
}

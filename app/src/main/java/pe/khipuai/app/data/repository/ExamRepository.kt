package pe.khipuai.app.data.repository

import pe.khipuai.app.data.remote.KhipuApiService
import pe.khipuai.app.data.remote.dto.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExamRepository @Inject constructor(
    private val apiService: KhipuApiService
) {
    suspend fun generateExam(courseId: String, request: ExamGenerateRequest): Result<ExamGenerateResponse> = runCatching {
        apiService.generateExam(courseId, request)
    }

    suspend fun submitExam(examId: String, answers: List<ExamAnswer>): Result<ExamResultResponse> = runCatching {
        apiService.submitExam(examId, ExamSubmitRequest(answers))
    }
}

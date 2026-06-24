package pe.khipuai.app.data.repository

import pe.khipuai.app.data.remote.KhipuApiService
import pe.khipuai.app.data.remote.dto.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    private val apiService: KhipuApiService
) {
    suspend fun fetchMyNotes(): Result<List<NoteResponse>> {
        return try {
            Result.success(apiService.getMyNotes())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNoteDetail(noteId: String): Result<NoteDetailResponse> {
        return try {
            Result.success(apiService.getNoteDetail(noteId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNoteLocalGraph(noteId: String): Result<GraphResponse> {
        return try {
            Result.success(apiService.getNoteLocalGraph(noteId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchStudyGuide(noteId: String): Result<StudyGuideResponse> {
        return try {
            Result.success(apiService.getStudyGuide(noteId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteNote(noteId: String): Result<Unit> {
        return try {
            apiService.deleteNote(noteId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateNote(noteId: String, title: String?, courseId: String?): Result<NoteResponse> {
        return try {
            Result.success(apiService.updateNote(noteId, NoteUpdateRequest(title = title, courseId = courseId)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNoteReviewSession(noteId: String): Result<ReviewSessionResponse> {
        return try {
            Result.success(apiService.getNoteReviewSession(noteId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getConceptReviewSession(conceptName: String): Result<List<ReviewConceptResponse>> {
        return try {
            Result.success(apiService.getConceptReviewSession(conceptName))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNoteReviewHistory(noteId: String): Result<List<pe.khipuai.app.data.remote.dto.ReviewSessionResponseDto>> {
        return try {
            Result.success(apiService.getNoteReviewHistory(noteId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitQuizResult(noteId: String, result: QuizResultRequest): Result<Unit> {
        return try {
            apiService.submitQuizResult(noteId, result)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateStandaloneQuiz(noteId: String, count: Int, difficulty: String, topics: List<String>): Result<StandaloneQuizResponse> {
        return try {
            val request = QuizGenerateRequest(count = count, difficulty = difficulty, topics = topics)
            Result.success(apiService.generateQuiz(noteId, request))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSavedQuizzes(noteId: String): Result<List<StandaloneQuizResponse>> {
        return try {
            Result.success(apiService.getSavedQuizzes(noteId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveQuiz(noteId: String, quiz: StandaloneQuizResponse): Result<Unit> {
        return try {
            apiService.saveQuiz(noteId, quiz)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteQuiz(noteId: String, quizId: String): Result<Unit> {
        return try {
            apiService.deleteQuiz(noteId, quizId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


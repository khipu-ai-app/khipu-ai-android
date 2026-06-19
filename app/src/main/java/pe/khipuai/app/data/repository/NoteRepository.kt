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

    suspend fun getNoteReviewHistory(noteId: String): Result<List<ReviewHistoryItemResponse>> {
        return try {
            Result.success(apiService.getNoteReviewHistory(noteId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


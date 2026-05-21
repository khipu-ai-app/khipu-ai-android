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

    suspend fun fetchStudyGuide(noteId: String): Result<StudyGuideResponse> {
        return try {
            Result.success(apiService.getStudyGuide(noteId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
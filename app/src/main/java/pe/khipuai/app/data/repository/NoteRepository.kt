package pe.khipuai.app.data.repository

import pe.khipuai.app.data.remote.KhipuApiService
import pe.khipuai.app.data.remote.dto.NoteResponse
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
}
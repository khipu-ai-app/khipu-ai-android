package pe.khipuai.app.data.repository

import pe.khipuai.app.data.remote.KhipuApiService
import pe.khipuai.app.data.remote.dto.StudyBlockResponse
import pe.khipuai.app.data.remote.dto.TaskToggleRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlannerRepository @Inject constructor(
    private val apiService: KhipuApiService
) {
    suspend fun fetchDailyAgenda(): Result<List<StudyBlockResponse>> {
        return try {
            Result.success(apiService.getTodayPlanner())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTaskCompletion(blockId: String, taskId: String, isChecked: Boolean): Result<Unit> {
        return try {
            Result.success(apiService.toggleTaskStatus(blockId, taskId, TaskToggleRequest(isChecked)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
package pe.khipuai.app.data.repository

import pe.khipuai.app.data.remote.KhipuApiService
import pe.khipuai.app.data.remote.dto.GraphResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GraphRepository @Inject constructor(
    private val apiService: KhipuApiService
) {
    suspend fun fetchCourseGraph(courseId: String): Result<GraphResponse> {
        return try {
            Result.success(apiService.getCourseGraph(courseId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
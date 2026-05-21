package pe.khipuai.app.data.repository

import pe.khipuai.app.data.remote.KhipuApiService
import pe.khipuai.app.data.remote.dto.CourseResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseRepository @Inject constructor(
    private val apiService: KhipuApiService
) {
    suspend fun fetchMyCourses(): Result<List<CourseResponse>> {
        return try {
            Result.success(apiService.getMyCourses())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
package pe.khipuai.app.data.repository

import pe.khipuai.app.data.remote.KhipuApiService
import pe.khipuai.app.data.remote.dto.CourseResponse
import pe.khipuai.app.data.remote.dto.OnboardingRequest
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

    suspend fun getCatalog(profileType: String): Result<List<String>> {
        return try {
            Result.success(apiService.getCatalogByProfile(profileType))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitOnboarding(fullName: String, profileType: String, selectedCourses: List<String>): Result<Unit> {
        return try {
            apiService.completeOnboarding(
                OnboardingRequest(
                    fullName = fullName,
                    profileType = profileType,
                    selectedCatalogCourses = selectedCourses,
                    customCourses = emptyList()
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
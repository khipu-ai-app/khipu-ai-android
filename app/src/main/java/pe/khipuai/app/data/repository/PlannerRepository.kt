package pe.khipuai.app.data.repository

import pe.khipuai.app.data.remote.KhipuApiService
import pe.khipuai.app.data.remote.dto.DueConceptResponse
import pe.khipuai.app.data.remote.dto.ReviewRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlannerRepository @Inject constructor(
    private val apiService: KhipuApiService
) {
    suspend fun fetchDailyAgenda(): Result<List<DueConceptResponse>> {
        return try {
            Result.success(apiService.getTodayPlanner())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitReviewRating(conceptName: String, rating: Int): Result<Unit> {
        return try {
            apiService.submitConceptReview(ReviewRequest(conceptName, rating))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
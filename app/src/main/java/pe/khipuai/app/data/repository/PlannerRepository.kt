package pe.khipuai.app.data.repository

import pe.khipuai.app.data.remote.KhipuApiService
import pe.khipuai.app.data.remote.dto.DueConceptResponse
import pe.khipuai.app.data.remote.dto.ManualScheduleItem
import pe.khipuai.app.data.remote.dto.ManualScheduleRequest
import pe.khipuai.app.data.remote.dto.PlannerStatsResponse
import pe.khipuai.app.data.remote.dto.PostponeRequest
import pe.khipuai.app.data.remote.dto.ReviewConceptResponse
import pe.khipuai.app.data.remote.dto.ReviewRequest
import pe.khipuai.app.data.remote.dto.ScheduleDayResponse
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

    suspend fun fetchDailyDeckSession(): Result<List<ReviewConceptResponse>> {
        return try {
            Result.success(apiService.getDailyDeckSession())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitReviewRating(conceptId: String, rating: Int, noteId: String? = null): Result<Unit> {
        return try {
            apiService.submitConceptReview(ReviewRequest(conceptId, rating, noteId))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchWeeklySchedule(): Result<List<ScheduleDayResponse>> {
        return try {
            Result.success(apiService.getWeeklySchedule())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchStats(): Result<PlannerStatsResponse> {
        return try {
            Result.success(apiService.getPlannerStats())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createManualSchedule(noteId: String, scheduledDate: String): Result<Unit> {
        return try {
            apiService.createManualSchedule(ManualScheduleRequest(noteId, scheduledDate))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun postponeConcepts(conceptIds: List<String>, days: Int = 1): Result<Unit> {
        return try {
            apiService.postponeConcepts(PostponeRequest(conceptIds, days))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchManualSchedules(
        fromIso: String? = null,
        toIso: String? = null
    ): Result<List<ManualScheduleItem>> {
        return try {
            Result.success(apiService.getManualSchedules(fromIso, toIso))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
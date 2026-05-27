package pe.khipuai.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── Agenda diaria SM-2 ────────────────────────────────────────────────────

@Serializable
data class DueConceptResponse(
    @SerialName("concept_id") val conceptId: String,
    @SerialName("concept_name") val conceptName: String,
    @SerialName("label") val label: String,
    @SerialName("next_review_date") val nextReviewDate: String,
    @SerialName("interval") val interval: Int,
    @SerialName("repetitions") val repetitions: Int,
    @SerialName("ease_factor") val easeFactor: Float,
    @SerialName("course_name") val courseName: String
)

@Serializable
data class ReviewRequest(
    @SerialName("concept_id") val conceptId: String,
    @SerialName("rating") val rating: Int
)

// ─── Agenda semanal (GET /planner/schedule) ────────────────────────────────

@Serializable
data class ScheduleDayResponse(
    @SerialName("date") val date: String,
    @SerialName("count") val count: Int
)

// ─── Estadísticas del planner (GET /planner/stats) ────────────────────────

@Serializable
data class CourseDistributionItem(
    @SerialName("course_name") val courseName: String,
    @SerialName("count") val count: Int
)

@Serializable
data class PlannerStatsResponse(
    @SerialName("streak_days") val streakDays: Int,
    @SerialName("total_concepts") val totalConcepts: Int,
    @SerialName("dominated_concepts") val dominatedConcepts: Int,
    @SerialName("mastery_percentage") val masteryPercentage: Int,
    @SerialName("course_distribution") val courseDistribution: List<CourseDistributionItem>
)
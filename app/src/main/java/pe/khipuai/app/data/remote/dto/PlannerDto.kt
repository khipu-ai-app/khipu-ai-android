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
    @SerialName("course_name") val courseName: String,
    @SerialName("note_id") val noteId: String? = null,
    // T-13: estado del concepto en el día de hoy. El backend envía estos
    // flags para que la UI pueda mostrar los conceptos ya repasados
    // (con un check verde y el rating) en lugar de hacerlos desaparecer.
    @SerialName("is_due") val isDue: Boolean = true,
    @SerialName("reviewed_today") val reviewedToday: Boolean = false,
    @SerialName("last_rating") val lastRating: Int? = null,
)

@Serializable
data class ReviewRequest(
    @SerialName("concept_id") val conceptId: String,
    @SerialName("rating") val rating: Int,
    @SerialName("note_id") val noteId: String? = null,
)

// ─── Agenda semanal (GET /planner/schedule) ────────────────────────────────

@Serializable
data class ScheduleDayResponse(
    @SerialName("date") val date: String,
    @SerialName("count") val count: Int
)

/**
 * T-11: respuesta de `GET /planner/day?date=YYYY-MM-DD`. Un concepto
 * agendado (futuro) o completado (pasado) para una fecha específica.
 */
@Serializable
data class DayConceptResponse(
    @SerialName("concept_id") val conceptId: String,
    @SerialName("concept_name") val conceptName: String,
    @SerialName("course_name") val courseName: String = "",
    @SerialName("note_id") val noteId: String? = null,
    @SerialName("completed") val completed: Boolean = false
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

// ─── Calendarización manual (POST /planner/manual-schedule) ────────────────

@Serializable
data class ManualScheduleRequest(
    @SerialName("note_id") val noteId: String,
    @SerialName("scheduled_date") val scheduledDate: String  // formato YYYY-MM-DD
)

@Serializable
data class ManualScheduleItem(
    @SerialName("id") val id: String,
    @SerialName("note_id") val noteId: String,
    @SerialName("note_title") val noteTitle: String,
    @SerialName("scheduled_date") val scheduledDate: String  // formato YYYY-MM-DD
)

@Serializable
data class PostponeRequest(
    @SerialName("concept_ids") val conceptIds: List<String>,
    @SerialName("days") val days: Int
)
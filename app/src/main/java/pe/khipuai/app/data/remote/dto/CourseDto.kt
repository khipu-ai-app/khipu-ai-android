package pe.khipuai.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OnboardingRequest(
    @SerialName("full_name") val fullName: String,
    @SerialName("profile_type") val profileType: String,
    @SerialName("selected_catalog_courses") val selectedCatalogCourses: List<String>,
    @SerialName("custom_courses") val customCourses: List<String>
)

@Serializable
data class CourseResponse(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("color") val color: String? = "#7F7F7F",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("is_from_catalog") val isFromCatalog: Boolean = false,
    @SerialName("catalog_key") val catalogKey: String? = null,
    // C-04
    @SerialName("exam_date") val examDate: String? = null,
)


@Serializable
data class CourseCreateRequest(
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("color") val color: String? = "#7F7F7F"
)

@Serializable
data class CourseUpdateRequest(
    @SerialName("name") val name: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("color") val color: String? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
    // C-04
    @SerialName("exam_date") val examDate: String? = null,
)

@Serializable
data class CourseDeleteInfoResponse(
    @SerialName("course_name") val courseName: String,
    @SerialName("notes_count") val notesCount: Int,
    @SerialName("concepts_count") val conceptsCount: Int,
    @SerialName("has_review_history") val hasReviewHistory: Boolean,
)

@Serializable
data class RescheduleForExamRequest(
    @SerialName("exam_date") val examDate: String,
)

@Serializable
data class ScheduleDay(
    @SerialName("date") val date: String,
    @SerialName("concepts") val concepts: Int,
)

@Serializable
data class RescheduleForExamResponse(
    @SerialName("concepts_rescheduled") val conceptsRescheduled: Int,
    @SerialName("schedule") val schedule: List<ScheduleDay>,
)
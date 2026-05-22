package pe.khipuai.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DueConceptResponse(
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
    @SerialName("concept_name") val conceptName: String,
    @SerialName("rating") val rating: Int
)
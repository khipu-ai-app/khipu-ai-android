package pe.khipuai.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchNoteResult(
    @SerialName("note_id") val note_id: String,
    @SerialName("note_title") val note_title: String,
    @SerialName("course_name") val course_name: String,
    @SerialName("snippet") val snippet: String,
    @SerialName("relevance_score") val relevance_score: Float,
    @SerialName("created_at") val created_at: String
)

@Serializable
data class SearchConceptResult(
    @SerialName("concept_name") val concept_name: String,
    @SerialName("label") val label: String,
    @SerialName("review_pending") val review_pending: Boolean,
    @SerialName("course_name") val course_name: String
)

@Serializable
data class SearchResponse(
    @SerialName("notes") val notes: List<SearchNoteResult>,
    @SerialName("concepts") val concepts: List<SearchConceptResult>
)

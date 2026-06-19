package pe.khipuai.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReviewConceptResponse(
    @SerialName("concept_id") val conceptId: String,
    @SerialName("concept_name") val conceptName: String,
    @SerialName("label") val label: String,
    @SerialName("definition") val definition: String? = null,
    @SerialName("source_note_title") val sourceNoteTitle: String? = null,
    @SerialName("source_snippet") val sourceSnippet: String? = null,
    @SerialName("ease_factor") val easeFactor: Float = 2.5f,
    @SerialName("interval") val interval: Int = 0,
    @SerialName("repetitions") val repetitions: Int = 0,
    @SerialName("next_review_date") val nextReviewDate: String = "",
    @SerialName("is_due") val isDue: Boolean = true,
)

@Serializable
data class ReviewSessionResponse(
    @SerialName("note_id") val noteId: String,
    @SerialName("note_title") val noteTitle: String,
    @SerialName("course_name") val courseName: String? = null,
    @SerialName("note_summary") val noteSummary: String = "",
    @SerialName("note_content") val noteContent: String = "",
    @SerialName("flashcards") val flashcards: List<FlashcardDto> = emptyList(),
    @SerialName("questions") val questions: List<PracticeQuestionDto> = emptyList(),
    @SerialName("concepts") val concepts: List<ReviewConceptResponse> = emptyList(),
    @SerialName("total_concepts") val totalConcepts: Int = 0,
)


@Serializable
data class ReviewHistoryItemResponse(
    @SerialName("id") val id: String,
    @SerialName("concept_name") val conceptName: String,
    @SerialName("rating") val rating: Int,
    @SerialName("reviewed_at") val reviewedAt: String,
)

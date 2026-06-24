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

/**
 * T-06: respuesta del endpoint `GET /notes/{id}/review-history`.
 * El backend agrupa los repasos por fecha y devuelve una lista de sesiones
 * en vez de items individuales.
 */
@Serializable
data class ReviewSessionResponseDto(
    @SerialName("session_key") val sessionKey: String,
    @SerialName("date_label") val dateLabel: String,
    @SerialName("average_rating") val averageRating: Double,
    @SerialName("concepts_reviewed") val conceptsReviewed: Int,
    @SerialName("next_review_date") val nextReviewDate: String? = null,
    @SerialName("concepts") val concepts: List<ReviewConceptItemDto> = emptyList()
)

@Serializable
data class ReviewConceptItemDto(
    @SerialName("concept_name") val conceptName: String,
    @SerialName("rating") val rating: Int,
    @SerialName("next_review_date") val nextReviewDate: String? = null
)

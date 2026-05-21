package pe.khipuai.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FlashcardDto(
    @SerialName("question") val question: String,
    @SerialName("answer") val answer: String
)

@Serializable
data class PracticeQuestionDto(
    @SerialName("id") val id: Int,
    @SerialName("question") val question: String,
    @SerialName("options") val options: List<String>
)

@Serializable
data class StudyGuideResponse(
    @SerialName("title") val title: String,
    @SerialName("date") val date: String,
    @SerialName("executive_summary") val executiveSummary: String,
    @SerialName("glossary") val glossary: String,
    @SerialName("flashcards") val flashcards: List<FlashcardDto>,
    @SerialName("questions") val questions: List<PracticeQuestionDto>
)
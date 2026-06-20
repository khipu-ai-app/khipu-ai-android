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
    @SerialName("options") val options: List<String>,
    @SerialName("correct_index") val correctIndex: Int,
    @SerialName("explanation") val explanation: String
)

@Serializable
data class GlossaryTermDto(
    @SerialName("term") val term: String,
    @SerialName("definition") val definition: String
)

@Serializable
data class StudyGuideResponse(
    @SerialName("title") val title: String,
    @SerialName("date") val date: String,
    @SerialName("executive_summary") val executiveSummary: String,
    @SerialName("glossary") val glossary: List<GlossaryTermDto>,
    @SerialName("flashcards") val flashcards: List<FlashcardDto>,
    @SerialName("questions") val questions: List<PracticeQuestionDto>
)

@Serializable
data class QuizResultRequest(
    @SerialName("score") val score: Int,
    @SerialName("total") val total: Int,
    @SerialName("percentage") val percentage: Float
)

@Serializable
data class StandaloneQuestionDto(
    @SerialName("id") val id: String,
    @SerialName("question") val question: String,
    @SerialName("options") val options: List<String>,
    @SerialName("correct_index") val correctIndex: Int,
    @SerialName("explanation") val explanation: String = ""
)

@Serializable
data class StandaloneQuizResponse(
    @SerialName("id") val id: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("topics") val topics: List<String> = emptyList(),
    @SerialName("difficulty") val difficulty: String = "Intermedio",
    @SerialName("score") val score: Int? = null,
    @SerialName("questions") val questions: List<StandaloneQuestionDto>
)

@Serializable
data class QuizGenerateRequest(
    @SerialName("count") val count: Int = 5,
    @SerialName("difficulty") val difficulty: String = "Intermedio",
    @SerialName("topics") val topics: List<String> = emptyList()
)
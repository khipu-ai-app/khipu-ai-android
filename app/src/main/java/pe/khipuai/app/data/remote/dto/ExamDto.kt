package pe.khipuai.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExamGenerateRequest(
    @SerialName("question_count") val questionCount: Int = 10,
    @SerialName("duration_minutes") val durationMinutes: Int = 15,
    @SerialName("topics") val topics: List<String> = emptyList(),
    @SerialName("difficulty") val difficulty: String = "mixed",
)

@Serializable
data class ExamQuestionResponse(
    @SerialName("id") val id: String,
    @SerialName("question") val question: String,
    @SerialName("options") val options: List<String>,
    @SerialName("topic") val topic: String = "General",
)

@Serializable
data class ExamGenerateResponse(
    @SerialName("exam_id") val examId: String,
    @SerialName("questions") val questions: List<ExamQuestionResponse>,
    @SerialName("duration_minutes") val durationMinutes: Int,
    @SerialName("expires_at") val expiresAt: String,
)

@Serializable
data class ExamAnswer(
    @SerialName("question_id") val questionId: String,
    @SerialName("selected_index") val selectedIndex: Int,
)

@Serializable
data class ExamSubmitRequest(
    @SerialName("answers") val answers: List<ExamAnswer>,
)

@Serializable
data class ExamQuestionResult(
    @SerialName("question_id") val questionId: String,
    @SerialName("question") val question: String,
    @SerialName("options") val options: List<String>,
    @SerialName("correct") val correct: Boolean,
    @SerialName("correct_index") val correctIndex: Int,
    @SerialName("selected_index") val selectedIndex: Int,
    @SerialName("topic") val topic: String,
)

@Serializable
data class ExamResultResponse(
    @SerialName("exam_id") val examId: String,
    @SerialName("score") val score: Int,
    @SerialName("total") val total: Int,
    @SerialName("percentage") val percentage: Float,
    @SerialName("time_spent_seconds") val timeSpentSeconds: Int,
    @SerialName("results") val results: List<ExamQuestionResult>,
    @SerialName("topic_breakdown") val topicBreakdown: Map<String, TopicBreakdown>,
    @SerialName("weak_concepts") val weakConcepts: List<String>,
)

@Serializable
data class TopicBreakdown(
    @SerialName("correct") val correct: Int,
    @SerialName("total") val total: Int,
    @SerialName("percentage") val percentage: Float,
)

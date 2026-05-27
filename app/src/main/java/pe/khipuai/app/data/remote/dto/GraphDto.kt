package pe.khipuai.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── Graph Course Response ─────────────────────────────────────────────────

@Serializable
data class NetworkNode(
    @SerialName("id") val id: String,
    @SerialName("label") val label: String,
    @SerialName("type") val type: String,        // "course" | "note" | "concept"
    @SerialName("review_pending") val reviewPending: Boolean? = false,
    @SerialName("ease_factor") val easeFactor: Float? = 2.5f,
    @SerialName("course_color") val courseColor: String? = null
)

@Serializable
data class NetworkEdge(
    @SerialName("source") val source: String,
    @SerialName("target") val target: String,
    @SerialName("type") val type: String,        // "HAS_NOTE" | "MENTIONS" | "RELATED_TO"
    @SerialName("weight") val weight: Float? = 1.0f,
    @SerialName("strength") val strength: Float? = 1.0f
)

@Serializable
data class GraphResponse(
    @SerialName("nodes") val nodes: List<NetworkNode>,
    @SerialName("edges") val edges: List<NetworkEdge>
)

// ─── Concept Detail Response (GET /graph/concept/{name}) ──────────────────

@Serializable
data class NeighborNode(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("strength") val strength: Float? = 1.0f
)

@Serializable
data class MentionedNote(
    @SerialName("note_id") val noteId: String,
    @SerialName("title") val title: String
)

@Serializable
data class ConceptDetailResponse(
    @SerialName("concept_id") val conceptId: String,
    @SerialName("concept_name") val conceptName: String,
    @SerialName("ease_factor") val easeFactor: Float,
    @SerialName("interval") val interval: Int,
    @SerialName("repetitions") val repetitions: Int,
    @SerialName("review_pending") val reviewPending: Boolean,
    @SerialName("neighbors") val neighbors: List<NeighborNode>,
    @SerialName("mentioned_in") val mentionedIn: List<MentionedNote>
)
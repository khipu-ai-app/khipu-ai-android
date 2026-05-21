package pe.khipuai.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkNode(
    @SerialName("id") val id: String,
    @SerialName("label") val label: String,
    @SerialName("type") val type: String, // "course", "note", "concept"
    @SerialName("review_pending") val reviewPending: Boolean? = false,
    @SerialName("ease_factor") val easeFactor: Float? = 2.5f
)

@Serializable
data class NetworkEdge(
    @SerialName("source") val source: String,
    @SerialName("target") val target: String,
    @SerialName("type") val type: String, // "HAS_NOTE", "MENTIONS", "RELATED_TO"
    @SerialName("weight") val weight: Float? = 1.0f,
    @SerialName("strength") val strength: Float? = 1.0f
)

@Serializable
data class GraphResponse(
    @SerialName("nodes") val nodes: List<NetworkNode>,
    @SerialName("edges") val edges: List<NetworkEdge>
)
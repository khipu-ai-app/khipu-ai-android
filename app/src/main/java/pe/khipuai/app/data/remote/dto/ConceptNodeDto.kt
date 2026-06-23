package pe.khipuai.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConceptNodeDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("neo4j_id") val neo4jId: String,
    @SerialName("category") val category: String = "Concepto",
    @SerialName("is_important") val isImportant: Boolean = false
)

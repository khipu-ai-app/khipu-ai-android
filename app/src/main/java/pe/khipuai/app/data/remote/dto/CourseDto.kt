package pe.khipuai.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CourseResponse(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("color") val color: String
)
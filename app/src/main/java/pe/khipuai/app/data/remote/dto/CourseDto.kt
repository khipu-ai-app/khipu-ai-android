package pe.khipuai.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OnboardingRequest(
    @SerialName("full_name") val fullName: String,
    @SerialName("profile_type") val profileType: String,
    @SerialName("selected_catalog_courses") val selectedCatalogCourses: List<String>,
    @SerialName("custom_courses") val customCourses: List<String>
)

@Serializable
data class CourseResponse(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("color") val color: String
)

@Serializable
data class CourseCreateRequest(
    @SerialName("name") val name: String,
    @SerialName("color") val color: String? = "#7F7F7F"
)

@Serializable
data class CourseUpdateRequest(
    @SerialName("name") val name: String? = null,
    @SerialName("color") val color: String? = null,
    @SerialName("is_active") val isActive: Boolean? = null
)
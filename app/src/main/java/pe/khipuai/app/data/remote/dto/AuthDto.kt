package pe.khipuai.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    @SerialName("id_token") val idToken: String
)

@Serializable
data class AuthResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String
)

@Serializable
data class UserProfileResponse(
    @SerialName("email") val email: String,
    @SerialName("full_name") val fullName: String?,
    @SerialName("profile_type") val profileType: String?
)
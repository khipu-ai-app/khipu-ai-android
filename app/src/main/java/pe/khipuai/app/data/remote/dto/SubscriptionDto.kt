package pe.khipuai.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionResponse(
    val plan: String,
    @SerialName("is_pro") val isPro: Boolean,
    @SerialName("expires_at") val expiresAt: String? = null
)

@Serializable
data class UpdatePlanRequest(
    val plan: String
)

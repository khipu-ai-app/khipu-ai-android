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
    @SerialName("profile_type") val profileType: String?,
    @SerialName("university") val university: String? = null,
    @SerialName("career") val career: String? = null,
    @SerialName("semester") val semester: Int? = null,
    @SerialName("study_goal_minutes") val studyGoalMinutes: Int? = null,
    @SerialName("study_days") val studyDays: List<Int>? = null,
    @SerialName("language") val language: String? = null,
    @SerialName("notification_preferences") val notificationPreferences: NotificationPreferencesDto? = null
)

@Serializable
data class UserRegisterRequest(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String,
    @SerialName("full_name") val fullName: String? = null
)

@Serializable
data class UserLoginRequest(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String
)

@Serializable
data class NotificationPreferencesDto(
    @SerialName("enabled") val enabled: Boolean? = null,
    @SerialName("review_reminders") val reviewReminders: Boolean? = null,
    @SerialName("processing_complete") val processingComplete: Boolean? = null,
    @SerialName("achievements") val achievements: Boolean? = null,
    @SerialName("reminder_hour") val reminderHour: Int? = null
)

@Serializable
data class UserUpdateRequest(
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("university") val university: String? = null,
    @SerialName("career") val career: String? = null,
    @SerialName("semester") val semester: Int? = null,
    @SerialName("study_goal_minutes") val studyGoalMinutes: Int? = null,
    @SerialName("study_days") val studyDays: List<Int>? = null,
    @SerialName("language") val language: String? = null,
    @SerialName("notification_preferences") val notificationPreferences: NotificationPreferencesDto? = null,
    @SerialName("fcm_token") val fcmToken: String? = null
)

@Serializable
data class UserDeleteRequest(
    @SerialName("password") val password: String
)

@Serializable
data class UsageResponse(
    @SerialName("captures_used") val capturesUsed: Int,
    @SerialName("captures_limit") val capturesLimit: Int,
    @SerialName("is_pro") val isPro: Boolean
)

@Serializable
data class RefreshTokenRequest(
    @SerialName("access_token") val accessToken: String
)
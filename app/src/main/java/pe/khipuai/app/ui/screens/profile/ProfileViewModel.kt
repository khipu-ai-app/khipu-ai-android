package pe.khipuai.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.khipuai.app.data.repository.AuthRepository
import javax.inject.Inject

data class ProfileUiState(
    val userName: String = "Cargando...",
    val email: String = "",
    val career: String = "Buscando información...",
    val university: String = "Universidad Nacional de San Agustín",
    val semester: Int? = null,
    val studyGoalMinutes: Int = 45,
    val studyDays: List<Int> = listOf(0,1,2,3,4,5,6),
    val isPro: Boolean = false,
    val isDarkMode: Boolean = false,
    val language: String = "Español",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            authRepository.fetchMyProfile()
                .onSuccess { profile ->
                    _uiState.value = _uiState.value.copy(
                        userName = profile.fullName ?: "Estudiante",
                        email = profile.email,
                        career = profile.career ?: profile.profileType?.replaceFirstChar { it.uppercase() } ?: "General",
                        university = profile.university ?: "No especificada",
                        semester = profile.semester,
                        studyGoalMinutes = profile.studyGoalMinutes ?: 45,
                        studyDays = profile.studyDays ?: listOf(0,1,2,3,4,5,6),
                        language = profile.language ?: "Español",
                        isPro = false,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        userName = "Estudiante Invitado",
                        career = "Desarrollo Local",
                        errorMessage = error.localizedMessage,
                        isLoading = false
                    )
                }
        }
    }

    fun updateProfile(
        fullName: String? = null,
        university: String? = null,
        career: String? = null,
        semester: Int? = null,
        studyGoalMinutes: Int? = null,
        studyDays: List<Int>? = null,
        language: String? = null,
        notificationPreferences: pe.khipuai.app.data.remote.dto.NotificationPreferencesDto? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null, successMessage = null)
            val request = pe.khipuai.app.data.remote.dto.UserUpdateRequest(
                fullName = fullName,
                university = university,
                career = career,
                semester = semester,
                studyGoalMinutes = studyGoalMinutes,
                studyDays = studyDays,
                language = language,
                notificationPreferences = notificationPreferences
            )
            authRepository.updateMyProfile(request)
                .onSuccess { profile ->
                    _uiState.value = _uiState.value.copy(
                        userName = profile.fullName ?: "Estudiante",
                        career = profile.career ?: profile.profileType?.replaceFirstChar { it.uppercase() } ?: "General",
                        university = profile.university ?: "No especificada",
                        semester = profile.semester,
                        studyGoalMinutes = profile.studyGoalMinutes ?: 45,
                        studyDays = profile.studyDays ?: listOf(0,1,2,3,4,5,6),
                        language = profile.language ?: "Español",
                        isSaving = false,
                        successMessage = "Perfil actualizado correctamente"
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = "Error al actualizar perfil: ${error.localizedMessage}"
                    )
                }
        }
    }

    fun deleteAccount(password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            authRepository.deleteMyAccount(password)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    authRepository.logout()
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = "Contraseña incorrecta o error de conexión."
                    )
                }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    fun toggleDarkMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isDarkMode = enabled)
    }

    fun logout(onLogoutSuccess: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLogoutSuccess()
        }
    }

    init {
        loadUserProfile()
    }
}

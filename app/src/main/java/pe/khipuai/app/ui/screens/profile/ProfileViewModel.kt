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
    val career: String = "Buscando información...",
    val university: String = "Universidad Nacional de San Agustín",
    val isPro: Boolean = false,
    val isDarkMode: Boolean = false,
    val language: String = "Español",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            authRepository.fetchMyProfile()
                .onSuccess { profile ->
                    _uiState.value = _uiState.value.copy(
                        userName = profile.fullName ?: "Estudiante UNSA",
                        // Capitalizamos el tipo de perfil (ej: "ingenieria" -> "Ingenieria")
                        career = profile.profileType?.replaceFirstChar { it.uppercase() } ?: "General",
                        isPro = true, // Vinculado a tu cuenta mock de desarrollo
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

    fun toggleDarkMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isDarkMode = enabled)
    }

    fun logout(onLogoutSuccess: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLogoutSuccess()
        }
    }
}
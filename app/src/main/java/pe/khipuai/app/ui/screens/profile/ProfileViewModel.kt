package pe.khipuai.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val userName: String = "Estudiante",
    val career: String = "Ingeniería Informática",
    val university: String = "Universidad Nacional",
    val isPro: Boolean = true,
    val isDarkMode: Boolean = false,
    val language: String = "Español",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    // TODO: Inject UserRepository, PreferencesRepository when implemented
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDarkMode = enabled)
            // TODO: Save preference to DataStore
        }
    }
    
    fun updateLanguage(language: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(language = language)
            // TODO: Save preference to DataStore
        }
    }
    
    fun updateUserInfo(name: String, career: String, university: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                userName = name,
                career = career,
                university = university
            )
            // TODO: Save to backend
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            // TODO: Implement logout logic
            // Clear user data
            // Navigate to login
        }
    }
}
package pe.khipuai.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.khipuai.app.data.repository.AuthRepository
import javax.inject.Inject

data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val isResetSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(email = email, errorMessage = null)
    }

    fun submitResetRequest() {
        val currentEmail = _uiState.value.email
        if (currentEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(currentEmail).matches()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Ingresa un correo válido")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            authRepository.forgotPassword(currentEmail).onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = true
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = pe.khipuai.app.core.network.NetworkErrorMapper.from(e).message
                )
            }
        }
    }
    
    fun submitNewPassword(code: String, newPassword: String) {
        val currentEmail = _uiState.value.email
        if (code.isBlank() || newPassword.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Ingresa el código y la nueva contraseña")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            authRepository.resetPassword(currentEmail, code, newPassword).onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isResetSuccess = true
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = pe.khipuai.app.core.network.NetworkErrorMapper.from(e).message
                )
            }
        }
    }
    
    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

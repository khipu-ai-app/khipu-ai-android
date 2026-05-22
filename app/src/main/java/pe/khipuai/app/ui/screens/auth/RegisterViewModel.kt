package pe.khipuai.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.khipuai.app.data.repository.AuthRepository
import javax.inject.Inject

data class RegisterUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRegistered: Boolean = false
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun register(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
        onResult: (Boolean) -> Unit
    ) {
        // 1. Validación local previa antes de gastar recursos de red
        val validationError = validateInputs(name, email, password, confirmPassword)
        if (validationError != null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = validationError
            )
            onResult(false)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            // 2. Disparar el registro real por HTTP hacia PostgreSQL vía AuthRepository
            authRepository.registerWithEmail(email, password, name)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false, isRegistered = true)
                    onResult(true)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.localizedMessage ?: "El correo ya existe o el servidor no responde."
                    )
                    onResult(false)
                }
        }
    }

    fun signInWithGoogle(idToken: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = authRepository.loginWithGoogle(idToken)

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRegistered = true
                )
                onResult(true)
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error en Khipu Server: ${exception.localizedMessage ?: "Servidor inaccesible"}"
                )
                onResult(false)
            }
        }
    }

    private fun validateInputs(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): String? {
        return when {
            name.isBlank() -> "Por favor ingresa tu nombre completo"
            email.isBlank() -> "Por favor ingresa tu email"
            password.isBlank() -> "Por favor ingresa una contraseña"
            confirmPassword.isBlank() -> "Por favor confirma tu contraseña"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Por favor ingresa un email válido"
            password.length < 8 -> "La contraseña debe tener al menos 8 caracteres"
            password != confirmPassword -> "Las contraseñas no coinciden"
            else -> null
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
package pe.khipuai.app.ui.screens.auth

import android.content.Context

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.khipuai.app.R
import pe.khipuai.app.core.auth.GoogleSignInHelper
import pe.khipuai.app.core.auth.GoogleSignInResult
import pe.khipuai.app.data.repository.AuthRepository
import javax.inject.Inject

data class RegisterUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRegistered: Boolean = false
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    application: Application,
    private val authRepository: AuthRepository,
    private val googleSignInHelper: GoogleSignInHelper
) : AndroidViewModel(application) {

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
            // No limpiamos errorMessage aquí: si el usuario reintenta, el Card
            // de error se mantiene y se actualiza con el nuevo resultado, sin
            // parpadeo. Solo se limpia en éxito.
            _uiState.value = _uiState.value.copy(isLoading = true)

            // 2. Disparar el registro real por HTTP hacia PostgreSQL vía AuthRepository
            authRepository.registerWithEmail(email, password, name)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false, isRegistered = true, errorMessage = null)
                    onResult(true)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = pe.khipuai.app.core.network.NetworkErrorMapper.from(exception).message
                    )
                    onResult(false)
                }
        }
    }

    /**
     * T-09: en RegisterScreen, "Iniciar con Google" significa realmente
     * "login con Google" (los usuarios nuevos de Google se crean
     * automáticamente en el backend en `POST /auth/google`).
     */
    fun signInWithGoogle(context: Context, onResult: (success: Boolean, needsOnboarding: Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val webClientId = getApplication<Application>()
                .getString(R.string.google_web_client_id)

            when (val result = googleSignInHelper.signIn(context, webClientId)) {
                is GoogleSignInResult.Success -> {
                    authRepository.loginWithGoogle(result.idToken)
                        .onSuccess {
                            val profileResult = authRepository.getMyProfile()
                            val needsOnboarding = profileResult.getOrNull()?.university.isNullOrBlank()
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isRegistered = true,
                                errorMessage = null
                            )
                            onResult(true, needsOnboarding)
                        }
                        .onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = pe.khipuai.app.core.network.NetworkErrorMapper.from(exception).message
                            )
                            onResult(false, false)
                        }
                }
                GoogleSignInResult.UserCancelled -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onResult(false, false)
                }
                GoogleSignInResult.NoCredentials -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No tienes ninguna cuenta de Google configurada en este dispositivo."
                    )
                    onResult(false, false)
                }
                GoogleSignInResult.MalformedCredential -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No se pudo leer la credencial de Google. Intenta de nuevo."
                    )
                    onResult(false, false)
                }
                is GoogleSignInResult.Failed -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No se pudo iniciar sesión con Google. Intenta con email y contraseña."
                    )
                    onResult(false, false)
                }
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
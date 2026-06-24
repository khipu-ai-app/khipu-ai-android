package pe.khipuai.app.ui.screens.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.khipuai.app.R
import pe.khipuai.app.core.auth.GoogleSignInHelper
import pe.khipuai.app.core.auth.GoogleSignInResult
import pe.khipuai.app.data.repository.AuthRepository
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    application: Application,
    private val authRepository: AuthRepository,
    private val googleSignInHelper: GoogleSignInHelper
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun login(onResult: (Boolean) -> Unit) {
        val email = _uiState.value.email
        val password = _uiState.value.password
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Por favor completa todos los campos")
            onResult(false)
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Por favor ingresa un email válido")
            onResult(false)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            authRepository.loginWithEmail(email, password)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false, isLoggedIn = true, errorMessage = null)
                    onResult(true)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = pe.khipuai.app.core.network.NetworkErrorMapper.from(exception, custom401 = "Usuario o contraseña incorrectos.").message
                    )
                    onResult(false)
                }
        }
    }

    /**
     * T-09: dispara el flujo de Google Sign-In con CredentialManager.
     *
     * Llama al [GoogleSignInHelper] para obtener el idToken, luego lo
     * envía al backend via [AuthRepository.loginWithGoogle]. Los errores
     * específicos del sheet (cancel, no credentials) se traducen a
     * mensajes en español para el usuario.
     */
    fun signInWithGoogle(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val webClientId = getApplication<Application>()
                .getString(R.string.google_web_client_id)

            when (val result = googleSignInHelper.signIn(webClientId)) {
                is GoogleSignInResult.Success -> {
                    authRepository.loginWithGoogle(result.idToken)
                        .onSuccess {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isLoggedIn = true,
                                errorMessage = null
                            )
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
                GoogleSignInResult.UserCancelled -> {
                    // El usuario canceló voluntariamente: no mostramos error.
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onResult(false)
                }
                GoogleSignInResult.NoCredentials -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No tienes ninguna cuenta de Google configurada en este dispositivo."
                    )
                    onResult(false)
                }
                GoogleSignInResult.MalformedCredential -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No se pudo leer la credencial de Google. Intenta de nuevo."
                    )
                    onResult(false)
                }
                is GoogleSignInResult.Failed -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No se pudo iniciar sesión con Google. Intenta con email y contraseña."
                    )
                    onResult(false)
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    // DEV-ONLY: usado por los botones de quick login del LoginScreen para
    // acceder como free@khipuai.app o pro@khipuai.app sin tipear credenciales.
    // La password está hardcoded en el seed del backend (KhipuTest1234).
    fun quickLogin(email: String, password: String = "KhipuTest1234", onResult: (Boolean) -> Unit) {
        _uiState.value = _uiState.value.copy(email = email, password = password)
        login(onResult)
    }
}

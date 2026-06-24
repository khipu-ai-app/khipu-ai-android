package pe.khipuai.app.core.auth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import pe.khipuai.app.core.datastore.SessionDataStore
import pe.khipuai.app.data.remote.KhipuApiService
import pe.khipuai.app.data.remote.dto.RefreshTokenRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * T-07: decide el destino inicial de la app al arrancar.
 *
 *   - Sin token → [AuthStartupState.NeedsLogin] (la app abre en Login)
 *   - Token válido (no expirado) → [AuthStartupState.LoggedIn] (directo a Home)
 *   - Token expirado → intenta `POST /auth/refresh`:
 *       - Refresh OK → [AuthStartupState.LoggedIn]
 *       - Refresh fail → limpia tokens + [AuthStartupState.NeedsLogin]
 *
 * El estado inicial es [AuthStartupState.Loading] mientras decide. El
 * [MainActivity] lo observa y, según el resultado, configura el
 * start destination del NavHost.
 */
sealed class AuthStartupState {
    data object Loading : AuthStartupState()
    data object NeedsLogin : AuthStartupState()
    data object LoggedIn : AuthStartupState()
}

@Singleton
class AuthStartupChecker @Inject constructor(
    private val sessionDataStore: SessionDataStore,
    private val apiService: KhipuApiService
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _state = MutableStateFlow<AuthStartupState>(AuthStartupState.Loading)
    val state: StateFlow<AuthStartupState> = _state.asStateFlow()

    /**
     * Llamar una sola vez al inicio (en `MainActivity.onCreate` o en el
     * `Composable` raíz). Decide el destino y emite el resultado al flow.
     *
     * Idempotente: si ya se ejecutó, no hace nada.
     */
    fun runOnce() {
        if (_state.value !is AuthStartupState.Loading) return
        scope.launch {
            _state.value = decide()
        }
    }

    /**
     * Llamar después de login/refresh/logout para resetear el state y
     * permitir que el siguiente arranque vuelva a decidir.
     */
    fun reset() {
        _state.value = AuthStartupState.Loading
    }

    private suspend fun decide(): AuthStartupState {
        val token = sessionDataStore.tokenFlow.firstOrNull()
        if (token.isNullOrEmpty()) {
            return AuthStartupState.NeedsLogin
        }
        if (!JwtUtils.isExpired(token)) {
            return AuthStartupState.LoggedIn
        }
        // Token expirado: intentar refresh
        return try {
            val resp = apiService.refreshToken(RefreshTokenRequest(token))
            if (resp.isSuccessful) {
                val fresh = resp.body()?.accessToken
                if (!fresh.isNullOrEmpty()) {
                    sessionDataStore.saveToken(fresh)
                    AuthStartupState.LoggedIn
                } else {
                    sessionDataStore.clearToken()
                    AuthStartupState.NeedsLogin
                }
            } else {
                sessionDataStore.clearToken()
                AuthStartupState.NeedsLogin
            }
        } catch (_: Exception) {
            sessionDataStore.clearToken()
            AuthStartupState.NeedsLogin
        }
    }
}

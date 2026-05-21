package pe.khipuai.app.data.repository

import kotlinx.coroutines.flow.Flow
import pe.khipuai.app.data.local.TokenManager
import pe.khipuai.app.data.remote.KhipuApiService
import pe.khipuai.app.data.remote.dto.AuthRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: KhipuApiService,
    private val tokenManager: TokenManager
) {

    // Expone el estado del token para saber si el usuario ya está logueado o no
    val isLoggedIn: Flow<String?> = tokenManager.accessToken

    suspend fun loginWithGoogle(idToken: String): Result<Unit> {
        return try {
            val response = apiService.googleAuth(AuthRequest(idToken = idToken))
            // Guardamos el token en DataStore para que el interceptor lo use en las próximas llamadas
            tokenManager.saveToken(response.accessToken)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        tokenManager.clearToken()
    }
}
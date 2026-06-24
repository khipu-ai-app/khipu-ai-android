package pe.khipuai.app.core.network

import android.util.Log
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import pe.khipuai.app.core.auth.AuthEventBus
import pe.khipuai.app.core.datastore.SessionDataStore
import pe.khipuai.app.data.remote.KhipuApiService
import pe.khipuai.app.data.remote.dto.RefreshTokenRequest
import javax.inject.Inject
import javax.inject.Provider

/**
 * OkHttp [Authenticator] que intercepta respuestas 401 y renueva el
 * access_token automáticamente vía `POST /auth/refresh`.
 *
 * Flujo:
 *  1. Una request retorna 401 con `Authorization: Bearer X`
 *  2. Llamamos `POST /auth/refresh` con el mismo `X`
 *  3. Si refresh 200: guardamos el nuevo token, reintentamos la request original
 *  4. Si refresh 401: limpiamos los tokens y emitimos [AuthEvent.SessionExpired]
 *     para que [MainActivity] navegue a Login
 *
 * El máximo de reintentos es 1 (si la request refrescada también da 401,
 * dejamos pasar el 401 al ViewModel sin entrar en loop infinito).
 *
 * Usamos [Provider] y [Lazy] para los servicios inyectados porque el
 * Authenticator se crea durante la inicialización de OkHttp, antes de
 * que el grafo de Hilt esté completamente listo.
 */
class TokenAuthenticator @Inject constructor(
    private val sessionDataStore: Provider<SessionDataStore>,
    private val apiService: Provider<KhipuApiService>,
    private val authEventBus: Provider<AuthEventBus>
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val path = response.request.url.encodedPath

        Log.w(
            "KhipuAuth",
            "authenticate() llamado para ${response.code} en $path " +
                "(priorResponses=${responseCount(response) - 1}, " +
                "hasAuth=${response.request.header("Authorization") != null})"
        )

        // Evitar loop infinito: si ya intentamos refrescar en este ciclo,
        // dejamos pasar el 401 al ViewModel
        if (responseCount(response) >= 2) {
            Log.w("KhipuAuth", "  → loop detectado, dejo pasar el 401")
            return null
        }

        // Si la request que falló es el propio endpoint de refresh, no tiene
        // sentido reintentarla: el refresh token ya no sirve.
        if (path.endsWith("/auth/refresh")) {
            Log.w("KhipuAuth", "  → /auth/refresh retornó 401, emitiendo SessionExpired")
            runBlocking { sessionDataStore.get().clearToken() }
            notifySessionExpired()
            return null
        }

        val currentToken = runBlocking {
            sessionDataStore.get().tokenFlow.firstOrNull()
        }
        if (currentToken.isNullOrEmpty()) {
            Log.w("KhipuAuth", "  → no hay token guardado, emitiendo SessionExpired")
            notifySessionExpired()
            return null
        }

        // Si la request que falló no es la que llevaba nuestro token, no
        // tiene sentido reintentarla — devolvemos null para que OkHttp
        // propague el 401
        val sentAuth = response.request.header("Authorization")
        if (sentAuth != "Bearer $currentToken") {
            Log.w(
                "KhipuAuth",
                "  → el header Authorization no coincide con el token guardado, dejo pasar el 401"
            )
            return null
        }

        Log.w("KhipuAuth", "  → intentando refresh con token guardado")
        val newToken = runBlocking {
            try {
                val resp = apiService.get().refreshToken(RefreshTokenRequest(currentToken))
                if (resp.isSuccessful) {
                    val fresh = resp.body()?.accessToken
                    if (!fresh.isNullOrEmpty()) {
                        sessionDataStore.get().saveToken(fresh)
                        Log.w("KhipuAuth", "  → refresh OK, nuevo token guardado")
                        fresh
                    } else null
                } else {
                    Log.w("KhipuAuth", "  → refresh retornó ${resp.code()}, no se obtuvo nuevo token")
                    null
                }
            } catch (e: Exception) {
                Log.w("KhipuAuth", "  → refresh excepción: ${e.message}")
                null
            }
        }

        return if (newToken != null) {
            response.request.newBuilder()
                .header("Authorization", "Bearer $newToken")
                .build()
        } else {
            // Refresh falló: limpiamos tokens y notificamos
            Log.w("KhipuAuth", "  → refresh falló, limpiando token y emitiendo SessionExpired")
            runBlocking { sessionDataStore.get().clearToken() }
            notifySessionExpired()
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private fun notifySessionExpired() {
        runCatching { authEventBus.get().emit(AuthEventBus.AuthEvent.SessionExpired) }
    }
}

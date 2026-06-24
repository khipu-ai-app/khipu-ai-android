package pe.khipuai.app.core.network

import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Excepción de red de alto nivel con un mensaje localizado en español listo
 * para mostrar al usuario en un Snackbar o banner.
 *
 * Se construye a partir de cualquier [Throwable] mediante [NetworkErrorMapper.from]
 * para que los ViewModels no tengan que conocer la jerarquía de OkHttp/Retrofit.
 */
class KhipuNetworkException(
    message: String,
    val isRetryable: Boolean = true
) : Exception(message)

/**
 * Mapea una excepción de red cruda a un [KhipuNetworkException] con mensaje
 * en español. Usar así en los ViewModels:
 *
 *   runCatching { apiService.getSomething() }
 *       .onFailure { showError(KhipuNetworkException.from(it).message) }
 */
object NetworkErrorMapper {

    fun from(throwable: Throwable): KhipuNetworkException = map(throwable, custom401 = null)

    /**
     * Variante para contextos donde un HTTP 401 no significa "sesión expirada"
     * sino "credenciales inválidas" (ej. login, eliminar cuenta con
     * contraseña). El resto de códigos se mapean igual.
     */
    fun from(throwable: Throwable, custom401: String?): KhipuNetworkException =
        map(throwable, custom401 = custom401)

    private fun map(throwable: Throwable, custom401: String?): KhipuNetworkException {
        return when (throwable) {
            is KhipuNetworkException -> throwable
            is HttpException -> mapHttp(throwable, custom401)
            is UnknownHostException -> KhipuNetworkException(
                "Sin conexión a internet. Revisa tu Wi-Fi o datos.",
                isRetryable = true
            )
            is SocketTimeoutException -> KhipuNetworkException(
                "La conexión está tardando demasiado. Intenta de nuevo.",
                isRetryable = true
            )
            is ConnectException -> KhipuNetworkException(
                "No pudimos conectar con el servidor. Intenta de nuevo.",
                isRetryable = true
            )
            is IOException -> KhipuNetworkException(
                "Error de red. Verifica tu conexión e intenta de nuevo.",
                isRetryable = true
            )
            else -> KhipuNetworkException(
                "Algo salió mal. Intenta de nuevo.",
                isRetryable = true
            )
        }
    }

    private fun mapHttp(e: HttpException, custom401: String?): KhipuNetworkException {
        // Si el backend retornó un cuerpo JSON con `detail` (formato estándar de
        // FastAPI/HTTPException), usamos ese mensaje. Es el más específico y
        // suele decir exactamente lo que pasó: "El correo ya está registrado",
        // "Credenciales incorrectas", etc. Solo hacemos fallback al mensaje
        // genérico del código si no hay un detail legible.
        val backendDetail = parseBackendDetail(e)
        if (!backendDetail.isNullOrBlank()) {
            return KhipuNetworkException(backendDetail, isRetryable = isRetryableCode(e.code()))
        }

        val (msg, retryable) = when (e.code()) {
            401 -> (custom401 ?: "Tu sesión ha expirado. Inicia sesión de nuevo.") to false
            403 -> "No tienes permiso para hacer esto." to false
            404 -> "No encontramos lo que buscabas." to false
            409 -> "Esta acción entra en conflicto con datos existentes." to false
            413 -> "El archivo es demasiado grande." to false
            422 -> "Los datos enviados no son válidos." to false
            429 -> "Demasiadas solicitudes. Espera un momento." to true
            in 500..599 -> "El servidor tuvo un problema. Intenta de nuevo." to true
            else -> "Ocurrió un error inesperado (${e.code()}). Intenta de nuevo." to true
        }
        return KhipuNetworkException(msg, isRetryable = retryable)
    }

    private fun isRetryableCode(code: Int): Boolean = when (code) {
        401, 403, 404, 409, 413, 422 -> false
        else -> true
    }

    /**
     * Lee el body de error del [HttpException] y extrae el campo `detail` de
     * FastAPI. Soporta los formatos comunes:
     *   - `{"detail": "mensaje simple"}`
     *   - `{"detail": [{"loc": [...], "msg": "..."}]}`  (errores de validación Pydantic)
     *
     * Retorna `null` si el body no es JSON, está vacío o no tiene `detail`.
     */
    private fun parseBackendDetail(e: HttpException): String? {
        val raw = try {
            e.response()?.errorBody()?.string()
        } catch (_: Throwable) {
            null
        } ?: return null

        return try {
            val json = JSONObject(raw)
            when (val detail = json.opt("detail")) {
                is String -> detail
                is org.json.JSONArray -> {
                    // Pydantic 422: lista de errores por campo
                    val first = detail.optJSONObject(0) ?: return null
                    val msg = first.optString("msg").takeIf { it.isNotBlank() } ?: return null
                    val loc = first.optJSONArray("loc")?.let { arr ->
                        (0 until arr.length())
                            .map { arr.optString(it) }
                            .filter { it.isNotBlank() && it != "body" }
                            .joinToString(".")
                    }
                    if (loc.isNullOrBlank()) msg else "$loc: $msg"
                }
                else -> null
            }
        } catch (_: Throwable) {
            null
        }
    }
}

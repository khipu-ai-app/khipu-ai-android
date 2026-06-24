package pe.khipuai.app.core.auth

import android.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONObject

/**
 * T-07: helpers para parsear JWT sin librerías externas.
 *
 * Un JWT tiene 3 partes separadas por `.`:
 *   header.payload.signature
 *
 * La payload (segunda parte) es base64url-encoded JSON. Solo nos interesa
 * el claim `exp` (expiration time, en segundos UNIX) para decidir si
 * podemos reusar el token sin un round-trip al backend.
 *
 * No validamos la firma aquí (el backend lo hace en cada request). Si la
 * firma está malformada, asumimos que el token es inválido.
 */
object JwtUtils {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Decodifica un JWT y retorna su payload. Retorna null si el formato
     * es inválido (no tiene 3 partes o la payload no es JSON parseable).
     */
    fun decodePayload(token: String): Map<String, Any>? {
        val parts = token.split(".")
        if (parts.size != 3) return null
        return try {
            val payloadJson = String(Base64.decode(parts[1], Base64.URL_SAFE))
            val obj = JSONObject(payloadJson)
            obj.keys().asSequence().associateWith { obj.get(it) }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Lee el claim `exp` (en segundos UNIX) y retorna true si el token
     * está expirado. Retorna true también si el claim no existe o no es
     * parseable (en caso de duda, asumimos expirado para forzar refresh).
     */
    fun isExpired(token: String): Boolean {
        val payload = decodePayload(token) ?: return true
        val exp = payload["exp"] as? Number ?: return true
        val expSeconds = exp.toLong()
        val nowSeconds = System.currentTimeMillis() / 1000
        return nowSeconds >= expSeconds
    }
}

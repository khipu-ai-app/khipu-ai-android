package pe.khipuai.app.core.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * T-09: helper para Google Sign-In con CredentialManager.
 *
 * Encapsula el flujo:
 *   1. Construir un [GetGoogleIdOption] con el `web_client_id` (configurado
 *      en Google Cloud Console y expuesto en `strings.xml`).
 *   2. Llamar a [CredentialManager.getCredential] (suspendida).
 *   3. Extraer el [GoogleIdTokenCredential] de la respuesta.
 *   4. Devolver el `idToken` para que el ViewModel lo envíe al backend.
 *
 * Errores específicos (devueltos como sealed [GoogleSignInResult]):
 *   - [GoogleSignInResult.NoCredentials]: el usuario no tiene cuenta de
 *     Google en el dispositivo.
 *   - [GoogleSignInResult.UserCancelled]: el usuario canceló el sheet.
 *   - [GoogleSignInResult.MalformedCredential]: el idToken no se pudo
 *     parsear (raro, indica bug nuestro o de Google).
 *   - [GoogleSignInResult.Failed]: cualquier otro error (sin red, etc).
 */
@Singleton
class GoogleSignInHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val credentialManager: CredentialManager by lazy {
        CredentialManager.create(context)
    }

    /**
     * Dispara el sheet de CredentialManager. La función es `suspend` y
     * DEBE ser llamada desde una coroutine. `credentialManager.getCredential`
     * es suspendida internamente, así que NO necesitamos wrap manual
     * con `suspendCancellableCoroutine`.
     */
    suspend fun signIn(webClientId: String): GoogleSignInResult {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false) // permite cuentas nuevas
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(true)            // auto-pick si hay 1 sola
                    .build()
            )
            .build()

        val response = try {
            credentialManager.getCredential(
                context = context,
                request = request
            )
        } catch (e: GetCredentialCancellationException) {
            return GoogleSignInResult.UserCancelled
        } catch (e: NoCredentialException) {
            return GoogleSignInResult.NoCredentials
        } catch (e: GetCredentialException) {
            return GoogleSignInResult.Failed(e.message ?: "Error de CredentialManager")
        } catch (t: Throwable) {
            return GoogleSignInResult.Failed(t.message ?: "Error desconocido")
        }

        return try {
            val googleId = GoogleIdTokenCredential.createFrom(response.credential.data)
            GoogleSignInResult.Success(idToken = googleId.idToken, email = googleId.id)
        } catch (e: GoogleIdTokenParsingException) {
            GoogleSignInResult.MalformedCredential
        }
    }
}

sealed class GoogleSignInResult {
    data class Success(val idToken: String, val email: String) : GoogleSignInResult()
    data object UserCancelled : GoogleSignInResult()
    data object NoCredentials : GoogleSignInResult()
    data object MalformedCredential : GoogleSignInResult()
    data class Failed(val message: String) : GoogleSignInResult()
}

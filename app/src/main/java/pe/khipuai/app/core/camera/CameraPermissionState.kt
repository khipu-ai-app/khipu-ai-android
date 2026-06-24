package pe.khipuai.app.core.camera

import android.Manifest

/**
 * T-05: estado de un permiso runtime de Android expuesto a la UI de Compose.
 * Sellamos los 3 estados posibles para que la pantalla pueda renderizar
 * cada caso sin adivinar:
 *
 *  - [Granted]: permiso concedido, podemos usar la cámara.
 *  - [Denied]: denegado pero puede re-intentar (la primera vez o tras
 *    "Denegar" sin marcar "No volver a mostrar").
 *  - [PermanentlyDenied]: marcado "No volver a mostrar". Solo podemos
 *    llevarlo a Settings.
 */
sealed class CameraPermissionState {
    data object Granted : CameraPermissionState()
    data object Denied : CameraPermissionState()
    data object PermanentlyDenied : CameraPermissionState()
    data object NotRequested : CameraPermissionState()

    companion object {
        const val PERMISSION: String = Manifest.permission.CAMERA
    }
}

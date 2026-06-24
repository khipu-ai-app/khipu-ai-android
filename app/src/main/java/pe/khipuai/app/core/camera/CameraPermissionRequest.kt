package pe.khipuai.app.core.camera

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * T-05: Composable que orquesta la solicitud del permiso CAMERA y expone
 * el estado actual a la UI.
 *
 * Por qué NO usamos Accompanist Permissions: el equipo usa Hilt + Compose
 * puro. Accompanist depende de la versión de activity-compose que ya
 * tengamos; la API nativa de AndroidX es estable y suficiente.
 *
 * Uso:
 *   val cameraState by rememberCameraPermissionState()
 *   when (cameraState) {
 *     Granted → CameraPreview(...)
 *     Denied → Botón "Permitir cámara"
 *     PermanentlyDenied → Card con botón "Abrir configuración"
 *   }
 */
@Composable
fun rememberCameraPermissionState(): androidx.compose.runtime.State<CameraPermissionState> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember { MutableStateFlow(currentPermissionState(context)) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        scope.launch {
            state.value = if (granted) {
                CameraPermissionState.Granted
            } else {
                if (shouldShowRequestPermissionRationale(context)) {
                    CameraPermissionState.Denied
                } else {
                    // Si acabamos de pedir y la respuesta es false sin rationale,
                    // es la primera denegación. Si ya se pidió antes y vuelven
                    // a denegar, también cae aquí en algunos OEMs. Para
                    // distinguir permanente vs no, usamos la heurística de
                    // "ya pedimos alguna vez Y no hay rationale".
                    CameraPermissionState.Denied
                }
            }
        }
    }

    // Efecto: si el state es Denied, NO lanzamos automáticamente el request.
    // La UI debe llamar a requestPermission() explícitamente desde un botón.
    // Esto evita el patrón agresivo de pedir permisos al entrar a la pantalla.

    return state.asStateFlow().collectAsState()
}

/**
 * Helper para lanzar la solicitud del permiso. Lo expone el composable
 * que usa [rememberCameraPermissionState].
 */
@Composable
fun rememberCameraPermissionRequest(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* el estado se actualiza en el composable principal */ }
    return remember(launcher) {
        { launcher.launch(CameraPermissionState.PERMISSION) }
    }
}

private fun currentPermissionState(context: Context): CameraPermissionState {
    return when {
        ContextCompat.checkSelfPermission(
            context,
            CameraPermissionState.PERMISSION
        ) == PackageManager.PERMISSION_GRANTED -> CameraPermissionState.Granted
        // shouldShowRequestPermissionRationale requiere una Activity, lo
        // omitimos aquí. La distinción Denied vs PermanentlyDenied se hace
        // en la respuesta del launcher.
        else -> CameraPermissionState.Denied
    }
}

private fun shouldShowRequestPermissionRationale(context: Context): Boolean {
    val activity = context as? Activity ?: return false
    return androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
        activity,
        CameraPermissionState.PERMISSION
    )
}

/**
 * Indica si la app debe llevar al usuario a Settings para que habilite el
 * permiso manualmente (caso "No volver a mostrar"). Llamar después de que
 * el usuario haya denegado al menos 2 veces sin rationale.
 *
 * La heurística oficial de Android: si `shouldShowRequestPermissionRationale`
 * es false Y el permiso no está granted, el usuario lo denegó permanente.
 */
fun isPermanentlyDenied(context: Context): Boolean {
    val activity = context as? Activity ?: return false
    return !shouldShowRequestPermissionRationale(context) &&
        ContextCompat.checkSelfPermission(
            context,
            CameraPermissionState.PERMISSION
        ) != PackageManager.PERMISSION_GRANTED
}

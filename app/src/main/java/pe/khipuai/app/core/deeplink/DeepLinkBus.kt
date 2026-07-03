package pe.khipuai.app.core.deeplink

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bus global de deep links. T-04.
 *
 * Cuando una notificación (local o FCM) lanza un Intent con el extra
 * `deep_link`, [MainActivity.onNewIntent] lo emite aquí. El [KhipuNavigation]
 * lo colecta y navega al destino correspondiente.
 *
 * SharedFlow con `replay=0` y `DROP_OLDEST` para no acumular navegación
 * diferida cuando el usuario no está en la app.
 */
@Singleton
class DeepLinkBus @Inject constructor() {

    private val _events = MutableSharedFlow<String>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun emit(deepLink: String) {
        _events.tryEmit(deepLink)
    }
}

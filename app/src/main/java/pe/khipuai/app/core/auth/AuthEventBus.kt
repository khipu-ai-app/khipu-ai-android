package pe.khipuai.app.core.auth

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bus global de eventos de autenticación. Singleton inyectado por Hilt.
 *
 * El [TokenAuthenticator] lo usa para notificar al [MainActivity] cuando
 * detecta que la sesión expiró y el refresh token también falló. La UI
 * observa este flow y navega a Login limpiando el backstack completo.
 *
 * T-08 lo especifica como `SharedFlow<AuthEvent>` para que la navegación
 * a Login funcione sin importar en qué pantalla esté el usuario.
 */
@Singleton
class AuthEventBus @Inject constructor() {

    sealed interface AuthEvent {
        /** El access_token venció y el refresh también. Hay que forzar logout. */
        data object SessionExpired : AuthEvent
    }

    private val _events = MutableSharedFlow<AuthEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    fun emit(event: AuthEvent) {
        _events.tryEmit(event)
    }
}

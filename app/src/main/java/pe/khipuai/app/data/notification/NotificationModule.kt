package pe.khipuai.app.data.notification

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * T-04: bindings de [NotificationDispatcher].
 *
 * DEV (activo): [@LocalDispatcher] → [LocalNotificationDispatcher]
 *   Muestra notificaciones del sistema sin Firebase. Sin red externa.
 *
 * PROD (desactivado): [@FcmDispatcher] → [FcmNotificationDispatcher]
 *   Stub que loggea. Para activarlo:
 *     - Agregar Firebase al proyecto (ver doc en [FcmNotificationDispatcher]).
 *     - Reemplazar el binding [@FcmDispatcher] por una implementación real
 *       que use FirebaseMessaging.
 *     - Inyectar `NotificationDispatcher` con qualifier `[@FcmDispatcher]`
 *       en el código que corresponda (workers del backend, hooks).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {

    @Binds
    @Singleton
    @LocalDispatcher
    abstract fun bindLocalDispatcher(
        impl: LocalNotificationDispatcher
    ): NotificationDispatcher

    @Binds
    @Singleton
    @FcmDispatcher
    abstract fun bindFcmStubDispatcher(
        impl: FcmNotificationDispatcher
    ): NotificationDispatcher
}

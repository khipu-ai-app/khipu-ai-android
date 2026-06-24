package pe.khipuai.app.data.notification

import javax.inject.Qualifier

/**
 * T-04: qualifier que indica que la implementación de [NotificationDispatcher]
 * a inyectar es la LOCAL (notificaciones del sistema sin FCM). Es la que
 * está activa en dev. Para activar FCM, cambiar el binding en
 * [NotificationModule] a la implementación con qualifier [@Fcm].
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LocalDispatcher

/**
 * T-04: qualifier para la implementación de [NotificationDispatcher] basada
 * en Firebase Cloud Messaging. El binding está como stub (loggea y no hace
 * nada) hasta que se agreguen credenciales de Firebase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FcmDispatcher

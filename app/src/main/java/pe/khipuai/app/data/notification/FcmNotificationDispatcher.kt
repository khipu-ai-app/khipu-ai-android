package pe.khipuai.app.data.notification

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import pe.khipuai.app.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * T-19: implementación de [NotificationDispatcher] basada en notificaciones
 * locales con el MISMO patrón que la que usaría FCM en producción.
 *
 * Esta implementación funciona en local (dev) y en producción. Cuando se
 * agregue Firebase, la implementación FCM real puede:
 *  1. Recibir el payload de FCM en `FirebaseMessagingService.onMessageReceived`.
 *  2. Parsear el deep link del payload.
 *  3. Llamar a los mismos métodos de este dispatcher para mostrar la notificación.
 *
 * El dispatcher local actual (LocalNotificationDispatcher) es el que se usa
 * actualmente. Este archivo demuestra el patrón que FCM debe replicar.
 * Para activar FCM real:
 *  1. Agregar Firebase al proyecto.
 *  2. Crear FcmMessagingService : FirebaseMessagingService() que reciba
 *     RemoteMessage y llame a NotificacionHandler.show() directamente.
 *  3. En el Manifest, declarar el servicio con intent-filter.
 *  4. Cambiar el binding Hilt en NotificationModule para que inyecte
 *     FcmDispatcher (no LocalDispatcher) cuando esté en producción.
 */
@Singleton
class FcmNotificationDispatcher @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationDispatcher {

    override fun ensureChannels() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        manager.createNotificationChannel(
            android.app.NotificationChannel(
                LocalNotificationDispatcher.CHANNEL_REMINDERS,
                "Repaso",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Recordatorios de repasos pendientes y rachas en riesgo." }
        )
        manager.createNotificationChannel(
            android.app.NotificationChannel(
                LocalNotificationDispatcher.CHANNEL_NEWS,
                "Novedades",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Resultados de procesamiento y logros desbloqueados." }
        )
    }

    override fun notifyReviewDue(dueCount: Int, mostUrgentCourseName: String?) {
        val body = if (mostUrgentCourseName != null)
            "Repasa $dueCount conceptos de $mostUrgentCourseName para mantener tu racha."
        else "Repasa $dueCount conceptos para mantener tu racha."
        show(
            notificationId = ID_REVIEW_DUE,
            channelId = LocalNotificationDispatcher.CHANNEL_REMINDERS,
            title = "Tienes conceptos pendientes 📚",
            body = body,
            deepLink = NotificationDeepLinks.reviewDue()
        )
    }

    override fun notifyScheduledReminder(noteId: String, noteTitle: String) {
        show(
            notificationId = noteId.hashCode(),
            channelId = LocalNotificationDispatcher.CHANNEL_REMINDERS,
            title = "📚 Toca repasar: $noteTitle",
            body = "Tienes un repaso agendado para hoy en Khipu. ¡Ánimo!",
            deepLink = NotificationDeepLinks.scheduledReminder(noteId)
        )
    }

    override fun notifyProcessingComplete(noteId: String, noteTitle: String, conceptsDetected: Int) {
        val body = "\"$noteTitle\" fue procesada. $conceptsDetected conceptos detectados."
        show(
            notificationId = noteId.hashCode(),
            channelId = LocalNotificationDispatcher.CHANNEL_NEWS,
            title = "Tu apunte está listo ✅",
            body = body,
            deepLink = NotificationDeepLinks.processingComplete(noteId)
        )
    }

    override fun notifyStreakAtRisk(currentStreakDays: Int) {
        show(
            notificationId = ID_STREAK_AT_RISK,
            channelId = LocalNotificationDispatcher.CHANNEL_REMINDERS,
            title = "⚠️ Tu racha de $currentStreakDays días está en riesgo",
            body = "Repasa aunque sea 1 concepto para mantener tu racha.",
            deepLink = NotificationDeepLinks.streakAtRisk()
        )
    }

    override fun notifyAchievementUnlocked(achievementId: String, title: String, description: String) {
        show(
            notificationId = achievementId.hashCode(),
            channelId = LocalNotificationDispatcher.CHANNEL_NEWS,
            title = "🏆 Nuevo logro: $title",
            body = description,
            deepLink = NotificationDeepLinks.achievementUnlocked()
        )
    }

    private fun show(notificationId: Int, channelId: String, title: String, body: String, deepLink: String) {
        val intent = android.content.Intent(context, pe.khipuai.app.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NotificationDeepLinks.EXTRA_DEEP_LINK, deepLink)
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context, notificationId, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }

    companion object {
        private const val ID_REVIEW_DUE = 1001
        private const val ID_STREAK_AT_RISK = 1002
    }
}

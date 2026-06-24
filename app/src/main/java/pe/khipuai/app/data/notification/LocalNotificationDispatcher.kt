package pe.khipuai.app.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import pe.khipuai.app.MainActivity
import pe.khipuai.app.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * T-04: implementación LOCAL de [NotificationDispatcher].
 *
 * Muestra notificaciones del sistema vía NotificationCompat. No usa Firebase.
 * Es la implementación activa en dev (Hilt binding por defecto).
 *
 * Canales:
 *  - CHANNEL_REMINDERS ("Repaso"): T-04 tipo 1 (repaso diario) y 3 (racha en riesgo).
 *  - CHANNEL_NEWS     ("Novedades"): T-04 tipo 2 (procesamiento OK) y 4 (logros).
 *
 * Permisos: Android 13+ requiere POST_NOTIFICATIONS. Si no está granted, las
 * notificaciones se construyen pero no se muestran (no crashea).
 */
@Singleton
class LocalNotificationDispatcher @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationDispatcher {

    override fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        val remindersChannel = NotificationChannel(
            CHANNEL_REMINDERS,
            "Repaso",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Recordatorios de repasos pendientes y rachas en riesgo."
        }
        manager.createNotificationChannel(remindersChannel)

        val newsChannel = NotificationChannel(
            CHANNEL_NEWS,
            "Novedades",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Resultados de procesamiento y logros desbloqueados."
        }
        manager.createNotificationChannel(newsChannel)
    }

    override fun notifyReviewDue(
        dueCount: Int,
        mostUrgentCourseName: String?
    ) {
        val body = if (mostUrgentCourseName != null) {
            "Repasa $dueCount conceptos de $mostUrgentCourseName para mantener tu racha."
        } else {
            "Repasa $dueCount conceptos para mantener tu racha."
        }
        show(
            notificationId = ID_REVIEW_DUE,
            channelId = CHANNEL_REMINDERS,
            title = "Tienes conceptos pendientes 📚",
            body = body,
            deepLink = NotificationDeepLinks.reviewDue()
        )
    }

    override fun notifyScheduledReminder(noteId: String, noteTitle: String) {
        show(
            notificationId = noteId.hashCode(),
            channelId = CHANNEL_REMINDERS,
            title = "📚 Toca repasar: $noteTitle",
            body = "Tienes un repaso agendado para hoy en Khipu. ¡Ánimo!",
            deepLink = NotificationDeepLinks.processingComplete(noteId)
        )
    }

    override fun notifyProcessingComplete(
        noteId: String,
        noteTitle: String,
        conceptsDetected: Int
    ) {
        val body = "\"$noteTitle\" fue procesada. $conceptsDetected conceptos detectados."
        show(
            notificationId = noteId.hashCode(),
            channelId = CHANNEL_NEWS,
            title = "Tu apunte está listo ✅",
            body = body,
            deepLink = NotificationDeepLinks.processingComplete(noteId)
        )
    }

    override fun notifyStreakAtRisk(currentStreakDays: Int) {
        val body = "Repasa aunque sea 1 concepto para mantener tu racha."
        show(
            notificationId = ID_STREAK_AT_RISK,
            channelId = CHANNEL_REMINDERS,
            title = "⚠️ Tu racha de $currentStreakDays días está en riesgo",
            body = body,
            deepLink = NotificationDeepLinks.streakAtRisk()
        )
    }

    override fun notifyAchievementUnlocked(
        achievementId: String,
        title: String,
        description: String
    ) {
        show(
            notificationId = achievementId.hashCode(),
            channelId = CHANNEL_NEWS,
            title = "🏆 Nuevo logro: $title",
            body = description,
            deepLink = NotificationDeepLinks.achievementUnlocked()
        )
    }

    // ── Internals ────────────────────────────────────────────────────────────
    private fun show(
        notificationId: Int,
        channelId: String,
        title: String,
        body: String,
        deepLink: String
    ) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NotificationDeepLinks.EXTRA_DEEP_LINK, deepLink)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
        // Sin permiso en Android 13+: silencioso. La UI debería pedirlo
        // explícitamente al inicio. (Fuera de scope de T-04.)
    }

    companion object {
        const val CHANNEL_REMINDERS = "khipu_reminders"
        const val CHANNEL_NEWS = "khipu_news"

        private const val ID_REVIEW_DUE = 1001
        private const val ID_STREAK_AT_RISK = 1002
    }
}

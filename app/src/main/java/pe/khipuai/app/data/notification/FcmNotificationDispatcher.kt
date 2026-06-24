package pe.khipuai.app.data.notification

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * T-04: stub de la implementación FCM de [NotificationDispatcher].
 *
 * Esta clase está lista para activarse cuando se agreguen credenciales de
 * Firebase. Por ahora loggea en vez de enviar push. El binding por defecto
 * de Hilt es la implementación LOCAL, así que este stub NO se usa en dev.
 *
 * Pasos para activarlo:
 *  1. Agregar `firebase-bom` y `firebase-messaging` en `app/build.gradle.kts`.
 *  2. Colocar `google-services.json` en `app/`.
 *  3. Aplicar el plugin `com.google.gms.google-services`.
 *  4. Crear `FcmMessagingService : FirebaseMessagingService()` (hay un
 *     template en `FcmMessagingService.kt`) y declararlo en el Manifest.
 *  5. Cambiar el binding de [@FcmDispatcher] en [NotificationModule] para
 *     apuntar a una implementación que use `FirebaseMessaging.getInstance().send(...)`.
 *  6. Asegurar que el backend (`send_fcm_notification` en
 *     `khipu-ai-backend/app/workers/tasks/schedule_reviews.py`) tiene el
 *     FCM_SERVER_KEY real.
 */
@Singleton
class FcmNotificationDispatcher @Inject constructor() : NotificationDispatcher {

    override fun ensureChannels() {
        // FCM crea los canales según el payload. En el cliente no hacemos nada.
        Log.d(TAG, "ensureChannels() — FCM gestiona los canales en el payload")
    }

    override fun notifyReviewDue(dueCount: Int, mostUrgentCourseName: String?) {
        Log.d(
            TAG,
            "FCM [REVIEW_DUE] would send: due=$dueCount, course=$mostUrgentCourseName"
        )
    }

    override fun notifyScheduledReminder(noteId: String, noteTitle: String) {
        Log.d(
            TAG,
            "FCM [SCHEDULED_REMINDER] would send: note=$noteId, title=$noteTitle"
        )
    }

    override fun notifyProcessingComplete(
        noteId: String,
        noteTitle: String,
        conceptsDetected: Int
    ) {
        Log.d(
            TAG,
            "FCM [PROCESSING_OK] would send: note=$noteId, title=$noteTitle, concepts=$conceptsDetected"
        )
    }

    override fun notifyStreakAtRisk(currentStreakDays: Int) {
        Log.d(
            TAG,
            "FCM [STREAK_AT_RISK] would send: streak=$currentStreakDays days"
        )
    }

    override fun notifyAchievementUnlocked(
        achievementId: String,
        title: String,
        description: String
    ) {
        Log.d(
            TAG,
            "FCM [ACHIEVEMENT] would send: id=$achievementId, title=$title"
        )
    }

    private companion object {
        const val TAG = "KhipuFcmStub"
    }
}

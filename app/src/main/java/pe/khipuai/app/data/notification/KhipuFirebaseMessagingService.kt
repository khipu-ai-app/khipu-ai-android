package pe.khipuai.app.data.notification

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pe.khipuai.app.data.repository.AuthRepository

/**
 * Servicio encargado de recibir notificaciones Push desde Firebase Cloud Messaging (FCM).
 */
@AndroidEntryPoint
class KhipuFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    @FcmDispatcher
    lateinit var fcmDispatcher: NotificationDispatcher

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Nuevo token FCM recibido: $token")
        
        // Enviar el token al backend de Khipu AI
        CoroutineScope(Dispatchers.IO).launch {
            try {
                authRepository.updateMyProfile(pe.khipuai.app.data.remote.dto.UserUpdateRequest(fcmToken = token))
                Log.d("FCM", "Token sincronizado con el backend")
            } catch (e: Exception) {
                Log.e("FCM", "Error sincronizando token FCM", e)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d("FCM", "Mensaje recibido desde: ${message.from}")

        val data = message.data
        val type = data["type"]

        // Derivar el mensaje al dispatcher local basándose en el payload
        when (type) {
            "processing_complete" -> {
                val noteId = data["noteId"] ?: return
                val noteTitle = data["noteTitle"] ?: "Apunte"
                val concepts = data["conceptsDetected"]?.toIntOrNull() ?: 0
                fcmDispatcher.notifyProcessingComplete(noteId, noteTitle, concepts)
            }
            "review_due" -> {
                val dueCount = data["dueCount"]?.toIntOrNull() ?: 0
                val mostUrgent = data["mostUrgentCourseName"]
                fcmDispatcher.notifyReviewDue(dueCount, mostUrgent)
            }
            "scheduled_reminder" -> {
                val noteId = data["noteId"] ?: return
                val noteTitle = data["noteTitle"] ?: "Apunte"
                fcmDispatcher.notifyScheduledReminder(noteId, noteTitle)
            }
            "streak_at_risk" -> {
                val currentStreakDays = data["currentStreakDays"]?.toIntOrNull() ?: 0
                fcmDispatcher.notifyStreakAtRisk(currentStreakDays)
            }
            "achievement_unlocked" -> {
                val achievementId = data["achievementId"] ?: return
                val title = data["title"] ?: "Logro desbloqueado"
                val description = data["description"] ?: ""
                fcmDispatcher.notifyAchievementUnlocked(achievementId, title, description)
            }
            else -> {
                // Mensajes sin tipo o desconocidos. Si vienen con notificación, el sistema los maneja solo si estamos en background.
                Log.w("FCM", "Tipo de mensaje desconocido o no especificado: $type")
            }
        }
    }
}

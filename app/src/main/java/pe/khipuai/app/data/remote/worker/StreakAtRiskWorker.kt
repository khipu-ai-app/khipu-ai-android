package pe.khipuai.app.data.remote.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import pe.khipuai.app.data.notification.LocalDispatcher
import pe.khipuai.app.data.notification.NotificationDispatcher
import pe.khipuai.app.data.remote.KhipuApiService
import javax.inject.Inject

/**
 * T-04 — Tipo 3: Racha en riesgo.
 *
 * Se ejecuta diariamente a las 18:00 (programado por
 * [pe.khipuai.app.data.notification.StreakAtRiskScheduler]). Consulta las
 * stats del planner y, si el usuario tiene racha >= 3 días, dispara la
 * notificación. El usuario ya puede haber repasado hoy; en ese caso, ignora
 * la notificación y listo. La heurística fina de "repasó hoy o no" se
 * puede mejorar en una iteración futura comparando con getTodayPlanner().
 */
@HiltWorker
class StreakAtRiskWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    @Inject
    lateinit var apiService: KhipuApiService

    @Inject
    @LocalDispatcher
    lateinit var dispatcher: NotificationDispatcher

    override suspend fun doWork(): Result {
        return try {
            val stats = apiService.getPlannerStats()
            if (stats.streakDays >= MIN_STREAK_DAYS) {
                dispatcher.notifyStreakAtRisk(currentStreakDays = stats.streakDays)
            }
            Result.success()
        } catch (e: Exception) {
            // Sin red → silencioso. El worker se reintenta la próxima
            // ejecución diaria. No es crítico avisar este error.
            Result.success()
        }
    }

    companion object {
        const val MIN_STREAK_DAYS = 3
    }
}

package pe.khipuai.app.data.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import pe.khipuai.app.data.remote.worker.StreakAtRiskWorker
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * T-04 — Tipo 3: programa el [StreakAtRiskWorker] para que se ejecute
 * diariamente a las 18:00 (hora local del device). Usa PeriodicWorkRequest
 * con initialDelay calculado hasta la próxima 18:00.
 *
 * PeriodicWorkRequest de WorkManager tiene un período mínimo de 15 min y
 * se ejecuta dentro de una ventana flexible. No garantiza exactitud al
 * minuto, lo cual está bien para esta notificación.
 */
object StreakAtRiskScheduler {

    private const val TRIGGER_HOUR = 18
    private const val UNIQUE_WORK_NAME = "streak_at_risk_daily"

    fun schedule(context: Context) {
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val target = now.toLocalDate().atTime(LocalTime.of(TRIGGER_HOUR, 0))
        val next = if (now.isBefore(target)) target else target.plusDays(1)
        val initialDelayMillis =
            Duration.between(now.atZone(ZoneId.systemDefault()), next.atZone(ZoneId.systemDefault()))
                .toMillis()

        val request = PeriodicWorkRequestBuilder<StreakAtRiskWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .addTag("streak_at_risk")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}

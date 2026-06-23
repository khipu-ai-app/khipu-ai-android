package pe.khipuai.app.data.notification

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import pe.khipuai.app.data.remote.worker.ManualScheduleReminderWorker
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Helper para programar el [ManualScheduleReminderWorker] de un repaso
 * agendado por el usuario. Se dispara a las 9:00 AM de la fecha objetivo.
 */
object ManualScheduleScheduler {

    private const val NOTIFICATION_HOUR = 9
    private const val NOTIFICATION_MINUTE = 0

    fun schedule(
        context: Context,
        noteId: String,
        noteTitle: String,
        scheduledDate: LocalDate
    ) {
        val triggerTime = LocalDateTime.of(
            scheduledDate,
            LocalTime.of(NOTIFICATION_HOUR, NOTIFICATION_MINUTE)
        )
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val delayMillis = java.time.Duration.between(now, triggerTime).toMillis()

        // Si la fecha objetivo ya pasó, no programamos
        if (delayMillis <= 0) return

        val inputData = Data.Builder()
            .putString(ManualScheduleReminderWorker.KEY_NOTE_ID, noteId)
            .putString(ManualScheduleReminderWorker.KEY_NOTE_TITLE, noteTitle)
            .build()

        val request = OneTimeWorkRequestBuilder<ManualScheduleReminderWorker>()
            .setInputData(inputData)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .addTag("manual_schedule")
            .build()

        val uniqueName = "manual_schedule_$noteId"
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueName,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}

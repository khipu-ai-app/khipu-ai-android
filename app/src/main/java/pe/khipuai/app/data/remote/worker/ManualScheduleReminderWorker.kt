package pe.khipuai.app.data.remote.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import pe.khipuai.app.data.notification.LocalDispatcher
import pe.khipuai.app.data.notification.NotificationDispatcher
import javax.inject.Inject

/**
 * Worker que muestra la notificación local de un repaso agendado manualmente.
 * Se programa con un delay (initialDelay) calculado en AnalysisViewModel para
 * que se dispare a la hora del repaso.
 *
 * T-04: usa [NotificationDispatcher] en vez de la helper directa, así el
 * mismo código funciona tanto en dev (local) como en prod (FCM).
 *
 * InputData:
 *  - "note_title" (String): título de la nota
 *  - "note_id" (String): id de la nota (para deep link)
 */
@HiltWorker
class ManualScheduleReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    @Inject
    @LocalDispatcher
    lateinit var dispatcher: NotificationDispatcher

    override suspend fun doWork(): Result {
        val noteTitle = inputData.getString(KEY_NOTE_TITLE) ?: "Tu apunte"
        val noteId = inputData.getString(KEY_NOTE_ID) ?: return Result.success()

        dispatcher.notifyScheduledReminder(
            noteId = noteId,
            noteTitle = noteTitle
        )
        return Result.success()
    }

    companion object {
        const val KEY_NOTE_TITLE = "note_title"
        const val KEY_NOTE_ID = "note_id"
    }
}

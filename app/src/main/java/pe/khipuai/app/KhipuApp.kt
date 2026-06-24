package pe.khipuai.app

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import pe.khipuai.app.data.notification.LocalDispatcher
import pe.khipuai.app.data.notification.NotificationDispatcher
import pe.khipuai.app.data.notification.StreakAtRiskScheduler
import javax.inject.Inject

@HiltAndroidApp
class KhipuApp : Application(), Configuration.Provider {

    // T-04: el dispatcher activo en dev (local) se inyecta vía Hilt.
    // Para migrar a FCM en prod, cambiar el qualifier a [@FcmDispatcher]
    // y agregar Firebase al build.gradle.
    @Inject
    @LocalDispatcher
    lateinit var notificationDispatcher: NotificationDispatcher

    // T-04: HiltWorkerFactory permite que los @HiltWorker (StreakAtRiskWorker,
    // ManualScheduleReminderWorker, UploadSyncWorker) reciban dependencias
    // inyectadas. Sin esto, las properties @Inject lateinit quedan en null
    // y el worker crashea con NPE al primer uso.
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        notificationDispatcher.ensureChannels()
        // T-04: programar el chequeo diario de racha en riesgo. Idempotente:
        // si ya existe, la policy KEEP lo deja igual.
        StreakAtRiskScheduler.schedule(this)
        try {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        } catch (e: Exception) {
            Log.w("KhipuApp", "Crashlytics setup skipped (missing google-services.json in dev).")
        }
    }
}
package pe.khipuai.app

import android.app.Application
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KhipuApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        } catch (e: Exception) {
            Log.w("KhipuApp", "Crashlytics setup skipped (missing google-services.json in dev).")
        }
    }
}
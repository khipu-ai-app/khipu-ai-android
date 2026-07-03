package pe.khipuai.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import android.util.Log
import pe.khipuai.app.R
import pe.khipuai.app.data.remote.KhipuApiService
import java.util.concurrent.TimeUnit

@HiltWorker
class KhipuWidgetRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @javax.inject.Inject
    lateinit var apiService: KhipuApiService

    override suspend fun doWork(): Result {
        Log.d(TAG, "Worker started")
        return try {
            val today = apiService.getTodayPlanner()
            Log.d(TAG, "API OK: ${today.size} concepts")
            val stats = try {
                apiService.getPlannerStats()
            } catch (_: Exception) { null }
            updateWidget(today, stats?.streakDays ?: 0)
            Result.success()
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP ${e.code()}: ${e.message()}")
            if (e.code() == 401) updateWidget(emptyList(), 0)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
            Result.success()
        }
    }

    private fun updateWidget(
        concepts: List<pe.khipuai.app.data.remote.dto.DueConceptResponse>,
        streakDays: Int
    ) {
        val context = applicationContext
        val widgetIds = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, KhipuWidgetProvider::class.java))
        if (widgetIds.isEmpty()) return

        val views = RemoteViews(context.packageName, R.layout.khipu_widget_layout)
        views.setTextViewText(R.id.widget_title, "Khipu AI  🔥 $streakDays")

        if (concepts.isEmpty()) {
            views.setTextViewText(R.id.widget_concept_1, "Sin repasos pendientes")
            views.setViewVisibility(R.id.widget_concept_2, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_concept_3, android.view.View.GONE)
        } else {
            val due = concepts.filter { it.isDue }
            val c1 = due.getOrNull(0)
            val c2 = due.getOrNull(1)
            val c3 = due.getOrNull(2)

            views.setTextViewText(R.id.widget_concept_1, "• ${c1?.label ?: ""}")
            views.setViewVisibility(R.id.widget_concept_1, android.view.View.VISIBLE)

            if (c2 != null) {
                views.setTextViewText(R.id.widget_concept_2, "• ${c2.label}")
                views.setViewVisibility(R.id.widget_concept_2, android.view.View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_concept_2, android.view.View.GONE)
            }

            if (c3 != null) {
                views.setTextViewText(R.id.widget_concept_3, "• ${c3.label}")
                views.setViewVisibility(R.id.widget_concept_3, android.view.View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_concept_3, android.view.View.GONE)
            }
        }

        for (id in widgetIds) {
            AppWidgetManager.getInstance(context).updateAppWidget(id, views)
        }
    }

    companion object {
        private const val TAG = "KhipuWidgetRefresh"
        private const val WORK_NAME = "khipu_widget_refresh"
        private const val IMMEDIATE_WORK_NAME = "khipu_widget_refresh_immediate"

        fun enqueueRefresh(context: Context) {
            val request = PeriodicWorkRequestBuilder<KhipuWidgetRefreshWorker>(
                30, TimeUnit.MINUTES
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun enqueueImmediate(context: Context) {
            val request = androidx.work.OneTimeWorkRequestBuilder<KhipuWidgetRefreshWorker>()
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}

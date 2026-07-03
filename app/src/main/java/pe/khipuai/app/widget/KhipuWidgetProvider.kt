package pe.khipuai.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import pe.khipuai.app.MainActivity
import pe.khipuai.app.R

class KhipuWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.khipu_widget_layout)
            views.setTextViewText(R.id.widget_title, "Khipu AI")
            views.setTextViewText(R.id.widget_concept_1, "Cargando…")
            views.setViewVisibility(R.id.widget_concept_2, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_concept_3, android.view.View.GONE)

            // Tap en cualquier parte → abre la app
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPending = PendingIntent.getActivity(
                context, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(android.R.id.background, openPending)

            try {
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                android.util.Log.e("KhipuWidget", "Error al actualizar widget $appWidgetId", e)
            }
        }

        // Encolar actualización inmediata + periódica
        KhipuWidgetRefreshWorker.enqueueRefresh(context)
        KhipuWidgetRefreshWorker.enqueueImmediate(context)
    }
}

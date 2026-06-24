package pe.khipuai.app.data.notification

/**
 * Helper para construir y parsear los deep links que las notificaciones
 * (locales o FCM) usan para abrir la pantalla correcta.
 *
 * Convenciones:
 *  - "planner"               → PlannerScreen (Tipo 1: repaso pendiente)
 *  - "analysis/{noteId}"     → AnalysisScreen de una nota (Tipo 2: procesamiento OK)
 *  - "planner"               → también para racha en riesgo (Tipo 3)
 *  - "achievements"          → StatisticsScreen en tab logros (Tipo 4)
 */
object NotificationDeepLinks {

    const val EXTRA_DEEP_LINK = "deep_link"

    fun reviewDue(): String = "planner"
    fun processingComplete(noteId: String): String = "analysis/$noteId"
    fun streakAtRisk(): String = "planner"
    fun achievementUnlocked(): String = "achievements"
}

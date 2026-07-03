package pe.khipuai.app.data.notification

/**
 * Helper para construir y parsear los deep links que las notificaciones
 * (locales o FCM) usan para abrir la pantalla correcta.
 *
 * Convenciones (T-19):
 *  - "planner"                  → PlannerScreen (repaso pendiente / racha en riesgo)
 *  - "analysis/{noteId}"        → AnalysisScreen (procesamiento completo)
 *  - "scheduled/{noteId}"       → NoteDetailScreen (repaso agendado manualmente)
 *  - "note/{noteId}"            → NoteDetailScreen (cualquier nota, genérico)
 *  - "achievements"             → AchievementsScreen (logro desbloqueado)
 *
 * Cada notificación debe usar la función correspondiente para que el deep link
 * siempre use el formato correcto. No construir deep links como strings crudos.
 */
object NotificationDeepLinks {

    const val EXTRA_DEEP_LINK = "deep_link"

    fun reviewDue(): String = "planner"
    fun processingComplete(noteId: String): String = "analysis/$noteId"
    fun scheduledReminder(noteId: String): String = "scheduled/$noteId"
    fun noteDetail(noteId: String): String = "note/$noteId"
    fun streakAtRisk(): String = "planner"
    fun achievementUnlocked(): String = "achievements"
}

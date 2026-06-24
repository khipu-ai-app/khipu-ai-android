package pe.khipuai.app.data.notification

/**
 * Abstracción del envío de notificaciones push de Khipu AI.
 *
 * T-04: existe una implementación local (notificaciones del sistema vía
 * [NotificationCompat]) y una stub FCM (lista para activarse cuando se
 * agreguen credenciales de Firebase). El binding se elige por Hilt según
 * el qualifier [@Local] o [@Fcm].
 *
 * El código de negocio (workers, hooks del pipeline) solo conoce esta
 * interface, no si la notificación viaja por FCM o se muestra directo.
 */
interface NotificationDispatcher {

    /** Crea los canales necesarios. Llamar una vez al iniciar la app. */
    fun ensureChannels()

    // ── Tipo 1: Repaso pendiente (diario) ───────────────────────────────────
    fun notifyReviewDue(
        dueCount: Int,
        mostUrgentCourseName: String?
    )

    /**
     * Repaso agendado manualmente por el user (F-08 ManualSchedule).
     * Sub-tipo del Tipo 1: en vez de ser un resumen diario, es un recordatorio
     * de UNA nota específica.
     */
    fun notifyScheduledReminder(
        noteId: String,
        noteTitle: String
    )

    // ── Tipo 2: Procesamiento de nota completado ────────────────────────────
    fun notifyProcessingComplete(
        noteId: String,
        noteTitle: String,
        conceptsDetected: Int
    )

    // ── Tipo 3: Racha en riesgo ─────────────────────────────────────────────
    fun notifyStreakAtRisk(currentStreakDays: Int)

    // ── Tipo 4: Logro desbloqueado ─────────────────────────────────────────
    fun notifyAchievementUnlocked(
        achievementId: String,
        title: String,
        description: String
    )
}

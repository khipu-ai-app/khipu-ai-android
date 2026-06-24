package pe.khipuai.app.ui.screens.achievements

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import pe.khipuai.app.data.notification.LocalDispatcher
import pe.khipuai.app.data.notification.NotificationDispatcher
import pe.khipuai.app.data.remote.dto.AchievementResponse
import pe.khipuai.app.data.remote.KhipuApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementManager @Inject constructor() {

    // T-04: cuando el backend reporta un logro recién desbloqueado,
    // disparamos la notificación push además del popup in-app.
    @Inject
    @LocalDispatcher
    lateinit var notificationDispatcher: NotificationDispatcher

    private val _popupQueue = MutableStateFlow<List<AchievementItem>>(emptyList())
    val popupQueue: StateFlow<List<AchievementItem>> = _popupQueue.asStateFlow()

    fun showAchievement(achievement: AchievementItem) {
        _popupQueue.update { it + achievement }
    }

    suspend fun checkAchievements(apiService: KhipuApiService, catalog: List<AchievementItem>) {
        try {
            val newlyUnlocked = apiService.checkAchievements()
            if (newlyUnlocked.isNotEmpty()) {
                val catalogMap = catalog.associateBy { it.id }
                val popups = newlyUnlocked.mapNotNull { response ->
                    catalogMap[response.achievementId]?.copy(
                        isUnlocked = true,
                        unlockedAt = response.unlockedAt
                    )
                }
                _popupQueue.update { it + popups }
                // T-04: Tipo 4 — notificar al usuario fuera de la app para
                // logros recién desbloqueados. Usamos el título del catálogo
                // (description localizada) y el id del catálogo.
                popups.forEach { item ->
                    notificationDispatcher.notifyAchievementUnlocked(
                        achievementId = item.id,
                        title = item.title,
                        description = item.description
                    )
                }
            }
        } catch (e: Exception) {
            // Ignore for now
        }
    }

    fun dismissCurrent() {
        _popupQueue.update { if (it.isNotEmpty()) it.drop(1) else it }
    }
}

package pe.khipuai.app.ui.screens.achievements

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import pe.khipuai.app.data.remote.dto.AchievementResponse
import pe.khipuai.app.data.remote.KhipuApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementManager @Inject constructor() {
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
            }
        } catch (e: Exception) {
            // Ignore for now
        }
    }

    fun dismissCurrent() {
        _popupQueue.update { if (it.isNotEmpty()) it.drop(1) else it }
    }
}

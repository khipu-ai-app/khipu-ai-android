package pe.khipuai.app.ui.screens.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.khipuai.app.data.remote.KhipuApiService
import pe.khipuai.app.data.remote.dto.AchievementResponse
import javax.inject.Inject

data class AchievementItem(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val isUnlocked: Boolean,
    val unlockedAt: String? = null
)

data class AchievementsUiState(
    val isLoading: Boolean = true,
    val achievements: List<AchievementItem> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val apiService: KhipuApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AchievementsUiState())
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()

    init {
        loadAchievements()
    }

    private fun loadAchievements() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val unlocked = apiService.getAchievements()
                val unlockedMap = unlocked.associateBy { it.achievementId }
                
                val combined = AchievementCatalog.items.map { item ->
                    val unlockedData = unlockedMap[item.id]
                    item.copy(
                        isUnlocked = unlockedData != null,
                        unlockedAt = unlockedData?.unlockedAt
                    )
                }.sortedBy { !it.isUnlocked } // Mostrar los desbloqueados primero

                _uiState.update { it.copy(isLoading = false, achievements = combined) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = pe.khipuai.app.core.network.NetworkErrorMapper.from(e).message) }
            }
        }
    }
}

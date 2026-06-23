package pe.khipuai.app.ui.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.khipuai.app.data.remote.KhipuApiService
import pe.khipuai.app.data.remote.dto.CourseDistributionItem
import pe.khipuai.app.data.remote.dto.DueConceptResponse
import pe.khipuai.app.data.remote.dto.ScheduleDayResponse
import javax.inject.Inject

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val streakDays: Int = 0,
    val totalConcepts: Int = 0,
    val dominatedConcepts: Int = 0,
    val masteryPercentage: Int = 0,
    val courseDistribution: List<CourseDistributionItem> = emptyList(),
    val weeklySchedule: List<ScheduleDayResponse> = emptyList(),
    val hardestConcepts: List<DueConceptResponse> = emptyList(),
    val dailyGoalConcepts: Int = 5,
    val dailyCompletedConcepts: Int = 0,
    val error: String? = null
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val apiService: KhipuApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Fetch everything in parallel
                val statsDeferred = async { apiService.getPlannerStats() }
                val scheduleDeferred = async { apiService.getWeeklySchedule() }
                val todayDeferred = async { apiService.getTodayPlanner() }
                
                val stats = statsDeferred.await()
                val schedule = scheduleDeferred.await()
                val today = todayDeferred.await()

                // Calculate hardest concepts (lowest ease factor)
                val hardest = today.sortedBy { it.easeFactor }.take(5)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        streakDays = stats.streakDays,
                        totalConcepts = stats.totalConcepts,
                        dominatedConcepts = stats.dominatedConcepts,
                        masteryPercentage = stats.masteryPercentage,
                        courseDistribution = stats.courseDistribution,
                        weeklySchedule = schedule,
                        hardestConcepts = hardest,
                        dailyCompletedConcepts = 0, // Se actualizará cuando el backend rastree repasos de hoy
                        dailyGoalConcepts = today.size.coerceAtLeast(5) // Usa los conceptos pendientes como meta
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Error al cargar estadísticas: ${e.localizedMessage}") }
            }
        }
    }
}

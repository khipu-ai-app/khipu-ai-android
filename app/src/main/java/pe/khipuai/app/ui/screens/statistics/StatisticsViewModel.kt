package pe.khipuai.app.ui.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.khipuai.app.data.remote.KhipuApiService
import pe.khipuai.app.data.remote.dto.CourseDistributionItem
import pe.khipuai.app.data.remote.dto.DueConceptResponse
import pe.khipuai.app.data.remote.dto.ScheduleDayResponse
import pe.khipuai.app.data.repository.CourseRepository
import javax.inject.Inject

/**
 * T-16: combinamos el `courseDistribution` que viene del backend
 * (courseName + count) con el color que el usuario eligió para cada
 * curso en la cache local. La UI puede entonces teñir las barras de
 * "Dominio por curso" con el color real.
 */
data class CourseDistributionWithColor(
    val item: CourseDistributionItem,
    val colorHex: String?,
)

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val streakDays: Int = 0,
    val totalConcepts: Int = 0,
    val dominatedConcepts: Int = 0,
    val masteryPercentage: Int = 0,
    val courseDistribution: List<CourseDistributionItem> = emptyList(),
    val courseDistributionWithColor: List<CourseDistributionWithColor> = emptyList(),
    val weeklySchedule: List<ScheduleDayResponse> = emptyList(),
    val hardestConcepts: List<DueConceptResponse> = emptyList(),
    val dailyGoalConcepts: Int = 5,
    val dailyCompletedConcepts: Int = 0,
    val error: String? = null
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val apiService: KhipuApiService,
    // T-16: inyectamos el repo de cursos para mapear courseName → color
    private val courseRepository: CourseRepository,
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
                // T-16: sincronizar cursos desde la red ANTES de tomar
                // el primer valor del Flow. Sin esto, `first()` puede
                // colgarse si Room está vacío en el primer arranque.
                val colorsDeferred = async {
                    courseRepository.fetchMyCourses()
                    courseRepository.observeAll().first()
                }

                val stats = statsDeferred.await()
                val schedule = scheduleDeferred.await()
                val today = todayDeferred.await()
                // T-16: si la sincronización de cursos falla, igualmente
                // seguimos mostrando la distribución con colores neutros
                // (no es un error bloqueante, solo pierde el color).
                val firstCourses = try {
                    colorsDeferred.await()
                } catch (e: Exception) {
                    emptyList<pe.khipuai.app.data.local.entity.CourseEntity>()
                }
                val colorByName = firstCourses.associate { it.name to it.color }
                val distributionWithColor = stats.courseDistribution.map { item ->
                    CourseDistributionWithColor(
                        item = item,
                        colorHex = colorByName[item.courseName],
                    )
                }

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
                        courseDistributionWithColor = distributionWithColor,
                        weeklySchedule = schedule,
                        hardestConcepts = hardest,
                        dailyCompletedConcepts = 0, // Se actualizará cuando el backend rastree repasos de hoy
                        dailyGoalConcepts = today.size.coerceAtLeast(5) // Usa los conceptos pendientes como meta
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = pe.khipuai.app.core.network.NetworkErrorMapper.from(e).message) }
            }
        }
    }
}

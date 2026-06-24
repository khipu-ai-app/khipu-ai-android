package pe.khipuai.app.ui.screens.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.khipuai.app.core.network.NetworkErrorMapper
import pe.khipuai.app.data.repository.AuthRepository
import javax.inject.Inject

data class PricingPlanUiModel(
    val id: String,
    val name: String,
    val price: String,
    val period: String,
    val description: String,
    val isHighlighted: Boolean = false,
    val badgeText: String? = null,
    val buttonText: String
)

data class FeatureComparisonUiModel(
    val name: String,
    val iconName: String,
    val freeValue: String,
    val proHasFeature: Boolean
)

data class SubscriptionUiState(
    val plans: List<PricingPlanUiModel> = emptyList(),
    val features: List<FeatureComparisonUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val isChangingPlan: Boolean = false,
    val currentPlan: String = "free",
    val isPro: Boolean = false,
    val errorMessage: String? = null,
    val pendingNavigationReason: String? = null
)

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    init {
        loadSubscriptionData()
        loadCurrentPlan()
    }

    private fun loadSubscriptionData() {
        _uiState.update {
            it.copy(
                plans = listOf(
                    PricingPlanUiModel(
                        id = "free",
                        name = "Free",
                        price = "$0",
                        period = "/ mes",
                        description = "5 capturas mensuales, todas las features principales.",
                        isHighlighted = false,
                        buttonText = "Plan actual"
                    ),
                    PricingPlanUiModel(
                        id = "pro",
                        name = "Pro",
                        price = "$9.99",
                        period = "/ mes",
                        description = "Capturas ilimitadas, sin restricciones.",
                        isHighlighted = true,
                        badgeText = "Recomendado",
                        buttonText = "Hazte Pro"
                    )
                ),
                features = listOf(
                    FeatureComparisonUiModel("Capturas por mes", "cloud", "5", true),
                    FeatureComparisonUiModel("Mapa mental avanzado", "map", "", true),
                    FeatureComparisonUiModel("Tutor IA ilimitado", "school", "10 msgs/día", true),
                    FeatureComparisonUiModel("Soporte prioritario", "mail", "", true)
                )
            )
        }
    }

    private fun loadCurrentPlan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.fetchMySubscription()
                .onSuccess { sub ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentPlan = sub.plan,
                            isPro = sub.isPro
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = NetworkErrorMapper.from(e).message
                        )
                    }
                }
        }
    }

    fun consumeNavigationReason() {
        _uiState.update { it.copy(pendingNavigationReason = null) }
    }

    fun selectPlan(planId: String) {
        if (planId == _uiState.value.currentPlan) return

        viewModelScope.launch {
            _uiState.update { it.copy(isChangingPlan = true, errorMessage = null) }
            authRepository.updateMySubscription(planId)
                .onSuccess { sub ->
                    _uiState.update {
                        it.copy(
                            isChangingPlan = false,
                            currentPlan = sub.plan,
                            isPro = sub.isPro
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isChangingPlan = false,
                            errorMessage = NetworkErrorMapper.from(e).message
                        )
                    }
                }
        }
    }
}

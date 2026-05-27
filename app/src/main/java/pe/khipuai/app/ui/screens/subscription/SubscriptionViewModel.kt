package pe.khipuai.app.ui.screens.subscription

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    val proHasFeature: Boolean // True muestra check, False muestra una línea/guion
)

data class SubscriptionUiState(
    val plans: List<PricingPlanUiModel> = emptyList(),
    val features: List<FeatureComparisonUiModel> = emptyList(),
    val isLoading: Boolean = false
)

class SubscriptionViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    init {
        loadSubscriptionData()
    }

    private fun loadSubscriptionData() {
        _uiState.value = _uiState.value.copy(
            plans = listOf(
                PricingPlanUiModel(
                    id = "monthly",
                    name = "Mensual",
                    price = "$9.99",
                    period = "/ mes",
                    description = "Flexibilidad total, cancela cuando quieras.",
                    isHighlighted = false,
                    buttonText = "Seleccionar Mensual"
                ),
                PricingPlanUiModel(
                    id = "yearly",
                    name = "Anual",
                    price = "$79.99",
                    period = "/ año",
                    description = "Ahorra un 33% en comparación con el plan mensual.",
                    isHighlighted = true,
                    badgeText = "Mejor Valor",
                    buttonText = "Prueba Khipu Pro Gratis"
                )
            ),
            features = listOf(
                FeatureComparisonUiModel("Almacenamiento de Nodos", "cloud", "Hasta 100", true),
                FeatureComparisonUiModel("Mapa Mental Avanzado", "map", "", true),
                FeatureComparisonUiModel("Modo Offline", "wifi_off", "", true),
                FeatureComparisonUiModel("Tutor Pro (IA Avanzada)", "school", "Básico", true)
            )
        )
    }

    fun selectPlan(planId: String) {
        // Disparador listo para integrar la API de pasarela (Stripe/Google Play Billing)
    }
}
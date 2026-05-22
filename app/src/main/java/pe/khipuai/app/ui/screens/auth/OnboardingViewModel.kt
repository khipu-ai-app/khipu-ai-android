package pe.khipuai.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.khipuai.app.data.repository.CourseRepository
import javax.inject.Inject

data class OnboardingUiState(
    val catalogCourses: List<String> = emptyList(),
    val selectedCourses: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val courseRepository: CourseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun loadCatalog(profileType: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            courseRepository.getCatalog(profileType)
                .onSuccess { list ->
                    _uiState.value = _uiState.value.copy(catalogCourses = list, selectedCourses = list, isLoading = false)
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = err.localizedMessage)
                }
        }
    }

    fun toggleCourseSelection(courseName: String) {
        val currentSelected = _uiState.value.selectedCourses.toMutableList()
        if (currentSelected.contains(courseName)) {
            currentSelected.remove(courseName)
        } else {
            currentSelected.add(courseName)
        }
        _uiState.value = _uiState.value.copy(selectedCourses = currentSelected)
    }

    fun saveOnboarding(fullName: String, profileType: String, onComplete: () -> Unit) {
        if (fullName.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Por favor, introduce tu nombre")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            courseRepository.submitOnboarding(fullName, profileType, _uiState.value.selectedCourses)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                    onComplete()
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = err.localizedMessage)
                }
        }
    }
}
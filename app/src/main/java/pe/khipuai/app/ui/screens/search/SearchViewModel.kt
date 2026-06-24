package pe.khipuai.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.khipuai.app.data.remote.KhipuApiService
import pe.khipuai.app.data.remote.dto.SearchConceptResult
import pe.khipuai.app.data.remote.dto.SearchNoteResult
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val notes: List<SearchNoteResult> = emptyList(),
    val concepts: List<SearchConceptResult> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val error: String? = null,
    val hasSearched: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val apiService: KhipuApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        // Here we could load recent searches from SharedPreferences or Room
        _uiState.update { it.copy(
            recentSearches = listOf("Cálculo diferencial", "Derivada", "Bases de datos")
        ) }
    }

    fun onQueryChanged(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }

        searchJob?.cancel()

        if (newQuery.isBlank()) {
            _uiState.update { it.copy(
                notes = emptyList(),
                concepts = emptyList(),
                hasSearched = false,
                isLoading = false,
                error = null
            ) }
            return
        }

        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            delay(400) // Debounce

            try {
                val response = apiService.searchGlobal(newQuery)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            notes = body.notes,
                            concepts = body.concepts,
                            hasSearched = true
                        )
                    }
                } else {
                    val err = pe.khipuai.app.core.network.NetworkErrorMapper.from(retrofit2.HttpException(response))
                    _uiState.update { it.copy(isLoading = false, error = err.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = pe.khipuai.app.core.network.NetworkErrorMapper.from(e).message) }
            }
        }
    }

    fun clearSearch() {
        onQueryChanged("")
    }
}

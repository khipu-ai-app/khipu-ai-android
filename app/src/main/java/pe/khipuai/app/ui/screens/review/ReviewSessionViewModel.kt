package pe.khipuai.app.ui.screens.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.khipuai.app.data.remote.dto.ReviewConceptResponse
import pe.khipuai.app.data.repository.NoteRepository
import pe.khipuai.app.data.repository.PlannerRepository
import javax.inject.Inject

data class ReviewSessionUiState(
    val noteTitle: String = "",
    val courseName: String? = null,
    val concepts: List<ReviewConceptResponse> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val isComplete: Boolean = false,
    val isFlipped: Boolean = false,
    val resultsSummary: ReviewResultsSummary? = null,
    val errorMessage: String? = null,
) {
    val currentConcept: ReviewConceptResponse?
        get() = concepts.getOrNull(currentIndex)

    val progress: String
        get() = "Concepto ${currentIndex + 1} de ${concepts.size}"

    val progressPercent: Float
        get() = if (concepts.isEmpty()) 0f else ((currentIndex) / concepts.size.toFloat())
}

data class ReviewResultsSummary(
    val remembered: Int,
    val forgotten: Int,
    val total: Int,
    val nextReviewDate: String,
    val results: List<ConceptResult>,
)

data class ConceptResult(
    val conceptName: String,
    val rating: Int,
)

@HiltViewModel
class ReviewSessionViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val plannerRepository: PlannerRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val noteId: String? = savedStateHandle["noteId"]
    private val courseId: String? = savedStateHandle["courseId"]
    private val conceptName: String? = savedStateHandle["conceptName"]

    private val conceptResults = mutableListOf<ConceptResult>()

    private val _uiState = MutableStateFlow(ReviewSessionUiState())
    val uiState: StateFlow<ReviewSessionUiState> = _uiState.asStateFlow()

    init {
        loadReviewSession()
    }

    fun loadReviewSession() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            when {
                noteId != null -> {
                    noteRepository.getNoteReviewSession(noteId)
                        .onSuccess { session ->
                            val pendingConcepts = session.concepts.filter { it.isDue }
                            _uiState.value = _uiState.value.copy(
                                noteTitle = session.noteTitle,
                                courseName = session.courseName,
                                concepts = pendingConcepts,
                                isLoading = false,
                                currentIndex = 0,
                                isComplete = pendingConcepts.isEmpty(),
                            )
                        }
                        .onFailure { error ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Error al cargar la sesión de repaso",
                            )
                        }
                }
                conceptName != null -> {
                    // Repaso de un solo concepto (F-09: disparado desde ConceptBottomSheet)
                    noteRepository.getConceptReviewSession(conceptName)
                        .onSuccess { concepts ->
                            _uiState.value = _uiState.value.copy(
                                noteTitle = "Repaso: $conceptName",
                                courseName = null,
                                concepts = concepts,
                                isLoading = false,
                                currentIndex = 0,
                                isComplete = concepts.isEmpty(),
                            )
                        }
                        .onFailure { error ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Error al cargar el concepto",
                            )
                        }
                }
                else -> {
                    // Mazo Diario (Daily Deck) F-10 Opción 1
                    plannerRepository.fetchDailyDeckSession()
                        .onSuccess { concepts ->
                            val pendingConcepts = concepts.filter { it.isDue }
                            _uiState.value = _uiState.value.copy(
                                noteTitle = "Mazo Diario",
                                courseName = "Todos los cursos",
                                concepts = pendingConcepts,
                                isLoading = false,
                                currentIndex = 0,
                                isComplete = pendingConcepts.isEmpty(),
                            )
                        }
                        .onFailure { error ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Error al cargar el mazo diario",
                            )
                        }
                }
            }
        }
    }

    fun toggleFlip() {
        _uiState.value = _uiState.value.copy(isFlipped = !_uiState.value.isFlipped)
    }

    fun submitRating(rating: Int) {
        val concept = _uiState.value.currentConcept ?: return
        val currentIdx = _uiState.value.currentIndex
        val total = _uiState.value.concepts.size

        conceptResults.add(ConceptResult(concept.label, rating))
        _uiState.value = _uiState.value.copy(
            isSubmitting = true,
        )

        viewModelScope.launch {
            plannerRepository.submitReviewRating(concept.conceptId, rating, noteId ?: conceptIdOrName(concept.conceptName))
                .onSuccess {
                    if (currentIdx + 1 >= total) {
                        val remembered = conceptResults.count { it.rating >= 3 }
                        val forgotten = conceptResults.count { it.rating < 3 }
                        val nextReviewDate = _uiState.value.concepts.map { it.nextReviewDate }.minOrNull() ?: ""
                        
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            isComplete = true,
                            resultsSummary = ReviewResultsSummary(
                                remembered = remembered,
                                forgotten = forgotten,
                                total = total,
                                nextReviewDate = nextReviewDate,
                                results = conceptResults.toList(),
                            ),
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            currentIndex = currentIdx + 1,
                            isFlipped = false,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = error.message ?: "Error al guardar tu evaluación",
                    )
                }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Para repasos por concepto (F-09) el `noteId` siempre es null. El backend
     * acepta `note_id` opcional en `POST /planner/review`, así que le pasamos
     * null en lugar de inventar un UUID.
     */
    private fun conceptIdOrName(@Suppress("UNUSED_PARAMETER") name: String): String? = null

    fun restartWithDifficult() {
        val forgottenLabels = conceptResults.filter { it.rating < 3 }.map { it.conceptName }
        val remainingConcepts = _uiState.value.concepts.filter { it.label in forgottenLabels }
        
        conceptResults.clear()
        _uiState.value = _uiState.value.copy(
            concepts = remainingConcepts,
            currentIndex = 0,
            isComplete = false,
            isFlipped = false,
            resultsSummary = null
        )
    }
}

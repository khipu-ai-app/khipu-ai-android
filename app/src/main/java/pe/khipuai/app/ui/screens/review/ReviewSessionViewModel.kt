package pe.khipuai.app.ui.screens.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.khipuai.app.data.remote.dto.ConceptTutorRequest
import pe.khipuai.app.data.remote.dto.ReviewConceptResponse
import pe.khipuai.app.data.repository.NoteRepository
import pe.khipuai.app.data.repository.PlannerRepository
import javax.inject.Inject

enum class ReviewEntryPoint { DAILY_DECK, COURSE, NOTE, CONCEPT }

data class ReviewSessionUiState(
    val noteTitle: String = "",
    val courseName: String? = null,
    val entryPoint: ReviewEntryPoint = ReviewEntryPoint.DAILY_DECK,
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
        get() = if (concepts.isEmpty()) 0f else (currentIndex.toFloat() / concepts.size)

    val canGoBack: Boolean
        get() = currentIndex > 0 && !isComplete && !isSubmitting

    val canSkip: Boolean
        get() = !isFlipped && !isComplete && !isSubmitting
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
                courseId != null -> {
                    plannerRepository.fetchCourseReviewSession(courseId)
                        .onSuccess { concepts ->
                            val pending = concepts.filter { it.isDue }
                            _uiState.value = _uiState.value.copy(
                                noteTitle = "Repaso del curso",
                                entryPoint = ReviewEntryPoint.COURSE,
                                concepts = pending,
                                isLoading = false,
                                isComplete = pending.isEmpty(),
                            )
                        }
                        .onFailure { error ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = pe.khipuai.app.core.network.NetworkErrorMapper.from(error).message,
                            )
                        }
                }
                noteId != null -> {
                    noteRepository.getNoteReviewSession(noteId)
                        .onSuccess { session ->
                            val pending = session.concepts.filter { it.isDue }
                            _uiState.value = _uiState.value.copy(
                                noteTitle = session.noteTitle,
                                courseName = session.courseName,
                                entryPoint = ReviewEntryPoint.NOTE,
                                concepts = pending,
                                isLoading = false,
                                isComplete = pending.isEmpty(),
                            )
                        }
                        .onFailure { error ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = pe.khipuai.app.core.network.NetworkErrorMapper.from(error).message,
                            )
                        }
                }
                conceptName != null -> {
                    noteRepository.getConceptReviewSession(conceptName)
                        .onSuccess { concepts ->
                            _uiState.value = _uiState.value.copy(
                                noteTitle = "Repaso: $conceptName",
                                entryPoint = ReviewEntryPoint.CONCEPT,
                                concepts = concepts,
                                isLoading = false,
                                isComplete = concepts.isEmpty(),
                            )
                        }
                        .onFailure { error ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = pe.khipuai.app.core.network.NetworkErrorMapper.from(error).message,
                            )
                        }
                }
                else -> {
                    plannerRepository.fetchDailyDeckSession()
                        .onSuccess { concepts ->
                            val pending = concepts.filter { it.isDue }
                            _uiState.value = _uiState.value.copy(
                                noteTitle = "Mazo Diario",
                                courseName = "Todos los cursos",
                                entryPoint = ReviewEntryPoint.DAILY_DECK,
                                concepts = pending,
                                isLoading = false,
                                isComplete = pending.isEmpty(),
                            )
                        }
                        .onFailure { error ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = pe.khipuai.app.core.network.NetworkErrorMapper.from(error).message,
                            )
                        }
                }
            }
        }
    }

    fun toggleFlip() {
        _uiState.value = _uiState.value.copy(isFlipped = !_uiState.value.isFlipped)
    }

    fun goBack() {
        val idx = _uiState.value.currentIndex
        if (idx > 0) {
            conceptResults.removeLastOrNull()
            _uiState.value = _uiState.value.copy(
                currentIndex = idx - 1,
                isFlipped = true,
            )
        }
    }

    fun skipConcept() {
        val currentIdx = _uiState.value.currentIndex
        val total = _uiState.value.concepts.size
        if (currentIdx + 1 >= total) {
            _uiState.value = _uiState.value.copy(
                isComplete = true,
                resultsSummary = ReviewResultsSummary(
                    remembered = conceptResults.count { it.rating >= 3 },
                    forgotten = conceptResults.count { it.rating < 3 },
                    total = total,
                    nextReviewDate = _uiState.value.concepts.map { it.nextReviewDate }.minOrNull() ?: "",
                    results = conceptResults.toList(),
                ),
            )
        } else {
            _uiState.value = _uiState.value.copy(
                currentIndex = currentIdx + 1,
                isFlipped = false,
            )
        }
    }

    fun submitRating(rating: Int) {
        val concept = _uiState.value.currentConcept ?: return
        val currentIdx = _uiState.value.currentIndex
        val total = _uiState.value.concepts.size

        if (currentIdx < conceptResults.size) {
            conceptResults[currentIdx] = ConceptResult(concept.label, rating)
        } else {
            conceptResults.add(ConceptResult(concept.label, rating))
        }
        _uiState.value = _uiState.value.copy(isSubmitting = true)

        viewModelScope.launch {
            plannerRepository.submitReviewRating(concept.conceptId, rating, noteId ?: conceptIdOrName(concept.conceptName))
                .onSuccess {
                    if (currentIdx + 1 >= total) {
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            isComplete = true,
                            resultsSummary = ReviewResultsSummary(
                                remembered = conceptResults.count { it.rating >= 3 },
                                forgotten = conceptResults.count { it.rating < 3 },
                                total = total,
                                nextReviewDate = _uiState.value.concepts.map { it.nextReviewDate }.minOrNull() ?: "",
                                results = conceptResults.toList(),
                            ),
                        )
                        // Añadir recompensa de tiempo por terminar el mazo de repaso (5 minutos por defecto)
                        plannerRepository.recordStudySession(5)
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
                        errorMessage = pe.khipuai.app.core.network.NetworkErrorMapper.from(error).message,
                    )
                }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

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

    fun askAboutConcept(
        conceptName: String,
        definition: String?,
        noteTitle: String?,
        question: String,
        onResult: (Result<String>) -> Unit,
    ) {
        viewModelScope.launch {
            val request = ConceptTutorRequest(
                conceptName = conceptName,
                definition = definition,
                noteTitle = noteTitle,
                question = question,
            )
            val result = noteRepository.askAboutConcept(request)
            onResult(result)
        }
    }
}

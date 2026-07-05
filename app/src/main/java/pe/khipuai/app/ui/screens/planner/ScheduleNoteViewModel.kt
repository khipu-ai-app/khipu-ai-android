package pe.khipuai.app.ui.screens.planner

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.khipuai.app.data.notification.ManualScheduleScheduler
import pe.khipuai.app.data.remote.dto.ScheduleDayResponse
import pe.khipuai.app.data.repository.PlannerRepository
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*
import javax.inject.Inject

data class ScheduleNoteUiState(
    val noteId: String = "",
    val noteTitle: String = "",
    val isLoading: Boolean = true,
    val isScheduling: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val weeklySchedule: List<ScheduleDayResponse> = emptyList(),
    val selectedDate: String = "" // YYYY-MM-DD
) {
    val totalLoadSelectedDate: Int
        get() = weeklySchedule.find { it.date == selectedDate }?.count ?: 0
}

@HiltViewModel
class ScheduleNoteViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val plannerRepository: PlannerRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val noteId: String = checkNotNull(savedStateHandle["noteId"])
    private val noteTitle: String = checkNotNull(savedStateHandle["noteTitle"])

    private val _uiState = MutableStateFlow(
        ScheduleNoteUiState(
            noteId = noteId,
            noteTitle = java.net.URLDecoder.decode(noteTitle, "UTF-8")
        )
    )
    val uiState: StateFlow<ScheduleNoteUiState> = _uiState.asStateFlow()

    init {
        // Preseleccionar hoy
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        _uiState.value = _uiState.value.copy(selectedDate = sdf.format(Date()))
        loadWeeklySchedule()
    }

    private fun loadWeeklySchedule() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            plannerRepository.fetchWeeklySchedule()
                .onSuccess { schedule ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        weeklySchedule = schedule
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = pe.khipuai.app.core.network.NetworkErrorMapper.from(error).message
                    )
                }
        }
    }

    fun selectDate(date: String) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
    }

    fun confirmSchedule() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScheduling = true, errorMessage = null)
            val result = plannerRepository.createManualSchedule(noteId, _uiState.value.selectedDate)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isScheduling = false, isSuccess = true)
                runCatching {
                    val target = LocalDate.parse(_uiState.value.selectedDate)
                    ManualScheduleScheduler.schedule(
                        context = context,
                        noteId = noteId,
                        noteTitle = _uiState.value.noteTitle.ifBlank { "Tu apunte" },
                        scheduledDate = target
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isScheduling = false,
                    errorMessage = pe.khipuai.app.core.network.NetworkErrorMapper.from(result.exceptionOrNull() ?: Exception()).message
                )
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

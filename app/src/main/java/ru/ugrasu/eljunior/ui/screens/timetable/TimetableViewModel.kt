package ru.ugrasu.eljunior.ui.screens.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.ugrasu.eljunior.data.model.ScheduleDay
import ru.ugrasu.eljunior.data.repository.AuthRepository
import ru.ugrasu.eljunior.data.repository.ItportScheduleRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class TimetableUiState(
    val isLoading: Boolean = true,
    val schedule: List<ScheduleDay> = emptyList(),
    val error: String? = null,
    val currentDate: LocalDate = LocalDate.now(),
    val groupId: Int? = null,
    val groupName: String? = null,
    val useWebViewFallback: Boolean = false,
    val webViewUrl: String? = null
)

@HiltViewModel
class TimetableViewModel @Inject constructor(
    private val scheduleRepository: ItportScheduleRepository,
    val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimetableUiState())
    val uiState: StateFlow<TimetableUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(currentDate = mondayOfWeek(LocalDate.now())) }
        loadSchedule()
    }

    fun loadSchedule() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    useWebViewFallback = false,
                    webViewUrl = null
                )
            }

            val sessionResult = authRepository.ensureItportSession()
            if (sessionResult.isFailure) {
                val loginResult = authRepository.ensureItportLogin(force = true)
                if (loginResult.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            useWebViewFallback = true,
                            webViewUrl = authRepository.getItportScheduleUrl(),
                            error = null
                        )
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = sessionResult.exceptionOrNull()?.message ?: "Ошибка авторизации"
                    )
                }
                return@launch
            }

            val groupId = sessionResult.getOrNull()
            if (groupId == null) {
                openWebViewFallback()
                return@launch
            }

            val groupName = authRepository.getGroupName()
            val currentDate = _uiState.value.currentDate
            val dateStr = currentDate.format(DateTimeFormatter.ISO_DATE)
            val schedule = scheduleRepository.getScheduleForDate(groupId, dateStr)

            if (schedule.isEmpty()) {
                val loginResult = authRepository.ensureItportLogin()
                if (loginResult.isSuccess) {
                    openWebViewFallback()
                    return@launch
                }
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    schedule = schedule,
                    groupId = groupId,
                    groupName = groupName,
                    error = null
                )
            }
        }
    }

    private fun openWebViewFallback() {
        _uiState.update {
            it.copy(
                isLoading = false,
                useWebViewFallback = true,
                webViewUrl = authRepository.getItportScheduleUrl(),
                error = null
            )
        }
    }

    fun goToPreviousWeek() {
        val previousDate = _uiState.value.currentDate.minusDays(7)
        _uiState.update { it.copy(currentDate = previousDate, useWebViewFallback = false) }
        loadSchedule()
    }

    fun goToNextWeek() {
        val nextDate = _uiState.value.currentDate.plusDays(7)
        _uiState.update { it.copy(currentDate = nextDate, useWebViewFallback = false) }
        loadSchedule()
    }

    fun goToToday() {
        _uiState.update {
            it.copy(
                currentDate = mondayOfWeek(LocalDate.now()),
                useWebViewFallback = false
            )
        }
        loadSchedule()
    }

    private fun mondayOfWeek(date: LocalDate): LocalDate {
        val dayOfWeek = date.dayOfWeek.value
        return if (dayOfWeek == 1) date else date.minusDays((dayOfWeek - 1).toLong())
    }
}

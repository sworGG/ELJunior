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
import android.util.Log

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
    val authRepository: AuthRepository,
    private val scheduleCache: ru.ugrasu.eljunior.data.repository.ScheduleCacheRepository
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

            val loginResult = authRepository.ensureItportLogin()
            if (loginResult.isFailure) {
                val retryLogin = authRepository.ensureItportLogin(force = true)
                if (retryLogin.isFailure) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = retryLogin.exceptionOrNull()?.message ?: "Ошибка авторизации"
                        )
                    }
                    return@launch
                }
            }

            var groupId = authRepository.ensureItportSession().getOrNull()
            if (groupId == null) {
                groupId = authRepository.ensureItportSession(force = true).getOrNull()
            }

            if (groupId == null) {
                openWebViewFallback()
                return@launch
            }

            val groupName = authRepository.getGroupName()
            val currentDate = _uiState.value.currentDate
            val dateStr = currentDate.format(DateTimeFormatter.ISO_DATE)
            val scheduleResult = scheduleRepository.getScheduleForDate(groupId, dateStr)

            scheduleResult.fold(
                onSuccess = { schedule ->
                    if (schedule.isEmpty()) {
                        // try load cached week
                        val weekStartIso = currentDate.toString()
                        val cached = scheduleCache.loadWeekSchedule(weekStartIso)
                        Log.d("TimetableVM", "Schedule empty from network, cachedDays=${cached.size}")
                        if (cached.isEmpty()) {
                            openWebViewFallback(groupId)
                            return@launch
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    schedule = cached,
                                    groupId = groupId,
                                    groupName = groupName,
                                    error = null
                                )
                            }
                            Log.d("TimetableVM", "Using cached schedule on timetable screen")
                            return@launch
                        }
                    }

                    // save schedule for week
                    val weekStartIso = currentDate.toString()
                    scheduleCache.saveWeekSchedule(weekStartIso, schedule)
                    Log.d("TimetableVM", "Saved schedule week=$weekStartIso days=${schedule.size}")

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            schedule = schedule,
                            groupId = groupId,
                            groupName = groupName,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    // on failure, try cached data
                    val weekStartIso = currentDate.toString()
                    val cached = scheduleCache.loadWeekSchedule(weekStartIso)
                    Log.d("TimetableVM", "Schedule load failed: ${error.message}. cachedDays=${cached.size}")
                    if (cached.isNotEmpty()) {
                        _uiState.update {
                            it.copy(isLoading = false, schedule = cached, groupId = groupId, groupName = groupName, error = null)
                        }
                        return@launch
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Ошибка загрузки расписания"
                        )
                    }
                }
            )
        }
    }

    private suspend fun openWebViewFallback(groupId: Int? = null) {
        val url = authRepository.getItportScheduleUrl(
            date = _uiState.value.currentDate,
            groupId = groupId,
            includeDate = false
        )
        _uiState.update {
            it.copy(
                isLoading = false,
                useWebViewFallback = true,
                webViewUrl = url,
                groupId = groupId ?: it.groupId,
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

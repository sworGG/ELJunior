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
    val groupId: Int? = null
)

@HiltViewModel
class TimetableViewModel @Inject constructor(
    private val scheduleRepository: ItportScheduleRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimetableUiState())
    val uiState: StateFlow<TimetableUiState> = _uiState.asStateFlow()

    init {
        // Переходим на понедельник текущей недели
        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek.value // 1 = понедельник, 7 = воскресенье
        val mondayDate = if (dayOfWeek == 1) {
            today
        } else {
            today.minusDays((dayOfWeek - 1).toLong())
        }
        _uiState.update { it.copy(currentDate = mondayDate) }
        loadSchedule()
    }

    fun loadSchedule() {
        viewModelScope.launch {
            val groupId = 8913 // Пока хардкодим, потом будем брать из профиля
            _uiState.update { it.copy(isLoading = true, error = null, groupId = groupId) }

            try {
                val currentDate = _uiState.value.currentDate
                val dateStr = currentDate.format(DateTimeFormatter.ISO_DATE)
                
                val schedule = scheduleRepository.getScheduleForDate(groupId, dateStr)
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        schedule = schedule
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Ошибка загрузки расписания"
                    )
                }
            }
        }
    }

    fun goToPreviousWeek() {
        val previousDate = _uiState.value.currentDate.minusDays(7)
        _uiState.update { it.copy(currentDate = previousDate) }
        loadSchedule()
    }

    fun goToNextWeek() {
        val nextDate = _uiState.value.currentDate.plusDays(7)
        _uiState.update { it.copy(currentDate = nextDate) }
        loadSchedule()
    }

    fun goToToday() {
        _uiState.update { it.copy(currentDate = LocalDate.now()) }
        loadSchedule()
    }
}

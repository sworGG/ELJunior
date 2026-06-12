package ru.ugrasu.eljunior.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.ugrasu.eljunior.data.model.Deadline
import ru.ugrasu.eljunior.data.model.DaySchedule
import ru.ugrasu.eljunior.data.model.LessonType
import ru.ugrasu.eljunior.data.model.ScheduleItem
import ru.ugrasu.eljunior.data.model.UrgentAlert
import ru.ugrasu.eljunior.data.model.UserProfile
import ru.ugrasu.eljunior.data.repository.AuthRepository
import ru.ugrasu.eljunior.data.repository.CourseRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

data class HomeUiState(
    val isLoadingDeadlines: Boolean = false,
    val user: UserProfile? = null,
    val urgentAlert: UrgentAlert? = null,
    val deadlines: List<Deadline> = emptyList(),
    val todaySchedule: DaySchedule? = null,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val courseRepository: CourseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val currentUser = authRepository.currentUser.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    init {
        _uiState.value = HomeUiState(todaySchedule = getMockTodaySchedule())
        loadDeadlines()
    }

    fun loadHomeData() {
        loadDeadlines()
    }

    private fun loadDeadlines() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingDeadlines = true, error = null)

            try {
                val deadlines = withContext(Dispatchers.IO) {
                    courseRepository.getUpcomingDeadlines(limit = 5)
                }

                val urgentAlert = deadlines.firstOrNull { it.isUrgent }?.let {
                    UrgentAlert.fromDeadline(it)
                }

                _uiState.value = _uiState.value.copy(
                    isLoadingDeadlines = false,
                    urgentAlert = urgentAlert,
                    deadlines = deadlines,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingDeadlines = false,
                    error = e.message ?: "Ошибка загрузки данных"
                )
            }
        }
    }

    /**
     * Mock schedule data - in production would come from university API
     */
    private fun getMockTodaySchedule(): DaySchedule {
        val today = LocalDate.now()
        val weekOfYear = today.get(WeekFields.of(Locale.getDefault()).weekOfYear())
        val isEvenWeek = weekOfYear % 2 == 0

        val items = listOf(
            ScheduleItem(
                id = "1",
                subject = "Иностранный язык",
                type = LessonType.PRACTICE,
                teacher = "Иванова М.В.",
                location = "Корпус 1, Ауд. 205",
                building = "1",
                room = "205",
                startTime = LocalTime.of(8, 30),
                endTime = LocalTime.of(10, 0),
                dayOfWeek = today.dayOfWeek,
                isEvenWeek = null
            ),
            ScheduleItem(
                id = "2",
                subject = "Высшая математика",
                type = LessonType.LECTURE,
                teacher = "Проф. Смирнов А.В.",
                location = "Корпус 2, Ауд. 301",
                building = "2",
                room = "301",
                startTime = LocalTime.of(10, 15),
                endTime = LocalTime.of(11, 45),
                dayOfWeek = today.dayOfWeek,
                isEvenWeek = null
            ),
            ScheduleItem(
                id = "3",
                subject = "Программирование",
                type = LessonType.LABORATORY,
                teacher = "Петров И.С.",
                location = "Корпус 1, Ауд. 105",
                building = "1",
                room = "105",
                startTime = LocalTime.of(12, 30),
                endTime = LocalTime.of(14, 0),
                dayOfWeek = today.dayOfWeek,
                isEvenWeek = true
            ),
            ScheduleItem(
                id = "4",
                subject = "Базы данных",
                type = LessonType.PRACTICE,
                teacher = "Козлова Е.А.",
                location = "Корпус 1, Ауд. 107",
                building = "1",
                room = "107",
                startTime = LocalTime.of(14, 15),
                endTime = LocalTime.of(15, 45),
                dayOfWeek = today.dayOfWeek,
                isEvenWeek = null
            )
        ).filter {
            it.isEvenWeek == null || it.isEvenWeek == isEvenWeek
        }

        return DaySchedule(
            date = today,
            dayOfWeek = today.dayOfWeek,
            isEvenWeek = isEvenWeek,
            items = items
        )
    }

    fun refresh() {
        loadHomeData()
    }
}

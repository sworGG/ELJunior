package ru.ugrasu.eljunior.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.ugrasu.eljunior.data.repository.ItportDebtsRepository
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
import ru.ugrasu.eljunior.data.model.Lesson
import ru.ugrasu.eljunior.data.model.LessonType
import ru.ugrasu.eljunior.data.model.ScheduleDay
import ru.ugrasu.eljunior.data.model.ScheduleItem
import ru.ugrasu.eljunior.data.model.UrgentAlert
import ru.ugrasu.eljunior.data.model.UserProfile
import ru.ugrasu.eljunior.data.repository.AuthRepository
import ru.ugrasu.eljunior.data.repository.CourseRepository
import ru.ugrasu.eljunior.data.repository.ItportScheduleRepository
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
import android.util.Log

data class HomeUiState(
    val isLoadingDeadlines: Boolean = false,
    val isLoadingDebts: Boolean = false,
    val isLoadingSchedule: Boolean = false,
    val activeDebtsCount: Int = 0,
    val user: UserProfile? = null,
    val urgentAlert: UrgentAlert? = null,
    val deadlines: List<Deadline> = emptyList(),
    val todaySchedule: DaySchedule? = null,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val courseRepository: CourseRepository,
    private val debtsRepository: ItportDebtsRepository,
    private val scheduleRepository: ItportScheduleRepository,
    private val scheduleCache: ru.ugrasu.eljunior.data.repository.ScheduleCacheRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val currentUser = authRepository.currentUser.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    init {
        loadDeadlines()
        loadDebts()
        loadSchedule()
    }

    fun loadHomeData() {
        loadDeadlines()
        loadDebts()
        loadSchedule()
    }

    private fun loadDebts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDebts = true) }
            try {
                val count = withContext(Dispatchers.IO) {
                    debtsRepository.getActiveDebts().size
                }
                _uiState.update { it.copy(isLoadingDebts = false, activeDebtsCount = count) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingDebts = false) }
            }
        }
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
    private fun loadSchedule() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSchedule = true) }

            try {
                val loginResult = authRepository.ensureItportLogin()
                if (loginResult.isFailure) {
                    authRepository.ensureItportLogin(force = true)
                }

                var groupId = authRepository.ensureItportSession().getOrNull()
                if (groupId == null) {
                    groupId = authRepository.ensureItportSession(force = true).getOrNull()
                }

                val today = LocalDate.now()
                val dateStr = today.format(DateTimeFormatter.ISO_DATE)
                val todaySchedule = if (groupId != null) {
                    val scheduleResult = withContext(Dispatchers.IO) {
                        scheduleRepository.getScheduleForDate(groupId, dateStr)
                    }
                    scheduleResult.fold(
                        onSuccess = { schedule ->
                                    // Save week schedule under monday key
                                    val weekStartIso = mondayOfWeek(today).toString()
                                    scheduleCache.saveWeekSchedule(weekStartIso, schedule)
                                    Log.d("HomeVM", "Saved week schedule days=${schedule.size} for $weekStartIso")
                                    schedule.find { it.date == today }?.toDaySchedule() ?: emptyDaySchedule(today)
                        },
                        onFailure = {
                            // Try load cached week schedule by monday key
                            val weekStartIso = mondayOfWeek(today).toString()
                            val cached = scheduleCache.loadWeekSchedule(weekStartIso)
                            Log.d("HomeVM", "Using cached schedule days=${cached.size} for $weekStartIso")
                            cached.find { it.date == today }?.toDaySchedule() ?: emptyDaySchedule(today)
                        }
                    )
                } else {
                    emptyDaySchedule(today)
                }

                _uiState.update {
                    it.copy(isLoadingSchedule = false, todaySchedule = todaySchedule)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoadingSchedule = false, todaySchedule = emptyDaySchedule(LocalDate.now()))
                }
            }
        }
    }

    private fun ScheduleDay.toDaySchedule(): DaySchedule {
        val weekOfYear = date.get(WeekFields.of(Locale.getDefault()).weekOfYear())
        return DaySchedule(
            date = date,
            dayOfWeek = date.dayOfWeek,
            isEvenWeek = weekOfYear % 2 == 0,
            items = lessons.map { it.toScheduleItem() }
        )
    }

    private fun Lesson.toScheduleItem(): ScheduleItem {
        return ScheduleItem(
            id = id,
            subject = discipline,
            type = mapLessonType(type),
            teacher = "",
            location = if (auditory.isNotBlank()) "Ауд. $auditory" else "",
            building = "",
            room = auditory,
            startTime = timeStart,
            endTime = timeEnd,
            dayOfWeek = date.dayOfWeek,
            isEvenWeek = null
        )
    }

    private fun mapLessonType(type: String?): LessonType {
        return when (type?.lowercase()?.trim()) {
            "лекция", "лек" -> LessonType.LECTURE
            "практика", "пр", "практ" -> LessonType.PRACTICE
            "лабораторная", "лаб" -> LessonType.LABORATORY
            "семинар" -> LessonType.SEMINAR
            else -> LessonType.LECTURE
        }
    }

    private fun emptyDaySchedule(date: LocalDate): DaySchedule {
        val weekOfYear = date.get(WeekFields.of(Locale.getDefault()).weekOfYear())
        return DaySchedule(
            date = date,
            dayOfWeek = date.dayOfWeek,
            isEvenWeek = weekOfYear % 2 == 0,
            items = emptyList()
        )
    }

    private fun mondayOfWeek(date: LocalDate): LocalDate {
        val dayOfWeek = date.dayOfWeek.value
        return if (dayOfWeek == 1) date else date.minusDays((dayOfWeek - 1).toLong())
    }

    fun refresh() {
        loadHomeData()
    }
}

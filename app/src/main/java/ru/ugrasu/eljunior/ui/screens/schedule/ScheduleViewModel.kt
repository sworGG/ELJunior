package ru.ugrasu.eljunior.ui.screens.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.ugrasu.eljunior.data.model.DaySchedule
import ru.ugrasu.eljunior.data.model.LessonType
import ru.ugrasu.eljunior.data.model.ScheduleItem
import ru.ugrasu.eljunior.data.repository.CourseRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

data class ScheduleUiState(
    val isLoading: Boolean = false,
    val selectedDate: LocalDate = LocalDate.now(),
    val weekDays: List<LocalDate> = emptyList(),
    val isEvenWeek: Boolean = false,
    val scheduleItems: List<ScheduleItem> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val courseRepository: CourseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        updateSelectedDate(LocalDate.now())
    }

    fun updateSelectedDate(date: LocalDate) {
        val weekStart = date.with(DayOfWeek.MONDAY)
        val weekDays = (0..6).map { weekStart.plusDays(it.toLong()) }
        val weekOfYear = date.get(WeekFields.of(Locale.getDefault()).weekOfYear())
        val isEvenWeek = weekOfYear % 2 == 0

        _uiState.value = _uiState.value.copy(
            selectedDate = date,
            weekDays = weekDays,
            isEvenWeek = isEvenWeek
        )

        loadScheduleForDate(date, isEvenWeek)
    }

    fun previousWeek() {
        val newDate = _uiState.value.selectedDate.minusWeeks(1)
        updateSelectedDate(newDate)
    }

    fun nextWeek() {
        val newDate = _uiState.value.selectedDate.plusWeeks(1)
        updateSelectedDate(newDate)
    }

    private fun loadScheduleForDate(date: LocalDate, isEvenWeek: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // В будущем здесь будет API запрос
                // val schedule = courseRepository.getScheduleForDate(date)

                // Пока используем mock-данные
                val scheduleItems = generateMockSchedule(date, isEvenWeek)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    scheduleItems = scheduleItems,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    private fun generateMockSchedule(date: LocalDate, isEvenWeek: Boolean): List<ScheduleItem> {
        val dayOfWeek = date.dayOfWeek

        return when (dayOfWeek) {
            DayOfWeek.MONDAY -> listOf(
                ScheduleItem(
                    id = "1",
                    subject = "Математический анализ",
                    teacher = "Иванов И.И.",
                    room = "А-301",
                    building = "Главный корпус",
                    location = "А-301, Главный корпус",
                    startTime = LocalTime.of(8, 30),
                    endTime = LocalTime.of(10, 0),
                    type = LessonType.LECTURE,
                    dayOfWeek = dayOfWeek
                ),
                ScheduleItem(
                    id = "2",
                    subject = "Программирование",
                    teacher = "Петров П.П.",
                    room = "Б-105",
                    building = "Технический корпус",
                    location = "А-301, Главный корпус",
                    startTime = LocalTime.of(10, 15),
                    endTime = LocalTime.of(11, 45),
                    type = LessonType.PRACTICE,
                    dayOfWeek = dayOfWeek
                ),
                ScheduleItem(
                    id = "3",
                    subject = "Физика",
                    teacher = "Сидоров С.С.",
                    room = "В-201",
                    building = "Лабораторный корпус",
                    location = "А-301, Главный корпус",
                    startTime = LocalTime.of(13, 0),
                    endTime = LocalTime.of(14, 30),
                    type = if (isEvenWeek) LessonType.LABORATORY else LessonType.LECTURE,
                    dayOfWeek = dayOfWeek
                )
            )
            DayOfWeek.TUESDAY -> listOf(
                ScheduleItem(
                    id = "4",
                    subject = "История России",
                    teacher = "Козлов К.К.",
                    room = "А-205",
                    building = "Главный корпус",
                    location = "А-301, Главный корпус",
                    startTime = LocalTime.of(10, 15),
                    endTime = LocalTime.of(11, 45),
                    type = LessonType.LECTURE,
                    dayOfWeek = dayOfWeek
                ),
                ScheduleItem(
                    id = "5",
                    subject = "Английский язык",
                    teacher = "Смирнова А.А.",
                    room = "Online",
                    building = "",
                    location = "А-301, Главный корпус",
                    startTime = LocalTime.of(13, 0),
                    endTime = LocalTime.of(14, 30),
                    type = LessonType.PRACTICE,
                    dayOfWeek = dayOfWeek
                )
            )
            DayOfWeek.WEDNESDAY -> listOf(
                ScheduleItem(
                    id = "6",
                    subject = "Математический анализ",
                    teacher = "Иванов И.И.",
                    room = "А-301",
                    building = "Главный корпус",
                    location = "А-301, Главный корпус",
                    startTime = LocalTime.of(8, 30),
                    endTime = LocalTime.of(10, 0),
                    type = LessonType.PRACTICE,
                    dayOfWeek = dayOfWeek
                ),
                ScheduleItem(
                    id = "7",
                    subject = "Базы данных",
                    teacher = "Николаев Н.Н.",
                    room = "Б-210",
                    building = "Технический корпус",
                    location = "А-301, Главный корпус",
                    startTime = LocalTime.of(10, 15),
                    endTime = LocalTime.of(11, 45),
                    type = LessonType.LECTURE,
                    dayOfWeek = dayOfWeek
                )
            )
            DayOfWeek.THURSDAY -> listOf(
                ScheduleItem(
                    id = "8",
                    subject = "Программирование",
                    teacher = "Петров П.П.",
                    room = "Б-105",
                    building = "Технический корпус",
                    location = "А-301, Главный корпус",
                    startTime = LocalTime.of(8, 30),
                    endTime = LocalTime.of(10, 0),
                    type = LessonType.LABORATORY,
                    dayOfWeek = dayOfWeek
                ),
                ScheduleItem(
                    id = "9",
                    subject = "Физика",
                    teacher = "Сидоров С.С.",
                    room = "В-201",
                    building = "Лабораторный корпус",
                    location = "А-301, Главный корпус",
                    startTime = LocalTime.of(10, 15),
                    endTime = LocalTime.of(11, 45),
                    type = LessonType.PRACTICE,
                    dayOfWeek = dayOfWeek
                )
            )
            DayOfWeek.FRIDAY -> listOf(
                ScheduleItem(
                    id = "10",
                    subject = "Базы данных",
                    teacher = "Николаев Н.Н.",
                    room = "Б-210",
                    building = "Технический корпус",
                    location = "А-301, Главный корпус",
                    startTime = LocalTime.of(10, 15),
                    endTime = LocalTime.of(11, 45),
                    type = LessonType.LABORATORY,
                    dayOfWeek = dayOfWeek
                ),
                ScheduleItem(
                    id = "11",
                    subject = "Философия",
                    teacher = "Кузнецов К.К.",
                    room = "А-102",
                    building = "Главный корпус",
                    location = "А-301, Главный корпус",
                    startTime = LocalTime.of(13, 0),
                    endTime = LocalTime.of(14, 30),
                    type = LessonType.LECTURE,
                    dayOfWeek = dayOfWeek
                )
            )
            else -> emptyList()
        }
    }

}

package ru.ugrasu.eljunior.data.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Schedule item for a class/lesson
 */
data class ScheduleItem(
    val id: String,
    val subject: String,
    val type: LessonType,
    val teacher: String,
    val location: String,
    val building: String,
    val room: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val dayOfWeek: DayOfWeek,
    val isEvenWeek: Boolean? = null, // null = every week, true = even, false = od
) {
    fun getFormattedTime(): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        return startTime.format(formatter)
    }

    fun getLocationString(): String {
        return "Корпус $building • Ауд. $room"
    }
}

/**
 * Day schedule with date info
 */
data class DaySchedule(
    val date: LocalDate,
    val dayOfWeek: DayOfWeek,
    val isEvenWeek: Boolean,
    val items: List<ScheduleItem>
) {
    fun getFormattedDate(): String {
        val dayFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale("ru"))
        val dayName = when (dayOfWeek) {
            DayOfWeek.MONDAY -> "ПН"
            DayOfWeek.TUESDAY -> "ВТ"
            DayOfWeek.WEDNESDAY -> "СР"
            DayOfWeek.THURSDAY -> "ЧТ"
            DayOfWeek.FRIDAY -> "ПТ"
            DayOfWeek.SATURDAY -> "СБ"
            DayOfWeek.SUNDAY -> "ВС"
        }
        val weekType = if (isEvenWeek) "Четная" else "Нечетная"
        return "${date.format(dayFormatter)}, $dayName ($weekType)"
    }
}

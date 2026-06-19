package ru.ugrasu.eljunior.data.model

import com.google.gson.annotations.SerializedName
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Moodle calendar event
 */
data class MoodleEvent(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("format") val format: Int?,
    @SerializedName("courseid") val courseId: Int?,
    @SerializedName("groupid") val groupId: Int?,
    @SerializedName("userid") val userId: Int?,
    @SerializedName("modulename") val moduleName: String?,
    @SerializedName("instance") val instance: Int?,
    @SerializedName("eventtype") val eventType: String?,
    @SerializedName("timestart") val timeStart: Long,
    @SerializedName("timeduration") val timeDuration: Int?,
    @SerializedName("visible") val visible: Int?,
    @SerializedName("url") val url: String?,
    @SerializedName("course") val course: EventCourse?
)

data class EventCourse(
    @SerializedName("id") val id: Int,
    @SerializedName("fullname") val fullName: String,
    @SerializedName("shortname") val shortName: String
)

/**
 * Deadline UI model
 */
data class Deadline(
    val id: Int,
    val title: String,
    val courseName: String,
    val courseId: Int?,
    val dueDateTime: LocalDateTime,
    val type: DeadlineType,
    val url: String?,
    val isUrgent: Boolean = false
) {
    companion object {
        fun fromMoodleEvent(event: MoodleEvent): Deadline {
            val dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(event.timeStart),
                ZoneId.systemDefault()
            )

            val type = when (event.moduleName) {
                "assign" -> DeadlineType.ASSIGNMENT
                "quiz" -> DeadlineType.QUIZ
                "forum" -> DeadlineType.FORUM
                else -> DeadlineType.OTHER
            }

            val now = LocalDateTime.now()
            val hoursUntil = ChronoUnit.HOURS.between(now, dateTime)

            return Deadline(
                id = event.id,
                title = event.name,
                courseName = event.course?.shortName ?: "",
                courseId = event.courseId,
                dueDateTime = dateTime,
                type = type,
                url = event.url,
                isUrgent = hoursUntil in 0..24
            )
        }
    }

    fun getFormattedTime(): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        return dueDateTime.format(formatter)
    }

    fun getFormattedDate(): String {
        val formatter = DateTimeFormatter.ofPattern("d MMM")
        return dueDateTime.format(formatter)
    }

    fun isToday(): Boolean {
        val today = LocalDateTime.now().toLocalDate()
        return dueDateTime.toLocalDate() == today
    }

    fun getTimeRemaining(): String {
        val now = LocalDateTime.now()
        val minutes = ChronoUnit.MINUTES.between(now, dueDateTime)

        return when {
            minutes < 0 -> "Просрочено"
            minutes < 60 -> "$minutes мин"
            minutes < 1440 -> "${minutes / 60} ч"
            else -> "${minutes / 1440} дн"
        }
    }
}

enum class DeadlineType(val displayName: String, val icon: String) {
    ASSIGNMENT("Задание", "assignment"),
    QUIZ("Тест", "quiz"),
    FORUM("Форум", "forum"),
    OTHER("Событие", "event")
}

/**
 * Alert/Notification for urgent events
 */
data class UrgentAlert(
    val id: Int,
    val title: String,
    val subtitle: String,
    val courseName: String,
    val moduleInfo: String,
    val dueDateTime: LocalDateTime,
    val url: String?,
    val type: DeadlineType
) {
    companion object {
        fun fromDeadline(deadline: Deadline): UrgentAlert {
            return UrgentAlert(
                id = deadline.id,
                title = deadline.title,
                subtitle = deadline.courseName,
                courseName = deadline.courseName,
                moduleInfo = deadline.type.displayName,
                dueDateTime = deadline.dueDateTime,
                url = deadline.url,
                type = deadline.type
            )
        }
    }

    fun getTimeRemaining(): String {
        val now = LocalDateTime.now()
        val minutes = ChronoUnit.MINUTES.between(now, dueDateTime)

        return when {
            minutes < 0 -> "Просрочено"
            minutes < 60 -> "$minutes мин"
            else -> "${minutes / 60} мин"
        }
    }

    fun getClosingTime(): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        return dueDateTime.format(formatter)
    }
}

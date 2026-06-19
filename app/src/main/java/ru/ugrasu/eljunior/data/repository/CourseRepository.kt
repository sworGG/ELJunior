package ru.ugrasu.eljunior.data.repository

import ru.ugrasu.eljunior.data.api.MoodleApi
import ru.ugrasu.eljunior.data.model.Course
import ru.ugrasu.eljunior.data.model.Deadline
import ru.ugrasu.eljunior.data.model.UrgentAlert
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseRepository @Inject constructor(
    private val moodleApi: MoodleApi,
    private val authRepository: AuthRepository
) {

    suspend fun getUserCourses(): List<Course> {
        val token = authRepository.getToken()
            ?: throw Exception("Не авторизован")
        val userId = authRepository.getUserId()
            ?: throw Exception("ID пользователя не найден")

        val response = moodleApi.getUserCourses(token, userId = userId)

        if (response.isSuccessful && response.body() != null) {
            return response.body()!!.map { Course.fromMoodleCourse(it) }
        } else {
            throw Exception("Не удалось загрузить курсы")
        }
    }

    suspend fun getUpcomingDeadlines(limit: Int = 10): List<Deadline> {
        val token = authRepository.getToken()
            ?: throw Exception("Не авторизован")

        val now = Instant.now().epochSecond
        val nextMonth = Instant.now().plus(30, ChronoUnit.DAYS).epochSecond

        val response = moodleApi.getCalendarEvents(
            token = token,
            timeFrom = now,
            timeTo = nextMonth,
            limit = limit
        )

        if (response.isSuccessful) {
            val events = response.body()?.events ?: emptyList()

            if (events.isEmpty()) {
                // Try fallback endpoint which may return upcoming events differently
                try {
                    val upcomingResp = moodleApi.getUpcomingEvents(token)
                    if (upcomingResp.isSuccessful) {
                        val upEvents = upcomingResp.body()?.events ?: emptyList()
                        if (upEvents.isNotEmpty()) {
                            return upEvents.map { Deadline.fromMoodleEvent(it) }.sortedBy { it.dueDateTime }
                        }
                    }
                } catch (e: Exception) {
                    // ignore and fall through to return empty list
                }
            }

            return events.map { Deadline.fromMoodleEvent(it) }.sortedBy { it.dueDateTime }
        } else {
            throw Exception("Не удалось загрузить дедлайны")
        }
    }

    suspend fun getDeadlines(
        timeFrom: Long,
        timeTo: Long?,
        limit: Int
    ): List<Deadline> {
        val token = authRepository.getToken()
            ?: throw Exception("Не авторизован")

        val response = moodleApi.getCalendarEvents(
            token = token,
            timeFrom = timeFrom,
            timeTo = timeTo,
            limit = limit
        )

        if (response.isSuccessful) {
            val events = response.body()?.events ?: emptyList()

            if (events.isEmpty()) {
                try {
                    val upcomingResp = moodleApi.getUpcomingEvents(token)
                    if (upcomingResp.isSuccessful) {
                        val upEvents = upcomingResp.body()?.events ?: emptyList()
                        if (upEvents.isNotEmpty()) {
                            return upEvents.map { Deadline.fromMoodleEvent(it) }.sortedBy { it.dueDateTime }
                        }
                    }
                } catch (e: Exception) {
                    // ignore and fall through to return empty list
                }
            }

            return events.map { Deadline.fromMoodleEvent(it) }.sortedBy { it.dueDateTime }
        } else {
            throw Exception("Не удалось загрузить дедлайны")
        }
    }

    suspend fun getUrgentAlerts(): List<UrgentAlert> {
        val deadlines = getUpcomingDeadlines(20)
        return deadlines
            .filter { it.isUrgent }
            .take(3)
            .map { UrgentAlert.fromDeadline(it) }
    }

    suspend fun getCourseDetails(courseId: Int): Course {
        val courses = getUserCourses()
        return courses.find { it.id == courseId }
            ?: throw Exception("Курс не найден")
    }

    suspend fun getCourseContents(courseId: Int): List<ru.ugrasu.eljunior.data.api.CourseSection> {
        val token = authRepository.getToken()
            ?: throw Exception("Не авторизован")

        val response = moodleApi.getCourseContents(token = token, courseId = courseId)

        if (response.isSuccessful && response.body() != null) {
            return response.body()!!
        } else {
            throw Exception("Не удалось загрузить содержимое курса")
        }
    }
}

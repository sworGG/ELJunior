package ru.ugrasu.eljunior.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.ugrasu.eljunior.data.model.ScheduleDay
import ru.ugrasu.eljunior.data.parser.ItportScheduleParser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItportScheduleRepository @Inject constructor(
    private val httpClient: OkHttpClient,
    private val authRepository: AuthRepository,
    private val parser: ItportScheduleParser
) {

    suspend fun getScheduleForDate(groupId: Int, date: String): List<ScheduleDay> {
        return getScheduleForWeek(groupId, date)
    }

    suspend fun getScheduleForWeek(groupId: Int, date: String): List<ScheduleDay> {
        return withContext(Dispatchers.IO) {
            loadSchedule(groupId, date, retryOnAuthFailure = true)
        }
    }

    private suspend fun loadSchedule(
        groupId: Int,
        date: String,
        retryOnAuthFailure: Boolean
    ): List<ScheduleDay> {
        return try {
            val url = "https://itport.ugrasu.ru/timetable/group/$groupId/$date"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("ItportSchedule", "Ошибка загрузки расписания: ${response.code}")
                    if (retryOnAuthFailure && response.code in listOf(401, 403)) {
                        return refreshSessionAndRetry(groupId, date)
                    }
                    return emptyList()
                }

                val html = response.body?.string().orEmpty()
                if (html.isBlank()) {
                    return emptyList()
                }

                if (isLoginPage(html)) {
                    Log.w("ItportSchedule", "Сессия itport истекла")
                    if (retryOnAuthFailure) {
                        return refreshSessionAndRetry(groupId, date)
                    }
                    return emptyList()
                }

                val schedule = parser.parseScheduleHtml(html, groupId.toString())
                Log.d("ItportSchedule", "Загружено расписание: ${schedule.size} дней")
                schedule
            }
        } catch (e: Exception) {
            Log.e("ItportSchedule", "Ошибка при загрузке расписания", e)
            emptyList()
        }
    }

    private suspend fun refreshSessionAndRetry(groupId: Int, date: String): List<ScheduleDay> {
        val session = authRepository.ensureItportSession(force = true)
        val resolvedGroupId = session.getOrNull() ?: groupId
        return loadSchedule(resolvedGroupId, date, retryOnAuthFailure = false)
    }

    private fun isLoginPage(html: String): Boolean {
        return html.contains("kt_login_signin_form") ||
            html.contains("Elios 2.0 - Авторизация")
    }
}

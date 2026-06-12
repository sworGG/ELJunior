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

    suspend fun getScheduleForWeek(groupId: Int, date: String): List<ScheduleDay> {
        return withContext(Dispatchers.IO) {
            try {
                // Получаем Moodle токен для авторизации
                val token = authRepository.getToken()
                
                // Получаем HTML страницу расписания
                val url = "https://itport.ugrasu.ru/timetable/group/$groupId/$date"
                
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $token")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36")
                    .build()

                val response = httpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    Log.w("ItportSchedule", "Ошибка загрузки расписания: ${response.code}")
                    // Даже если ошибка, может быть cookies сработают на следующий раз
                    return@withContext emptyList()
                }

                val html = response.body?.string()
                    ?: throw Exception("Пустой ответ от сервера")

                Log.d("ItportSchedule", "Получен HTML: ${html.length} символов")

                // Парсим HTML
                val schedule = parser.parseScheduleHtml(html, groupId.toString())
                
                Log.d("ItportSchedule", "Загружено расписание: ${schedule.size} дней")
                
                schedule
            } catch (e: Exception) {
                Log.e("ItportSchedule", "Ошибка при загрузке расписания", e)
                emptyList()
            }
        }
    }

    suspend fun getScheduleForDate(groupId: Int, date: String): List<ScheduleDay> {
        return getScheduleForWeek(groupId, date)
    }
}

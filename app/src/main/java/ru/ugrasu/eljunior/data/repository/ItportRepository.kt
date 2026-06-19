package ru.ugrasu.eljunior.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.ugrasu.eljunior.data.model.AcademicProgressData
import ru.ugrasu.eljunior.data.model.EliosNotification
import ru.ugrasu.eljunior.data.model.ItportAcademicRecordDto
import ru.ugrasu.eljunior.data.model.ItportNotificationDto
import ru.ugrasu.eljunior.data.model.toAcademicProgressData
import ru.ugrasu.eljunior.data.model.toDomainNotifications
import ru.ugrasu.eljunior.util.toUserMessage
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItportRepository @Inject constructor(
    private val httpClient: OkHttpClient,
    private val authRepository: AuthRepository,
    private val gson: Gson
) {
    companion object {
        private const val BASE_URL = "https://itport.ugrasu.ru"
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36"
    }

    suspend fun fetchAcademicProgress(): Result<AcademicProgressData> {
        return withContext(Dispatchers.IO) {
            try {
                val loginResult = authRepository.ensureItportLogin()
                if (loginResult.isFailure) {
                    return@withContext Result.failure(
                        loginResult.exceptionOrNull() ?: Exception("Не удалось войти в itport")
                    )
                }

                val response = executePost(
                    "$BASE_URL/lk/stud/academicProgress/getAcademicProgress"
                )

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Не удалось загрузить успеваемость (${response.code})")
                    )
                }

                val body = response.body?.string().orEmpty()
                response.close()

                if (body.isBlank()) {
                    return@withContext Result.success(
                        AcademicProgressData(passed = emptyList(), debts = emptyList(), averageGrade = null)
                    )
                }

                val type = object : TypeToken<List<ItportAcademicRecordDto>>() {}.type
                val items: List<ItportAcademicRecordDto> = try {
                    gson.fromJson(body, type) ?: emptyList()
                } catch (e: Exception) {
                    // Иногда API возвращает один объект вместо массива
                    try {
                        val singleItem: ItportAcademicRecordDto? = gson.fromJson(body, ItportAcademicRecordDto::class.java)
                        singleItem?.let { listOf(it) } ?: emptyList()
                    } catch (inner: Exception) {
                        return@withContext Result.failure(
                            Exception("Неверный формат данных академической успеваемости: ${e.toUserMessage()}")
                        )
                    }
                }

                Result.success(items.toAcademicProgressData())
            } catch (e: Exception) {
                Result.failure(Exception("Ошибка загрузки успеваемости: ${e.toUserMessage()}"))
            }
        }
    }

    suspend fun fetchNotifications(): Result<List<EliosNotification>> {
        return withContext(Dispatchers.IO) {
            try {
                val loginResult = authRepository.ensureItportLogin()
                if (loginResult.isFailure) {
                    return@withContext Result.failure(
                        loginResult.exceptionOrNull() ?: Exception("Не удалось войти в itport")
                    )
                }

                val response = executePost(
                    "$BASE_URL/lk/stud/notifications/getAllNotifications"
                )

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Не удалось загрузить уведомления (${response.code})")
                    )
                }

                val body = response.body?.string().orEmpty()
                response.close()

                if (body.isBlank()) {
                    return@withContext Result.success(emptyList())
                }

                val type = object : TypeToken<List<ItportNotificationDto>>() {}.type
                val items: List<ItportNotificationDto> = gson.fromJson(body, type) ?: emptyList()

                Result.success(items.toDomainNotifications())
            } catch (e: Exception) {
                Result.failure(Exception("Ошибка загрузки уведомлений: ${e.toUserMessage()}"))
            }
        }
    }

    private fun executePost(url: String): okhttp3.Response {
        val requestBuilder = Request.Builder()
            .url(url)
            .post(FormBody.Builder().build())
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json, text/plain, */*")
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Referer", "$BASE_URL/lk/")

        getXsrfToken()?.let { token ->
            requestBuilder.header("X-XSRF-TOKEN", token)
        }

        return httpClient.newCall(requestBuilder.build()).execute()
    }

    private fun getXsrfToken(): String? {
        val cookieJar = httpClient.cookieJar
        val cookies = cookieJar.loadForRequest(
            okhttp3.HttpUrl.Builder()
                .scheme("https")
                .host("itport.ugrasu.ru")
                .build()
        )

        val rawToken = cookies.firstOrNull { it.name == "XSRF-TOKEN" }?.value
            ?: return null

        return URLDecoder.decode(rawToken, StandardCharsets.UTF_8.name())
    }
}

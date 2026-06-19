package ru.ugrasu.eljunior.data.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.ugrasu.eljunior.data.model.ItportAcademicItem
import ru.ugrasu.eljunior.data.model.ItportCardData
import ru.ugrasu.eljunior.di.InMemoryCookieJar
import ru.ugrasu.eljunior.util.ItportUrls
import java.net.URLDecoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JSON API личного кабинета студента itport (/lk/stud/...).
 * Перед вызовом нужна активная сессия: [ru.ugrasu.eljunior.data.repository.AuthRepository.ensureItportLogin].
 */
@Singleton
class ItportApiClient @Inject constructor(
    private val httpClient: OkHttpClient,
    private val cookieJar: InMemoryCookieJar
) {
    companion object {
        private const val TAG = "ItportApiClient"
        private const val HOST = ItportUrls.HOST

        const val CARD_DATA = "/lk/stud/cardData/getCardData"
        const val ACADEMIC_PROGRESS = "/lk/stud/academicProgress/getAcademicProgress"

        private val SESSION_WARMUP_URLS = listOf(
            "${ItportUrls.BASE}/a",
            "${ItportUrls.BASE}/lk/stud",
            ItportUrls.STUDENT_TIMETABLE
        )
    }

    private val gson = Gson()

    suspend fun fetchCardData(): Result<ItportCardData> = withContext(Dispatchers.IO) {
        runCatching {
            val json = postJson(CARD_DATA)
            ItportCardDataParser.parse(json, gson)
        }
    }

    suspend fun fetchAcademicProgress(): Result<List<ItportAcademicItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val json = postJson(ACADEMIC_PROGRESS)
            val type = object : TypeToken<List<ItportAcademicItem>>() {}.type
            gson.fromJson<List<ItportAcademicItem>>(json, type).orEmpty()
        }
    }

    /**
     * Обновляет XSRF-TOKEN после входа — без этого POST /lk/stud/... возвращает CSRF mismatch.
     */
    suspend fun warmUpSession(redirectPath: String? = null) = withContext(Dispatchers.IO) {
        redirectPath?.let { getPage(ItportUrls.resolve(it)) }
        SESSION_WARMUP_URLS.forEach { getPage(it) }
    }

    suspend fun isSessionActive(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = executePost(CARD_DATA, "{}")
            when {
                response.isSuccessful && response.body.trimStart().startsWith("[") -> true
                response.code == 403 -> false
                response.code == 401 -> false
                response.body.contains("CSRF token mismatch", ignoreCase = true) -> {
                    warmUpSession()
                    val retry = executePost(CARD_DATA, "{}")
                    retry.isSuccessful && retry.body.trimStart().startsWith("[")
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Session check failed", e)
            false
        }
    }

    private fun postJson(path: String, body: String = "{}"): String {
        val response = executePost(path, body)
        if (!response.isSuccessful) {
            throw Exception("itport API $path вернул ${response.code}")
        }
        return response.body
    }

    private data class PostResult(val code: Int, val body: String) {
        val isSuccessful get() = code in 200..299
    }

    private fun executePost(path: String, body: String): PostResult {
        val url = if (path.startsWith("http")) path else ItportUrls.resolve(path)

        val request = Request.Builder()
            .url(url)
            .post(body.toRequestBody("application/json".toMediaType()))
            .header("User-Agent", userAgent())
            .header("Accept", "application/json, text/plain, */*")
            .header("Content-Type", "application/json")
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Referer", "${ItportUrls.BASE}/a")
            .apply { applyCsrfHeaders(this) }
            .build()

        httpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.w(TAG, "POST $path -> ${response.code}: ${responseBody.take(200)}")
            }
            return PostResult(response.code, responseBody)
        }
    }

    private fun applyCsrfHeaders(builder: Request.Builder) {
        val xsrf = readXsrfToken()
        if (xsrf != null) {
            builder.header("X-XSRF-TOKEN", xsrf)
        } else {
            readMetaCsrfToken()?.let { builder.header("X-CSRF-TOKEN", it) }
        }
    }

    private fun getPage(url: String) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent())
            .build()
        httpClient.newCall(request).execute().use { response ->
            response.body?.string()
            Log.d(TAG, "Warmup GET ${response.request.url} -> ${response.code}")
        }
    }

    private fun readMetaCsrfToken(): String? {
        val page = Request.Builder()
            .url(ItportUrls.LOGIN)
            .header("User-Agent", userAgent())
            .build()
        httpClient.newCall(page).execute().use { response ->
            val html = response.body?.string().orEmpty()
            return Regex("""<meta name="csrf-token" content="([^"]+)"""")
                .find(html)?.groupValues?.get(1)
        }
    }

    private fun readXsrfToken(): String? {
        val raw = cookieJar.getCookiesForHost(HOST)
            .firstOrNull { it.name == "XSRF-TOKEN" }
            ?.value
            ?: return null
        return runCatching { URLDecoder.decode(raw, Charsets.UTF_8.name()) }.getOrNull()
    }

    private fun userAgent(): String {
        return "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}

// Small parser helper for cardData response
private object ItportCardDataParser {
    fun parse(json: String, gson: Gson): ItportCardData {
        val element = gson.fromJson(json, com.google.gson.JsonElement::class.java)
        val obj = when {
            element.isJsonArray -> element.asJsonArray.firstOrNull()?.asJsonObject
            element.isJsonObject -> element.asJsonObject
            else -> null
        } ?: throw Exception("Empty card data")

        val timetableId = obj.get("timetable_id")?.let { field ->
            when {
                field.isJsonNull -> null
                field.isJsonPrimitive && field.asJsonPrimitive.isNumber -> field.asInt
                field.isJsonPrimitive && field.asJsonPrimitive.isString -> field.asString.toIntOrNull()
                else -> null
            }
        }

        return ItportCardData(
            timetableId = timetableId,
            groupName = obj.get("fgroup")?.takeIf { !it.isJsonNull }?.asString,
            fullName = obj.get("ffullname")?.takeIf { !it.isJsonNull }?.asString,
            email = obj.get("femail")?.takeIf { !it.isJsonNull }?.asString,
            averageGrade = obj.get("abs_ball")?.takeIf { !it.isJsonNull }?.asFloat
        )
    }
}

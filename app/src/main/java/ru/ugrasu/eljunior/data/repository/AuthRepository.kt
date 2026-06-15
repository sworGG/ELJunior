package ru.ugrasu.eljunior.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.gson.Gson
import com.google.gson.JsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.ugrasu.eljunior.data.api.MoodleApi
import ru.ugrasu.eljunior.data.model.ItportLoginRequest
import ru.ugrasu.eljunior.data.model.ItportLoginResponse
import ru.ugrasu.eljunior.data.model.ItportStudentCard
import ru.ugrasu.eljunior.data.model.UserProfile
import ru.ugrasu.eljunior.di.InMemoryCookieJar
import ru.ugrasu.eljunior.util.toUserMessage
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moodleApi: MoodleApi,
    private val httpClient: OkHttpClient,
    private val cookieJar: InMemoryCookieJar,
    private val groupDirectory: ItportGroupDirectory
) {
    private val gson = Gson()
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("moodle_token")
        private val USER_ID_KEY = intPreferencesKey("user_id")
        private val USERNAME_KEY = stringPreferencesKey("username")
        private val PASSWORD_KEY = stringPreferencesKey("password")
        private val FIRST_NAME_KEY = stringPreferencesKey("first_name")
        private val LAST_NAME_KEY = stringPreferencesKey("last_name")
        private val FULL_NAME_KEY = stringPreferencesKey("full_name")
        private val AVATAR_URL_KEY = stringPreferencesKey("avatar_url")
        private val GROUP_ID_KEY = intPreferencesKey("itport_group_id")
        private val GROUP_NAME_KEY = stringPreferencesKey("itport_group_name")

        private const val ITPORT_HOST = "itport.ugrasu.ru"
        private const val ITPORT_BASE_URL = "https://itport.ugrasu.ru"
        private const val ITPORT_LOGIN_URL = "$ITPORT_BASE_URL/login"
        private const val ITPORT_STUDENT_TIMETABLE_URL = "$ITPORT_BASE_URL/timetable/student"
        private const val ITPORT_STUDENT_CARD_URL = "$ITPORT_BASE_URL/lk/stud/cardData/getCardData"
        private val CSRF_PATTERN = Regex("""<meta name="csrf-token" content="([^"]+)"""")

        private val GROUP_PROBE_URLS = listOf(
            ITPORT_STUDENT_TIMETABLE_URL,
            "$ITPORT_BASE_URL/dashboard"
        )

        private val GROUP_NAME_PATTERN = Regex("""[А-ЯA-ZIVXLC]{2,6}\d{1,3}[А-ЯA-Zа-яa-z]?""")
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[TOKEN_KEY] != null
    }

    val currentUser: Flow<UserProfile?> = context.dataStore.data.map { preferences ->
        val userId = preferences[USER_ID_KEY] ?: return@map null
        UserProfile(
            id = userId,
            username = preferences[USERNAME_KEY] ?: "",
            firstName = preferences[FIRST_NAME_KEY] ?: "",
            lastName = preferences[LAST_NAME_KEY] ?: "",
            fullName = preferences[FULL_NAME_KEY] ?: "",
            avatarUrl = preferences[AVATAR_URL_KEY]
        )
    }

    suspend fun getToken(): String? {
        return context.dataStore.data.first()[TOKEN_KEY]
    }

    suspend fun getUserId(): Int? {
        return context.dataStore.data.first()[USER_ID_KEY]
    }

    suspend fun getUsername(): String? {
        return context.dataStore.data.first()[USERNAME_KEY]
    }

    suspend fun getPassword(): String? {
        return context.dataStore.data.first()[PASSWORD_KEY]
    }

    suspend fun login(username: String, password: String): Result<UserProfile> {
        return try {
            // Step 1: Get token
            val tokenResponse = moodleApi.getToken(username, password)

            if (!tokenResponse.isSuccessful) {
                return Result.failure(Exception("Ошибка сервера: ${tokenResponse.code()}"))
            }

            val tokenBody = tokenResponse.body()
            if (tokenBody?.token == null) {
                val errorMessage = tokenBody?.error ?: "Неверный логин или пароль"
                return Result.failure(Exception(errorMessage))
            }

            val token = tokenBody.token

            // Step 2: Get site info
            val siteInfoResponse = moodleApi.getSiteInfo(token)

            if (!siteInfoResponse.isSuccessful) {
                return Result.failure(Exception("Не удалось получить данные пользователя"))
            }

            val siteInfo = siteInfoResponse.body()
                ?: return Result.failure(Exception("Пустой ответ от сервера"))

            // Step 3: Save to DataStore (включая пароль)
            val userProfile = UserProfile.fromSiteInfo(siteInfo)
            saveUserData(token, userProfile, password)

            // itport — в фоне, чтобы расписание открывалось без повторного входа
            backgroundScope.launch {
                ensureItportSession(force = true)
            }

            Result.success(userProfile)
        } catch (e: Exception) {
            Result.failure(Exception("Ошибка подключения: ${e.toUserMessage()}"))
        }
    }

    private suspend fun saveUserData(token: String, user: UserProfile, password: String = "") {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[USER_ID_KEY] = user.id
            preferences[USERNAME_KEY] = user.username
            if (password.isNotEmpty()) {
                preferences[PASSWORD_KEY] = password
            }
            preferences[FIRST_NAME_KEY] = user.firstName
            preferences[LAST_NAME_KEY] = user.lastName
            preferences[FULL_NAME_KEY] = user.fullName
            user.avatarUrl?.let { preferences[AVATAR_URL_KEY] = it }
        }
    }

    suspend fun getGroupId(): Int? {
        return context.dataStore.data.first()[GROUP_ID_KEY]
    }

    suspend fun getGroupName(): String? {
        return context.dataStore.data.first()[GROUP_NAME_KEY]
    }

    /**
     * Гарантирует активную сессию itport (без определения группы).
     */
    suspend fun ensureItportLogin(force: Boolean = false): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (!force && cookieJar.hasSessionCookie(ITPORT_HOST)) {
                    return@withContext Result.success(Unit)
                }

                val loginResponse = performItportLogin(
                    username = getUsername()
                        ?: return@withContext Result.failure(Exception("Не авторизован")),
                    password = getPassword()
                        ?: return@withContext Result.failure(Exception("Войдите заново, чтобы открыть расписание"))
                )
                visitItportRedirect(loginResponse.redirect)
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("AuthRepository", "Ошибка входа itport", e)
                Result.failure(Exception("Не удалось войти в itport: ${e.toUserMessage()}"))
            }
        }
    }

    /**
     * Гарантирует активную сессию itport и возвращает ID учебной группы.
     */
    suspend fun ensureItportSession(force: Boolean = false): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                if (!force) {
                    val cachedId = getGroupId()
                    if (cachedId != null) {
                        if (cookieJar.hasSessionCookie(ITPORT_HOST)) {
                            resolveGroupFromStudentCard()?.let { resolved ->
                                if (resolved.id != cachedId) {
                                    saveGroupInfo(resolved.id, resolved.name)
                                    return@withContext Result.success(resolved.id)
                                }
                            }
                            findGroupNameFromMoodle()?.let { groupName ->
                                resolveGroupFromDirectory(groupName)?.let { resolved ->
                                    if (resolved.id != cachedId) {
                                        saveGroupInfo(resolved.id, resolved.name)
                                        return@withContext Result.success(resolved.id)
                                    }
                                }
                            }
                        }
                        return@withContext Result.success(cachedId)
                    }
                }

                val username = getUsername()
                    ?: return@withContext Result.failure(Exception("Не авторизован"))
                val password = getPassword()
                    ?: return@withContext Result.failure(Exception("Войдите заново, чтобы открыть расписание"))

                val loginResponse = performItportLogin(username, password)
                visitItportRedirect(loginResponse.redirect)
                val resolvedGroup = resolveGroupIdAfterLogin(loginResponse.redirect)
                    ?: return@withContext Result.failure(Exception("Не удалось определить учебную группу"))

                saveGroupInfo(resolvedGroup.id, resolvedGroup.name)
                Result.success(resolvedGroup.id)
            } catch (e: Exception) {
                Log.e("AuthRepository", "Ошибка сессии itport", e)
                Result.failure(Exception("Не удалось войти в itport: ${e.toUserMessage()}"))
            }
        }
    }

    private fun performItportLogin(username: String, password: String): ItportLoginResponse {
        val loginPage = fetchItportPage(ITPORT_LOGIN_URL)
        val csrfToken = extractCsrfToken(loginPage.html)
            ?: throw Exception("Не удалось получить CSRF-токен")

        val loginResponse = postItportLogin(username, password, csrfToken)
        if (loginResponse.redirect.isNullOrBlank()) {
            val message = loginResponse.message ?: "Ошибка авторизации на itport"
            throw Exception(message)
        }

        Log.d("AuthRepository", "itport redirect: ${loginResponse.redirect}")
        return loginResponse
    }

    private data class ResolvedGroup(val id: Int, val name: String?)

    private suspend fun resolveGroupIdAfterLogin(redirect: String?): ResolvedGroup? {
        resolveGroupFromStudentCard()?.let { return it }

        findGroupNameFromMoodle()?.let { groupName ->
            resolveGroupFromDirectory(groupName)?.let { return it }
        }

        val candidates = buildList {
            redirect?.let { add(resolveItportUrl(it)) }
            addAll(GROUP_PROBE_URLS)
        }.distinct()

        candidates.forEach { url ->
            val page = fetchItportPage(url)
            ItportGroupResolver.fromPage(page.html, page.finalUrl)?.let { groupId ->
                return ResolvedGroup(groupId, ItportGroupResolver.extractGroupName(page.html))
            }
        }

        return null
    }

    private fun resolveGroupFromDirectory(groupName: String): ResolvedGroup? {
        val groupId = groupDirectory.findGroupIdByName(groupName) ?: return null
        Log.d("AuthRepository", "Группа из справочника: $groupName -> $groupId")
        return ResolvedGroup(groupId, groupName.trim())
    }

    private fun resolveGroupFromStudentCard(): ResolvedGroup? {
        fetchItportPage("$ITPORT_BASE_URL/lk/stud")
        val card = fetchStudentCard() ?: return null
        val timetableId = card.timetableId
        if (timetableId == null || timetableId <= 0) return null

        Log.d("AuthRepository", "Группа из ЛК: timetable_id=$timetableId")
        val groupName = fetchItportPage("$ITPORT_BASE_URL/timetable/group/$timetableId")
            .let { page -> ItportGroupResolver.extractGroupName(page.html) }

        return ResolvedGroup(timetableId, groupName)
    }

    private fun fetchStudentCard(): ItportStudentCard? {
        val xsrfToken = getXsrfToken()
        val body = "{}".toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(ITPORT_STUDENT_CARD_URL)
            .post(body)
            .header("User-Agent", defaultUserAgent())
            .header("Accept", "application/json")
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Referer", "$ITPORT_BASE_URL/lk/stud")
            .apply { xsrfToken?.let { header("X-XSRF-TOKEN", it) } }
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.w("AuthRepository", "getCardData: HTTP ${response.code}, body=${responseBody.take(200)}")
                    return null
                }
                parseStudentCardResponse(responseBody)
            }
        } catch (e: Exception) {
            Log.w("AuthRepository", "getCardData failed", e)
            null
        }
    }

    private fun parseStudentCardResponse(body: String): ItportStudentCard? {
        if (body.isBlank()) return null

        return try {
            val element = gson.fromJson(body, JsonElement::class.java)
            val obj = when {
                element.isJsonArray -> element.asJsonArray.firstOrNull()?.asJsonObject
                element.isJsonObject -> element.asJsonObject
                else -> null
            } ?: return null

            val timetableId = obj.get("timetable_id")?.let { field ->
                when {
                    field.isJsonNull -> null
                    field.isJsonPrimitive && field.asJsonPrimitive.isNumber -> field.asInt
                    field.isJsonPrimitive && field.asJsonPrimitive.isString -> field.asString.toIntOrNull()
                    else -> null
                }
            }

            ItportStudentCard(
                timetableId = timetableId,
                fullName = obj.get("ffullname")?.takeIf { !it.isJsonNull }?.asString
            )
        } catch (e: Exception) {
            Log.w("AuthRepository", "parseStudentCardResponse failed", e)
            null
        }
    }

    private fun getXsrfToken(): String? {
        return cookieJar.getCookiesForHost(ITPORT_HOST)
            .find { it.name == "XSRF-TOKEN" }
            ?.value
            ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
    }

    private fun visitItportRedirect(redirect: String?) {
        if (redirect.isNullOrBlank()) return
        fetchItportPage(resolveItportUrl(redirect))
    }

    private suspend fun findGroupNameFromMoodle(): String? {
        return try {
            val token = getToken() ?: return null
            val userId = getUserId() ?: return null
            val response = moodleApi.getUserProfile(token, userId = userId)
            if (!response.isSuccessful) return null

            response.body()?.firstOrNull()?.customfields.orEmpty()
                .let { fields ->
                    fields.firstNotNullOfOrNull { field ->
                        val shortName = field.shortname?.lowercase().orEmpty()
                        val name = field.name?.lowercase().orEmpty()
                        val isGroupField = shortName.contains("group") ||
                            shortName.contains("gruppa") ||
                            name.contains("групп")
                        if (isGroupField) {
                            field.displayvalue?.takeIf { it.isNotBlank() }
                                ?: field.value?.takeIf { it.isNotBlank() }
                        } else {
                            null
                        }
                    } ?: fields.firstNotNullOfOrNull { field ->
                        val value = field.displayvalue?.takeIf { it.isNotBlank() }
                            ?: field.value?.takeIf { it.isNotBlank() }
                            ?: return@firstNotNullOfOrNull null
                        GROUP_NAME_PATTERN.find(value)?.value
                    }
                }
        } catch (e: Exception) {
            Log.w("AuthRepository", "Не удалось получить группу из Moodle", e)
            null
        }
    }

    private fun postItportLogin(
        username: String,
        password: String,
        csrfToken: String
    ): ItportLoginResponse {
        val body = gson.toJson(
            ItportLoginRequest(email = username, password = password)
        ).toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(ITPORT_LOGIN_URL)
            .post(body)
            .header("User-Agent", defaultUserAgent())
            .header("Accept", "application/json")
            .header("X-Requested-With", "XMLHttpRequest")
            .header("X-CSRF-TOKEN", csrfToken)
            .header("Referer", ITPORT_LOGIN_URL)
            .build()

        httpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw Exception("itport вернул код ${response.code}")
            }
            return gson.fromJson(responseBody, ItportLoginResponse::class.java)
                ?: throw Exception("Пустой ответ itport")
        }
    }

    private data class ItportPage(val html: String, val finalUrl: String)

    private fun fetchItportPage(url: String): ItportPage {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", defaultUserAgent())
            .build()

        httpClient.newCall(request).execute().use { response ->
            return ItportPage(
                html = response.body?.string().orEmpty(),
                finalUrl = response.request.url.toString()
            )
        }
    }

    private suspend fun saveGroupInfo(groupId: Int, groupName: String?) {
        context.dataStore.edit { preferences ->
            preferences[GROUP_ID_KEY] = groupId
            groupName?.let { preferences[GROUP_NAME_KEY] = it }
        }
    }

    private fun extractCsrfToken(html: String): String? {
        return CSRF_PATTERN.find(html)?.groupValues?.get(1)
    }

    suspend fun getItportScheduleUrl(
        date: LocalDate? = null,
        groupId: Int? = null,
        includeDate: Boolean = true
    ): String {
        val resolvedGroupId = groupId ?: getGroupId()
        if (resolvedGroupId != null) {
            val base = "$ITPORT_BASE_URL/timetable/group/$resolvedGroupId"
            return if (includeDate && date != null) {
                "$base/${date.format(DateTimeFormatter.ISO_DATE)}"
            } else {
                base
            }
        }
        return ITPORT_STUDENT_TIMETABLE_URL
    }

    private fun resolveItportUrl(pathOrUrl: String): String {
        return if (pathOrUrl.startsWith("http")) {
            pathOrUrl
        } else {
            "https://itport.ugrasu.ru${if (pathOrUrl.startsWith("/")) pathOrUrl else "/$pathOrUrl"}"
        }
    }

    private fun defaultUserAgent(): String {
        return "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }

    fun getItportCookies(): List<okhttp3.Cookie> {
        return cookieJar.getCookiesForHost(ITPORT_HOST)
    }

    fun buildWebViewCookie(cookie: okhttp3.Cookie): String {
        val segments = mutableListOf("${cookie.name}=${cookie.value}")
        if (cookie.path.isNotEmpty()) segments.add("path=${cookie.path}")
        if (cookie.domain.isNotEmpty()) segments.add("domain=${cookie.domain}")
        if (cookie.secure) segments.add("Secure")
        return segments.joinToString("; ")
    }

    suspend fun logout() {
        cookieJar.clear()
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun refreshUserProfile(): Result<UserProfile> {
        val token = getToken() ?: return Result.failure(Exception("Не авторизован"))
        val password = getPassword() ?: ""

        return try {
            val response = moodleApi.getSiteInfo(token)
            if (response.isSuccessful && response.body() != null) {
                val userProfile = UserProfile.fromSiteInfo(response.body()!!)
                saveUserData(token, userProfile, password)
                Result.success(userProfile)
            } else {
                Result.failure(Exception("Не удалось обновить профиль"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(): UserProfile? {
        return currentUser.first()
    }
}

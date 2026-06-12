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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.ugrasu.eljunior.data.api.MoodleApi
import ru.ugrasu.eljunior.data.model.UserProfile
import ru.ugrasu.eljunior.util.toUserMessage
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moodleApi: MoodleApi,
    private val httpClient: OkHttpClient
) {
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("moodle_token")
        private val USER_ID_KEY = intPreferencesKey("user_id")
        private val USERNAME_KEY = stringPreferencesKey("username")
        private val PASSWORD_KEY = stringPreferencesKey("password")
        private val FIRST_NAME_KEY = stringPreferencesKey("first_name")
        private val LAST_NAME_KEY = stringPreferencesKey("last_name")
        private val FULL_NAME_KEY = stringPreferencesKey("full_name")
        private val AVATAR_URL_KEY = stringPreferencesKey("avatar_url")
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
            
            // Step 4: Авторизуемся на itport с теми же учётными данными
            loginToItport(username, password)

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

    /**
     * Автоматическая авторизация на itport.ugrasu.ru
     * Использует те же учётные данные (username/password)
     */
    private suspend fun loginToItport(username: String, password: String) {
        withContext(Dispatchers.IO) {
            try {
                val formBody = FormBody.Builder()
                    .add("login", username)
                    .add("password", password)
                    .add("submit", "Вход")
                    .build()

                val request = Request.Builder()
                    .url("https://itport.ugrasu.ru/login")
                    .post(formBody)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36")
                    .build()

                val response = httpClient.newCall(request).execute()
                
                if (response.isSuccessful) {
                    Log.d("AuthRepository", "Успешная авторизация на itport.ugrasu.ru")
                } else {
                    Log.w("AuthRepository", "Ошибка авторизации на itport: ${response.code}")
                }
                response.close()
            } catch (e: Exception) {
                Log.e("AuthRepository", "Ошибка при попытке авторизации на itport", e)
                // Не выбрасываем ошибку - главная авторизация уже прошла
            }
        }
    }

    suspend fun logout() {
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

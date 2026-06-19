package ru.ugrasu.eljunior.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.ugrasu.eljunior.data.model.ItportDirectoryGroup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItportGroupDirectory @Inject constructor(
    private val httpClient: OkHttpClient
) {
    private val gson = Gson()
    private var cachedGroups: List<ItportDirectoryGroup>? = null

    fun findGroupIdByName(groupName: String): Int? {
        val normalizedQuery = normalizeGroupName(groupName)
        if (normalizedQuery.isBlank()) return null

        val groups = cachedGroups ?: fetchGroups().also { cachedGroups = it }
        return groups.firstOrNull { group ->
            normalizeGroupName(group.name.orEmpty()) == normalizedQuery ||
                normalizeGroupName(group.number.orEmpty()) == normalizedQuery
        }?.groupOid?.takeIf { it > 0 }
    }

    private fun fetchGroups(): List<ItportDirectoryGroup> {
        val request = Request.Builder()
            .url(GROUPS_URL)
            .header("Accept", "application/json")
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string().orEmpty()
                val type = object : TypeToken<List<ItportDirectoryGroup>>() {}.type
                gson.fromJson<List<ItportDirectoryGroup>>(body, type).orEmpty()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val GROUPS_URL = "https://www.ugrasu.ru/api/directory/groups"

        fun normalizeGroupName(name: String): String {
            return name.trim()
                .replace("-", "")
                .replace(" ", "")
                .uppercase()
        }
    }
}

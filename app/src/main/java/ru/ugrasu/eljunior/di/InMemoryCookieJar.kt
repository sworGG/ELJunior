package ru.ugrasu.eljunior.di

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryCookieJar @Inject constructor() : CookieJar {
    private val cookieStore = mutableMapOf<String, MutableMap<String, Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val hostCookies = cookieStore.getOrPut(url.host) { mutableMapOf() }
        cookies.forEach { cookie ->
            hostCookies[cookie.name] = cookie
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookieStore[url.host]?.values?.filter { it.matches(url) } ?: emptyList()
    }

    fun getCookiesForHost(host: String): List<Cookie> {
        return cookieStore[host]?.values?.toList() ?: emptyList()
    }

    fun hasSessionCookie(host: String): Boolean {
        return cookieStore[host]?.containsKey("elios_20_session") == true
    }

    fun clear() {
        cookieStore.clear()
    }
}

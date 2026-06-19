package ru.ugrasu.eljunior.util

import ru.ugrasu.eljunior.BuildConfig

fun normalizeMoodleUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    if (url.startsWith("http://") || url.startsWith("https://")) return url
    val base = BuildConfig.MOODLE_BASE_URL.trimEnd('/')
    return if (url.startsWith("/")) "$base$url" else "$base/$url"
}

package ru.ugrasu.eljunior.util

object ItportUrls {
    const val HOST = "itport.ugrasu.ru"
    const val BASE = "https://itport.ugrasu.ru"
    const val LOGIN = "$BASE/login"
    const val STUDENT_TIMETABLE = "$BASE/timetable/student"
    const val DASHBOARD = "$BASE/dashboard"
    const val LK = "$BASE/lk"

    fun resolve(pathOrUrl: String): String {
        return if (pathOrUrl.startsWith("http")) {
            pathOrUrl
        } else {
            "$BASE${if (pathOrUrl.startsWith("/")) pathOrUrl else "/$pathOrUrl"}"
        }
    }

    fun groupSchedule(groupId: Int, weekStartIso: String): String {
        return "$BASE/timetable/group/$groupId/$weekStartIso"
    }

    fun studentTimetable(timetableId: Int, weekStartIso: String? = null): String {
        return if (weekStartIso != null) {
            "$STUDENT_TIMETABLE/$timetableId/$weekStartIso"
        } else {
            "$STUDENT_TIMETABLE/$timetableId"
        }
    }
}

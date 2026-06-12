package ru.ugrasu.eljunior.data.api

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import ru.ugrasu.eljunior.data.model.MoodleCourse
import ru.ugrasu.eljunior.data.model.MoodleEvent
import ru.ugrasu.eljunior.data.model.SiteInfo
import ru.ugrasu.eljunior.data.model.TokenResponse

/**
 * Moodle Web Services API
 *
 * Documentation: https://docs.moodle.org/dev/Web_service_API_functions
 */
interface MoodleApi {

    /**
     * Get authentication token
     * POST /login/token.php
     */
    @FormUrlEncoded
    @POST("login/token.php")
    suspend fun getToken(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("service") service: String = "moodle_mobile_app"
    ): Response<TokenResponse>

    /**
     * Get site info and user data
     * core_webservice_get_site_info
     */
    @GET("webservice/rest/server.php")
    suspend fun getSiteInfo(
        @Query("wstoken") token: String,
        @Query("wsfunction") function: String = "core_webservice_get_site_info",
        @Query("moodlewsrestformat") format: String = "json"
    ): Response<SiteInfo>

    /**
     * Get enrolled courses for current user
     * core_enrol_get_users_courses
     */
    @GET("webservice/rest/server.php")
    suspend fun getUserCourses(
        @Query("wstoken") token: String,
        @Query("wsfunction") function: String = "core_enrol_get_users_courses",
        @Query("userid") userId: Int,
        @Query("moodlewsrestformat") format: String = "json"
    ): Response<List<MoodleCourse>>

    /**
     * Get calendar events (deadlines, assignments, etc.)
     * core_calendar_get_action_events_by_timesort
     */
    @GET("webservice/rest/server.php")
    suspend fun getCalendarEvents(
        @Query("wstoken") token: String,
        @Query("wsfunction") function: String = "core_calendar_get_action_events_by_timesort",
        @Query("timesortfrom") timeFrom: Long,
        @Query("timesortto") timeTo: Long? = null,
        @Query("limitnum") limit: Int = 20,
        @Query("moodlewsrestformat") format: String = "json"
    ): Response<CalendarEventsResponse>

    /**
     * Get upcoming calendar events
     * core_calendar_get_calendar_upcoming_view
     */
    @GET("webservice/rest/server.php")
    suspend fun getUpcomingEvents(
        @Query("wstoken") token: String,
        @Query("wsfunction") function: String = "core_calendar_get_calendar_upcoming_view",
        @Query("moodlewsrestformat") format: String = "json"
    ): Response<UpcomingEventsResponse>

    /**
     * Get course contents (modules, sections)
     * core_course_get_contents
     */
    @GET("webservice/rest/server.php")
    suspend fun getCourseContents(
        @Query("wstoken") token: String,
        @Query("wsfunction") function: String = "core_course_get_contents",
        @Query("courseid") courseId: Int,
        @Query("moodlewsrestformat") format: String = "json"
    ): Response<List<CourseSection>>

    /**
     * Get user profile
     * core_user_get_users_by_field
     */
    @GET("webservice/rest/server.php")
    suspend fun getUserProfile(
        @Query("wstoken") token: String,
        @Query("wsfunction") function: String = "core_user_get_users_by_field",
        @Query("field") field: String = "id",
        @Query("values[0]") userId: Int,
        @Query("moodlewsrestformat") format: String = "json"
    ): Response<List<UserProfileResponse>>
}

/**
 * Calendar events response wrapper
 */
data class CalendarEventsResponse(
    val events: List<MoodleEvent>
)

/**
 * Upcoming events response wrapper
 */
data class UpcomingEventsResponse(
    val events: List<MoodleEvent>
)

/**
 * Course section with modules
 */
data class CourseSection(
    val id: Int,
    val name: String,
    val visible: Int?,
    val summary: String?,
    val modules: List<CourseModule>?
)

/**
 * Course module (activity)
 */
data class CourseModule(
    val id: Int,
    val name: String,
    val instance: Int?,
    val modname: String?,
    val modplural: String?,
    val visible: Int?,
    val url: String?
)

/**
 * User profile response
 */
data class UserCustomField(
    val type: String?,
    val value: String?,
    val displayvalue: String?,
    val name: String?,
    val shortname: String?
)

data class UserProfileResponse(
    val id: Int,
    val username: String,
    val firstname: String,
    val lastname: String,
    val fullname: String,
    val email: String?,
    val profileimageurl: String?,
    val profileimageurlsmall: String?,
    val customfields: List<UserCustomField>?
)

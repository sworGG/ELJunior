package ru.ugrasu.eljunior.data.model

import com.google.gson.annotations.SerializedName

/**
 * Course from Moodle API
 */
data class MoodleCourse(
    @SerializedName("id") val id: Int,
    @SerializedName("shortname") val shortName: String,
    @SerializedName("fullname") val fullName: String,
    @SerializedName("displayname") val displayName: String?,
    @SerializedName("summary") val summary: String?,
    @SerializedName("summaryformat") val summaryFormat: Int?,
    @SerializedName("startdate") val startDate: Long?,
    @SerializedName("enddate") val endDate: Long?,
    @SerializedName("visible") val visible: Int?,
    @SerializedName("progress") val progress: Float?,
    @SerializedName("hasprogress") val hasProgress: Boolean?,
    @SerializedName("isfavourite") val isFavourite: Boolean?,
    @SerializedName("hidden") val hidden: Boolean?,
    @SerializedName("overviewfiles") val overviewFiles: List<OverviewFile>?
)

data class OverviewFile(
    @SerializedName("filename") val filename: String?,
    @SerializedName("filepath") val filepath: String?,
    @SerializedName("fileurl") val fileUrl: String?,
    @SerializedName("mimetype") val mimeType: String?
)

/**
 * Course UI model
 */
data class Course(
    val id: Int,
    val name: String,
    val shortName: String,
    val description: String,
    val imageUrl: String?,
    val progress: Float,
    val isFavourite: Boolean = false
) {
    companion object {
        fun fromMoodleCourse(course: MoodleCourse): Course {
            val plainSummary = course.summary
                ?.replace(Regex("<[^>]*>"), "")
                ?.take(200)
                ?: ""

            return Course(
                id = course.id,
                name = course.displayName ?: course.fullName,
                shortName = course.shortName,
                description = plainSummary,
                imageUrl = course.overviewFiles?.firstOrNull()?.fileUrl,
                progress = course.progress ?: 0f,
                isFavourite = course.isFavourite ?: false
            )
        }
    }
}

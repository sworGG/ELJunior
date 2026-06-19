package ru.ugrasu.eljunior.data.model

import com.google.gson.annotations.SerializedName

/**
 * Moodle token response after successful login
 */
data class TokenResponse(
    @SerializedName("token") val token: String?,
    @SerializedName("privatetoken") val privateToken: String?,
    @SerializedName("error") val error: String?,
    @SerializedName("errorcode") val errorCode: String?
)

/**
 * User site info from Moodle
 */
data class SiteInfo(
    @SerializedName("userid") val userId: Int,
    @SerializedName("username") val username: String,
    @SerializedName("firstname") val firstName: String,
    @SerializedName("lastname") val lastName: String,
    @SerializedName("fullname") val fullName: String,
    @SerializedName("sitename") val siteName: String,
    @SerializedName("userpictureurl") val avatarUrl: String?,
    @SerializedName("lang") val language: String?
)

/**
 * User profile data for UI
 */
data class UserProfile(
    val id: Int,
    val username: String,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val avatarUrl: String?,
    val role: String = "Студент ЮГУ"
) {
    companion object {
        fun fromSiteInfo(siteInfo: SiteInfo): UserProfile {
            return UserProfile(
                id = siteInfo.userId,
                username = siteInfo.username,
                firstName = siteInfo.firstName,
                lastName = siteInfo.lastName,
                fullName = siteInfo.fullName,
                avatarUrl = siteInfo.avatarUrl
            )
        }
    }

    fun getInitials(): String {
        val first = firstName.firstOrNull()?.uppercase() ?: ""
        val last = lastName.firstOrNull()?.uppercase() ?: ""
        return "$first$last"
    }

    fun getShortName(): String {
        val lastInitial = lastName.firstOrNull()?.let { "$it." } ?: ""
        return "$firstName $lastInitial"
    }
}

/**
 * Extended student profile from eluniver.ugrasu.ru (Moodle /my/)
 */
data class StudentPersonalData(
    val id: Int,
    val username: String,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val email: String?,
    val avatarUrl: String?,
    val phone: String?,
    val city: String?,
    val country: String?,
    val institution: String?,
    val department: String?,
    val customFields: List<PersonalDataField>
) {
    companion object {
        fun fromMoodleUser(user: ru.ugrasu.eljunior.data.api.UserProfileResponse): StudentPersonalData {
            val phone = listOfNotNull(user.phone1, user.phone2)
                .firstOrNull { it.isNotBlank() }

            val customFields = user.customfields.orEmpty()
                .mapNotNull { field ->
                    val label = field.name?.takeIf { it.isNotBlank() }
                        ?: field.shortname?.takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null
                    val value = field.displayvalue?.takeIf { it.isNotBlank() }
                        ?: field.value?.takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null
                    PersonalDataField(label = label, value = value)
                }

            return StudentPersonalData(
                id = user.id,
                username = user.username,
                firstName = user.firstname,
                lastName = user.lastname,
                fullName = user.fullname,
                email = user.email?.takeIf { it.isNotBlank() },
                avatarUrl = user.profileimageurl?.takeIf { it.isNotBlank() },
                phone = phone,
                city = user.city?.takeIf { it.isNotBlank() },
                country = user.country?.takeIf { it.isNotBlank() },
                institution = user.institution?.takeIf { it.isNotBlank() },
                department = user.department?.takeIf { it.isNotBlank() },
                customFields = customFields
            )
        }
    }
}

data class PersonalDataField(
    val label: String,
    val value: String
)

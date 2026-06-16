package ru.ugrasu.eljunior.data.model

import com.google.gson.annotations.SerializedName
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Ответ POST /lk/stud/cardData/getCardData (массив, берём [0]).
 */
data class ItportCardData(
    @SerializedName("timetable_id") val timetableId: Int?,
    @SerializedName("fgroup") val groupName: String?,
    @SerializedName("ffullname") val fullName: String?,
    @SerializedName("femail") val email: String?,
    @SerializedName("abs_ball") val averageGrade: Float?
)

/**
 * Элемент POST /lk/stud/academicProgress/getAcademicProgress.
 */
data class ItportAcademicItem(
    @SerializedName("dis") val discipline: String?,
    @SerializedName("discipline") val disciplineAlt: String?,
    @SerializedName("typework") val typeWork: String?,
    @SerializedName("examiner") val examiner: String?,
    @SerializedName("semestr") val semester: String?,
    @SerializedName("fsemester") val semesterAlt: String?,
    @SerializedName("fwmark") val mark: Int?,
    @SerializedName("is_dolg") val isDebt: Int?,
    @SerializedName("zach") val statusLabel: String?,
    @SerializedName("ended_at") val endedAt: String?
) {
    fun toDebt(): Debt {
        val subject = discipline?.takeIf { it.isNotBlank() }
            ?: disciplineAlt?.takeIf { it.isNotBlank() }
            ?: "Дисциплина"

        return Debt(
            id = listOf(subject, typeWork, semester, examiner).joinToString("|"),
            subject = subject,
            debtType = typeWork.orEmpty(),
            teacher = examiner.orEmpty(),
            dueDate = parseDate(endedAt),
            status = if ((isDebt ?: 0) != 0) DebtStatus.ACTIVE else DebtStatus.CLOSED,
            description = statusLabel.orEmpty()
        )
    }

    private fun parseDate(raw: String?): LocalDate? {
        if (raw.isNullOrBlank()) return null
        return try {
            LocalDate.parse(raw.substringBefore("T"), DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (_: Exception) {
            null
        }
    }
}

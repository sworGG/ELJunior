package ru.ugrasu.eljunior.data.model

import com.google.gson.annotations.SerializedName

data class AcademicProgressData(
    val passed: List<AcademicRecord>,
    val debts: List<AcademicRecord>,
    val averageGrade: Float?
)

data class AcademicRecord(
    val discipline: String,
    val grade: String,
    val semester: String?,
    val typeOfWork: String?,
    val examiner: String?,
    val finishedAt: String?,
    val isDebt: Boolean
)

data class ItportAcademicRecordDto(
    @SerializedName("dis") val discipline: String? = null,
    @SerializedName("fwmark") val gradeMark: Int? = null,
    @SerializedName("is_dolg") val isDebtFlag: Int? = null,
    @SerializedName("ended_at") val endedAt: String? = null,
    @SerializedName("semestr") val semester: String? = null,
    @SerializedName("typework") val typeOfWork: String? = null,
    @SerializedName("examiner") val examiner: String? = null,
    @SerializedName("zach") val creditMark: String? = null
) {
    fun toDomain(isDebt: Boolean): AcademicRecord {
        return AcademicRecord(
            discipline = discipline.orEmpty().ifBlank { "Без названия" },
            grade = formatGrade(),
            semester = semester?.takeIf { it.isNotBlank() },
            typeOfWork = typeOfWork?.takeIf { it.isNotBlank() },
            examiner = examiner?.takeIf { it.isNotBlank() },
            finishedAt = endedAt?.takeIf { it.isNotBlank() },
            isDebt = isDebt
        )
    }

    private fun formatGrade(): String {
        val mark = gradeMark ?: return creditMark?.takeIf { it.isNotBlank() } ?: "—"
        return when (mark) {
            0, 1 -> creditMark?.takeIf { it.isNotBlank() } ?: "Зачёт"
            else -> mark.toString()
        }
    }
}

fun AcademicRecord.getDisplayDate(): String? {
    val date = finishedAt?.takeIf { it.isNotBlank() }?.substringBefore(" ") ?: return null
    return if (date.startsWith("9999")) null else date
}

fun List<ItportAcademicRecordDto>.toAcademicProgressData(): AcademicProgressData {
    val passed = filter { (it.isDebtFlag ?: 0) == 0 && (it.gradeMark ?: -1) >= 0 }
        .map { it.toDomain(isDebt = false) }
    val debts = filter { (it.isDebtFlag ?: 0) != 0 }
        .map { it.toDomain(isDebt = true) }

    val grades = passed.mapNotNull { it.grade.toFloatOrNull()?.takeIf { it >= 3 } }
    val average = if (grades.isNotEmpty()) grades.average().toFloat() else null

    return AcademicProgressData(
        passed = passed,
        debts = debts,
        averageGrade = average
    )
}

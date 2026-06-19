package ru.ugrasu.eljunior.data.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class DebtStatus(val displayName: String) {
    ACTIVE("Активна"),
    CLOSED("Закрыта")
}

data class Debt(
    val id: String,
    val subject: String,
    val debtType: String,
    val teacher: String,
    val dueDate: LocalDate?,
    val status: DebtStatus,
    val description: String = ""
) {
    val isActive: Boolean get() = status == DebtStatus.ACTIVE

    fun getFormattedDate(): String {
        val date = dueDate ?: return ""
        val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("ru"))
        return date.format(formatter)
    }
}

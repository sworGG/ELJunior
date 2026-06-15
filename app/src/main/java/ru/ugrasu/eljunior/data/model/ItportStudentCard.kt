package ru.ugrasu.eljunior.data.model

import com.google.gson.annotations.SerializedName

/**
 * Данные студента из личного кабинета itport (POST lk/stud/cardData/getCardData).
 */
data class ItportStudentCard(
    @SerializedName("timetable_id") val timetableId: Int?,
    @SerializedName("ffullname") val fullName: String?
)

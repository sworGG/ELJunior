package ru.ugrasu.eljunior.data.model

import java.time.LocalDate
import java.time.LocalTime

data class Lesson(
    val id: String,
    val date: LocalDate,
    val timeStart: LocalTime,
    val timeEnd: LocalTime,
    val discipline: String,
    val groupName: String,
    val auditory: String,
    val type: String? = null // лекция, практика, лабораторная и т.д.
)

data class ScheduleDay(
    val date: LocalDate,
    val dayOfWeek: String,
    val lessons: List<Lesson>
)

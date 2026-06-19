package ru.ugrasu.eljunior.data.model

data class ScheduleLoadResult(
    val schedule: List<ScheduleDay> = emptyList(),
    val groupId: Int? = null,
    val groupName: String? = null,
    val error: String? = null
)

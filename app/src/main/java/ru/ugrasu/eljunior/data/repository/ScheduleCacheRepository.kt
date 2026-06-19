package ru.ugrasu.eljunior.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.ugrasu.eljunior.data.model.Lesson
import ru.ugrasu.eljunior.data.model.ScheduleDay
import java.io.File
import android.util.Log
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

// Simple DTO classes that serialize to plain JSON-friendly types
data class LessonDto(
    val id: String,
    val dateIso: String,
    val timeStartIso: String,
    val timeEndIso: String,
    val discipline: String,
    val groupName: String,
    val auditory: String,
    val type: String?
)

data class ScheduleDayDto(
    val dateIso: String,
    val dayOfWeek: String,
    val lessons: List<LessonDto>
)

@Singleton
class ScheduleCacheRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private fun fileForWeek(weekStartIso: String): File {
        val name = "schedule_week_${weekStartIso}.json"
        return File(context.filesDir, name)
    }

    suspend fun saveWeekSchedule(weekStartIso: String, schedule: List<ScheduleDay>) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("ScheduleCache", "Saving week schedule for $weekStartIso, days=${schedule.size}")
                val dto = schedule.map { sd ->
                    ScheduleDayDto(
                        dateIso = sd.date.toString(),
                        dayOfWeek = sd.dayOfWeek,
                        lessons = sd.lessons.map { l ->
                            LessonDto(
                                id = l.id,
                                dateIso = l.date.toString(),
                                timeStartIso = l.timeStart.toString(),
                                timeEndIso = l.timeEnd.toString(),
                                discipline = l.discipline,
                                groupName = l.groupName,
                                auditory = l.auditory,
                                type = l.type
                            )
                        }
                    )
                }
                val json = gson.toJson(dto)
                fileForWeek(weekStartIso).writeText(json)
                Log.d("ScheduleCache", "Saved schedule file=${fileForWeek(weekStartIso).absolutePath}")
            } catch (e: Exception) {
                Log.w("ScheduleCache", "Failed to save schedule for $weekStartIso", e)
            }
        }
    }

    suspend fun loadWeekSchedule(weekStartIso: String): List<ScheduleDay> {
        return withContext(Dispatchers.IO) {
            try {
                val file = fileForWeek(weekStartIso)
                if (!file.exists()) {
                    Log.d("ScheduleCache", "No cache file for $weekStartIso")
                    return@withContext emptyList()
                }
                Log.d("ScheduleCache", "Loading cache file=${file.absolutePath}")
                val json = file.readText()
                val type = object : TypeToken<List<ScheduleDayDto>>() {}.type
                val items: List<ScheduleDayDto> = gson.fromJson(json, type) ?: emptyList()
                Log.d("ScheduleCache", "Loaded ${items.size} cached days for $weekStartIso")
                // convert DTOs back to domain models
                items.map { sd ->
                    val lessons = sd.lessons.mapNotNull { ld ->
                        try {
                            Lesson(
                                id = ld.id,
                                date = LocalDate.parse(ld.dateIso),
                                timeStart = LocalTime.parse(ld.timeStartIso),
                                timeEnd = LocalTime.parse(ld.timeEndIso),
                                discipline = ld.discipline,
                                groupName = ld.groupName,
                                auditory = ld.auditory,
                                type = ld.type
                            )
                        } catch (e: Exception) {
                            Log.w("ScheduleCache", "Failed parse lesson dto", e)
                            null
                        }
                    }
                    ScheduleDay(
                        date = LocalDate.parse(sd.dateIso),
                        dayOfWeek = sd.dayOfWeek,
                        lessons = lessons
                    )
                }
            } catch (e: Exception) {
                Log.w("ScheduleCache", "Failed to load schedule cache for $weekStartIso", e)
                emptyList()
            }
        }
    }
}


package ru.ugrasu.eljunior.data.parser

import android.util.Log
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import ru.ugrasu.eljunior.data.model.Lesson
import ru.ugrasu.eljunior.data.model.ScheduleDay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class ItportScheduleParser {

    fun parseScheduleHtml(html: String, groupId: String): List<ScheduleDay> {
        val doc = Jsoup.parse(html)
        val scheduleData = mutableListOf<ScheduleDay>()
        
        Log.d("Parser", "HTML length: ${html.length}")
        Log.d("Parser", "HTML preview: ${html.take(500)}")
        
        // Проверяем все таблицы в документе
        val allTables = doc.select("table")
        Log.d("Parser", "Total tables found: ${allTables.size}")
        allTables.forEachIndexed { idx, table ->
            Log.d("Parser", "Table $idx classes: ${table.className()}")
        }
        
        // Ищем таблицу с расписанием
        val table = doc.select("table.tableView-timetable").firstOrNull()
        if (table == null) {
            Log.e("Parser", "Table not found")
            return emptyList()
        }

        val thead = table.selectFirst("thead")
        val tbody = table.selectFirst("tbody")

        if (thead == null || tbody == null) {
            Log.e("Parser", "thead=$thead, tbody=$tbody")
            return emptyList()
        }

        // Парсим заголовок таблицы для получения времён пар
        val timeSlots = parseTimeSlots(thead)
        Log.d("Parser", "Found time slots: ${timeSlots.size}")

        // Парсим строки с днями и парами
        val rows = tbody.select("tr")
        Log.d("Parser", "Found rows: ${rows.size}")

        rows.forEach { row ->
            val cells = row.select("td")
            if (cells.isNotEmpty()) {
                // Первая колонка содержит дату и день недели
                val dayCell = cells[0]
                val dateInfo = parseDateCell(dayCell)

                if (dateInfo != null) {
                    val (date, dayOfWeek) = dateInfo
                    val lessons = mutableListOf<Lesson>()
                    
                    Log.d("Parser", "Day: $date ($dayOfWeek), cells in row: ${cells.size}")

                    // Остальные колонки содержат пары (индекс соответствует времени)
                    for (i in 1 until cells.size) {
                        val cell = cells[i]
                        val timeSlot = timeSlots.getOrNull(i - 1)
                        
                        // Проверяем есть ли в ячейке пара
                        val lessonDiv = cell.selectFirst(".tableView-lesson")
                        if (lessonDiv != null && timeSlot != null) {
                            val lesson = parseLessonDiv(lessonDiv, timeSlot, date, groupId)
                            if (lesson != null) {
                                lessons.add(lesson)
                                Log.d("Parser", "Added lesson: ${lesson.discipline} at ${lesson.timeStart}")
                            }
                        }
                    }
                    
                    Log.d("Parser", "Day lessons count: ${lessons.size}")

                    scheduleData.add(
                        ScheduleDay(
                            date = date,
                            dayOfWeek = dayOfWeek,
                            lessons = lessons
                        )
                    )
                }
            }
        }

        Log.d("Parser", "Total schedule days: ${scheduleData.size}")
        return scheduleData
    }

    private fun parseTimeSlots(thead: Element): List<Pair<LocalTime, LocalTime>> {
        val slots = mutableListOf<Pair<LocalTime, LocalTime>>()
        val headerCells = thead.select("th")
        
        Log.d("Parser", "Header cells count: ${headerCells.size}")

        // Пропускаем первый th (который "Время")
        for (i in 1 until headerCells.size) {
            val cell = headerCells[i]
            val timeText = cell.html()
            
            Log.d("Parser", "Cell $i HTML: $timeText")
            
            // Парсим время с <br> тегом: "08:15<br>09:35"
            val timeMatch = Regex("""(\d{2}:\d{2})<br[^>]*>(\d{2}:\d{2})""").find(timeText)
            
            if (timeMatch != null) {
                try {
                    val formatter = DateTimeFormatter.ofPattern("HH:mm")
                    val start = LocalTime.parse(timeMatch.groupValues[1], formatter)
                    val end = LocalTime.parse(timeMatch.groupValues[2], formatter)
                    slots.add(Pair(start, end))
                    Log.d("Parser", "Time slot $i: $start - $end")
                } catch (e: Exception) {
                    Log.e("Parser", "Error parsing time", e)
                    // skip invalid time
                }
            } else {
                Log.d("Parser", "No regex match in: $timeText")
            }
        }

        return slots
    }

    private fun parseDateCell(cell: Element): Pair<LocalDate, String>? {
        val text = cell.text().trim()
        Log.d("Parser", "Date cell text: $text")
        
        // Пример: "20 апреля\nпонедельник"
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) {
            Log.d("Parser", "No lines in date cell")
            return null
        }

        val dateText = lines[0]
        val dayOfWeek = lines.getOrNull(1) ?: ""

        // Парсим дату
        val date = parseDateFromText(dateText)
        if (date == null) {
            Log.d("Parser", "Failed to parse date: $dateText")
            return null
        }

        return Pair(date, dayOfWeek)
    }

    private fun parseLessonDiv(
        lessonDiv: Element,
        timeSlot: Pair<LocalTime, LocalTime>,
        date: LocalDate,
        groupId: String
    ): Lesson? {
        // Извлекаем название дисциплины из .disc-name
        val discNameEl = lessonDiv.selectFirst(".disc-name")
        if (discNameEl == null) {
            Log.d("Parser", "No .disc-name found")
            return null
        }

        // Получаем весь текст и извлекаем только название (без типа)
        val fullText = discNameEl.text().trim()
        Log.d("Parser", "Disc full text: $fullText")
        
        // Тип занятия находится в <span> внутри .disc-name
        val typeEl = discNameEl.selectFirst("span")
        val type = typeEl?.text()?.trim()
        Log.d("Parser", "Type: $type")
        
        // Получаем только название дисциплины (удаляем тип)
        val discipline = if (type != null) {
            fullText.replace(type, "").trim()
        } else {
            fullText
        }

        if (discipline.isEmpty()) {
            Log.d("Parser", "Discipline is empty after cleaning")
            return null
        }

        // Пытаемся найти аудиторию
        val cabEl = lessonDiv.selectFirst(".disc-cab")
        val auditory = cabEl?.selectFirst("div")?.text()?.trim() ?: ""
        Log.d("Parser", "Auditory: $auditory")

        return Lesson(
            id = UUID.randomUUID().toString(),
            date = date,
            timeStart = timeSlot.first,
            timeEnd = timeSlot.second,
            discipline = discipline,
            groupName = groupId,
            auditory = auditory,
            type = type
        )
    }

    private fun parseDateFromText(text: String): LocalDate? {
        // Пример: "20 апреля" или "20 мая"
        return try {
            val monthMap = mapOf(
                "января" to 1, "февраля" to 2, "марта" to 3,
                "апреля" to 4, "мая" to 5, "июня" to 6,
                "июля" to 7, "августа" to 8, "сентября" to 9,
                "октября" to 10, "ноября" to 11, "декабря" to 12
            )

            val parts = text.split(" ")
            val day = parts.getOrNull(0)?.toIntOrNull() ?: return null
            val monthName = parts.getOrNull(1) ?: return null
            val month = monthMap[monthName]
            if (month == null) {
                Log.d("Parser", "Unknown month: $monthName")
                return null
            }

            // Определяем год (текущий год)
            val year = LocalDate.now().year

            LocalDate.of(year, month, day)
        } catch (e: Exception) {
            Log.e("Parser", "Error parsing date from '$text'", e)
            null
        }
    }
}

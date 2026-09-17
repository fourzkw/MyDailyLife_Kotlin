package com.mydailylife.schedule.data

import java.net.HttpURLConnection
import java.net.URL
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import kotlin.math.abs

data class CourseIcsImportResult(
    val courses: List<CourseItem>,
    val suggestedTermStart: LocalDate,
)

/**
 * Parses university-style ICS feeds (one VEVENT per class meeting) into [CourseItem]s,
 * merging identical slots across weeks into [CourseItem.teachingWeeks].
 */
object CourseIcsImporter {
    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")
    private val basicDateTime = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    private val basicDate = DateTimeFormatter.ofPattern("yyyyMMdd")

    fun looksLikeUrl(text: String): Boolean {
        val t = text.trim().lowercase()
        return t.startsWith("http://") || t.startsWith("https://")
    }

    fun looksLikeIcs(text: String): Boolean {
        val t = text.trim()
        return t.contains("BEGIN:VCALENDAR", ignoreCase = true)
    }

    fun fetchUrl(url: String): String {
        var current = url.trim()
        repeat(MAX_REDIRECTS) {
            val connection = (URL(current).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 20_000
                readTimeout = 60_000
                requestMethod = "GET"
                setRequestProperty("Accept", "text/calendar,*/*")
                setRequestProperty("User-Agent", "MyDailyLife-ICS")
            }
            val code: Int
            val redirectTo: String?
            val body: String?
            try {
                connection.connect()
                code = connection.responseCode
                redirectTo = if (code in 300..399) {
                    connection.getHeaderField("Location")
                } else {
                    null
                }
                body = if (code in 200..299) {
                    connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } else {
                    null
                }
            } finally {
                connection.disconnect()
            }
            when {
                code in 300..399 -> {
                    val location = redirectTo ?: error("重定向失败（HTTP $code）")
                    current = if (location.startsWith("http")) {
                        location
                    } else {
                        URL(URL(current), location).toString()
                    }
                }
                code in 200..299 -> return body ?: error("下载内容为空")
                else -> error("下载失败（HTTP $code）")
            }
        }
        error("重定向过多")
    }

    fun parse(icsContent: String): CourseIcsImportResult {
        val content = icsContent.removePrefix("\uFEFF").trim()
        if (!looksLikeIcs(content)) error("无效的 ICS 内容（缺少 VCALENDAR）")

        val events = parseEvents(content)
        if (events.isEmpty()) error("未找到有效的课程事件")

        val withSlots = events.mapNotNull { toOccurrence(it) }
        if (withSlots.isEmpty()) error("未能将事件映射到课表节次")

        val earliest = withSlots.minOf { it.date }
        val termStart = earliest.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        val grouped = withSlots.groupBy {
            listOf(it.title, it.teacher, it.location, it.weekday, it.startSlot, it.endSlot)
        }
        val courses = grouped.map { (_, occs) ->
            val sample = occs.first()
            val weeks = occs.map {
                ChronoUnit.WEEKS.between(termStart, it.date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
                    .toInt() + 1
            }.filter { it >= 1 }.distinct().sorted()
            CourseItem(
                id = UUID.randomUUID().toString(),
                title = sample.title,
                teacher = sample.teacher,
                location = sample.location,
                weekday = sample.weekday,
                startSlot = sample.startSlot,
                endSlot = sample.endSlot,
                teachingWeeks = weeks,
                teachingWeekLabel = CourseItem.formatWeekList(weeks),
            )
        }.sortedWith(compareBy({ it.weekday }, { it.startSlot }, { it.title }))

        val maxWeek = courses.flatMap { it.teachingWeeks }.maxOrNull() ?: 1
        return CourseIcsImportResult(
            courses = courses,
            suggestedTermStart = termStart,
        ).also {
            if (it.courses.isEmpty()) error("未解析到课程")
            // touch maxWeek for clarity in callers via courses
            check(maxWeek >= 1)
        }
    }

    private data class RawEvent(
        var summary: String = "",
        var description: String = "",
        var location: String = "",
        var dtStart: String = "",
        var dtEnd: String = "",
    )

    private data class Occurrence(
        val title: String,
        val teacher: String,
        val location: String,
        val weekday: Int,
        val startSlot: Int,
        val endSlot: Int,
        val date: LocalDate,
    )

    private fun parseEvents(content: String): List<RawEvent> {
        val lines = unfoldLines(content.lineSequence().map { it.trimEnd('\r') }.toList())
        val events = mutableListOf<RawEvent>()
        var current: RawEvent? = null
        var inEvent = false
        var inAlarm = false
        for (line in lines) {
            when {
                line.equals("BEGIN:VEVENT", ignoreCase = true) -> {
                    inEvent = true
                    inAlarm = false
                    current = RawEvent()
                }
                line.equals("END:VEVENT", ignoreCase = true) && inEvent -> {
                    current?.takeIf { it.summary.isNotBlank() && it.dtStart.isNotBlank() }
                        ?.let { events += it }
                    current = null
                    inEvent = false
                    inAlarm = false
                }
                line.equals("BEGIN:VALARM", ignoreCase = true) -> inAlarm = true
                line.equals("END:VALARM", ignoreCase = true) -> inAlarm = false
                inEvent && !inAlarm && line.contains(':') -> {
                    val (name, value) = splitProperty(line)
                    val key = name.substringBefore(';').uppercase()
                    val ev = current ?: continue
                    when (key) {
                        "SUMMARY" -> ev.summary = unescape(value)
                        "DESCRIPTION" -> ev.description = unescape(value)
                        "LOCATION" -> ev.location = unescape(value)
                        "DTSTART" -> ev.dtStart = value.trim()
                        "DTEND" -> ev.dtEnd = value.trim()
                    }
                }
            }
        }
        return events
    }

    private fun unfoldLines(raw: List<String>): List<String> {
        if (raw.isEmpty()) return emptyList()
        val out = mutableListOf<String>()
        val buf = StringBuilder(raw.first())
        for (i in 1 until raw.size) {
            val line = raw[i]
            if (line.startsWith(" ") || line.startsWith("\t")) {
                buf.append(line.drop(1))
            } else {
                out += buf.toString()
                buf.clear()
                buf.append(line)
            }
        }
        out += buf.toString()
        return out
    }

    private fun splitProperty(line: String): Pair<String, String> {
        val idx = line.indexOf(':')
        if (idx < 0) return line to ""
        return line.substring(0, idx) to line.substring(idx + 1)
    }

    private fun unescape(value: String): String =
        value.replace("\\n", "\n")
            .replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\\\", "\\")

    private fun toOccurrence(event: RawEvent): Occurrence? {
        val start = parseDateTime(event.dtStart) ?: return null
        val end = parseDateTime(event.dtEnd) ?: start.plusMinutes(45)
        val startSlot = slotForStart(start.toLocalTime()) ?: return null
        val endSlot = slotForEnd(end.toLocalTime(), startSlot)
        return Occurrence(
            title = event.summary.trim(),
            teacher = extractTeacher(event.description),
            location = event.location.trim(),
            weekday = start.dayOfWeek.value, // Mon=1 … Sun=7
            startSlot = startSlot,
            endSlot = endSlot,
            date = start.toLocalDate(),
        )
    }

    private fun parseDateTime(raw: String): LocalDateTime? {
        val value = raw.trim()
        if (value.isEmpty()) return null
        return try {
            when {
                value.endsWith("Z", ignoreCase = true) -> {
                    val cleaned = value.dropLast(1)
                    val local = LocalDateTime.parse(cleaned, basicDateTime)
                    Instant.from(local.atZone(ZoneId.of("UTC"))).atZone(zone).toLocalDateTime()
                }
                'T' in value -> LocalDateTime.parse(value.take(15), basicDateTime)
                value.length >= 8 -> LocalDate.parse(value.take(8), basicDate).atStartOfDay()
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun slotForStart(time: LocalTime): Int? {
        val minutes = time.hour * 60 + time.minute
        val best = CourseGridDefaults.timeSlots.mapIndexed { index, label ->
            val parts = label.split(':')
            val slotMin = parts[0].toInt() * 60 + parts[1].toInt()
            index + 1 to abs(slotMin - minutes)
        }.minByOrNull { it.second } ?: return null
        return if (best.second <= 30) best.first else null
    }

    private fun slotForEnd(end: LocalTime, startSlot: Int): Int {
        val endMin = end.hour * 60 + end.minute
        var last = startSlot
        CourseGridDefaults.timeSlots.forEachIndexed { index, label ->
            val parts = label.split(':')
            val slotMin = parts[0].toInt() * 60 + parts[1].toInt()
            // Count a period if it started before the class ended (with small grace).
            if (slotMin < endMin - 5) {
                last = index + 1
            }
        }
        return last.coerceAtLeast(startSlot)
            .coerceAtMost(CourseGridDefaults.slotCount)
    }

    private fun extractTeacher(description: String): String {
        if (description.isBlank()) return ""
        val patterns = listOf(
            Regex("""教师[:：]\s*([^-\[\\;\n\r]+)"""),
            Regex("""老师[:：]\s*([^-\[\\;\n\r]+)"""),
            Regex("""Lecturer[:：]\s*([^\n\r;]+)""", RegexOption.IGNORE_CASE),
        )
        for (p in patterns) {
            val m = p.find(description) ?: continue
            val name = m.groupValues[1].trim()
                .replace(Regex("""[\\;]+$"""), "")
                .trim()
            if (name.isNotEmpty()) return name
        }
        return ""
    }

    private const val MAX_REDIRECTS = 8
}

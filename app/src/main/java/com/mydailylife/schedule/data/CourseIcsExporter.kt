package com.mydailylife.schedule.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.UUID

/**
 * Builds a university-style ICS (one VEVENT per class meeting) from [CourseStore].
 */
object CourseIcsExporter {
    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")
    private val basicDateTime = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    private val stampFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")

    fun export(store: CourseStore): String {
        if (store.courses.isEmpty()) error("当前没有课程可导出")
        val termStart = store.termStartDate
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: CourseGridDefaults.defaultTermStart()
        val maxWeek = store.maxTeachingWeek.coerceIn(1, 30)
        val stamp = java.time.Instant.now().atZone(ZoneId.of("UTC")).format(stampFormatter)
        val events = StringBuilder()
        store.courses.forEach { course ->
            val weeks = if (course.teachingWeeks.isEmpty()) {
                (1..maxWeek).toList()
            } else {
                course.teachingWeeks.filter { it in 1..maxWeek }.distinct().sorted()
            }
            weeks.forEach { week ->
                val weekMonday = CourseGridDefaults.weekStartForTeachingWeek(termStart, week)
                val date = weekMonday.with(
                    TemporalAdjusters.nextOrSame(DayOfWeek.of(course.weekday.coerceIn(1, 7))),
                )
                val start = date.atTime(
                    CourseScheduleBridge.slotStartTime(course.startSlot, store.periodSchedule),
                )
                val end = date.atTime(
                    CourseScheduleBridge.slotEndTime(course.endSlot, store.periodSchedule),
                )
                val uid = "${course.id}-w$week-${UUID.nameUUIDFromBytes("$date".toByteArray())}@mydailylife"
                val description = buildString {
                    if (course.teacher.isNotBlank()) append("教师：${course.teacher}")
                    if (course.weeksSummary().isNotBlank()) {
                        if (isNotEmpty()) append("\\n")
                        append("周次：${course.weeksSummary()}")
                    }
                }
                events.append("BEGIN:VEVENT\r\n")
                events.append("UID:").append(escapeText(uid)).append("\r\n")
                events.append("DTSTAMP:").append(stamp).append("\r\n")
                events.append("DTSTART:").append(start.format(basicDateTime)).append("\r\n")
                events.append("DTEND:").append(end.format(basicDateTime)).append("\r\n")
                events.append("SUMMARY:").append(escapeText(course.title)).append("\r\n")
                if (course.location.isNotBlank()) {
                    events.append("LOCATION:").append(escapeText(course.location)).append("\r\n")
                }
                if (description.isNotBlank()) {
                    events.append("DESCRIPTION:").append(escapeText(description)).append("\r\n")
                }
                events.append("END:VEVENT\r\n")
            }
        }
        if (events.isEmpty()) error("未能生成课程事件")
        return buildString {
            append("BEGIN:VCALENDAR\r\n")
            append("VERSION:2.0\r\n")
            append("PRODID:-//MyDailyLife//Courses//CN\r\n")
            append("CALSCALE:GREGORIAN\r\n")
            append("METHOD:PUBLISH\r\n")
            append("X-WR-TIMEZONE:").append(zone.id).append("\r\n")
            append(events)
            append("END:VCALENDAR\r\n")
        }
    }

    private fun escapeText(value: String): String =
        value
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\r\n", "\\n")
            .replace("\n", "\\n")
            .replace("\r", "\\n")
}

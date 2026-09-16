package com.mydailylife.schedule.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object ScheduleTimeFormat {
    private val zone: ZoneId = ZoneId.systemDefault()
    private val dateTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.CHINA)
    private val displayFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.CHINA)

    fun formatDisplay(millis: Long): String {
        if (millis <= 0L) return ""
        val date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        val today = LocalDate.now(zone)
        val timePart = Instant.ofEpochMilli(millis).atZone(zone)
            .toLocalTime()
            .format(DateTimeFormatter.ofPattern("HH:mm"))
        return when (date) {
            today -> "今天 $timePart"
            today.plusDays(1) -> "明天 $timePart"
            today.minusDays(1) -> "昨天 $timePart"
            else -> Instant.ofEpochMilli(millis).atZone(zone).format(displayFormatter)
        }
    }

    fun formatEditable(millis: Long): String {
        if (millis <= 0L) return ""
        return Instant.ofEpochMilli(millis).atZone(zone).format(dateTimeFormatter)
    }

    fun parseEditable(text: String, fallback: Long = System.currentTimeMillis()): Long {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return fallback
        return runCatching {
            LocalDateTime.parse(trimmed, dateTimeFormatter).atZone(zone).toInstant().toEpochMilli()
        }.getOrElse {
            parseRelative(trimmed, fallback)
        }
    }

    /** Blank → 0 (unset). Invalid text → null. */
    fun parseOptional(text: String): Long? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0L
        return runCatching {
            LocalDateTime.parse(trimmed, dateTimeFormatter).atZone(zone).toInstant().toEpochMilli()
        }.getOrElse {
            val relative = parseRelative(trimmed, Long.MIN_VALUE)
            if (relative == Long.MIN_VALUE) null else relative
        }
    }

    private fun parseRelative(text: String, fallback: Long): Long {
        val now = LocalDateTime.now(zone)
        val match = Regex("""^(今天|明天|昨天)\s*(\d{1,2}):(\d{2})$""").matchEntire(text)
            ?: return fallback
        val dayOffset = when (match.groupValues[1]) {
            "今天" -> 0L
            "明天" -> 1L
            "昨天" -> -1L
            else -> 0L
        }
        val hour = match.groupValues[2].toInt()
        val minute = match.groupValues[3].toInt()
        return now.toLocalDate()
            .plusDays(dayOffset)
            .atTime(hour, minute)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
    }

    fun startOfDayMillis(date: LocalDate = LocalDate.now(zone)): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun endOfDayMillis(date: LocalDate = LocalDate.now(zone)): Long =
        date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

    fun daysUntil(endMillis: Long, nowMillis: Long = System.currentTimeMillis()): Long {
        if (endMillis <= 0L) return Long.MAX_VALUE
        val end = Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDate()
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        return ChronoUnit.DAYS.between(now, end)
    }
}

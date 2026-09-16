package com.mydailylife.schedule.reminder

import com.mydailylife.schedule.data.ScheduleItem
import com.mydailylife.schedule.data.ScheduleQuery
import com.mydailylife.schedule.data.ScheduleTimeMode
import java.time.Instant
import java.time.ZoneId

data class ReminderSlot(
    val kind: ReminderKind,
    val dueMillis: Long,
)

/**
 * Next start/end due times for Once, Daily, and Weekly items.
 * Unlimited has no clock and is never scheduled.
 */
object ReminderTimes {
    const val IMMEDIATE_DELAY_MS = 1_500L
    private const val MAX_LOOKAHEAD_DAYS = 400

    fun slots(
        item: ScheduleItem,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
        alreadyFired: (ReminderKind, Long) -> Boolean = { _, _ -> false },
    ): List<ReminderSlot> {
        if (item.completed || !item.reminderEnabled) return emptyList()
        if (item.timeModeEnum == ScheduleTimeMode.Unlimited) return emptyList()
        return ReminderKind.entries.mapNotNull { kind ->
            val due = nextDueMillis(item, kind, nowMillis, zone, alreadyFired) ?: return@mapNotNull null
            ReminderSlot(kind, due)
        }
    }

    fun nextDueMillis(
        item: ScheduleItem,
        kind: ReminderKind,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
        alreadyFired: (ReminderKind, Long) -> Boolean = { _, _ -> false },
    ): Long? {
        if (item.completed || !item.reminderEnabled) return null
        val kindEnabled = when (kind) {
            ReminderKind.Start -> item.remindAtStart
            ReminderKind.End -> item.remindAtEnd
        }
        if (!kindEnabled) return null
        val stored = storedMillis(item, kind)
        if (stored <= 0L) return null

        return when (item.timeModeEnum) {
            ScheduleTimeMode.Unlimited -> null
            ScheduleTimeMode.Once -> {
                if (stored <= nowMillis) return null
                if (alreadyFired(kind, stored)) return null
                stored
            }
            ScheduleTimeMode.Daily, ScheduleTimeMode.Weekly -> {
                val clock = Instant.ofEpochMilli(stored).atZone(zone)
                    .toLocalTime()
                    .withSecond(0)
                    .withNano(0)
                var date = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
                repeat(MAX_LOOKAHEAD_DAYS) {
                    if (ScheduleQuery.occursOn(item, date, zone)) {
                        val due = date.atTime(clock).atZone(zone).toInstant().toEpochMilli()
                        if (due > nowMillis && !alreadyFired(kind, due)) return due
                    }
                    date = date.plusDays(1)
                }
                null
            }
        }
    }

    fun computeTriggerMillis(
        dueMillis: Long,
        leadMinutes: Int,
        nowMillis: Long,
    ): Long? {
        val remaining = dueMillis - nowMillis
        if (remaining <= 0L) return null
        val leadMs = leadMinutes.coerceAtLeast(0) * 60_000L
        return if (remaining <= leadMs) {
            nowMillis + IMMEDIATE_DELAY_MS
        } else {
            dueMillis - leadMs
        }
    }

    fun formatRemaining(remainingMs: Long): String {
        val minutes = ((remainingMs + 59_999L) / 60_000L).coerceAtLeast(1L)
        return when {
            minutes < 60 -> "${minutes}分钟"
            minutes % 60L == 0L -> "${minutes / 60}小时"
            else -> "${minutes / 60}小时${minutes % 60}分钟"
        }
    }

    private fun storedMillis(item: ScheduleItem, kind: ReminderKind): Long =
        when (kind) {
            ReminderKind.Start -> item.startTimeMillis
            ReminderKind.End -> item.endTimeMillis
        }
}

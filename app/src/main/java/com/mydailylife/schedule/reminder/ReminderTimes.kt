package com.mydailylife.schedule.reminder

import com.mydailylife.schedule.data.ScheduleItem
import com.mydailylife.schedule.data.ScheduleQuery
import com.mydailylife.schedule.data.ScheduleTimeMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ReminderSlot(
    val kind: ReminderKind,
    val dueMillis: Long,
)

/** One upcoming notification for the manage list (sorted by [triggerMillis]). */
data class UpcomingReminder(
    val scheduleId: String,
    val title: String,
    val kind: ReminderKind,
    val timeMode: ScheduleTimeMode,
    val dueMillis: Long,
    val triggerMillis: Long,
    val leadMinutes: Int,
    /** Overrides [timeMode].label in the manage list (e.g. 「课表」). */
    val listTag: String? = null,
) {
    val kindLabel: String
        get() = when (kind) {
            ReminderKind.Start -> "开始"
            ReminderKind.End -> "截止"
        }

    val tagLabel: String get() = listTag ?: timeMode.label
}

/**
 * Next start/end due times for Once, Daily, and Weekly items.
 * Unlimited has no clock and is never scheduled.
 */
object ReminderTimes {
    const val IMMEDIATE_DELAY_MS = 1_500L
    private const val MAX_LOOKAHEAD_DAYS = 400
    /** Default manage-list window: today … today+6 (7 calendar days). */
    const val MANAGE_DEFAULT_RANGE_DAYS = 7
    /** Legacy wide horizon constant (unused by manage UI). */
    const val MANAGE_LOOKAHEAD_DAYS = 60

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

    /**
     * All upcoming reminder fires with trigger date in [[rangeStart], [rangeEnd]] (inclusive).
     */
    fun listUpcoming(
        items: List<ScheduleItem>,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
        rangeStart: LocalDate = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate(),
        rangeEnd: LocalDate = rangeStart.plusDays((MANAGE_DEFAULT_RANGE_DAYS - 1).toLong()),
        alreadyFired: (String, ReminderKind, Long) -> Boolean = { _, _, _ -> false },
    ): List<UpcomingReminder> {
        val start = minOf(rangeStart, rangeEnd)
        val end = maxOf(rangeStart, rangeEnd)
        // Max lead is 1 day — include dues slightly past [end] whose trigger may still fall in range.
        val dueHorizon = end.plusDays(1)
        return items
            .asSequence()
            .filter { !it.completed && it.reminderEnabled }
            .filter { it.timeModeEnum != ScheduleTimeMode.Unlimited }
            .flatMap { item ->
                ReminderKind.entries.asSequence().flatMap { kind ->
                    duesInWindow(item, kind, nowMillis, dueHorizon, zone)
                        .filterNot { due -> alreadyFired(item.id, kind, due) }
                        .mapNotNull { due ->
                            val trigger = computeTriggerMillis(
                                dueMillis = due,
                                leadMinutes = item.reminderBeforeMinutes,
                                nowMillis = nowMillis,
                            ) ?: return@mapNotNull null
                            val triggerDate = Instant.ofEpochMilli(trigger).atZone(zone).toLocalDate()
                            if (triggerDate.isBefore(start) || triggerDate.isAfter(end)) {
                                return@mapNotNull null
                            }
                            UpcomingReminder(
                                scheduleId = item.id,
                                title = item.title,
                                kind = kind,
                                timeMode = item.timeModeEnum,
                                dueMillis = due,
                                triggerMillis = trigger,
                                leadMinutes = item.reminderBeforeMinutes,
                            )
                        }
                }
            }
            .sortedWith(compareBy({ it.triggerMillis }, { it.dueMillis }, { it.title }))
            .toList()
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

    private fun duesInWindow(
        item: ScheduleItem,
        kind: ReminderKind,
        nowMillis: Long,
        horizon: LocalDate,
        zone: ZoneId,
    ): Sequence<Long> {
        val kindEnabled = when (kind) {
            ReminderKind.Start -> item.remindAtStart
            ReminderKind.End -> item.remindAtEnd
        }
        if (!kindEnabled) return emptySequence()
        val stored = storedMillis(item, kind)
        if (stored <= 0L) return emptySequence()

        return when (item.timeModeEnum) {
            ScheduleTimeMode.Unlimited -> emptySequence()
            ScheduleTimeMode.Once -> {
                if (stored <= nowMillis) emptySequence()
                else {
                    val dueDate = Instant.ofEpochMilli(stored).atZone(zone).toLocalDate()
                    if (dueDate.isAfter(horizon)) emptySequence() else sequenceOf(stored)
                }
            }
            ScheduleTimeMode.Daily, ScheduleTimeMode.Weekly -> {
                val clock = Instant.ofEpochMilli(stored).atZone(zone)
                    .toLocalTime()
                    .withSecond(0)
                    .withNano(0)
                sequence {
                    var date = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
                    while (!date.isAfter(horizon)) {
                        if (ScheduleQuery.occursOn(item, date, zone)) {
                            val due = date.atTime(clock).atZone(zone).toInstant().toEpochMilli()
                            if (due > nowMillis) yield(due)
                        }
                        date = date.plusDays(1)
                    }
                }
            }
        }
    }

    private fun storedMillis(item: ScheduleItem, kind: ReminderKind): Long =
        when (kind) {
            ReminderKind.Start -> item.startTimeMillis
            ReminderKind.End -> item.endTimeMillis
        }
}

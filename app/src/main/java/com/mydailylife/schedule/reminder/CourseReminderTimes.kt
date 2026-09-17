package com.mydailylife.schedule.reminder

import com.mydailylife.schedule.data.CourseScheduleBridge
import com.mydailylife.schedule.data.CourseStore
import com.mydailylife.schedule.data.ScheduleTimeMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class CourseReminderSlot(
    /** Stable id: `course:{courseId}` — same as bridge day-list items. */
    val scheduleId: String,
    val title: String,
    val dueMillis: Long,
    val leadMinutes: Int,
)

/**
 * Class-start dues for 上课提醒 (AlarmManager + 通知管理列表).
 */
object CourseReminderTimes {
    /** How far ahead to search for the next occurrence of each course (scheduler). */
    const val LOOKAHEAD_DAYS = 60

    fun nextSlots(
        store: CourseStore,
        leadMinutes: Int,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
        alreadyFired: (scheduleId: String, dueMillis: Long) -> Boolean = { _, _ -> false },
        lookaheadDays: Int = LOOKAHEAD_DAYS,
    ): List<CourseReminderSlot> {
        if (store.courses.isEmpty()) return emptyList()
        val termStart = store.termStartDate
            ?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
        val startDate = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val lead = leadMinutes.coerceAtLeast(0)
        return store.courses.mapNotNull { course ->
            var date = startDate
            repeat(lookaheadDays.coerceAtLeast(1)) {
                if (CourseScheduleBridge.occursOnDate(course, date, termStart)) {
                    val start = date.atTime(
                        CourseScheduleBridge.slotStartTime(course.startSlot, store.periodSchedule),
                    )
                    val due = start.atZone(zone).toInstant().toEpochMilli()
                    val scheduleId = "${CourseScheduleBridge.ID_PREFIX}${course.id}"
                    if (due > nowMillis && !alreadyFired(scheduleId, due)) {
                        return@mapNotNull CourseReminderSlot(
                            scheduleId = scheduleId,
                            title = course.title,
                            dueMillis = due,
                            leadMinutes = lead,
                        )
                    }
                }
                date = date.plusDays(1)
            }
            null
        }
    }

    /**
     * All upcoming class-start fires with trigger date in [[rangeStart], [rangeEnd]] (inclusive).
     */
    fun listUpcoming(
        store: CourseStore,
        leadMinutes: Int,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
        rangeStart: LocalDate = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate(),
        rangeEnd: LocalDate = rangeStart.plusDays(
            (ReminderTimes.MANAGE_DEFAULT_RANGE_DAYS - 1).toLong(),
        ),
    ): List<UpcomingReminder> {
        if (store.courses.isEmpty()) return emptyList()
        val termStart = store.termStartDate
            ?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val start = minOf(rangeStart, rangeEnd)
        val end = maxOf(rangeStart, rangeEnd)
        val scanFrom = minOf(today, start)
        val scanTo = end.plusDays(1)
        val lead = leadMinutes.coerceAtLeast(0)
        return store.courses.asSequence().flatMap { course ->
            sequence {
                var date = scanFrom
                while (!date.isAfter(scanTo)) {
                    if (CourseScheduleBridge.occursOnDate(course, date, termStart)) {
                        val classStart = date.atTime(
                            CourseScheduleBridge.slotStartTime(course.startSlot, store.periodSchedule),
                        )
                        val due = classStart.atZone(zone).toInstant().toEpochMilli()
                        if (due > nowMillis) {
                            val trigger = ReminderTimes.computeTriggerMillis(
                                dueMillis = due,
                                leadMinutes = lead,
                                nowMillis = nowMillis,
                            )
                            if (trigger != null) {
                                val triggerDate = Instant.ofEpochMilli(trigger).atZone(zone).toLocalDate()
                                if (!triggerDate.isBefore(start) && !triggerDate.isAfter(end)) {
                                    yield(
                                        UpcomingReminder(
                                            scheduleId = "${CourseScheduleBridge.ID_PREFIX}${course.id}",
                                            title = course.title,
                                            kind = ReminderKind.Start,
                                            timeMode = ScheduleTimeMode.Weekly,
                                            dueMillis = due,
                                            triggerMillis = trigger,
                                            leadMinutes = lead,
                                            listTag = "课表",
                                        ),
                                    )
                                }
                            }
                        }
                    }
                    date = date.plusDays(1)
                }
            }
        }.sortedWith(compareBy({ it.triggerMillis }, { it.dueMillis }, { it.title })).toList()
    }
}

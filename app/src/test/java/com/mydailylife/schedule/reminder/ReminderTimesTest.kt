package com.mydailylife.schedule.reminder

import com.mydailylife.schedule.data.ScheduleItem
import com.mydailylife.schedule.data.ScheduleTimeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class ReminderTimesTest {
    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")

    @Test
    fun onceSchedulesStartAndEndSeparately() {
        val start = at(LocalDate.of(2026, 9, 16), LocalTime.of(10, 0))
        val end = at(LocalDate.of(2026, 9, 16), LocalTime.of(12, 0))
        val now = at(LocalDate.of(2026, 9, 16), LocalTime.of(8, 0))
        val item = item(
            timeMode = ScheduleTimeMode.Once,
            start = start,
            end = end,
        )
        val slots = ReminderTimes.slots(item, now, zone)
        assertEquals(listOf(ReminderKind.Start, ReminderKind.End), slots.map { it.kind })
        assertEquals(start, slots[0].dueMillis)
        assertEquals(end, slots[1].dueMillis)
    }

    @Test
    fun onceSkipsDisabledKind() {
        val start = at(LocalDate.of(2026, 9, 16), LocalTime.of(10, 0))
        val end = at(LocalDate.of(2026, 9, 16), LocalTime.of(12, 0))
        val now = at(LocalDate.of(2026, 9, 16), LocalTime.of(8, 0))
        val item = item(
            timeMode = ScheduleTimeMode.Once,
            start = start,
            end = end,
            remindAtStart = false,
        )
        val slots = ReminderTimes.slots(item, now, zone)
        assertEquals(listOf(ReminderKind.End), slots.map { it.kind })
        assertEquals(end, slots.single().dueMillis)
    }

    @Test
    fun dailyUsesTomorrowWhenTodaysClockPassed() {
        val clock = LocalTime.of(10, 0)
        val stored = at(LocalDate.of(2000, 1, 1), clock)
        val now = at(LocalDate.of(2026, 9, 16), LocalTime.of(11, 0))
        val item = item(
            timeMode = ScheduleTimeMode.Daily,
            start = stored,
            end = 0L,
            remindAtEnd = false,
        )
        val due = ReminderTimes.nextDueMillis(item, ReminderKind.Start, now, zone)
        assertEquals(at(LocalDate.of(2026, 9, 17), clock), due)
    }

    @Test
    fun dailySkipsExcludedDate() {
        val clock = LocalTime.of(10, 0)
        val stored = at(LocalDate.of(2000, 1, 1), clock)
        val now = at(LocalDate.of(2026, 9, 16), LocalTime.of(8, 0))
        val item = item(
            timeMode = ScheduleTimeMode.Daily,
            start = stored,
            end = 0L,
            remindAtEnd = false,
            excluded = listOf("2026-09-16"),
        )
        val due = ReminderTimes.nextDueMillis(item, ReminderKind.Start, now, zone)
        assertEquals(at(LocalDate.of(2026, 9, 17), clock), due)
    }

    @Test
    fun weeklyJumpsToNextSelectedWeekday() {
        val clock = LocalTime.of(9, 30)
        val stored = at(LocalDate.of(2000, 1, 1), clock)
        // 2026-09-16 is Wednesday; next Monday is 2026-09-21.
        val now = at(LocalDate.of(2026, 9, 16), LocalTime.of(8, 0))
        val item = item(
            timeMode = ScheduleTimeMode.Weekly,
            start = stored,
            end = 0L,
            remindAtEnd = false,
            weekdays = listOf(1),
        )
        val due = ReminderTimes.nextDueMillis(item, ReminderKind.Start, now, zone)
        assertEquals(at(LocalDate.of(2026, 9, 21), clock), due)
    }

    @Test
    fun overdueOnceIsNotScheduled() {
        val start = at(LocalDate.of(2026, 9, 16), LocalTime.of(10, 0))
        val now = at(LocalDate.of(2026, 9, 16), LocalTime.of(11, 0))
        val item = item(
            timeMode = ScheduleTimeMode.Once,
            start = start,
            end = 0L,
            remindAtEnd = false,
        )
        assertNull(ReminderTimes.nextDueMillis(item, ReminderKind.Start, now, zone))
    }

    @Test
    fun triggerUsesLeadWindowThenAsap() {
        val due = 1_000_000L
        assertEquals(
            due - 15 * 60_000L,
            ReminderTimes.computeTriggerMillis(due, 15, due - 60 * 60_000L),
        )
        val now = due - 10 * 60_000L
        assertEquals(
            now + ReminderTimes.IMMEDIATE_DELAY_MS,
            ReminderTimes.computeTriggerMillis(due, 15, now),
        )
        assertNull(ReminderTimes.computeTriggerMillis(due, 15, due))
    }

    @Test
    fun alreadyFiredOccurrenceIsSkipped() {
        val start = at(LocalDate.of(2026, 9, 16), LocalTime.of(10, 0))
        val now = at(LocalDate.of(2026, 9, 16), LocalTime.of(8, 0))
        val item = item(
            timeMode = ScheduleTimeMode.Once,
            start = start,
            end = 0L,
            remindAtEnd = false,
        )
        val due = ReminderTimes.nextDueMillis(item, ReminderKind.Start, now, zone) { kind, millis ->
            kind == ReminderKind.Start && millis == start
        }
        assertNull(due)
        assertTrue(
            ReminderTimes.slots(item, now, zone) { kind, millis ->
                kind == ReminderKind.Start && millis == start
            }.isEmpty(),
        )
    }

    @Test
    fun listUpcomingGroupsDailyByDateOrder() {
        val clock = LocalTime.of(10, 0)
        val stored = at(LocalDate.of(2000, 1, 1), clock)
        val now = at(LocalDate.of(2026, 9, 16), LocalTime.of(8, 0))
        val item = item(
            timeMode = ScheduleTimeMode.Daily,
            start = stored,
            end = 0L,
            remindAtEnd = false,
        )
        val list = ReminderTimes.listUpcoming(
            items = listOf(item),
            nowMillis = now,
            zone = zone,
            lookaheadDays = 2,
        )
        assertEquals(3, list.size)
        assertTrue(list.zipWithNext().all { (a, b) -> a.triggerMillis <= b.triggerMillis })
        assertEquals(
            listOf(
                LocalDate.of(2026, 9, 16),
                LocalDate.of(2026, 9, 17),
                LocalDate.of(2026, 9, 18),
            ),
            list.map {
                java.time.Instant.ofEpochMilli(it.dueMillis).atZone(zone).toLocalDate()
            },
        )
    }

    private fun at(date: LocalDate, time: LocalTime): Long =
        date.atTime(time).atZone(zone).toInstant().toEpochMilli()

    private fun item(
        timeMode: ScheduleTimeMode,
        start: Long,
        end: Long,
        remindAtStart: Boolean = true,
        remindAtEnd: Boolean = true,
        weekdays: List<Int> = emptyList(),
        excluded: List<String> = emptyList(),
    ) = ScheduleItem(
        id = "test",
        title = "t",
        startTimeMillis = start,
        endTimeMillis = end,
        reminderEnabled = true,
        remindAtStart = remindAtStart,
        remindAtEnd = remindAtEnd,
        timeMode = timeMode.storageKey,
        weekdays = weekdays,
        excludedDates = excluded,
    )
}

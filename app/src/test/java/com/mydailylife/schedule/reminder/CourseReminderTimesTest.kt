package com.mydailylife.schedule.reminder

import com.mydailylife.schedule.data.CourseItem
import com.mydailylife.schedule.data.CoursePeriodPresets
import com.mydailylife.schedule.data.CoursePeriodSchedule
import com.mydailylife.schedule.data.CourseScheduleBridge
import com.mydailylife.schedule.data.CourseStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class CourseReminderTimesTest {
    @Test
    fun nextSlots_returnsUpcomingClassStart() {
        val zone = ZoneId.of("Asia/Shanghai")
        val today = LocalDate.of(2026, 9, 7) // Monday
        val termStart = today
        val store = CourseStore(
            courses = listOf(
                CourseItem(
                    id = "math",
                    title = "高等数学",
                    weekday = 1,
                    startSlot = 1,
                    endSlot = 2,
                    teachingWeeks = listOf(1),
                ),
            ),
            termStartDate = termStart.toString(),
            periodSchedule = CoursePeriodSchedule(
                presetId = CoursePeriodPresets.CQU,
                periods = CoursePeriodPresets.cqu,
            ),
        )
        val morning = today.atTime(LocalTime.of(7, 0)).atZone(zone).toInstant().toEpochMilli()
        val slots = CourseReminderTimes.nextSlots(
            store = store,
            leadMinutes = 15,
            nowMillis = morning,
            zone = zone,
        )
        assertEquals(1, slots.size)
        assertEquals("${CourseScheduleBridge.ID_PREFIX}math", slots[0].scheduleId)
        assertEquals("高等数学", slots[0].title)
        val expectedDue = today.atTime(CourseScheduleBridge.slotStartTime(1, store.periodSchedule))
            .atZone(zone).toInstant().toEpochMilli()
        assertEquals(expectedDue, slots[0].dueMillis)
        assertTrue(slots[0].dueMillis > morning)
    }
}

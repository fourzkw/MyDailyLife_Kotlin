package com.mydailylife.schedule.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CourseScheduleBridgeTest {
    @Test
    fun mapsSlotsAndFiltersByWeekday() {
        val course = CourseItem(
            id = "c1",
            title = "高等数学",
            teacher = "张三",
            location = "A101",
            weekday = 1,
            startSlot = 1,
            endSlot = 2,
        )
        val store = CourseStore(
            courses = listOf(course),
            termStartDate = "2026-09-07",
        )
        val monday = LocalDate.of(2026, 9, 7)
        val items = CourseScheduleBridge.toScheduleItems(store, monday)
        assertEquals(1, items.size)
        assertTrue(CourseScheduleBridge.isCourseItem(items[0]))
        assertEquals("高等数学", items[0].title)
        assertEquals("张三 · A101", items[0].description)
        assertEquals(listOf("课表"), items[0].tags)

        val tuesday = LocalDate.of(2026, 9, 8)
        assertTrue(CourseScheduleBridge.toScheduleItems(store, tuesday).isEmpty())
    }
}

class ScheduleSortModeTest {
    @Test
    fun sortsByTimeAndUrgency() {
        val earlyLow = ScheduleItem(
            id = "1",
            title = "early",
            priority = Priority.Low.storageKey,
            startTimeMillis = 1000L,
        )
        val lateUrgent = ScheduleItem(
            id = "2",
            title = "late",
            priority = Priority.Urgent.storageKey,
            startTimeMillis = 2000L,
        )
        val byTime = ScheduleQuery.sorted(listOf(lateUrgent, earlyLow), ScheduleSortMode.Time)
        assertEquals(listOf("1", "2"), byTime.map { it.id })

        val byUrgency = ScheduleQuery.sorted(listOf(earlyLow, lateUrgent), ScheduleSortMode.Urgency)
        assertEquals(listOf("2", "1"), byUrgency.map { it.id })
    }
}

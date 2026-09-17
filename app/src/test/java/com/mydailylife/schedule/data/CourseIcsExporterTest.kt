package com.mydailylife.schedule.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CourseIcsExporterTest {
    @Test
    fun export_containsEventsMatchingStore() {
        val termStart = LocalDate.of(2026, 9, 7)
        val store = CourseStore(
            courses = listOf(
                CourseItem(
                    id = "c1",
                    title = "高等数学",
                    teacher = "张三",
                    location = "教一101",
                    weekday = 1,
                    startSlot = 1,
                    endSlot = 2,
                    teachingWeeks = listOf(1, 2),
                ),
            ),
            termStartDate = termStart.toString(),
            maxTeachingWeek = 18,
            periodSchedule = CoursePeriodSchedule(
                presetId = CoursePeriodPresets.CQU,
                periods = CoursePeriodPresets.cqu,
            ),
        )
        val ics = CourseIcsExporter.export(store)
        assertTrue(ics.contains("BEGIN:VCALENDAR"))
        assertTrue(ics.contains("SUMMARY:高等数学"))
        assertTrue(ics.contains("LOCATION:教一101"))
        assertTrue(ics.contains("教师：张三"))
        assertEquals(2, ics.split("BEGIN:VEVENT").size - 1)

        val roundTrip = CourseIcsImporter.parse(ics)
        assertEquals(1, roundTrip.courses.size)
        assertEquals("高等数学", roundTrip.courses[0].title)
        assertEquals("张三", roundTrip.courses[0].teacher)
        assertEquals(1, roundTrip.courses[0].weekday)
        assertEquals(1, roundTrip.courses[0].startSlot)
        assertEquals(2, roundTrip.courses[0].endSlot)
    }
}

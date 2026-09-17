package com.mydailylife.schedule.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CourseIcsImporterTest {
    @Test
    fun parsesUtcEventsIntoSlotsAndWeeks() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            BEGIN:VEVENT
            SUMMARY:高等数学
            DESCRIPTION:教师: 张三
            LOCATION:A101
            DTSTART:20260907T003000Z
            DTEND:20260907T021000Z
            END:VEVENT
            BEGIN:VEVENT
            SUMMARY:高等数学
            DESCRIPTION:教师: 张三
            LOCATION:A101
            DTSTART:20260914T003000Z
            DTEND:20260914T021000Z
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val result = CourseIcsImporter.parse(ics)
        assertEquals(LocalDate.of(2026, 9, 7), result.suggestedTermStart)
        assertEquals(1, result.courses.size)
        val course = result.courses.first()
        assertEquals("高等数学", course.title)
        assertEquals("张三", course.teacher)
        assertEquals("A101", course.location)
        assertEquals(1, course.weekday) // Monday
        assertEquals(1, course.startSlot) // 08:30
        assertEquals(2, course.endSlot)
        assertEquals(listOf(1, 2), course.teachingWeeks)
    }

    @Test
    fun looksLikeHelpers() {
        assertTrue(CourseIcsImporter.looksLikeUrl("https://example.com/a.ics"))
        assertTrue(CourseIcsImporter.looksLikeIcs("BEGIN:VCALENDAR\nEND:VCALENDAR"))
    }
}

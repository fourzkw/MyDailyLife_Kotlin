package com.mydailylife.schedule.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CourseGridDefaultsTest {
    @Test
    fun mondayOfMonthWeek_septemberSecond() {
        // 2025-09-01 is Monday → week 1 Mon = 9/1 → week 2 Mon = 9/8
        assertEquals(
            LocalDate.of(2025, 9, 8),
            CourseGridDefaults.mondayOfMonthWeek(2025, 9, 2),
        )
        // 2024-09-01 is Sunday → week 1 Mon = 8/26 → week 2 Mon = 9/2
        assertEquals(
            LocalDate.of(2024, 9, 2),
            CourseGridDefaults.mondayOfMonthWeek(2024, 9, 2),
        )
        // 2026-09-01 is Tuesday → week 1 Mon = 8/31 → week 2 Mon = 9/7
        assertEquals(
            LocalDate.of(2026, 9, 7),
            CourseGridDefaults.mondayOfMonthWeek(2026, 9, 2),
        )
    }

    @Test
    fun defaultTermStart_autumnAndSpring() {
        assertEquals(
            CourseGridDefaults.termStartForSemester(AcademicSemester.Autumn, 2025),
            CourseGridDefaults.defaultTermStart(LocalDate.of(2025, 9, 20)),
        )
        assertEquals(
            CourseGridDefaults.termStartForSemester(AcademicSemester.Spring, 2026),
            CourseGridDefaults.defaultTermStart(LocalDate.of(2026, 3, 10)),
        )
        // January still maps to previous autumn
        assertEquals(
            CourseGridDefaults.termStartForSemester(AcademicSemester.Autumn, 2025),
            CourseGridDefaults.defaultTermStart(LocalDate.of(2026, 1, 5)),
        )
    }

    @Test
    fun asTermStartMonday() {
        assertEquals(
            LocalDate.of(2025, 9, 8),
            CourseGridDefaults.asTermStartMonday(LocalDate.of(2025, 9, 10)),
        )
    }
}

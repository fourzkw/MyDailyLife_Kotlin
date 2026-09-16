package com.mydailylife.schedule.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseExcelImporterTest {
    @Test
    fun parsesChineseHeaderCsv() {
        val csv = """
            课程名,教师,地点,星期,开始节次,结束节次
            高等数学,张老师,A101,1,1,2
            大学英语,李老师,B203,二,3,4
        """.trimIndent()
        val courses = CourseExcelImporter.parseCsv(csv)
        assertEquals(2, courses.size)
        assertEquals("高等数学", courses[0].title)
        assertEquals(1, courses[0].weekday)
        assertEquals(1, courses[0].startSlot)
        assertEquals(2, courses[0].endSlot)
        assertEquals(2, courses[1].weekday)
        assertEquals("B203", courses[1].location)
    }

    @Test
    fun rejectsXlsxHintViaNameHelper() {
        assertTrue(CourseExcelImporter.isLikelySpreadsheetName("a.xlsx"))
        assertTrue(CourseExcelImporter.isCsvName("a.csv"))
        assertTrue(!CourseExcelImporter.isCsvName("a.xlsx"))
    }
}

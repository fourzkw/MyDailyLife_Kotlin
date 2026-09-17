package com.mydailylife.schedule.data

import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream

class CourseXlsReaderTest {
    @Test
    fun parseBytes_readsLegacyXls() {
        val bytes = ByteArrayOutputStream().use { out ->
            HSSFWorkbook().use { wb ->
                val sheet = wb.createSheet("courses")
                val header = sheet.createRow(0)
                listOf("课程名", "教师", "地点", "星期", "开始节次", "结束节次").forEachIndexed { i, v ->
                    header.createCell(i).setCellValue(v)
                }
                val row = sheet.createRow(1)
                listOf("线性代数", "李四", "理教203", "2", "3", "4").forEachIndexed { i, v ->
                    row.createCell(i).setCellValue(v)
                }
                // Numeric weekday / slots (common Excel export)
                val row2 = sheet.createRow(2)
                row2.createCell(0).setCellValue("大学英语")
                row2.createCell(1).setCellValue("王五")
                row2.createCell(2).setCellValue("外教楼1")
                row2.createCell(3).setCellValue(5.0)
                row2.createCell(4).setCellValue(1.0)
                row2.createCell(5).setCellValue(2.0)
                wb.write(out)
            }
            out.toByteArray()
        }

        val courses = CourseExcelImporter.parseBytes("sample.xls", bytes)
        assertEquals(2, courses.size)
        assertEquals("线性代数", courses[0].title)
        assertEquals(2, courses[0].weekday)
        assertEquals(3, courses[0].startSlot)
        assertEquals(4, courses[0].endSlot)
        assertEquals("大学英语", courses[1].title)
        assertEquals(5, courses[1].weekday)
        assertEquals(1, courses[1].startSlot)
        assertEquals(2, courses[1].endSlot)
    }
}

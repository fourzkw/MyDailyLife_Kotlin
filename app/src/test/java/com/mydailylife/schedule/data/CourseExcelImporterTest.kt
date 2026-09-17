package com.mydailylife.schedule.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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
    fun parsesXlsxBytes() {
        val bytes = minimalCourseXlsx()
        val courses = CourseExcelImporter.parseBytes("courses.xlsx", bytes)
        assertEquals(1, courses.size)
        assertEquals("高等数学", courses[0].title)
        assertEquals("张老师", courses[0].teacher)
        assertEquals(1, courses[0].weekday)
        assertEquals(1, courses[0].startSlot)
        assertEquals(2, courses[0].endSlot)
    }

    @Test
    fun nameHelpers() {
        assertTrue(CourseExcelImporter.isLikelySpreadsheetName("a.xlsx"))
        assertTrue(CourseExcelImporter.isCsvName("a.csv"))
        assertTrue(CourseExcelImporter.isXlsxName("a.xlsx"))
        assertTrue(!CourseExcelImporter.isCsvName("a.xlsx"))
    }

    /** Tiny OOXML workbook with one data row. */
    private fun minimalCourseXlsx(): ByteArray {
        val shared = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="8" uniqueCount="8">
              <si><t>课程名</t></si>
              <si><t>教师</t></si>
              <si><t>地点</t></si>
              <si><t>星期</t></si>
              <si><t>开始节次</t></si>
              <si><t>结束节次</t></si>
              <si><t>高等数学</t></si>
              <si><t>张老师</t></si>
            </sst>
        """.trimIndent()
        val sheet = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <sheetData>
                <row r="1">
                  <c r="A1" t="s"><v>0</v></c>
                  <c r="B1" t="s"><v>1</v></c>
                  <c r="C1" t="s"><v>2</v></c>
                  <c r="D1" t="s"><v>3</v></c>
                  <c r="E1" t="s"><v>4</v></c>
                  <c r="F1" t="s"><v>5</v></c>
                </row>
                <row r="2">
                  <c r="A2" t="s"><v>6</v></c>
                  <c r="B2" t="s"><v>7</v></c>
                  <c r="C2"><v>A101</v></c>
                  <c r="D2"><v>1</v></c>
                  <c r="E2"><v>1</v></c>
                  <c r="F2"><v>2</v></c>
                </row>
              </sheetData>
            </worksheet>
        """.trimIndent()
        val workbook = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <sheets><sheet name="Sheet1" sheetId="1" r:id="rId1"/></sheets>
            </workbook>
        """.trimIndent()
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun put(path: String, body: String) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(body.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            put("[Content_Types].xml", "<Types></Types>")
            put("xl/sharedStrings.xml", shared)
            put("xl/workbook.xml", workbook)
            put("xl/worksheets/sheet1.xml", sheet)
        }
        return out.toByteArray()
    }
}

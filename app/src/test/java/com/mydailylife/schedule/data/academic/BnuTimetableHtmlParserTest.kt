package com.mydailylife.schedule.data.academic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BnuTimetableHtmlParserTest {
    private fun sampleHtml(): String {
        val stream = requireNotNull(
            javaClass.classLoader.getResourceAsStream("bnu_xskcb_sample.html"),
        ) { "missing bnu_xskcb_sample.html" }
        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    @Test
    fun parsesSampleXskcbHtml() {
        val html = sampleHtml()
        assertTrue(BnuTimetableHtmlParser.looksLikeBnuTimetable(html))
        val courses = BnuTimetableHtmlParser.parse(html)
        assertTrue("expected many course blocks, got ${courses.size}", courses.size >= 20)

        val functionalMon = courses.filter {
            it.title == "泛函分析" && it.weekday == 1 && it.startSlot == 1 && it.endSlot == 2
        }
        assertTrue(functionalMon.isNotEmpty())
        assertTrue(functionalMon.any { it.teacher == "王华阳" && it.teachingWeekLabel == "1" })
        assertTrue(functionalMon.any { it.teacher == "熊金钢" && it.teachingWeeks == (2..16).toList() })

        val pde = courses.first {
            it.title == "偏微分方程" && it.weekday == 4 && it.startSlot == 1
        }
        assertEquals("徐桂香", pde.teacher)
        assertEquals("八402", pde.location)
        assertEquals((1..16).toList(), pde.teachingWeeks)

        val sci = courses.first { it.title == "科学计算" }
        assertEquals(3, sci.weekday)
        assertEquals(5, sci.startSlot)
        assertEquals(7, sci.endSlot)

        val multiWeek = courses.first {
            it.title.startsWith("习近平") && it.teachingWeekLabel == "1-3,5,15"
        }
        assertEquals(2, multiWeek.weekday)
        assertEquals(3, multiWeek.startSlot)
        assertEquals(4, multiWeek.endSlot)
        assertEquals(listOf(1, 2, 3, 5, 15), multiWeek.teachingWeeks)
    }

    @Test
    fun parsesWebViewNormalizedStyleSpaces() {
        val html = """
            <table id="mytable">
              <tr class="H"><td></td><td>星期一</td><td>星期二</td><td>星期三</td><td>星期四</td><td>星期五</td><td>星期六</td><td>星期日</td></tr>
              <tr>
                <td>一</td>
                <td><div style="padding-bottom: 5px; clear: both;">泛函分析<br>王华阳<br>1[1-2]<br>九101</div></td>
                <td><div class="div_nokb"></div></td>
                <td><div class="div_nokb"></div></td>
                <td><div class="div_nokb"></div></td>
                <td><div class="div_nokb"></div></td>
                <td><div class="div_nokb"></div></td>
                <td><div class="div_nokb"></div></td>
              </tr>
            </table>
        """.trimIndent()
        assertTrue(BnuTimetableHtmlParser.looksLikeBnuTimetable(html))
        val courses = BnuTimetableHtmlParser.parse(html)
        assertEquals(1, courses.size)
        assertEquals("泛函分析", courses[0].title)
        assertEquals(1, courses[0].weekday)
        assertEquals(1, courses[0].startSlot)
        assertEquals(2, courses[0].endSlot)
    }

    @Test
    fun parseCourseBlockHandlesLeadingZeroWeek() {
        val course = requireNotNull(
            BnuTimetableHtmlParser.parseCourseBlock(
                "测度与概率\n何辉\n01[1-2]\n八109",
                weekday = 2,
            ),
        )
        assertEquals("1", course.teachingWeekLabel)
        assertEquals(listOf(1), course.teachingWeeks)
        assertEquals(1, course.startSlot)
        assertEquals(2, course.endSlot)
    }

    @Test
    fun parseCapturePayloadUsesPageHtml() {
        val html = sampleHtml()
        val payload = buildString {
            append("""{"pageUrl":"https://zyfw.bnu.edu.cn/wsxk.xskcb.jsp","pageHtml":""")
            append(kotlinx.serialization.json.JsonPrimitive(html))
            append(""","network":[],"domCourses":[]}""")
        }
        val result = AcademicTimetableParser.parseCapturePayload(payload)
        assertTrue(result.courses.size >= 20)
        assertTrue(result.sourceHint.contains("bnu"))
    }
}

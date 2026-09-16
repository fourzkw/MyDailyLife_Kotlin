package com.mydailylife.schedule.data.academic

import com.mydailylife.schedule.data.CourseItem
import com.mydailylife.schedule.data.academic.AcademicTimetableParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AcademicTimetableParserTest {
    @Test
    fun parsesCquMyTableDetail() {
        val body = """
            {
              "classTimetableVOList": [
                {
                  "id": "1920726",
                  "weekDay": "1",
                  "period": "0000011",
                  "courseName": "产品设计",
                  "roomName": "EB407",
                  "instructorName": "王冰-33036[主讲];范正妍-31105[辅讲];",
                  "teachingWeekFormat": "10",
                  "periodFormat": "6-7",
                  "weekDayFormat": "一"
                },
                {
                  "id": "1920999",
                  "weekDay": "3",
                  "courseName": "产品设计",
                  "roomName": "EB407",
                  "instructorName": "王冰-33036[主讲];",
                  "teachingWeekFormat": "11",
                  "periodFormat": "6-7",
                  "weekDayFormat": "三"
                }
              ],
              "maxSection": 13
            }
        """.trimIndent()
        val courses = AcademicTimetableParser.parseJsonBlob(body)
        assertEquals(2, courses.size)
        assertEquals("产品设计", courses[0].title)
        assertEquals(1, courses[0].weekday)
        assertEquals(6, courses[0].startSlot)
        assertEquals(7, courses[0].endSlot)
        assertEquals("EB407", courses[0].location)
        assertEquals("王冰、范正妍", courses[0].teacher)
        assertEquals(listOf(10), courses[0].teachingWeeks)
        assertEquals("10", courses[0].teachingWeekLabel)
        assertEquals(3, courses[1].weekday)
        assertEquals(listOf(11), courses[1].teachingWeeks)
    }

    @Test
    fun parseTeachingWeekFormatRanges() {
        assertEquals(listOf(1, 2), AcademicTimetableParser.parseTeachingWeekFormat("1-2"))
        assertEquals(listOf(6, 11), AcademicTimetableParser.parseTeachingWeekFormat("6,11"))
        assertEquals(listOf(10), AcademicTimetableParser.parseTeachingWeekFormat("10"))
    }

    @Test
    fun weeksFromBitmask() {
        assertEquals(listOf(10), CourseItem.weeksFromBitmask("0000000001"))
        assertEquals(listOf(1, 2), CourseItem.weeksFromBitmask("11"))
    }

    @Test
    fun parsesPeriodBitmask() {
        assertEquals(6 to 7, AcademicTimetableParser.parsePeriodBitmask("0000011"))
        assertEquals(1 to 2, AcademicTimetableParser.parsePeriodBitmask("1100000"))
    }

    @Test
    fun cleansInstructorName() {
        assertEquals(
            "王冰、范正妍",
            AcademicTimetableParser.cleanInstructorName("王冰-33036[主讲];范正妍-31105[辅讲];"),
        )
    }

    @Test
    fun parsesCaptureEnvelopePreferringNetworkCquBody() {
        val inner = """{"classTimetableVOList":[{"courseName":"线性代数","weekDay":"5","periodFormat":"1-2","roomName":"A102","instructorName":"赵"}],"maxSection":13}"""
        val payload = """{"pageUrl":"https://my.cqu.edu.cn/","network":[{"url":"https://x/my-table-detail","body":${
            inner.replace("\"", "\\\"")
        }}],"domCourses":[]}"""
        // Use proper JSON embedding
        val proper = buildString {
            append("""{"pageUrl":"https://my.cqu.edu.cn/","network":[{"url":"https://x/my-table-detail","body":""")
            append(kotlinx.serialization.json.JsonPrimitive(inner))
            append("""}],"domCourses":[]}""")
        }
        val result = AcademicTimetableParser.parseCapturePayload(proper)
        assertEquals(1, result.courses.size)
        assertEquals("线性代数", result.courses[0].title)
        assertEquals(5, result.courses[0].weekday)
        assertTrue(result.sourceHint.contains("network") || result.sourceHint.contains("cqu"))
    }

    @Test
    fun parsesNetworkStyleCourseList() {
        val body = """
            {"data":{"courseList":[
              {"courseName":"高等数学","teacherName":"张","classroom":"A101","weekday":1,"startSlot":1,"endSlot":2},
              {"kcmc":"大学英语","jsxm":"李","cdmc":"B203","xqj":2,"skjc":3,"jsjc":4}
            ]}}
        """.trimIndent()
        val courses = AcademicTimetableParser.parseJsonBlob(body)
        assertEquals(2, courses.size)
        assertEquals("高等数学", courses[0].title)
        assertEquals(1, courses[0].weekday)
        assertEquals("大学英语", courses[1].title)
        assertEquals(2, courses[1].weekday)
        assertEquals(3, courses[1].startSlot)
        assertEquals(4, courses[1].endSlot)
    }

    @Test
    fun parsesCaptureEnvelopeWithDom() {
        val payload = """
            {"pageUrl":"https://my.cqu.edu.cn/","network":[],"domCourses":[
              {"课程名":"线性代数","教师":"赵","地点":"A102","星期":"五","开始节次":"1","结束节次":"2"}
            ]}
        """.trimIndent()
        val quoted = "\"" + payload.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        val result = AcademicTimetableParser.parseCapturePayload(quoted)
        assertEquals(1, result.courses.size)
        assertEquals("线性代数", result.courses[0].title)
        assertEquals(5, result.courses[0].weekday)
        assertTrue(result.sourceHint.contains("dom"))
    }
}

package com.mydailylife.schedule.data.academic

import com.mydailylife.schedule.data.CourseItem
import java.util.UUID

/**
 * Parses BNU `wsxk.xskcb*.jsp` HTML timetable (`#mytable`).
 *
 * Cell course blocks look like:
 * ```
 * 泛函分析<br>王华阳 <br>1[1-2]<br>九101
 * ```
 * i.e. title / teacher / weeks[slots] / location.
 */
object BnuTimetableHtmlParser {
    /**
     * Course cells use a padded div. Raw JSP has `padding-bottom:5px;clear:both;`
     * but WebView `outerHTML` often inserts spaces — keep the matcher loose.
     */
    private val courseDivRegex = Regex(
        """<div\b[^>]*padding-bottom\s*:\s*5px[^>]*>(.*?)</div>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val weeksSlotsInHtml = Regex("""\d[\d,，\-~～—]*\s*\[\s*\d+""")

    private val weeksSlotsRegex = Regex(
        """^\s*(.+?)\s*\[(\d+)\s*(?:[-～~—到至]\s*(\d+))?\s*\]\s*$""",
    )
    private val trRegex = Regex(
        """<tr\b[^>]*>(.*?)</tr>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val tdRegex = Regex(
        """<td\b([^>]*)>(.*?)</td>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val brSplit = Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE)

    fun looksLikeBnuTimetable(html: String): Boolean {
        if (html.isBlank()) return false
        val t = html.lowercase()
        val hasWeekHeader = "星期一" in html || "星期日" in html || "星期二" in html
        if (!hasWeekHeader) return false
        return "mytable" in t ||
            "xskcb" in t ||
            "学生选课课程表" in html ||
            "学生课表" in html ||
            "div_nokb" in t ||
            courseDivRegex.containsMatchIn(html) ||
            weeksSlotsInHtml.containsMatchIn(html)
    }

    fun parse(html: String): List<CourseItem> {
        if (html.isBlank()) return emptyList()
        val tableHtml = extractMyTable(html) ?: html
        val collected = linkedMapOf<String, CourseItem>()

        trRegex.findAll(tableHtml).forEach { trMatch ->
            val rowHtml = trMatch.groupValues[1]
            // Skip pure header row
            if (rowHtml.contains("星期一") && rowHtml.contains("星期日")) return@forEach

            val cells = tdRegex.findAll(rowHtml).map { it.groupValues[2] }.toList()
            if (cells.isEmpty()) return@forEach

            val dayStart = indexOfFirstDayCell(cells)
            if (dayStart < 0) return@forEach
            val dayCells = cells.drop(dayStart).take(7)
            dayCells.forEachIndexed { dayIndex, cellHtml ->
                val weekday = dayIndex + 1
                extractCourseBlocks(cellHtml).forEach { block ->
                    parseCourseBlock(block, weekday)?.let { course ->
                        collected.putIfAbsent(dedupeKey(course), course)
                    }
                }
            }
        }

        return collected.values
            .sortedWith(compareBy({ it.weekday }, { it.startSlot }, { it.title }, { it.teachingWeekLabel }))
            .toList()
    }

    private fun extractMyTable(html: String): String? {
        val start = Regex(
            """<table[^>]*\bid\s*=\s*['"]mytable['"][^>]*>""",
            RegexOption.IGNORE_CASE,
        ).find(html)?.range?.first ?: return null
        val end = html.indexOf("</table>", startIndex = start, ignoreCase = true)
        if (end < 0) return html.substring(start)
        return html.substring(start, end + "</table>".length)
    }

    /** Leading cells are 上午/下午/晚上 and/or 大节「一」…「六」; the next 7 are Mon–Sun. */
    private fun indexOfFirstDayCell(cells: List<String>): Int {
        for (i in cells.indices) {
            val plain = stripTags(cells[i]).replace("\\s+".toRegex(), "")
            if (plain == "上午" || plain == "下午" || plain == "晚上") continue
            if (plain in setOf("一", "二", "三", "四", "五", "六")) continue
            // First non-label cell (may be empty day cell with div_nokb).
            return i
        }
        return -1
    }

    private fun extractCourseBlocks(cellHtml: String): List<String> =
        courseDivRegex.findAll(cellHtml).map { match ->
            brSplit.split(match.groupValues[1])
                .map { stripTags(it).trim() }
                .filter { it.isNotEmpty() }
                .joinToString("\n")
        }.filter { it.isNotBlank() }.toList()

    fun parseCourseBlock(block: String, weekday: Int): CourseItem? {
        val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size < 3) return null
        val title = lines[0]
        if (title.length < 2) return null

        val metaIndex = lines.indexOfFirst { weeksSlotsRegex.containsMatchIn(it) }
            .takeIf { it >= 0 } ?: return null
        val meta = weeksSlotsRegex.matchEntire(lines[metaIndex]) ?: return null
        val weekLabel = lines[metaIndex].substringBefore('[').trim()
            .replace(Regex("""^0+(\d)"""), "$1")
        val startSlot = meta.groupValues[2].toInt()
        val endSlot = meta.groupValues[3].toIntOrNull() ?: startSlot
        if (startSlot !in 1..20) return null

        val teacher = if (metaIndex >= 2) lines[1].trim() else ""
        val location = lines.getOrNull(metaIndex + 1).orEmpty().trim()
        val weeks = AcademicTimetableParser.parseTeachingWeekFormat(weekLabel)

        return CourseItem(
            id = UUID.randomUUID().toString(),
            title = title,
            teacher = teacher,
            location = location,
            weekday = weekday,
            startSlot = startSlot,
            endSlot = endSlot.coerceIn(startSlot, 20),
            teachingWeeks = weeks,
            teachingWeekLabel = weekLabel,
        )
    }

    private fun stripTags(html: String): String =
        html.replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&ensp;", " ")
            .replace("&emsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .trim()

    private fun dedupeKey(course: CourseItem): String =
        listOf(
            course.title,
            course.weekday,
            course.startSlot,
            course.endSlot,
            course.location,
            course.teacher,
            course.teachingWeekLabel,
            course.teachingWeeks.joinToString(","),
        ).joinToString("|")
}

package com.mydailylife.schedule.data

import java.util.UUID

/**
 * Parses Excel-exported CSV for course import.
 *
 * Expected header (order flexible; Chinese or English keys):
 * 课程名/title, 教师/teacher, 地点/location, 星期/weekday, 开始节次/start, 结束节次/end
 *
 * Weekday: 1–7 (Mon–Sun) or 一…日 / 周一…周日.
 * `.xlsx` is not parsed yet — save as CSV from Excel first.
 */
object CourseExcelImporter {
    private val titleKeys = setOf("课程名", "课程", "名称", "title", "name", "course")
    private val teacherKeys = setOf("教师", "老师", "讲师", "teacher")
    private val locationKeys = setOf("地点", "教室", "location", "room")
    private val weekdayKeys = setOf("星期", "周几", "weekday", "day")
    private val startKeys = setOf("开始节次", "开始", "start", "startslot", "start_slot")
    private val endKeys = setOf("结束节次", "结束", "end", "endslot", "end_slot")

    fun parseCsv(text: String): List<CourseItem> {
        val rows = readCsvRows(text)
        if (rows.isEmpty()) error("文件为空")
        val header = rows.first().map { normalizeHeader(it) }
        val titleIdx = indexOf(header, titleKeys) ?: error("缺少「课程名」列")
        val teacherIdx = indexOf(header, teacherKeys)
        val locationIdx = indexOf(header, locationKeys)
        val weekdayIdx = indexOf(header, weekdayKeys) ?: error("缺少「星期」列")
        val startIdx = indexOf(header, startKeys) ?: error("缺少「开始节次」列")
        val endIdx = indexOf(header, endKeys) ?: error("缺少「结束节次」列")

        val maxSlot = CourseGridDefaults.slotCount
        val courses = rows.drop(1).mapNotNull { cols ->
            if (cols.all { it.isBlank() }) return@mapNotNull null
            val title = cols.getOrNull(titleIdx)?.trim().orEmpty()
            if (title.isEmpty()) return@mapNotNull null
            val weekday = parseWeekday(cols.getOrNull(weekdayIdx).orEmpty())
                ?: error("无法识别星期：${cols.getOrNull(weekdayIdx)}")
            val start = cols.getOrNull(startIdx)?.trim()?.toIntOrNull()
                ?: error("开始节次无效：${cols.getOrNull(startIdx)}")
            val end = cols.getOrNull(endIdx)?.trim()?.toIntOrNull()
                ?: error("结束节次无效：${cols.getOrNull(endIdx)}")
            if (start !in 1..maxSlot || end !in start..maxSlot) {
                error("节次需在 1–$maxSlot，且结束≥开始（课程：$title）")
            }
            CourseItem(
                id = UUID.randomUUID().toString(),
                title = title,
                teacher = cols.getOrNull(teacherIdx ?: -1)?.trim().orEmpty(),
                location = cols.getOrNull(locationIdx ?: -1)?.trim().orEmpty(),
                weekday = weekday,
                startSlot = start,
                endSlot = end,
            )
        }
        if (courses.isEmpty()) error("未解析到有效课程行")
        return courses
    }

    fun isLikelySpreadsheetName(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".csv") ||
            lower.endsWith(".xlsx") ||
            lower.endsWith(".xls") ||
            lower.endsWith(".tsv")
    }

    fun isCsvName(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".csv") || lower.endsWith(".tsv")
    }

    private fun indexOf(header: List<String>, keys: Set<String>): Int? {
        val normalizedKeys = keys.map { normalizeHeader(it) }.toSet()
        return header.indexOfFirst { it in normalizedKeys }.takeIf { it >= 0 }
    }

    private fun normalizeHeader(raw: String): String =
        raw.trim()
            .removePrefix("\uFEFF")
            .lowercase()
            .replace(" ", "")
            .replace("_", "")

    private fun parseWeekday(raw: String): Int? {
        val t = raw.trim()
        t.toIntOrNull()?.takeIf { it in 1..7 }?.let { return it }
        val mapped = when (t.replace("周", "").replace("星期", "")) {
            "一", "1", "mon", "monday" -> 1
            "二", "2", "tue", "tuesday" -> 2
            "三", "3", "wed", "wednesday" -> 3
            "四", "4", "thu", "thursday" -> 4
            "五", "5", "fri", "friday" -> 5
            "六", "6", "sat", "saturday" -> 6
            "日", "天", "7", "sun", "sunday" -> 7
            else -> null
        }
        return mapped
    }

    /** Minimal RFC4180-ish CSV reader (quoted fields, comma/tab/semicolon). */
    private fun readCsvRows(text: String): List<List<String>> {
        val cleaned = text.removePrefix("\uFEFF")
        if (cleaned.isBlank()) return emptyList()
        val firstLine = cleaned.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
        val delimiter = when {
            firstLine.count { it == '\t' } >= firstLine.count { it == ',' } &&
                firstLine.contains('\t') -> '\t'
            firstLine.count { it == ';' } > firstLine.count { it == ',' } -> ';'
            else -> ','
        }
        val rows = mutableListOf<List<String>>()
        val field = StringBuilder()
        val row = mutableListOf<String>()
        var inQuotes = false
        var i = 0
        while (i < cleaned.length) {
            val c = cleaned[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < cleaned.length && cleaned[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                !inQuotes && c == delimiter -> {
                    row += field.toString()
                    field.clear()
                }
                !inQuotes && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && i + 1 < cleaned.length && cleaned[i + 1] == '\n') i++
                    row += field.toString()
                    field.clear()
                    if (row.any { it.isNotBlank() }) rows += row.toList()
                    row.clear()
                }
                else -> field.append(c)
            }
            i++
        }
        row += field.toString()
        if (row.any { it.isNotBlank() }) rows += row
        return rows
    }
}

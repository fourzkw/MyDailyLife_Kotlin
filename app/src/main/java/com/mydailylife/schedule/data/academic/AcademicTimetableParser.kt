package com.mydailylife.schedule.data.academic

import com.mydailylife.schedule.data.CourseGridDefaults
import com.mydailylife.schedule.data.CourseItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

data class AcademicCaptureResult(
    val courses: List<CourseItem>,
    val sourceHint: String,
)

/**
 * Turns WebView capture JSON (network buffers + DOM scrape) into [CourseItem]s.
 * Optimized for CQU `my-table-detail` → `classTimetableVOList`.
 */
object AcademicTimetableParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private const val MAX_SLOT = 20

    fun parseCapturePayload(raw: String): AcademicCaptureResult {
        val text = unwrapEvaluateJavascriptResult(raw)
        if (text.isBlank() || text == "null") {
            error("未捕获到页面数据，请确认已打开课表并展开课程后再试")
        }
        val root = runCatching { json.parseToJsonElement(text) }.getOrElse {
            error("捕获结果不是有效 JSON")
        }
        val collected = linkedMapOf<String, CourseItem>()
        var hint = "unknown"

        if (root is JsonObject) {
            val network = root["network"] as? JsonArray
            network?.forEach { entry ->
                val obj = entry as? JsonObject ?: return@forEach
                val body = obj.stringProp("body") ?: return@forEach
                val fromBody = parseJsonBlob(body)
                fromBody.forEach { course ->
                    collected.putIfAbsent(dedupeKey(course), course)
                }
                if (fromBody.isNotEmpty()) hint = "network"
            }
            val dom = root["domCourses"] as? JsonArray
            if (dom != null) {
                parseCourseArray(dom).forEach { course ->
                    collected.putIfAbsent(dedupeKey(course), course)
                }
                if (hint == "unknown" && collected.isNotEmpty()) hint = "dom"
            }
            if (collected.isEmpty()) {
                parseJsonBlob(text).forEach { course ->
                    collected.putIfAbsent(dedupeKey(course), course)
                }
                if (collected.isNotEmpty()) hint = "cqu-root"
            }
        } else if (root is JsonArray) {
            parseCourseArray(root).forEach { course ->
                collected.putIfAbsent(dedupeKey(course), course)
            }
            hint = "array"
        }

        val courses = collected.values
            .sortedWith(compareBy({ it.weekday }, { it.startSlot }, { it.title }))
            .toList()
        if (courses.isEmpty()) {
            error("未能识别课表字段。请打开课表页、展开课程后重试；若仍失败，把课表接口样例发给开发者适配")
        }
        return AcademicCaptureResult(courses = courses, sourceHint = hint)
    }

    /** Directly parse a buffered network response body (e.g. my-table-detail). */
    fun parseJsonBlob(body: String): List<CourseItem> {
        val element = runCatching { json.parseToJsonElement(body) }.getOrNull() ?: return emptyList()
        return when (element) {
            is JsonArray -> parseCourseArray(element)
            is JsonObject -> {
                // Prefer CQU timetable list when present.
                val cqu = element["classTimetableVOList"] as? JsonArray
                if (cqu != null) {
                    return parseCourseArray(cqu)
                }
                val arrays = findCourseArrays(element)
                arrays.flatMap { parseCourseArray(it) }.ifEmpty {
                    listOfNotNull(parseCourseObject(element))
                }
            }
            else -> emptyList()
        }
    }

    private fun findCourseArrays(obj: JsonObject): List<JsonArray> {
        val keys = listOf(
            "classTimetableVOList",
            "courses", "courseList", "classList", "kbList", "data", "list",
            "rows", "records", "result", "timetable", "schedule",
        )
        val found = mutableListOf<JsonArray>()
        fun walk(el: JsonElement, depth: Int) {
            if (depth > 6) return
            when (el) {
                is JsonArray -> {
                    if (el.isNotEmpty() && el.first() is JsonObject) found += el
                }
                is JsonObject -> {
                    keys.forEach { k ->
                        el[k]?.let { walk(it, depth + 1) }
                    }
                    if (found.isEmpty()) {
                        el.values.forEach { walk(it, depth + 1) }
                    }
                }
                else -> Unit
            }
        }
        walk(obj, 0)
        return found.distinct()
    }

    private fun parseCourseArray(array: JsonArray): List<CourseItem> =
        array.mapNotNull { el -> (el as? JsonObject)?.let { parseCourseObject(it) } }

    private fun parseCourseObject(obj: JsonObject): CourseItem? {
        val title = firstString(
            obj,
            "课程名", "课程名称", "课名", "courseName", "kcmc", "jxbmc", "name", "title", "course",
        ) ?: return null
        if (title.length < 2) return null

        val teacherRaw = firstString(
            obj,
            "教师", "老师", "讲师", "instructorName", "teacherName", "teacher", "jsxm", "jszc",
        ).orEmpty()
        val location = firstString(
            obj,
            "地点", "教室", "上课地点", "roomName", "classroom", "placeName", "room", "cdmc", "jxdd",
        ).orEmpty()

        val weekday = parseWeekday(
            firstString(
                obj,
                "星期", "周几", "weekDay", "weekDayFormat", "weekday", "xqj", "dayOfWeek", "xq",
            ) ?: firstString(obj, "classTime", "sksj", "timeText", "rawText"),
        ) ?: inferWeekdayFromClassTime(obj) ?: return null

        val (start, end) = parseSlots(obj) ?: return null
        if (start !in 1..MAX_SLOT) return null
        val endSlot = end.coerceIn(start, MAX_SLOT)

        val weekLabel = firstString(obj, "teachingWeekFormat").orEmpty()
        val weeksFromBits = CourseItem.weeksFromBitmask(firstString(obj, "teachingWeek"))
        val weeks = weeksFromBits.ifEmpty {
            parseTeachingWeekFormat(weekLabel)
        }

        return CourseItem(
            id = firstString(obj, "id", "classId").orEmpty().ifBlank { UUID.randomUUID().toString() },
            title = title.trim(),
            teacher = cleanInstructorName(teacherRaw),
            location = location.trim(),
            weekday = weekday,
            startSlot = start,
            endSlot = endSlot,
            teachingWeeks = weeks,
            teachingWeekLabel = weekLabel.ifBlank {
                if (weeks.isEmpty()) "" else CourseItem.formatWeekList(weeks)
            },
            courseCode = firstString(obj, "courseCode").orEmpty(),
            classNbr = firstString(obj, "classNbr").orEmpty(),
            campusName = firstString(obj, "campusName").orEmpty(),
            courseStudyNature = firstString(obj, "courseStudyNature").orEmpty(),
        )
    }

    /** Parse labels like "10", "1-2", "6,11", "1-2,5,8-9". */
    fun parseTeachingWeekFormat(label: String?): List<Int> {
        if (label.isNullOrBlank()) return emptyList()
        val weeks = linkedSetOf<Int>()
        label.split(',', '，', ';', '；').map { it.trim() }.filter { it.isNotEmpty() }.forEach { part ->
            val range = part.replace("～", "-").replace("~", "-").replace("—", "-")
            val m = Regex("""(\d+)\s*-\s*(\d+)""").matchEntire(range)
            if (m != null) {
                val a = m.groupValues[1].toInt()
                val b = m.groupValues[2].toInt()
                for (w in minOf(a, b)..maxOf(a, b)) weeks += w
            } else {
                range.toIntOrNull()?.let { weeks += it }
            }
        }
        return weeks.sorted()
    }

    /** "王冰-33036[主讲];范正妍-31105[辅讲];" → "王冰、范正妍" */
    fun cleanInstructorName(raw: String): String {
        if (raw.isBlank()) return ""
        return raw.split(';', '；', ',', '，')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { part ->
                part.substringBefore('-')
                    .substringBefore('[')
                    .trim()
            }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString("、")
    }

    private fun parseSlots(obj: JsonObject): Pair<Int, Int>? {
        val start = firstInt(obj, "开始节次", "开始", "startSlot", "start", "skjc", "jcs", "beginLesson", "periodStart")
        val end = firstInt(obj, "结束节次", "结束", "endSlot", "end", "jsjc", "endLesson", "periodEnd")
        if (start != null && end != null) return start to end
        if (start != null) return start to start

        val rangeText = firstString(
            obj,
            "periodFormat", "节次", "上课节次", "jcdm", "jce", "classTime", "sksj", "timeText", "rawText", "raw",
        ) ?: obj["raw"]?.let { rawEl ->
            if (rawEl is JsonArray) rawEl.joinToString(" ") {
                (it as? JsonPrimitive)?.contentOrNull.orEmpty()
            } else null
        }
        parseSlotRange(rangeText)?.let { return it }

        // CQU bitstring e.g. "0000011" → slots 6-7 (1-based from left)
        val periodBits = firstString(obj, "period")
        return parsePeriodBitmask(periodBits)
    }

    fun parsePeriodBitmask(bits: String?): Pair<Int, Int>? {
        if (bits.isNullOrBlank()) return null
        val ones = bits.mapIndexedNotNull { index, c ->
            if (c == '1') index + 1 else null
        }
        if (ones.isEmpty()) return null
        return ones.first() to ones.last()
    }

    private fun parseSlotRange(text: String?): Pair<Int, Int>? {
        if (text.isNullOrBlank()) return null
        val normalized = text.replace("～", "-").replace("~", "-").replace("—", "-")
        Regex("""(\d+)\s*[-到至]\s*(\d+)\s*节?""").find(normalized)?.let {
            val a = it.groupValues[1].toInt()
            val b = it.groupValues[2].toInt()
            return minOf(a, b) to maxOf(a, b)
        }
        Regex("""第?\s*(\d+)\s*节""").find(normalized)?.let {
            val a = it.groupValues[1].toInt()
            return a to a
        }
        Regex("""(\d{1,2})""").find(normalized)?.let {
            val a = it.groupValues[1].toInt()
            if (a in 1..MAX_SLOT) return a to a
        }
        return null
    }

    private fun inferWeekdayFromClassTime(obj: JsonObject): Int? {
        val text = firstString(obj, "classTime", "sksj", "timeText", "rawText", "weekDayFormat") ?: return null
        return parseWeekday(text)
    }

    private fun parseWeekday(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        val t = raw.trim()
        t.toIntOrNull()?.takeIf { it in 1..7 }?.let { return it }
        Regex("""[周星期]?([一二三四五六日天1-7])""").find(t)?.groupValues?.getOrNull(1)?.let { token ->
            return when (token) {
                "一", "1" -> 1
                "二", "2" -> 2
                "三", "3" -> 3
                "四", "4" -> 4
                "五", "5" -> 5
                "六", "6" -> 6
                "日", "天", "7" -> 7
                else -> null
            }
        }
        return when {
            t.contains("周一") || t.contains("星期一") -> 1
            t.contains("周二") || t.contains("星期二") -> 2
            t.contains("周三") || t.contains("星期三") -> 3
            t.contains("周四") || t.contains("星期四") -> 4
            t.contains("周五") || t.contains("星期五") -> 5
            t.contains("周六") || t.contains("星期六") -> 6
            t.contains("周日") || t.contains("星期日") || t.contains("星期天") -> 7
            else -> null
        }
    }

    private fun JsonObject.stringProp(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

    private fun firstString(obj: JsonObject, vararg keys: String): String? {
        keys.forEach { key ->
            obj[key]?.let { el ->
                when (el) {
                    is JsonPrimitive -> el.contentOrNull?.takeIf { it.isNotBlank() }
                    is JsonArray -> el.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                        .takeIf { it.isNotBlank() }
                    else -> null
                }?.let { return it }
            }
        }
        obj.entries.forEach { (k, v) ->
            val lk = k.lowercase()
            keys.forEach { want ->
                if (lk == want.lowercase()) {
                    val s = (v as? JsonPrimitive)?.contentOrNull
                    if (!s.isNullOrBlank()) return s
                }
            }
        }
        return null
    }

    private fun firstInt(obj: JsonObject, vararg keys: String): Int? {
        firstString(obj, *keys)?.trim()?.toIntOrNull()?.let { return it }
        keys.forEach { key ->
            val el = obj[key] ?: return@forEach
            if (el is JsonPrimitive) {
                el.contentOrNull?.toIntOrNull()?.let { return it }
                runCatching { el.jsonPrimitive.content.toDouble().toInt() }.getOrNull()?.let { return it }
            }
        }
        return null
    }

    private fun dedupeKey(course: CourseItem): String =
        listOf(
            course.title,
            course.weekday,
            course.startSlot,
            course.endSlot,
            course.location,
            course.teachingWeekLabel,
            course.teachingWeeks.joinToString(","),
        ).joinToString("|")

    fun unwrapEvaluateJavascriptResult(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.length >= 2 && trimmed.startsWith('"') && trimmed.endsWith('"')) {
            return runCatching {
                json.parseToJsonElement(trimmed).jsonPrimitive.content
            }.getOrDefault(
                trimmed.removeSurrounding("\"").replace("\\\"", "\"").replace("\\n", "\n"),
            )
        }
        return trimmed
    }
}

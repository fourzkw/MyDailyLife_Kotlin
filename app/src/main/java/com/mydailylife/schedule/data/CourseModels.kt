package com.mydailylife.schedule.data

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

@Serializable
data class CourseItem(
    val id: String,
    val title: String,
    val teacher: String = "",
    val location: String = "",
    /** ISO weekday 1=Mon … 7=Sun. */
    val weekday: Int,
    val startSlot: Int,
    val endSlot: Int,
    /**
     * Teaching weeks when this slot runs (1-based).
     * Empty = every week (Excel / legacy).
     */
    val teachingWeeks: List<Int> = emptyList(),
    /** Human label from教务, e.g. "10" / "1-2" / "6,11". */
    val teachingWeekLabel: String = "",
    val courseCode: String = "",
    val classNbr: String = "",
    val campusName: String = "",
    val courseStudyNature: String = "",
) {
    fun occursInTeachingWeek(week: Int): Boolean =
        teachingWeeks.isEmpty() || week in teachingWeeks

    fun weeksSummary(): String = when {
        teachingWeekLabel.isNotBlank() -> teachingWeekLabel
        teachingWeeks.isEmpty() -> "每周"
        else -> formatWeekList(teachingWeeks)
    }

    companion object {
        fun formatWeekList(weeks: List<Int>): String {
            if (weeks.isEmpty()) return "每周"
            val sorted = weeks.distinct().sorted()
            val parts = mutableListOf<String>()
            var i = 0
            while (i < sorted.size) {
                val start = sorted[i]
                var end = start
                while (i + 1 < sorted.size && sorted[i + 1] == end + 1) {
                    i++
                    end = sorted[i]
                }
                parts += if (start == end) "$start" else "$start-$end"
                i++
            }
            return parts.joinToString(",")
        }

        /** Decode CQU `teachingWeek` bitstring; index 0 = week 1. */
        fun weeksFromBitmask(bits: String?): List<Int> {
            if (bits.isNullOrBlank()) return emptyList()
            return bits.mapIndexedNotNull { index, c ->
                if (c == '1') index + 1 else null
            }
        }
    }
}

@Serializable
data class CourseStore(
    val courses: List<CourseItem> = emptyList(),
    /** ISO date of Monday for teaching week 1; null = unknown. */
    val termStartDate: String? = null,
    val maxTeachingWeek: Int = 25,
    /** Wall-clock times for each teaching period (editable in课表设置). */
    val periodSchedule: CoursePeriodSchedule = CoursePeriodSchedule(),
)

enum class CourseImportMethod(
    val title: String,
    val subtitle: String,
) {
    Excel(
        title = "Excel / CSV",
        subtitle = "支持 .xlsx / .xls / CSV 表格导入",
    ),
    Academic(
        title = "教务系统",
        subtitle = "选择学校后登录教务，捕获课表",
    ),
    Ics(
        title = "ICS 订阅",
        subtitle = "粘贴订阅链接、ICS 内容或选择 .ics 文件",
    ),
}

enum class AcademicSemester {
    /** 秋季：当年九月第二周周一为第 1 周。 */
    Autumn,
    /** 春季：当年二月第二周周一为第 1 周。 */
    Spring,
}

object CourseGridDefaults {
    val weekdayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

    /** Fallback when store has no schedule yet; matches 重大默认. */
    val timeSlots: List<String> get() = CoursePeriodPresets.cqu.map { it.start }

    val slotCount: Int get() = CoursePeriodPresets.cqu.size

    fun teachingWeekForDate(termStart: LocalDate, date: LocalDate): Int {
        val startMonday = termStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val dateMonday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weeks = ChronoUnit.WEEKS.between(startMonday, dateMonday).toInt() + 1
        return weeks.coerceAtLeast(1)
    }

    fun weekStartForTeachingWeek(termStart: LocalDate, teachingWeek: Int): LocalDate {
        val startMonday = termStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return startMonday.plusWeeks((teachingWeek - 1).toLong())
    }

    /**
     * Estimate term start Monday so that [anchorTeachingWeek] falls on [anchorMonday].
     */
    fun estimateTermStart(anchorMonday: LocalDate, anchorTeachingWeek: Int): LocalDate {
        val week = anchorTeachingWeek.coerceAtLeast(1)
        return anchorMonday.minusWeeks((week - 1).toLong())
    }

    /**
     * Monday of the [weekOfMonth]-th calendar week of [month].
     * Week 1 is the week that contains the 1st (Monday may fall in the previous month).
     */
    fun mondayOfMonthWeek(year: Int, month: Int, weekOfMonth: Int): LocalDate {
        val first = LocalDate.of(year, month, 1)
        val week1Monday = first.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return week1Monday.plusWeeks((weekOfMonth.coerceAtLeast(1) - 1).toLong())
    }

    fun termStartForSemester(semester: AcademicSemester, year: Int): LocalDate = when (semester) {
        AcademicSemester.Autumn -> mondayOfMonthWeek(year, 9, 2)
        AcademicSemester.Spring -> mondayOfMonthWeek(year, 2, 2)
    }

    /**
     * Default first-week Monday for [today]:
     * - Aug–Dec → autumn of this year (Sept week 2)
     * - Jan → autumn of previous year
     * - Feb–Jul → spring of this year (Feb week 2)
     */
    fun defaultTermStart(today: LocalDate = LocalDate.now()): LocalDate {
        val semester = inferredSemester(today)
        val year = when {
            semester == AcademicSemester.Autumn && today.monthValue == 1 -> today.year - 1
            else -> today.year
        }
        return termStartForSemester(semester, year)
    }

    fun inferredSemester(today: LocalDate = LocalDate.now()): AcademicSemester =
        when (today.monthValue) {
            in 2..7 -> AcademicSemester.Spring
            else -> AcademicSemester.Autumn
        }

    /** Normalize any picked date to that week's Monday (teaching week boundary). */
    fun asTermStartMonday(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}

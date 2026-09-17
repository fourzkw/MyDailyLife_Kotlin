package com.mydailylife.schedule.data

import kotlinx.serialization.Serializable
import java.time.LocalTime

/** One teaching period with wall-clock start/end (`HH:mm`). */
@Serializable
data class CoursePeriod(
    val start: String,
    val end: String,
) {
    fun label(): String = "$start–$end"

    fun startTime(): LocalTime = parseHm(start)
    fun endTime(): LocalTime = parseHm(end)

    companion object {
        fun parseHm(raw: String): LocalTime {
            val parts = raw.trim().split(':', '：')
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            return LocalTime.of(h.coerceIn(0, 23), m.coerceIn(0, 59))
        }

        fun formatHm(time: LocalTime): String =
            "%02d:%02d".format(time.hour, time.minute)
    }
}

/**
 * Editable period timetable shown on the course grid.
 * [presetId] is [CoursePeriodPresets.BNU] / [CoursePeriodPresets.CQU] / [CoursePeriodPresets.CUSTOM].
 */
@Serializable
data class CoursePeriodSchedule(
    val presetId: String = CoursePeriodPresets.CQU,
    val periods: List<CoursePeriod> = CoursePeriodPresets.cqu,
) {
    val slotCount: Int get() = periods.size.coerceAtLeast(1)

    /** 1-based slot after which a lunch break band is drawn. */
    fun lunchBreakAfterSlot(): Int = 4

    /**
     * 1-based slot after which an evening break band is drawn
     * (last afternoon period; evening starts at the next slot).
     */
    fun eveningBreakAfterSlot(): Int {
        val eveningIndex0 = periods.indexOfFirst { it.startTime().hour >= 18 }
        return if (eveningIndex0 > 0) eveningIndex0 else 8
    }

    fun withPeriod(index0: Int, period: CoursePeriod): CoursePeriodSchedule {
        if (index0 !in periods.indices) return this
        val next = periods.toMutableList().also { it[index0] = period }
        return copy(presetId = CoursePeriodPresets.CUSTOM, periods = next)
    }

    fun applyPreset(presetId: String): CoursePeriodSchedule {
        val preset = CoursePeriodPresets.periodsFor(presetId) ?: return this
        return CoursePeriodSchedule(presetId = presetId, periods = preset)
    }
}

object CoursePeriodPresets {
    const val BNU = "bnu"
    const val CQU = "cqu"
    const val CUSTOM = "custom"

    /** 北京师范大学默认作息（图1）。 */
    val bnu: List<CoursePeriod> = listOf(
        CoursePeriod("08:00", "08:45"),
        CoursePeriod("08:55", "09:40"),
        CoursePeriod("10:00", "10:45"),
        CoursePeriod("10:55", "11:40"),
        CoursePeriod("13:30", "14:15"),
        CoursePeriod("14:25", "15:10"),
        CoursePeriod("15:30", "16:15"),
        CoursePeriod("16:25", "17:10"),
        CoursePeriod("18:00", "18:45"),
        CoursePeriod("18:55", "19:40"),
        CoursePeriod("19:50", "20:35"),
        CoursePeriod("20:45", "21:30"),
    )

    /** 重庆大学默认作息（图2）。 */
    val cqu: List<CoursePeriod> = listOf(
        CoursePeriod("08:30", "09:15"),
        CoursePeriod("09:25", "10:10"),
        CoursePeriod("10:30", "11:15"),
        CoursePeriod("11:25", "12:10"),
        CoursePeriod("13:30", "14:15"),
        CoursePeriod("14:25", "15:10"),
        CoursePeriod("15:20", "16:05"),
        CoursePeriod("16:25", "17:10"),
        CoursePeriod("17:20", "18:05"),
        CoursePeriod("19:00", "19:45"),
        CoursePeriod("19:55", "20:40"),
        CoursePeriod("20:50", "21:35"),
    )

    fun periodsFor(presetId: String): List<CoursePeriod>? = when (presetId) {
        BNU -> bnu
        CQU -> cqu
        else -> null
    }

    fun label(presetId: String): String = when (presetId) {
        BNU -> "北京师范大学"
        CQU -> "重庆大学"
        CUSTOM -> "自定义"
        else -> presetId
    }

    val selectable: List<String> = listOf(BNU, CQU)
}

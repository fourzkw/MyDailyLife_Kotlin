package com.mydailylife.schedule.data

/**
 * Discrete course-grid type scale (sp values).
 * Level [LEGACY] matches the previous hardcoded sizes; [DEFAULT] is one step smaller.
 */
object CourseGridFontScale {
    const val MIN = 0
    const val MAX = 4
    /** Previous app sizes (time 7 / title 10 / meta 9). */
    const val LEGACY = 2
    /** Default: one step smaller than [LEGACY]. */
    const val DEFAULT = 1

    data class Sizes(
        val timeSp: Int,
        val titleSp: Int,
        val metaSp: Int,
        val timeLineSp: Int,
        val titleLineSp: Int,
        val metaLineSp: Int,
    )

    fun coerce(level: Int): Int = level.coerceIn(MIN, MAX)

    fun label(level: Int): String = when (coerce(level)) {
        0 -> "更小"
        1 -> "小"
        2 -> "中"
        3 -> "大"
        else -> "更大"
    }

    fun sizes(level: Int): Sizes {
        val delta = coerce(level) - LEGACY
        fun sz(base: Int): Int = (base + delta).coerceAtLeast(5)
        fun lh(base: Int): Int = (base + delta).coerceAtLeast(8)
        return Sizes(
            timeSp = sz(7),
            titleSp = sz(10),
            metaSp = sz(9),
            timeLineSp = lh(11),
            titleLineSp = lh(12),
            metaLineSp = lh(11),
        )
    }
}

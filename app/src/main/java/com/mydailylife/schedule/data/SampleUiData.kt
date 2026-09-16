package com.mydailylife.schedule.data

/** @deprecated Prefer [CourseGridDefaults] / [CourseItem]; kept for any leftover refs. */
@Deprecated("Use CourseGridDefaults and CourseItem")
object SampleUiData {
    data class CourseBlock(
        val id: String,
        val title: String,
        val teacher: String = "",
        val location: String = "",
        val weekday: Int,
        val startSlot: Int,
        val endSlot: Int,
    )

    val courseBlocks = emptyList<CourseBlock>()
    val weekdays = CourseGridDefaults.weekdayLabels
    val timeSlots = CourseGridDefaults.timeSlots
}

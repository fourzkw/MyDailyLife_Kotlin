package com.mydailylife.schedule.data

/** Placeholder sample for screens not yet wired (courses). */
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

    val courseBlocks = listOf(
        CourseBlock("cs1", "高等数学", "张老师", "A101", weekday = 1, startSlot = 1, endSlot = 2),
        CourseBlock("cs2", "大学英语", "李老师", "B203", weekday = 2, startSlot = 3, endSlot = 4),
        CourseBlock("cs3", "程序设计", "王老师", "C305", weekday = 3, startSlot = 5, endSlot = 7),
        CourseBlock("cs4", "线性代数", "赵老师", "A102", weekday = 5, startSlot = 1, endSlot = 2),
    )

    val weekdays = listOf("一", "二", "三", "四", "五", "六", "日")
    val timeSlots = listOf(
        "08:30", "09:25", "10:25", "11:20",
        "14:00", "14:55", "15:55", "16:50",
        "18:30", "19:25", "20:20", "21:15",
    )
}

package com.mydailylife.schedule.data

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val notificationsEnabled: Boolean = true,
    /** Minutes before end/start to remind; used as create default. */
    val reminderBeforeMinutes: Int = 15,
    /** 上课提醒（课表节次开始前）；仍受 [notificationsEnabled] 总开关约束. */
    val courseRemindersEnabled: Boolean = true,
    /** Minutes before class start; options share [ReminderMinuteOptions]. */
    val courseReminderBeforeMinutes: Int = 15,
    val defaultPriority: String = Priority.Medium.storageKey,
    val presetTags: List<String> = DefaultPresetTags,
    /** Last successful ICS subscription URL (empty if none). */
    val courseIcsUrl: String = "",
    /** 日程列表排序：综合 / 时间 / 紧急度. */
    val scheduleSortMode: String = ScheduleSortMode.Comprehensive.storageKey,
    /**
     * 课表网格字号挡位 0–4；默认 [CourseGridFontScale.DEFAULT]（比旧版小一号）.
     */
    val courseGridFontLevel: Int = CourseGridFontScale.DEFAULT,
) {
    val defaultPriorityEnum: Priority get() = Priority.fromStorage(defaultPriority)
    val scheduleSortModeEnum: ScheduleSortMode
        get() = ScheduleSortMode.fromStorage(scheduleSortMode)
    val courseGridFontLevelClamped: Int
        get() = CourseGridFontScale.coerce(courseGridFontLevel)

    companion object {
        val DefaultPresetTags: List<String> =
            listOf("工作", "生活", "学习", "健康", "娱乐")

        val ReminderMinuteOptions: List<Int> = listOf(15, 30, 60, 120, 1440)

        fun reminderLabel(minutes: Int): String = when (minutes) {
            15 -> "15 分钟"
            30 -> "30 分钟"
            60 -> "1 小时"
            120 -> "2 小时"
            1440 -> "1 天"
            else -> "$minutes 分钟"
        }
    }
}

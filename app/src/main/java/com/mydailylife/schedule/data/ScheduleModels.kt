package com.mydailylife.schedule.data

import kotlinx.serialization.Serializable

enum class Priority(val storageKey: String, val label: String) {
    Urgent("urgent", "紧急"),
    High("high", "高"),
    Medium("medium", "中"),
    Low("low", "低");

    companion object {
        fun fromStorage(key: String): Priority =
            entries.find { it.storageKey == key } ?: Medium
    }
}

enum class ScheduleType(val storageKey: String, val label: String) {
    Schedule("schedule", "日程"),
    Task("task", "任务");

    companion object {
        fun fromStorage(key: String): ScheduleType =
            entries.find { it.storageKey == key } ?: Schedule
    }
}

@Serializable
data class ScheduleItem(
    val id: String,
    val title: String,
    val description: String = "",
    val type: String = ScheduleType.Schedule.storageKey,
    val priority: String = Priority.Medium.storageKey,
    val tags: List<String> = emptyList(),
    /** Epoch millis; 0 means unset (typical for tasks). */
    val startTimeMillis: Long = 0L,
    val endTimeMillis: Long = 0L,
    val completed: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderBeforeMinutes: Int = 15,
    val repeat: String = "none",
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
) {
    val priorityEnum: Priority get() = Priority.fromStorage(priority)
    val typeEnum: ScheduleType get() = ScheduleType.fromStorage(type)
}

enum class DateFilter(val label: String) {
    All("全部"),
    Today("今天"),
    Tomorrow("明天"),
    Week("本周"),
}

val ItemsFilterChips = listOf(
    "全部",
    "今天",
    "明天",
    "本周",
    "日程",
    "任务",
)

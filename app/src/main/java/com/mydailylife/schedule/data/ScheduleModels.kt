package com.mydailylife.schedule.data

import kotlinx.serialization.Serializable
import java.time.DayOfWeek

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

/**
 * How an item relates to time. Replaces the old 日程/任务 split.
 *
 * - [Once]: one-shot; optional start/end datetime (each is date + clock time; may span days)
 * - [Daily]: every day; optional start/end (time-of-day / anchor)
 * - [Weekly]: selected weekdays ([weekdays] ISO 1=Mon…7=Sun)
 * - [Unlimited]: no time constraint; appears on every day; complete/delete applies to the whole item
 *
 * Legacy storage key `range` is read as [Once].
 */
enum class ScheduleTimeMode(val storageKey: String, val label: String) {
    Once("once", "仅一次"),
    Daily("daily", "每天"),
    Weekly("weekly", "每周"),
    Unlimited("unlimited", "无限制");

    companion object {
        fun fromStorage(key: String): ScheduleTimeMode =
            entries.find { it.storageKey == key }
                ?: when (key) {
                    "range" -> Once
                    "none" -> Once
                    "monthly", "yearly" -> Once
                    else -> Once
                }
    }
}

val WeekdayLabels = listOf(
    DayOfWeek.MONDAY to "一",
    DayOfWeek.TUESDAY to "二",
    DayOfWeek.WEDNESDAY to "三",
    DayOfWeek.THURSDAY to "四",
    DayOfWeek.FRIDAY to "五",
    DayOfWeek.SATURDAY to "六",
    DayOfWeek.SUNDAY to "日",
)

@Serializable
data class ScheduleItem(
    val id: String,
    val title: String,
    val description: String = "",
    val priority: String = Priority.Medium.storageKey,
    val tags: List<String> = emptyList(),
    /** Epoch millis; 0 means unset. */
    val startTimeMillis: Long = 0L,
    val endTimeMillis: Long = 0L,
    val completed: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderBeforeMinutes: Int = 15,
    val timeMode: String = ScheduleTimeMode.Once.storageKey,
    /** ISO day-of-week values 1–7; used when [timeMode] is weekly. */
    val weekdays: List<Int> = emptyList(),
    /** ISO local dates `yyyy-MM-dd` skipped for daily/weekly recurrence. */
    val excludedDates: List<String> = emptyList(),
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
) {
    val priorityEnum: Priority get() = Priority.fromStorage(priority)
    val timeModeEnum: ScheduleTimeMode get() = ScheduleTimeMode.fromStorage(timeMode)

    fun excludes(date: java.time.LocalDate): Boolean =
        date.toString() in excludedDates
}

enum class DeleteScope {
    /** Skip only the occurrence on a given day (daily / weekly). */
    ThisDay,
    /** Remove the whole item. */
    Entire,
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
)

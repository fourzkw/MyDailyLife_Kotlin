package com.mydailylife.schedule.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

object ScheduleQuery {
    private val zone: ZoneId = ZoneId.systemDefault()

    fun calculatePriorityScore(item: ScheduleItem, nowMillis: Long = System.currentTimeMillis()): Double {
        val urgency = when (item.priorityEnum) {
            Priority.Urgent -> 4.0
            Priority.High -> 3.0
            Priority.Medium -> 2.0
            Priority.Low -> 1.0
        }
        var score = urgency * 0.4

        val dueMillis = when {
            item.endTimeMillis > 0L -> item.endTimeMillis
            item.startTimeMillis > 0L -> item.startTimeMillis
            else -> 0L
        }
        score += if (dueMillis <= 0L || item.timeModeEnum == ScheduleTimeMode.Unlimited) {
            urgency * 0.1
        } else {
            val diffDays = ScheduleTimeFormat.daysUntil(dueMillis, nowMillis)
            val timeScore = when {
                diffDays < 0 -> 4.0
                diffDays == 0L -> 3.0
                diffDays <= 3 -> 2.0
                diffDays <= 7 -> 1.0
                else -> 0.0
            }
            timeScore * 0.4
        }

        score += if (item.completed) 0.0 else 2.0 * 0.2
        return score
    }

    fun sorted(
        items: List<ScheduleItem>,
        mode: ScheduleSortMode = ScheduleSortMode.Comprehensive,
    ): List<ScheduleItem> {
        return when (mode) {
            ScheduleSortMode.Comprehensive -> items
                .map { it to calculatePriorityScore(it) }
                .sortedWith(
                    compareByDescending<Pair<ScheduleItem, Double>> { it.second }
                        .thenBy { itemSortKey(it.first) }
                        .thenByDescending { it.first.updatedAtMillis },
                )
                .map { it.first }
            ScheduleSortMode.Time -> items.sortedWith(
                compareBy<ScheduleItem> { itemSortKey(it) }
                    .thenByDescending { priorityRank(it.priorityEnum) }
                    .thenByDescending { it.updatedAtMillis },
            )
            ScheduleSortMode.Urgency -> items.sortedWith(
                compareByDescending<ScheduleItem> { priorityRank(it.priorityEnum) }
                    .thenBy { itemSortKey(it) }
                    .thenByDescending { it.updatedAtMillis },
            )
        }
    }

    private fun priorityRank(priority: Priority): Int = when (priority) {
        Priority.Urgent -> 4
        Priority.High -> 3
        Priority.Medium -> 2
        Priority.Low -> 1
    }

    private fun itemSortKey(item: ScheduleItem): Long {
        return when {
            item.startTimeMillis > 0L -> item.startTimeMillis
            item.endTimeMillis > 0L -> item.endTimeMillis
            else -> Long.MAX_VALUE
        }
    }

    fun pendingVisible(
        items: List<ScheduleItem>,
        query: String,
        chip: String,
        mode: ScheduleSortMode = ScheduleSortMode.Comprehensive,
    ): List<ScheduleItem> {
        val keyword = query.trim()
        val today = LocalDate.now(zone)
        return sorted(items, mode)
            .filter { !it.completed }
            .filter { item ->
                if (keyword.isEmpty()) true
                else {
                    item.title.contains(keyword, ignoreCase = true) ||
                        item.description.contains(keyword, ignoreCase = true)
                }
            }
            .filter { item -> matchesChip(item, chip, today) }
    }

    fun completed(
        items: List<ScheduleItem>,
        mode: ScheduleSortMode = ScheduleSortMode.Comprehensive,
    ): List<ScheduleItem> =
        sorted(items, mode).filter { it.completed }

    fun occursOn(
        item: ScheduleItem,
        date: LocalDate,
        zone: ZoneId = ScheduleQuery.zone,
    ): Boolean {
        return when (item.timeModeEnum) {
            // One global item on every day; complete/delete toggles the whole item.
            ScheduleTimeMode.Unlimited -> true
            ScheduleTimeMode.Once -> !item.excludes(date) && withinDateSpan(item, date, zone)
            ScheduleTimeMode.Daily -> !item.excludes(date) && withinRecurrenceWindow(item, date, zone)
            ScheduleTimeMode.Weekly -> {
                if (item.excludes(date)) return false
                val iso = date.dayOfWeek.value
                if (item.weekdays.isNotEmpty() && iso !in item.weekdays) return false
                withinRecurrenceWindow(item, date, zone)
            }
        }
    }

    fun itemsForDate(
        items: List<ScheduleItem>,
        date: LocalDate,
        mode: ScheduleSortMode = ScheduleSortMode.Comprehensive,
    ): List<ScheduleItem> =
        sorted(items.filter { occursOn(it, date) }, mode)

    fun dayPrioritiesInMonth(
        items: List<ScheduleItem>,
        month: YearMonth,
    ): Map<LocalDate, List<Priority>> {
        val result = linkedMapOf<LocalDate, MutableList<Priority>>()
        val days = month.lengthOfMonth()
        for (day in 1..days) {
            val date = month.atDay(day)
            items.forEach { item ->
                if (!item.completed && occursOn(item, date)) {
                    result.getOrPut(date) { mutableListOf() }.add(item.priorityEnum)
                }
            }
        }
        return result.mapValues { (_, priorities) ->
            priorities
                .sortedBy {
                    when (it) {
                        Priority.Urgent -> 0
                        Priority.High -> 1
                        Priority.Medium -> 2
                        Priority.Low -> 3
                    }
                }
                .distinct()
                .take(3)
        }
    }

    fun statistics(items: List<ScheduleItem>, nowMillis: Long = System.currentTimeMillis()): ScheduleStatistics {
        val total = items.size
        val completedCount = items.count { it.completed }
        val urgent = items.count { it.priorityEnum == Priority.Urgent && !it.completed }
        val overdue = items.count { item ->
            !item.completed && isOverdue(item, nowMillis)
        }
        val completionRate = if (total > 0) completedCount.toFloat() / total else 0f

        val priorityBars = Priority.entries.map { priority ->
            val count = items.count { it.priorityEnum == priority }
            PriorityBar(
                priority = priority,
                count = count,
                ratio = if (total > 0) count.toFloat() / total else 0f,
            )
        }.filter { it.count > 0 }

        val tagCounts = linkedMapOf<String, Int>()
        items.forEach { item ->
            item.tags.forEach { tag ->
                tagCounts[tag] = (tagCounts[tag] ?: 0) + 1
            }
        }
        val tagTotal = tagCounts.values.sum().coerceAtLeast(1)
        val topTags = tagCounts.entries
            .sortedByDescending { it.value }
            .take(8)
            .map { (name, count) ->
                TagStat(name = name, count = count, ratio = count.toFloat() / tagTotal)
            }

        val today = LocalDate.now(zone)
        val weekTrend = (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val count = items.count { occursOn(it, date) }
            DayTrend(
                date = date,
                label = when (date.dayOfWeek) {
                    DayOfWeek.MONDAY -> "一"
                    DayOfWeek.TUESDAY -> "二"
                    DayOfWeek.WEDNESDAY -> "三"
                    DayOfWeek.THURSDAY -> "四"
                    DayOfWeek.FRIDAY -> "五"
                    DayOfWeek.SATURDAY -> "六"
                    DayOfWeek.SUNDAY -> "日"
                },
                count = count,
            )
        }

        return ScheduleStatistics(
            total = total,
            completed = completedCount,
            urgent = urgent,
            overdue = overdue,
            completionRate = completionRate,
            priorityBars = priorityBars,
            topTags = topTags,
            weekTrend = weekTrend,
        )
    }

    private fun isOverdue(item: ScheduleItem, nowMillis: Long): Boolean {
        return when (item.timeModeEnum) {
            ScheduleTimeMode.Unlimited, ScheduleTimeMode.Daily, ScheduleTimeMode.Weekly -> false
            ScheduleTimeMode.Once -> {
                val due = when {
                    item.endTimeMillis > 0L -> item.endTimeMillis
                    item.startTimeMillis > 0L -> item.startTimeMillis
                    else -> return false
                }
                due < nowMillis
            }
        }
    }

    private fun matchesChip(item: ScheduleItem, chip: String, today: LocalDate): Boolean {
        return when (chip) {
            "全部" -> true
            "今天" -> occursOn(item, today)
            "明天" -> occursOn(item, today.plusDays(1))
            "本周" -> (0L..7L).any { occursOn(item, today.plusDays(it)) }
            else -> true
        }
    }

    private fun withinRecurrenceWindow(
        item: ScheduleItem,
        date: LocalDate,
        zone: ZoneId,
    ): Boolean {
        val start = dateOf(item.startTimeMillis, zone)
        if (start != null && date.isBefore(start)) return false
        val end = dateOf(item.endTimeMillis, zone)
        // Same-day end is time-of-day only; later end date closes the series.
        if (start != null && end != null && end.isAfter(start) && date.isAfter(end)) return false
        return true
    }

    /** Once: inclusive calendar span from start and/or end; unset times → not on any day. */
    private fun withinDateSpan(
        item: ScheduleItem,
        date: LocalDate,
        zone: ZoneId,
    ): Boolean {
        val start = dateOf(item.startTimeMillis, zone)
        val end = dateOf(item.endTimeMillis, zone)
        return when {
            start != null && end != null -> !date.isBefore(start) && !date.isAfter(end)
            start != null -> date == start
            end != null -> date == end
            else -> false
        }
    }

    private fun dateOf(millis: Long, zone: ZoneId = ScheduleQuery.zone): LocalDate? {
        if (millis <= 0L) return null
        return Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    }
}

data class PriorityBar(
    val priority: Priority,
    val count: Int,
    val ratio: Float,
)

data class TagStat(
    val name: String,
    val count: Int,
    val ratio: Float,
)

data class DayTrend(
    val date: LocalDate,
    val label: String,
    val count: Int,
)

data class ScheduleStatistics(
    val total: Int = 0,
    val completed: Int = 0,
    val urgent: Int = 0,
    val overdue: Int = 0,
    val completionRate: Float = 0f,
    val priorityBars: List<PriorityBar> = emptyList(),
    val topTags: List<TagStat> = emptyList(),
    val weekTrend: List<DayTrend> = emptyList(),
)

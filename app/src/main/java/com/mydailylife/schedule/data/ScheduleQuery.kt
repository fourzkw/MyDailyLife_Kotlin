package com.mydailylife.schedule.data

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

        score += if (item.typeEnum == ScheduleType.Task) {
            val taskTime = when (item.priorityEnum) {
                Priority.Urgent -> 2.0
                Priority.High -> 1.5
                Priority.Medium -> 1.0
                Priority.Low -> 0.5
            }
            taskTime * 0.4
        } else {
            val diffDays = ScheduleTimeFormat.daysUntil(item.endTimeMillis, nowMillis)
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

    fun sorted(items: List<ScheduleItem>): List<ScheduleItem> {
        return items
            .map { it to calculatePriorityScore(it) }
            .sortedWith(
                compareByDescending<Pair<ScheduleItem, Double>> { it.second }
                    .thenComparator { a, b ->
                        val left = a.first
                        val right = b.first
                        when {
                            left.typeEnum == ScheduleType.Task && right.typeEnum == ScheduleType.Task ->
                                left.updatedAtMillis.compareTo(right.updatedAtMillis)
                            left.typeEnum != ScheduleType.Task && right.typeEnum != ScheduleType.Task ->
                                left.endTimeMillis.compareTo(right.endTimeMillis)
                            left.typeEnum != ScheduleType.Task -> -1
                            else -> 1
                        }
                    },
            )
            .map { it.first }
    }

    fun pendingVisible(
        items: List<ScheduleItem>,
        query: String,
        chip: String,
    ): List<ScheduleItem> {
        val keyword = query.trim()
        val today = LocalDate.now(zone)
        return sorted(items)
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

    fun completed(items: List<ScheduleItem>): List<ScheduleItem> =
        sorted(items).filter { it.completed }

    /** Align with original: day assignment uses endTime when present. */
    fun anchorDate(item: ScheduleItem): LocalDate? {
        val millis = when {
            item.endTimeMillis > 0L -> item.endTimeMillis
            item.startTimeMillis > 0L -> item.startTimeMillis
            else -> return null
        }
        return Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    }

    fun itemsForDate(items: List<ScheduleItem>, date: LocalDate): List<ScheduleItem> =
        sorted(items.filter { anchorDate(it) == date })

    fun dayPrioritiesInMonth(
        items: List<ScheduleItem>,
        month: YearMonth,
    ): Map<LocalDate, List<Priority>> {
        return items
            .mapNotNull { item ->
                val date = anchorDate(item) ?: return@mapNotNull null
                if (YearMonth.from(date) != month) return@mapNotNull null
                date to item.priorityEnum
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, priorities) ->
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
            !item.completed &&
                item.endTimeMillis > 0L &&
                item.endTimeMillis < nowMillis
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
            val count = items.count { anchorDate(it) == date }
            DayTrend(
                date = date,
                label = when (date.dayOfWeek.value) {
                    1 -> "一"
                    2 -> "二"
                    3 -> "三"
                    4 -> "四"
                    5 -> "五"
                    6 -> "六"
                    else -> "日"
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

    private fun matchesChip(item: ScheduleItem, chip: String, today: LocalDate): Boolean {
        return when (chip) {
            "全部" -> true
            "日程" -> item.typeEnum == ScheduleType.Schedule
            "任务" -> item.typeEnum == ScheduleType.Task
            "今天", "明天", "本周" -> {
                if (item.typeEnum == ScheduleType.Task) true
                else {
                    val endDate = if (item.endTimeMillis <= 0L) {
                        null
                    } else {
                        Instant.ofEpochMilli(item.endTimeMillis)
                            .atZone(zone)
                            .toLocalDate()
                    }
                    when (chip) {
                        "今天" -> endDate == today
                        "明天" -> endDate == today.plusDays(1)
                        "本周" -> endDate != null &&
                            !endDate.isBefore(today) &&
                            !endDate.isAfter(today.plusDays(7))
                        else -> true
                    }
                }
            }
            else -> true
        }
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

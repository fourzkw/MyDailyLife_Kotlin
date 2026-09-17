package com.mydailylife.schedule.data

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Maps 课表 entries onto the 日程 day list as read-only synthetic [ScheduleItem]s. */
object CourseScheduleBridge {
    const val ID_PREFIX = "course:"
    const val TAG = "课表"

    fun isCourseItem(item: ScheduleItem): Boolean =
        item.id.startsWith(ID_PREFIX)

    fun isCourseItem(id: String): Boolean =
        id.startsWith(ID_PREFIX)

    fun coursesForDate(store: CourseStore, date: LocalDate): List<CourseItem> {
        val termStart = store.termStartDate
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        return store.courses.filter { occursOnDate(it, date, termStart) }
    }

    fun toScheduleItems(
        store: CourseStore,
        date: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<ScheduleItem> =
        coursesForDate(store, date).map { toScheduleItem(it, date, zone) }

    fun occursOnDate(
        course: CourseItem,
        date: LocalDate,
        termStart: LocalDate?,
    ): Boolean {
        if (course.weekday != date.dayOfWeek.value) return false
        if (course.teachingWeeks.isEmpty()) return true
        if (termStart == null) return true
        val week = CourseGridDefaults.teachingWeekForDate(termStart, date)
        return course.occursInTeachingWeek(week)
    }

    fun toScheduleItem(
        course: CourseItem,
        date: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): ScheduleItem {
        val start = date.atTime(slotStartTime(course.startSlot))
        val end = date.atTime(slotEndTime(course.endSlot))
        val desc = listOf(course.teacher, course.location)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
        return ScheduleItem(
            id = "$ID_PREFIX${course.id}",
            title = course.title,
            description = desc,
            priority = Priority.Medium.storageKey,
            tags = listOf(TAG),
            startTimeMillis = start.atZone(zone).toInstant().toEpochMilli(),
            endTimeMillis = end.atZone(zone).toInstant().toEpochMilli(),
            completed = false,
            timeMode = ScheduleTimeMode.Once.storageKey,
            createdAtMillis = 0L,
            updatedAtMillis = 0L,
        )
    }

    fun slotStartTime(slot: Int): LocalTime {
        val label = CourseGridDefaults.timeSlots.getOrNull((slot - 1).coerceAtLeast(0))
            ?: return LocalTime.of(8, 30)
        val parts = label.split(':')
        return LocalTime.of(parts[0].toInt(), parts[1].toInt())
    }

    /** Approximate class end: start of last period + 45 minutes. */
    fun slotEndTime(endSlot: Int): LocalTime =
        slotStartTime(endSlot).plusMinutes(45)
}

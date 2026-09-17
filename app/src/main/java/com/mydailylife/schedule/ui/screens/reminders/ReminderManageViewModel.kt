package com.mydailylife.schedule.ui.screens.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydailylife.schedule.data.CourseRepository
import com.mydailylife.schedule.data.ScheduleRepository
import com.mydailylife.schedule.data.SettingsRepository
import com.mydailylife.schedule.reminder.CourseReminderTimes
import com.mydailylife.schedule.reminder.ReminderTimes
import com.mydailylife.schedule.reminder.UpcomingReminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class ReminderDayGroup(
    val date: LocalDate,
    val entries: List<UpcomingReminder>,
)

data class ReminderManageUiState(
    val loaded: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val rangeStart: LocalDate = LocalDate.now(),
    val rangeEnd: LocalDate = LocalDate.now().plusDays(
        (ReminderTimes.MANAGE_DEFAULT_RANGE_DAYS - 1).toLong(),
    ),
    val groups: List<ReminderDayGroup> = emptyList(),
    val totalCount: Int = 0,
) {
    val rangeDayCount: Int
        get() = (ChronoUnit.DAYS.between(rangeStart, rangeEnd).toInt() + 1).coerceAtLeast(1)
}

class ReminderManageViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val settingsRepository: SettingsRepository,
    private val courseRepository: CourseRepository,
) : ViewModel() {
    private val zone: ZoneId = ZoneId.systemDefault()

    private fun defaultRange(): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now(zone)
        return today to today.plusDays((ReminderTimes.MANAGE_DEFAULT_RANGE_DAYS - 1).toLong())
    }

    private val rangeStart = MutableStateFlow(defaultRange().first)
    private val rangeEnd = MutableStateFlow(defaultRange().second)

    val uiState: StateFlow<ReminderManageUiState> = combine(
        scheduleRepository.schedules,
        settingsRepository.settings,
        courseRepository.store,
        rangeStart,
        rangeEnd,
    ) { schedules, settings, courseStore, start, end ->
        val (lo, hi) = if (start.isAfter(end)) end to start else start to end
        if (!settings.notificationsEnabled) {
            ReminderManageUiState(
                loaded = true,
                notificationsEnabled = false,
                rangeStart = lo,
                rangeEnd = hi,
            )
        } else {
            val now = System.currentTimeMillis()
            val scheduleUpcoming = ReminderTimes.listUpcoming(
                items = schedules,
                nowMillis = now,
                zone = zone,
                rangeStart = lo,
                rangeEnd = hi,
            )
            val courseUpcoming = if (settings.courseRemindersEnabled) {
                CourseReminderTimes.listUpcoming(
                    store = courseStore,
                    leadMinutes = settings.courseReminderBeforeMinutes,
                    nowMillis = now,
                    zone = zone,
                    rangeStart = lo,
                    rangeEnd = hi,
                )
            } else {
                emptyList()
            }
            val upcoming = (scheduleUpcoming + courseUpcoming)
                .sortedWith(compareBy({ it.triggerMillis }, { it.dueMillis }, { it.title }))
            val groups = upcoming
                .groupBy { Instant.ofEpochMilli(it.triggerMillis).atZone(zone).toLocalDate() }
                .toSortedMap()
                .map { (date, entries) -> ReminderDayGroup(date, entries) }
            ReminderManageUiState(
                loaded = true,
                notificationsEnabled = true,
                rangeStart = lo,
                rangeEnd = hi,
                groups = groups,
                totalCount = upcoming.size,
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ReminderManageUiState(loaded = false),
    )

    init {
        viewModelScope.launch {
            scheduleRepository.ensureLoaded()
            settingsRepository.ensureLoaded()
            courseRepository.ensureLoaded()
        }
    }

    fun setRangeStart(date: LocalDate) {
        rangeStart.value = date
        rangeEnd.update { end -> if (end.isBefore(date)) date else end }
    }

    fun setRangeEnd(date: LocalDate) {
        rangeEnd.value = date
        rangeStart.update { start -> if (start.isAfter(date)) date else start }
    }

    fun resetRangeToDefault() {
        val (start, end) = defaultRange()
        rangeStart.value = start
        rangeEnd.value = end
    }

    companion object {
        fun factory(
            scheduleRepository: ScheduleRepository,
            settingsRepository: SettingsRepository,
            courseRepository: CourseRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ReminderManageViewModel(
                    scheduleRepository,
                    settingsRepository,
                    courseRepository,
                ) as T
            }
        }
    }
}

package com.mydailylife.schedule.ui.screens.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydailylife.schedule.data.ScheduleRepository
import com.mydailylife.schedule.data.SettingsRepository
import com.mydailylife.schedule.reminder.ReminderTimes
import com.mydailylife.schedule.reminder.UpcomingReminder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ReminderDayGroup(
    val date: LocalDate,
    val entries: List<UpcomingReminder>,
)

data class ReminderManageUiState(
    val loaded: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val groups: List<ReminderDayGroup> = emptyList(),
    val totalCount: Int = 0,
)

class ReminderManageViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val zone: ZoneId = ZoneId.systemDefault()

    val uiState: StateFlow<ReminderManageUiState> = combine(
        scheduleRepository.schedules,
        settingsRepository.settings,
    ) { schedules, settings ->
        if (!settings.notificationsEnabled) {
            ReminderManageUiState(
                loaded = true,
                notificationsEnabled = false,
            )
        } else {
            val now = System.currentTimeMillis()
            val upcoming = ReminderTimes.listUpcoming(schedules, now, zone)
            val groups = upcoming
                .groupBy { Instant.ofEpochMilli(it.triggerMillis).atZone(zone).toLocalDate() }
                .toSortedMap()
                .map { (date, entries) -> ReminderDayGroup(date, entries) }
            ReminderManageUiState(
                loaded = true,
                notificationsEnabled = true,
                groups = groups,
                totalCount = upcoming.size,
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ReminderManageUiState(),
    )

    init {
        viewModelScope.launch {
            scheduleRepository.ensureLoaded()
            settingsRepository.ensureLoaded()
        }
    }

    companion object {
        fun factory(
            scheduleRepository: ScheduleRepository,
            settingsRepository: SettingsRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ReminderManageViewModel(scheduleRepository, settingsRepository) as T
            }
        }
    }
}

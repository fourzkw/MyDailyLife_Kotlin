package com.mydailylife.schedule.ui.screens.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydailylife.schedule.data.Priority
import com.mydailylife.schedule.data.ScheduleItem
import com.mydailylife.schedule.data.ScheduleRepository
import com.mydailylife.schedule.data.ScheduleTimeMode
import com.mydailylife.schedule.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

data class CreateUiState(
    val editId: String? = null,
    val title: String = "",
    val description: String = "",
    val timeMode: ScheduleTimeMode = ScheduleTimeMode.Once,
    val weekdays: Set<Int> = emptySet(),
    /** Used by Once (with [startTime]). */
    val startDate: LocalDate? = null,
    /** Used by Once (with [endTime]). May differ from [startDate] for multi-day. */
    val endDate: LocalDate? = null,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val priority: Priority = Priority.Medium,
    val tagsText: String = "",
    val presetTags: List<String> = emptyList(),
    val reminderEnabled: Boolean = true,
    val reminderBeforeMinutes: Int = 15,
    val remindAtStart: Boolean = true,
    val remindAtEnd: Boolean = true,
    val loaded: Boolean = false,
    val saved: Boolean = false,
    val deleted: Boolean = false,
    val error: String? = null,
) {
    val isEditing: Boolean get() = editId != null
    val screenTitle: String get() = if (isEditing) "编辑事项" else "创建事项"
    val showWeekdays: Boolean get() = timeMode == ScheduleTimeMode.Weekly
    /** Once: one field each for start/end, each holding date + clock time. */
    val showDateTimePickers: Boolean get() = timeMode == ScheduleTimeMode.Once
    /** Clock-only for daily/weekly. */
    val showTimeOfDayPickers: Boolean
        get() = timeMode == ScheduleTimeMode.Daily || timeMode == ScheduleTimeMode.Weekly
}

class CreateViewModel(
    private val repository: ScheduleRepository,
    private val settingsRepository: SettingsRepository,
    private val editId: String?,
) : ViewModel() {
    private val zone: ZoneId = ZoneId.systemDefault()
    private val _uiState = MutableStateFlow(CreateUiState(editId = editId))
    val uiState: StateFlow<CreateUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureLoaded()
            settingsRepository.ensureLoaded()
            val settings = settingsRepository.settings.value
            if (editId != null) {
                val existing = repository.getById(editId)
                if (existing != null) {
                    _uiState.value = fromItem(existing).copy(presetTags = settings.presetTags)
                } else {
                    _uiState.update {
                        it.copy(
                            loaded = true,
                            error = "事项不存在",
                            presetTags = settings.presetTags,
                        )
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        loaded = true,
                        priority = settings.defaultPriorityEnum,
                        reminderEnabled = settings.notificationsEnabled,
                        reminderBeforeMinutes = settings.reminderBeforeMinutes,
                        remindAtStart = true,
                        remindAtEnd = true,
                        presetTags = settings.presetTags,
                    )
                }
            }
        }
    }

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }

    fun onTimeModeChange(value: ScheduleTimeMode) = _uiState.update { state ->
        when (value) {
            ScheduleTimeMode.Unlimited -> state.copy(
                timeMode = value,
                startDate = null,
                endDate = null,
                startTime = null,
                endTime = null,
                weekdays = emptySet(),
            )
            ScheduleTimeMode.Daily, ScheduleTimeMode.Weekly -> state.copy(
                timeMode = value,
                startDate = null,
                endDate = null,
                weekdays = when {
                    value != ScheduleTimeMode.Weekly -> emptySet()
                    state.weekdays.isEmpty() -> setOf(DayOfWeek.MONDAY.value)
                    else -> state.weekdays
                },
            )
            ScheduleTimeMode.Once -> state.copy(
                timeMode = value,
                weekdays = emptySet(),
            )
        }
    }

    fun onWeekdayToggle(isoDay: Int) = _uiState.update { state ->
        val next = state.weekdays.toMutableSet()
        if (isoDay in next) {
            if (next.size > 1) next.remove(isoDay)
        } else {
            next.add(isoDay)
        }
        state.copy(weekdays = next)
    }

    fun onStartDateChange(value: LocalDate?) = _uiState.update { it.copy(startDate = value) }
    fun onEndDateChange(value: LocalDate?) = _uiState.update { it.copy(endDate = value) }
    fun onStartTimeChange(value: LocalTime?) = _uiState.update { it.copy(startTime = value) }
    fun onEndTimeChange(value: LocalTime?) = _uiState.update { it.copy(endTime = value) }

    /** Once mode: set or clear start as a single date+time pair. */
    fun onStartDateTimeChange(date: LocalDate?, time: LocalTime?) = _uiState.update {
        it.copy(startDate = date, startTime = time)
    }

    /** Once mode: set or clear end as a single date+time pair. */
    fun onEndDateTimeChange(date: LocalDate?, time: LocalTime?) = _uiState.update {
        it.copy(endDate = date, endTime = time)
    }
    fun onPriorityChange(value: Priority) = _uiState.update { it.copy(priority = value) }
    fun onTagsChange(value: String) = _uiState.update { it.copy(tagsText = value) }

    fun onPresetTagClick(tag: String) = _uiState.update { state ->
        val current = state.tagsText
            .split(',', '，', ';', '；', ' ')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toMutableList()
        if (tag in current) current.remove(tag) else current.add(tag)
        state.copy(tagsText = current.joinToString(", "))
    }

    fun onReminderChange(value: Boolean) = _uiState.update { it.copy(reminderEnabled = value) }
    fun onReminderBeforeMinutesChange(minutes: Int) =
        _uiState.update { it.copy(reminderBeforeMinutes = minutes) }
    fun onRemindAtStartChange(value: Boolean) = _uiState.update { it.copy(remindAtStart = value) }
    fun onRemindAtEndChange(value: Boolean) = _uiState.update { it.copy(remindAtEnd = value) }

    fun save() {
        val state = _uiState.value
        val title = state.title.trim()
        if (title.isEmpty()) {
            _uiState.update { it.copy(error = "请输入标题") }
            return
        }
        if (state.timeMode == ScheduleTimeMode.Weekly && state.weekdays.isEmpty()) {
            _uiState.update { it.copy(error = "请至少选择一个星期") }
            return
        }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = state.editId?.let { repository.getById(it) }
            val (startMillis, endMillis) = runCatching {
                resolveMillis(state)
            }.getOrElse { e ->
                _uiState.update { it.copy(error = e.message ?: "时间无效") }
                return@launch
            }

            val tags = state.tagsText
                .split(',', '，', ';', '；', ' ')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()

            val keepExcluded = state.timeMode == ScheduleTimeMode.Daily ||
                state.timeMode == ScheduleTimeMode.Weekly

            val item = ScheduleItem(
                id = existing?.id ?: UUID.randomUUID().toString(),
                title = title,
                description = state.description.trim(),
                priority = state.priority.storageKey,
                tags = tags,
                startTimeMillis = startMillis,
                endTimeMillis = endMillis,
                completed = existing?.completed ?: false,
                reminderEnabled = state.timeMode != ScheduleTimeMode.Unlimited &&
                    state.reminderEnabled,
                reminderBeforeMinutes = state.reminderBeforeMinutes,
                remindAtStart = state.remindAtStart,
                remindAtEnd = state.remindAtEnd,
                timeMode = state.timeMode.storageKey,
                weekdays = if (state.timeMode == ScheduleTimeMode.Weekly) {
                    state.weekdays.sorted()
                } else {
                    emptyList()
                },
                excludedDates = if (keepExcluded) {
                    existing?.excludedDates.orEmpty()
                } else {
                    emptyList()
                },
                createdAtMillis = existing?.createdAtMillis ?: now,
                updatedAtMillis = now,
            )
            repository.upsert(item)
            _uiState.update { it.copy(saved = true, error = null) }
        }
    }

    fun consumeError() = _uiState.update { it.copy(error = null) }

    fun delete() {
        val id = _uiState.value.editId ?: return
        viewModelScope.launch {
            repository.delete(id)
            _uiState.update { it.copy(deleted = true, error = null) }
        }
    }

    private fun fromItem(existing: ScheduleItem): CreateUiState {
        val start = millisToDateTime(existing.startTimeMillis)
        val end = millisToDateTime(existing.endTimeMillis)
        val common = CreateUiState(
            editId = existing.id,
            title = existing.title,
            description = existing.description,
            priority = existing.priorityEnum,
            tagsText = existing.tags.joinToString(", "),
            reminderEnabled = existing.reminderEnabled,
            reminderBeforeMinutes = existing.reminderBeforeMinutes,
            remindAtStart = existing.remindAtStart,
            remindAtEnd = existing.remindAtEnd,
            loaded = true,
        )
        return when (existing.timeModeEnum) {
            ScheduleTimeMode.Unlimited -> common.copy(timeMode = ScheduleTimeMode.Unlimited)
            ScheduleTimeMode.Daily, ScheduleTimeMode.Weekly -> common.copy(
                timeMode = existing.timeModeEnum,
                weekdays = existing.weekdays.toSet(),
                startTime = start?.second,
                endTime = end?.second,
            )
            ScheduleTimeMode.Once -> common.copy(
                timeMode = ScheduleTimeMode.Once,
                startDate = start?.first,
                endDate = end?.first,
                startTime = start?.second,
                endTime = end?.second,
            )
        }
    }

    private fun resolveMillis(state: CreateUiState): Pair<Long, Long> {
        return when (state.timeMode) {
            ScheduleTimeMode.Unlimited -> 0L to 0L
            ScheduleTimeMode.Daily, ScheduleTimeMode.Weekly -> {
                // Anchor date only carries time-of-day; recurrence ignores calendar day.
                val anchor = TimeOfDayAnchor
                val start = state.startTime?.let { combine(anchor, it) } ?: 0L
                val end = state.endTime?.let { combine(anchor, it) } ?: 0L
                if (start > 0L && end > 0L && end < start) {
                    error("结束时刻不能早于开始时刻")
                }
                start to end
            }
            ScheduleTimeMode.Once -> {
                val startDate = state.startDate
                val startTime = state.startTime
                val endDate = state.endDate
                val endTime = state.endTime
                if (startDate == null && endDate == null && startTime == null && endTime == null) {
                    return 0L to 0L
                }
                if ((startDate == null) != (startTime == null)) {
                    error("开始时间需同时包含日期与时刻")
                }
                if ((endDate == null) != (endTime == null)) {
                    error("结束时间需同时包含日期与时刻")
                }
                val start = if (startDate != null && startTime != null) {
                    combine(startDate, startTime)
                } else {
                    0L
                }
                val end = if (endDate != null && endTime != null) {
                    combine(endDate, endTime)
                } else {
                    0L
                }
                if (start > 0L && end > 0L && end < start) {
                    error("结束时间不能早于开始时间")
                }
                start to end
            }
        }
    }

    private fun combine(date: LocalDate, time: LocalTime): Long =
        date.atTime(time).atZone(zone).toInstant().toEpochMilli()

    private fun millisToDateTime(millis: Long): Pair<LocalDate, LocalTime>? {
        if (millis <= 0L) return null
        val zdt = Instant.ofEpochMilli(millis).atZone(zone)
        return zdt.toLocalDate() to zdt.toLocalTime().withSecond(0).withNano(0)
    }

    companion object {
        /** Stored with daily/weekly times so date is ignored by recurrence window. */
        val TimeOfDayAnchor: LocalDate = LocalDate.of(2000, 1, 1)

        fun factory(
            repository: ScheduleRepository,
            settingsRepository: SettingsRepository,
            editId: String?,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CreateViewModel(repository, settingsRepository, editId) as T
            }
        }
    }
}

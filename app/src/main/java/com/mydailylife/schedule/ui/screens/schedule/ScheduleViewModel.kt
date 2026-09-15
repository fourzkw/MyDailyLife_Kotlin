package com.mydailylife.schedule.ui.screens.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydailylife.schedule.data.Priority
import com.mydailylife.schedule.data.ScheduleItem
import com.mydailylife.schedule.data.ScheduleQuery
import com.mydailylife.schedule.data.ScheduleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

data class ScheduleUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val month: YearMonth = YearMonth.now(),
    val monthExpanded: Boolean = false,
    val weekDates: List<LocalDate> = emptyList(),
    val dayDots: Map<LocalDate, List<Priority>> = emptyMap(),
    val pendingItems: List<ScheduleItem> = emptyList(),
    val completedItems: List<ScheduleItem> = emptyList(),
    val completedExpanded: Boolean = false,
    val query: String = "",
    val message: String? = null,
)

class ScheduleViewModel(
    private val repository: ScheduleRepository,
) : ViewModel() {
    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val monthExpanded = MutableStateFlow(false)
    private val completedExpanded = MutableStateFlow(false)
    private val query = MutableStateFlow("")
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ScheduleUiState> = combine(
        repository.schedules,
        selectedDate,
        monthExpanded,
        query,
        combine(message, completedExpanded) { msg, completedExp -> msg to completedExp },
    ) { schedules, date, expanded, q, msgAndCompleted ->
        val (msg, completedExp) = msgAndCompleted
        val month = YearMonth.from(date)
        val week = weekOf(date)
        val keyword = q.trim()
        val dayItems = ScheduleQuery.itemsForDate(schedules, date).filter { item ->
            if (keyword.isEmpty()) true
            else {
                item.title.contains(keyword, ignoreCase = true) ||
                    item.description.contains(keyword, ignoreCase = true)
            }
        }
        val dots = buildMap {
            putAll(ScheduleQuery.dayPrioritiesInMonth(schedules, month))
            week.forEach { weekDay ->
                val weekMonth = YearMonth.from(weekDay)
                if (weekMonth != month && !containsKey(weekDay)) {
                    putAll(ScheduleQuery.dayPrioritiesInMonth(schedules, weekMonth))
                }
            }
        }
        ScheduleUiState(
            selectedDate = date,
            month = month,
            monthExpanded = expanded,
            weekDates = week,
            dayDots = dots,
            pendingItems = dayItems.filter { !it.completed },
            completedItems = dayItems.filter { it.completed },
            completedExpanded = completedExp,
            query = q,
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScheduleUiState(weekDates = weekOf(LocalDate.now())))

    init {
        viewModelScope.launch { repository.ensureLoaded() }
    }

    fun toggleMonthExpanded() {
        monthExpanded.value = !monthExpanded.value
    }

    fun expandMonth() {
        monthExpanded.value = true
    }

    fun collapseMonth() {
        monthExpanded.value = false
    }

    fun toggleCompletedExpanded() {
        completedExpanded.value = !completedExpanded.value
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }

    fun shiftDay(days: Long) {
        selectedDate.value = selectedDate.value.plusDays(days)
    }

    fun previousPeriod() {
        if (monthExpanded.value) {
            selectedDate.value = selectedDate.value.minusMonths(1).withDayOfMonth(
                minOf(selectedDate.value.dayOfMonth, selectedDate.value.minusMonths(1).lengthOfMonth()),
            )
        } else {
            selectedDate.value = selectedDate.value.minusWeeks(1)
        }
    }

    fun nextPeriod() {
        if (monthExpanded.value) {
            selectedDate.value = selectedDate.value.plusMonths(1).withDayOfMonth(
                minOf(selectedDate.value.dayOfMonth, selectedDate.value.plusMonths(1).lengthOfMonth()),
            )
        } else {
            selectedDate.value = selectedDate.value.plusWeeks(1)
        }
    }

    fun goToday() {
        selectedDate.value = LocalDate.now()
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun toggleCompleted(id: String) {
        viewModelScope.launch {
            repository.toggleCompleted(id)
            message.value = "状态已更新"
        }
    }

    fun consumeMessage() {
        message.value = null
    }

    companion object {
        fun weekOf(date: LocalDate): List<LocalDate> {
            val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            return (0..6).map { monday.plusDays(it.toLong()) }
        }

        fun factory(repository: ScheduleRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ScheduleViewModel(repository) as T
                }
            }
    }
}

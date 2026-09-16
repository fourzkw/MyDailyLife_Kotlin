package com.mydailylife.schedule.ui.screens.calendar

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
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val dayDots: Map<LocalDate, List<Priority>> = emptyMap(),
    val selectedItems: List<ScheduleItem> = emptyList(),
)

class CalendarViewModel(
    private val repository: ScheduleRepository,
) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now())
    private val selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<CalendarUiState> = combine(
        repository.schedules,
        month,
        selectedDate,
    ) { schedules, currentMonth, selected ->
        CalendarUiState(
            month = currentMonth,
            selectedDate = selected,
            dayDots = ScheduleQuery.dayPrioritiesInMonth(schedules, currentMonth),
            selectedItems = ScheduleQuery.itemsForDate(schedules, selected),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    init {
        viewModelScope.launch { repository.ensureLoaded() }
    }

    fun previousMonth() {
        month.value = month.value.minusMonths(1)
    }

    fun nextMonth() {
        month.value = month.value.plusMonths(1)
    }

    fun goToday() {
        val today = LocalDate.now()
        month.value = YearMonth.from(today)
        selectedDate.value = today
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
        month.value = YearMonth.from(date)
    }

    fun toggleCompleted(id: String) {
        viewModelScope.launch { repository.toggleCompleted(id) }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun deleteOccurrence(id: String, date: LocalDate) {
        viewModelScope.launch { repository.excludeOccurrence(id, date) }
    }

    companion object {
        fun factory(repository: ScheduleRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CalendarViewModel(repository) as T
                }
            }
    }
}

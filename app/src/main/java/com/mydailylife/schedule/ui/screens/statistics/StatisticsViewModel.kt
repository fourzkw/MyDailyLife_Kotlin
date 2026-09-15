package com.mydailylife.schedule.ui.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydailylife.schedule.data.ScheduleQuery
import com.mydailylife.schedule.data.ScheduleRepository
import com.mydailylife.schedule.data.ScheduleStatistics
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StatisticsUiState(
    val stats: ScheduleStatistics = ScheduleStatistics(),
    val cleared: Boolean = false,
)

class StatisticsViewModel(
    private val repository: ScheduleRepository,
) : ViewModel() {
    val uiState: StateFlow<StatisticsUiState> = repository.schedules
        .map { StatisticsUiState(stats = ScheduleQuery.statistics(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState())

    init {
        viewModelScope.launch { repository.ensureLoaded() }
    }

    fun resetAllSchedules() {
        viewModelScope.launch { repository.clearAll() }
    }

    companion object {
        fun factory(repository: ScheduleRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StatisticsViewModel(repository) as T
                }
            }
    }
}

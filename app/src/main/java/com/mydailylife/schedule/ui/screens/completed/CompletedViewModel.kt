package com.mydailylife.schedule.ui.screens.completed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydailylife.schedule.data.ScheduleItem
import com.mydailylife.schedule.data.ScheduleQuery
import com.mydailylife.schedule.data.ScheduleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CompletedUiState(
    val items: List<ScheduleItem> = emptyList(),
)

class CompletedViewModel(
    private val repository: ScheduleRepository,
) : ViewModel() {
    val uiState: StateFlow<CompletedUiState> = repository.schedules
        .map { CompletedUiState(items = ScheduleQuery.completed(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CompletedUiState())

    init {
        viewModelScope.launch { repository.ensureLoaded() }
    }

    fun clearCompleted() {
        viewModelScope.launch { repository.clearCompleted() }
    }

    fun toggleCompleted(id: String) {
        viewModelScope.launch { repository.toggleCompleted(id) }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    companion object {
        fun factory(repository: ScheduleRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CompletedViewModel(repository) as T
                }
            }
    }
}

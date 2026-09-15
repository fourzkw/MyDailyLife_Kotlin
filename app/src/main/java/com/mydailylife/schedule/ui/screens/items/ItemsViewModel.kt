package com.mydailylife.schedule.ui.screens.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydailylife.schedule.data.ItemsFilterChips
import com.mydailylife.schedule.data.ScheduleItem
import com.mydailylife.schedule.data.ScheduleQuery
import com.mydailylife.schedule.data.ScheduleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ItemsUiState(
    val items: List<ScheduleItem> = emptyList(),
    val query: String = "",
    val selectedFilter: String = "全部",
    val filterChips: List<String> = ItemsFilterChips,
    val message: String? = null,
)

class ItemsViewModel(
    private val repository: ScheduleRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val selectedFilter = MutableStateFlow("全部")
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ItemsUiState> = combine(
        repository.schedules,
        query,
        selectedFilter,
        message,
    ) { schedules, q, filter, msg ->
        ItemsUiState(
            items = ScheduleQuery.pendingVisible(schedules, q, filter),
            query = q,
            selectedFilter = filter,
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ItemsUiState())

    init {
        viewModelScope.launch { repository.ensureLoaded() }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onFilterSelected(value: String) {
        selectedFilter.value = value
    }

    fun toggleCompleted(id: String) {
        viewModelScope.launch {
            repository.toggleCompleted(id)
            message.value = "已标记完成"
        }
    }

    fun consumeMessage() {
        message.value = null
    }

    companion object {
        fun factory(repository: ScheduleRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ItemsViewModel(repository) as T
                }
            }
    }
}

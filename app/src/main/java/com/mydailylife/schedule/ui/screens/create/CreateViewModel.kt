package com.mydailylife.schedule.ui.screens.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydailylife.schedule.data.Priority
import com.mydailylife.schedule.data.ScheduleItem
import com.mydailylife.schedule.data.ScheduleRepository
import com.mydailylife.schedule.data.ScheduleTimeFormat
import com.mydailylife.schedule.data.ScheduleType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class CreateUiState(
    val editId: String? = null,
    val title: String = "",
    val description: String = "",
    val type: ScheduleType = ScheduleType.Schedule,
    val startTimeText: String = "",
    val endTimeText: String = "",
    val priority: Priority = Priority.Medium,
    val tagsText: String = "",
    val reminderEnabled: Boolean = true,
    val loaded: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
) {
    val isEditing: Boolean get() = editId != null
    val screenTitle: String get() = if (isEditing) "编辑日程" else "创建日程"
}

class CreateViewModel(
    private val repository: ScheduleRepository,
    private val editId: String?,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateUiState(editId = editId))
    val uiState: StateFlow<CreateUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureLoaded()
            if (editId != null) {
                val existing = repository.getById(editId)
                if (existing != null) {
                    _uiState.value = CreateUiState(
                        editId = existing.id,
                        title = existing.title,
                        description = existing.description,
                        type = existing.typeEnum,
                        startTimeText = ScheduleTimeFormat.formatEditable(existing.startTimeMillis),
                        endTimeText = ScheduleTimeFormat.formatEditable(existing.endTimeMillis),
                        priority = existing.priorityEnum,
                        tagsText = existing.tags.joinToString(", "),
                        reminderEnabled = existing.reminderEnabled,
                        loaded = true,
                    )
                } else {
                    _uiState.update { it.copy(loaded = true, error = "事项不存在") }
                }
            } else {
                val now = System.currentTimeMillis()
                val inOneHour = now + 60 * 60 * 1000L
                _uiState.update {
                    it.copy(
                        startTimeText = ScheduleTimeFormat.formatEditable(now),
                        endTimeText = ScheduleTimeFormat.formatEditable(inOneHour),
                        loaded = true,
                    )
                }
            }
        }
    }

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }
    fun onTypeChange(value: ScheduleType) = _uiState.update { it.copy(type = value) }
    fun onStartTimeChange(value: String) = _uiState.update { it.copy(startTimeText = value) }
    fun onEndTimeChange(value: String) = _uiState.update { it.copy(endTimeText = value) }
    fun onPriorityChange(value: Priority) = _uiState.update { it.copy(priority = value) }
    fun onTagsChange(value: String) = _uiState.update { it.copy(tagsText = value) }
    fun onReminderChange(value: Boolean) = _uiState.update { it.copy(reminderEnabled = value) }

    fun save() {
        val state = _uiState.value
        val title = state.title.trim()
        if (title.isEmpty()) {
            _uiState.update { it.copy(error = "请输入标题") }
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = state.editId?.let { repository.getById(it) }
            val startMillis = if (state.type == ScheduleType.Task) {
                0L
            } else {
                ScheduleTimeFormat.parseEditable(state.startTimeText, now)
            }
            val endMillis = if (state.type == ScheduleType.Task) {
                ScheduleTimeFormat.parseEditable(
                    state.endTimeText.ifBlank { state.startTimeText },
                    now + 7L * 24 * 60 * 60 * 1000,
                )
            } else {
                ScheduleTimeFormat.parseEditable(state.endTimeText, startMillis + 60 * 60 * 1000L)
            }
            val tags = state.tagsText
                .split(',', '，', ';', '；', ' ')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()

            val item = ScheduleItem(
                id = existing?.id ?: UUID.randomUUID().toString(),
                title = title,
                description = state.description.trim(),
                type = state.type.storageKey,
                priority = state.priority.storageKey,
                tags = tags,
                startTimeMillis = startMillis,
                endTimeMillis = endMillis,
                completed = existing?.completed ?: false,
                reminderEnabled = state.reminderEnabled,
                reminderBeforeMinutes = existing?.reminderBeforeMinutes ?: 15,
                repeat = existing?.repeat ?: "none",
                createdAtMillis = existing?.createdAtMillis ?: now,
                updatedAtMillis = now,
            )
            repository.upsert(item)
            _uiState.update { it.copy(saved = true, error = null) }
        }
    }

    fun consumeError() = _uiState.update { it.copy(error = null) }

    companion object {
        fun factory(
            repository: ScheduleRepository,
            editId: String?,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CreateViewModel(repository, editId) as T
            }
        }
    }
}

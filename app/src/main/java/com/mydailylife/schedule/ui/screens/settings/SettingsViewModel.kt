package com.mydailylife.schedule.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydailylife.schedule.data.AppSettings
import com.mydailylife.schedule.data.Priority
import com.mydailylife.schedule.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val loaded: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val reminderBeforeMinutes: Int = 15,
    val defaultPriority: Priority = Priority.Medium,
    val presetTags: List<String> = AppSettings.DefaultPresetTags,
    val courseAutoUpdate: Boolean = false,
) {
    val reminderLabel: String get() = AppSettings.reminderLabel(reminderBeforeMinutes)
    val priorityLabel: String get() = defaultPriority.label
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = settingsRepository.settings
        .map { settings ->
            SettingsUiState(
                loaded = true,
                notificationsEnabled = settings.notificationsEnabled,
                reminderBeforeMinutes = settings.reminderBeforeMinutes,
                defaultPriority = settings.defaultPriorityEnum,
                presetTags = settings.presetTags,
                courseAutoUpdate = settings.courseAutoUpdate,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    init {
        viewModelScope.launch { settingsRepository.ensureLoaded() }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNotificationsEnabled(enabled) }
    }

    fun setReminderBeforeMinutes(minutes: Int) {
        viewModelScope.launch { settingsRepository.setReminderBeforeMinutes(minutes) }
    }

    fun setDefaultPriority(priority: Priority) {
        viewModelScope.launch { settingsRepository.setDefaultPriority(priority) }
    }

    fun setCourseAutoUpdate(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setCourseAutoUpdate(enabled) }
    }

    fun addTag(tag: String) {
        viewModelScope.launch { settingsRepository.addTag(tag) }
    }

    fun removeTag(tag: String) {
        viewModelScope.launch { settingsRepository.removeTag(tag) }
    }

    companion object {
        fun factory(settingsRepository: SettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(settingsRepository) as T
                }
            }
    }
}

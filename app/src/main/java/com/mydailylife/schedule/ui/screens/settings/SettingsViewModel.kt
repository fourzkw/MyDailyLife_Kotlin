package com.mydailylife.schedule.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydailylife.schedule.data.AppSettings
import com.mydailylife.schedule.data.Priority
import com.mydailylife.schedule.data.SettingsRepository
import com.mydailylife.schedule.data.update.AppUpdater
import com.mydailylife.schedule.data.update.UpdateCheckResult
import com.mydailylife.schedule.data.update.UpdateManifest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed class AppUpdateUiState {
    data object Idle : AppUpdateUiState()
    data object Checking : AppUpdateUiState()
    data object UpToDate : AppUpdateUiState()
    data class Available(val manifest: UpdateManifest) : AppUpdateUiState()
    data class Downloading(
        val manifest: UpdateManifest,
        val progress: Float,
    ) : AppUpdateUiState()
    data class Ready(
        val manifest: UpdateManifest,
        val apkFile: File,
    ) : AppUpdateUiState()
    data class Error(val message: String) : AppUpdateUiState()
    data class NeedsInstallPermission(
        val manifest: UpdateManifest,
        val apkFile: File,
    ) : AppUpdateUiState()
}

data class SettingsUiState(
    val loaded: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val reminderBeforeMinutes: Int = 15,
    val defaultPriority: Priority = Priority.Medium,
    val presetTags: List<String> = AppSettings.DefaultPresetTags,
    val courseAutoUpdate: Boolean = false,
    val update: AppUpdateUiState = AppUpdateUiState.Idle,
) {
    val reminderLabel: String get() = AppSettings.reminderLabel(reminderBeforeMinutes)
    val priorityLabel: String get() = defaultPriority.label
}

class SettingsViewModel(
    application: Application,
    private val settingsRepository: SettingsRepository,
) : AndroidViewModel(application) {
    private val updater = AppUpdater(application)

    private val _updateState = MutableStateFlow<AppUpdateUiState>(AppUpdateUiState.Idle)
    val updateState: StateFlow<AppUpdateUiState> = _updateState.asStateFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        _updateState,
    ) { settings, update ->
        SettingsUiState(
            loaded = true,
            notificationsEnabled = settings.notificationsEnabled,
            reminderBeforeMinutes = settings.reminderBeforeMinutes,
            defaultPriority = settings.defaultPriorityEnum,
            presetTags = settings.presetTags,
            courseAutoUpdate = settings.courseAutoUpdate,
            update = update,
        )
    }.stateIn(
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

    fun checkForUpdate() {
        val current = _updateState.value
        if (current is AppUpdateUiState.Checking || current is AppUpdateUiState.Downloading) return
        viewModelScope.launch {
            _updateState.value = AppUpdateUiState.Checking
            runCatching { updater.checkForUpdate() }
                .onSuccess { result ->
                    _updateState.value = when (result) {
                        UpdateCheckResult.UpToDate -> AppUpdateUiState.UpToDate
                        is UpdateCheckResult.Available -> AppUpdateUiState.Available(result.manifest)
                    }
                }
                .onFailure { e ->
                    _updateState.value = AppUpdateUiState.Error(e.message ?: "检查更新失败")
                }
        }
    }

    fun downloadAndInstall() {
        val manifest = when (val state = _updateState.value) {
            is AppUpdateUiState.Available -> state.manifest
            is AppUpdateUiState.Ready -> {
                proceedInstall(state.manifest, state.apkFile)
                return
            }
            is AppUpdateUiState.NeedsInstallPermission -> {
                proceedInstall(state.manifest, state.apkFile)
                return
            }
            else -> return
        }
        viewModelScope.launch {
            _updateState.value = AppUpdateUiState.Downloading(manifest, 0f)
            runCatching {
                updater.downloadApk(manifest) { progress ->
                    _updateState.value = AppUpdateUiState.Downloading(manifest, progress)
                }
            }.onSuccess { file ->
                proceedInstall(manifest, file)
            }.onFailure { e ->
                _updateState.value = AppUpdateUiState.Error(e.message ?: "下载失败")
            }
        }
    }

    fun openInstallPermissionSettings() {
        getApplication<Application>().startActivity(updater.installPermissionSettingsIntent())
    }

    fun dismissUpdateMessage() {
        when (_updateState.value) {
            is AppUpdateUiState.UpToDate,
            is AppUpdateUiState.Error,
            is AppUpdateUiState.Available,
            -> _updateState.value = AppUpdateUiState.Idle
            else -> Unit
        }
    }

    private fun proceedInstall(manifest: UpdateManifest, apkFile: File) {
        if (!updater.canInstallPackages()) {
            _updateState.value = AppUpdateUiState.NeedsInstallPermission(manifest, apkFile)
            return
        }
        runCatching {
            updater.installApk(apkFile)
            _updateState.value = AppUpdateUiState.Ready(manifest, apkFile)
        }.onFailure { e ->
            _updateState.value = AppUpdateUiState.Error(e.message ?: "无法打开安装界面")
        }
    }

    companion object {
        fun factory(settingsRepository: SettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    error("Use create(modelClass, extras) with Application")
                }

                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: androidx.lifecycle.viewmodel.CreationExtras,
                ): T {
                    val app = checkNotNull(
                        extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY],
                    )
                    @Suppress("UNCHECKED_CAST")
                    return SettingsViewModel(app, settingsRepository) as T
                }
            }
    }
}

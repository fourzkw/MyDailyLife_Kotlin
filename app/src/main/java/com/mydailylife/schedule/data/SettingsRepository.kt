package com.mydailylife.schedule.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

class SettingsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, "settings.json")
    private val mutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private var loaded = false

    suspend fun ensureLoaded() {
        if (loaded) return
        mutex.withLock {
            if (loaded) return
            val fromDisk = withContext(Dispatchers.IO) { readFromDisk() }
            _settings.value = fromDisk ?: AppSettings()
            if (fromDisk == null) {
                writeToDisk(_settings.value)
            }
            loaded = true
        }
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        ensureLoaded()
        mutex.withLock {
            val next = transform(_settings.value)
            persistLocked(next)
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) =
        update { it.copy(notificationsEnabled = enabled) }

    suspend fun setReminderBeforeMinutes(minutes: Int) =
        update { it.copy(reminderBeforeMinutes = minutes) }

    suspend fun setDefaultPriority(priority: Priority) =
        update { it.copy(defaultPriority = priority.storageKey) }

    suspend fun setCourseIcsUrl(url: String) =
        update { it.copy(courseIcsUrl = url.trim()) }

    suspend fun setScheduleSortMode(mode: ScheduleSortMode) =
        update { it.copy(scheduleSortMode = mode.storageKey) }

    suspend fun addTag(tag: String) {
        val trimmed = tag.trim()
        if (trimmed.isEmpty()) return
        update { current ->
            if (trimmed in current.presetTags) current
            else current.copy(presetTags = current.presetTags + trimmed)
        }
    }

    suspend fun removeTag(tag: String) = update { current ->
        current.copy(presetTags = current.presetTags.filterNot { it == tag })
    }

    private fun persistLocked(value: AppSettings) {
        _settings.value = value
        writeToDisk(value)
    }

    private fun readFromDisk(): AppSettings? {
        if (!file.exists()) return null
        return runCatching {
            json.decodeFromString(AppSettings.serializer(), file.readText())
        }.getOrNull()
    }

    private fun writeToDisk(value: AppSettings) {
        file.writeText(json.encodeToString(AppSettings.serializer(), value))
    }
}

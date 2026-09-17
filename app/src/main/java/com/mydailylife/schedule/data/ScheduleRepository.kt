package com.mydailylife.schedule.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

class ScheduleRepository(context: Context) {
    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, "schedules.json")
    private val mutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val _schedules = MutableStateFlow<List<ScheduleItem>>(emptyList())
    val schedules: StateFlow<List<ScheduleItem>> = _schedules.asStateFlow()

    private var loaded = false

    suspend fun ensureLoaded() {
        if (loaded) return
        mutex.withLock {
            if (loaded) return
            val items = withContext(Dispatchers.IO) { readFromDisk() }
            _schedules.value = items
            loaded = true
        }
    }

    suspend fun getById(id: String): ScheduleItem? {
        ensureLoaded()
        return _schedules.value.find { it.id == id }
    }

    suspend fun upsert(item: ScheduleItem) {
        ensureLoaded()
        mutex.withLock {
            val current = _schedules.value.toMutableList()
            val index = current.indexOfFirst { it.id == item.id }
            if (index >= 0) current[index] = item else current.add(item)
            persistLocked(current)
        }
    }

    suspend fun delete(id: String) {
        ensureLoaded()
        mutex.withLock {
            persistLocked(_schedules.value.filterNot { it.id == id })
        }
    }

    /** Skip one day for a recurring item without removing the series. */
    suspend fun excludeOccurrence(id: String, date: java.time.LocalDate) {
        ensureLoaded()
        mutex.withLock {
            val key = date.toString()
            val updated = _schedules.value.map { item ->
                if (item.id != id) item
                else when (item.timeModeEnum) {
                    // Unlimited / Once: no per-day skip; caller should delete entire.
                    ScheduleTimeMode.Daily, ScheduleTimeMode.Weekly -> {
                        if (key in item.excludedDates) item
                        else item.copy(
                            excludedDates = item.excludedDates + key,
                            updatedAtMillis = System.currentTimeMillis(),
                        )
                    }
                    else -> item
                }
            }
            persistLocked(updated)
        }
    }

    suspend fun toggleCompleted(id: String) {
        ensureLoaded()
        mutex.withLock {
            val updated = _schedules.value.map { item ->
                if (item.id != id) item
                else item.copy(
                    completed = !item.completed,
                    updatedAtMillis = System.currentTimeMillis(),
                )
            }
            persistLocked(updated)
        }
    }

    suspend fun clearCompleted() {
        ensureLoaded()
        mutex.withLock {
            persistLocked(_schedules.value.filterNot { it.completed })
        }
    }

    suspend fun clearAll() {
        ensureLoaded()
        mutex.withLock {
            persistLocked(emptyList())
        }
    }

    private suspend fun persistLocked(items: List<ScheduleItem>) {
        withContext(Dispatchers.IO) { writeToDisk(items) }
        _schedules.value = items
    }

    private fun readFromDisk(): List<ScheduleItem> {
        if (!file.exists()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(ScheduleItem.serializer()), file.readText())
        }.getOrDefault(emptyList())
    }

    private fun writeToDisk(items: List<ScheduleItem>) {
        file.writeText(json.encodeToString(ListSerializer(ScheduleItem.serializer()), items))
    }
}

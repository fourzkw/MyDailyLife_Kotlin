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
            if (items.isEmpty()) {
                val seeded = sampleSchedules()
                writeToDisk(seeded)
                _schedules.value = seeded
            } else {
                _schedules.value = items
            }
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

    private fun sampleSchedules(): List<ScheduleItem> {
        val now = System.currentTimeMillis()
        val todayStart = ScheduleTimeFormat.startOfDayMillis()
        fun atHour(dayOffset: Long, hour: Int, minute: Int = 0): Long {
            return todayStart + dayOffset * 24 * 60 * 60 * 1000L +
                hour * 60 * 60 * 1000L + minute * 60 * 1000L
        }
        return listOf(
            ScheduleItem(
                id = "seed-1",
                title = "高等数学作业",
                description = "完成第三章习题 1-12",
                type = ScheduleType.Schedule.storageKey,
                priority = Priority.Urgent.storageKey,
                tags = listOf("学习", "作业"),
                startTimeMillis = atHour(0, 14),
                endTimeMillis = atHour(0, 16),
                reminderEnabled = true,
                createdAtMillis = now,
                updatedAtMillis = now,
            ),
            ScheduleItem(
                id = "seed-2",
                title = "项目组周会",
                description = "同步本周进度",
                priority = Priority.High.storageKey,
                tags = listOf("工作"),
                startTimeMillis = atHour(0, 10),
                endTimeMillis = atHour(0, 11),
                createdAtMillis = now,
                updatedAtMillis = now,
            ),
            ScheduleItem(
                id = "seed-3",
                title = "买生活用品",
                type = ScheduleType.Task.storageKey,
                priority = Priority.Low.storageKey,
                tags = listOf("生活"),
                endTimeMillis = atHour(7, 23, 59),
                createdAtMillis = now,
                updatedAtMillis = now,
            ),
            ScheduleItem(
                id = "seed-4",
                title = "英语口语练习",
                priority = Priority.Medium.storageKey,
                tags = listOf("学习"),
                startTimeMillis = atHour(1, 19),
                endTimeMillis = atHour(1, 20),
                createdAtMillis = now,
                updatedAtMillis = now,
            ),
            ScheduleItem(
                id = "seed-c1",
                title = "晨跑 5 公里",
                priority = Priority.Medium.storageKey,
                tags = listOf("健康"),
                startTimeMillis = atHour(-1, 7),
                endTimeMillis = atHour(-1, 8),
                completed = true,
                createdAtMillis = now,
                updatedAtMillis = now,
            ),
            ScheduleItem(
                id = "seed-c2",
                title = "提交实验报告",
                priority = Priority.High.storageKey,
                tags = listOf("学习"),
                startTimeMillis = atHour(-1, 21),
                endTimeMillis = atHour(-1, 22),
                completed = true,
                createdAtMillis = now,
                updatedAtMillis = now,
            ),
        )
    }
}

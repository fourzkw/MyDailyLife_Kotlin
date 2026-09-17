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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class CourseRepository(context: Context) {
    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, "courses.json")
    private val mutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val _store = MutableStateFlow(CourseStore())
    val store: StateFlow<CourseStore> = _store.asStateFlow()

    private var loaded = false

    suspend fun ensureLoaded() {
        if (loaded) return
        mutex.withLock {
            if (loaded) return
            val loadedStore = withContext(Dispatchers.IO) { readFromDisk() }
            _store.value = loadedStore
            loaded = true
        }
    }

    suspend fun replaceAll(
        items: List<CourseItem>,
        termStartDate: LocalDate? = null,
        maxTeachingWeek: Int? = null,
    ) {
        ensureLoaded()
        mutex.withLock {
            val inferredMax = maxTeachingWeek
                ?: items.flatMap { it.teachingWeeks }.maxOrNull()
                ?: _store.value.maxTeachingWeek
            val term = termStartDate?.toString() ?: _store.value.termStartDate
            persistLocked(
                CourseStore(
                    courses = items,
                    termStartDate = term,
                    maxTeachingWeek = inferredMax.coerceIn(1, 30),
                ),
            )
        }
    }

    suspend fun setTermStartDate(date: LocalDate?) {
        ensureLoaded()
        mutex.withLock {
            persistLocked(_store.value.copy(termStartDate = date?.toString()))
        }
    }

    suspend fun clearAll() {
        ensureLoaded()
        mutex.withLock {
            persistLocked(CourseStore())
        }
    }

    private suspend fun persistLocked(store: CourseStore) {
        withContext(Dispatchers.IO) { writeToDisk(store) }
        _store.value = store
    }

    private fun readFromDisk(): CourseStore {
        if (!file.exists()) return CourseStore()
        val text = file.readText()
        // New format
        runCatching {
            json.decodeFromString(CourseStore.serializer(), text)
        }.getOrNull()?.let { return it }
        // Legacy bare list
        val legacy = runCatching {
            json.decodeFromString(ListSerializer(CourseItem.serializer()), text)
        }.getOrDefault(emptyList())
        return CourseStore(courses = legacy)
    }

    private fun writeToDisk(store: CourseStore) {
        file.writeText(json.encodeToString(CourseStore.serializer(), store))
    }

    companion object {
        fun todayMonday(): LocalDate =
            LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
}

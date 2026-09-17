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
                    periodSchedule = _store.value.periodSchedule,
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

    suspend fun setPeriodSchedule(schedule: CoursePeriodSchedule) {
        ensureLoaded()
        mutex.withLock {
            persistLocked(_store.value.copy(periodSchedule = schedule))
        }
    }

    suspend fun clearAll() {
        ensureLoaded()
        mutex.withLock {
            persistLocked(
                CourseStore(
                    termStartDate = _store.value.termStartDate,
                    periodSchedule = _store.value.periodSchedule,
                ),
            )
        }
    }

    suspend fun addCourse(course: CourseItem) {
        ensureLoaded()
        mutex.withLock {
            val next = _store.value.courses + course
            val maxWeek = next.flatMap { it.teachingWeeks }.maxOrNull()
                ?.coerceAtLeast(_store.value.maxTeachingWeek)
                ?: _store.value.maxTeachingWeek
            persistLocked(
                _store.value.copy(
                    courses = next,
                    maxTeachingWeek = maxWeek.coerceIn(1, 30),
                ),
            )
        }
    }

    suspend fun updateCourse(course: CourseItem) {
        ensureLoaded()
        mutex.withLock {
            val next = _store.value.courses.map { if (it.id == course.id) course else it }
            persistLocked(_store.value.copy(courses = next))
        }
    }

    suspend fun removeCourse(id: String) {
        ensureLoaded()
        mutex.withLock {
            persistLocked(_store.value.copy(courses = _store.value.courses.filterNot { it.id == id }))
        }
    }

    /**
     * Remove only [week] from a course. If the course was "every week", expand to
     * 1..[maxTeachingWeek] minus [week]. Deletes the course when no weeks remain.
     */
    suspend fun removeCourseOccurrence(id: String, week: Int) {
        ensureLoaded()
        mutex.withLock {
            val current = _store.value
            val course = current.courses.find { it.id == id } ?: return@withLock
            val maxWeek = current.maxTeachingWeek.coerceAtLeast(1)
            val remaining = when {
                course.teachingWeeks.isEmpty() ->
                    (1..maxWeek).filter { it != week }
                else ->
                    course.teachingWeeks.filter { it != week }
            }
            val nextCourses = if (remaining.isEmpty()) {
                current.courses.filterNot { it.id == id }
            } else {
                current.courses.map {
                    if (it.id != id) it
                    else it.copy(
                        teachingWeeks = remaining,
                        teachingWeekLabel = CourseItem.formatWeekList(remaining),
                    )
                }
            }
            persistLocked(current.copy(courses = nextCourses))
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

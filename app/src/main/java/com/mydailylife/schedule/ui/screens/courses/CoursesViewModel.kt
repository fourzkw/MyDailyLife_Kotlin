package com.mydailylife.schedule.ui.screens.courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydailylife.schedule.data.CourseExcelImporter
import com.mydailylife.schedule.data.CourseGridDefaults
import com.mydailylife.schedule.data.CourseIcsImporter
import com.mydailylife.schedule.data.CourseItem
import com.mydailylife.schedule.data.CoursePeriodSchedule
import com.mydailylife.schedule.data.CourseRepository
import com.mydailylife.schedule.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class PendingCourseImport(
    val courses: List<CourseItem>,
    val sourceHint: String,
    val suggestedTermStart: LocalDate? = null,
)

data class CoursesUiState(
    val courses: List<CourseItem> = emptyList(),
    /** Courses visible in [teachingWeek]. */
    val visibleCourses: List<CourseItem> = emptyList(),
    val teachingWeek: Int = 1,
    val maxTeachingWeek: Int = 25,
    val termStartDate: LocalDate? = null,
    val weekDates: List<LocalDate> = emptyList(),
    val periodSchedule: CoursePeriodSchedule = CoursePeriodSchedule(),
    val message: String? = null,
    val importing: Boolean = false,
    val pendingImport: PendingCourseImport? = null,
    val icsSubscriptionUrl: String = "",
)

class CoursesViewModel(
    private val repository: CourseRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val message = MutableStateFlow<String?>(null)
    private val importing = MutableStateFlow(false)
    private val teachingWeek = MutableStateFlow(1)
    private val pendingImport = MutableStateFlow<PendingCourseImport?>(null)

    val uiState: StateFlow<CoursesUiState> = combine(
        combine(
            repository.store,
            teachingWeek,
            message,
            importing,
            pendingImport,
        ) { store, week, msg, busy, pending ->
            val maxWeek = store.maxTeachingWeek.coerceIn(1, 30)
            val clamped = week.coerceIn(1, maxWeek)
            val termStart = store.termStartDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val weekStart = if (termStart != null) {
                CourseGridDefaults.weekStartForTeachingWeek(termStart, clamped)
            } else {
                CourseRepository.todayMonday().plusWeeks((clamped - 1).toLong())
            }
            val dates = (0..6).map { weekStart.plusDays(it.toLong()) }
            val visible = store.courses.filter { it.occursInTeachingWeek(clamped) }
            CoursesUiState(
                courses = store.courses,
                visibleCourses = visible,
                teachingWeek = clamped,
                maxTeachingWeek = maxWeek,
                termStartDate = termStart,
                weekDates = dates,
                periodSchedule = store.periodSchedule,
                message = msg,
                importing = busy,
                pendingImport = pending,
            )
        },
        settingsRepository.settings,
    ) { base, settings ->
        base.copy(icsSubscriptionUrl = settings.courseIcsUrl)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CoursesUiState(),
    )

    init {
        viewModelScope.launch {
            repository.ensureLoaded()
            settingsRepository.ensureLoaded()
            syncTeachingWeekToToday()
        }
    }

    fun consumeMessage() {
        message.value = null
    }

    fun shiftTeachingWeek(delta: Int) {
        teachingWeek.update { (it + delta).coerceIn(1, uiState.value.maxTeachingWeek) }
    }

    fun goCurrentTeachingWeek() {
        syncTeachingWeekToToday()
    }

    fun setTermStartDate(date: LocalDate) {
        viewModelScope.launch {
            repository.setTermStartDate(CourseGridDefaults.asTermStartMonday(date))
            syncTeachingWeekToToday()
            message.value = "已更新第一周起始日"
        }
    }

    fun applyDefaultTermStart() {
        setTermStartDate(CourseGridDefaults.defaultTermStart())
    }

    fun setPeriodSchedule(schedule: CoursePeriodSchedule) {
        viewModelScope.launch {
            repository.setPeriodSchedule(schedule)
            message.value = "已更新上课时间"
        }
    }

    private fun syncTeachingWeekToToday() {
        val store = repository.store.value
        val termStart = store.termStartDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val week = if (termStart != null) {
            CourseGridDefaults.teachingWeekForDate(termStart, LocalDate.now())
                .coerceIn(1, store.maxTeachingWeek.coerceAtLeast(1))
        } else {
            1
        }
        teachingWeek.value = week
    }

    fun importExcelBytes(fileName: String, bytes: ByteArray) {
        viewModelScope.launch {
            importing.value = true
            try {
                val courses = withContext(Dispatchers.Default) {
                    CourseExcelImporter.parseBytes(fileName, bytes)
                }
                if (courses.isEmpty()) {
                    message.value = "未解析到课程行"
                    return@launch
                }
                pendingImport.value = PendingCourseImport(
                    courses = courses,
                    sourceHint = "Excel · $fileName",
                )
            } catch (e: Exception) {
                message.value = e.message?.takeIf { it.isNotBlank() } ?: "导入失败"
            } finally {
                importing.update { false }
            }
        }
    }

    fun importIcsInput(raw: String) {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            message.value = "请输入订阅链接或 ICS 内容"
            return
        }
        viewModelScope.launch {
            importing.value = true
            try {
                val (content, savedUrl) = withContext(Dispatchers.IO) {
                    if (CourseIcsImporter.looksLikeUrl(trimmed)) {
                        CourseIcsImporter.fetchUrl(trimmed) to trimmed
                    } else {
                        trimmed to null
                    }
                }
                val result = withContext(Dispatchers.Default) {
                    CourseIcsImporter.parse(content)
                }
                if (savedUrl != null) {
                    settingsRepository.setCourseIcsUrl(savedUrl)
                }
                pendingImport.value = PendingCourseImport(
                    courses = result.courses,
                    sourceHint = if (savedUrl != null) "ICS 订阅" else "ICS 内容",
                    suggestedTermStart = result.suggestedTermStart,
                )
            } catch (e: Exception) {
                message.value = e.message?.takeIf { it.isNotBlank() } ?: "ICS 导入失败"
            } finally {
                importing.update { false }
            }
        }
    }

    fun importIcsBytes(fileName: String, bytes: ByteArray) {
        viewModelScope.launch {
            importing.value = true
            try {
                val text = withContext(Dispatchers.IO) { bytes.toString(Charsets.UTF_8) }
                val result = withContext(Dispatchers.Default) {
                    CourseIcsImporter.parse(text)
                }
                pendingImport.value = PendingCourseImport(
                    courses = result.courses,
                    sourceHint = "ICS 文件 · $fileName",
                    suggestedTermStart = result.suggestedTermStart,
                )
            } catch (e: Exception) {
                message.value = e.message?.takeIf { it.isNotBlank() } ?: "ICS 导入失败"
            } finally {
                importing.update { false }
            }
        }
    }

    fun refreshIcsSubscription() {
        val url = settingsRepository.settings.value.courseIcsUrl
        if (url.isBlank()) {
            message.value = "还没有保存的 ICS 订阅链接"
            return
        }
        viewModelScope.launch {
            importing.value = true
            try {
                val content = withContext(Dispatchers.IO) { CourseIcsImporter.fetchUrl(url) }
                val result = withContext(Dispatchers.Default) {
                    CourseIcsImporter.parse(content)
                }
                pendingImport.value = PendingCourseImport(
                    courses = result.courses,
                    sourceHint = "ICS 刷新",
                    suggestedTermStart = result.suggestedTermStart,
                )
            } catch (e: Exception) {
                message.value = e.message?.takeIf { it.isNotBlank() } ?: "刷新失败"
            } finally {
                importing.update { false }
            }
        }
    }

    fun dismissPendingImport() {
        pendingImport.value = null
    }

    fun confirmPendingImport(termStart: LocalDate) {
        val pending = pendingImport.value ?: return
        viewModelScope.launch {
            importing.value = true
            try {
                val maxWeek = pending.courses.flatMap { it.teachingWeeks }.maxOrNull() ?: 25
                repository.replaceAll(
                    items = pending.courses,
                    termStartDate = CourseGridDefaults.asTermStartMonday(termStart),
                    maxTeachingWeek = maxWeek,
                )
                pendingImport.value = null
                syncTeachingWeekToToday()
                message.value = "已导入 ${pending.courses.size} 门课（${pending.sourceHint}）"
            } catch (e: Exception) {
                message.value = e.message?.takeIf { it.isNotBlank() } ?: "导入失败"
            } finally {
                importing.update { false }
            }
        }
    }

    fun clearAllCourses() {
        viewModelScope.launch {
            repository.clearAll()
            message.value = "已清空课表"
        }
    }

    fun addManualCourse(
        title: String,
        teacher: String,
        location: String,
        weekday: Int,
        startSlot: Int,
        endSlot: Int,
        everyWeek: Boolean,
    ) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) {
            message.value = "请填写课程名称"
            return
        }
        val week = teachingWeek.value
        val course = CourseItem(
            id = java.util.UUID.randomUUID().toString(),
            title = trimmed,
            teacher = teacher.trim(),
            location = location.trim(),
            weekday = weekday,
            startSlot = startSlot,
            endSlot = endSlot,
            teachingWeeks = if (everyWeek) emptyList() else listOf(week),
            teachingWeekLabel = if (everyWeek) "每周" else "$week",
        )
        viewModelScope.launch {
            val overlap = repository.store.value.courses.any {
                it.weekday == weekday &&
                    it.startSlot <= endSlot &&
                    it.endSlot >= startSlot &&
                    it.occursInTeachingWeek(week)
            }
            if (overlap) {
                message.value = "该时段已有课程，请先删除或换时段"
                return@launch
            }
            repository.addCourse(course)
            message.value = "已添加「${course.title}」"
        }
    }

    fun updateManualCourse(course: CourseItem) {
        viewModelScope.launch {
            repository.updateCourse(course)
            message.value = "已保存课程"
        }
    }

    fun deleteCourseAll(id: String) {
        viewModelScope.launch {
            repository.removeCourse(id)
            message.value = "已删除该课程（全部周次）"
        }
    }

    fun deleteCourseThisWeek(id: String) {
        viewModelScope.launch {
            repository.removeCourseOccurrence(id, teachingWeek.value)
            message.value = "已删除本周这一节"
        }
    }

    companion object {
        fun factory(
            repository: CourseRepository,
            settingsRepository: SettingsRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CoursesViewModel(repository, settingsRepository) as T
                }
            }
    }
}

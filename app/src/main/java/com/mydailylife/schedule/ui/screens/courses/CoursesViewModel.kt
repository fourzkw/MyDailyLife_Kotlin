package com.mydailylife.schedule.ui.screens.courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydailylife.schedule.data.CourseExcelImporter
import com.mydailylife.schedule.data.CourseGridDefaults
import com.mydailylife.schedule.data.CourseItem
import com.mydailylife.schedule.data.CourseRepository
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
)

data class CoursesUiState(
    val courses: List<CourseItem> = emptyList(),
    /** Courses visible in [teachingWeek]. */
    val visibleCourses: List<CourseItem> = emptyList(),
    val teachingWeek: Int = 1,
    val maxTeachingWeek: Int = 25,
    val termStartDate: LocalDate? = null,
    val weekDates: List<LocalDate> = emptyList(),
    val message: String? = null,
    val importing: Boolean = false,
    val pendingImport: PendingCourseImport? = null,
)

class CoursesViewModel(
    private val repository: CourseRepository,
) : ViewModel() {
    private val message = MutableStateFlow<String?>(null)
    private val importing = MutableStateFlow(false)
    private val teachingWeek = MutableStateFlow(1)
    private val pendingImport = MutableStateFlow<PendingCourseImport?>(null)

    val uiState: StateFlow<CoursesUiState> = combine(
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
            message = msg,
            importing = busy,
            pendingImport = pending,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CoursesUiState(),
    )

    init {
        viewModelScope.launch {
            repository.ensureLoaded()
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
                val hasExt = fileName.contains('.')
                if (hasExt && !CourseExcelImporter.isCsvName(fileName)) {
                    message.value =
                        "暂不支持直接解析该格式，请在 Excel 中另存为 CSV（UTF-8）后再导入"
                    return@launch
                }
                val text = withContext(Dispatchers.IO) {
                    bytes.toString(Charsets.UTF_8)
                }
                val courses = withContext(Dispatchers.Default) {
                    CourseExcelImporter.parseCsv(text)
                }
                if (courses.isEmpty()) {
                    message.value = "未解析到课程行"
                    return@launch
                }
                pendingImport.value = PendingCourseImport(
                    courses = courses,
                    sourceHint = "Excel/CSV · $fileName",
                )
            } catch (e: Exception) {
                message.value = e.message?.takeIf { it.isNotBlank() } ?: "导入失败"
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
                message.value = "已从 Excel/CSV 导入 ${pending.courses.size} 门课"
            } catch (e: Exception) {
                message.value = e.message?.takeIf { it.isNotBlank() } ?: "导入失败"
            } finally {
                importing.update { false }
            }
        }
    }

    companion object {
        fun factory(repository: CourseRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CoursesViewModel(repository) as T
                }
            }
    }
}

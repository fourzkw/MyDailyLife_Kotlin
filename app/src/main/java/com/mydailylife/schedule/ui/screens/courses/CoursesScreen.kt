package com.mydailylife.schedule.ui.screens.courses

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mydailylife.schedule.data.AcademicSemester
import com.mydailylife.schedule.data.CourseGridDefaults
import com.mydailylife.schedule.data.CourseImportMethod
import com.mydailylife.schedule.data.CourseItem
import com.mydailylife.schedule.ui.components.PrimaryPillButton
import com.mydailylife.schedule.ui.components.SectionHeader
import com.mydailylife.schedule.ui.components.SettingsRow
import com.mydailylife.schedule.ui.components.horizontalSwipe
import com.mydailylife.schedule.ui.theme.Body
import com.mydailylife.schedule.ui.theme.CardShape
import com.mydailylife.schedule.ui.theme.Hairline
import com.mydailylife.schedule.ui.theme.HairlineSoft
import com.mydailylife.schedule.ui.theme.Ink
import com.mydailylife.schedule.ui.theme.Muted
import com.mydailylife.schedule.ui.theme.OnSoftPrimary
import com.mydailylife.schedule.ui.theme.PriorityHigh
import com.mydailylife.schedule.ui.theme.PriorityLow
import com.mydailylife.schedule.ui.theme.PriorityMedium
import com.mydailylife.schedule.ui.theme.PriorityUrgent
import com.mydailylife.schedule.ui.theme.Rausch
import com.mydailylife.schedule.ui.theme.RauschSoft
import com.mydailylife.schedule.ui.theme.SurfaceCard
import com.mydailylife.schedule.ui.theme.SurfaceSoft
import com.mydailylife.schedule.ui.theme.SurfaceStrong
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Wide enough for "08:30" on one line at label size. */
private val TimeGutterWidth = 48.dp
private val SlotHeight = 56.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursesScreen(
    onAcademicImport: () -> Unit,
    viewModel: CoursesViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val today = LocalDate.now()
    val weekDates = uiState.weekDates.ifEmpty {
        val monday = com.mydailylife.schedule.data.CourseRepository.todayMonday()
        (0..6).map { monday.plusDays(it.toLong()) }
    }
    val weekEnd = weekDates.lastOrNull() ?: today
    val weekStart = weekDates.firstOrNull() ?: today
    val rangeFormatter = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)
    val weekRange = "${weekStart.format(rangeFormatter)} – ${weekEnd.format(rangeFormatter)}"
    val teachingLabel = "第${uiState.teachingWeek}周"
    val isCurrentWeek = uiState.teachingWeek == currentTeachingWeekHint(uiState)

    var showImportSheet by remember { mutableStateOf(false) }
    var showIcsPlaceholder by remember { mutableStateOf(false) }
    var showExcelHint by remember { mutableStateOf(false) }
    var showCourseSettings by remember { mutableStateOf(false) }
    var detailCourse by remember { mutableStateOf<CourseItem?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val excelPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val name = queryDisplayName(context, uri) ?: "course.csv"
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
            if (bytes == null) {
                snackbar.showSnackbar("无法读取所选文件")
            } else {
                viewModel.importExcelBytes(name, bytes)
            }
        }
    }

    LaunchedEffect(uiState.message) {
        val msg = uiState.message ?: return@LaunchedEffect
        snackbar.showSnackbar(msg)
        viewModel.consumeMessage()
    }

    uiState.pendingImport?.let { pending ->
        val preview = pending.courses.take(6).joinToString("\n") {
            "· ${it.title} 周${it.weekday} ${it.startSlot}-${it.endSlot}节"
        } + if (pending.courses.size > 6) "\n…" else ""
        TermStartConfirmDialog(
            title = "确认导入",
            summary = "识别到 ${pending.courses.size} 门课（来源：${pending.sourceHint}）。导入将替换当前课表。\n\n$preview",
            initialTermStart = uiState.termStartDate ?: CourseGridDefaults.defaultTermStart(),
            onDismiss = { viewModel.dismissPendingImport() },
            onConfirm = { viewModel.confirmPendingImport(it) },
        )
    }

    if (showIcsPlaceholder) {
        PlaceholderDialog(
            title = "ICS 订阅",
            body = "订阅链接与 ICS 文件解析将在后续版本接入。可先用 Excel / CSV 或教务导入课表。",
            onDismiss = { showIcsPlaceholder = false },
        )
    }
    if (showExcelHint) {
        AlertDialog(
            onDismissRequest = { showExcelHint = false },
            title = { Text("Excel / CSV 导入") },
            text = {
                Text(
                    "请使用表头：课程名、教师、地点、星期、开始节次、结束节次。\n" +
                        "星期可用 1–7 或 一…日；节次从 1 起。\n" +
                        "未填周次的课程视为每周都上。\n" +
                        "若为 .xlsx，请先在 Excel 中另存为 CSV（UTF-8）后再选文件。",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExcelHint = false
                        excelPicker.launch(
                            arrayOf(
                                "text/*",
                                "text/comma-separated-values",
                                "text/csv",
                                "application/vnd.ms-excel",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "application/octet-stream",
                                "*/*",
                            ),
                        )
                    },
                ) { Text("选择文件") }
            },
            dismissButton = {
                TextButton(onClick = { showExcelHint = false }) { Text("取消") }
            },
        )
    }

    if (showImportSheet) {
        ModalBottomSheet(
            onDismissRequest = { showImportSheet = false },
            sheetState = sheetState,
            containerColor = SurfaceSoft,
        ) {
            Column(modifier = Modifier.padding(bottom = 28.dp)) {
                Text(
                    text = "选择导入方式",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                CourseImportMethod.entries.forEachIndexed { index, method ->
                    if (index > 0) HorizontalDivider(color = Hairline)
                    SettingsRow(
                        title = method.title,
                        subtitle = method.subtitle,
                        onClick = {
                            showImportSheet = false
                            when (method) {
                                CourseImportMethod.Excel -> showExcelHint = true
                                CourseImportMethod.Academic -> onAcademicImport()
                                CourseImportMethod.Ics -> showIcsPlaceholder = true
                            }
                        },
                    )
                }
            }
        }
    }

    detailCourse?.let { course ->
        ModalBottomSheet(
            onDismissRequest = { detailCourse = null },
            sheetState = detailSheetState,
            containerColor = SurfaceSoft,
        ) {
            CourseDetailContent(course = course)
        }
    }

    if (showCourseSettings) {
        ModalBottomSheet(
            onDismissRequest = { showCourseSettings = false },
            sheetState = settingsSheetState,
            containerColor = SurfaceSoft,
        ) {
            CourseSettingsSheet(
                termStartDate = uiState.termStartDate,
                onSaveTermStart = {
                    viewModel.setTermStartDate(it)
                    showCourseSettings = false
                },
                onApplyDefault = {
                    viewModel.applyDefaultTermStart()
                    showCourseSettings = false
                },
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .horizontalSwipe(
                onSwipeLeft = { viewModel.shiftTeachingWeek(1) },
                onSwipeRight = { viewModel.shiftTeachingWeek(-1) },
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionHeader(
                    title = "课表",
                    action = if (isCurrentWeek) "本周" else "回到本周",
                    onAction = { viewModel.goCurrentTeachingWeek() },
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { showCourseSettings = true }) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "课表设置",
                        tint = Muted,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            WeekNavigatorCard(
                teachingLabel = teachingLabel,
                weekRange = weekRange,
                isCurrentWeek = isCurrentWeek,
                canGoPrev = uiState.teachingWeek > 1,
                canGoNext = uiState.teachingWeek < uiState.maxTeachingWeek,
                onPrev = { viewModel.shiftTeachingWeek(-1) },
                onNext = { viewModel.shiftTeachingWeek(1) },
            )

            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(SurfaceCard)
                    .border(1.dp, HairlineSoft, CardShape)
                    .padding(top = 10.dp, bottom = 8.dp),
            ) {
                WeekdayHeaderRow(weekDates = weekDates, today = today)
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = HairlineSoft)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 4.dp),
                ) {
                    if (uiState.visibleCourses.isEmpty()) {
                        EmptyWeekHint(
                            hasAnyCourses = uiState.courses.isNotEmpty(),
                            teachingWeek = uiState.teachingWeek,
                        )
                    }
                    CourseGrid(
                        courses = uiState.visibleCourses,
                        weekDates = weekDates,
                        today = today,
                        slotHeight = SlotHeight,
                        onCourseClick = { detailCourse = it },
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            PrimaryPillButton(
                text = if (uiState.importing) "导入中…" else "导入课表",
                onClick = { showImportSheet = true },
                enabled = !uiState.importing,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp),
        )
    }
}

private fun currentTeachingWeekHint(uiState: CoursesUiState): Int {
    val term = uiState.termStartDate ?: return uiState.teachingWeek
    return CourseGridDefaults.teachingWeekForDate(term, LocalDate.now())
        .coerceIn(1, uiState.maxTeachingWeek)
}

@Composable
private fun CourseSettingsSheet(
    termStartDate: LocalDate?,
    onSaveTermStart: (LocalDate) -> Unit,
    onApplyDefault: () -> Unit,
) {
    val initial = termStartDate ?: CourseGridDefaults.defaultTermStart()
    var draft by remember(termStartDate) {
        mutableStateOf(CourseGridDefaults.asTermStartMonday(initial))
    }
    val today = LocalDate.now()
    val semesterLabel = when (CourseGridDefaults.inferredSemester(today)) {
        AcademicSemester.Autumn -> "当前按秋季学期推算"
        AcademicSemester.Spring -> "当前按春季学期推算"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
    ) {
        Text("课表设置", style = MaterialTheme.typography.titleMedium, color = Ink)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$semesterLabel。默认：秋=9月第二周周一，春=2月第二周周一。",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
        )
        Spacer(modifier = Modifier.height(12.dp))
        TermStartEditor(
            selected = draft,
            onSelect = { draft = it },
            caption = "修改后左右滑的教学周会按新起点重新对齐。",
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onApplyDefault) { Text("恢复默认") }
            TextButton(onClick = { onSaveTermStart(draft) }) { Text("保存") }
        }
    }
}

@Composable
private fun WeekNavigatorCard(
    teachingLabel: String,
    weekRange: String,
    isCurrentWeek: Boolean,
    canGoPrev: Boolean,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(if (isCurrentWeek) RauschSoft else SurfaceStrong)
            .border(
                1.dp,
                if (isCurrentWeek) Rausch.copy(alpha = 0.22f) else HairlineSoft,
                CardShape,
            )
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onPrev,
            enabled = canGoPrev,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "上一周",
                tint = if (canGoPrev) Ink else Muted.copy(alpha = 0.4f),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = teachingLabel,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrentWeek) OnSoftPrimary else Ink,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = weekRange,
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
        }
        IconButton(
            onClick = onNext,
            enabled = canGoNext,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "下一周",
                tint = if (canGoNext) Ink else Muted.copy(alpha = 0.4f),
            )
        }
    }
}

@Composable
private fun WeekdayHeaderRow(
    weekDates: List<LocalDate>,
    today: LocalDate,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(TimeGutterWidth))
        weekDates.forEach { date ->
            val isToday = date == today
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(CardShape)
                    .background(if (isToday) RauschSoft else Color.Transparent)
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = CourseGridDefaults.weekdayLabels[date.dayOfWeek.value - 1],
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) OnSoftPrimary else Muted,
                    fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                )
                Text(
                    text = "${date.dayOfMonth}",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isToday) OnSoftPrimary else Ink,
                    fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun EmptyWeekHint(
    hasAnyCourses: Boolean,
    teachingWeek: Int,
) {
    Text(
        text = if (hasAnyCourses) {
            "第${teachingWeek}周暂无课程，左右滑动或点箭头切换"
        } else {
            "还没有课表，点下方导入"
        },
        color = Muted,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun CourseGrid(
    courses: List<CourseItem>,
    weekDates: List<LocalDate>,
    today: LocalDate,
    slotHeight: Dp,
    onCourseClick: (CourseItem) -> Unit,
) {
    val todayIndex = weekDates.indexOfFirst { it == today }
    Box(modifier = Modifier.padding(horizontal = 8.dp)) {
        Column {
            CourseGridDefaults.timeSlots.forEachIndexed { index, time ->
                val isBreakAfter = index == 3 || index == 8
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(slotHeight),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = time,
                        modifier = Modifier
                            .width(TimeGutterWidth)
                            .padding(top = 2.dp, end = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                        ),
                        color = Muted,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        textAlign = TextAlign.End,
                    )
                    repeat(7) { dayIndex ->
                        val isTodayCol = dayIndex == todayIndex
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(
                                    if (isTodayCol) RauschSoft.copy(alpha = 0.45f)
                                    else Color.Transparent,
                                )
                                .border(0.5.dp, HairlineSoft),
                        )
                    }
                }
                if (isBreakAfter) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = TimeGutterWidth)
                            .height(6.dp)
                            .background(SurfaceStrong),
                    )
                }
            }
        }

        // Break bands shift subsequent blocks; account for gaps after slot 4 and 9 (0-based 3, 8).
        fun topOffset(startSlot: Int): Dp {
            var extra = 0.dp
            if (startSlot > 4) extra += 6.dp
            if (startSlot > 9) extra += 6.dp
            return slotHeight * (startSlot - 1) + extra
        }

        fun blockHeight(startSlot: Int, endSlot: Int): Dp {
            var extra = 0.dp
            if (startSlot <= 4 && endSlot > 4) extra += 6.dp
            if (startSlot <= 9 && endSlot > 9) extra += 6.dp
            return slotHeight * (endSlot - startSlot + 1) + extra
        }

        courses.forEach { course ->
            val top = topOffset(course.startSlot)
            val height = blockHeight(course.startSlot, course.endSlot)
            val palette = courseBlockColors(course)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = TimeGutterWidth)
                    .padding(top = top),
            ) {
                repeat((course.weekday - 1).coerceAtLeast(0)) {
                    Spacer(modifier = Modifier.weight(1f))
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(height)
                        .padding(horizontal = 2.dp, vertical = 2.dp)
                        .clip(CardShape)
                        .background(palette.background)
                        .border(1.dp, palette.border, CardShape)
                        .clickable { onCourseClick(course) },
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(3.dp)
                            .fillMaxHeight()
                            .background(palette.accent),
                    )
                    Column(
                        modifier = Modifier.padding(start = 7.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    ) {
                        Text(
                            text = course.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = Ink,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 14.sp,
                        )
                        if (course.location.isNotBlank()) {
                            Text(
                                text = course.location,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = Body,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                repeat((7 - course.weekday).coerceAtLeast(0)) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private data class CourseBlockPalette(
    val background: Color,
    val border: Color,
    val accent: Color,
)

private fun courseBlockColors(course: CourseItem): CourseBlockPalette {
    val accents = listOf(
        Rausch,
        PriorityUrgent,
        PriorityHigh,
        PriorityMedium,
        PriorityLow,
        Color(0xFF8BB8FF),
        Color(0xFFB39DDB),
    )
    val fills = listOf(
        RauschSoft,
        Color(0xFFFFF0F3),
        Color(0xFFFFF4EC),
        Color(0xFFFFF8E8),
        Color(0xFFEDF8F0),
        Color(0xFFEEF4FF),
        Color(0xFFF5F0FB),
    )
    val idx = abs(course.title.hashCode()) % accents.size
    val accent = accents[idx]
    return CourseBlockPalette(
        background = fills[idx],
        border = accent.copy(alpha = 0.28f),
        accent = accent.copy(alpha = 0.85f),
    )
}

@Composable
private fun CourseDetailContent(course: CourseItem) {
    val weekdayLabel = CourseGridDefaults.weekdayLabels.getOrNull(course.weekday - 1) ?: "${course.weekday}"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(course.title, style = MaterialTheme.typography.titleLarge, color = Ink)
        Spacer(modifier = Modifier.height(12.dp))
        DetailRow("教师", course.teacher.ifBlank { "—" })
        DetailRow("地点", course.location.ifBlank { "—" })
        DetailRow("校区", course.campusName.ifBlank { "—" })
        DetailRow("星期", "周$weekdayLabel")
        DetailRow("节次", "${course.startSlot}–${course.endSlot} 节")
        DetailRow("周次", course.weeksSummary())
        if (course.courseCode.isNotBlank()) DetailRow("课程号", course.courseCode)
        if (course.classNbr.isNotBlank()) DetailRow("教学班", course.classNbr)
        if (course.courseStudyNature.isNotBlank()) DetailRow("课程性质", course.courseStudyNature)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
            modifier = Modifier.width(72.dp),
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = Ink)
    }
}

@Composable
private fun PlaceholderDialog(
    title: String,
    body: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("知道了") }
        },
    )
}

private fun queryDisplayName(context: android.content.Context, uri: Uri): String? {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) return cursor.getString(index)
            }
        }
    return uri.lastPathSegment
}

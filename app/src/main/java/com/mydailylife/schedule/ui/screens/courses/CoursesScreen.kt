package com.mydailylife.schedule.ui.screens.courses

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.mydailylife.schedule.ui.theme.ScreenHeaderToContent
import com.mydailylife.schedule.ui.theme.ScreenHorizontalPadding
import com.mydailylife.schedule.ui.theme.ScreenTopPadding
import com.mydailylife.schedule.ui.theme.SurfaceCard
import com.mydailylife.schedule.ui.theme.SurfaceSoft
import com.mydailylife.schedule.ui.theme.SurfaceStrong
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Narrow gutter so day columns get more width. */
private val TimeGutterWidth = 34.dp
private val SlotHeight = 42.dp
private val BreakBandHeight = 4.dp
private val TimeLabelSize = 7.sp
private val BlockTitleSize = 10.sp
private val BlockMetaSize = 9.sp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    var showIcsDialog by remember { mutableStateOf(false) }
    var icsInput by remember { mutableStateOf("") }
    var showExcelHint by remember { mutableStateOf(false) }
    var showCourseSettings by remember { mutableStateOf(false) }
    var detailCourse by remember { mutableStateOf<CourseItem?>(null) }
    var draftSlots by remember { mutableStateOf<List<DraftSlot>>(emptyList()) }
    var liveDraft by remember { mutableStateOf<DraftSlot?>(null) }
    var addTarget by remember { mutableStateOf<DraftSlot?>(null) }
    var editCourse by remember { mutableStateOf<CourseItem?>(null) }
    var deleteCourse by remember { mutableStateOf<CourseItem?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(uiState.teachingWeek) {
        draftSlots = emptyList()
        liveDraft = null
        addTarget = null
    }

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

    val icsPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val name = queryDisplayName(context, uri) ?: "schedule.ics"
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
            if (bytes == null) {
                snackbar.showSnackbar("无法读取所选文件")
            } else {
                showIcsDialog = false
                viewModel.importIcsBytes(name, bytes)
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
            initialTermStart = pending.suggestedTermStart
                ?: uiState.termStartDate
                ?: CourseGridDefaults.defaultTermStart(),
            onDismiss = { viewModel.dismissPendingImport() },
            onConfirm = { viewModel.confirmPendingImport(it) },
        )
    }

    if (showIcsDialog) {
        AlertDialog(
            onDismissRequest = { showIcsDialog = false },
            title = { Text("ICS 订阅") },
            text = {
                Column {
                    Text(
                        text = "粘贴订阅链接，或直接粘贴 ICS 文本。也可选择本地 .ics 文件。",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = icsInput,
                        onValueChange = { icsInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        placeholder = { Text("https://… 或 BEGIN:VCALENDAR…") },
                    )
                    if (uiState.icsSubscriptionUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "已保存：${uiState.icsSubscriptionUrl}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val input = icsInput
                        showIcsDialog = false
                        viewModel.importIcsInput(input)
                    },
                    enabled = !uiState.importing,
                ) { Text(if (uiState.importing) "处理中…" else "导入") }
            },
            dismissButton = {
                Row {
                    if (uiState.icsSubscriptionUrl.isNotBlank()) {
                        TextButton(
                            onClick = {
                                showIcsDialog = false
                                viewModel.refreshIcsSubscription()
                            },
                            enabled = !uiState.importing,
                        ) { Text("刷新订阅") }
                    }
                    TextButton(
                        onClick = {
                            icsPicker.launch(
                                arrayOf(
                                    "text/calendar",
                                    "text/plain",
                                    "application/octet-stream",
                                    "*/*",
                                ),
                            )
                        },
                    ) { Text("选文件") }
                    TextButton(onClick = { showIcsDialog = false }) { Text("取消") }
                }
            },
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
                        "支持直接选择 .xlsx 或 CSV（UTF-8）。",
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
                                CourseImportMethod.Ics -> {
                                    icsInput = uiState.icsSubscriptionUrl
                                    showIcsDialog = true
                                }
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
            CourseDetailContent(
                course = course,
                onEdit = {
                    detailCourse = null
                    editCourse = course
                },
                onDelete = {
                    detailCourse = null
                    deleteCourse = course
                },
            )
        }
    }

    addTarget?.let { draft ->
        AddCourseDialog(
            draft = draft,
            teachingWeek = uiState.teachingWeek,
            onDismiss = { addTarget = null },
            onConfirm = { title, teacher, location, everyWeek ->
                viewModel.addManualCourse(
                    title = title,
                    teacher = teacher,
                    location = location,
                    weekday = draft.weekday,
                    startSlot = draft.startSlot,
                    endSlot = draft.endSlot,
                    everyWeek = everyWeek,
                )
                draftSlots = draftSlots.filterNot { it == draft }
                addTarget = null
            },
        )
    }

    editCourse?.let { course ->
        EditCourseDialog(
            course = course,
            onDismiss = { editCourse = null },
            onSave = {
                viewModel.updateManualCourse(it)
                editCourse = null
            },
        )
    }

    deleteCourse?.let { course ->
        DeleteCourseDialog(
            course = course,
            teachingWeek = uiState.teachingWeek,
            onDismiss = { deleteCourse = null },
            onConfirm = { scope ->
                when (scope) {
                    CourseDeleteScope.ThisWeek -> viewModel.deleteCourseThisWeek(course.id)
                    CourseDeleteScope.AllWeeks -> viewModel.deleteCourseAll(course.id)
                }
                deleteCourse = null
            },
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清空课表") },
            text = { Text("将删除全部课程，学期起始日会保留。此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        draftSlots = emptyList()
                        viewModel.clearAllCourses()
                    },
                ) { Text("清空") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("取消") }
            },
        )
    }

    if (showCourseSettings) {
        ModalBottomSheet(
            onDismissRequest = { showCourseSettings = false },
            sheetState = settingsSheetState,
            containerColor = SurfaceSoft,
        ) {
            CourseSettingsSheet(
                termStartDate = uiState.termStartDate,
                icsSubscriptionUrl = uiState.icsSubscriptionUrl,
                importing = uiState.importing,
                hasCourses = uiState.courses.isNotEmpty(),
                onSaveTermStart = {
                    viewModel.setTermStartDate(it)
                    showCourseSettings = false
                },
                onApplyDefault = {
                    viewModel.applyDefaultTermStart()
                    showCourseSettings = false
                },
                onRefreshIcs = {
                    showCourseSettings = false
                    viewModel.refreshIcsSubscription()
                },
                onClearCourses = {
                    showCourseSettings = false
                    showClearConfirm = true
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
                .padding(horizontal = ScreenHorizontalPadding),
        ) {
            Spacer(modifier = Modifier.height(ScreenTopPadding))
            SectionHeader(
                title = "课表",
                action = if (isCurrentWeek) "本周" else "回到本周",
                onAction = { viewModel.goCurrentTeachingWeek() },
                trailing = {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "课表设置",
                        tint = Muted,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { showCourseSettings = true },
                    )
                },
            )

            Spacer(modifier = Modifier.height(ScreenHeaderToContent))
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
                    CourseGrid(
                        courses = uiState.visibleCourses,
                        weekDates = weekDates,
                        today = today,
                        slotHeight = SlotHeight,
                        draftSlots = draftSlots,
                        liveDraft = liveDraft,
                        onLiveDraftChange = { liveDraft = it },
                        onDraftCommitted = { draft ->
                            draftSlots = mergeDraftSlot(draftSlots, draft)
                            liveDraft = null
                        },
                        onDraftClick = { addTarget = it },
                        onDraftRemove = { draft ->
                            draftSlots = draftSlots.filterNot { it == draft }
                        },
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
    icsSubscriptionUrl: String,
    importing: Boolean,
    hasCourses: Boolean,
    onSaveTermStart: (LocalDate) -> Unit,
    onApplyDefault: () -> Unit,
    onRefreshIcs: () -> Unit,
    onClearCourses: () -> Unit,
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
        if (icsSubscriptionUrl.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Hairline)
            Spacer(modifier = Modifier.height(12.dp))
            Text("ICS 订阅", style = MaterialTheme.typography.titleSmall, color = Ink)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = icsSubscriptionUrl,
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = onRefreshIcs,
                enabled = !importing,
            ) { Text(if (importing) "刷新中…" else "刷新课表") }
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Hairline)
        Spacer(modifier = Modifier.height(8.dp))
        SettingsRow(
            title = "清空课表",
            subtitle = if (hasCourses) "删除全部课程" else "当前没有课程",
            onClick = if (hasCourses) onClearCourses else null,
        )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CourseGrid(
    courses: List<CourseItem>,
    weekDates: List<LocalDate>,
    today: LocalDate,
    slotHeight: Dp,
    draftSlots: List<DraftSlot>,
    liveDraft: DraftSlot?,
    onLiveDraftChange: (DraftSlot?) -> Unit,
    onDraftCommitted: (DraftSlot) -> Unit,
    onDraftClick: (DraftSlot) -> Unit,
    onDraftRemove: (DraftSlot) -> Unit,
    onCourseClick: (CourseItem) -> Unit,
) {
    val todayIndex = weekDates.indexOfFirst { it == today }
    val density = LocalDensity.current
    val slotCount = CourseGridDefaults.slotCount
    val breakBand = BreakBandHeight
    val gridHeight = slotHeight * slotCount + breakBand * 2
    val slotHeightPx = with(density) { slotHeight.toPx() }
    val breakPx = with(density) { breakBand.toPx() }
    val gutterPx = with(density) { TimeGutterWidth.toPx() }

    fun topOffset(startSlot: Int): Dp {
        var extra = 0.dp
        if (startSlot > 4) extra += breakBand
        if (startSlot > 9) extra += breakBand
        return slotHeight * (startSlot - 1) + extra
    }

    fun blockHeight(startSlot: Int, endSlot: Int): Dp {
        var extra = 0.dp
        if (startSlot <= 4 && endSlot > 4) extra += breakBand
        if (startSlot <= 9 && endSlot > 9) extra += breakBand
        return slotHeight * (endSlot - startSlot + 1) + extra
    }

    fun slotAtY(y: Float): Int {
        var cursor = 0f
        for (slot in 1..slotCount) {
            val bottom = cursor + slotHeightPx
            if (y <= bottom || slot == slotCount) return slot
            cursor = bottom
            if (slot == 4 || slot == 9) cursor += breakPx
        }
        return slotCount
    }

    fun hitTest(offset: Offset, width: Float): Pair<Int, Int>? {
        if (offset.x < gutterPx || width <= gutterPx) return null
        val colWidth = (width - gutterPx) / 7f
        val dayIndex = ((offset.x - gutterPx) / colWidth).toInt().coerceIn(0, 6)
        val weekday = dayIndex + 1
        val slot = slotAtY(offset.y.coerceAtLeast(0f))
        return weekday to slot
    }

    fun isOccupied(weekday: Int, slot: Int): Boolean =
        courses.any {
            it.weekday == weekday && slot in it.startSlot..it.endSlot
        }

    var anchorWeekday by remember { mutableStateOf<Int?>(null) }
    var anchorSlot by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth()
            .height(gridHeight),
    ) {
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
                            .padding(top = 1.dp, end = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = TimeLabelSize,
                            lineHeight = 11.sp,
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
                            .height(breakBand)
                            .background(SurfaceStrong),
                    )
                }
            }
        }

        // Long-press drag on empty cells (behind course/draft overlays).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(courses, slotHeightPx, breakPx, gutterPx) {
                    var pending: DraftSlot? = null
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            val hit = hitTest(offset, size.width.toFloat())
                            if (hit == null || isOccupied(hit.first, hit.second)) {
                                pending = null
                                anchorWeekday = null
                                anchorSlot = null
                                onLiveDraftChange(null)
                                return@detectDragGesturesAfterLongPress
                            }
                            anchorWeekday = hit.first
                            anchorSlot = hit.second
                            pending = DraftSlot(
                                weekday = hit.first,
                                startSlot = hit.second,
                                endSlot = hit.second,
                            )
                            onLiveDraftChange(pending)
                        },
                        onDrag = { change, _ ->
                            val day = anchorWeekday ?: return@detectDragGesturesAfterLongPress
                            val start = anchorSlot ?: return@detectDragGesturesAfterLongPress
                            change.consume()
                            val hit = hitTest(change.position, size.width.toFloat())
                                ?: return@detectDragGesturesAfterLongPress
                            if (hit.first != day) return@detectDragGesturesAfterLongPress
                            var lo = min(start, hit.second)
                            var hi = max(start, hit.second)
                            if (hit.second >= start) {
                                for (s in start..hi) {
                                    if (s != start && isOccupied(day, s)) {
                                        hi = s - 1
                                        break
                                    }
                                }
                            } else {
                                for (s in start downTo lo) {
                                    if (s != start && isOccupied(day, s)) {
                                        lo = s + 1
                                        break
                                    }
                                }
                            }
                            if (lo <= hi) {
                                pending = DraftSlot(day, lo, hi)
                                onLiveDraftChange(pending)
                            }
                        },
                        onDragEnd = {
                            pending?.let(onDraftCommitted)
                            pending = null
                            anchorWeekday = null
                            anchorSlot = null
                        },
                        onDragCancel = {
                            pending = null
                            onLiveDraftChange(null)
                            anchorWeekday = null
                            anchorSlot = null
                        },
                    )
                },
        )

        @Composable
        fun placeBlock(
            weekday: Int,
            startSlot: Int,
            endSlot: Int,
            content: @Composable () -> Unit,
        ) {
            val top = topOffset(startSlot)
            val height = blockHeight(startSlot, endSlot)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = TimeGutterWidth)
                    .padding(top = top),
            ) {
                repeat((weekday - 1).coerceAtLeast(0)) {
                    Spacer(modifier = Modifier.weight(1f))
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(height)
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                ) {
                    content()
                }
                repeat((7 - weekday).coerceAtLeast(0)) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        courses.forEach { course ->
            val palette = courseBlockColors(course)
            placeBlock(course.weekday, course.startSlot, course.endSlot) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
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
                        modifier = Modifier.padding(start = 6.dp, end = 3.dp, top = 2.dp, bottom = 2.dp),
                    ) {
                        Text(
                            text = course.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = BlockTitleSize,
                                lineHeight = 12.sp,
                            ),
                            color = Ink,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (course.teacher.isNotBlank()) {
                            Text(
                                text = course.teacher,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = BlockMetaSize,
                                    lineHeight = 11.sp,
                                ),
                                color = Body,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (course.location.isNotBlank()) {
                            Text(
                                text = course.location,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = BlockMetaSize,
                                    lineHeight = 11.sp,
                                ),
                                color = Muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        val visibleDrafts = buildList {
            addAll(draftSlots)
            liveDraft?.let { add(it) }
        }
        visibleDrafts.forEach { draft ->
            placeBlock(draft.weekday, draft.startSlot, draft.endSlot) {
                val isLive = draft == liveDraft
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CardShape)
                        .background(RauschSoft)
                        .border(1.dp, Rausch.copy(alpha = 0.45f), CardShape)
                        .then(
                            if (!isLive) {
                                Modifier.combinedClickable(
                                    onClick = { onDraftClick(draft) },
                                    onLongClick = { onDraftRemove(draft) },
                                )
                            } else {
                                Modifier
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!isLive) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "添加课程，长按删除",
                            tint = Rausch,
                            modifier = Modifier.size(20.dp),
                        )
                    }
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
private fun CourseDetailContent(
    course: CourseItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
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
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDelete) { Text("删除") }
            TextButton(onClick = onEdit) { Text("编辑") }
        }
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

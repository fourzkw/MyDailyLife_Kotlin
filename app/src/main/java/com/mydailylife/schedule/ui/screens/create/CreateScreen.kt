package com.mydailylife.schedule.ui.screens.create

import android.widget.NumberPicker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mydailylife.schedule.data.AppSettings
import com.mydailylife.schedule.data.Priority
import com.mydailylife.schedule.data.ScheduleItem
import com.mydailylife.schedule.data.ScheduleTimeMode
import com.mydailylife.schedule.data.WeekdayLabels
import com.mydailylife.schedule.ui.components.DeleteScheduleDialog
import com.mydailylife.schedule.ui.components.FormFieldShell
import com.mydailylife.schedule.ui.components.PrimaryPillButton
import com.mydailylife.schedule.ui.components.SettingsRow
import com.mydailylife.schedule.ui.theme.CardShape
import com.mydailylife.schedule.ui.theme.Hairline
import com.mydailylife.schedule.ui.theme.Ink
import com.mydailylife.schedule.ui.theme.Muted
import com.mydailylife.schedule.ui.theme.MutedSoft
import com.mydailylife.schedule.ui.theme.OnSoftPrimary
import com.mydailylife.schedule.ui.theme.PillShape
import com.mydailylife.schedule.ui.theme.PriorityUrgent
import com.mydailylife.schedule.ui.theme.Rausch
import com.mydailylife.schedule.ui.theme.RauschSoft
import com.mydailylife.schedule.ui.theme.SurfaceSoft
import com.mydailylife.schedule.ui.theme.SurfaceStrong
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class PickerTarget {
    StartDate,
    EndDate,
    StartTime,
    EndTime,
    /** Once: date then clock for start. */
    StartDateTime,
    /** Once: date then clock for end. */
    EndDateTime,
}

/** Two-step date → time flow for Once mode. */
private data class DateTimePickSession(
    val target: PickerTarget,
    val pendingDate: LocalDate? = null,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateScreen(
    onBack: () -> Unit,
    viewModel: CreateViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var pickerTarget by remember { mutableStateOf<PickerTarget?>(null) }
    var dateTimeSession by remember { mutableStateOf<DateTimePickSession?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showLeadDialog by remember { mutableStateOf(false) }
    val zone = remember { ZoneId.systemDefault() }
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("yyyy年M月d日 E", Locale.CHINA)
    }
    val timeFormatter = remember {
        DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
    }
    val dateTimeFormatter = remember {
        DateTimeFormatter.ofPattern("yyyy年M月d日 E HH:mm", Locale.CHINA)
    }

    LaunchedEffect(uiState.saved, uiState.deleted) {
        if (uiState.saved || uiState.deleted) onBack()
    }
    LaunchedEffect(uiState.error) {
        val err = uiState.error ?: return@LaunchedEffect
        snackbar.showSnackbar(err)
        viewModel.consumeError()
    }

    if (showDeleteConfirm) {
        DeleteScheduleDialog(
            item = ScheduleItem(
                id = uiState.editId.orEmpty(),
                title = uiState.title.ifBlank { "该事项" },
                timeMode = uiState.timeMode.storageKey,
            ),
            onDeleteEntire = {
                showDeleteConfirm = false
                viewModel.delete()
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.screenTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceSoft,
                    titleContentColor = Ink,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            FormFieldShell(
                label = "标题",
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                placeholder = "输入事项标题",
            )
            Spacer(modifier = Modifier.height(16.dp))
            FormFieldShell(
                label = "描述",
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                placeholder = "可选备注",
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("时间", style = MaterialTheme.typography.labelMedium, color = Muted)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ScheduleTimeMode.entries.forEach { mode ->
                    ChoiceChip(
                        text = mode.label,
                        selected = mode == uiState.timeMode,
                        onClick = { viewModel.onTimeModeChange(mode) },
                    )
                }
            }
            if (uiState.showWeekdays) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("重复星期", style = MaterialTheme.typography.labelMedium, color = Muted)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WeekdayLabels.forEach { (day, label) ->
                        ChoiceChip(
                            text = label,
                            selected = day.value in uiState.weekdays,
                            onClick = { viewModel.onWeekdayToggle(day.value) },
                        )
                    }
                }
            }
            if (uiState.showDateTimePickers) {
                Spacer(modifier = Modifier.height(16.dp))
                val startLabel = uiState.startDate?.let { date ->
                    uiState.startTime?.let { time -> date.atTime(time).format(dateTimeFormatter) }
                }
                val endLabel = uiState.endDate?.let { date ->
                    uiState.endTime?.let { time -> date.atTime(time).format(dateTimeFormatter) }
                }
                PickerField(
                    label = "开始时间（可选）",
                    value = startLabel,
                    placeholder = "点击选择日期与时刻",
                    clearable = true,
                    onClick = {
                        dateTimeSession = DateTimePickSession(PickerTarget.StartDateTime)
                    },
                    onClear = { viewModel.onStartDateTimeChange(null, null) },
                )
                Spacer(modifier = Modifier.height(12.dp))
                PickerField(
                    label = "结束时间（可选）",
                    value = endLabel,
                    placeholder = "点击选择日期与时刻",
                    clearable = true,
                    onClick = {
                        dateTimeSession = DateTimePickSession(PickerTarget.EndDateTime)
                    },
                    onClear = { viewModel.onEndDateTimeChange(null, null) },
                )
            }
            if (uiState.showTimeOfDayPickers) {
                Spacer(modifier = Modifier.height(16.dp))
                PickerField(
                    label = "开始时刻（可选）",
                    value = uiState.startTime?.format(timeFormatter),
                    placeholder = "点击选择时刻",
                    clearable = true,
                    onClick = { pickerTarget = PickerTarget.StartTime },
                    onClear = { viewModel.onStartTimeChange(null) },
                )
                Spacer(modifier = Modifier.height(12.dp))
                PickerField(
                    label = "结束时刻（可选）",
                    value = uiState.endTime?.format(timeFormatter),
                    placeholder = "点击选择时刻",
                    clearable = true,
                    onClick = { pickerTarget = PickerTarget.EndTime },
                    onClear = { viewModel.onEndTimeChange(null) },
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("优先级", style = MaterialTheme.typography.labelMedium, color = Muted)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.entries.forEach { p ->
                    ChoiceChip(
                        text = p.label,
                        selected = p == uiState.priority,
                        onClick = { viewModel.onPriorityChange(p) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            FormFieldShell(
                label = "标签",
                value = uiState.tagsText,
                onValueChange = viewModel::onTagsChange,
                placeholder = "用逗号分隔",
            )
            if (uiState.presetTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                val selectedTags = uiState.tagsText
                    .split(',', '，', ';', '；', ' ')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toSet()
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    uiState.presetTags.forEach { tag ->
                        ChoiceChip(
                            text = tag,
                            selected = tag in selectedTags,
                            onClick = { viewModel.onPresetTagClick(tag) },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            val canRemind = uiState.timeMode != ScheduleTimeMode.Unlimited
            val hasStart = if (uiState.showDateTimePickers) {
                uiState.startDate != null && uiState.startTime != null
            } else {
                uiState.startTime != null
            }
            val hasEnd = if (uiState.showDateTimePickers) {
                uiState.endDate != null && uiState.endTime != null
            } else {
                uiState.endTime != null
            }
            val reminderSubtitle = when {
                !canRemind -> "无限制事项没有时刻，无法提醒"
                !uiState.reminderEnabled -> "关闭后本条不弹出本地提醒"
                !hasStart && !hasEnd -> "请先设置开始或结束时间"
                else -> "开始/结束前按提前量本地通知"
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(SurfaceSoft),
            ) {
                SettingsRow(
                    title = "提醒",
                    subtitle = reminderSubtitle,
                    checked = uiState.reminderEnabled && canRemind,
                    onCheckedChange = { enabled ->
                        if (canRemind) viewModel.onReminderChange(enabled)
                    },
                )
                if (canRemind && uiState.reminderEnabled) {
                    HorizontalDivider(color = Hairline)
                    SettingsRow(
                        title = "提前提醒",
                        trailingText = AppSettings.reminderLabel(uiState.reminderBeforeMinutes),
                        onClick = { showLeadDialog = true },
                    )
                    HorizontalDivider(color = Hairline)
                    SettingsRow(
                        title = "开始时提醒",
                        subtitle = if (hasStart) "到达开始时间前提醒" else "请先设置开始时间",
                        checked = uiState.remindAtStart && hasStart,
                        onCheckedChange = { enabled ->
                            if (hasStart) viewModel.onRemindAtStartChange(enabled)
                        },
                    )
                    HorizontalDivider(color = Hairline)
                    SettingsRow(
                        title = "结束时提醒",
                        subtitle = if (hasEnd) "到达结束时间前提醒" else "请先设置结束时间",
                        checked = uiState.remindAtEnd && hasEnd,
                        onCheckedChange = { enabled ->
                            if (hasEnd) viewModel.onRemindAtEndChange(enabled)
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            PrimaryPillButton(
                text = "保存",
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
            )
            if (uiState.isEditing) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "删除事项",
                    style = MaterialTheme.typography.labelLarge,
                    color = PriorityUrgent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(PillShape)
                        .clickable { showDeleteConfirm = true }
                        .padding(vertical = 14.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    if (showLeadDialog) {
        AlertDialog(
            onDismissRequest = { showLeadDialog = false },
            title = { Text("提前提醒") },
            text = {
                Column {
                    AppSettings.ReminderMinuteOptions.forEach { minutes ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.onReminderBeforeMinutesChange(minutes)
                                    showLeadDialog = false
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = minutes == uiState.reminderBeforeMinutes,
                                onClick = {
                                    viewModel.onReminderBeforeMinutesChange(minutes)
                                    showLeadDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Rausch),
                            )
                            Text(
                                AppSettings.reminderLabel(minutes),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Ink,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLeadDialog = false }) { Text("取消") }
            },
        )
    }

    when (val target = pickerTarget) {
        PickerTarget.StartDate, PickerTarget.EndDate -> {
            val initial = when (target) {
                PickerTarget.StartDate -> uiState.startDate
                PickerTarget.EndDate -> uiState.endDate
                else -> null
            } ?: LocalDate.now(zone)
            DatePickerSheet(
                initialDate = initial,
                onDismiss = { pickerTarget = null },
                onConfirm = { date ->
                    when (target) {
                        PickerTarget.StartDate -> viewModel.onStartDateChange(date)
                        PickerTarget.EndDate -> viewModel.onEndDateChange(date)
                        else -> Unit
                    }
                    pickerTarget = null
                },
            )
        }
        PickerTarget.StartTime, PickerTarget.EndTime -> {
            val initial = when (target) {
                PickerTarget.StartTime -> uiState.startTime
                PickerTarget.EndTime -> uiState.endTime
                else -> null
            } ?: LocalTime.of(9, 0)
            TimePickerSheet(
                initialTime = initial,
                onDismiss = { pickerTarget = null },
                onConfirm = { time ->
                    when (target) {
                        PickerTarget.StartTime -> viewModel.onStartTimeChange(time)
                        PickerTarget.EndTime -> viewModel.onEndTimeChange(time)
                        else -> Unit
                    }
                    pickerTarget = null
                },
            )
        }
        PickerTarget.StartDateTime, PickerTarget.EndDateTime, null -> Unit
    }

    dateTimeSession?.let { session ->
        val pendingDate = session.pendingDate
        if (pendingDate == null) {
            val initial = when (session.target) {
                PickerTarget.StartDateTime -> uiState.startDate
                PickerTarget.EndDateTime -> uiState.endDate
                else -> null
            } ?: LocalDate.now(zone)
            DatePickerSheet(
                initialDate = initial,
                onDismiss = { dateTimeSession = null },
                onConfirm = { date ->
                    dateTimeSession = session.copy(pendingDate = date)
                },
            )
        } else {
            val initial = when (session.target) {
                PickerTarget.StartDateTime -> uiState.startTime
                PickerTarget.EndDateTime -> uiState.endTime
                else -> null
            } ?: LocalTime.of(9, 0)
            TimePickerSheet(
                initialTime = initial,
                onDismiss = { dateTimeSession = null },
                onConfirm = { time ->
                    when (session.target) {
                        PickerTarget.StartDateTime ->
                            viewModel.onStartDateTimeChange(pendingDate, time)
                        PickerTarget.EndDateTime ->
                            viewModel.onEndDateTimeChange(pendingDate, time)
                        else -> Unit
                    }
                    dateTimeSession = null
                },
            )
        }
    }
}

@Composable
private fun PickerField(
    label: String,
    value: String?,
    placeholder: String,
    clearable: Boolean,
    onClick: () -> Unit,
    onClear: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Muted)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(PillShape)
                .background(SurfaceStrong)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value ?: placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = if (value != null) Ink else MutedSoft,
                modifier = Modifier.weight(1f),
            )
            if (clearable && value != null) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier
                        .height(24.dp)
                        .width(24.dp),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "清除",
                        tint = Muted,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerSheet(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    // DatePicker uses UTC midnight millis for the selected day.
    val initialMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = state.selectedDateMillis ?: return@TextButton
                    val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    onConfirm(date)
                },
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    ) {
        DatePicker(state = state)
    }
}

@Composable
private fun TimePickerSheet(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    var hour by remember { mutableIntStateOf(initialTime.hour) }
    var minute by remember { mutableIntStateOf(initialTime.minute) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(LocalTime.of(hour, minute)) },
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TimeWheelPicker(
                    value = hour,
                    range = 0..23,
                    onValueChange = { hour = it },
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = ":",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Ink,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                TimeWheelPicker(
                    value = minute,
                    range = 0..59,
                    onValueChange = { minute = it },
                    modifier = Modifier.weight(1f),
                )
            }
        },
    )
}

/** Single looping wheel (0–23 / 0–59), not Material's dual-ring dial. */
@Composable
private fun TimeWheelPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val labels = remember(range) {
        range.map { String.format("%02d", it) }.toTypedArray()
    }
    AndroidView(
        modifier = modifier.height(160.dp),
        factory = { context ->
            NumberPicker(context).apply {
                // Set bounds before displayedValues.
                minValue = range.first
                maxValue = range.last
                displayedValues = labels
                this.value = value.coerceIn(range)
                wrapSelectorWheel = true
                descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                setOnValueChangedListener { _, _, newVal -> onValueChange(newVal) }
            }
        },
        update = { picker ->
            if (picker.value != value) picker.value = value.coerceIn(range)
            picker.setOnValueChangedListener { _, _, newVal -> onValueChange(newVal) }
        },
    )
}

@Composable
private fun ChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        color = if (selected) OnSoftPrimary else Ink,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .clip(PillShape)
            .background(if (selected) RauschSoft else SurfaceStrong)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

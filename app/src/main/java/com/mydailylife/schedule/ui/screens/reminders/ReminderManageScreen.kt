package com.mydailylife.schedule.ui.screens.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mydailylife.schedule.data.AppSettings
import com.mydailylife.schedule.reminder.UpcomingReminder
import com.mydailylife.schedule.ui.components.MdlTopAppBar
import com.mydailylife.schedule.ui.theme.Canvas
import com.mydailylife.schedule.ui.theme.Ink
import com.mydailylife.schedule.ui.theme.Muted
import com.mydailylife.schedule.ui.theme.Rausch
import com.mydailylife.schedule.ui.theme.SurfaceCard
import com.mydailylife.schedule.ui.theme.mdlCardSurface
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class RangePickTarget { Start, End }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderManageScreen(
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
    viewModel: ReminderManageViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    val timeFormatter = remember {
        DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
    }
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)
    }
    val shortDateFormatter = remember {
        DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)
    }
    var pickTarget by remember { mutableStateOf<RangePickTarget?>(null) }

    pickTarget?.let { target ->
        val initial = when (target) {
            RangePickTarget.Start -> uiState.rangeStart
            RangePickTarget.End -> uiState.rangeEnd
        }
        ReminderDatePickerDialog(
            initialDate = initial,
            onDismiss = { pickTarget = null },
            onConfirm = { date ->
                when (target) {
                    RangePickTarget.Start -> viewModel.setRangeStart(date)
                    RangePickTarget.End -> viewModel.setRangeEnd(date)
                }
                pickTarget = null
            },
        )
    }

    Scaffold(
        topBar = {
            MdlTopAppBar(title = "通知管理", onBack = onBack)
        },
        containerColor = Canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        when {
            !uiState.loaded -> {
                Text(
                    text = "加载中…",
                    color = Muted,
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp),
                )
            }
            !uiState.notificationsEnabled -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                ) {
                    Text(
                        text = "通知已关闭",
                        style = MaterialTheme.typography.titleMedium,
                        color = Ink,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "可在设置中重新开启本地提醒。开启后，这里会按日期列出即将发出的提醒。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted,
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item(key = "range") {
                        ReminderRangeBar(
                            rangeStart = uiState.rangeStart,
                            rangeEnd = uiState.rangeEnd,
                            dayCount = uiState.rangeDayCount,
                            totalCount = uiState.totalCount,
                            shortDateFormatter = shortDateFormatter,
                            onPickStart = { pickTarget = RangePickTarget.Start },
                            onPickEnd = { pickTarget = RangePickTarget.End },
                            onReset = { viewModel.resetRangeToDefault() },
                        )
                    }
                    if (uiState.groups.isEmpty()) {
                        item(key = "empty") {
                            Column(modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)) {
                                Text(
                                    text = "该范围内暂无待发提醒",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Ink,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "可调整上方起止日期，或为日程 / 课表开启提醒。",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Muted,
                                )
                            }
                        }
                    } else {
                        uiState.groups.forEach { group ->
                            item(key = "day-${group.date}") {
                                Text(
                                    text = formatDayLabel(group.date, today, dateFormatter),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Ink,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                                )
                            }
                            items(
                                items = group.entries,
                                key = { "${it.scheduleId}-${it.kind.storageKey}-${it.dueMillis}" },
                            ) { entry ->
                                ReminderManageRow(
                                    entry = entry,
                                    zone = zone,
                                    timeFormatter = timeFormatter,
                                    onClick = { onOpenItem(entry.scheduleId) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderRangeBar(
    rangeStart: LocalDate,
    rangeEnd: LocalDate,
    dayCount: Int,
    totalCount: Int,
    shortDateFormatter: DateTimeFormatter,
    onPickStart: () -> Unit,
    onPickEnd: () -> Unit,
    onReset: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .mdlCardSurface(fill = SurfaceCard)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = "展示范围",
            style = MaterialTheme.typography.labelLarge,
            color = Ink,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RangeDateChip(
                label = "开始",
                value = rangeStart.format(shortDateFormatter),
                onClick = onPickStart,
                modifier = Modifier.weight(1f),
            )
            Text("–", color = Muted)
            RangeDateChip(
                label = "结束",
                value = rangeEnd.format(shortDateFormatter),
                onClick = onPickEnd,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "共 $totalCount 条 · $dayCount 天",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
            Text(
                text = "恢复默认",
                style = MaterialTheme.typography.labelMedium,
                color = Rausch,
                modifier = Modifier.clickable(onClick = onReset),
            )
        }
    }
}

@Composable
private fun RangeDateChip(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .mdlCardSurface()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Muted)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, color = Ink)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
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
private fun ReminderManageRow(
    entry: UpcomingReminder,
    zone: ZoneId,
    timeFormatter: DateTimeFormatter,
    onClick: () -> Unit,
) {
    val triggerTime = Instant.ofEpochMilli(entry.triggerMillis).atZone(zone).toLocalTime()
    val dueTime = Instant.ofEpochMilli(entry.dueMillis).atZone(zone).toLocalTime()
    val leadLabel = AppSettings.reminderLabel(entry.leadMinutes)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .mdlCardSurface()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium,
                color = Ink,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = triggerTime.format(timeFormatter),
                style = MaterialTheme.typography.titleMedium,
                color = Ink,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${entry.kindLabel} ${dueTime.format(timeFormatter)} · 提前 $leadLabel · ${entry.tagLabel}",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
        )
    }
}

private fun formatDayLabel(
    date: LocalDate,
    today: LocalDate,
    formatter: DateTimeFormatter,
): String = when (date) {
    today -> "今天 · ${date.format(formatter)}"
    today.plusDays(1) -> "明天 · ${date.format(formatter)}"
    else -> date.format(formatter)
}

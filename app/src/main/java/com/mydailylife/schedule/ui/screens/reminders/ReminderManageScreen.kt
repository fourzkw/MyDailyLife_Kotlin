package com.mydailylife.schedule.ui.screens.reminders

import androidx.compose.foundation.background
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mydailylife.schedule.data.AppSettings
import com.mydailylife.schedule.reminder.ReminderTimes
import com.mydailylife.schedule.reminder.UpcomingReminder
import com.mydailylife.schedule.ui.components.MdlTopAppBar
import com.mydailylife.schedule.ui.theme.Canvas
import com.mydailylife.schedule.ui.theme.CardShape
import com.mydailylife.schedule.ui.theme.Ink
import com.mydailylife.schedule.ui.theme.Muted
import com.mydailylife.schedule.ui.theme.SurfaceSoft
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
            uiState.groups.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                ) {
                    Text(
                        text = "暂无待发提醒",
                        style = MaterialTheme.typography.titleMedium,
                        color = Ink,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "为仅一次、每天或每周事项开启提醒，并设置开始/结束时间后会出现在这里。",
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
                    item(key = "summary") {
                        Text(
                            text = "共 ${uiState.totalCount} 条 · 未来 ${ReminderTimes.MANAGE_LOOKAHEAD_DAYS} 天",
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
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
            .clip(CardShape)
            .background(SurfaceSoft)
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
            text = "${entry.kindLabel} ${dueTime.format(timeFormatter)} · 提前 $leadLabel · ${entry.timeMode.label}",
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

package com.mydailylife.schedule.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mydailylife.schedule.data.Priority
import com.mydailylife.schedule.data.ScheduleItem
import com.mydailylife.schedule.data.ScheduleTimeMode
import com.mydailylife.schedule.ui.components.DeleteScheduleDialog
import com.mydailylife.schedule.ui.components.PrimaryPillButton
import com.mydailylife.schedule.ui.components.ScheduleCard
import com.mydailylife.schedule.ui.components.SectionHeader
import com.mydailylife.schedule.ui.components.priorityColor
import com.mydailylife.schedule.ui.components.showBriefSnackbar
import com.mydailylife.schedule.ui.theme.Canvas
import com.mydailylife.schedule.ui.theme.Ink
import com.mydailylife.schedule.ui.theme.Muted
import com.mydailylife.schedule.ui.theme.OnPrimary
import com.mydailylife.schedule.ui.theme.Rausch
import com.mydailylife.schedule.ui.theme.ScreenHeaderToContent
import com.mydailylife.schedule.ui.theme.ScreenHorizontalPadding
import com.mydailylife.schedule.ui.theme.ScreenTopPadding
import com.mydailylife.schedule.ui.theme.SurfaceSoft
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    onCreate: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: CalendarViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()
    val month = uiState.month
    var pendingDelete by remember { mutableStateOf<ScheduleItem?>(null) }

    pendingDelete?.let { item ->
        val canSkipDay = item.timeModeEnum == ScheduleTimeMode.Daily ||
            item.timeModeEnum == ScheduleTimeMode.Weekly
        DeleteScheduleDialog(
            item = item,
            occurrenceDate = uiState.selectedDate.takeIf { canSkipDay },
            onDeleteThisDay = if (canSkipDay) {
                {
                    viewModel.deleteOccurrence(item.id, uiState.selectedDate)
                    pendingDelete = null
                    scope.launch { snackbar.showBriefSnackbar("已删除当天") }
                }
            } else {
                null
            },
            onDeleteEntire = {
                viewModel.delete(item.id)
                pendingDelete = null
                scope.launch { snackbar.showBriefSnackbar("已删除") }
            },
            onDismiss = { pendingDelete = null },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = ScreenHorizontalPadding),
    ) {
        Spacer(modifier = Modifier.height(ScreenTopPadding))
        SectionHeader(
            title = "日历",
            action = "今天",
            onAction = viewModel::goToday,
        )
        Spacer(modifier = Modifier.height(ScreenHeaderToContent))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = viewModel::previousMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上个月")
            }
            Text(
                text = "${month.year}年${month.monthValue}月",
                style = MaterialTheme.typography.displayMedium,
                color = Ink,
            )
            IconButton(onClick = viewModel::nextMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下个月")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = Muted,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        val firstDow = month.atDay(1).dayOfWeek.value
        val daysInMonth = month.lengthOfMonth()
        val cells = buildList {
            repeat(firstDow - 1) { add(null as LocalDate?) }
            for (d in 1..daysInMonth) add(month.atDay(d))
            while (size % 7 != 0) add(null)
            while (size < 42) add(null)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(SurfaceSoft)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            cells.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        CalendarDayCell(
                            date = date,
                            month = month,
                            today = today,
                            selected = date == uiState.selectedDate,
                            dots = date?.let { uiState.dayDots[it] }.orEmpty(),
                            onClick = { if (date != null) viewModel.selectDate(date) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "${uiState.selectedDate.month.getDisplayName(TextStyle.FULL, Locale.CHINA)}" +
                "${uiState.selectedDate.dayOfMonth}日",
            style = MaterialTheme.typography.titleLarge,
            color = Ink,
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (uiState.selectedItems.isEmpty()) {
            Text(
                text = "这一天暂无日程",
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
            )
            Spacer(modifier = Modifier.height(12.dp))
            PrimaryPillButton(text = "添加日程", onClick = onCreate)
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(uiState.selectedItems, key = { it.id }) { item ->
                    ScheduleCard(
                        item = item,
                        onClick = { onEdit(item.id) },
                        onLongClick = {
                            viewModel.toggleCompleted(item.id)
                            scope.launch {
                                snackbar.showBriefSnackbar(
                                    if (item.completed) "已恢复为未完成" else "已标记完成",
                                )
                            }
                        },
                        onDeleteClick = { pendingDelete = item },
                    )
                }
            }
        }
        SnackbarHost(hostState = snackbar)
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate?,
    month: YearMonth,
    today: LocalDate,
    selected: Boolean,
    dots: List<Priority>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .then(if (date != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (date != null) {
            val isToday = date == today
            val inMonth = YearMonth.from(date) == month
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                selected -> Rausch
                                isToday -> Canvas
                                else -> Color.Transparent
                            },
                        )
                        .then(
                            if (isToday && !selected) {
                                Modifier.border(1.dp, Rausch, CircleShape)
                            } else {
                                Modifier
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        color = when {
                            selected -> OnPrimary
                            !inMonth -> Muted.copy(alpha = 0.45f)
                            else -> Ink
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selected || isToday) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
                if (dots.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        dots.forEach { priority ->
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(priorityColor(priority)),
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

package com.mydailylife.schedule.ui.screens.schedule

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
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
import com.mydailylife.schedule.ui.components.MdlFab
import com.mydailylife.schedule.ui.components.MdlSearchPill
import com.mydailylife.schedule.ui.components.PrimaryPillButton
import com.mydailylife.schedule.ui.components.ScheduleCard
import com.mydailylife.schedule.ui.components.SectionHeader
import com.mydailylife.schedule.ui.components.bidirectionalSwipe
import com.mydailylife.schedule.ui.components.horizontalSwipe
import com.mydailylife.schedule.ui.components.priorityColor
import com.mydailylife.schedule.ui.theme.Canvas
import com.mydailylife.schedule.ui.theme.CardShape
import com.mydailylife.schedule.ui.theme.Ink
import com.mydailylife.schedule.ui.theme.Muted
import com.mydailylife.schedule.ui.theme.OnPrimary
import com.mydailylife.schedule.ui.theme.OnSoftPrimary
import com.mydailylife.schedule.ui.theme.Rausch
import com.mydailylife.schedule.ui.theme.RauschSoft
import com.mydailylife.schedule.ui.theme.SurfaceSoft
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private const val DaySlideMs = 280

private data class DayPage(
    val date: LocalDate,
    val pendingItems: List<ScheduleItem>,
    val completedItems: List<ScheduleItem>,
    val completedExpanded: Boolean,
    val query: String,
)

private data class WeekStripPage(
    val weekDates: List<LocalDate>,
    val selectedDate: LocalDate,
    val dayDots: Map<LocalDate, List<Priority>>,
)

@Composable
fun ScheduleScreen(
    onCreate: () -> Unit,
    onEdit: (String) -> Unit,
    onManageReminders: () -> Unit,
    viewModel: ScheduleViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val today = LocalDate.now()
    val dayPage = DayPage(
        date = uiState.selectedDate,
        pendingItems = uiState.pendingItems,
        completedItems = uiState.completedItems,
        completedExpanded = uiState.completedExpanded,
        query = uiState.query,
    )
    var pendingDelete by remember { mutableStateOf<ScheduleItem?>(null) }

    LaunchedEffect(uiState.message) {
        val msg = uiState.message ?: return@LaunchedEffect
        snackbar.showSnackbar(msg)
        viewModel.consumeMessage()
    }

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
                }
            } else {
                null
            },
            onDeleteEntire = {
                viewModel.delete(item.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = { MdlFab(onClick = onCreate) },
        containerColor = Canvas,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(
                title = "日程",
                action = "通知管理",
                onAction = onManageReminders,
            )
            Spacer(modifier = Modifier.height(12.dp))

            CalendarStrip(
                uiState = uiState,
                today = today,
                onPrevious = viewModel::previousPeriod,
                onNext = viewModel::nextPeriod,
                onToggleExpand = viewModel::toggleMonthExpanded,
                onExpand = viewModel::expandMonth,
                onCollapse = viewModel::collapseMonth,
                onSelectDate = viewModel::selectDate,
                onShiftDay = viewModel::shiftDay,
            )

            AnimatedContent(
                targetState = dayPage,
                contentKey = { it.date },
                transitionSpec = { daySlideTransition() },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
                    .horizontalSwipe(
                        onSwipeLeft = { viewModel.shiftDay(1) },
                        onSwipeRight = { viewModel.shiftDay(-1) },
                    ),
                label = "day-page",
            ) { page ->
                val hasAnyItems = page.pendingItems.isNotEmpty() || page.completedItems.isNotEmpty()
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = formatSelectedTitle(page.date, today),
                        style = MaterialTheme.typography.titleLarge,
                        color = Ink,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    MdlSearchPill(
                        query = page.query,
                        onQueryChange = viewModel::onQueryChange,
                        placeholder = "搜索当日事项",
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (!hasAnyItems) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "这一天暂无日程",
                                color = Muted,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            PrimaryPillButton(text = "添加日程", onClick = onCreate)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 88.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(page.pendingItems, key = { it.id }) { item ->
                                ScheduleCard(
                                    item = item,
                                    onClick = { onEdit(item.id) },
                                    onLongClick = { viewModel.toggleCompleted(item.id) },
                                    onDeleteClick = { pendingDelete = item },
                                )
                            }
                            if (page.completedItems.isNotEmpty()) {
                                item(key = "completed-header") {
                                    CompletedSectionHeader(
                                        count = page.completedItems.size,
                                        expanded = page.completedExpanded,
                                        onClick = viewModel::toggleCompletedExpanded,
                                    )
                                }
                                if (page.completedExpanded) {
                                    items(page.completedItems, key = { "done-${it.id}" }) { item ->
                                        ScheduleCard(
                                            item = item,
                                            onClick = { onEdit(item.id) },
                                            onLongClick = { viewModel.toggleCompleted(item.id) },
                                            onDeleteClick = { pendingDelete = item },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletedSectionHeader(
    count: Int,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "已完成 · $count",
            style = MaterialTheme.typography.titleMedium,
            color = Muted,
            fontWeight = FontWeight.SemiBold,
        )
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (expanded) "收起已完成" else "展开已完成",
            tint = Muted,
        )
    }
}

@Composable
private fun CalendarStrip(
    uiState: ScheduleUiState,
    today: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleExpand: () -> Unit,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onShiftDay: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SurfaceSoft)
            .bidirectionalSwipe(
                onSwipeLeft = { onShiftDay(1) },
                onSwipeRight = { onShiftDay(-1) },
                onSwipeUp = if (uiState.monthExpanded) onCollapse else null,
                onSwipeDown = if (!uiState.monthExpanded) onExpand else null,
            )
            .padding(horizontal = 4.dp, vertical = 8.dp),
    ) {
        AnimatedVisibility(
            visible = !uiState.monthExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedContent(
                    targetState = WeekStripPage(
                        weekDates = uiState.weekDates,
                        selectedDate = uiState.selectedDate,
                        dayDots = uiState.dayDots,
                    ),
                    contentKey = { it.weekDates.firstOrNull() },
                    transitionSpec = { weekSlideTransition() },
                    modifier = Modifier
                        .weight(1f)
                        .clipToBounds(),
                    label = "week-strip",
                ) { page ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        page.weekDates.forEach { date ->
                            WeekDayCell(
                                date = date,
                                today = today,
                                selected = date == page.selectedDate,
                                dots = page.dayDots[date].orEmpty(),
                                onClick = { onSelectDate(date) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "展开月历",
                        tint = OnSoftPrimary,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.monthExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onPrevious) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "上个月",
                        )
                    }
                    Text(
                        text = "${uiState.month.year}年${uiState.month.monthValue}月",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        color = Ink,
                        fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(onClick = onNext) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "下个月",
                        )
                    }
                }
                WeekdayHeader()
                Spacer(modifier = Modifier.height(4.dp))
                MonthGrid(
                    month = uiState.month,
                    today = today,
                    selectedDate = uiState.selectedDate,
                    dayDots = uiState.dayDots,
                    onSelectDate = onSelectDate,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    IconButton(onClick = onToggleExpand) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "收起月历",
                            tint = OnSoftPrimary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        weekdayLabels.forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = Muted,
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    selectedDate: LocalDate,
    dayDots: Map<LocalDate, List<Priority>>,
    onSelectDate: (LocalDate) -> Unit,
) {
    val firstDow = month.atDay(1).dayOfWeek.value
    val cells = buildList {
        repeat(firstDow - 1) { add(null as LocalDate?) }
        for (d in 1..month.lengthOfMonth()) add(month.atDay(d))
        while (size % 7 != 0) add(null)
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    DayCell(
                        date = date,
                        month = month,
                        today = today,
                        selected = date == selectedDate,
                        dots = date?.let { dayDots[it] }.orEmpty(),
                        forceInMonthStyle = false,
                        onClick = { if (date != null) onSelectDate(date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekDayCell(
    date: LocalDate,
    today: LocalDate,
    selected: Boolean,
    dots: List<Priority>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val weekday = weekdayLabels[date.dayOfWeek.value - 1]
    val isToday = date == today
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = weekday,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) OnSoftPrimary else Muted,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    when {
                        selected -> Rausch
                        isToday -> RauschSoft
                        else -> Color.Transparent
                    },
                )
                .then(
                    if (isToday && !selected) {
                        Modifier.border(1.dp, Rausch.copy(alpha = 0.5f), CircleShape)
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
                    isToday -> OnSoftPrimary
                    else -> Ink
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected || isToday) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
        if (dots.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                dots.take(3).forEach { priority ->
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

private val weekdayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

@Composable
private fun DayCell(
    date: LocalDate?,
    month: YearMonth,
    today: LocalDate,
    selected: Boolean,
    dots: List<Priority>,
    forceInMonthStyle: Boolean,
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
            val inMonth = forceInMonthStyle || YearMonth.from(date) == month
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                selected -> Rausch
                                isToday -> RauschSoft
                                else -> Color.Transparent
                            },
                        )
                        .then(
                            if (isToday && !selected) {
                                Modifier.border(1.dp, Rausch.copy(alpha = 0.5f), CircleShape)
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
                            !inMonth -> Muted.copy(alpha = 0.4f)
                            isToday -> OnSoftPrimary
                            else -> Ink
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selected || isToday) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
                if (dots.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        dots.take(3).forEach { priority ->
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

private fun formatSelectedTitle(date: LocalDate, today: LocalDate): String {
    val base = "${date.month.getDisplayName(TextStyle.FULL, Locale.CHINA)}${date.dayOfMonth}日"
    return when (date) {
        today -> "$base · 今天"
        today.plusDays(1) -> "$base · 明天"
        today.minusDays(1) -> "$base · 昨天"
        else -> base
    }
}

private fun AnimatedContentTransitionScope<DayPage>.daySlideTransition(): ContentTransform =
    horizontalSlideTransition(forward = targetState.date > initialState.date)

private fun AnimatedContentTransitionScope<WeekStripPage>.weekSlideTransition(): ContentTransform =
    horizontalSlideTransition(
        forward = (targetState.weekDates.firstOrNull() ?: targetState.selectedDate) >
            (initialState.weekDates.firstOrNull() ?: initialState.selectedDate),
    )

private fun horizontalSlideTransition(forward: Boolean): ContentTransform =
    if (forward) {
        (slideInHorizontally(animationSpec = tween(DaySlideMs)) { it / 3 } + fadeIn(tween(DaySlideMs))) togetherWith
            (slideOutHorizontally(animationSpec = tween(DaySlideMs)) { -it / 3 } + fadeOut(tween(DaySlideMs)))
    } else {
        (slideInHorizontally(animationSpec = tween(DaySlideMs)) { -it / 3 } + fadeIn(tween(DaySlideMs))) togetherWith
            (slideOutHorizontally(animationSpec = tween(DaySlideMs)) { it / 3 } + fadeOut(tween(DaySlideMs)))
    }

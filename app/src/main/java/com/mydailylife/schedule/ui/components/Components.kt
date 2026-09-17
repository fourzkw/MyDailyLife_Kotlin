package com.mydailylife.schedule.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mydailylife.schedule.data.Priority
import com.mydailylife.schedule.data.ScheduleItem
import com.mydailylife.schedule.data.ScheduleTimeFormat
import com.mydailylife.schedule.data.ScheduleTimeMode
import com.mydailylife.schedule.data.WeekdayLabels
import com.mydailylife.schedule.ui.theme.Body
import com.mydailylife.schedule.ui.theme.ButtonShape
import com.mydailylife.schedule.ui.theme.Canvas
import com.mydailylife.schedule.ui.theme.CardShape
import com.mydailylife.schedule.ui.theme.CompletionProgressEnd
import com.mydailylife.schedule.ui.theme.CompletionProgressStart
import com.mydailylife.schedule.ui.theme.Hairline
import com.mydailylife.schedule.ui.theme.Ink
import com.mydailylife.schedule.ui.theme.Muted
import com.mydailylife.schedule.ui.theme.MutedSoft
import com.mydailylife.schedule.ui.theme.OnPrimary
import com.mydailylife.schedule.ui.theme.OnSoftPrimary
import com.mydailylife.schedule.ui.theme.PillShape
import com.mydailylife.schedule.ui.theme.PriorityHigh
import com.mydailylife.schedule.ui.theme.PriorityLow
import com.mydailylife.schedule.ui.theme.PriorityMedium
import com.mydailylife.schedule.ui.theme.PriorityUrgent
import com.mydailylife.schedule.ui.theme.Rausch
import com.mydailylife.schedule.ui.theme.RauschSoft
import com.mydailylife.schedule.ui.theme.SurfaceSoft
import com.mydailylife.schedule.ui.theme.SurfaceStrong
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.hypot
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun MdlSearchPill(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索事项",
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(PillShape)
            .border(1.dp, Hairline, PillShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = Muted,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink),
            cursorBrush = SolidColor(Rausch),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = MutedSoft)
                }
                inner()
            },
        )
    }
}

@Composable
fun MdlFilterChips(
    labels: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 0.dp),
    ) {
        items(labels) { label ->
            val isSelected = label == selected
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) OnSoftPrimary else Body,
                modifier = Modifier
                    .clip(PillShape)
                    .background(if (isSelected) RauschSoft else SurfaceStrong)
                    .clickable { onSelected(label) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

private const val LongPressCompleteMs = 1000
private const val LongPressUncompleteMs = 500
/** Delay before long-press progress starts; also used to separate tap from hold. */
private const val LongPressArmDelayMs = 80L

private fun scheduleTimeSubtitle(item: ScheduleItem): String {
    val mode = item.timeModeEnum
    val modeLabel = when (mode) {
        ScheduleTimeMode.Weekly -> {
            val days = WeekdayLabels
                .filter { it.first.value in item.weekdays }
                .joinToString("") { it.second }
            if (days.isEmpty()) mode.label else "${mode.label} $days"
        }
        else -> mode.label
    }
    val start = ScheduleTimeFormat.formatDisplay(item.startTimeMillis)
    val end = ScheduleTimeFormat.formatDisplay(item.endTimeMillis)
    val range = when {
        start.isNotBlank() && end.isNotBlank() -> "$start – $end"
        start.isNotBlank() -> start
        end.isNotBlank() -> end
        else -> ""
    }
    return when {
        mode == ScheduleTimeMode.Unlimited -> modeLabel
        range.isBlank() -> modeLabel
        mode == ScheduleTimeMode.Once -> range
        else -> "$modeLabel · $range"
    }
}

@Composable
fun DeleteScheduleDialog(
    item: ScheduleItem,
    occurrenceDate: LocalDate? = null,
    onDeleteThisDay: (() -> Unit)? = null,
    onDeleteEntire: () -> Unit,
    onDismiss: () -> Unit,
) {
    val canSkipDay = onDeleteThisDay != null &&
        occurrenceDate != null &&
        (
            item.timeModeEnum == ScheduleTimeMode.Daily ||
                item.timeModeEnum == ScheduleTimeMode.Weekly
            )
    val dateLabel = occurrenceDate?.let {
        DateTimeFormatter.ofPattern("M月d日", Locale.CHINA).format(it)
    }.orEmpty()
    val seriesLabel = when (item.timeModeEnum) {
        ScheduleTimeMode.Daily -> "全部每天重复"
        ScheduleTimeMode.Weekly -> "全部每周重复"
        else -> "整条事项"
    }
    val bodyText = when {
        canSkipDay -> "这是重复事项，请选择删除范围："
        item.timeModeEnum == ScheduleTimeMode.Unlimited ->
            "该事项每天都会显示；删除后将从所有日期移除，此操作不可恢复。"
        else -> "确定删除该事项吗？此操作不可恢复。"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除「${item.title}」") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = bodyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (canSkipDay) Muted else Ink,
                )
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (canSkipDay) {
                    TextButton(onClick = onDeleteThisDay!!) {
                        Text("仅删除 $dateLabel", color = PriorityUrgent)
                    }
                    TextButton(onClick = onDeleteEntire) {
                        Text("删除$seriesLabel", color = PriorityUrgent)
                    }
                } else {
                    TextButton(onClick = onDeleteEntire) {
                        Text("删除", color = PriorityUrgent)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
fun ScheduleCard(
    item: ScheduleItem,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    /** Invoked on double-tap; typically opens the delete confirmation dialog. */
    onDeleteClick: (() -> Unit)? = null,
) {
    val timeText = scheduleTimeSubtitle(item)
    val priority = item.priorityEnum
    val progress = remember(item.id) { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val currentOnDeleteClick by rememberUpdatedState(onDeleteClick)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SurfaceSoft)
            .then(
                if (onClick != null || onLongClick != null || onDeleteClick != null) {
                    Modifier.pointerInput(item.id, item.completed) {
                        val touchSlop = viewConfiguration.touchSlop
                        val doubleTapTimeout = viewConfiguration.doubleTapTimeoutMillis
                        var pendingSingleTap: Job? = null
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var longPressFired = false
                            var longPressArmed = false
                            var scrolledAway = false
                            var lastDx = 0f
                            var lastDy = 0f
                            val holdMs = if (item.completed) {
                                LongPressUncompleteMs
                            } else {
                                LongPressCompleteMs
                            }
                            val pressJob = scope.launch {
                                if (currentOnLongClick == null) return@launch
                                delay(LongPressArmDelayMs)
                                if (scrolledAway) return@launch
                                longPressArmed = true
                                pendingSingleTap?.cancel()
                                pendingSingleTap = null
                                progress.snapTo(0f)
                                progress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(
                                        durationMillis = holdMs,
                                        easing = LinearEasing,
                                    ),
                                )
                                if (scrolledAway) return@launch
                                longPressFired = true
                                currentOnLongClick?.invoke()
                                delay(300)
                                progress.snapTo(0f)
                            }
                            try {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    if (!change.pressed) break
                                    val dx = change.position.x - down.position.x
                                    val dy = change.position.y - down.position.y
                                    lastDx = dx
                                    lastDy = dy
                                    if (!scrolledAway) {
                                        val adx = kotlin.math.abs(dx)
                                        val ady = kotlin.math.abs(dy)
                                        if (adx > touchSlop || ady > touchSlop) {
                                            scrolledAway = true
                                            pressJob.cancel()
                                            scope.launch { progress.snapTo(0f) }
                                        }
                                    }
                                }
                            } finally {
                                pressJob.cancel()
                            }

                            if (longPressFired) {
                                scope.launch { progress.snapTo(0f) }
                                return@awaitEachGesture
                            }
                            scope.launch { progress.snapTo(0f) }
                            if (longPressArmed || scrolledAway) return@awaitEachGesture

                            val moved = hypot(lastDx.toDouble(), lastDy.toDouble())
                            if (moved > touchSlop) return@awaitEachGesture

                            // Double-tap → delete; single tap (after timeout) → edit.
                            if (pendingSingleTap?.isActive == true) {
                                pendingSingleTap?.cancel()
                                pendingSingleTap = null
                                currentOnDeleteClick?.invoke()
                            } else if (currentOnDeleteClick != null) {
                                pendingSingleTap = scope.launch {
                                    delay(doubleTapTimeout)
                                    currentOnClick?.invoke()
                                }
                            } else {
                                currentOnClick?.invoke()
                            }
                        }
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        val progressValue = progress.value
        if (progressValue > 0f) {
            Box(modifier = Modifier.matchParentSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressValue.coerceIn(0f, 1f))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    CompletionProgressStart.copy(alpha = 0.35f),
                                    CompletionProgressEnd.copy(alpha = 0.45f),
                                ),
                            ),
                        ),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(PillShape)
                    .background(priorityColor(priority)),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (timeText.isNotBlank()) {
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSoft,
                        )
                    }
                    item.tags.take(3).forEach { tag ->
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = Body,
                            modifier = Modifier
                                .clip(PillShape)
                                .background(SurfaceStrong)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
            Text(
                text = priority.label,
                style = MaterialTheme.typography.labelSmall,
                color = priorityColor(priority),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}


@Composable
fun MdlFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "新建",
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        containerColor = Rausch,
        contentColor = OnPrimary,
    ) {
        Icon(Icons.Default.Add, contentDescription = contentDescription)
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.displaySmall,
            color = Ink,
            modifier = Modifier.weight(1f, fill = false),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (action != null && onAction != null) {
                Text(
                    text = action,
                    style = MaterialTheme.typography.labelMedium,
                    color = Rausch,
                    modifier = Modifier.clickable(onClick = onAction),
                )
            }
            trailing?.invoke()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MdlTopAppBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Canvas,
            titleContentColor = Ink,
            navigationIconContentColor = Ink,
            actionIconContentColor = Ink,
        ),
        // Parent NavHost Scaffold already applied status-bar insets.
        windowInsets = WindowInsets(0, 0, 0, 0),
    )
}

@Composable
fun PrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(PillShape)
            .background(if (enabled) Rausch else Rausch.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = OnPrimary)
    }
}

@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailingText: String? = null,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null && checked == null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        }
        when {
            checked != null && onCheckedChange != null -> {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Rausch,
                        checkedThumbColor = OnPrimary,
                    ),
                )
            }
            trailingText != null -> {
                Text(trailingText, style = MaterialTheme.typography.bodyMedium, color = Muted)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MutedSoft)
            }
            onClick != null -> {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MutedSoft)
            }
        }
    }
}

@Composable
fun FormFieldShell(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Muted)
        Spacer(modifier = Modifier.height(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink),
            cursorBrush = SolidColor(Rausch),
            modifier = Modifier
                .fillMaxWidth()
                .clip(ButtonShape)
                .border(1.dp, Hairline, ButtonShape)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            decorationBox = { inner ->
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = MutedSoft)
                }
                inner()
            },
        )
    }
}

fun priorityColor(priority: Priority) = when (priority) {
    Priority.Urgent -> PriorityUrgent
    Priority.High -> PriorityHigh
    Priority.Medium -> PriorityMedium
    Priority.Low -> PriorityLow
}

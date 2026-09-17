package com.mydailylife.schedule.ui.screens.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mydailylife.schedule.ui.components.PrimaryPillButton
import com.mydailylife.schedule.ui.components.SectionHeader
import com.mydailylife.schedule.ui.components.priorityColor
import com.mydailylife.schedule.ui.theme.CardShape
import com.mydailylife.schedule.ui.theme.Ink
import com.mydailylife.schedule.ui.theme.Muted
import com.mydailylife.schedule.ui.theme.PillShape
import com.mydailylife.schedule.ui.theme.Rausch
import com.mydailylife.schedule.ui.theme.ScreenHeaderToContent
import com.mydailylife.schedule.ui.theme.ScreenHorizontalPadding
import com.mydailylife.schedule.ui.theme.ScreenTopPadding
import com.mydailylife.schedule.ui.theme.SurfaceSoft
import com.mydailylife.schedule.ui.theme.SurfaceStrong
import kotlin.math.roundToInt

@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stats = uiState.stats
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenHorizontalPadding),
    ) {
        Spacer(modifier = Modifier.height(ScreenTopPadding))
        SectionHeader(title = "统计")
        Spacer(modifier = Modifier.height(ScreenHeaderToContent))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(SurfaceSoft)
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            OverviewCell(value = stats.total.toString(), label = "总日程")
            OverviewCell(value = stats.completed.toString(), label = "已完成", accent = true)
            OverviewCell(value = stats.urgent.toString(), label = "紧急待办")
            OverviewCell(value = stats.overdue.toString(), label = "已过期")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(SurfaceSoft)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${(stats.completionRate * 100).roundToInt()}%",
                    style = MaterialTheme.typography.displayLarge,
                    color = Rausch,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("完成率", style = MaterialTheme.typography.bodyMedium, color = Muted)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "已完成 ${stats.completed} · 待完成 ${stats.total - stats.completed}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("优先级分布", style = MaterialTheme.typography.titleLarge, color = Ink)
        Spacer(modifier = Modifier.height(12.dp))
        if (stats.priorityBars.isEmpty()) {
            Text("暂无数据", color = Muted, style = MaterialTheme.typography.bodyMedium)
        } else {
            stats.priorityBars.forEach { bar ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = bar.priority.label,
                        modifier = Modifier.width(40.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink,
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clip(PillShape)
                            .background(SurfaceStrong),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(bar.ratio.coerceIn(0f, 1f))
                                .height(10.dp)
                                .clip(PillShape)
                                .background(priorityColor(bar.priority)),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = bar.count.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("分类统计", style = MaterialTheme.typography.titleLarge, color = Ink)
        Spacer(modifier = Modifier.height(12.dp))
        if (stats.topTags.isEmpty()) {
            Text("暂无标签", color = Muted, style = MaterialTheme.typography.bodyMedium)
        } else {
            stats.topTags.forEach { tag ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = tag.name,
                        modifier = Modifier.width(72.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink,
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(PillShape)
                            .background(SurfaceStrong),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(tag.ratio.coerceIn(0f, 1f))
                                .height(8.dp)
                                .clip(PillShape)
                                .background(Rausch.copy(alpha = 0.45f)),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tag.count.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("近 7 日事项", style = MaterialTheme.typography.titleLarge, color = Ink)
        Spacer(modifier = Modifier.height(12.dp))
        val max = (stats.weekTrend.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            stats.weekTrend.forEach { day ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = day.count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Muted,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((100f * day.count / max).coerceAtLeast(if (day.count > 0) 8f else 2f).dp)
                            .clip(CardShape)
                            .background(
                                if (day.count > 0) Rausch.copy(alpha = 0.45f) else SurfaceStrong,
                            ),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = day.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = Muted,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        PrimaryPillButton(
            text = "重置统计（清空全部事项）",
            onClick = { showResetDialog = true },
        )
        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("确认重置") },
            text = { Text("将清空所有日程数据，此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetAllSchedules()
                        showResetDialog = false
                    },
                ) {
                    Text("清空", color = Rausch)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun OverviewCell(
    value: String,
    label: String,
    accent: Boolean = false,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = if (accent) Rausch else Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Muted,
            textAlign = TextAlign.Center,
        )
    }
}

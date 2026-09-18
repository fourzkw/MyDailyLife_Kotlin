package com.mydailylife.schedule.ui.screens.courses

import android.widget.NumberPicker
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mydailylife.schedule.data.CoursePeriod
import com.mydailylife.schedule.data.CoursePeriodSchedule
import com.mydailylife.schedule.ui.theme.Hairline
import com.mydailylife.schedule.ui.theme.Ink
import com.mydailylife.schedule.ui.theme.Muted
import com.mydailylife.schedule.ui.theme.SurfaceCard
import com.mydailylife.schedule.ui.theme.mdlCardSurface
import java.time.LocalTime

@Composable
fun CoursePeriodScheduleEditor(
    schedule: CoursePeriodSchedule,
    onScheduleChange: (CoursePeriodSchedule) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var editingField by remember { mutableStateOf<PeriodField?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "点某一节的起止时间即可修改。",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .mdlCardSurface(fill = SurfaceCard)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            schedule.periods.forEachIndexed { index, period ->
                if (index > 0) HorizontalDivider(color = Hairline, modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "第${index + 1}节",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink,
                        modifier = Modifier.weight(0.9f),
                    )
                    Text(
                        text = period.start,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                editingIndex = index
                                editingField = PeriodField.Start
                            }
                            .padding(vertical = 6.dp),
                    )
                    Text("–", color = Muted, modifier = Modifier.padding(horizontal = 4.dp))
                    Text(
                        text = period.end,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                editingIndex = index
                                editingField = PeriodField.End
                            }
                            .padding(vertical = 6.dp),
                    )
                }
            }
        }
    }

    val editIdx = editingIndex
    val field = editingField
    if (editIdx != null && field != null && editIdx in schedule.periods.indices) {
        val current = schedule.periods[editIdx]
        val initial = when (field) {
            PeriodField.Start -> current.startTime()
            PeriodField.End -> current.endTime()
        }
        PeriodTimePickerDialog(
            title = "第${editIdx + 1}节 · ${if (field == PeriodField.Start) "开始" else "结束"}",
            initialTime = initial,
            onDismiss = {
                editingIndex = null
                editingField = null
            },
            onConfirm = { time ->
                val hm = CoursePeriod.formatHm(time)
                val updated = when (field) {
                    PeriodField.Start -> current.copy(start = hm)
                    PeriodField.End -> current.copy(end = hm)
                }
                onScheduleChange(schedule.withPeriod(editIdx, updated))
                editingIndex = null
                editingField = null
            },
        )
    }
}

private enum class PeriodField { Start, End }

@Composable
private fun PeriodTimePickerDialog(
    title: String,
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    var hour by remember { mutableIntStateOf(initialTime.hour) }
    var minute by remember { mutableIntStateOf(initialTime.minute) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(hour, minute)) }) { Text("确定") }
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
                PeriodTimeWheel(
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
                PeriodTimeWheel(
                    value = minute,
                    range = 0..59,
                    onValueChange = { minute = it },
                    modifier = Modifier.weight(1f),
                )
            }
        },
    )
}

@Composable
private fun PeriodTimeWheel(
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

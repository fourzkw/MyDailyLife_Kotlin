package com.mydailylife.schedule.ui.screens.courses

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mydailylife.schedule.data.AcademicSemester
import com.mydailylife.schedule.data.CourseGridDefaults
import com.mydailylife.schedule.ui.theme.Ink
import com.mydailylife.schedule.ui.theme.Muted
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val termDateFmt = DateTimeFormatter.ofPattern("yyyy年M月d日（EEE）", Locale.CHINA)

@Composable
fun TermStartEditor(
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    caption: String = "教学第 1 周从所选日期所在周的周一起算。",
) {
    var showPicker by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val autumnYear = if (today.monthValue == 1) today.year - 1 else today.year
    val springYear = today.year

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "第一周起始：${selected.format(termDateFmt)}",
            style = MaterialTheme.typography.bodyMedium,
            color = Ink,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = caption,
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = { showPicker = true }) {
            Text("选择日期")
        }
        TextButton(
            onClick = {
                onSelect(
                    CourseGridDefaults.termStartForSemester(AcademicSemester.Autumn, autumnYear),
                )
            },
        ) {
            Text("秋季默认（${autumnYear}年9月第二周）")
        }
        TextButton(
            onClick = {
                onSelect(
                    CourseGridDefaults.termStartForSemester(AcademicSemester.Spring, springYear),
                )
            },
        ) {
            Text("春季默认（${springYear}年2月第二周）")
        }
    }

    if (showPicker) {
        CourseDatePickerDialog(
            initialDate = selected,
            onDismiss = { showPicker = false },
            onConfirm = { date ->
                showPicker = false
                onSelect(CourseGridDefaults.asTermStartMonday(date))
            },
        )
    }
}

@Composable
fun TermStartConfirmDialog(
    title: String,
    summary: String,
    initialTermStart: LocalDate = CourseGridDefaults.defaultTermStart(),
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    var termStart by remember {
        mutableStateOf(CourseGridDefaults.asTermStartMonday(initialTermStart))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(summary, style = MaterialTheme.typography.bodyMedium, color = Ink)
                Spacer(modifier = Modifier.height(12.dp))
                TermStartEditor(
                    selected = termStart,
                    onSelect = { termStart = it },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(termStart) }) { Text("导入") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDatePickerDialog(
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

package com.mydailylife.schedule.ui.screens.courses

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mydailylife.schedule.data.CourseGridDefaults
import com.mydailylife.schedule.data.CourseItem
import com.mydailylife.schedule.ui.theme.Ink
import com.mydailylife.schedule.ui.theme.Muted
import com.mydailylife.schedule.ui.theme.Rausch
import kotlin.math.max
import kotlin.math.min

/** Empty-cell draft range waiting for the user to tap + and fill course info. */
data class DraftSlot(
    val weekday: Int,
    val startSlot: Int,
    val endSlot: Int,
)

fun mergeDraftSlot(existing: List<DraftSlot>, incoming: DraftSlot): List<DraftSlot> {
    val others = existing.filter { it.weekday != incoming.weekday }
    val sameDay = (existing.filter { it.weekday == incoming.weekday } + incoming)
        .sortedBy { it.startSlot }
    val merged = mutableListOf<DraftSlot>()
    for (slot in sameDay) {
        val last = merged.lastOrNull()
        if (last != null && slot.startSlot <= last.endSlot + 1) {
            merged[merged.lastIndex] = last.copy(
                endSlot = max(last.endSlot, slot.endSlot),
                startSlot = min(last.startSlot, slot.startSlot),
            )
        } else {
            merged += slot
        }
    }
    return others + merged
}

enum class CourseRepeatMode { EveryWeek, ThisWeekOnly }

enum class CourseDeleteScope { ThisWeek, AllWeeks }

@Composable
fun AddCourseDialog(
    draft: DraftSlot,
    teachingWeek: Int,
    onDismiss: () -> Unit,
    onConfirm: (title: String, teacher: String, location: String, everyWeek: Boolean) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var teacher by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(CourseRepeatMode.EveryWeek) }
    val weekdayLabel = CourseGridDefaults.weekdayLabels.getOrNull(draft.weekday - 1) ?: "${draft.weekday}"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加课程") },
        text = {
            Column {
                Text(
                    text = "周$weekdayLabel · ${draft.startSlot}–${draft.endSlot} 节",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    label = { Text("课程名称") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    singleLine = true,
                    label = { Text("教师（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    singleLine = true,
                    label = { Text("地点（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("重复", style = MaterialTheme.typography.labelLarge, color = Ink)
                Spacer(modifier = Modifier.height(4.dp))
                RepeatOption(
                    selected = mode == CourseRepeatMode.EveryWeek,
                    title = "每周都有",
                    subtitle = "之后每周同一时段都显示",
                    onClick = { mode = CourseRepeatMode.EveryWeek },
                )
                RepeatOption(
                    selected = mode == CourseRepeatMode.ThisWeekOnly,
                    title = "仅一次（本周）",
                    subtitle = "只在第${teachingWeek}周出现",
                    onClick = { mode = CourseRepeatMode.ThisWeekOnly },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        title,
                        teacher,
                        location,
                        mode == CourseRepeatMode.EveryWeek,
                    )
                },
                enabled = title.isNotBlank(),
            ) { Text("添加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
fun EditCourseDialog(
    course: CourseItem,
    onDismiss: () -> Unit,
    onSave: (CourseItem) -> Unit,
) {
    var title by remember(course.id) { mutableStateOf(course.title) }
    var teacher by remember(course.id) { mutableStateOf(course.teacher) }
    var location by remember(course.id) { mutableStateOf(course.location) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑课程") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    label = { Text("课程名称") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    singleLine = true,
                    label = { Text("教师") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    singleLine = true,
                    label = { Text("地点") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        course.copy(
                            title = title.trim(),
                            teacher = teacher.trim(),
                            location = location.trim(),
                        ),
                    )
                },
                enabled = title.isNotBlank(),
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
fun DeleteCourseDialog(
    course: CourseItem,
    teachingWeek: Int,
    onDismiss: () -> Unit,
    onConfirm: (CourseDeleteScope) -> Unit,
) {
    var scope by remember { mutableStateOf(CourseDeleteScope.ThisWeek) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除课程") },
        text = {
            Column {
                Text(
                    text = "「${course.title}」· 第${teachingWeek}周",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
                Spacer(modifier = Modifier.height(8.dp))
                RepeatOption(
                    selected = scope == CourseDeleteScope.ThisWeek,
                    title = "仅删除本节（本周）",
                    subtitle = "其它周次的同一时段保留",
                    onClick = { scope = CourseDeleteScope.ThisWeek },
                )
                RepeatOption(
                    selected = scope == CourseDeleteScope.AllWeeks,
                    title = "删除所有同时段",
                    subtitle = "清除该课程全部周次",
                    onClick = { scope = CourseDeleteScope.AllWeeks },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(scope) }) { Text("删除") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun RepeatOption(
    selected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = Rausch),
        )
        Column(modifier = Modifier.padding(start = 4.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = Ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Muted)
        }
    }
}

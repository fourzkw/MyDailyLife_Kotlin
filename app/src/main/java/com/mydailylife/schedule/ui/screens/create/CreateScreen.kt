package com.mydailylife.schedule.ui.screens.create

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mydailylife.schedule.data.Priority
import com.mydailylife.schedule.data.ScheduleType
import com.mydailylife.schedule.ui.components.FormFieldShell
import com.mydailylife.schedule.ui.components.PrimaryPillButton
import com.mydailylife.schedule.ui.components.SettingsRow
import com.mydailylife.schedule.ui.theme.Ink
import com.mydailylife.schedule.ui.theme.Muted
import com.mydailylife.schedule.ui.theme.OnSoftPrimary
import com.mydailylife.schedule.ui.theme.PillShape
import com.mydailylife.schedule.ui.theme.RauschSoft
import com.mydailylife.schedule.ui.theme.SurfaceSoft
import com.mydailylife.schedule.ui.theme.SurfaceStrong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    onBack: () -> Unit,
    viewModel: CreateViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onBack()
    }
    LaunchedEffect(uiState.error) {
        val err = uiState.error ?: return@LaunchedEffect
        snackbar.showSnackbar(err)
        viewModel.consumeError()
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
            Text("类型", style = MaterialTheme.typography.labelMedium, color = Muted)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScheduleType.entries.forEach { type ->
                    val selected = type == uiState.type
                    Text(
                        text = type.label,
                        color = if (selected) OnSoftPrimary else Ink,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .clip(PillShape)
                            .background(if (selected) RauschSoft else SurfaceStrong)
                            .clickable { viewModel.onTypeChange(type) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
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
            if (uiState.type == ScheduleType.Schedule) {
                Spacer(modifier = Modifier.height(16.dp))
                FormFieldShell(
                    label = "开始时间",
                    value = uiState.startTimeText,
                    onValueChange = viewModel::onStartTimeChange,
                    placeholder = "yyyy-MM-dd HH:mm 或 今天 14:00",
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            FormFieldShell(
                label = if (uiState.type == ScheduleType.Task) "截止时间（可选）" else "结束时间",
                value = uiState.endTimeText,
                onValueChange = viewModel::onEndTimeChange,
                placeholder = "yyyy-MM-dd HH:mm",
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("优先级", style = MaterialTheme.typography.labelMedium, color = Muted)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.entries.forEach { p ->
                    val selected = p == uiState.priority
                    Text(
                        text = p.label,
                        color = if (selected) OnSoftPrimary else Ink,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .clip(PillShape)
                            .background(if (selected) RauschSoft else SurfaceStrong)
                            .clickable { viewModel.onPriorityChange(p) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
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
            Spacer(modifier = Modifier.height(8.dp))
            SettingsRow(
                title = "提醒",
                subtitle = "结束前通知（通知能力后续接入）",
                checked = uiState.reminderEnabled,
                onCheckedChange = viewModel::onReminderChange,
            )
            Spacer(modifier = Modifier.height(24.dp))
            PrimaryPillButton(
                text = "保存",
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

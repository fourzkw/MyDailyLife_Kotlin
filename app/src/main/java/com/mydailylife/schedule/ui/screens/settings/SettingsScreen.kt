package com.mydailylife.schedule.ui.screens.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mydailylife.schedule.BuildConfig
import com.mydailylife.schedule.asMdlApp
import com.mydailylife.schedule.data.AppSettings
import com.mydailylife.schedule.data.AppThemeId
import com.mydailylife.schedule.data.Priority
import com.mydailylife.schedule.reminder.ReminderPermission
import com.mydailylife.schedule.ui.components.SectionHeader
import com.mydailylife.schedule.ui.components.SettingsRow
import com.mydailylife.schedule.ui.components.showBriefSnackbar
import com.mydailylife.schedule.ui.theme.CardOutlined
import com.mydailylife.schedule.ui.theme.CardShape
import com.mydailylife.schedule.ui.theme.ChipOutlined
import com.mydailylife.schedule.ui.theme.BorderStrong
import com.mydailylife.schedule.ui.theme.Hairline
import com.mydailylife.schedule.ui.theme.Ink
import com.mydailylife.schedule.ui.theme.MdlDialogContainer
import com.mydailylife.schedule.ui.theme.Muted
import com.mydailylife.schedule.ui.theme.PillShape
import com.mydailylife.schedule.ui.theme.Rausch
import com.mydailylife.schedule.ui.theme.ScreenHeaderToContent
import com.mydailylife.schedule.ui.theme.ScreenHorizontalPadding
import com.mydailylife.schedule.ui.theme.ScreenTopPadding
import com.mydailylife.schedule.ui.theme.SurfaceSoft
import com.mydailylife.schedule.ui.theme.SurfaceStrong
import com.mydailylife.schedule.ui.theme.mdlCardSurface
import kotlinx.coroutines.launch

private enum class SettingsDialog {
    Reminder,
    Priority,
    Tags,
    Theme,
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val app = context.applicationContext.asMdlApp()
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }
    var newTag by remember { mutableStateOf("") }
    var showOpenSettingsDialog by remember { mutableStateOf(false) }
    var showExactAlarmDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.setNotificationsEnabled(true)
            if (!app.reminderScheduler.canScheduleExactAlarms()) {
                showExactAlarmDialog = true
            } else {
                scope.launch { snackbar.showBriefSnackbar("已开启通知提醒") }
            }
        } else {
            showOpenSettingsDialog = true
        }
    }

    fun enableNotifications() {
        when {
            ReminderPermission.hasNotificationPermission(context) -> {
                viewModel.setNotificationsEnabled(true)
                if (!app.reminderScheduler.canScheduleExactAlarms()) {
                    showExactAlarmDialog = true
                } else {
                    scope.launch { snackbar.showBriefSnackbar("已开启通知提醒") }
                }
            }
            ReminderPermission.needsNotificationPermission() -> {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            else -> {
                showOpenSettingsDialog = true
            }
        }
    }

    fun onNotificationsCheckedChange(enabled: Boolean) {
        if (enabled) {
            enableNotifications()
        } else {
            viewModel.setNotificationsEnabled(false)
            scope.launch { snackbar.showBriefSnackbar("已关闭通知提醒") }
        }
    }

    if (showOpenSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showOpenSettingsDialog = false },
            shape = CardShape,
            containerColor = MdlDialogContainer,
            title = { Text("需要通知权限") },
            text = {
                Text("请在系统设置中允许通知，以便准时提醒日程。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showOpenSettingsDialog = false
                        context.startActivity(ReminderPermission.appNotificationSettingsIntent(context))
                    },
                ) { Text("去设置") }
            },
            dismissButton = {
                TextButton(onClick = { showOpenSettingsDialog = false }) { Text("取消") }
            },
        )
    }

    if (showExactAlarmDialog) {
        AlertDialog(
            onDismissRequest = { showExactAlarmDialog = false },
            shape = CardShape,
            containerColor = MdlDialogContainer,
            title = { Text("建议开启精确闹钟") },
            text = {
                Text("系统限制了精确闹钟时，提醒可能略有延迟。可在设置中允许本应用使用闹钟与提醒。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExactAlarmDialog = false
                        context.startActivity(ReminderPermission.exactAlarmSettingsIntent(context))
                    },
                ) { Text("去开启") }
            },
            dismissButton = {
                TextButton(onClick = { showExactAlarmDialog = false }) { Text("稍后") }
            },
        )
    }

    when (dialog) {
        SettingsDialog.Reminder -> {
            ChoiceDialog(
                title = "默认提前提醒",
                options = AppSettings.ReminderMinuteOptions.map {
                    it to AppSettings.reminderLabel(it)
                },
                selectedKey = uiState.reminderBeforeMinutes,
                onDismiss = { dialog = null },
                onSelect = { minutes ->
                    viewModel.setReminderBeforeMinutes(minutes)
                    dialog = null
                },
            )
        }
        SettingsDialog.Priority -> {
            ChoiceDialog(
                title = "默认优先级",
                options = Priority.entries.map { it to it.label },
                selectedKey = uiState.defaultPriority,
                onDismiss = { dialog = null },
                onSelect = { priority ->
                    viewModel.setDefaultPriority(priority)
                    dialog = null
                },
            )
        }
        SettingsDialog.Theme -> {
            ChoiceDialog(
                title = "外观主题",
                options = AppThemeId.entries.map { it to "${it.label} · ${it.subtitle}" },
                selectedKey = uiState.themeId,
                onDismiss = { dialog = null },
                onSelect = { theme ->
                    viewModel.setThemeId(theme)
                    dialog = null
                    scope.launch { snackbar.showBriefSnackbar("已切换为${theme.label}") }
                },
            )
        }
        SettingsDialog.Tags -> {
            AlertDialog(
                onDismissRequest = {
                    dialog = null
                    newTag = ""
                },
                shape = CardShape,
                containerColor = MdlDialogContainer,
                title = { Text("标签管理") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (uiState.presetTags.isEmpty()) {
                            Text("暂无预设标签", color = Muted)
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                uiState.presetTags.forEach { tag ->
                                    Row(
                                        modifier = Modifier
                                            .clip(PillShape)
                                            .then(
                                                if (ChipOutlined) {
                                                    Modifier.border(1.dp, BorderStrong, PillShape)
                                                } else {
                                                    Modifier
                                                },
                                            )
                                            .background(
                                                if (ChipOutlined) {
                                                    androidx.compose.ui.graphics.Color.Transparent
                                                } else {
                                                    SurfaceStrong
                                                },
                                            )
                                            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(tag, style = MaterialTheme.typography.labelMedium, color = Ink)
                                        IconButton(
                                            onClick = { viewModel.removeTag(tag) },
                                            modifier = Modifier.height(28.dp),
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "删除 $tag",
                                                tint = Muted,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        OutlinedTextField(
                            value = newTag,
                            onValueChange = { newTag = it },
                            singleLine = true,
                            label = { Text("新标签") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.addTag(newTag)
                            newTag = ""
                        },
                    ) { Text("添加") }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            dialog = null
                            newTag = ""
                        },
                    ) { Text("完成") }
                },
            )
        }
        null -> Unit
    }

    val notificationSubtitle = when {
        !uiState.notificationsEnabled -> "关闭后不会弹出本地提醒"
        !ReminderPermission.hasNotificationPermission(context) ->
            "系统通知权限未开启，再次打开开关可重新授权"
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !app.reminderScheduler.canScheduleExactAlarms() ->
            "已启用；精确闹钟未授权，提醒可能延迟"
        else -> "已启用仅一次、每天、每周事项的本地提醒"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenHorizontalPadding),
        ) {
            Spacer(modifier = Modifier.height(ScreenTopPadding))
            SectionHeader(title = "设置")
            Spacer(modifier = Modifier.height(ScreenHeaderToContent))

            SettingsGroup(title = "通知") {
                SettingsRow(
                    title = "启用通知",
                    subtitle = notificationSubtitle,
                    checked = uiState.notificationsEnabled,
                    onCheckedChange = ::onNotificationsCheckedChange,
                )
                HorizontalDivider(color = Hairline)
                SettingsRow(
                    title = "默认提前提醒",
                    trailingText = uiState.reminderLabel,
                    onClick = { dialog = SettingsDialog.Reminder },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            SettingsGroup(title = "外观") {
                SettingsRow(
                    title = "主题",
                    subtitle = uiState.themeId.subtitle,
                    trailingText = uiState.themeLabel,
                    onClick = { dialog = SettingsDialog.Theme },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            SettingsGroup(title = "默认值") {
                SettingsRow(
                    title = "默认优先级",
                    trailingText = uiState.priorityLabel,
                    onClick = { dialog = SettingsDialog.Priority },
                )
                HorizontalDivider(color = Hairline)
                SettingsRow(
                    title = "标签管理",
                    subtitle = if (uiState.presetTags.isEmpty()) {
                        "暂无预设"
                    } else {
                        "${uiState.presetTags.size} 个预设"
                    },
                    onClick = { dialog = SettingsDialog.Tags },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            SettingsGroup(title = "关于") {
                val updateSubtitle = when (val update = uiState.update) {
                    AppUpdateUiState.Checking -> "正在检查…"
                    is AppUpdateUiState.Downloading -> {
                        if (update.progress < 0f) "正在下载…"
                        else "正在下载 ${(update.progress * 100).toInt()}%"
                    }
                    is AppUpdateUiState.Available -> "发现新版本 ${update.manifest.versionName}"
                    is AppUpdateUiState.Ready -> "已下载，等待安装"
                    is AppUpdateUiState.NeedsInstallPermission -> "需要允许安装未知应用"
                    else -> "点击检查更新"
                }
                SettingsRow(
                    title = "版本",
                    subtitle = updateSubtitle,
                    trailingText = BuildConfig.VERSION_NAME,
                    onClick = {
                        when (val update = uiState.update) {
                            is AppUpdateUiState.Available,
                            is AppUpdateUiState.Ready,
                            is AppUpdateUiState.NeedsInstallPermission,
                            -> Unit
                            is AppUpdateUiState.Downloading,
                            AppUpdateUiState.Checking,
                            -> Unit
                            else -> viewModel.checkForUpdate()
                        }
                    },
                )
                if (uiState.update is AppUpdateUiState.Downloading) {
                    val progress = (uiState.update as AppUpdateUiState.Downloading).progress
                    if (progress < 0f) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            color = Rausch,
                        )
                    } else {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            color = Rausch,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        SnackbarHost(hostState = snackbar, modifier = Modifier.padding(16.dp))
    }

    when (val update = uiState.update) {
        is AppUpdateUiState.Available -> {
            AlertDialog(
                onDismissRequest = {
                    if (!update.manifest.force) viewModel.dismissUpdateMessage()
                },
                title = { Text("发现新版本 ${update.manifest.versionName}") },
                text = {
                    Text(
                        update.manifest.changelog.ifBlank { "有可用更新，是否下载并安装？" },
                    )
                },
                confirmButton = {
                    TextButton(onClick = viewModel::downloadAndInstall) {
                        Text("下载并安装")
                    }
                },
                dismissButton = if (update.manifest.force) {
                    null
                } else {
                    {
                        TextButton(onClick = viewModel::dismissUpdateMessage) {
                            Text("稍后")
                        }
                    }
                },
            )
        }
        is AppUpdateUiState.NeedsInstallPermission -> {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("允许安装应用") },
                text = {
                    Text("请允许本应用安装未知来源应用，返回后再点「继续安装」。")
                },
                confirmButton = {
                    TextButton(onClick = viewModel::openInstallPermissionSettings) {
                        Text("去设置")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::downloadAndInstall) {
                        Text("继续安装")
                    }
                },
            )
        }
        AppUpdateUiState.UpToDate -> {
            LaunchedEffect(update) {
                snackbar.showBriefSnackbar("已是最新版本")
                viewModel.dismissUpdateMessage()
            }
        }
        is AppUpdateUiState.Error -> {
            LaunchedEffect(update.message) {
                snackbar.showBriefSnackbar(update.message)
                viewModel.dismissUpdateMessage()
            }
        }
        else -> Unit
    }
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selectedKey: T,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = CardShape,
        containerColor = MdlDialogContainer,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (key, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(key) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = key == selectedKey,
                            onClick = { onSelect(key) },
                            colors = RadioButtonDefaults.colors(selectedColor = Rausch),
                        )
                        Text(label, style = MaterialTheme.typography.bodyLarge, color = Ink)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = Muted,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .mdlCardSurface(),
    ) {
        content()
    }
}

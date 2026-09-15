package com.mydailylife.schedule.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mydailylife.schedule.ui.components.SectionHeader
import com.mydailylife.schedule.ui.components.SettingsRow
import com.mydailylife.schedule.ui.theme.CardShape
import com.mydailylife.schedule.ui.theme.Hairline
import com.mydailylife.schedule.ui.theme.Muted
import com.mydailylife.schedule.ui.theme.SurfaceSoft
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    var notifications by remember { mutableStateOf(true) }
    var courseAutoUpdate by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(title = "设置")
            Spacer(modifier = Modifier.height(16.dp))

            SettingsGroup(title = "通知") {
                SettingsRow(
                    title = "启用通知",
                    subtitle = "日程与课程本地提醒",
                    checked = notifications,
                    onCheckedChange = { notifications = it },
                )
                HorizontalDivider(color = Hairline)
                SettingsRow(
                    title = "默认提前提醒",
                    trailingText = "15 分钟",
                    onClick = {
                        scope.launch { snackbar.showSnackbar("设置持久化将在下一阶段接入") }
                    },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            SettingsGroup(title = "默认值") {
                SettingsRow(
                    title = "默认优先级",
                    trailingText = "中",
                    onClick = {
                        scope.launch { snackbar.showSnackbar("设置持久化将在下一阶段接入") }
                    },
                )
                HorizontalDivider(color = Hairline)
                SettingsRow(
                    title = "标签管理",
                    onClick = {
                        scope.launch { snackbar.showSnackbar("标签管理将在下一阶段接入") }
                    },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            SettingsGroup(title = "课表") {
                SettingsRow(
                    title = "自动更新课表",
                    subtitle = "按间隔刷新 ICS 订阅",
                    checked = courseAutoUpdate,
                    onCheckedChange = { courseAutoUpdate = it },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            SettingsGroup(title = "关于") {
                SettingsRow(title = "版本", trailingText = "1.0 (UI 壳)")
                HorizontalDivider(color = Hairline)
                SettingsRow(
                    title = "使用指南",
                    onClick = {
                        scope.launch { snackbar.showSnackbar("外链将在下一阶段接入") }
                    },
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        SnackbarHost(hostState = snackbar, modifier = Modifier.padding(16.dp))
    }
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
            .clip(CardShape)
            .background(SurfaceSoft),
    ) {
        content()
    }
}

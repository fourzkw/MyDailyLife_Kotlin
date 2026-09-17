package com.mydailylife.schedule.ui.screens.completed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mydailylife.schedule.data.ScheduleItem
import com.mydailylife.schedule.ui.components.DeleteScheduleDialog
import com.mydailylife.schedule.ui.components.MdlTopAppBar
import com.mydailylife.schedule.ui.components.ScheduleCard
import com.mydailylife.schedule.ui.components.showBriefSnackbar
import com.mydailylife.schedule.ui.theme.Muted
import com.mydailylife.schedule.ui.theme.PriorityUrgent
import com.mydailylife.schedule.ui.theme.ScreenHeaderToContent
import com.mydailylife.schedule.ui.theme.ScreenHorizontalPadding
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletedScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: CompletedViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pendingDelete by remember { mutableStateOf<ScheduleItem?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    pendingDelete?.let { item ->
        DeleteScheduleDialog(
            item = item,
            onDeleteEntire = {
                viewModel.delete(item.id)
                pendingDelete = null
                scope.launch { snackbar.showBriefSnackbar("已删除") }
            },
            onDismiss = { pendingDelete = null },
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清空已完成") },
            text = {
                Text("确定删除全部 ${uiState.items.size} 个已完成事项吗？此操作不可恢复。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        viewModel.clearCompleted()
                        scope.launch { snackbar.showBriefSnackbar("已清空完成事项") }
                    },
                ) {
                    Text("清空", color = PriorityUrgent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("取消")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            MdlTopAppBar(
                title = "已完成事项",
                onBack = onBack,
                actions = {
                    if (uiState.items.isNotEmpty()) {
                        TextButton(onClick = { showClearConfirm = true }) {
                            Text("清空", color = PriorityUrgent)
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = ScreenHorizontalPadding),
        ) {
            Spacer(modifier = Modifier.height(ScreenHeaderToContent))
            if (uiState.items.isEmpty()) {
                Text("暂无已完成事项", color = Muted)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(uiState.items, key = { it.id }) { item ->
                        ScheduleCard(
                            item = item,
                            onClick = { onEdit(item.id) },
                            onLongClick = {
                                viewModel.toggleCompleted(item.id)
                                scope.launch { snackbar.showBriefSnackbar("已恢复为未完成") }
                            },
                            onDeleteClick = { pendingDelete = item },
                        )
                    }
                }
            }
        }
    }
}

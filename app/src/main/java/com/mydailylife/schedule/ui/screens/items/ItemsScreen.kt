package com.mydailylife.schedule.ui.screens.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mydailylife.schedule.data.ScheduleItem
import com.mydailylife.schedule.data.ScheduleTimeMode
import com.mydailylife.schedule.ui.components.DeleteScheduleDialog
import com.mydailylife.schedule.ui.components.MdlFab
import com.mydailylife.schedule.ui.components.MdlFilterChips
import com.mydailylife.schedule.ui.components.MdlSearchPill
import com.mydailylife.schedule.ui.components.ScheduleCard
import com.mydailylife.schedule.ui.components.SectionHeader
import com.mydailylife.schedule.ui.components.showBriefSnackbar
import com.mydailylife.schedule.ui.theme.Muted
import com.mydailylife.schedule.ui.theme.ScreenHeaderToContent
import com.mydailylife.schedule.ui.theme.ScreenHorizontalPadding
import com.mydailylife.schedule.ui.theme.ScreenTopPadding
import java.time.LocalDate

@Composable
fun ItemsScreen(
    onCreate: () -> Unit,
    onEdit: (String) -> Unit,
    onOpenCompleted: () -> Unit,
    viewModel: ItemsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<ScheduleItem?>(null) }

    LaunchedEffect(uiState.message) {
        val msg = uiState.message ?: return@LaunchedEffect
        snackbar.showBriefSnackbar(msg)
        viewModel.consumeMessage()
    }

    pendingDelete?.let { item ->
        val today = LocalDate.now()
        val canSkipDay = item.timeModeEnum == ScheduleTimeMode.Daily ||
            item.timeModeEnum == ScheduleTimeMode.Weekly
        DeleteScheduleDialog(
            item = item,
            occurrenceDate = today.takeIf { canSkipDay },
            onDeleteThisDay = if (canSkipDay) {
                {
                    viewModel.deleteOccurrence(item.id, today)
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = ScreenHorizontalPadding),
        ) {
            Spacer(modifier = Modifier.height(ScreenTopPadding))
            SectionHeader(
                title = "事项",
                action = "已完成",
                onAction = onOpenCompleted,
            )
            Spacer(modifier = Modifier.height(ScreenHeaderToContent))
            MdlSearchPill(query = uiState.query, onQueryChange = viewModel::onQueryChange)
            Spacer(modifier = Modifier.height(12.dp))
            MdlFilterChips(
                labels = uiState.filterChips,
                selected = uiState.selectedFilter,
                onSelected = viewModel::onFilterSelected,
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (uiState.items.isEmpty()) {
                Text(
                    text = "暂无待办事项，点击右下角新建",
                    color = Muted,
                    modifier = Modifier.padding(top = 24.dp),
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 88.dp),
                ) {
                    items(uiState.items, key = { it.id }) { item ->
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

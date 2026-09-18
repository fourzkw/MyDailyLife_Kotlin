package com.mydailylife.schedule.ui.screens.courses

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mydailylife.schedule.data.academic.AcademicSchool
import com.mydailylife.schedule.data.academic.AcademicSchools
import com.mydailylife.schedule.ui.components.MdlTopAppBar
import com.mydailylife.schedule.ui.components.SettingsRow
import com.mydailylife.schedule.ui.theme.ButtonShape
import com.mydailylife.schedule.ui.theme.Canvas
import com.mydailylife.schedule.ui.theme.Hairline
import com.mydailylife.schedule.ui.theme.Ink
import com.mydailylife.schedule.ui.theme.Muted
import com.mydailylife.schedule.ui.theme.mdlCardSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicSchoolPickerScreen(
    onBack: () -> Unit,
    onSchoolSelected: (AcademicSchool) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val schools = remember(query) { AcademicSchools.filtered(query) }
    val sections = remember(schools) {
        schools.groupBy { it.initialLetter.uppercaseChar() }
            .toSortedMap()
    }

    Scaffold(
        topBar = {
            MdlTopAppBar(title = "选择学校", onBack = onBack)
        },
        containerColor = Canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                shape = ButtonShape,
                placeholder = { Text("搜索学校名称或拼音") },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = Muted)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (schools.isEmpty()) {
                Text(
                    text = if (query.isBlank()) "暂无已接入的学校" else "未找到匹配的学校",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    sections.forEach { (letter, items) ->
                        item(key = "header-$letter") {
                            Text(
                                text = letter.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                color = Muted,
                                modifier = Modifier.padding(top = 12.dp, bottom = 6.dp, start = 4.dp),
                            )
                        }
                        item(key = "group-$letter") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .mdlCardSurface(),
                            ) {
                                items.forEachIndexed { index, school ->
                                    if (index > 0) HorizontalDivider(color = Hairline)
                                    SettingsRow(
                                        title = school.name,
                                        subtitle = school.subtitle.ifBlank { null },
                                        trailingText = "导入",
                                        onClick = { onSchoolSelected(school) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

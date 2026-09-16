package com.mydailylife.schedule.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val Schedule = "schedule"
    const val Courses = "courses"
    const val Statistics = "statistics"
    const val Settings = "settings"
    const val Create = "create"
    const val CreateWithId = "create/{scheduleId}"
    const val Completed = "completed"
    const val Reminders = "reminders"

    val tabs = listOf(Schedule, Courses, Statistics, Settings)

    fun create(scheduleId: String? = null): String =
        if (scheduleId.isNullOrBlank()) Create else "create/$scheduleId"
}

data class TabDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

val tabDestinations = listOf(
    TabDestination(Routes.Schedule, "日程", Icons.Outlined.CalendarMonth),
    TabDestination(Routes.Courses, "课表", Icons.Outlined.GridView),
    TabDestination(Routes.Statistics, "统计", Icons.Outlined.BarChart),
    TabDestination(Routes.Settings, "设置", Icons.Outlined.Settings),
)

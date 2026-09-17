package com.mydailylife.schedule.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mydailylife.schedule.asMdlApp
import com.mydailylife.schedule.data.CourseScheduleBridge
import com.mydailylife.schedule.ui.screens.completed.CompletedScreen
import com.mydailylife.schedule.ui.screens.completed.CompletedViewModel
import com.mydailylife.schedule.ui.screens.courses.AcademicImportScreen
import com.mydailylife.schedule.ui.screens.courses.AcademicSchoolPickerScreen
import com.mydailylife.schedule.ui.screens.courses.CoursesScreen
import com.mydailylife.schedule.ui.screens.courses.CoursesViewModel
import com.mydailylife.schedule.ui.screens.create.CreateScreen
import com.mydailylife.schedule.ui.screens.create.CreateViewModel
import com.mydailylife.schedule.ui.screens.reminders.ReminderManageScreen
import com.mydailylife.schedule.ui.screens.reminders.ReminderManageViewModel
import com.mydailylife.schedule.ui.screens.schedule.ScheduleScreen
import com.mydailylife.schedule.ui.screens.schedule.ScheduleViewModel
import com.mydailylife.schedule.ui.screens.settings.SettingsScreen
import com.mydailylife.schedule.ui.screens.settings.SettingsViewModel
import com.mydailylife.schedule.ui.screens.statistics.StatisticsScreen
import com.mydailylife.schedule.ui.screens.statistics.StatisticsViewModel
import com.mydailylife.schedule.ui.theme.Muted
import com.mydailylife.schedule.ui.theme.RauschActive
import com.mydailylife.schedule.ui.theme.RauschSoft
import com.mydailylife.schedule.ui.theme.SurfaceSoft

@Composable
fun MdlNavHost(
    openScheduleId: String? = null,
    onOpenScheduleConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in Routes.tabs
    val app = LocalContext.current.applicationContext.asMdlApp()
    val repository = app.scheduleRepository

    LaunchedEffect(openScheduleId) {
        val id = openScheduleId ?: return@LaunchedEffect
        navController.navigate(Routes.create(id)) {
            launchSingleTop = true
        }
        onOpenScheduleConsumed()
    }
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = SurfaceSoft,
                    tonalElevation = 0.dp,
                ) {
                    tabDestinations.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                )
                            },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = RauschActive,
                                selectedTextColor = RauschActive,
                                unselectedIconColor = Muted,
                                unselectedTextColor = Muted,
                                indicatorColor = RauschSoft,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Schedule,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.Schedule) {
                val vm: ScheduleViewModel = viewModel(
                    factory = ScheduleViewModel.factory(
                        repository,
                        app.courseRepository,
                        app.settingsRepository,
                    ),
                )
                ScheduleScreen(
                    onCreate = { navController.navigate(Routes.create()) },
                    onEdit = { id -> navController.navigate(Routes.create(id)) },
                    onManageReminders = { navController.navigate(Routes.Reminders) },
                    viewModel = vm,
                )
            }
            composable(Routes.Courses) {
                val vm: CoursesViewModel = viewModel(
                    factory = CoursesViewModel.factory(
                        app.courseRepository,
                        app.settingsRepository,
                    ),
                )
                CoursesScreen(
                    onAcademicImport = { navController.navigate(Routes.AcademicSchoolPicker) },
                    viewModel = vm,
                )
            }
            composable(Routes.AcademicSchoolPicker) {
                AcademicSchoolPickerScreen(
                    onBack = { navController.popBackStack() },
                    onSchoolSelected = { school ->
                        navController.navigate(Routes.academicImport(school.id))
                    },
                )
            }
            composable(
                route = Routes.AcademicImport,
                arguments = listOf(
                    navArgument("schoolId") { type = NavType.StringType },
                ),
            ) { entry ->
                val schoolId = entry.arguments?.getString("schoolId").orEmpty()
                AcademicImportScreen(
                    schoolId = schoolId,
                    courseRepository = app.courseRepository,
                    onBack = { navController.popBackStack() },
                    onImported = {
                        navController.popBackStack(Routes.AcademicSchoolPicker, inclusive = true)
                    },
                )
            }
            composable(Routes.Statistics) {
                val vm: StatisticsViewModel = viewModel(
                    factory = StatisticsViewModel.factory(repository),
                )
                StatisticsScreen(viewModel = vm)
            }
            composable(Routes.Settings) {
                val vm: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.factory(app.settingsRepository),
                )
                SettingsScreen(viewModel = vm)
            }
            composable(Routes.Reminders) {
                val vm: ReminderManageViewModel = viewModel(
                    factory = ReminderManageViewModel.factory(
                        scheduleRepository = repository,
                        settingsRepository = app.settingsRepository,
                        courseRepository = app.courseRepository,
                    ),
                )
                ReminderManageScreen(
                    onBack = { navController.popBackStack() },
                    onOpenItem = { id ->
                        if (CourseScheduleBridge.isCourseItem(id)) {
                            navController.navigate(Routes.Courses) {
                                launchSingleTop = true
                            }
                        } else {
                            navController.navigate(Routes.create(id))
                        }
                    },
                    viewModel = vm,
                )
            }
            composable(Routes.Create) {
                val vm: CreateViewModel = viewModel(
                    factory = CreateViewModel.factory(
                        repository = repository,
                        settingsRepository = app.settingsRepository,
                        editId = null,
                    ),
                )
                CreateScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = vm,
                )
            }
            composable(
                route = Routes.CreateWithId,
                arguments = listOf(
                    navArgument("scheduleId") { type = NavType.StringType },
                ),
            ) { entry ->
                val id = entry.arguments?.getString("scheduleId")
                val vm: CreateViewModel = viewModel(
                    factory = CreateViewModel.factory(
                        repository = repository,
                        settingsRepository = app.settingsRepository,
                        editId = id,
                    ),
                )
                CreateScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = vm,
                )
            }
            composable(Routes.Completed) {
                val vm: CompletedViewModel = viewModel(
                    factory = CompletedViewModel.factory(repository),
                )
                CompletedScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(Routes.create(id)) },
                    viewModel = vm,
                )
            }
        }
    }
}

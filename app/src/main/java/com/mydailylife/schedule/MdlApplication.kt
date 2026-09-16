package com.mydailylife.schedule

import android.app.Application
import android.content.Context
import com.mydailylife.schedule.data.CourseRepository
import com.mydailylife.schedule.data.ScheduleRepository
import com.mydailylife.schedule.data.SettingsRepository
import com.mydailylife.schedule.reminder.NotificationHelper
import com.mydailylife.schedule.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MdlApplication : Application() {
    lateinit var scheduleRepository: ScheduleRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var courseRepository: CourseRepository
        private set
    lateinit var reminderScheduler: ReminderScheduler
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        scheduleRepository = ScheduleRepository(this)
        settingsRepository = SettingsRepository(this)
        courseRepository = CourseRepository(this)
        reminderScheduler = ReminderScheduler(this)
        NotificationHelper.ensureChannel(this)
        startReminderSync()
    }

    private fun startReminderSync() {
        appScope.launch {
            scheduleRepository.ensureLoaded()
            settingsRepository.ensureLoaded()
            combine(
                scheduleRepository.schedules,
                settingsRepository.settings,
            ) { items, settings ->
                items to settings
            }.collect { (items, settings) ->
                reminderScheduler.rescheduleAll(items, settings)
            }
        }
    }
}

fun Context.asMdlApp(): MdlApplication =
    (applicationContext as? MdlApplication)
        ?: error("Application must be MdlApplication")

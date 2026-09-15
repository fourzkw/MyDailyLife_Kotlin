package com.mydailylife.schedule

import android.app.Application
import android.content.Context
import com.mydailylife.schedule.data.ScheduleRepository

class MdlApplication : Application() {
    lateinit var scheduleRepository: ScheduleRepository
        private set

    override fun onCreate() {
        super.onCreate()
        scheduleRepository = ScheduleRepository(this)
    }
}

fun Context.asMdlApp(): MdlApplication =
    (applicationContext as? MdlApplication)
        ?: error("Application must be MdlApplication")

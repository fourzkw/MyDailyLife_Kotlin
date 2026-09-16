package com.mydailylife.schedule.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Starts the process after reboot so [com.mydailylife.schedule.MdlApplication] can
 * re-register alarms from persisted schedules.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
    }
}

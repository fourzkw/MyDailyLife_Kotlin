package com.mydailylife.schedule.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ReminderIntents.ACTION_FIRE) return
        val appContext = context.applicationContext
        val scheduleId = intent.getStringExtra(ReminderIntents.EXTRA_SCHEDULE_ID) ?: return
        val dueMillis = intent.getLongExtra(ReminderIntents.EXTRA_DUE_MILLIS, 0L)
        val title = intent.getStringExtra(ReminderIntents.EXTRA_TITLE).orEmpty()
            .ifBlank { "日程提醒" }
        val body = intent.getStringExtra(ReminderIntents.EXTRA_BODY).orEmpty()
            .ifBlank { "你有一个事项即将到期" }

        // Mark before showing so a reschedule race cannot fire again for the same due.
        ReminderScheduler(appContext).markFired(scheduleId, dueMillis)

        NotificationHelper.showReminder(
            context = appContext,
            scheduleId = scheduleId,
            title = title,
            body = body,
        )
    }
}

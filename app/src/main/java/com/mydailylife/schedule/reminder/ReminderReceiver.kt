package com.mydailylife.schedule.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mydailylife.schedule.asMdlApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ReminderIntents.ACTION_FIRE) return
        val appContext = context.applicationContext
        val scheduleId = intent.getStringExtra(ReminderIntents.EXTRA_SCHEDULE_ID) ?: return
        val kind = ReminderKind.fromStorage(intent.getStringExtra(ReminderIntents.EXTRA_KIND))
        val dueMillis = intent.getLongExtra(ReminderIntents.EXTRA_DUE_MILLIS, 0L)
        val title = intent.getStringExtra(ReminderIntents.EXTRA_TITLE).orEmpty()
            .ifBlank { "日程提醒" }
        val body = intent.getStringExtra(ReminderIntents.EXTRA_BODY).orEmpty()
            .ifBlank { "你有一个事项即将到期" }

        // Mark before showing so a reschedule race cannot fire again for the same due.
        val scheduler = ReminderScheduler(appContext)
        scheduler.markFired(scheduleId, kind, dueMillis)

        NotificationHelper.showReminder(
            context = appContext,
            scheduleId = scheduleId,
            kind = kind,
            title = title,
            body = body,
        )

        val pending = goAsync()
        val app = runCatching { appContext.asMdlApp() }.getOrNull()
        if (app == null) {
            pending.finish()
            return
        }
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                app.scheduleRepository.ensureLoaded()
                app.settingsRepository.ensureLoaded()
                app.reminderScheduler.rescheduleAll(
                    app.scheduleRepository.schedules.value,
                    app.settingsRepository.settings.value,
                )
            } finally {
                pending.finish()
            }
        }
    }
}

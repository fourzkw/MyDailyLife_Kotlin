package com.mydailylife.schedule.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.mydailylife.schedule.MainActivity
import com.mydailylife.schedule.R

object NotificationHelper {
    const val CHANNEL_ID = "schedule_reminders"
    private const val CHANNEL_NAME = "日程提醒"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService<NotificationManager>() ?: return
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "事项与上课前提醒"
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun areNotificationsEnabled(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun showReminder(
        context: Context,
        scheduleId: String,
        kind: ReminderKind = ReminderKind.End,
        title: String,
        body: String,
    ) {
        ensureChannel(context)
        if (!areNotificationsEnabled(context)) return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ReminderIntents.EXTRA_SCHEDULE_ID, scheduleId)
        }
        val contentPi = PendingIntent.getActivity(
            context,
            requestCode(scheduleId, kind),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(requestCode(scheduleId, kind), notification)
        }
    }

    fun requestCode(
        scheduleId: String,
        kind: ReminderKind = ReminderKind.End,
    ): Int = (scheduleId + kind.storageKey).hashCode() and 0x7fffffff
}

package com.mydailylife.schedule.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.getSystemService
import com.mydailylife.schedule.data.AppSettings
import com.mydailylife.schedule.data.ScheduleItem
import com.mydailylife.schedule.data.ScheduleTimeMode

/**
 * Schedules alarms for **Once** items with a due time.
 *
 * - If remaining time until due is greater than the lead window → fire at (due − lead).
 * - If remaining time is within the lead window (and due is still ahead) → fire ASAP once.
 * - Each (item id + due millis) pair is reminded at most once.
 */
class ReminderScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val alarmManager = appContext.getSystemService<AlarmManager>()

    fun rescheduleAll(items: List<ScheduleItem>, settings: AppSettings) {
        cancelAllTracked()
        pruneFiredFor(items)
        if (!settings.notificationsEnabled) return
        val now = System.currentTimeMillis()
        val scheduled = linkedSetOf<String>()
        items.forEach { item ->
            val due = dueMillis(item) ?: return@forEach
            if (hasFiredForDue(item.id, due)) return@forEach
            val triggerAt = computeTriggerMillis(item, now) ?: return@forEach
            if (schedule(item, due, triggerAt, now)) {
                scheduled += item.id
            }
        }
        prefs.edit().putStringSet(KEY_IDS, scheduled).apply()
    }

    fun markFired(scheduleId: String, dueMillis: Long) {
        if (scheduleId.isBlank() || dueMillis <= 0L) return
        prefs.edit().putLong(firedKey(scheduleId), dueMillis).apply()
    }

    fun cancelAllTracked() {
        val ids = prefs.getStringSet(KEY_IDS, emptySet()).orEmpty().toSet()
        ids.forEach { cancel(it) }
        prefs.edit().remove(KEY_IDS).apply()
    }

    fun cancel(scheduleId: String) {
        val pi = pendingIntent(
            scheduleId = scheduleId,
            dueMillis = 0L,
            title = "",
            body = "",
            create = false,
        ) ?: return
        alarmManager?.cancel(pi)
        pi.cancel()
    }

    fun canScheduleExactAlarms(): Boolean {
        val am = alarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.canScheduleExactAlarms()
        } else {
            true
        }
    }

    private fun schedule(
        item: ScheduleItem,
        dueMillis: Long,
        triggerAtMillis: Long,
        nowMillis: Long,
    ): Boolean {
        val am = alarmManager ?: return false
        val remainingMs = (dueMillis - nowMillis).coerceAtLeast(0L)
        val leadMs = item.reminderBeforeMinutes.coerceAtLeast(0) * 60_000L
        val body = when {
            remainingMs <= 0L -> "事项已到截止时间"
            leadMs > 0L && remainingMs <= leadMs -> "距离截止不足 ${formatRemaining(remainingMs)}"
            item.reminderBeforeMinutes > 0 ->
                "将在 ${AppSettings.reminderLabel(item.reminderBeforeMinutes)}后到期"
            else -> "事项即将到期"
        }
        val pi = pendingIntent(item.id, dueMillis, item.title, body, create = true) ?: return false
        val whenMs = triggerAtMillis.coerceAtLeast(nowMillis + IMMEDIATE_DELAY_MS)
        return runCatching {
            if (canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pi)
            }
            true
        }.getOrDefault(false)
    }

    private fun pendingIntent(
        scheduleId: String,
        dueMillis: Long,
        title: String,
        body: String,
        create: Boolean,
    ): PendingIntent? {
        val intent = Intent(appContext, ReminderReceiver::class.java).apply {
            action = ReminderIntents.ACTION_FIRE
            data = Uri.parse("mdl://reminder/$scheduleId")
            putExtra(ReminderIntents.EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(ReminderIntents.EXTRA_DUE_MILLIS, dueMillis)
            putExtra(ReminderIntents.EXTRA_TITLE, title)
            putExtra(ReminderIntents.EXTRA_BODY, body)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return if (create) {
            PendingIntent.getBroadcast(
                appContext,
                NotificationHelper.requestCode(scheduleId),
                intent,
                flags,
            )
        } else {
            PendingIntent.getBroadcast(
                appContext,
                NotificationHelper.requestCode(scheduleId),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }

    private fun hasFiredForDue(scheduleId: String, dueMillis: Long): Boolean =
        prefs.getLong(firedKey(scheduleId), 0L) == dueMillis

    private fun pruneFiredFor(items: List<ScheduleItem>) {
        val alive = items.map { it.id }.toSet()
        val editor = prefs.edit()
        prefs.all.keys
            .filter { it.startsWith(FIRED_PREFIX) }
            .forEach { key ->
                val id = key.removePrefix(FIRED_PREFIX)
                if (id !in alive) editor.remove(key)
            }
        editor.apply()
    }

    companion object {
        private const val PREFS = "schedule_reminders"
        private const val KEY_IDS = "scheduled_ids"
        private const val FIRED_PREFIX = "fired_"
        /** Small delay so AlarmManager accepts an "immediate" trigger. */
        private const val IMMEDIATE_DELAY_MS = 1_500L

        private fun firedKey(scheduleId: String) = FIRED_PREFIX + scheduleId

        fun dueMillis(item: ScheduleItem): Long? {
            if (item.completed) return null
            if (!item.reminderEnabled) return null
            if (item.timeModeEnum != ScheduleTimeMode.Once) return null
            return when {
                item.endTimeMillis > 0L -> item.endTimeMillis
                item.startTimeMillis > 0L -> item.startTimeMillis
                else -> null
            }
        }

        /**
         * @return trigger time, or null if no reminder should be scheduled.
         * Within the lead window (remaining ≤ lead, remaining > 0) → ASAP.
         * Before the lead window → (due − lead).
         */
        fun computeTriggerMillis(
            item: ScheduleItem,
            nowMillis: Long = System.currentTimeMillis(),
        ): Long? {
            val due = dueMillis(item) ?: return null
            val remaining = due - nowMillis
            if (remaining <= 0L) return null
            val leadMs = item.reminderBeforeMinutes.coerceAtLeast(0) * 60_000L
            return if (remaining <= leadMs) {
                nowMillis + IMMEDIATE_DELAY_MS
            } else {
                due - leadMs
            }
        }

        private fun formatRemaining(remainingMs: Long): String {
            val minutes = ((remainingMs + 59_999L) / 60_000L).coerceAtLeast(1L)
            return when {
                minutes < 60 -> "${minutes}分钟"
                minutes % 60L == 0L -> "${minutes / 60}小时"
                else -> "${minutes / 60}小时${minutes % 60}分钟"
            }
        }
    }
}

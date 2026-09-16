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
import java.time.ZoneId

/**
 * Schedules start/end alarms for Once, Daily, and Weekly items.
 *
 * - If remaining time until due is greater than the lead window → fire at (due − lead).
 * - If remaining time is within the lead window (and due is still ahead) → fire ASAP once.
 * - Each (item id + kind + due millis) triple is reminded at most once.
 */
class ReminderScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val alarmManager = appContext.getSystemService<AlarmManager>()
    private val zone: ZoneId = ZoneId.systemDefault()

    fun rescheduleAll(items: List<ScheduleItem>, settings: AppSettings) {
        cancelAllTracked()
        pruneFiredFor(items)
        if (!settings.notificationsEnabled) return
        val now = System.currentTimeMillis()
        val scheduled = linkedSetOf<String>()
        items.forEach { item ->
            ReminderTimes.slots(item, now, zone) { kind, due ->
                hasFired(item.id, kind, due)
            }.forEach { slot ->
                val triggerAt = ReminderTimes.computeTriggerMillis(
                    dueMillis = slot.dueMillis,
                    leadMinutes = item.reminderBeforeMinutes,
                    nowMillis = now,
                ) ?: return@forEach
                if (schedule(item, slot, triggerAt, now)) {
                    scheduled += trackKey(item.id, slot.kind)
                }
            }
        }
        prefs.edit().putStringSet(KEY_IDS, scheduled).apply()
    }

    fun markFired(scheduleId: String, kind: ReminderKind, dueMillis: Long) {
        if (scheduleId.isBlank() || dueMillis <= 0L) return
        prefs.edit().putLong(firedKey(scheduleId, kind), dueMillis).apply()
    }

    fun cancelAllTracked() {
        val ids = prefs.getStringSet(KEY_IDS, emptySet()).orEmpty().toSet()
        ids.forEach { key ->
            val (id, kind) = parseTrackKey(key) ?: return@forEach
            cancel(id, kind)
        }
        prefs.edit().remove(KEY_IDS).apply()
    }

    fun cancel(scheduleId: String, kind: ReminderKind) {
        val pi = pendingIntent(
            scheduleId = scheduleId,
            kind = kind,
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
        slot: ReminderSlot,
        triggerAtMillis: Long,
        nowMillis: Long,
    ): Boolean {
        val am = alarmManager ?: return false
        val remainingMs = (slot.dueMillis - nowMillis).coerceAtLeast(0L)
        val leadMs = item.reminderBeforeMinutes.coerceAtLeast(0) * 60_000L
        val pointLabel = when (slot.kind) {
            ReminderKind.Start -> "开始"
            ReminderKind.End -> "截止"
        }
        val body = when {
            remainingMs <= 0L -> "事项已到${pointLabel}时间"
            leadMs > 0L && remainingMs <= leadMs ->
                "距离${pointLabel}不足 ${ReminderTimes.formatRemaining(remainingMs)}"
            item.reminderBeforeMinutes > 0 ->
                "将在 ${AppSettings.reminderLabel(item.reminderBeforeMinutes)}后$pointLabel"
            else -> "事项即将$pointLabel"
        }
        val pi = pendingIntent(
            item.id,
            slot.kind,
            slot.dueMillis,
            item.title,
            body,
            create = true,
        ) ?: return false
        val whenMs = triggerAtMillis.coerceAtLeast(nowMillis + ReminderTimes.IMMEDIATE_DELAY_MS)
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
        kind: ReminderKind,
        dueMillis: Long,
        title: String,
        body: String,
        create: Boolean,
    ): PendingIntent? {
        val intent = Intent(appContext, ReminderReceiver::class.java).apply {
            action = ReminderIntents.ACTION_FIRE
            data = Uri.parse("mdl://reminder/$scheduleId/${kind.storageKey}")
            putExtra(ReminderIntents.EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(ReminderIntents.EXTRA_KIND, kind.storageKey)
            putExtra(ReminderIntents.EXTRA_DUE_MILLIS, dueMillis)
            putExtra(ReminderIntents.EXTRA_TITLE, title)
            putExtra(ReminderIntents.EXTRA_BODY, body)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val requestCode = NotificationHelper.requestCode(scheduleId, kind)
        return if (create) {
            PendingIntent.getBroadcast(appContext, requestCode, intent, flags)
        } else {
            PendingIntent.getBroadcast(
                appContext,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }

    private fun hasFired(scheduleId: String, kind: ReminderKind, dueMillis: Long): Boolean =
        prefs.getLong(firedKey(scheduleId, kind), 0L) == dueMillis

    private fun pruneFiredFor(items: List<ScheduleItem>) {
        val alive = items.map { it.id }.toSet()
        val editor = prefs.edit()
        prefs.all.keys
            .filter { it.startsWith(FIRED_PREFIX) }
            .forEach { key ->
                val rest = key.removePrefix(FIRED_PREFIX)
                val id = rest.substringBeforeLast('_', missingDelimiterValue = rest)
                if (id !in alive) editor.remove(key)
            }
        editor.apply()
    }

    companion object {
        private const val PREFS = "schedule_reminders"
        private const val KEY_IDS = "scheduled_ids"
        private const val FIRED_PREFIX = "fired_"

        private fun firedKey(scheduleId: String, kind: ReminderKind) =
            FIRED_PREFIX + scheduleId + "_" + kind.storageKey

        private fun trackKey(scheduleId: String, kind: ReminderKind) =
            "$scheduleId:${kind.storageKey}"

        private fun parseTrackKey(key: String): Pair<String, ReminderKind>? {
            val parts = key.split(':', limit = 2)
            if (parts.size != 2) {
                // Legacy single-id keys from Once-only scheduling.
                return key to ReminderKind.End
            }
            return parts[0] to ReminderKind.fromStorage(parts[1])
        }
    }
}

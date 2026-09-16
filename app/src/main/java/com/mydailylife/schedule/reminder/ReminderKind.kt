package com.mydailylife.schedule.reminder

enum class ReminderKind(val storageKey: String) {
    Start("start"),
    End("end");

    companion object {
        fun fromStorage(key: String?): ReminderKind =
            entries.find { it.storageKey == key } ?: End
    }
}

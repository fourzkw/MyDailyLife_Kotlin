package com.mydailylife.schedule.data

/**
 * App visual theme. Persist [storageKey] in [AppSettings.themeId].
 */
enum class AppThemeId(
    val storageKey: String,
    val label: String,
    val subtitle: String,
) {
    SoftCoral(
        storageKey = "soft_coral",
        label = "柔和珊瑚",
        subtitle = "浅灰画布 · 胶囊按钮",
    ),
    PaperCampus(
        storageKey = "paper_campus",
        label = "纸感校园",
        subtitle = "暖炭校刊风 · 直角票券 · 酒红按钮",
    ),
    ;

    companion object {
        val Default: AppThemeId = SoftCoral

        fun fromStorage(key: String?): AppThemeId =
            entries.find { it.storageKey == key } ?: Default
    }
}

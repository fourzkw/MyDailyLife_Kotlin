package com.mydailylife.schedule.data.update

import kotlinx.serialization.Serializable

@Serializable
data class UpdateManifest(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val force: Boolean = false,
    val changelog: String = "",
    val sha256: String? = null,
)

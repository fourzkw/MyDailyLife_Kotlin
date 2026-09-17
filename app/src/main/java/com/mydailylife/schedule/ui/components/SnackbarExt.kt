package com.mydailylife.schedule.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope

/** Default on-screen time for short tip snackbars (Material Short ≈ 4s is too long). */
const val BriefSnackbarMillis = 1_500L

/**
 * Shows [message] briefly then dismisses. Prefer this for tips like
 * 「课程请在课表页查看」so they don't block the UI for seconds.
 */
suspend fun SnackbarHostState.showBriefSnackbar(
    message: String,
    displayMillis: Long = BriefSnackbarMillis,
) {
    coroutineScope {
        val job = launch {
            showSnackbar(
                message = message,
                withDismissAction = false,
                duration = SnackbarDuration.Indefinite,
            )
        }
        delay(displayMillis.coerceAtLeast(400L))
        currentSnackbarData?.dismiss()
        job.join()
    }
}

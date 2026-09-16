package com.mydailylife.schedule

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mydailylife.schedule.ui.navigation.MdlNavHost
import com.mydailylife.schedule.ui.theme.Canvas
import com.mydailylife.schedule.ui.theme.MyDailyLifeTheme

@Composable
fun MdlApp(
    openScheduleId: String? = null,
    onOpenScheduleConsumed: () -> Unit = {},
) {
    MyDailyLifeTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Canvas,
        ) {
            MdlNavHost(
                openScheduleId = openScheduleId,
                onOpenScheduleConsumed = onOpenScheduleConsumed,
            )
        }
    }
}

package com.mydailylife.schedule

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mydailylife.schedule.ui.navigation.MdlNavHost
import com.mydailylife.schedule.ui.theme.Canvas
import com.mydailylife.schedule.ui.theme.Ink
import com.mydailylife.schedule.ui.theme.LocalMdlPalette
import com.mydailylife.schedule.ui.theme.MyDailyLifeTheme
import com.mydailylife.schedule.ui.theme.notebookRuledBackground

@Composable
fun MdlApp(
    openScheduleId: String? = null,
    onOpenScheduleConsumed: () -> Unit = {},
) {
    val app = LocalContext.current.asMdlApp()
    LaunchedEffect(Unit) {
        app.settingsRepository.ensureLoaded()
    }
    val settings by app.settingsRepository.settings.collectAsStateWithLifecycle()
    val themeId = settings.themeIdEnum

    MyDailyLifeTheme(themeId = themeId) {
        val palette = LocalMdlPalette.current
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (palette.notebookRuled) {
                        Modifier.notebookRuledBackground(Ink)
                    } else {
                        Modifier
                    },
                ),
            color = Canvas,
        ) {
            MdlNavHost(
                openScheduleId = openScheduleId,
                onOpenScheduleConsumed = onOpenScheduleConsumed,
            )
        }
    }
}

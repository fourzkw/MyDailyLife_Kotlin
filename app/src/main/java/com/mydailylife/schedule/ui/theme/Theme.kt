package com.mydailylife.schedule.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.mydailylife.schedule.data.AppThemeId

@Composable
fun MyDailyLifeTheme(
    themeId: AppThemeId = AppThemeId.Default,
    content: @Composable () -> Unit,
) {
    val palette = remember(themeId) { themeId.palette() }
    val shapes = remember(themeId) { themeId.shapes() }
    val typography = remember(themeId) { themeId.typography() }
    val colorScheme = remember(palette) {
        lightColorScheme(
            primary = palette.rausch,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.rauschSoft,
            onPrimaryContainer = palette.onSoftPrimary,
            secondary = palette.body,
            onSecondary = palette.onPrimary,
            secondaryContainer = palette.surfaceStrong,
            onSecondaryContainer = palette.ink,
            background = palette.canvas,
            onBackground = palette.ink,
            surface = palette.surfaceSoft,
            onSurface = palette.ink,
            surfaceVariant = palette.surfaceStrong,
            onSurfaceVariant = palette.muted,
            outline = palette.hairline,
            outlineVariant = palette.hairlineSoft,
            error = palette.error,
            onError = palette.onPrimary,
            errorContainer = palette.errorContainer,
            onErrorContainer = palette.onErrorContainer,
        )
    }

    val view = LocalView.current
    val lightStatusBars = palette.lightStatusBars
    SideEffect {
        val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = lightStatusBars
    }

    CompositionLocalProvider(
        LocalMdlPalette provides palette,
        LocalMdlShapes provides shapes,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes.material,
            content = content,
        )
    }
}

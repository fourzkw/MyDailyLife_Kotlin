package com.mydailylife.schedule.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Rausch,
    onPrimary = OnPrimary,
    primaryContainer = RauschSoft,
    onPrimaryContainer = OnSoftPrimary,
    secondary = Body,
    onSecondary = OnPrimary,
    secondaryContainer = SurfaceStrong,
    onSecondaryContainer = Ink,
    background = Canvas,
    onBackground = Ink,
    surface = SurfaceSoft,
    onSurface = Ink,
    surfaceVariant = SurfaceStrong,
    onSurfaceVariant = Muted,
    outline = Hairline,
    outlineVariant = HairlineSoft,
    error = Color(0xFFE07A6A),
    onError = OnPrimary,
    errorContainer = Color(0xFFFFEDEB),
    onErrorContainer = Color(0xFF8F3E34),
)

@Composable
fun MyDailyLifeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = MdlTypography,
        shapes = MdlShapes,
        content = content,
    )
}

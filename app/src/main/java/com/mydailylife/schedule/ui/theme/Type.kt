package com.mydailylife.schedule.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mydailylife.schedule.data.AppThemeId

private val Sans = FontFamily.SansSerif
private val Serif = FontFamily.Serif
private val Mono = FontFamily.Monospace

private fun baseTypography(
    displayFamily: FontFamily,
    titleFamily: FontFamily = Sans,
    labelLargeWeight: FontWeight = FontWeight.Medium,
    labelLargeTracking: Float = 0f,
    labelSmallFamily: FontFamily = Sans,
    labelSmallTracking: Float = 0f,
    displayTracking: Float = -0.18f,
): Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 40.sp,
        letterSpacing = displayTracking.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = displayTracking.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = displayTracking.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 30.sp,
        letterSpacing = displayTracking.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = titleFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = titleFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = labelLargeWeight,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = labelLargeTracking.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = labelSmallFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 13.sp,
        letterSpacing = labelSmallTracking.sp,
    ),
)

/** Soft coral — all sans, product UI. */
val SoftCoralTypography: Typography = baseTypography(displayFamily = Sans)

/**
 * Paper campus — Songti-like serif titles, bold tracked CTA labels,
 * monospace micro-labels.
 */
val PaperCampusTypography: Typography = baseTypography(
    displayFamily = Serif,
    titleFamily = Serif,
    labelLargeWeight = FontWeight.Bold,
    labelLargeTracking = 0.6f,
    labelSmallFamily = Mono,
    labelSmallTracking = 1.2f,
    displayTracking = -0.9f,
)

fun AppThemeId.typography(): Typography = when (this) {
    AppThemeId.SoftCoral -> SoftCoralTypography
    AppThemeId.PaperCampus -> PaperCampusTypography
}

/** Default / fallback; prefer [AppThemeId.typography] inside [MyDailyLifeTheme]. */
val MdlTypography: Typography = SoftCoralTypography

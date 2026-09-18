package com.mydailylife.schedule.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.mydailylife.schedule.data.AppThemeId

@Immutable
data class MdlPalette(
    val rausch: Color,
    val rauschActive: Color,
    val rauschDisabled: Color,
    val rauschSoft: Color,
    val ink: Color,
    val body: Color,
    val muted: Color,
    val mutedSoft: Color,
    val hairline: Color,
    val hairlineSoft: Color,
    val borderStrong: Color,
    val canvas: Color,
    val surfaceSoft: Color,
    val surfaceCard: Color,
    val surfaceStrong: Color,
    val onPrimary: Color,
    val onSoftPrimary: Color,
    val legalLink: Color,
    val priorityUrgent: Color,
    val priorityHigh: Color,
    val priorityMedium: Color,
    val priorityLow: Color,
    val completionProgressStart: Color,
    val completionProgressEnd: Color,
    val error: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    /** Light status/nav icon glyphs when true. */
    val lightStatusBars: Boolean = true,
    /** Draw ruled-notebook lines on the canvas. */
    val notebookRuled: Boolean = false,
)

/** Current default: soft coral on cool gray canvas. */
val SoftCoralPalette = MdlPalette(
    rausch = Color(0xFFFF8A9E),
    rauschActive = Color(0xFFFF6B85),
    rauschDisabled = Color(0xFFFFE8ED),
    rauschSoft = Color(0xFFFFF2F5),
    ink = Color(0xFF4A5568),
    body = Color(0xFF5C6678),
    muted = Color(0xFF8B95A5),
    mutedSoft = Color(0xFFAAB3C0),
    hairline = Color(0xFFE8ECF1),
    hairlineSoft = Color(0xFFF0F3F7),
    borderStrong = Color(0xFFD5DBE4),
    canvas = Color(0xFFF6F8FB),
    surfaceSoft = Color(0xFFFFFFFF),
    surfaceCard = Color(0xFFFFFFFF),
    surfaceStrong = Color(0xFFEEF2F6),
    onPrimary = Color(0xFFFFFFFF),
    onSoftPrimary = Color(0xFFC4475E),
    legalLink = Color(0xFF6B9FFF),
    priorityUrgent = Color(0xFFFF9AAB),
    priorityHigh = Color(0xFFFFB088),
    priorityMedium = Color(0xFFFFD78A),
    priorityLow = Color(0xFF8FD4A0),
    completionProgressStart = Color(0xFF10B981),
    completionProgressEnd = Color(0xFF34D399),
    error = Color(0xFFE07A6A),
    errorContainer = Color(0xFFFFEDEB),
    onErrorContainer = Color(0xFF8F3E34),
)

/**
 * Dark editorial “field notes” look:
 * warm charcoal ruled canvas, cream ink, wine CTA, sharp ticket chrome.
 */
val PaperCampusPalette = MdlPalette(
    // --wine / --wine-deep / soft wash
    rausch = Color(0xFFBC5B5D),
    rauschActive = Color(0xFFCF7468),
    rauschDisabled = Color(0xFF7A4548),
    rauschSoft = Color(0xFF4A3535),
    // --ink / cream text
    ink = Color(0xFFECE6DC),
    body = Color(0xFFF2EBE2),
    muted = Color(0xFFC4BBB0),
    mutedSoft = Color(0xFFA3998E),
    // --line / --line-strong / --stone
    hairline = Color(0xFF6F675E),
    hairlineSoft = Color(0xFF4A443E),
    borderStrong = Color(0xFF9A9186),
    // lifted charcoal (was #1D1B19) — closer to site section-dark #312B27
    canvas = Color(0xFF312B27),
    surfaceSoft = Color(0xFF3C3530),
    surfaceCard = Color(0xFF3C3530),
    surfaceStrong = Color(0xFF4A433C),
    onPrimary = Color(0xFFFFFAF2),
    onSoftPrimary = Color(0xFFF6EEE5),
    legalLink = Color(0xFFCF7468),
    priorityUrgent = Color(0xFFCF7468),
    priorityHigh = Color(0xFFD4A574),
    priorityMedium = Color(0xFFC9B88A),
    priorityLow = Color(0xFF8FA87A),
    completionProgressStart = Color(0xFF5F8F6A),
    completionProgressEnd = Color(0xFF8FA87A),
    error = Color(0xFFBC5B5D),
    errorContainer = Color(0xFF4A3535),
    onErrorContainer = Color(0xFFF6EEE5),
    lightStatusBars = false,
    notebookRuled = true,
)

fun AppThemeId.palette(): MdlPalette = when (this) {
    AppThemeId.SoftCoral -> SoftCoralPalette
    AppThemeId.PaperCampus -> PaperCampusPalette
}

val LocalMdlPalette = staticCompositionLocalOf { SoftCoralPalette }

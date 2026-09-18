package com.mydailylife.schedule.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/** Semantic colors — resolve from the active [LocalMdlPalette]. */

val Rausch: Color
    @Composable @ReadOnlyComposable get() = LocalMdlPalette.current.rausch

val RauschActive: Color
    @Composable @ReadOnlyComposable get() = LocalMdlPalette.current.rauschActive

val RauschDisabled: Color
    @Composable @ReadOnlyComposable get() = LocalMdlPalette.current.rauschDisabled

val RauschSoft: Color
    @Composable @ReadOnlyComposable get() = LocalMdlPalette.current.rauschSoft

val Ink: Color
    @Composable @ReadOnlyComposable get() = LocalMdlPalette.current.ink

val Body: Color
    @Composable @ReadOnlyComposable get() = LocalMdlPalette.current.body

val Muted: Color
    @Composable @ReadOnlyComposable get() = LocalMdlPalette.current.muted

val MutedSoft: Color
    @Composable @ReadOnlyComposable get() = LocalMdlPalette.current.mutedSoft

val Hairline: Color
    @Composable @ReadOnlyComposable get() = LocalMdlPalette.current.hairline

val HairlineSoft: Color
    @Composable @ReadOnlyComposable get() = LocalMdlPalette.current.hairlineSoft

val BorderStrong: Color
    @Composable @ReadOnlyComposable get() = LocalMdlPalette.current.borderStrong

val Canvas: Color
    @Composable @ReadOnlyComposable get() = LocalMdlPalette.current.canvas

val SurfaceSoft: Color
    @Composable @ReadOnlyComposable get() = LocalMdlPalette.current.surfaceSoft

val SurfaceCard: Color
    @Composable @ReadOnlyComposable get() = LocalMdlPalette.current.surfaceCard

val SurfaceStrong: Color
    @Composable @ReadOnlyComposable get() = LocalMdlPalette.current.surfaceStrong

val OnPrimary: Color
    @Composable @ReadOnlyComposable get() = LocalMdlPalette.current.onPrimary

val OnSoftPrimary: Color
    @Composable @ReadOnlyComposable get() = LocalMdlPalette.current.onSoftPrimary

val LegalLink: Color
    @Composable @ReadOnlyComposable get() = LocalMdlPalette.current.legalLink

val PriorityUrgent: Color
    @Composable @ReadOnlyComposable get() = LocalMdlPalette.current.priorityUrgent

val PriorityHigh: Color
    @Composable @ReadOnlyComposable get() = LocalMdlPalette.current.priorityHigh

val PriorityMedium: Color
    @Composable @ReadOnlyComposable get() = LocalMdlPalette.current.priorityMedium

val PriorityLow: Color
    @Composable @ReadOnlyComposable get() = LocalMdlPalette.current.priorityLow

val CompletionProgressStart: Color
    @Composable @ReadOnlyComposable get() = LocalMdlPalette.current.completionProgressStart

val CompletionProgressEnd: Color
    @Composable @ReadOnlyComposable get() = LocalMdlPalette.current.completionProgressEnd

package com.mydailylife.schedule.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.mydailylife.schedule.data.AppThemeId

/**
 * Per-theme geometry: cards, chips, primary CTA, FAB, fields.
 * SoftCoral = pills; PaperCampus = sharp editorial / ticket chrome (0 radius).
 */
@Immutable
data class MdlShapeTokens(
    val material: Shapes,
    /** Filter chips, tags, priority pills. */
    val chip: RoundedCornerShape,
    val card: RoundedCornerShape,
    /** Text fields / secondary controls. */
    val field: RoundedCornerShape,
    val primaryButton: RoundedCornerShape,
    val search: RoundedCornerShape,
    val fab: Shape,
    /**
     * When true, primary CTA is outline (ghost) instead of solid fill.
     * Landing primary is solid wine — keep false for PaperCampus.
     */
    val primaryButtonOutlined: Boolean,
    /** Cards / settings groups get a 1px editorial border. */
    val cardOutlined: Boolean,
    /** Chips use border + transparent fill (badge style). */
    val chipOutlined: Boolean,
)

private val Sharp = RoundedCornerShape(0.dp)

val SoftCoralShapes = MdlShapeTokens(
    material = Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(14.dp),
        large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(32.dp),
    ),
    chip = RoundedCornerShape(9999.dp),
    card = RoundedCornerShape(14.dp),
    field = RoundedCornerShape(8.dp),
    primaryButton = RoundedCornerShape(9999.dp),
    search = RoundedCornerShape(9999.dp),
    fab = CircleShape,
    primaryButtonOutlined = false,
    cardOutlined = false,
    chipOutlined = false,
)

val PaperCampusShapes = MdlShapeTokens(
    material = Shapes(
        extraSmall = Sharp,
        small = Sharp,
        medium = Sharp,
        large = Sharp,
        extraLarge = Sharp,
    ),
    chip = Sharp,
    card = Sharp,
    field = Sharp,
    primaryButton = Sharp,
    search = Sharp,
    fab = RectangleShape,
    primaryButtonOutlined = false,
    cardOutlined = true,
    chipOutlined = true,
)

fun AppThemeId.shapes(): MdlShapeTokens = when (this) {
    AppThemeId.SoftCoral -> SoftCoralShapes
    AppThemeId.PaperCampus -> PaperCampusShapes
}

val LocalMdlShapes = staticCompositionLocalOf { SoftCoralShapes }

/** Alias kept for call sites — resolves to theme chip shape. */
val PillShape: RoundedCornerShape
    @Composable @ReadOnlyComposable get() = LocalMdlShapes.current.chip

val CardShape: RoundedCornerShape
    @Composable @ReadOnlyComposable get() = LocalMdlShapes.current.card

val ButtonShape: RoundedCornerShape
    @Composable @ReadOnlyComposable get() = LocalMdlShapes.current.field

val PrimaryButtonShape: RoundedCornerShape
    @Composable @ReadOnlyComposable get() = LocalMdlShapes.current.primaryButton

val SearchShape: RoundedCornerShape
    @Composable @ReadOnlyComposable get() = LocalMdlShapes.current.search

val FabShape: Shape
    @Composable @ReadOnlyComposable get() = LocalMdlShapes.current.fab

val PrimaryButtonOutlined: Boolean
    @Composable @ReadOnlyComposable get() = LocalMdlShapes.current.primaryButtonOutlined

val CardOutlined: Boolean
    @Composable @ReadOnlyComposable get() = LocalMdlShapes.current.cardOutlined

val ChipOutlined: Boolean
    @Composable @ReadOnlyComposable get() = LocalMdlShapes.current.chipOutlined

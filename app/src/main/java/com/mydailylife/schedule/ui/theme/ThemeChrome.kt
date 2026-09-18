package com.mydailylife.schedule.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Card / settings group chrome — theme shape + optional editorial border. */
@Composable
fun Modifier.mdlCardSurface(
    fill: Color = SurfaceSoft,
    outlined: Boolean = CardOutlined,
): Modifier {
    val shape = CardShape
    return this
        .clip(shape)
        .then(if (outlined) Modifier.border(1.dp, Hairline, shape) else Modifier)
        .background(fill)
}

/** Text-field / picker row chrome. */
@Composable
fun Modifier.mdlFieldSurface(
    fill: Color = SurfaceStrong,
    outlined: Boolean = CardOutlined,
): Modifier {
    val shape = ButtonShape
    return this
        .clip(shape)
        .then(
            if (outlined) {
                Modifier.border(1.dp, BorderStrong, shape)
            } else {
                Modifier
            },
        )
        .background(fill)
}

/** Bottom sheet container shape — sharp for editorial, soft for coral. */
val MdlBottomSheetShape: Shape
    @Composable
    @ReadOnlyComposable
    get() = if (CardOutlined) {
        RectangleShape
    } else {
        RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    }

/** Dialog / sheet container fill. */
val MdlDialogContainer: Color
    @Composable
    @ReadOnlyComposable
    get() = SurfaceSoft

/**
 * Selectable option chip (priority / repeat / tags).
 * SoftCoral: filled pill; PaperCampus: outlined ticket badge.
 */
@Composable
fun MdlChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val outlined = ChipOutlined
    val shape = PillShape
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = when {
            outlined && selected -> Rausch
            outlined -> Muted
            selected -> OnSoftPrimary
            else -> Ink
        },
        modifier = modifier
            .clip(shape)
            .then(
                if (outlined) {
                    Modifier.border(
                        1.dp,
                        if (selected) Rausch else BorderStrong,
                        shape,
                    )
                } else {
                    Modifier
                },
            )
            .background(
                when {
                    outlined -> Color.Transparent
                    selected -> RauschSoft
                    else -> SurfaceStrong
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

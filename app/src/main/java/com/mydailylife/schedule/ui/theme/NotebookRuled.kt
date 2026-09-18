package com.mydailylife.schedule.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Faint horizontal ruled lines (notebook rhythm ≈ 48dp).
 */
fun Modifier.notebookRuledBackground(
    lineColor: Color,
    step: Dp = 48.dp,
    alpha: Float = 0.06f,
): Modifier = drawBehind {
    val stroke = 1.dp.toPx()
    val gap = step.toPx()
    var y = gap
    val color = lineColor.copy(alpha = alpha)
    while (y < size.height) {
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = stroke,
        )
        y += gap
    }
}

package com.mydailylife.schedule.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.horizontalSwipe(
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    thresholdPx: Float = 80f,
): Modifier = pointerInput(Unit) {
    var total = 0f
    detectHorizontalDragGestures(
        onDragStart = { total = 0f },
        onHorizontalDrag = { change, dragAmount ->
            change.consume()
            total += dragAmount
        },
        onDragEnd = {
            when {
                total > thresholdPx -> onSwipeRight()
                total < -thresholdPx -> onSwipeLeft()
            }
        },
    )
}

fun Modifier.verticalSwipe(
    onSwipeUp: (() -> Unit)? = null,
    onSwipeDown: (() -> Unit)? = null,
    thresholdPx: Float = 60f,
): Modifier = pointerInput(onSwipeUp, onSwipeDown) {
    var total = 0f
    detectVerticalDragGestures(
        onDragStart = { total = 0f },
        onVerticalDrag = { change, dragAmount ->
            change.consume()
            total += dragAmount
        },
        onDragEnd = {
            when {
                total > thresholdPx -> onSwipeDown?.invoke()
                total < -thresholdPx -> onSwipeUp?.invoke()
            }
        },
    )
}

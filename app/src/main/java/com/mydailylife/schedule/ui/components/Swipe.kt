package com.mydailylife.schedule.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs

fun Modifier.horizontalSwipe(
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    thresholdPx: Float = 80f,
): Modifier = pointerInput(onSwipeLeft, onSwipeRight, thresholdPx) {
    val touchSlop = viewConfiguration.touchSlop
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var totalX = 0f
        var lockedHorizontal = false
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull() ?: break
            if (!change.pressed) break
            // Child already consumed this pointer (e.g. nested gesture).
            if (change.isConsumed) return@awaitEachGesture

            val delta = change.positionChange()
            if (!lockedHorizontal) {
                val dx = change.position.x - down.position.x
                val dy = change.position.y - down.position.y
                if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                    if (abs(dy) >= abs(dx)) return@awaitEachGesture
                    lockedHorizontal = true
                    totalX = dx
                    change.consume()
                }
            } else {
                totalX += delta.x
                change.consume()
            }
        }
        if (lockedHorizontal) {
            when {
                totalX > thresholdPx -> onSwipeRight()
                totalX < -thresholdPx -> onSwipeLeft()
            }
        }
    }
}

fun Modifier.verticalSwipe(
    onSwipeUp: (() -> Unit)? = null,
    onSwipeDown: (() -> Unit)? = null,
    thresholdPx: Float = 60f,
): Modifier = pointerInput(onSwipeUp, onSwipeDown, thresholdPx) {
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

/**
 * Single gesture detector that routes horizontal vs vertical swipes,
 * so expand/collapse and day-shift do not steal each other.
 */
fun Modifier.bidirectionalSwipe(
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null,
    onSwipeUp: (() -> Unit)? = null,
    onSwipeDown: (() -> Unit)? = null,
    horizontalThresholdPx: Float = 64f,
    verticalThresholdPx: Float = 56f,
): Modifier = pointerInput(
    onSwipeLeft,
    onSwipeRight,
    onSwipeUp,
    onSwipeDown,
    horizontalThresholdPx,
    verticalThresholdPx,
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var total = Offset.Zero
        var lockedAxis: Axis? = null
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull() ?: break
            if (!change.pressed) break
            if (change.isConsumed) return@awaitEachGesture
            val delta = change.positionChange()
            total += delta
            if (lockedAxis == null) {
                val ax = abs(total.x)
                val ay = abs(total.y)
                if (ax > 12f || ay > 12f) {
                    lockedAxis = if (ax >= ay) Axis.Horizontal else Axis.Vertical
                }
            }
            if (lockedAxis != null) {
                change.consume()
            }
        }
        when (lockedAxis) {
            Axis.Horizontal -> when {
                total.x > horizontalThresholdPx -> onSwipeRight?.invoke()
                total.x < -horizontalThresholdPx -> onSwipeLeft?.invoke()
            }
            Axis.Vertical -> when {
                total.y > verticalThresholdPx -> onSwipeDown?.invoke()
                total.y < -verticalThresholdPx -> onSwipeUp?.invoke()
            }
            null -> Unit
        }
    }
}

private enum class Axis { Horizontal, Vertical }

package io.adambailey.framebeat.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** The thumb's radius: the browser range input's 20dp knob. */
private val THUMB_RADIUS = 10.dp

/**
 * An integer slider over [range], drawn like the web's range input with
 * `accent-sky-400`: a thin track filled in Bass Sky up to a round thumb.
 * Touch or drag anywhere on it to set the value. With keyboard focus, the
 * left and right arrows step it by one. [label] names it for screen readers.
 */
@Composable
fun Slider(value: Int, range: IntRange, onValueChange: (Int) -> Unit, label: String, modifier: Modifier = Modifier) {
    val change by rememberUpdatedState(onValueChange)
    // The key handler steps from the newest value, even before a recomposition catches up.
    val current by rememberUpdatedState(value)
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val span = (range.last - range.first).coerceAtLeast(1)

    Box(
        modifier
            .height(32.dp)
            .semantics {
                contentDescription = label
                // Read the number, not a percentage.
                stateDescription = "$value"
                progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), range.first.toFloat()..range.last.toFloat(), steps = span - 1)
                setProgress { target ->
                    change(target.roundToInt().coerceIn(range))
                    true
                }
            }
            .pointerInput(range) {
                val radius = THUMB_RADIUS.toPx()
                fun at(x: Float) = range.first + (((x - radius) / (size.width - 2 * radius).coerceAtLeast(1f)).coerceIn(0f, 1f) * span).roundToInt()
                // Only a tap or a sideways drag sets the value: an up-and-down
                // drag that starts on the slider still scrolls the column.
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { c, _ -> c.consume() }
                    if (drag != null) {
                        change(at(drag.position.x))
                        horizontalDrag(drag.id) {
                            it.consume()
                            change(at(it.position.x))
                        }
                    } else {
                        val end = currentEvent.changes.firstOrNull { it.id == down.id }
                        if (end != null && end.changedToUp() && !end.isConsumed) {
                            end.consume()
                            change(at(down.position.x))
                        }
                    }
                }
            }
            .onKeyEvent {
                if (it.type != KeyEventType.KeyDown) return@onKeyEvent false
                val step = when (it.key) {
                    Key.DirectionLeft, Key.DirectionDown -> -1
                    Key.DirectionRight, Key.DirectionUp -> 1
                    else -> return@onKeyEvent false
                }
                change((current + step).coerceIn(range))
                true
            }
            .focusable(interactionSource = interaction)
            .drawBehind {
                val radius = THUMB_RADIUS.toPx()
                val track = 4.dp.toPx()
                val y = size.height / 2
                val thumbX = radius + (size.width - 2 * radius) * (value - range.first) / span
                drawRoundRect(Palette.ControlFillPressed, Offset(0f, y - track / 2), Size(size.width, track), CornerRadius(track / 2))
                drawRoundRect(Palette.BassSky, Offset(0f, y - track / 2), Size(thumbX, track), CornerRadius(track / 2))
                drawCircle(Palette.BassSky, radius, Offset(thumbX, y))
                if (focused) drawCircle(Palette.BassSky, radius + 4.dp.toPx(), Offset(thumbX, y), style = Stroke(2.dp.toPx()))
            },
    )
}

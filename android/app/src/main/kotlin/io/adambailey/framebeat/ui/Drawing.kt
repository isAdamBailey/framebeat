package io.adambailey.framebeat.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.util.lerp
import kotlin.math.hypot
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * The value at progress [p] through keyframes given as (offset, value) pairs,
 * interpolated linearly between them. The Web Animations API does the same when
 * the easing is set on the whole animation, so pass [p] already eased.
 */
internal fun keyframeAt(p: Float, vararg frames: Pair<Float, Float>): Float {
    if (p <= frames.first().first) return frames.first().second
    for (i in 1 until frames.size) {
        val (t1, v1) = frames[i]
        if (p <= t1) {
            val (t0, v0) = frames[i - 1]
            return lerp(v0, v1, (p - t0) / (t1 - t0))
        }
    }
    return frames.last().second
}

/** CSS `linear-gradient(<angle>deg, ...)` across [r]: 0° points up, 90° right, and the line reaches both far corners. */
internal fun cssLinearGradient(angle: Float, r: Rect, vararg stops: Pair<Float, Color>): Brush {
    val rad = Math.toRadians(angle.toDouble()).toFloat()
    val dir = Offset(sin(rad), -cos(rad))
    val half = (abs(r.width * dir.x) + abs(r.height * dir.y)) / 2
    return Brush.linearGradient(*stops, start = r.center - dir * half, end = r.center + dir * half)
}

/** A blurred ellipse: solid in the middle, fading to nothing [blur] past [r]'s edge. */
internal fun DrawScope.softOval(r: Rect, color: Color, blur: Float) {
    val outer = r.inflate(blur)
    // Where [r]'s own edge falls along the gradient: a blur leaves it at half strength.
    val edge = (1 - 2 * blur / outer.width).coerceIn(0f, 1f)
    scale(1f, outer.height / outer.width, pivot = outer.topLeft) {
        drawOval(
            Brush.radialGradient(
                0f to color,
                edge to color.copy(alpha = color.alpha * 0.5f),
                1f to Color.Transparent,
                center = outer.topLeft + Offset(outer.width / 2, outer.width / 2),
                radius = outer.width / 2,
            ),
            outer.topLeft,
            Size(outer.width, outer.width),
        )
    }
}

/** An ellipse filling [r] with a CSS `radial-gradient(ellipse at focus, ...)`, sized to the farthest corner. */
internal fun DrawScope.ellipticalRadial(r: Rect, focus: Offset, vararg stops: Pair<Float, Color>) {
    // Draw a circle-shaped gradient in a square, then squash the square to the box.
    scale(1f, r.height / r.width, pivot = r.topLeft) {
        val center = r.topLeft + Offset(r.width * focus.x, r.width * focus.y)
        val reach = r.width * hypot(maxOf(focus.x, 1 - focus.x), maxOf(focus.y, 1 - focus.y))
        drawOval(Brush.radialGradient(*stops, center = center, radius = reach), r.topLeft, Size(r.width, r.width))
    }
}

package io.adambailey.framebeat.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.util.lerp
import android.graphics.Matrix
import kotlin.math.sqrt
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

/**
 * CSS `radial-gradient(ellipse farthest-corner at focus, ...)` over [r]: an
 * ellipse with the farthest side's proportions, grown to pass through the
 * farthest corner. [focus] is a fraction of [r].
 */
internal fun cssRadialGradient(r: Rect, focus: Offset, vararg stops: Pair<Float, Color>): Brush = object : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val center = Offset(r.left + r.width * focus.x, r.top + r.height * focus.y)
        val rx = r.width * maxOf(focus.x, 1 - focus.x) * SQRT2
        val ry = r.height * maxOf(focus.y, 1 - focus.y) * SQRT2
        return RadialGradientShader(center, rx, stops.map { it.second }, stops.map { it.first }).apply {
            setLocalMatrix(Matrix().apply { setScale(1f, ry / rx, center.x, center.y) })
        }
    }
}

private val SQRT2 = sqrt(2f)

/**
 * A ring outside [shape], as Tailwind's `ring` draws one: a [width] stroke of
 * [color], [gap] past the edge (`ring-offset`). [show] is read while drawing,
 * so turning the ring on or off redraws without recomposing. Put it before any clip.
 */
internal fun Modifier.outerRing(
    shape: Shape,
    color: Color,
    width: Dp = 2.dp,
    gap: Dp = 0.dp,
    show: () -> Boolean,
): Modifier = drawWithCache {
    // Built once per size, not on every frame the ring is drawn.
    val inset = (gap + width / 2).toPx()
    val outline = shape.createOutline(Size(size.width + 2 * inset, size.height + 2 * inset), layoutDirection, this)
    val stroke = Stroke(width.toPx())
    onDrawWithContent {
        drawContent()
        if (show()) translate(-inset, -inset) { drawOutline(outline, color, style = stroke) }
    }
}

/** A drawn oval filling its box, for rings around the drum. */
internal val OvalShape = GenericShape { size, _ -> addOval(Rect(Offset.Zero, size)) }

package io.adambailey.framebeat.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import io.adambailey.framebeat.engine.DRUM_KEYS
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.drop
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.adambailey.framebeat.engine.DrumGeometry
import io.adambailey.framebeat.engine.LineId
import io.adambailey.framebeat.engine.MalletSwing
import io.adambailey.framebeat.engine.Side
import io.adambailey.framebeat.engine.Sound
import io.adambailey.framebeat.engine.Strikes
import kotlin.math.hypot
import kotlin.random.Random

// Port of src/components/drum/DrumCanvas.vue and Mallet.vue: layered gradients
// standing in for a wood-and-hide frame drum, two independently animated
// mallets, and color-coded ripple strikes. Approximates the web's CSS layers
// (blurs and inset shadows become soft gradients) rather than pixel-matching
// them; the zones, ripples, and mallet aim come from :engine's DrumGeometry.

/** How long a ripple grows and fades: the web's `RIPPLE_DURATION_MS`. */
private const val RIPPLE_MS = 700

/** Mallet.vue's strike: rest, wind-up, hit, rest, over one ease-out. */
private const val SWING_MS = 260

// Neither is a data class: each strike is a new instance, so a repeated
// target is still a new ripple and a new swing.
private class Ripple(val x: Double, val y: Double, val color: Color)

private class Swing(val target: MalletSwing)

/**
 * The drum's ripples and mallet swings: a strike adds a ripple at a point in
 * drum-ellipse units and swings that side's mallet onto it.
 */
private class DrumAnimations {
    val ripples = mutableStateListOf<Ripple>()
    var left by mutableStateOf<Swing?>(null)
    var right by mutableStateOf<Swing?>(null)

    fun strike(sound: Sound, side: Side, dx: Double, dy: Double) {
        ripples += Ripple(dx, dy, rippleColor(sound))
        val swing = Swing(DrumGeometry.swing(side, dx, dy))
        if (side == Side.Left) left = swing else right = swing
    }
}

/** DrumCanvas.vue's ±0.06 spread, so repeated sequencer hits don't stack on one spot. */
private fun jitter() = (Random.nextDouble() - 0.5) * 0.12

/** `SOUND_META[sound].ripple`: the sound's color, partly transparent. */
private fun rippleColor(sound: Sound): Color =
    Palette.forSound(sound).copy(alpha = if (sound == Sound.Click) 0.6f else 0.55f)

/**
 * The frame drum. Touch anywhere on it: the zone under the finger plays
 * through [onStrike], with a ripple there and the mallet on that side swinging
 * onto it. Every finger strikes, so two hands can drum together. A tap in the
 * empty corners of the box does nothing.
 *
 * Each new strike in [strikes] (the sequencer's, already heard) animates the
 * same way without sounding: the top line on the left mallet, the bottom on
 * the right, aimed at its sound's zone with a little jitter.
 *
 * With keyboard focus, `DRUM_KEYS` strike too (Q W E and I O P by zone, the
 * arrows a Tone per side). It takes
 * focus each time [playing] turns true, so the keys are ready alongside the
 * sequencer, as DrumCanvas.vue does. The focus ring shows only
 * while the keyboard is in use, as `:focus-visible` does on the web.
 *
 * The caller sizes it at the web's 32:30 aspect. [scale] shrinks the mallets'
 * fixed-size parts along with a scaled-down stage.
 */
@Composable
fun DrumView(
    onStrike: (Sound) -> Unit,
    strikes: () -> Strikes,
    playing: () -> Boolean,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
) {
    val animations = remember { DrumAnimations() }
    val interaction = remember { MutableInteractionSource() }
    val focusVisible = rememberFocusVisible(interaction)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        // Skips the value on hand, so a rotation while playing doesn't take focus.
        snapshotFlow(playing).drop(1).collect { if (it) focus.requestFocus() }
    }
    // Watched outside composition, so a sequencer hit animates without
    // recomposing the drum. The strikes on hand at the start, such as from
    // before a rotation, are already seen and do not replay.
    val latestStrikes by rememberUpdatedState(strikes)
    LaunchedEffect(Unit) {
        var seen = latestStrikes()
        snapshotFlow { latestStrikes() }.collect { now ->
            for (line in LineId.entries) {
                val strike = now[line]
                if (strike == null || strike == seen[line]) continue
                val side = if (line == LineId.Top) Side.Left else Side.Right
                val zone = DrumGeometry.zonePoint(strike.sound, side)
                animations.strike(strike.sound, side, zone.x + jitter(), zone.y + jitter())
            }
            seen = now
        }
    }
    // Every way of striking the drum plays the sound and animates the same way.
    fun hit(sound: Sound, side: Side, dx: Double, dy: Double) {
        onStrike(sound)
        animations.strike(sound, side, dx, dy)
    }
    Box(
        modifier
            // The web's ring-2 with ring-offset-4, around the drum's oval.
            .outerRing(OvalShape, Palette.BassSky, gap = 4.dp, show = focusVisible)
            .focusRequester(focus)
            .onKeyDown { _, name ->
                val key = DRUM_KEYS[name] ?: return@onKeyDown false
                val zone = DrumGeometry.zonePoint(key.sound, key.side)
                hit(key.sound, key.side, zone.x, zone.y)
                true
            }
            .focusable(interactionSource = interaction)
            .semantics {
                contentDescription = "Frame drum"
                role = Role.Button
                // A screen reader's activation strikes the center.
                onClick(label = "Strike") {
                    hit(Sound.Bass, Side.Right, 0.0, 0.0)
                    true
                }
            }
            .pointerInput(onStrike) {
                awaitPointerEventScope {
                    while (true) {
                        // Every change is consumed, the web's `touch-none`: a finger
                        // that lands on the drum strikes it and never scrolls the page.
                        for (change in awaitPointerEvent().changes) {
                            val down = change.changedToDown()
                            change.consume()
                            if (!down) continue
                            val tapped = DrumGeometry.classify(
                                change.position.x.toDouble() / size.width,
                                change.position.y.toDouble() / size.height,
                            ) ?: continue
                            hit(tapped.sound, tapped.side, tapped.dx, tapped.dy)
                        }
                    }
                }
            },
    ) {
        // Its own layer, so ripple and mallet frames do not redraw the static art.
        Canvas(Modifier.fillMaxSize().graphicsLayer()) { drawDrum(scale) }
        for (ripple in animations.ripples) {
            key(ripple) { RippleView(ripple, scale, onDone = { animations.ripples.remove(ripple) }) }
        }
        MalletView(Side.Left, animations.left, scale)
        MalletView(Side.Right, animations.right, scale)
    }
}

@Composable
private fun RippleView(ripple: Ripple, scale: Float, onDone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(RIPPLE_MS, easing = EaseOut))
        onDone()
    }
    Canvas(Modifier.fillMaxSize()) {
        val p = progress.value
        val grow = 0.2f + 1.2f * p
        val center = Offset(
            size.width * (DrumGeometry.CENTER_X + ripple.x * DrumGeometry.HALF_W).toFloat(),
            size.height * (DrumGeometry.CENTER_Y + ripple.y * DrumGeometry.HALF_H).toFloat(),
        )
        val ring = Size(size.width * 0.46f * grow, size.height * 0.30f * grow)
        drawOval(
            color = ripple.color.copy(alpha = ripple.color.alpha * 0.85f * (1 - p)),
            topLeft = center - Offset(ring.width / 2, ring.height / 2),
            size = ring,
            style = Stroke(2.dp.toPx() * scale * grow),
        )
    }
}

/**
 * One mallet, drawn on the drum's box the way Mallet.vue lays it out: a box
 * 38% × 58% of the drum at its bottom corner, pivoting at (12%, 96%) of that
 * box (mirrored on the right), with the stick tilted 24° at rest. A swing
 * plays Mallet.vue's keyframes under one ease-out, as the Web Animations API
 * applies a whole-animation easing.
 */
@Composable
private fun MalletView(side: Side, swing: Swing?, scale: Float) {
    val progress = remember { Animatable(1f) }
    LaunchedEffect(swing) {
        if (swing == null) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(1f, tween(SWING_MS, easing = EaseOut))
    }
    Canvas(Modifier.fillMaxSize()) {
        val left = side == Side.Left
        val boxW = size.width * DrumGeometry.MALLET_BOX_W.toFloat()
        val boxH = size.height * DrumGeometry.MALLET_BOX_H.toFloat()
        val pivot = DrumGeometry.pivot(side).let {
            Offset(size.width * (it.x / DrumGeometry.CANVAS_W).toFloat(), size.height * (it.y / DrumGeometry.CANVAS_H).toFloat())
        }

        val pose = malletPose(progress.value, swing?.target, wind = if (left) -7f else 7f)
        translate(pose.x * boxW, pose.y * boxH) {
            rotate(pose.rotation, pivot) {
                val stickW = 10.dp.toPx() * scale
                val stickH = size.height * (DrumGeometry.REACH / DrumGeometry.CANVAS_H).toFloat()
                // The stick sits 8% in from the box's outer edge (the pivot is 12% in), its foot on the pivot's row.
                val inset = boxW * 0.04f - stickW / 2
                val foot = Offset(if (left) pivot.x - inset else pivot.x + inset, pivot.y)
                rotate(DrumGeometry.baseRotation(side).toFloat(), foot) {
                    drawStick(foot, stickW, stickH, scale)
                }
            }
        }
    }
}

private class Pose(val rotation: Float, val x: Float, val y: Float)

/** Mallet.vue's keyframes, rest to wind-up to hit to rest, at eased progress [p]. */
private fun malletPose(p: Float, target: MalletSwing?, wind: Float): Pose {
    if (target == null) return Pose(0f, 0f, 0f)
    return Pose(
        rotation = keyframeAt(p, 0f to 0f, 0.28f to wind, 0.5f to target.rotation.toFloat(), 1f to 0f),
        x = keyframeAt(p, 0f to 0f, 0.28f to 0f, 0.5f to target.x.toFloat(), 1f to 0f),
        y = keyframeAt(p, 0f to 0f, 0.28f to -0.03f, 0.5f to target.y.toFloat(), 1f to 0f),
    )
}

/** The stick upright from [foot], with the felt tip's top 8dp past its end. */
private fun DrawScope.drawStick(foot: Offset, width: Float, height: Float, scale: Float) {
    val top = foot.y - height
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(Color(0xFFFCD34D), Color(0xFF92400E), Color(0xFF451A03)),
            startY = top,
            endY = foot.y,
        ),
        topLeft = Offset(foot.x - width / 2, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(width / 2),
    )
    val tip = 24.dp.toPx() * scale
    val tipCenter = Offset(foot.x, top - 8.dp.toPx() * scale + tip / 2)
    softOval(Rect(tipCenter + Offset(0f, 2.dp.toPx() * scale), tip / 2), Color.Black.copy(alpha = 0.4f), 4.dp.toPx() * scale)
    val corner = tipCenter - Offset(tip / 2, tip / 2)
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0xFFEFE0BD),
            0.55f to Color(0xFFD3B686),
            1f to Color(0xFFA3805A),
            center = corner + Offset(tip * 0.35f, tip * 0.3f),
            radius = tip * hypot(0.65f, 0.7f),
        ),
        radius = tip / 2,
        center = tipCenter,
    )
}

// The drum's layers, top to bottom of DrumCanvas.vue, as fractions of the box.

private fun DrawScope.drawDrum(scale: Float) {
    val w = size.width
    val h = size.height
    fun box(left: Float, top: Float, width: Float, height: Float) = Rect(w * left, h * top, w * (left + width), h * (top + height))

    // Ground shadow.
    softOval(box(0.04f, 0.78f, 0.92f, 0.16f), Color.Black.copy(alpha = 0.5f), 12.dp.toPx() * scale)

    // Shell depth: dark wood layers peeking out below the frame.
    val lower = box(0f, 0.18f, 1f, 0.68f)
    drawOval(Brush.verticalGradient(listOf(Color(0xFF292524), Color(0xFF0C0A09)), lower.top, lower.bottom), lower.topLeft, lower.size)
    val upper = box(0f, 0.12f, 1f, 0.68f)
    drawOval(Brush.verticalGradient(listOf(Color(0xFF78350F), Color(0xFF1C1917)), upper.top, upper.bottom), upper.topLeft, upper.size)

    // Wooden frame with turned grain: a conic gradient repeating every 27°.
    val frame = box(0f, 0.06f, 1f, 0.68f)
    val grainCenter = Offset(frame.center.x, frame.top + frame.height * 0.4f)
    drawOval(Brush.sweepGradient(*woodGrain, center = grainCenter), frame.topLeft, frame.size)
    insetRim(frame, Color(0x38FFECC8), fromTop = true, depth = 6.dp.toPx() * scale)
    insetRim(frame, Color(0x80000000), fromTop = false, depth = 10.dp.toPx() * scale)

    // Drumhead skin, lit from the upper left.
    val skin = box(0.09f, 0.12f, 0.82f, 0.56f)
    drawOval(
        cssRadialGradient(
            skin,
            focus = Offset(0.38f, 0.3f),
            0f to Color(0xFFF2E5CB),
            0.42f to Color(0xFFE4CDA2),
            0.74f to Color(0xFFC9A878),
            1f to Color(0xFFAB8757),
        ),
        skin.topLeft,
        skin.size,
    )
    insetRim(skin, Color(0x733C230A), fromTop = true, depth = 10.dp.toPx() * scale)
    insetRim(skin, Color(0x40FFF0D2), fromTop = false, depth = 4.dp.toPx() * scale)

    // Tension ring and skin wrinkle hints.
    val ring = Color(0x3378350F)
    for (r in listOf(box(0.13f, 0.15f, 0.74f, 0.5f), box(0.31f, 0.26f, 0.38f, 0.28f))) {
        drawOval(ring, r.topLeft, r.size, style = Stroke(1.dp.toPx() * scale))
    }

    // Sheen.
    softOval(box(0.16f, 0.16f, 0.34f, 0.22f), Color.White.copy(alpha = 0.1f), 2.dp.toPx() * scale)
}

/** `repeating-conic-gradient(#8a5224 0deg, #9a6130 9deg, #714119 18deg, #8a5224 27deg)`, as sweep stops. */
private val woodGrain: Array<Pair<Float, Color>> = run {
    val colors = listOf(Color(0xFF8A5224), Color(0xFF9A6130), Color(0xFF714119), Color(0xFF8A5224))
    // One stop a degree. CSS counts from 12 o'clock and Compose from 3, so the
    // pattern restarts (and leaves its seam) at Compose's 270°, as on the web.
    Array(361) { t ->
        val within = ((t + 90) % 360) % 27 / 9f
        val i = within.toInt()
        t / 360f to lerp(colors[i], colors[i + 1], within - i)
    }
}

/** Stands in for a CSS inset shadow: a band of [color] inside the ellipse's top or bottom edge, [depth] deep. */
private fun DrawScope.insetRim(r: Rect, color: Color, fromTop: Boolean, depth: Float) {
    val oval = Path().apply { addOval(r) }
    val brush = if (fromTop) {
        Brush.verticalGradient(listOf(color, Color.Transparent), r.top, r.top + depth * 3)
    } else {
        Brush.verticalGradient(listOf(Color.Transparent, color), r.bottom - depth * 3, r.bottom)
    }
    clipPath(oval) { drawOval(brush, r.topLeft, r.size, style = Stroke(depth * 2)) }
}

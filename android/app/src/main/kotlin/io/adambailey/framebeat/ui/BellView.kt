package io.adambailey.framebeat.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

// Port of src/components/drum/Bell.vue: a hanging chime that swings when it
// rings, its clapper swinging on its own, and a soft shine passing over it.
// The art is drawn at its 112 x 136 base size and scaled as a whole.

private val Brass = Color(0xFFD9A836)
private val BrassDark = Color(0xFF8F6319)

/**
 * The bell, [scale] × its base art, with the "Bell" caption below. A touch
 * rings it through [onRing]. Each change of [trigger] after the first
 * composition plays the ring animation, whether the bell was struck by hand
 * or rang on the one.
 */
@Composable
fun BellView(trigger: Int, onRing: () -> Unit, scale: Float, modifier: Modifier = Modifier) {
    val body = remember { Animatable(1f) }
    val clapper = remember { Animatable(1f) }
    val shine = remember { Animatable(1f) }
    // A ring seen before this composition, such as before a rotation, does not replay.
    val initialTrigger = remember { trigger }
    LaunchedEffect(trigger) {
        if (trigger == initialTrigger) return@LaunchedEffect
        coroutineScope {
            for ((anim, ms) in listOf(body to 800, clapper to 700, shine to 600)) {
                launch {
                    anim.snapTo(0f)
                    anim.animateTo(1f, tween(ms, easing = EaseOut))
                }
            }
        }
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            Modifier
                .size((StageLayout.BELL_WIDTH * scale).dp, (StageLayout.BELL_HEIGHT * scale).dp)
                .semantics {
                    contentDescription = "Bell"
                    role = Role.Button
                    onClick(label = "Ring") {
                        onRing()
                        true
                    }
                }
                .pointerInput(onRing) {
                    awaitPointerEventScope {
                        while (true) {
                            // Consume the whole gesture, as the drum does, so it never scrolls the page.
                            val changes = awaitPointerEvent().changes
                            if (changes.any { it.changedToDown() }) onRing()
                            changes.forEach { it.consume() }
                        }
                    }
                },
        ) {
            scale(scale * density, pivot = Offset.Zero) {
                drawBell(
                    bodyRotation = keyframeAt(body.value, 0f to 0f, 0.2f to -16f, 0.4f to 12f, 0.6f to -8f, 0.8f to 4f, 1f to 0f),
                    clapperRotation = keyframeAt(clapper.value, 0f to 0f, 0.25f to 14f, 0.5f to -10f, 0.75f to 5f, 1f to 0f),
                    shineAlpha = keyframeAt(shine.value, 0f to 0f, 0.5f to 0.6f, 1f to 0f),
                    shineShift = keyframeAt(shine.value, 0f to 0f, 0.5f to 8f, 1f to 16f),
                )
            }
        }
        BasicText(
            "Bell",
            Modifier.clearAndSetSemantics {},
            style = TextStyle(color = Palette.LabelMuted, fontSize = 11.sp),
        )
    }
}

/** The bell in base units (dp at scale 1), laid out as Bell.vue's stacked divs. */
private fun DrawScope.drawBell(bodyRotation: Float, clapperRotation: Float, shineAlpha: Float, shineShift: Float) {
    val cx = StageLayout.BELL_WIDTH / 2
    fun centered(top: Float, width: Float, height: Float) = Rect(cx - width / 2, top, cx + width / 2, top + height)

    // Stand arm.
    val arm = centered(0f, 8f, 24f)
    drawRoundRect(Color(0xFF44403C), arm.topLeft, arm.size, CornerRadius(4f))

    // Everything below hangs from the arm at y = 20 and swings from there.
    translate(top = 20f) {
        rotate(bodyRotation, Offset(cx, 0f)) {
            // Cord.
            val cord = centered(0f, 2f, 20f)
            drawRect(Color(0xFF78716C), cord.topLeft, cord.size)

            // Hanging loop, round on top, its border drawn inside the box.
            val loop = centered(20f, 14f, 12f).deflate(1f)
            drawPath(rounded(loop, top = CornerRadius(loop.width / 2)), Color(0xFFB45309), style = Stroke(2f))

            // Dome.
            val dome = centered(28f, 44f, 24f)
            drawPath(
                rounded(dome, top = CornerRadius(22f)),
                cssLinearGradient(160f, dome, 0f to Color(0xFFF8DD85), 0.55f to Brass, 1f to Color(0xFFA06D1C)),
            )

            // Body tapering to the mouth, with its shadow.
            val mouth = centered(52f, 56f, 24f)
            softShadow(mouth.translate(0f, 4f), 0.45f)
            drawPath(
                rounded(mouth, bottom = CornerRadius(mouth.width * 0.3f, mouth.height * 0.3f)),
                cssLinearGradient(155f, mouth, 0.08f to Color(0xFFF5D879), 0.5f to Brass, 1f to BrassDark),
            )

            // Flared lip.
            val lip = centered(72f, 64f, 8f)
            softShadow(lip.translate(0f, 2f), 0.5f)
            drawOval(cssLinearGradient(160f, lip, 0f to Color(0xFFE9BD55), 1f to BrassDark), lip.topLeft, lip.size)

            // Clapper, swinging from under the lip.
            rotate(clapperRotation, Offset(cx, 82f)) {
                val rod = centered(82f, 2f, 8f)
                drawRect(Color(0xFF57534E), rod.topLeft, rod.size)
                drawCircle(Color(0xFF92400E), radius = 4f, center = Offset(cx, 94f))
            }
        }

        // Shine: a soft white streak that passes left to right, not swinging with the body.
        if (shineAlpha > 0f) {
            val streak = Rect(cx - 32f + shineShift, 20f, cx - 12f + shineShift, 60f)
            softOval(streak, Color.White.copy(alpha = 0.7f * shineAlpha), 3f)
        }
    }
}

/** A box with its top corners rounded by [top] and its bottom corners by [bottom]. */
private fun rounded(r: Rect, top: CornerRadius = CornerRadius.Zero, bottom: CornerRadius = CornerRadius.Zero) = Path().apply {
    addRoundRect(RoundRect(r, topLeft = top, topRight = top, bottomLeft = bottom, bottomRight = bottom))
}

/** A drop shadow under a brass part: a soft dark oval at [r]. */
private fun DrawScope.softShadow(r: Rect, alpha: Float) = softOval(r, Color.Black.copy(alpha = alpha), 4f)

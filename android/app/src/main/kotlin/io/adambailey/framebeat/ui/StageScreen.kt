package io.adambailey.framebeat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.adambailey.framebeat.engine.Sound
import kotlin.math.sqrt

/** The stage row's own top and bottom padding: the web's `py-4`. */
private const val ROW_PADDING = 16f

/**
 * The instrument screen, laid out by DESIGN.md's "Phone and Tablet (native)"
 * section: one scrolling column of the title, the caption, and the drum and
 * bell in one row. The same composition at both widths; [StageLayout] sizes it.
 */
@Composable
fun StageScreen(bellTrigger: Int, onStrike: (Sound) -> Unit, onRingBell: () -> Unit) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Palette.Stage)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        val density = LocalDensity.current
        // Heights in px of the whole column and of the stage row, so a short
        // expanded window can give the stage whatever the rest leaves over.
        var columnPx by remember { mutableIntStateOf(0) }
        var rowPx by remember { mutableIntStateOf(0) }
        val rest = with(density) { (columnPx - rowPx).toDp().value }
        val layout = StageLayout.of(maxWidth.value, stageRoom = maxHeight.value - rest - 2 * ROW_PADDING)
        val expanded = layout.expanded

        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .widthIn(max = StageLayout.COLUMN_MAX.dp)
                .fillMaxWidth()
                .padding(horizontal = layout.gutter.dp)
                .padding(top = if (expanded) 56.dp else 24.dp, bottom = if (expanded) 40.dp else 32.dp)
                .onSizeChanged { columnPx = it.height },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText(
                "FrameBeat",
                Modifier.semantics { heading() },
                style = TextStyle(
                    color = Palette.Title,
                    fontFamily = Fonts.Display,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = if (expanded) 48.sp else 36.sp,
                    lineHeight = 1.1.em,
                    letterSpacing = (-0.025).em,
                    textAlign = TextAlign.Center,
                ),
            )
            Spacer(Modifier.height(8.dp))
            BasicText(
                "Tap the drum or the bell to play",
                style = TextStyle(
                    color = Palette.Caption,
                    fontSize = if (expanded) 14.sp else 12.sp,
                    textAlign = TextAlign.Center,
                ),
            )
            Spacer(Modifier.height(if (expanded) 24.dp else 16.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .onSizeChanged { rowPx = it.height }
                    .drawBehind { drawStageGlow() }
                    .padding(vertical = ROW_PADDING.dp),
                horizontalArrangement = Arrangement.spacedBy(layout.gap.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DrumView(
                    onStrike = onStrike,
                    modifier = Modifier.size(layout.drumWidth.dp, layout.drumHeight.dp),
                    scale = layout.stageScale,
                )
                BellView(trigger = bellTrigger, onRing = onRingBell, scale = layout.bellScale)
            }
        }
    }
}

/**
 * The soft light the instrument sits under: the web's
 * `radial-gradient(ellipse at center, rgba(30,41,59,0.55), transparent 65%)`
 * across the stage row, which shrinks with it.
 */
private fun DrawScope.drawStageGlow() {
    val w = size.width
    // A circle in a w × w square, squashed to the row: CSS's farthest-corner ellipse.
    scale(1f, size.height / w, pivot = Offset.Zero) {
        drawRect(
            Brush.radialGradient(
                0f to Palette.PanelBorder.copy(alpha = 0.55f),
                0.65f to Palette.PanelBorder.copy(alpha = 0f),
                center = Offset(w / 2, w / 2),
                radius = w / sqrt(2f),
            ),
            size = Size(w, w),
        )
    }
}

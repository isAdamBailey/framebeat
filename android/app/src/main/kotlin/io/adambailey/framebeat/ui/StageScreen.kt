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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.adambailey.framebeat.session.Playback
import io.adambailey.framebeat.session.SessionViewModel

/** The stage row's own top and bottom padding: the web's `py-4`. */
private const val ROW_PADDING = 16f

private enum class Slot { Header, Stage, Panel }

/**
 * The instrument screen, laid out by DESIGN.md's "Phone and Tablet (native)"
 * section: one scrolling column of the title, the caption, the drum and bell
 * in one row, then the panel. The same composition at both widths;
 * [StageLayout] sizes the stage.
 */
@Composable
fun StageScreen(model: SessionViewModel) {
    // Compact or expanded comes from the whole window, as Android's window size classes do.
    val windowWidth = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp().value }
    PlaybackFrames(model.playback)
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Palette.Stage)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        val contentWidth = maxWidth.value
        val expanded = StageLayout.of(windowWidth, contentWidth).expanded
        val top = if (expanded) 56.dp else 24.dp
        val bottom = if (expanded) 40.dp else 32.dp
        val viewportHeight = maxHeight - top - bottom
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .widthIn(max = StageLayout.COLUMN_MAX.dp)
                .fillMaxWidth()
                .padding(horizontal = StageLayout.of(windowWidth, contentWidth).gutter.dp)
                .padding(top = top, bottom = bottom),
        ) {
            StageColumn(
                expanded = expanded,
                viewportHeight = viewportHeight,
                stageFor = { room -> StageLayout.of(windowWidth, contentWidth, room) },
                header = { Header(expanded) },
                stage = { stage -> Stage(stage, model) },
                panel = { Panel(model.session, model.playback, expanded) },
            )
        }
    }
}

/** The web's requestAnimationFrame loop: lights the steps as they are heard, while playing. */
@Composable
private fun PlaybackFrames(playback: Playback) {
    val playing = playback.playing
    LaunchedEffect(playing) {
        while (playing) withFrameNanos { playback.frame() }
    }
}

/**
 * Stacks [header], the stage, and [panel], centered. The header and panel are
 * measured first, so a short expanded window can give the stage exactly the
 * height they leave, in one pass.
 */
@Composable
private fun StageColumn(
    expanded: Boolean,
    viewportHeight: Dp,
    stageFor: (stageRoom: Float) -> StageLayout,
    header: @Composable () -> Unit,
    stage: @Composable (StageLayout) -> Unit,
    panel: @Composable () -> Unit,
) {
    SubcomposeLayout { constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val headers = subcompose(Slot.Header, header).map { it.measure(loose) }
        val panels = subcompose(Slot.Panel, panel).map { it.measure(loose) }
        val headerGap = (if (expanded) 24.dp else 16.dp).roundToPx()
        // The web's `mb-8` under the stage row, when there is a panel to space from.
        val panelGap = if (panels.isEmpty()) 0 else 32.dp.roundToPx()
        val used = headers.sumOf { it.height } + headerGap + panelGap + panels.sumOf { it.height }
        val room = (viewportHeight.roundToPx() - used).toDp().value - 2 * ROW_PADDING
        val sized = stageFor(room)
        val stages = subcompose(Slot.Stage) { stage(sized) }.map { it.measure(loose) }

        val width = constraints.maxWidth
        val height = used + stages.sumOf { it.height }
        layout(width, height) {
            var y = 0
            fun place(pieces: List<Placeable>, gapAfter: Int) {
                for (p in pieces) {
                    p.placeRelative((width - p.width) / 2, y)
                    y += p.height
                }
                y += gapAfter
            }
            place(headers, headerGap)
            place(stages, panelGap)
            place(panels, 0)
        }
    }
}

@Composable
private fun Header(expanded: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
    }
}

/**
 * The drum and the bell in one row, under the stage glow: the web's
 * `radial-gradient(ellipse at center, rgba(30,41,59,0.55), transparent 65%)`
 * across the row, narrowed by the stage scale as the row's height shrinks.
 */
@Composable
private fun Stage(layout: StageLayout, model: SessionViewModel) {
    Row(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                val halfWidth = size.width * layout.stageScale / 2
                val glow = Rect(center.x - halfWidth, 0f, center.x + halfWidth, size.height)
                drawRect(
                    cssRadialGradient(
                        glow,
                        focus = Offset(0.5f, 0.5f),
                        0f to Palette.PanelBorder.copy(alpha = 0.55f),
                        0.65f to Palette.PanelBorder.copy(alpha = 0f),
                    ),
                    glow.topLeft,
                    glow.size,
                )
            }
            .padding(vertical = ROW_PADDING.dp)
            // Holds only the drum and the bell, so Space and B work while either has focus.
            .instrumentShortcuts(model::shortcut),
        horizontalArrangement = Arrangement.spacedBy(layout.gap.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DrumView(
            onStrike = model::strike,
            strikes = { model.session.strikes },
            playing = { model.playback.playing },
            modifier = Modifier.size(layout.drumWidth.dp, layout.drumHeight.dp),
            scale = layout.stageScale,
        )
        BellView(
            trigger = model.session.bellTrigger,
            onRing = model::ringBell,
            scale = layout.bellScale,
        )
    }
}

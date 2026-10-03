package io.adambailey.framebeat.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.offset
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import io.adambailey.framebeat.engine.LineId
import io.adambailey.framebeat.engine.Ranges
import io.adambailey.framebeat.engine.Sound
import io.adambailey.framebeat.session.Playback
import io.adambailey.framebeat.session.Session
import kotlin.math.roundToInt

// Port of TransportControls.vue, Sequencer.vue, SequencerLine.vue,
// LineHeader.vue, and TogglePill.vue: the flat control panel under the stage.

private val Pill = RoundedCornerShape(50)

/** Half the widest dot target: how far a step line's end dots reach past it. */
private val DOT_BLEED = 18.dp

/** DESIGN.md's Label style: 11sp, semibold, tracked, uppercase. */
private val LabelStyle = TextStyle(color = Palette.Label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.14.em)

/**
 * A ring around a round control, outside its edge as Tailwind's `ring` draws
 * one: a [width] stroke, [gap] past the edge. Put it before any clip.
 */
private fun Modifier.outerRing(show: Boolean, color: Color, width: Dp = 2.dp, gap: Dp = 0.dp): Modifier =
    if (!show) {
        this
    } else {
        drawWithContent {
            drawContent()
            val inset = (gap + width / 2).toPx()
            drawRoundRect(
                color,
                Offset(-inset, -inset),
                Size(size.width + 2 * inset, size.height + 2 * inset),
                CornerRadius(size.height / 2 + inset),
                style = Stroke(width.toPx()),
            )
        }
    }

/** The sky focus ring, 2dp out (`ring-offset-2`). Touch never focuses a control, so it shows for keyboards only. */
private fun Modifier.focusRing(focused: Boolean): Modifier = outerRing(focused, Palette.BassSky, gap = 2.dp)

/**
 * Widens this element's layer by [bleed] on each side without moving its
 * content, so an offscreen layer (a group fade) has room for what is drawn past its edges.
 */
private fun Modifier.bleed(bleed: Dp): Modifier = layout { measurable, constraints ->
    val extra = bleed.roundToPx()
    val placeable = measurable.measure(constraints.offset(horizontal = 2 * extra))
    layout(placeable.width - 2 * extra, placeable.height) { placeable.place(-extra, 0) }
}

/**
 * The panel: transport on top, then both step lines. Play and Pause drive
 * [playback]; every other control edits [session].
 */
@Composable
fun Panel(session: Session, playback: Playback, expanded: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .fillMaxWidth()
            // No clip: the Play button's glow spills past the panel, as on the web.
            .background(Palette.Panel, shape)
            .border(1.dp, Palette.PanelBorder, shape)
            .padding(if (expanded) 24.dp else 16.dp),
    ) {
        Transport(session, playback, expanded)
        Spacer(Modifier.height(32.dp))
        StepLines(session, playback, expanded)
    }
}

@Composable
private fun Transport(session: Session, playback: Playback, expanded: Boolean) {
    if (expanded) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            PlayButton(playback.playing, playback::toggle, Modifier.width(176.dp))
            TempoRow(session, expanded, Modifier.weight(1f))
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            PlayButton(playback.playing, playback::toggle, Modifier.fillMaxWidth())
            TempoRow(session, expanded, Modifier.fillMaxWidth())
        }
    }
}

/** The tempo slider and its readout, then Chime on 1. */
@Composable
private fun TempoRow(session: Session, expanded: Boolean, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(if (expanded) 24.dp else 16.dp), verticalAlignment = Alignment.Bottom) {
        Tempo(session.bpm, session::changeBpm, Modifier.weight(1f))
        TogglePill(
            pressed = session.chimeOnOne,
            onToggle = { session.chimeOnOne = it },
            icon = if (session.chimeOnOne) Icons.Bell else Icons.BellOff,
            text = "Chime on 1",
        )
    }
}

/** The one primary action, and The One Glow Rule's one glow. */
@Composable
private fun PlayButton(playing: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val label = if (playing) "Pause" else "Play"
    Row(
        modifier
            .scale(if (pressed) 0.97f else 1f)
            .height(64.dp)
            .dropShadow(Pill, Shadow(radius = 24.dp, color = Palette.BassSky.copy(alpha = 0.35f), offset = DpOffset(0.dp, 8.dp)))
            .focusRing(focused)
            .clip(Pill)
            .background(Palette.BassSky)
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (playing) Icons.Pause else Icons.Play, Palette.Stage, 24.dp)
        BasicText(label, style = TextStyle(color = Palette.Stage, fontSize = 18.sp, fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun Tempo(bpm: Int, onChange: (Int) -> Unit, modifier: Modifier) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            BasicText("TEMPO", Modifier.alignByBaseline(), style = LabelStyle)
            Spacer(Modifier.weight(1f))
            BasicText(
                "$bpm",
                Modifier.alignByBaseline(),
                style = numeralStyle(20.sp, FontWeight.Bold, Palette.Numeral),
            )
            Spacer(Modifier.width(4.dp))
            BasicText("BPM", Modifier.alignByBaseline(), style = TextStyle(color = Palette.Label, fontSize = 12.sp, fontWeight = FontWeight.Medium))
        }
        Spacer(Modifier.height(6.dp))
        Slider(bpm, Ranges.bpm, onChange, label = "Tempo, beats per minute", modifier = Modifier.fillMaxWidth())
    }
}

/** DESIGN.md's Numeral style: Fraunces, tabular figures. */
private fun numeralStyle(size: TextUnit, weight: FontWeight, color: Color) = TextStyle(
    color = color,
    fontFamily = Fonts.Numeral,
    fontWeight = weight,
    fontSize = size,
    lineHeight = 1.em,
    fontFeatureSettings = "tnum",
)

/**
 * The panel's secondary-control shape: a bordered pill with an icon and a
 * label, lit with a neutral fill when pressed. Mute and Chime on 1 both use
 * it. [iconOnly] hides the label but keeps it as the accessible name.
 */
@Composable
private fun TogglePill(
    pressed: Boolean,
    onToggle: (Boolean) -> Unit,
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    iconOnly: Boolean = false,
    description: String = text,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val ink = if (pressed) Color.White else Palette.ControlText
    Row(
        modifier
            .heightIn(min = 36.dp)
            .widthIn(min = 36.dp)
            .focusRing(focused)
            .clip(Pill)
            .background(if (pressed) Palette.ControlFillPressed else Color.Transparent)
            .border(1.dp, if (pressed) Palette.Label else Palette.LabelMuted, Pill)
            .toggleable(pressed, interaction, indication = null, role = Role.Switch, onValueChange = onToggle)
            .semantics { contentDescription = description }
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, ink, 16.dp)
        if (!iconOnly) BasicText(text, style = TextStyle(color = ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun Icon(icon: ImageVector, tint: Color, size: Dp) {
    Image(icon, contentDescription = null, Modifier.size(size), colorFilter = ColorFilter.tint(tint))
}

@Composable
private fun StepLines(session: Session, playback: Playback, expanded: Boolean) {
    val inset = Modifier.padding(horizontal = if (expanded) 40.dp else 24.dp)
    Column {
        LineHeader("Top line", LineId.Top, session, expanded, inset)
        Box(inset) {
            PlayheadBar({ playback.progress }, Modifier.matchParentSize())
            Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                for (id in LineId.entries) StepLine(id, session, playback)
            }
        }
        Spacer(Modifier.height(16.dp))
        LineHeader("Bottom line — sets the pulse", LineId.Bottom, session, expanded, inset)
    }
}

/**
 * The thin bar sweeping both lines through the bar, under their dots, with
 * DESIGN.md's playhead glow. [progress] is read only to place it, so the
 * bar moves every frame without recomposing anything.
 */
@Composable
private fun PlayheadBar(progress: () -> Double, modifier: Modifier) {
    Layout(
        content = {
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .dropShadow(Pill, Shadow(radius = 12.dp, color = Palette.BassSky.copy(alpha = 0.9f)))
                    .background(Palette.BassSky, Pill),
            )
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val bar = measurables.single().measure(constraints.copy(minWidth = 0))
        layout(constraints.maxWidth, constraints.maxHeight) {
            // Its own layer: moving it each frame does not redraw the glow.
            bar.placeRelativeWithLayer((progress() * constraints.maxWidth).roundToInt() - bar.width / 2, 0)
        }
    }
}

/**
 * A line's label and mute, then its step-count numeral, count slider, and
 * sound picker. Below [StageLayout.BREAKPOINT] the picker takes its own row
 * and Mute shows its icon only, as the web does below 640px.
 */
@Composable
private fun LineHeader(label: String, id: LineId, session: Session, expanded: Boolean, modifier: Modifier) {
    val line = session.line(id)
    Column(modifier.padding(bottom = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BasicText(label.uppercase(), Modifier.weight(1f), style = LabelStyle)
            TogglePill(
                pressed = line.muted,
                onToggle = { session.changeMuted(id, it) },
                icon = if (line.muted) Icons.VolumeOff else Icons.Volume,
                text = "Mute",
                iconOnly = !expanded,
                description = "Mute $label",
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(if (expanded) 20.dp else 16.dp), verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                "${line.count}",
                // The web's `w-[1.4ch]`: two digits spill evenly into the gap.
                Modifier.width(if (expanded) 40.dp else 30.dp).wrapContentWidth(unbounded = true),
                style = numeralStyle(if (expanded) 48.sp else 36.sp, FontWeight.Black, Palette.textFor(line.sound)),
                softWrap = false,
            )
            Slider(line.count, Ranges.count, { session.changeCount(id, it) }, label = "$label step count", modifier = Modifier.weight(1f))
            if (expanded) SoundPicker(line.sound, { session.changeSound(id, it) }, label, fill = false)
        }
        if (!expanded) {
            Spacer(Modifier.height(12.dp))
            SoundPicker(line.sound, { session.changeSound(id, it) }, label, fill = true)
        }
    }
}

/**
 * Bass | Tone | Click, a segmented pill. The active segment takes its sound's
 * color, the one place the picker uses it. [fill] stretches it to the row.
 */
@Composable
private fun SoundPicker(sound: Sound, onChange: (Sound) -> Unit, label: String, fill: Boolean) {
    Row(
        (if (fill) Modifier.fillMaxWidth() else Modifier)
            .clip(Pill)
            .background(Palette.Stage.copy(alpha = 0.6f))
            .border(1.dp, Palette.PickerBorder, Pill)
            .padding(2.dp)
            .semantics { contentDescription = "$label sound" }
            .selectableGroup(),
    ) {
        for (candidate in Sound.entries) {
            val active = candidate == sound
            val interaction = remember { MutableInteractionSource() }
            val focused by interaction.collectIsFocusedAsState()
            Row(
                (if (fill) Modifier.weight(1f) else Modifier)
                    .height(32.dp)
                    .focusRing(focused)
                    .clip(Pill)
                    .background(if (active) Palette.PanelBorder else Color.Transparent)
                    .border(1.dp, if (active) Palette.forSound(candidate) else Color.Transparent, Pill)
                    .selectable(active, interaction, indication = null, role = Role.RadioButton) { onChange(candidate) }
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(8.dp).background(Palette.dotFor(candidate), CircleShape))
                BasicText(
                    candidate.label,
                    style = TextStyle(
                        color = if (active) Palette.textFor(candidate) else Palette.ControlText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        }
    }
}

/**
 * One line of step dots on a thin track. Step i of n sits at i/n of the bar,
 * so a playhead sweeping the bar crosses each dot as it plays. Each dot's
 * touch target is up to 36dp wide but never wider than its step's slot.
 */
@Composable
private fun StepLine(id: LineId, session: Session, playback: Playback) {
    val line = session.line(id)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            // Fade the line as one group, like CSS opacity, so the track never
            // shows through a dot. The layer bleeds past the edges to hold the end dots.
            .bleed(DOT_BLEED)
            .graphicsLayer { alpha = if (line.muted) 0.4f else 1f }
            .padding(horizontal = DOT_BLEED),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth().height(4.dp).background(Palette.ControlFillPressed, Pill))
        val slot = maxWidth / line.count
        val hit = min(36.dp, slot)
        for (i in 0 until line.count) {
            StepDot(
                on = line.dots[i],
                current = { playback.current(id) == i },
                color = Palette.dotFor(line.sound),
                description = "${id.name} line step ${i + 1}",
                onToggle = { session.toggleDot(id, i) },
                modifier = Modifier.offset(x = slot * i - hit / 2).width(hit),
            )
        }
    }
}

@Composable
private fun StepDot(on: Boolean, current: () -> Boolean, color: Color, description: String, onToggle: () -> Unit, modifier: Modifier) {
    // Only the dot that lights and the one that dims recompose on a step.
    val isCurrent by remember { derivedStateOf(current) }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Box(
        modifier
            .height(36.dp)
            .toggleable(on, interaction, indication = null, role = Role.Checkbox) { onToggle() }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        val dot = if (on) 24.dp else 20.dp
        Box(
            Modifier
                .scale(if (isCurrent) 1.25f else 1f)
                .size(dot)
                .then(
                    if (on) {
                        Modifier
                            .dropShadow(CircleShape, Shadow(radius = 8.dp, color = Color.Black.copy(alpha = 0.3f), offset = DpOffset(0.dp, 4.dp)))
                            .background(color, CircleShape)
                    } else {
                        Modifier.background(Palette.PanelSolid, CircleShape).border(2.dp, Palette.Label, CircleShape)
                    },
                )
                .outerRing(isCurrent, Color.White.copy(alpha = 0.8f))
                .focusRing(focused),
        )
    }
}

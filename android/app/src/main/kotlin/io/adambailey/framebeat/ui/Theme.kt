package io.adambailey.framebeat.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import io.adambailey.framebeat.R
import io.adambailey.framebeat.engine.Sound

/** Colors from DESIGN.md and the web header. */
object Palette {
    /** `colors.stage` (slate-950): the page background. Also `@color/stage`, for the window and launcher icon. */
    val Stage = Color(0xFF020617)

    /** `colors.panel` (slate-900) at 70%: the panel's surface. */
    val Panel = Color(0xB30F172A)

    /** The panel's own color, opaque: fills an off step dot (`bg-slate-900`). */
    val PanelSolid = Color(0xFF0F172A)

    /** `colors.panel-border` (slate-800): the panel's hairline, and an active sound segment's fill. The stage glow is this at 55%. */
    val PanelBorder = Color(0xFF1E293B)

    /** `colors.label` (slate-400): uppercase panel labels, the BPM unit, and an off dot's ring. */
    val Label = Color(0xFF94A3B8)

    /** `colors.control-text` (slate-300): toggle pill and sound picker text at rest. */
    val ControlText = Color(0xFFCBD5E1)

    /** `colors.control-fill-pressed` (slate-700): a pressed pill's fill, and the slider and step tracks. */
    val ControlFillPressed = Color(0xFF334155)

    /** The sound picker's track border (slate-600). */
    val PickerBorder = Color(0xFF475569)

    /** The BPM numeral's ink (slate-200). */
    val Numeral = Color(0xFFE2E8F0)

    /** `colors.label-muted` (slate-500): captions on the stage, such as "Bell". Also `control-border`, a pill's edge at rest. */
    val LabelMuted = Color(0xFF64748B)

    /** The title's ink: stone-200, from `HomeView.vue`'s header (not a DESIGN.md token). */
    val Title = Color(0xFFE7E5E4)

    /** The header caption: stone-500, `colors.wood-neutral`. Stone belongs to the header zone. */
    val Caption = Color(0xFF78716C)

    /** The Three-Sound Rule: these three mean Bass, Tone, and Click, and nothing else. */
    val BassSky = Color(0xFF38BDF8)
    val ToneAmber = Color(0xFFFBBF24)
    val ClickEmber = Color(0xFFF97316)

    fun forSound(sound: Sound): Color = when (sound) {
        Sound.Bass -> BassSky
        Sound.Edge -> ToneAmber
        Sound.Click -> ClickEmber
    }

    /** `SOUND_META[sound].dot`: a step dot and a picker swatch, one shade deeper (sky-500, amber-500, orange-500). */
    fun dotFor(sound: Sound): Color = when (sound) {
        Sound.Bass -> Color(0xFF0EA5E9)
        Sound.Edge -> Color(0xFFF59E0B)
        Sound.Click -> ClickEmber
    }

    /** `SOUND_META[sound].text`: an active picker segment's label (Click is orange-400). */
    fun textFor(sound: Sound): Color = if (sound == Sound.Click) Color(0xFFFB923C) else forSound(sound)
}

/**
 * Fraunces "soft" variable font, copied from the Mac app's bundled TTF.
 * The One Display Face Rule: the title and headline numerals only.
 */
object Fonts {
    /** DESIGN.md `typography.display`, weight 600. */
    val Display = FontFamily(
        Font(
            R.font.fraunces,
            weight = FontWeight.SemiBold,
            // Pin the variable font's wght axis to the same weight (its default is 900).
            variationSettings = FontVariation.Settings(FontWeight.SemiBold, FontStyle.Normal),
        ),
    )

    /** DESIGN.md `typography.numeral`: 900 for step counts, 700 for BPM. */
    val Numeral = FontFamily(
        Font(R.font.fraunces, weight = FontWeight.Bold, variationSettings = FontVariation.Settings(FontWeight.Bold, FontStyle.Normal)),
        Font(R.font.fraunces, weight = FontWeight.Black, variationSettings = FontVariation.Settings(FontWeight.Black, FontStyle.Normal)),
    )
}

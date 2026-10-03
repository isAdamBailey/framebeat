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

    /** `colors.panel-border` (slate-800). The stage glow is this at 55%. */
    val PanelBorder = Color(0xFF1E293B)

    /** `colors.label-muted` (slate-500): captions on the stage, such as "Bell". */
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
}

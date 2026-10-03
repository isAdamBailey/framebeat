package io.adambailey.framebeat.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import io.adambailey.framebeat.R

/** Colors from DESIGN.md and the web header. */
object Palette {
    /** `colors.stage` (slate-950): the page background. */
    val Stage = Color(0xFF020617)

    /** The title's ink: stone-200, from `HomeView.vue`'s header (not a DESIGN.md token). */
    val Title = Color(0xFFE7E5E4)
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

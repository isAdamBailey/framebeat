package io.adambailey.framebeat.ui

import kotlin.math.max
import kotlin.math.min

/**
 * How big the drum and bell are, from DESIGN.md's "Phone and Tablet (native)"
 * section: one composition at two sizes. Every length is in dp.
 *
 * Compact width (under [BREAKPOINT]): the drum takes the row first, up to
 * [DRUM_MAX], and the bell takes what is left, between [BELL_MIN_SCALE] and
 * [BELL_COMPACT_MAX_SCALE]. The column scrolls, so the stage never shrinks for height.
 *
 * Expanded width: the iPad stage, a [DRUM_MAX] drum and the bell at
 * [BELL_EXPANDED_SCALE]. When the window is short, both scale down together
 * by [stageScale], to a floor of [MIN_STAGE_SCALE], as `ContentView.swift` does.
 */
data class StageLayout(
    val expanded: Boolean,
    /** Side gutter of the column. */
    val gutter: Float,
    val drumWidth: Float,
    /** The bell art's scale over its [BELL_WIDTH] × [BELL_HEIGHT] base. */
    val bellScale: Float,
    /** Space between the drum and the bell. */
    val gap: Float,
    /** 1, or less when a short expanded window shrinks the stage. Also scales the stage glow. */
    val stageScale: Float,
) {
    val drumHeight: Float get() = drumWidth * DRUM_ASPECT

    companion object {
        /** Android's compact/medium window boundary. */
        const val BREAKPOINT = 600f

        const val DRUM_MAX = 340f

        /** The drum box's height over its width: the web's `aspect-[32/30]`. */
        const val DRUM_ASPECT = 30f / 32f

        const val BELL_WIDTH = 112f
        const val BELL_HEIGHT = 136f
        const val BELL_MIN_SCALE = 0.5f
        const val BELL_COMPACT_MAX_SCALE = 0.94f
        const val BELL_EXPANDED_SCALE = 1.625f

        const val COMPACT_GUTTER = 16f
        const val EXPANDED_GUTTER = 24f
        const val COMPACT_GAP = 8f
        const val EXPANDED_GAP = 24f

        /** The expanded column's widest, the web's `max-w-3xl`. */
        const val COLUMN_MAX = 768f

        const val MIN_STAGE_SCALE = 0.45f

        /**
         * The layout for a window [windowWidth] wide, with [stageRoom] of height
         * left for the drum once everything else in the column is placed. Only
         * an expanded window reads [stageRoom].
         */
        fun of(windowWidth: Float, stageRoom: Float = Float.POSITIVE_INFINITY): StageLayout {
            if (windowWidth < BREAKPOINT) {
                val row = windowWidth - 2 * COMPACT_GUTTER
                val drum = max(0f, min(DRUM_MAX, row - COMPACT_GAP - BELL_WIDTH * BELL_MIN_SCALE))
                val bell = ((row - COMPACT_GAP - drum) / BELL_WIDTH).coerceIn(BELL_MIN_SCALE, BELL_COMPACT_MAX_SCALE)
                return StageLayout(false, COMPACT_GUTTER, drum, bell, COMPACT_GAP, stageScale = 1f)
            }
            val scale = (stageRoom / (DRUM_MAX * DRUM_ASPECT)).coerceIn(MIN_STAGE_SCALE, 1f)
            return StageLayout(true, EXPANDED_GUTTER, DRUM_MAX * scale, BELL_EXPANDED_SCALE * scale, EXPANDED_GAP, scale)
        }
    }
}

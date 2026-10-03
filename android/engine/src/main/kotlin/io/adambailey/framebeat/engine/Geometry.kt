package io.adambailey.framebeat.engine

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot

// Port of src/lib/geometry.ts: drum-ellipse and mallet-aim math, with no UI dependency.

/** A point in drum-ellipse units: (0, 0) is the center and ±1 is the rim. */
data class DrumPoint(val x: Double, val y: Double)

/** A tap classified into a zone. [dx] and [dy] are in drum-ellipse units, clamped to 1.15. */
data class DrumHit(val sound: Sound, val side: Side, val dx: Double, val dy: Double)

/**
 * The mallet's swing toward a point: [rotation] in degrees relative to the mallet's rest tilt,
 * and a shift of [x] and [y] as fractions of the mallet's own box. The web formats the shift
 * as a percentage string rounded to 0.01%; this keeps the unrounded fraction.
 */
data class MalletSwing(val rotation: Double, val x: Double, val y: Double)

object DrumGeometry {
    // Drum ellipse as fractions of the canvas box.
    const val CENTER_X = 0.5
    const val CENTER_Y = 0.4
    const val HALF_W = 0.5
    const val HALF_H = 0.34

    // Canvas-unit geometry (canvas = 100 x 93.75): where each mallet pivots and
    // how far its felt tip sits from that pivot, so strikes can be aimed exactly.
    const val CANVAS_W = 100.0
    const val CANVAS_H = 93.75

    /** The mallet's box as a fraction of the canvas, `Mallet.vue`'s `h-[58%] w-[38%]`. */
    const val MALLET_BOX_W = 0.38
    const val MALLET_BOX_H = 0.58

    /** Felt-tip distance from the pivot, in canvas units. */
    const val REACH = 0.7 * MALLET_BOX_H * CANVAS_H

    private val leftPivot = DrumPoint(6.56, 89.7)
    private val rightPivot = DrumPoint(93.44, 89.7)

    /** The mallet's rest tilt in degrees. [swing]'s rotation is relative to it. */
    fun baseRotation(side: Side): Double = if (side == Side.Left) 24.0 else -24.0

    // Each mallet rests on the frame's side: that resting spot is the Click
    // zone. Edge sits two thirds of the way in from there, Bass at the centre.
    private fun clickPoint(side: Side) = if (side == Side.Left) DrumPoint(-0.56, 0.55) else DrumPoint(0.56, 0.55)

    fun zonePoint(sound: Sound, side: Side): DrumPoint {
        val click = clickPoint(side)
        return when (sound) {
            Sound.Click -> click
            Sound.Edge -> DrumPoint(click.x * 2 / 3, click.y * 2 / 3)
            Sound.Bass -> DrumPoint(0.0, 0.0)
        }
    }

    /**
     * Classifies a tap at fractional position ([x], [y]) within the drum illustration's box.
     * Returns null for a tap in the empty space around the drum.
     */
    fun classify(x: Double, y: Double): DrumHit? {
        val dx = (x - CENTER_X) / HALF_W
        val dy = (y - CENTER_Y) / HALF_H
        val dist = hypot(dx, dy)
        if (dist > 1.35) return null
        // Zone radii match the mallet aim: Bass < 0.26 < Edge < 0.65 < Click.
        val sound = if (dist < 0.26) Sound.Bass else if (dist < 0.65) Sound.Edge else Sound.Click
        val clamp = if (dist > 1.15) 1.15 / dist else 1.0
        return DrumHit(sound, if (dx < 0) Side.Left else Side.Right, dx * clamp, dy * clamp)
    }

    /** Rotation and shift that land the felt tip on the drum-ellipse point ([gx], [gy]). */
    fun swing(side: Side, gx: Double, gy: Double): MalletSwing {
        val pivot = if (side == Side.Left) leftPivot else rightPivot
        val rx = (CENTER_X + HALF_W * gx) * CANVAS_W - pivot.x
        val ry = (CENTER_Y + HALF_H * gy) * CANVAS_H - pivot.y
        val d = hypot(rx, ry).takeIf { it != 0.0 } ?: 1.0
        val angle = atan2(rx, -ry) * 180 / PI // 0 = straight up
        val boxW = MALLET_BOX_W * CANVAS_W
        val boxH = MALLET_BOX_H * CANVAS_H
        return MalletSwing(
            rotation = angle - baseRotation(side),
            x = (rx - REACH * rx / d) / boxW,
            y = (ry - REACH * ry / d) / boxH,
        )
    }
}

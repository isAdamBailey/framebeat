package io.adambailey.framebeat.engine

// Port of src/lib/controls.ts: the instrument's starting state, control ranges,
// and keyboard shortcuts.

const val MAX_STEPS = 16

object Defaults {
    const val BPM = 90
    const val CHIME_ON_ONE = true
    val top = Line(count = 3, sound = Sound.Edge)
    val bottom = Line(count = 4, sound = Sound.Bass)

    /** A fresh line: every step filled in, not muted. */
    fun line(line: LineId): Line = if (line == LineId.Top) top else bottom
}

object Ranges {
    val bpm = 40..200
    val count = 1..MAX_STEPS
}

/** A drum shortcut: which mallet strikes, and which zone it hits. */
data class DrumKey(val side: Side, val sound: Sound)

/**
 * Q W E / I O P mirror the qwerty row's own left-right symmetry outward-in:
 * Q and P sit at the outer ends, so they play the rim Click zone; E and I sit
 * innermost, so they play the centre Bass zone; W and O in between play Tone.
 * The arrows are a quick tone strike per side.
 *
 * Keys are the web's `KeyboardEvent.key`, lowercased. The app maps its own key
 * codes to these names.
 */
val DRUM_KEYS: Map<String, DrumKey> = mapOf(
    "q" to DrumKey(Side.Left, Sound.Click),
    "w" to DrumKey(Side.Left, Sound.Edge),
    "e" to DrumKey(Side.Left, Sound.Bass),
    "arrowleft" to DrumKey(Side.Left, Sound.Edge),
    "i" to DrumKey(Side.Right, Sound.Bass),
    "o" to DrumKey(Side.Right, Sound.Edge),
    "p" to DrumKey(Side.Right, Sound.Click),
    "arrowright" to DrumKey(Side.Right, Sound.Edge),
)

enum class InstrumentShortcut { PlayPause, Bell }

/**
 * Space plays or pauses; B rings the bell. Key repeat and Ctrl, Alt, or Meta
 * chords are ignored for both. [key] is the web's `KeyboardEvent.key`.
 */
fun instrumentShortcut(
    key: String,
    repeat: Boolean,
    meta: Boolean,
    ctrl: Boolean,
    alt: Boolean,
): InstrumentShortcut? {
    if (repeat || meta || ctrl || alt) return null
    if (key == " ") return InstrumentShortcut.PlayPause
    if (key.lowercase() == "b") return InstrumentShortcut.Bell
    return null
}

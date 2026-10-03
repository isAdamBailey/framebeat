package io.adambailey.framebeat.engine

// Port of src/types/drum.ts: the shared domain types.

/** A drum voice. [id] is the web's string value; [label] is `SOUND_META[sound].label` in src/lib/drumSounds.ts. */
enum class Sound(val id: String, val label: String) {
    Bass("bass", "Bass"),
    Edge("edge", "Tone"),
    Click("click", "Click"),
    ;

    companion object {
        fun fromId(id: String): Sound? = entries.firstOrNull { it.id == id }
    }
}

/** Which mallet: the web's `'left' | 'right'`. */
enum class Side(val id: String) {
    Left("left"),
    Right("right"),
    ;

    companion object {
        fun fromId(id: String): Side? = entries.firstOrNull { it.id == id }
    }
}

/** Which step line: the web's `'top' | 'bottom'`. */
enum class LineId { Top, Bottom }

/** One step line. [dots] always holds [MAX_STEPS] entries; only the first [count] play. */
data class Line(
    val count: Int,
    val sound: Sound,
    val dots: List<Boolean> = List(MAX_STEPS) { true },
    val muted: Boolean = false,
)

/** A strike from the sequencer for one line. [id] bumps on every strike, so a repeated sound is still a new event. */
data class Strike(val sound: Sound, val line: LineId, val id: Int)

/** The most recent strike per line. */
data class Strikes(val top: Strike? = null, val bottom: Strike? = null)

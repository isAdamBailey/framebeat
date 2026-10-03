package io.adambailey.framebeat.engine

// Port of src/lib/voiceSpec.ts: every voice's parameters as data. Copied from
// the web, not from VoiceSpec.swift. Times are seconds after the strike.

enum class Waveform(val id: String) {
    Sine("sine"),
    Triangle("triangle"),
}

/** An oscillator through its own gain envelope, stopped at [stop]. */
class OscillatorSpec(val waveform: Waveform, val freq: Envelope, val gain: Envelope, val stop: Double)

/** A band of looped noise through a filter and a gain envelope, stopped at [stop]. Q 1 is the Web Audio default. */
class NoiseSpec(val filter: FilterType, val frequency: Double, val q: Double, val gain: Envelope, val stop: Double)

class VoiceSpec(val oscillators: List<OscillatorSpec>, val noises: List<NoiseSpec>) {
    /** When the last part stops. */
    val stop: Double = (oscillators.map { it.stop } + noises.map { it.stop }).max()
}

object Voices {
    // Open Bass Tone: pitch-drop sine (100 -> ~58 Hz) + lowpass-filtered noise impact
    val bass = VoiceSpec(
        oscillators = listOf(
            OscillatorSpec(
                Waveform.Sine,
                freq = Envelope(0.0 to 100.0, 0.3 to 58.0),
                gain = Envelope(0.0 to 0.0001, 0.008 to 0.9, 0.55 to 0.0001),
                stop = 0.6,
            ),
        ),
        noises = listOf(
            NoiseSpec(FilterType.Lowpass, frequency = 350.0, q = 1.0, gain = Envelope(0.0 to 0.5, 0.06 to 0.0001), stop = 0.08),
        ),
    )

    // Open Tone: a warm, pitched hand tone pitched about an octave above the
    // bass: a sine dropping 220 -> 150 Hz, the drumhead's first overtone at
    // ~1.59x the fundamental, and a soft band-limited skin thump for the attack.
    val edge = VoiceSpec(
        oscillators = listOf(
            OscillatorSpec(
                Waveform.Sine,
                freq = Envelope(0.0 to 220.0, 0.18 to 150.0),
                gain = Envelope(0.0 to 0.0001, 0.006 to 0.75, 0.34 to 0.0001),
                stop = 0.38,
            ),
            OscillatorSpec(
                Waveform.Sine,
                freq = Envelope(0.0 to 350.0, 0.12 to 240.0),
                gain = Envelope(0.0 to 0.0001, 0.004 to 0.22, 0.16 to 0.0001),
                stop = 0.2,
            ),
        ),
        noises = listOf(
            NoiseSpec(FilterType.Bandpass, frequency = 700.0, q = 0.9, gain = Envelope(0.0 to 0.35, 0.04 to 0.0001), stop = 0.06),
        ),
    )

    // Rim Click: a pronounced wooden knock for hits on the frame's side:
    // sharp high transient + a resonant woodblock body.
    val click = VoiceSpec(
        oscillators = listOf(
            OscillatorSpec(
                Waveform.Triangle,
                freq = Envelope(0.0 to 1450.0, 0.07 to 1050.0),
                gain = Envelope(0.0 to 0.55, 0.09 to 0.0001),
                stop = 0.1,
            ),
            OscillatorSpec(Waveform.Sine, freq = Envelope(0.0 to 2400.0), gain = Envelope(0.0 to 0.25, 0.035 to 0.0001), stop = 0.04),
        ),
        noises = listOf(
            NoiseSpec(FilterType.Highpass, frequency = 2200.0, q = 1.0, gain = Envelope(0.0 to 1.0, 0.03 to 0.0001), stop = 0.05),
        ),
    )

    // Bell chime marking the start of each bar ("the one"), also struck by hand.
    // A small bell's partials around an E5 strike note: a low hum an octave
    // below, the minor-third tierce and the quint that give a bell its color,
    // and the octave nominal. Lower partials ring longest, so it blooms and
    // then settles onto the hum.
    private class Partial(val freq: Double, val gain: Double, val dur: Double)

    private val dingPartials = listOf(
        Partial(329.6, 0.07, 1.8),
        Partial(659.3, 0.2, 1.4),
        Partial(790.0, 0.06, 0.9),
        Partial(988.0, 0.045, 0.7),
        Partial(1318.5, 0.04, 0.5),
    )

    val ding = VoiceSpec(
        oscillators = dingPartials.map {
            OscillatorSpec(
                Waveform.Sine,
                freq = Envelope(0.0 to it.freq),
                gain = Envelope(0.0 to 0.0001, 0.005 to it.gain, it.dur to 0.0001),
                stop = it.dur + 0.05,
            )
        },
        noises = emptyList(),
    )

    fun forSound(sound: Sound): VoiceSpec = when (sound) {
        Sound.Bass -> bass
        Sound.Edge -> edge
        Sound.Click -> click
    }
}

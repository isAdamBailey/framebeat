import Foundation

/// The single source of truth for every drum voice's parameters — copied
/// verbatim from `src/lib/drumAudio.ts`. Both `OfflineRenderer` (bulk,
/// array-based rendering — used by `frame-beat-render`/tests) and
/// `LiveAudioEngine` (streaming, sample-by-sample rendering for real
/// playback) build their voices from these same specs, so the two
/// rendering models can never drift apart on what a "bass" hit actually
/// sounds like.
public enum Waveform: Sendable { case sine, triangle }

public struct OscillatorSpec: Sendable {
    public let waveform: Waveform
    public let freq: Envelope
    public let gain: Envelope
}

public struct FilteredNoiseSpec: Sendable {
    public let kind: BiquadFilter.Kind
    public let frequency: Double
    public let q: Double
    public let gain: Envelope
}

public enum VoiceSpec {
    /// Every drum-hit voice (bass/edge/click) as its oscillator and
    /// filtered-noise components.
    public static func components(for sound: Sound) -> (oscillators: [OscillatorSpec], noises: [FilteredNoiseSpec]) {
        switch sound {
        case .bass:
            return (
                oscillators: [
                    OscillatorSpec(waveform: .sine,
                        freq: Envelope([(0, 100), (0.3, 58)]),
                        gain: Envelope([(0, 0.0001), (0.008, 0.9), (0.55, 0.0001)])),
                ],
                noises: [
                    FilteredNoiseSpec(kind: .lowpass, frequency: 350, q: 1,
                        gain: Envelope([(0, 0.5), (0.06, 0.0001)])),
                ]
            )
        case .edge:
            // Open Tone: warm pitched hand tone about an octave above the
            // bass, plus the drumhead's ~1.59x overtone and a soft skin thump.
            return (
                oscillators: [
                    OscillatorSpec(waveform: .sine,
                        freq: Envelope([(0, 220), (0.18, 150)]),
                        gain: Envelope([(0, 0.0001), (0.006, 0.75), (0.34, 0.0001)])),
                    OscillatorSpec(waveform: .sine,
                        freq: Envelope([(0, 350), (0.12, 240)]),
                        gain: Envelope([(0, 0.0001), (0.004, 0.22), (0.16, 0.0001)])),
                ],
                noises: [
                    FilteredNoiseSpec(kind: .bandpass, frequency: 700, q: 0.9,
                        gain: Envelope([(0, 0.35), (0.04, 0.0001)])),
                ]
            )
        case .click:
            return (
                oscillators: [
                    OscillatorSpec(waveform: .triangle,
                        freq: Envelope([(0, 1450), (0.07, 1050)]),
                        gain: Envelope([(0, 0.55), (0.09, 0.0001)])),
                    OscillatorSpec(waveform: .sine,
                        freq: Envelope([(0, 2400)]),
                        gain: Envelope([(0, 0.25), (0.035, 0.0001)])),
                ],
                noises: [
                    FilteredNoiseSpec(kind: .highpass, frequency: 2200, q: 1,
                        gain: Envelope([(0, 1.0), (0.03, 0.0001)])),
                ]
            )
        }
    }

    /// The bell's partials around an E5 strike note: hum, strike, tierce,
    /// quint, and nominal — matching `playDing()` in `drumAudio.ts`.
    public static func dingComponents() -> [OscillatorSpec] {
        let partials: [(freq: Double, gain: Double, dur: Double)] = [
            (329.6, 0.07, 1.8),
            (659.3, 0.2, 1.4),
            (790.0, 0.06, 0.9),
            (988.0, 0.045, 0.7),
            (1318.5, 0.04, 0.5),
        ]
        return partials.map {
            OscillatorSpec(waveform: .sine,
                freq: Envelope([(0, $0.freq)]),
                gain: Envelope([(0, 0.0001), (0.005, $0.gain), ($0.dur, 0.0001)]))
        }
    }
}

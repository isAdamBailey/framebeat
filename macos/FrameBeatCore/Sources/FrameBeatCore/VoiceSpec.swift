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
            return (
                oscillators: [
                    OscillatorSpec(waveform: .triangle,
                        freq: Envelope([(0, 420), (0.06, 180)]),
                        gain: Envelope([(0, 0.4), (0.12, 0.0001)])),
                ],
                noises: [
                    FilteredNoiseSpec(kind: .bandpass, frequency: 2600, q: 1.4,
                        gain: Envelope([(0, 0.7), (0.09, 0.0001)])),
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

    /// The bar-marker chime's three inharmonic sine partials.
    public static func dingComponents() -> [OscillatorSpec] {
        let partials: [(freq: Double, gain: Double, dur: Double)] = [
            (1318.5, 0.15, 1.1),
            (1975.5, 0.06, 0.8),
            (2637.0, 0.035, 0.6),
        ]
        return partials.map {
            OscillatorSpec(waveform: .sine,
                freq: Envelope([(0, $0.freq)]),
                gain: Envelope([(0, 0.0001), (0.005, $0.gain), ($0.dur, 0.0001)]))
        }
    }
}

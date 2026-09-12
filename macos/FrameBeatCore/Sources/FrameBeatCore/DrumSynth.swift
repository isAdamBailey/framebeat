import Foundation

/// Offline (non-realtime) renderer for the four drum voices, ported 1:1 from
/// `src/lib/drumAudio.ts`. All envelope timings/values below are copied
/// verbatim from that file — see the comment above each `play...` function
/// there for what each parameter represents.
///
/// This renders into an in-memory sample buffer rather than driving a live
/// `AVAudioEngine`, so it can be exercised (and diffed against the web app)
/// from the command line without an app bundle, entitlements, or a signed
/// build — see `frame-beat-render`. The real-time engine (Phase 3 of the
/// port) wraps this same per-voice math in an `AVAudioSourceNode` render
/// callback driven by a sample-accurate trigger queue.
public final class OfflineRenderer {
    public let sampleRate: Double
    public private(set) var buffer: [Double]
    private let noise: [Double]
    private let masterGain = 0.85

    /// - Parameters:
    ///   - durationSeconds: length of audio to allocate for triggered events;
    ///     a 1s tail is added automatically so the longest voice (the ~1.15s
    ///     bar-chime ding) never gets truncated near the end of the buffer.
    public init(sampleRate: Double, durationSeconds: Double) {
        self.sampleRate = sampleRate
        let n = Int((durationSeconds * sampleRate).rounded(.up)) + Int(sampleRate)
        buffer = Array(repeating: 0, count: n)
        noise = OfflineRenderer.makeNoiseTable(sampleRate: sampleRate)
    }

    // A single shared 1-second noise table, read from index 0 on every
    // strike — matches drumAudio.ts's module-level `noiseBuffer` singleton
    // (one `AudioBuffer`, `loop = true`, restarted from t=0 each trigger)
    // rather than a fresh RNG draw per voice, so every noise-based voice in
    // one render reuses the same table deterministically. This does NOT
    // make a rendered pattern directly A/B-able against a web recording —
    // the two noise tables are independently random draws (same statistics,
    // different values) and the web app's DynamicsCompressorNode reshapes
    // the mix in a way `finalizeMono()`'s tanh soft-clip does not. Compare
    // per-voice (single strikes, filter impulse responses) instead of whole
    // rendered patterns.
    private static func makeNoiseTable(sampleRate: Double) -> [Double] {
        NoiseTable.make(sampleRate: sampleRate)
    }

    public func trigger(_ sound: Sound, atSample startSample: Int) {
        let spec = VoiceSpec.components(for: sound)
        for osc in spec.oscillators {
            renderOscillator(startSample: startSample, waveform: osc.waveform, freq: osc.freq, gain: osc.gain)
        }
        for noise in spec.noises {
            renderFilteredNoise(startSample: startSample, kind: noise.kind, frequency: noise.frequency, q: noise.q, gain: noise.gain)
        }
    }

    public func triggerDing(atSample startSample: Int) {
        for osc in VoiceSpec.dingComponents() {
            renderOscillator(startSample: startSample, waveform: osc.waveform, freq: osc.freq, gain: osc.gain)
        }
    }

    // MARK: - Rendering primitives

    /// Phase-accumulator oscillator: `phase += 2π·f[n]/sampleRate` per
    /// sample, i.e. numerically integrating instantaneous frequency, rather
    /// than evaluating `sin(2π·f(t)·t)` — the latter is only correct for
    /// constant frequency and gets the open-bass pitch glide audibly wrong.
    ///
    /// `triangle` uses a closed-form (non-band-limited) triangle wave; Web
    /// Audio's built-in triangle oscillator is band-limited. The harmonic
    /// difference is small (triangle harmonics already roll off as 1/n²) but
    /// unverified by ear — check the edge-slap/wood-click voices first if a
    /// side-by-side comparison sounds "thinner" or "brighter" than expected.
    private func renderOscillator(startSample: Int, waveform: Waveform, freq: Envelope, gain: Envelope) {
        let dur = max(freq.duration, gain.duration)
        let n = Int(dur * sampleRate) + 1
        let dt = 1.0 / sampleRate
        var phase = 0.0
        for i in 0..<n {
            let t = Double(i) * dt
            phase += 2 * Double.pi * freq.value(at: t) * dt
            let s: Double
            switch waveform {
            case .sine: s = sin(phase)
            case .triangle: s = (2 / Double.pi) * asin(sin(phase))
            }
            addSample(startSample + i, s * gain.value(at: t) * masterGain)
        }
    }

    private func renderFilteredNoise(startSample: Int, kind: BiquadFilter.Kind, frequency: Double, q: Double, gain: Envelope) {
        var filter = BiquadFilter(kind: kind, frequency: frequency, q: q, sampleRate: sampleRate)
        let n = Int(gain.duration * sampleRate) + 1
        for i in 0..<n {
            let raw = noise[i % noise.count]
            let filtered = filter.process(raw)
            let t = Double(i) / sampleRate
            addSample(startSample + i, filtered * gain.value(at: t) * masterGain)
        }
    }

    private func addSample(_ index: Int, _ value: Double) {
        guard index >= 0, index < buffer.count else { return }
        buffer[index] += value
    }

    /// Soft-clip the finished mix and return interleaved `Float`s, standing
    /// in for drumAudio.ts's `DynamicsCompressorNode` (a real compressor's
    /// attack/release curve isn't reproduced here — this is a peak-safety net
    /// only, not a timbral match).
    public func finalizeMono() -> [Float] {
        buffer.map { Float(tanh($0)) }
    }
}

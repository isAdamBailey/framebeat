import Foundation

/// A component of a live (real-time, streaming) voice: something that can
/// produce one sample at a time given "how many samples since this
/// component started," and knows when it's done. Unlike `OfflineRenderer`
/// (which renders a whole voice into an array in one call), this is the
/// pull-based shape `AVAudioSourceNode`'s render callback needs — it asks
/// for N frames at a time and nothing more, on the audio thread, where
/// allocating or blocking is not allowed.
///
/// Built from the same `OscillatorSpec`/`FilteredNoiseSpec` tables in
/// `VoiceSpec.swift` that `OfflineRenderer` uses, so a live-triggered hit
/// and an offline-rendered one are driven by identical parameters.
public protocol LiveVoiceComponent: AnyObject {
    /// Total time (0-based) `next()` continues producing non-silent
    /// samples for.
    var duration: Double { get }
    /// Advances the component by exactly one sample and returns it. Must be
    /// called at a steady sample rate starting from local time 0 — these
    /// components are stateful (running phase, filter memory), not
    /// evaluable at an arbitrary offset out of order.
    func next() -> Double
}

public final class LiveOscillatorComponent: LiveVoiceComponent {
    private let waveform: Waveform
    private let freq: Envelope
    private let gain: Envelope
    private let sampleRate: Double
    private var phase: Double = 0
    private var sampleIndex: Int = 0

    public var duration: Double { max(freq.duration, gain.duration) }

    public init(spec: OscillatorSpec, sampleRate: Double) {
        self.waveform = spec.waveform
        self.freq = spec.freq
        self.gain = spec.gain
        self.sampleRate = sampleRate
    }

    public func next() -> Double {
        let dt = 1.0 / sampleRate
        let t = Double(sampleIndex) * dt
        phase += 2 * Double.pi * freq.value(at: t) * dt
        sampleIndex += 1
        let s: Double
        switch waveform {
        case .sine: s = sin(phase)
        case .triangle: s = (2 / Double.pi) * asin(sin(phase))
        }
        return s * gain.value(at: t)
    }
}

public final class LiveFilteredNoiseComponent: LiveVoiceComponent {
    private var filter: BiquadFilter
    private let gain: Envelope
    private let sampleRate: Double
    private let noiseTable: [Double]
    private var sampleIndex: Int = 0

    public var duration: Double { gain.duration }

    public init(spec: FilteredNoiseSpec, sampleRate: Double, noiseTable: [Double]) {
        self.filter = BiquadFilter(kind: spec.kind, frequency: spec.frequency, q: spec.q, sampleRate: sampleRate)
        self.gain = spec.gain
        self.sampleRate = sampleRate
        self.noiseTable = noiseTable
    }

    public func next() -> Double {
        let t = Double(sampleIndex) / sampleRate
        let raw = noiseTable[sampleIndex % noiseTable.count]
        let filtered = filter.process(raw)
        sampleIndex += 1
        return filtered * gain.value(at: t)
    }
}

/// A whole drum hit (all its oscillator/noise components) plus the
/// absolute sample time it should start at. Built by `LiveAudioEngine`
/// from `VoiceSpec`; advanced by the render callback.
public final class ActiveVoice {
    public let startSample: Int64
    private let sampleRate: Double
    private var components: [LiveVoiceComponent]

    public init(startSample: Int64, components: [LiveVoiceComponent], sampleRate: Double) {
        self.startSample = startSample
        self.components = components
        self.sampleRate = sampleRate
    }

    public var isFinished: Bool { components.isEmpty }

    /// Mixes this voice's contribution into one output sample at
    /// `globalSample`, dropping components as they finish.
    public func sample(atGlobalSample globalSample: Int64) -> Double {
        guard globalSample >= startSample else { return 0 }
        let t = Double(globalSample - startSample) / sampleRate
        var sum = 0.0
        components.removeAll { component in
            if t > component.duration { return true }
            sum += component.next()
            return false
        }
        return sum
    }
}

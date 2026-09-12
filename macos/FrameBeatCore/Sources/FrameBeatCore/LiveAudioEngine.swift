import AVFoundation
import os

/// The Phase 3 real-time audio engine: `AVAudioEngine` → one
/// `AVAudioSourceNode` (sample-accurate render callback) → `mainMixerNode`
/// → output. Triggers are queued and consumed on the engine's own sample
/// clock, so `trigger(_:atSample:)` with a future sample starts exactly on
/// that sample — the native equivalent of Web Audio's `osc.start(t)`.
///
/// Renders incrementally, sample by sample, inside the real-time callback
/// itself, rather than pre-rendering each hit to a buffer and handing it to
/// a player-node pool — that alternative can't guarantee sample-accurate
/// start times or avoid per-hit allocation/rendering overhead.
public final class LiveAudioEngine {
    private let engine = AVAudioEngine()
    private let format: AVAudioFormat
    public let sampleRate: Double
    private let noiseTable: [Double]
    private let masterGain = 0.85

    // MARK: - State shared between the calling thread and the audio thread

    /// Guards `pendingTriggers` and `sampleClock`. A short, O(1),
    /// non-allocating critical section (array append/removeAll, an Int64
    /// read/write) guarded by `os_unfair_lock` rather than a true lock-free
    /// SPSC ring buffer. Real-time-audio guidance is right that a *blocking*
    /// lock on the audio thread is unsafe (priority inversion, unbounded
    /// wait), but a spinlock around work this short and non-blocking is
    /// standard practice in shipping engines (JUCE's `CriticalSection` does
    /// the same). A genuinely lock-free ring buffer would be a safer future
    /// upgrade, but isn't required for a queue that grows by ~1 entry per
    /// drum hit rather than at audio rate.
    private var lock = os_unfair_lock()
    private var pendingTriggers: [(sample: Int64, sound: Sound?)] = [] // sound == nil means the bar-chime ding
    private var sampleClock: Int64 = 0

    /// Touched only inside the render callback (the audio thread) — never
    /// read or written from any other thread.
    private var activeVoices: [ActiveVoice] = []

    public init(sampleRate: Double = 48000) {
        self.sampleRate = sampleRate
        format = AVAudioFormat(standardFormatWithSampleRate: sampleRate, channels: 1)!
        noiseTable = NoiseTable.make(sampleRate: sampleRate)
    }

    /// The engine's current position on its own sample clock. Pass
    /// `now + n` to `trigger`/`triggerDing` to schedule a hit `n` samples in
    /// the future; a past or present sample plays back as soon as the next
    /// render callback runs (clamped, like drumAudio.ts's
    /// `Math.max(when, audioCtx.currentTime)`).
    public var now: Int64 {
        os_unfair_lock_lock(&lock)
        defer { os_unfair_lock_unlock(&lock) }
        return sampleClock
    }

    /// Output latency in seconds, for compensating visual sync the way
    /// `useSequencer.ts`'s `frame()` does with `outputLatency`/`baseLatency`.
    /// Commonly 0 on macOS depending on the output device — callers should
    /// not assume this alone is sufficient and should measure/verify on the
    /// actual device before relying on it (see Phase 4 notes).
    public var outputLatencySeconds: Double {
        engine.outputNode.presentationLatency
    }

    public func start() throws {
        let source = AVAudioSourceNode(format: format) { [weak self] _, _, frameCount, audioBufferList in
            self?.render(frameCount: frameCount, into: audioBufferList) ?? noErr
        }
        engine.attach(source)
        engine.connect(source, to: engine.mainMixerNode, format: format)
        try engine.start()
    }

    public func stop() {
        engine.stop()
    }

    public func trigger(_ sound: Sound, atSample sample: Int64) {
        os_unfair_lock_lock(&lock)
        pendingTriggers.append((sample, sound))
        os_unfair_lock_unlock(&lock)
    }

    public func triggerDing(atSample sample: Int64) {
        os_unfair_lock_lock(&lock)
        pendingTriggers.append((sample, nil))
        os_unfair_lock_unlock(&lock)
    }

    private func makeComponents(for sound: Sound?) -> [LiveVoiceComponent] {
        if let sound {
            let spec = VoiceSpec.components(for: sound)
            var components: [LiveVoiceComponent] = spec.oscillators.map {
                LiveOscillatorComponent(spec: $0, sampleRate: sampleRate)
            }
            components += spec.noises.map {
                LiveFilteredNoiseComponent(spec: $0, sampleRate: sampleRate, noiseTable: noiseTable)
            }
            return components
        }
        return VoiceSpec.dingComponents().map { LiveOscillatorComponent(spec: $0, sampleRate: sampleRate) }
    }

    /// Runs on the real-time audio thread on every buffer pull. Building
    /// `ActiveVoice`/component objects for newly-due triggers does allocate
    /// — acceptable for a drum machine's trigger rate (at most a handful of
    /// hits per buffer), not something a latency-critical pro-audio engine
    /// would accept. Flagged as a known limitation, not a correctness bug;
    /// a pooled/preallocated voice design would remove it if it ever
    /// mattered here.
    private func render(frameCount: AVAudioFrameCount, into audioBufferList: UnsafeMutablePointer<AudioBufferList>) -> OSStatus {
        os_unfair_lock_lock(&lock)
        let startOfBuffer = sampleClock
        let horizon = startOfBuffer + Int64(frameCount)
        var due: [(sample: Int64, sound: Sound?)] = []
        pendingTriggers.removeAll { trigger in
            guard trigger.sample < horizon else { return false }
            due.append(trigger)
            return true
        }
        os_unfair_lock_unlock(&lock)

        for trigger in due {
            let clampedStart = max(trigger.sample, startOfBuffer)
            activeVoices.append(ActiveVoice(
                startSample: clampedStart,
                components: makeComponents(for: trigger.sound),
                sampleRate: sampleRate
            ))
        }

        let buffers = UnsafeMutableAudioBufferListPointer(audioBufferList)
        let out = buffers[0].mData!.assumingMemoryBound(to: Float.self)

        for i in 0..<Int(frameCount) {
            let globalSample = startOfBuffer + Int64(i)
            var mixed = 0.0
            for voice in activeVoices {
                mixed += voice.sample(atGlobalSample: globalSample)
            }
            out[i] = Float(tanh(mixed * masterGain))
        }

        activeVoices.removeAll { $0.isFinished }

        os_unfair_lock_lock(&lock)
        sampleClock = horizon
        os_unfair_lock_unlock(&lock)

        return noErr
    }
}

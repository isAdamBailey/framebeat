import AVFoundation
import FrameBeatCore

/// A minimal real-time playback path for the demo app: each trigger renders
/// a short buffer offline (reusing `OfflineRenderer`'s exact per-voice math)
/// and hands it to a small round-robin pool of `AVAudioPlayerNode`s so
/// overlapping strikes stay polyphonic.
///
/// This is NOT the Phase 3 design in the plan (a single `AVAudioSourceNode`
/// fed by a lock-free sample-accurate trigger queue) — that's the right
/// architecture for the shipped app, giving sample-accurate start times and
/// no per-strike render/allocate overhead. This is the fast path to a
/// runnable, audible demo of the ported synth engine.
public final class RealtimeAudio {
    private let engine = AVAudioEngine()
    private let format: AVAudioFormat
    private let sampleRate = 48000.0
    private var players: [AVAudioPlayerNode] = []
    private var nextPlayer = 0

    public init(voiceCount: Int = 8) {
        format = AVAudioFormat(standardFormatWithSampleRate: sampleRate, channels: 1)!
        for _ in 0..<voiceCount {
            let player = AVAudioPlayerNode()
            engine.attach(player)
            engine.connect(player, to: engine.mainMixerNode, format: format)
            players.append(player)
        }
        try? engine.start()
    }

    public func play(_ sound: Sound) {
        schedule { $0.trigger(sound, atSample: 0) }
    }

    public func playDing() {
        schedule { $0.triggerDing(atSample: 0) }
    }

    private func schedule(_ trigger: (OfflineRenderer) -> Void) {
        let renderer = OfflineRenderer(sampleRate: sampleRate, durationSeconds: 1.3)
        trigger(renderer)
        let mono = renderer.finalizeMono()
        guard let buffer = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: AVAudioFrameCount(mono.count)) else { return }
        buffer.frameLength = AVAudioFrameCount(mono.count)
        buffer.floatChannelData![0].update(from: mono, count: mono.count)

        let player = players[nextPlayer]
        nextPlayer = (nextPlayer + 1) % players.count
        player.stop()
        player.scheduleBuffer(buffer, at: nil)
        player.play()
    }
}

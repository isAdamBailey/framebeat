import Foundation
import FrameBeatCore

/// Demo-only scheduler: generates a batch of events with `Sequencer` up
/// front and fires each one with `DispatchQueue.main.asyncAfter`. Fine for a
/// short demo run; NOT the Phase 4 design (a 25ms look-ahead timer booking a
/// rolling 120ms horizon onto the live audio clock, so tempo/step-count
/// changes mid-play can re-anchor cleanly). This version can't change
/// parameters mid-play — call `stop()` and `start()` again instead.
@MainActor
public final class DemoSequencer {
    public private(set) var isPlaying = false
    public var onStep: ((LineId, Int) -> Void)?
    public var onBell: (() -> Void)?

    private let audio: RealtimeAudio
    private var workItems: [DispatchWorkItem] = []

    public init(audio: RealtimeAudio) {
        self.audio = audio
    }

    public func start(top: Line, bottom: Line, bpm: Double, bars: Int = 200) {
        stop()
        isPlaying = true
        let events = Sequencer.generateEvents(top: top, bottom: bottom, bpm: bpm, bars: bars)
        let startDelay = 0.15
        for event in events {
            let item = DispatchWorkItem { [weak self] in
                guard let self, self.isPlaying else { return }
                switch event.kind {
                case .bell:
                    self.audio.playDing()
                    self.onBell?()
                case .step(let line, let index, let sound, let audible):
                    if audible { self.audio.play(sound) }
                    self.onStep?(line, index)
                }
            }
            workItems.append(item)
            DispatchQueue.main.asyncAfter(deadline: .now() + startDelay + event.time, execute: item)
        }
    }

    public func stop() {
        isPlaying = false
        workItems.forEach { $0.cancel() }
        workItems.removeAll()
    }
}

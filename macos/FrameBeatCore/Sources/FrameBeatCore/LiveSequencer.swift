import Foundation

/// The Phase 4 real-time scheduler: a 25ms look-ahead timer booking both
/// step streams onto `LiveAudioEngine`'s own sample clock, matching
/// `useSequencer.ts`'s `scheduler()`/`anchor()` design. Booking against the
/// engine's own sample clock (rather than firing events with
/// `DispatchQueue.main.asyncAfter`, which measures wall-clock time subject
/// to main-run-loop scheduling jitter) is what makes this drift-free and
/// able to re-anchor mid-play.
///
/// Each step's sample time is computed as `anchor + stepIndex *
/// stepDuration` (see `LiveScheduleMath`), not by accumulating a running
/// total, for the same float-drift reason documented on the offline
/// `Sequencer`.
@MainActor
public final class LiveSequencer {
    public private(set) var isPlaying = false

    /// Fires on every step (audible or not) so a playhead/step-index UI can
    /// track position even on a muted or silent step.
    public var onIndexUpdate: ((LineId, Int) -> Void)?
    /// Fires only when the step is actually sounding (has a dot, line not
    /// muted) — the signal a mallet-strike/ripple animation should key off,
    /// matching `useSequencer.ts`'s `frame()` (`onStep` only called when
    /// `data.dots[event.index] && !data.muted`).
    public var onStrike: ((LineId, Int, Sound) -> Void)?
    /// Fires on every bar boundary regardless of mute state.
    public var onBell: (() -> Void)?
    /// Fires ~120Hz with the bottom line's fractional position through its
    /// bar (0..<1), matching `useSequencer.ts`'s `progress` — drives the
    /// sweeping playhead bar independent of per-step visual events.
    public var onProgressUpdate: ((Double) -> Void)?

    private let engine: LiveAudioEngine
    private var top: Line
    private var bottom: Line
    private var bpm: Double

    private var anchorSample: Int64 = 0
    private var bottomIndex = 0
    private var topIndex = 0
    private var lastBottomSample: Int64 = 0
    private var lastBottomIndex = 0
    private var reanchorRequested = false
    private var pendingTop: Line?
    private var pendingBottom: Line?
    private var pendingBpm: Double?

    private enum QueueEvent {
        case bell(sample: Int64)
        case step(line: LineId, index: Int, sound: Sound, sample: Int64, audible: Bool)
        var sample: Int64 {
            switch self {
            case .bell(let s): return s
            case .step(_, _, _, let s, _): return s
            }
        }
    }
    private var visualQueue: [QueueEvent] = []

    private var schedulerTimer: Timer?
    private var visualTimer: Timer?

    public init(engine: LiveAudioEngine, top: Line, bottom: Line, bpm: Double) {
        self.engine = engine
        self.top = top
        self.bottom = bottom
        self.bpm = bpm
    }

    private var beatDuration: Double { 60.0 / bpm }
    private var barDuration: Double { beatDuration * Double(bottom.count) }
    private var topStepDuration: Double { barDuration / Double(top.count) }

    public func start() {
        stop()
        anchor(atSample: engine.now + Int64(0.1 * engine.sampleRate))
        isPlaying = true
        scheduleTick()
        let scheduler = Timer(timeInterval: 0.025, repeats: true) { [weak self] _ in
            Task { @MainActor in self?.scheduleTick() }
        }
        RunLoop.main.add(scheduler, forMode: .common)
        schedulerTimer = scheduler

        let visual = Timer(timeInterval: 1.0 / 120.0, repeats: true) { [weak self] _ in
            Task { @MainActor in self?.drainVisualQueue() }
        }
        RunLoop.main.add(visual, forMode: .common)
        visualTimer = visual
    }

    public func stop() {
        schedulerTimer?.invalidate()
        schedulerTimer = nil
        visualTimer?.invalidate()
        visualTimer = nil
        isPlaying = false
        visualQueue.removeAll()
    }

    /// Update the pattern/tempo. If the bar shape actually changed (bpm or
    /// either line's step count), the new values are held as pending and
    /// swapped in exactly on the next bar boundary — mirrors
    /// `useSequencer.ts`'s `watch([bpm, top.count, bottom.count])`. Until
    /// then, scheduling continues under the *old* shape so the current bar
    /// isn't truncated and no events collide with what's already been
    /// committed to the audio engine. Sound/mute/dot changes (no shape
    /// change) take effect immediately, on the next natural step.
    public func update(top: Line, bottom: Line, bpm: Double) {
        if top.count != self.top.count || bottom.count != self.bottom.count || bpm != self.bpm {
            pendingTop = top
            pendingBottom = bottom
            pendingBpm = bpm
            reanchorRequested = true
        } else {
            self.top = top
            self.bottom = bottom
            self.bpm = bpm
        }
    }

    private func anchor(atSample sample: Int64) {
        anchorSample = sample
        bottomIndex = 0
        topIndex = 0
        lastBottomSample = sample
        lastBottomIndex = 0
    }

    private func bottomSample(_ index: Int) -> Int64 {
        LiveScheduleMath.sampleTime(anchorSample: anchorSample, stepIndex: index, stepDurationSeconds: beatDuration, sampleRate: engine.sampleRate)
    }

    private func topSample(_ index: Int) -> Int64 {
        LiveScheduleMath.sampleTime(anchorSample: anchorSample, stepIndex: index, stepDurationSeconds: topStepDuration, sampleRate: engine.sampleRate)
    }

    private func scheduleTick() {
        guard isPlaying else { return }
        let horizon = engine.now + Int64(0.12 * engine.sampleRate)
        if reanchorRequested {
            performReanchor(notBefore: horizon)
        }
        scheduleUpTo(horizon)
    }

    /// Finishes the current (old-shape) bar up to its next boundary at or
    /// after `notBefore`, then swaps in the pending top/bottom/bpm and
    /// re-anchors exactly there. Booking the old shape right up to the
    /// boundary — rather than jumping straight to the new anchor — means
    /// nothing already committed to the engine (everything before
    /// `notBefore`) is ever contradicted or duplicated, and the visual
    /// queue never gets an out-of-order entry needing a separate flush.
    private func performReanchor(notBefore: Int64) {
        let barDurationSamples = Int64((barDuration * engine.sampleRate).rounded())
        let boundary = LiveScheduleMath.nextBarBoundary(anchorSample: anchorSample, barDurationSamples: barDurationSamples, notBefore: notBefore)
        scheduleUpTo(boundary)

        if let newTop = pendingTop, let newBottom = pendingBottom, let newBpm = pendingBpm {
            top = newTop
            bottom = newBottom
            bpm = newBpm
        }
        pendingTop = nil
        pendingBottom = nil
        pendingBpm = nil
        reanchorRequested = false
        anchor(atSample: boundary)
    }

    private func scheduleUpTo(_ horizon: Int64) {
        while bottomSample(bottomIndex) < horizon || topSample(topIndex) < horizon {
            let bSample = bottomSample(bottomIndex)
            let tSample = topSample(topIndex)
            if bSample <= tSample {
                scheduleBottomStep(at: bSample)
                bottomIndex += 1
            } else {
                scheduleTopStep(at: tSample)
                topIndex += 1
            }
        }
    }

    private func scheduleBottomStep(at sample: Int64) {
        let idx = bottomIndex % bottom.count
        if idx == 0 {
            engine.triggerDing(atSample: sample)
            visualQueue.append(.bell(sample: sample))
        }
        let audible = bottom.dots[idx] && !bottom.muted
        if audible { engine.trigger(bottom.sound, atSample: sample) }
        visualQueue.append(.step(line: .bottom, index: idx, sound: bottom.sound, sample: sample, audible: audible))
    }

    private func scheduleTopStep(at sample: Int64) {
        let idx = topIndex % top.count
        let audible = top.dots[idx] && !top.muted
        if audible { engine.trigger(top.sound, atSample: sample) }
        visualQueue.append(.step(line: .top, index: idx, sound: top.sound, sample: sample, audible: audible))
    }

    /// Drains events whose sample time has already been *heard* (compensating
    /// for output latency), matching `useSequencer.ts`'s `frame()`.
    private func drainVisualQueue() {
        let latencySamples = Int64(engine.outputLatencySeconds * engine.sampleRate)
        let now = engine.now + latencySamples
        while let first = visualQueue.first, first.sample <= now {
            visualQueue.removeFirst()
            switch first {
            case .bell:
                onBell?()
            case .step(let line, let index, let sound, let sample, let audible):
                if line == .bottom {
                    lastBottomSample = sample
                    lastBottomIndex = index
                }
                onIndexUpdate?(line, index)
                if audible { onStrike?(line, index, sound) }
            }
        }
        guard isPlaying else { return }
        let elapsedSamples = Double(now - lastBottomSample)
        let beatSamples = beatDuration * engine.sampleRate
        let frac = max(0, min(elapsedSamples / beatSamples, 1))
        let progress = (Double(lastBottomIndex) + frac).truncatingRemainder(dividingBy: Double(bottom.count)) / Double(bottom.count)
        onProgressUpdate?(progress)
    }
}

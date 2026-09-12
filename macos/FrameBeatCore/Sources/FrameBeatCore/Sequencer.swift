import Foundation

public enum LineId: Sendable { case top, bottom }

public struct ScheduledEvent: Sendable {
    public enum Kind: Sendable {
        case bell
        case step(line: LineId, index: Int, sound: Sound, audible: Bool)
    }
    public let time: Double // seconds, relative to bar-0/step-0
    public let kind: Kind
}

/// Offline equivalent of the scheduling math in
/// `src/composables/useSequencer.ts`'s `scheduler()`/`anchor()`: two
/// independent step streams (top, bottom) interleaved by next-due time, with
/// the bottom line's `count` + `bpm` defining the bar and the top line
/// dividing that same bar into its own step count. The live version books
/// events into a rolling 120ms look-ahead window on a 25ms timer; this
/// generates the full event list for a fixed number of bars up front, which
/// is what an offline (non-realtime) render needs and is easier to assert
/// timing invariants against in a test.
public enum Sequencer {
    public static func generateEvents(top: Line, bottom: Line, bpm: Double, bars: Int) -> [ScheduledEvent] {
        precondition(bars > 0 && top.count > 0 && bottom.count > 0)
        let beatDur = 60.0 / bpm
        let barDur = beatDur * Double(bottom.count)
        let topStepDur = barDur / Double(top.count)
        let endTime = Double(bars) * barDur

        // The live scheduler in useSequencer.ts merges the two streams by
        // repeatedly comparing `next` accumulators (`bStream.next += bd`)
        // against a rolling look-ahead horizon — fine for a realtime loop,
        // but accumulated floating-point error in `+=` can push the final
        // event's time a hair past (or short of) an exact bar boundary,
        // which either drops or duplicates the last bar when the horizon is
        // a fixed end time instead of "now". For this offline generator,
        // compute each step's time directly as `index * stepDuration`
        // (multiplication, not accumulation) so every event time is exact
        // and the loop bound is unambiguous — order doesn't matter here
        // since events carry absolute times and the renderer schedules by
        // sample index, not by list position.
        let totalBottomSteps = bars * bottom.count
        let totalTopSteps = Int((endTime / topStepDur).rounded()) // == bars * top.count

        var events: [ScheduledEvent] = []
        events.reserveCapacity(totalBottomSteps + totalTopSteps + bars)

        for k in 0..<totalBottomSteps {
            let time = Double(k) * beatDur
            let idx = k % bottom.count
            if idx == 0 {
                // "The one": bottom step 0 is when both lines restart
                // together, so the bar-marker chime rings there regardless
                // of either line's mute state — matches scheduleStep() in
                // useSequencer.ts.
                events.append(ScheduledEvent(time: time, kind: .bell))
            }
            events.append(ScheduledEvent(
                time: time,
                kind: .step(line: .bottom, index: idx, sound: bottom.sound, audible: bottom.dots[idx] && !bottom.muted)
            ))
        }
        for k in 0..<totalTopSteps {
            let time = Double(k) * topStepDur
            let idx = k % top.count
            events.append(ScheduledEvent(
                time: time,
                kind: .step(line: .top, index: idx, sound: top.sound, audible: top.dots[idx] && !top.muted)
            ))
        }
        return events.sorted { $0.time < $1.time }
    }
}

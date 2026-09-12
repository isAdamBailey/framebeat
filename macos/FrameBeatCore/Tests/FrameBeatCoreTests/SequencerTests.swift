import XCTest
@testable import FrameBeatCore

final class SequencerTests: XCTestCase {
    func testDefaultPatternBarBoundariesCoincide() {
        let top = Line(count: 3, sound: .edge)
        let bottom = Line(count: 4, sound: .bass)
        let events = Sequencer.generateEvents(top: top, bottom: bottom, bpm: 90, bars: 8)

        let beatDur = 60.0 / 90.0
        let barDur = beatDur * 4

        let bellTimes = events.compactMap { e -> Double? in
            if case .bell = e.kind { return e.time }
            return nil
        }
        XCTAssertEqual(bellTimes.count, 8)
        for (k, t) in bellTimes.enumerated() {
            XCTAssertEqual(t, Double(k) * barDur, accuracy: 1e-9)
        }

        let bottomZero = Set(events.compactMap { e -> Double? in
            if case .step(.bottom, 0, _, _) = e.kind { return e.time }
            return nil
        })
        let topZero = Set(events.compactMap { e -> Double? in
            if case .step(.top, 0, _, _) = e.kind { return e.time }
            return nil
        })
        XCTAssertEqual(bottomZero, topZero, "bottom step 0 and top step 0 must land on exactly the same times")
    }

    func testUnevenPolyrhythmStillLocksOnBarBoundary() {
        // A less tidy ratio (5 against 7) is a better stress test of the
        // interleave logic than the app's 3-against-4 default.
        let top = Line(count: 7, sound: .click)
        let bottom = Line(count: 5, sound: .bass)
        let events = Sequencer.generateEvents(top: top, bottom: bottom, bpm: 120, bars: 6)

        let bottomZero = Set(events.compactMap { e -> Double? in
            if case .step(.bottom, 0, _, _) = e.kind { return e.time }
            return nil
        })
        let topZero = Set(events.compactMap { e -> Double? in
            if case .step(.top, 0, _, _) = e.kind { return e.time }
            return nil
        })
        XCTAssertEqual(bottomZero.count, 6)
        XCTAssertEqual(bottomZero, topZero)
    }

    func testMutedLineStillCarriesBarChimeButNoAudibleStep() {
        var bottom = Line(count: 4, sound: .bass)
        bottom.muted = true
        let top = Line(count: 3, sound: .edge)
        let events = Sequencer.generateEvents(top: top, bottom: bottom, bpm: 100, bars: 2)

        let bellCount = events.filter { if case .bell = $0.kind { return true } else { return false } }.count
        XCTAssertEqual(bellCount, 2, "bar chime must still ring even when the bottom line is muted")

        let bottomAudible = events.contains { e in
            if case .step(.bottom, _, _, let audible) = e.kind { return audible }
            return false
        }
        XCTAssertFalse(bottomAudible, "a muted line's steps must never be marked audible")
    }
}

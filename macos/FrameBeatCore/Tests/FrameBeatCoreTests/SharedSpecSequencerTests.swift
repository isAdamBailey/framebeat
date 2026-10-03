import XCTest
@testable import FrameBeatCore

/// `spec/sequencer.json` against `Sequencer.swift` and `LiveScheduleMath.swift`.
final class SharedSpecSequencerTests: XCTestCase {
    private struct Spec: Decodable {
        struct Event: Decodable {
            let time: Double
            let kind: String
            let line: String?
            let index: Int?
            let sound: String?
            let audible: Bool?
        }
        struct Sequence: Decodable {
            let name: String
            let top: SharedSpec.SpecLine
            let bottom: SharedSpec.SpecLine
            let bpm: Double
            let bars: Int
            let chimeOnOne: Bool
            let events: [Event]
        }
        struct Boundary: Decodable { let anchor: Int64; let bar: Int64; let notBefore: Int64; let expected: Int64 }
        let tolerance: Double
        let sequences: [Sequence]
        let nextBarBoundary: [Boundary]
    }

    private func spec() throws -> Spec { try SharedSpec.load("sequencer") }

    /// Bell, then bottom, then top — the order the spec gives events that
    /// land together.
    private func rank(_ event: ScheduledEvent) -> Int {
        switch event.kind {
        case .bell: return 0
        case .step(let line, _, _, _): return line == .bottom ? 1 : 2
        }
    }

    func testEventLists() throws {
        let spec = try spec()
        XCTAssertFalse(spec.sequences.isEmpty)
        for c in spec.sequences {
            let events = Sequencer.generateEvents(
                top: try c.top.line(), bottom: try c.bottom.line(), bpm: c.bpm, bars: c.bars, chimeOnOne: c.chimeOnOne
            ).sorted {
                abs($0.time - $1.time) <= spec.tolerance ? rank($0) < rank($1) : $0.time < $1.time
            }
            XCTAssertEqual(events.count, c.events.count, c.name)
            for (i, (actual, expected)) in zip(events, c.events).enumerated() {
                let label = "\(c.name), event \(i)"
                XCTAssertEqual(actual.time, expected.time, accuracy: spec.tolerance, label)
                switch actual.kind {
                case .bell:
                    XCTAssertEqual(expected.kind, "bell", label)
                case .step(let line, let index, let sound, let audible):
                    XCTAssertEqual(expected.kind, "step", label)
                    XCTAssertEqual(line == .top ? "top" : "bottom", expected.line, label)
                    XCTAssertEqual(index, expected.index, label)
                    XCTAssertEqual(sound.rawValue, expected.sound, label)
                    XCTAssertEqual(audible, expected.audible, label)
                }
            }
        }
    }

    func testNextBarBoundary() throws {
        let spec = try spec()
        XCTAssertFalse(spec.nextBarBoundary.isEmpty)
        for c in spec.nextBarBoundary {
            XCTAssertEqual(
                LiveScheduleMath.nextBarBoundary(anchorSample: c.anchor, barDurationSamples: c.bar, notBefore: c.notBefore),
                c.expected,
                "anchor \(c.anchor), bar \(c.bar), not before \(c.notBefore)"
            )
        }
    }
}

import XCTest
@testable import FrameBeatCore

/// `spec/controls.json`. The starting state, ranges, and key map live in the
/// app target (`AppState.swift`, the panel views, `DrumView.swift`), which
/// this package's tests cannot import, so those checks read the app's Swift
/// source. A failure here means the source no longer states the spec value
/// in the expected form, or states a different one.
final class SharedSpecControlsTests: XCTestCase {
    private struct Spec: Decodable {
        struct Defaults: Decodable {
            let bpm: Double
            let top: SharedSpec.SpecLine
            let bottom: SharedSpec.SpecLine
            let chimeOnOne: Bool
        }
        struct Range: Decodable { let min: Int; let max: Int }
        struct Ranges: Decodable { let bpm: Range; let count: Range }
        struct Key: Decodable { let side: String; let sound: String }
        let defaults: Defaults
        let ranges: Ranges
        let drumKeys: [String: Key]
    }

    private func spec() throws -> Spec { try SharedSpec.load("controls") }

    private func appSource(_ path: String) throws -> String {
        try String(contentsOf: SharedSpec.repoRoot.appendingPathComponent("macos/FrameBeat/\(path)"), encoding: .utf8)
    }

    private func assertContains(_ source: String, _ text: String, _ file: String, line: UInt = #line) {
        XCTAssertTrue(source.contains(text), "\(file) should contain `\(text)`", line: line)
    }

    func testDefaultLinesFillEveryStepAndAreNotMuted() throws {
        let spec = try spec()
        for expected in [spec.defaults.top, spec.defaults.bottom] {
            let line = Line(count: expected.count, sound: try SharedSpec.sound(expected.sound))
            XCTAssertEqual(line, try expected.line())
        }
    }

    func testAppStateDefaults() throws {
        let d = try spec().defaults
        let source = try appSource("Model/AppState.swift")
        assertContains(source, "var bpm: Double = \(Int(d.bpm))", "AppState.swift")
        assertContains(source, "var top = Line(count: \(d.top.count), sound: .\(d.top.sound))", "AppState.swift")
        assertContains(source, "var bottom = Line(count: \(d.bottom.count), sound: .\(d.bottom.sound))", "AppState.swift")
        assertContains(source, "var chimeOnOne = \(d.chimeOnOne)", "AppState.swift")
    }

    func testRanges() throws {
        let r = try spec().ranges
        assertContains(try appSource("Views/Panel/TransportControls.swift"), "in: \(r.bpm.min)...\(r.bpm.max)", "TransportControls.swift")
        assertContains(try appSource("Views/Panel/LineHeaderView.swift"), "in: \(r.count.min)...\(r.count.max)", "LineHeaderView.swift")
    }

    func testDrumKeys() throws {
        let keys = try spec().drumKeys
        XCTAssertEqual(keys.count, 8)
        let source = try appSource("Views/Drum/DrumView.swift")
        for (key, mapped) in keys {
            switch key {
            case "arrowleft", "arrowright":
                let arrow = key == "arrowleft" ? "leftArrow" : "rightArrow"
                assertContains(source, "case .\(arrow):\n                keyStrike(side: .\(mapped.side), sound: .\(mapped.sound))", "DrumView.swift")
            default:
                assertContains(source, "\"\(key)\": (.\(mapped.side), .\(mapped.sound))", "DrumView.swift")
            }
        }
    }
}

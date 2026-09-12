import XCTest
@testable import FrameBeatCore

final class LiveScheduleMathTests: XCTestCase {
    func testNextBarBoundaryReturnsAnchorWhenAlreadyAtOrAfterNotBefore() {
        XCTAssertEqual(LiveScheduleMath.nextBarBoundary(anchorSample: 1000, barDurationSamples: 500, notBefore: 1000), 1000)
        XCTAssertEqual(LiveScheduleMath.nextBarBoundary(anchorSample: 1000, barDurationSamples: 500, notBefore: 500), 1000)
    }

    func testNextBarBoundaryRoundsUpToTheNextWholeBar() {
        // notBefore lands mid-bar (1200 is 200 samples into the second bar
        // of 500-sample bars starting at 1000) — must not truncate it.
        XCTAssertEqual(LiveScheduleMath.nextBarBoundary(anchorSample: 1000, barDurationSamples: 500, notBefore: 1200), 1500)
    }

    func testNextBarBoundaryHandlesExactMultiples() {
        XCTAssertEqual(LiveScheduleMath.nextBarBoundary(anchorSample: 0, barDurationSamples: 500, notBefore: 1500), 1500)
    }

    func testNextBarBoundaryAtRealisticSampleRateAndTempo() {
        // 90 BPM, 4-step bottom line: barDur = (60/90)*4 = 2.6667s.
        let sampleRate = 48000.0
        let barDurationSamples = Int64((2.0 / 3.0 * 4 * sampleRate).rounded())
        let anchor: Int64 = 4800 // 0.1s in
        let notBefore = anchor + Int64(0.12 * sampleRate) // one scheduler horizon ahead

        let boundary = LiveScheduleMath.nextBarBoundary(anchorSample: anchor, barDurationSamples: barDurationSamples, notBefore: notBefore)

        XCTAssertGreaterThanOrEqual(boundary, notBefore)
        XCTAssertEqual((boundary - anchor) % barDurationSamples, 0, "boundary must land on a whole bar from the anchor")
    }
}

import XCTest
@testable import FrameBeatCore

final class EnvelopeTests: XCTestCase {
    func testExponentialRampMatchesWebAudioFormula() {
        // value(t) = v0 * (v1/v0) ^ ((t - t0) / (t1 - t0))
        let env = Envelope([(0, 0.0001), (0.008, 0.9)])
        let t = 0.004
        let expected = 0.0001 * pow(0.9 / 0.0001, (t - 0) / (0.008 - 0))
        XCTAssertEqual(env.value(at: t), expected, accuracy: 1e-12)
    }

    func testHoldsFirstValueBeforeStart() {
        let env = Envelope([(0.01, 5), (0.02, 10)])
        XCTAssertEqual(env.value(at: 0), 5)
    }

    func testHoldsLastValueAfterEnd() {
        let env = Envelope([(0, 1), (0.1, 2)])
        XCTAssertEqual(env.value(at: 1), 2)
    }
}

final class OfflineRendererTests: XCTestCase {
    func testTriggeringAVoiceProducesNonZeroBoundedAudio() {
        let renderer = OfflineRenderer(sampleRate: 48000, durationSeconds: 1)
        renderer.trigger(.bass, atSample: 1000)
        let mono = renderer.finalizeMono()

        let peak = mono.map { abs($0) }.max() ?? 0
        XCTAssertGreaterThan(peak, 0, "bass voice should produce audible output")
        XCTAssertLessThanOrEqual(peak, 1.0, "soft clip must keep the mix within [-1, 1]")

        // Nothing should sound before the trigger sample.
        XCTAssertEqual(mono[0..<1000].map(abs).max(), 0)
    }

    func testDingProducesLongerTailThanADrumHit() {
        let renderer = OfflineRenderer(sampleRate: 48000, durationSeconds: 2)
        renderer.triggerDing(atSample: 0)
        let mono = renderer.finalizeMono()
        // The ding's longest partial decays over 1.1s — expect non-negligible
        // energy well past where a ~0.6s drum voice would already be silent.
        let sampleAt900ms = Int(0.9 * 48000)
        XCTAssertGreaterThan(abs(mono[sampleAt900ms]) + abs(mono[sampleAt900ms + 1]) + abs(mono[sampleAt900ms + 2]), 0)
    }

    func testNoNaNOrInfiniteSamples() {
        let renderer = OfflineRenderer(sampleRate: 48000, durationSeconds: 1)
        for sound in Sound.allCases {
            renderer.trigger(sound, atSample: 0)
        }
        renderer.triggerDing(atSample: 0)
        for s in renderer.finalizeMono() {
            XCTAssertFalse(s.isNaN)
            XCTAssertFalse(s.isInfinite)
        }
    }
}

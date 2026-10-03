import XCTest
@testable import FrameBeatCore

/// `spec/voices.json` against `VoiceSpec.swift` and `Envelope.swift`.
final class SharedSpecVoiceTests: XCTestCase {
    private struct Spec: Decodable {
        struct Oscillator: Decodable { let waveform: String; let freq: [[Double]]; let gain: [[Double]] }
        struct Noise: Decodable { let filter: String; let frequency: Double; let q: Double; let gain: [[Double]] }
        struct Voice: Decodable { let oscillators: [Oscillator]; let noises: [Noise] }
        struct Sample: Decodable { let t: Double; let value: Double }
        struct EnvelopeCase: Decodable { let voice: String; let part: String; let samples: [Sample] }
        let tolerance: Double
        let voices: [String: Voice]
        let envelopes: [EnvelopeCase]
    }

    private func spec() throws -> Spec { try SharedSpec.load("voices") }

    private func components(_ voice: String) throws -> (oscillators: [OscillatorSpec], noises: [FilteredNoiseSpec]) {
        voice == "ding" ? (VoiceSpec.dingComponents(), []) : VoiceSpec.components(for: try SharedSpec.sound(voice))
    }

    private func breakpoints(_ envelope: Envelope) -> [[Double]] {
        envelope.points.map { [$0.time, $0.value] }
    }

    private func waveform(_ w: Waveform) -> String {
        switch w {
        case .sine: return "sine"
        case .triangle: return "triangle"
        }
    }

    private func filter(_ kind: BiquadFilter.Kind) -> String {
        switch kind {
        case .lowpass: return "lowpass"
        case .highpass: return "highpass"
        case .bandpass: return "bandpass"
        }
    }

    func testVoiceParameters() throws {
        let spec = try spec()
        XCTAssertEqual(Set(spec.voices.keys), ["bass", "edge", "click", "ding"])
        for (name, expected) in spec.voices {
            let actual = try components(name)
            XCTAssertEqual(actual.oscillators.count, expected.oscillators.count, name)
            for (a, e) in zip(actual.oscillators, expected.oscillators) {
                XCTAssertEqual(waveform(a.waveform), e.waveform, name)
                XCTAssertEqual(breakpoints(a.freq), e.freq, "\(name) freq")
                XCTAssertEqual(breakpoints(a.gain), e.gain, "\(name) gain")
            }
            XCTAssertEqual(actual.noises.count, expected.noises.count, name)
            for (a, e) in zip(actual.noises, expected.noises) {
                XCTAssertEqual(filter(a.kind), e.filter, name)
                XCTAssertEqual(a.frequency, e.frequency, name)
                XCTAssertEqual(a.q, e.q, name)
                XCTAssertEqual(breakpoints(a.gain), e.gain, "\(name) noise gain")
            }
        }
    }

    func testEnvelopeSamples() throws {
        let spec = try spec()
        XCTAssertFalse(spec.envelopes.isEmpty)
        for c in spec.envelopes {
            let parts = c.part.split(separator: ".").map(String.init)
            XCTAssertEqual(parts.count, 3, c.part)
            let index = try XCTUnwrap(Int(parts[1]), c.part)
            let voice = try components(c.voice)
            let envelope: Envelope
            if parts[0] == "noises" {
                envelope = voice.noises[index].gain
            } else {
                let osc = voice.oscillators[index]
                envelope = parts[2] == "freq" ? osc.freq : osc.gain
            }
            for s in c.samples {
                XCTAssertEqual(envelope.value(at: s.t), s.value, accuracy: spec.tolerance, "\(c.voice) \(c.part) at \(s.t)")
            }
        }
    }
}

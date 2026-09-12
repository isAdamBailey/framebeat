import Foundation

/// Minimal 16-bit PCM mono WAV writer — no AVFoundation dependency, so the
/// render CLI works from plain `swift run` without an app bundle or audio
/// device entitlement.
public enum WavWriter {
    public static func write(samples: [Float], sampleRate: Double, to url: URL) throws {
        var data = Data()
        let channels: UInt16 = 1
        let bitsPerSample: UInt16 = 16
        let byteRate = UInt32(sampleRate) * UInt32(channels) * UInt32(bitsPerSample / 8)
        let blockAlign = channels * (bitsPerSample / 8)
        let dataSize = UInt32(samples.count * 2)

        func append(_ s: String) { data.append(s.data(using: .ascii)!) }
        func append(_ v: UInt32) { withUnsafeBytes(of: v.littleEndian) { data.append(contentsOf: $0) } }
        func append(_ v: UInt16) { withUnsafeBytes(of: v.littleEndian) { data.append(contentsOf: $0) } }

        append("RIFF")
        append(UInt32(36 + dataSize))
        append("WAVE")
        append("fmt ")
        append(UInt32(16))
        append(UInt16(1)) // PCM
        append(channels)
        append(UInt32(sampleRate))
        append(byteRate)
        append(blockAlign)
        append(bitsPerSample)
        append("data")
        append(dataSize)

        for s in samples {
            let clamped = max(-1.0, min(1.0, s))
            let intSample = Int16(clamped * Float(Int16.max))
            append(UInt16(bitPattern: intSample))
        }

        try data.write(to: url)
    }
}

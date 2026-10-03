import Foundation
import XCTest
@testable import FrameBeatCore

/// Loads the shared test cases in `spec/` at the repo root. The web and
/// Android test suites run the same files, so all three copies of every
/// constant are checked against one contract. The path is built from
/// `#filePath`, so `Package.swift` does not need a resource entry.
enum SharedSpec {
    static let repoRoot = URL(fileURLWithPath: #filePath)
        .deletingLastPathComponent() // FrameBeatCoreTests
        .deletingLastPathComponent() // Tests
        .deletingLastPathComponent() // FrameBeatCore
        .deletingLastPathComponent() // macos
        .deletingLastPathComponent()

    static func load<T: Decodable>(_ name: String, as type: T.Type = T.self) throws -> T {
        let data = try Data(contentsOf: repoRoot.appendingPathComponent("spec/\(name).json"))
        return try JSONDecoder().decode(T.self, from: data)
    }

    static func sound(_ name: String) throws -> Sound {
        try XCTUnwrap(Sound(rawValue: name), "unknown sound \(name)")
    }

    static func side(_ name: String) throws -> DrumGeometry.Side {
        switch name {
        case "left": return .left
        case "right": return .right
        default: throw SpecError(message: "unknown side \(name)")
        }
    }

    struct SpecError: Error, CustomStringConvertible {
        let message: String
        var description: String { message }
    }

    struct SpecLine: Decodable {
        let count: Int
        let sound: String
        let dots: [Bool]
        let muted: Bool

        func line() throws -> Line {
            Line(count: count, sound: try SharedSpec.sound(sound), dots: dots, muted: muted)
        }
    }
}

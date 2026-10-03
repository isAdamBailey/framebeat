import XCTest
@testable import FrameBeatCore

/// `spec/geometry.json` against `Geometry.swift`.
final class SharedSpecGeometryTests: XCTestCase {
    private struct Spec: Decodable {
        struct Hit: Decodable { let sound: String; let side: String; let dx: Double; let dy: Double }
        struct Classify: Decodable { let note: String; let x: Double; let y: Double; let expected: Hit? }
        struct ZonePoint: Decodable { let sound: String; let side: String; let x: Double; let y: Double }
        struct Swing: Decodable {
            let side: String
            let gx: Double
            let gy: Double
            let rotation: Double
            let x: Double
            let y: Double
        }
        let tolerance: Double
        let swingTolerance: Double
        let classify: [Classify]
        let zonePoint: [ZonePoint]
        let swing: [Swing]
    }

    private func spec() throws -> Spec { try SharedSpec.load("geometry") }

    func testClassify() throws {
        let spec = try spec()
        XCTAssertFalse(spec.classify.isEmpty)
        for c in spec.classify {
            let hit = DrumGeometry.classify(x: c.x, y: c.y)
            guard let expected = c.expected else {
                XCTAssertNil(hit, c.note)
                continue
            }
            let actual = try XCTUnwrap(hit, c.note)
            XCTAssertEqual(actual.sound, try SharedSpec.sound(expected.sound), c.note)
            XCTAssertEqual(actual.side, try SharedSpec.side(expected.side), c.note)
            XCTAssertEqual(actual.dx, expected.dx, accuracy: spec.tolerance, c.note)
            XCTAssertEqual(actual.dy, expected.dy, accuracy: spec.tolerance, c.note)
        }
    }

    func testZonePoint() throws {
        let spec = try spec()
        XCTAssertEqual(spec.zonePoint.count, Sound.allCases.count * 2)
        for c in spec.zonePoint {
            let point = DrumGeometry.zonePoint(sound: try SharedSpec.sound(c.sound), side: try SharedSpec.side(c.side))
            XCTAssertEqual(point.x, c.x, accuracy: spec.tolerance, "\(c.sound) \(c.side)")
            XCTAssertEqual(point.y, c.y, accuracy: spec.tolerance, "\(c.sound) \(c.side)")
        }
    }

    func testSwing() throws {
        let spec = try spec()
        XCTAssertFalse(spec.swing.isEmpty)
        for c in spec.swing {
            let label = "\(c.side) to (\(c.gx), \(c.gy))"
            let swing = DrumGeometry.swing(side: try SharedSpec.side(c.side), gx: c.gx, gy: c.gy)
            XCTAssertEqual(swing.rotationDegrees, c.rotation, accuracy: spec.tolerance, label)
            XCTAssertEqual(swing.dx, c.x, accuracy: spec.swingTolerance, label)
            XCTAssertEqual(swing.dy, c.y, accuracy: spec.swingTolerance, label)
        }
    }
}

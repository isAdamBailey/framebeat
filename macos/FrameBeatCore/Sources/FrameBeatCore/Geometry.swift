import Foundation

/// Drum-ellipse and mallet-aim geometry — a straight port of
/// `src/lib/geometry.ts`. Pure math, no UI framework dependency (same
/// separation the web app keeps between `geometry.ts` and `DrumCanvas.vue`).
public enum DrumGeometry {
    public enum Side: Sendable { case left, right }

    /// Drum ellipse as fractions of the illustration's bounding box.
    public static let drumCenterX = 0.5
    public static let drumCenterY = 0.4
    public static let drumHalfW = 0.5
    public static let drumHalfH = 0.34

    /// Canvas-unit geometry (canvas = 100 x 93.75): where each mallet pivots
    /// and how far its felt tip sits from that pivot.
    public static let canvasW = 100.0
    public static let canvasH = 93.75

    private static let leftPivot = (x: 6.56, y: 89.7)
    private static let rightPivot = (x: 93.44, y: 89.7)
    private static let leftBaseRot = 24.0
    private static let rightBaseRot = -24.0

    public static let reach = 0.7 * 0.58 * canvasH // felt-tip distance from the pivot

    // Each mallet rests on the frame's side — that resting spot is the Click
    // zone. Edge sits two-thirds of the way in from there, Bass at centre.
    private static let leftClickPoint = (x: -0.56, y: 0.55)
    private static let rightClickPoint = (x: 0.56, y: 0.55)

    public static func zonePoint(sound: Sound, side: Side) -> (x: Double, y: Double) {
        let click = side == .left ? leftClickPoint : rightClickPoint
        switch sound {
        case .click: return click
        case .edge: return (click.x * 2 / 3, click.y * 2 / 3)
        case .bass: return (0, 0)
        }
    }

    /// Rotation + shift (as a fraction, 0...1, of the mallet's own box) that
    /// lands the felt tip on the drum-ellipse point (gx, gy).
    public static func swing(side: Side, gx: Double, gy: Double) -> (rotationDegrees: Double, dx: Double, dy: Double) {
        let pivot = side == .left ? leftPivot : rightPivot
        let baseRot = side == .left ? leftBaseRot : rightBaseRot
        let rx = (drumCenterX + drumHalfW * gx) * canvasW - pivot.x
        let ry = (drumCenterY + drumHalfH * gy) * canvasH - pivot.y
        let d = max(Foundation.hypot(rx, ry), 1e-9)
        let angle = atan2(rx, -ry) * 180 / .pi // 0 = straight up
        let boxW = 0.38 * canvasW
        let boxH = 0.58 * canvasH
        return (
            rotationDegrees: angle - baseRot,
            dx: (rx - reach * rx / d) / boxW,
            dy: (ry - reach * ry / d) / boxH
        )
    }

    /// Classifies a tap at fractional position (x, y) within the
    /// illustration's bounding box into a drum zone, matching
    /// `DrumCanvas.vue`'s `strike()`. Returns nil for a tap in the empty
    /// space around the drum. `dx`/`dy` come back clamped to the rim for a
    /// zonePoint-style call and are in the same [-1, 1]-ish drum-ellipse
    /// units as `zonePoint`/`swing` use.
    public static func classify(x: Double, y: Double) -> (sound: Sound, side: Side, dx: Double, dy: Double)? {
        let dx = (x - drumCenterX) / drumHalfW
        let dy = (y - drumCenterY) / drumHalfH
        let dist = Foundation.hypot(dx, dy)
        guard dist <= 1.35 else { return nil }
        let sound: Sound = dist < 0.26 ? .bass : (dist < 0.65 ? .edge : .click)
        let clamp = dist > 1.15 ? 1.15 / dist : 1
        return (sound, dx < 0 ? .left : .right, dx * clamp, dy * clamp)
    }
}

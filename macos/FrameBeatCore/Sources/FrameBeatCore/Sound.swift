// Swift port of src/types/drum.ts — shared domain types.

public enum Sound: String, Sendable, CaseIterable {
    case bass
    case edge
    case click
}

public struct Line: Sendable {
    public var count: Int
    public var sound: Sound
    public var dots: [Bool]
    public var muted: Bool

    public init(count: Int, sound: Sound, dots: [Bool]? = nil, muted: Bool = false) {
        self.count = count
        self.sound = sound
        self.dots = dots ?? Array(repeating: true, count: 16)
        self.muted = muted
    }
}

// Swift port of src/types/drum.ts — shared domain types.

public enum Sound: String, Sendable, CaseIterable, Equatable {
    case bass
    case edge
    case click
}

public struct Line: Sendable, Equatable {
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

/// A strike arriving from the sequencer for one line — mirrors `Strike` in
/// src/types/drum.ts. `id` bumps on every new strike so observers can detect
/// a repeated identical sound as a distinct event.
public struct Strike: Sendable, Equatable {
    public var sound: Sound
    public var line: LineId
    public var id: Int

    public init(sound: Sound, line: LineId, id: Int) {
        self.sound = sound
        self.line = line
        self.id = id
    }
}

/// Mirrors `Strikes` in src/types/drum.ts: the most recent strike per line.
public struct Strikes: Sendable, Equatable {
    public var top: Strike?
    public var bottom: Strike?

    public init(top: Strike? = nil, bottom: Strike? = nil) {
        self.top = top
        self.bottom = bottom
    }
}

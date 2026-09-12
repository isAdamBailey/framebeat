import Observation
import FrameBeatCore

/// Phase 2 port of `src/App.vue`'s top-level refs. `Sound`/`Line`/`Strike`/
/// `Strikes` live in FrameBeatCore (shared with the engine/scheduler); this
/// just holds them the way `App.vue` does, for views to bind against.
///
/// `playing`/`currentTop`/`currentBottom`/`progress` are NOT here — on the
/// web app those are owned by `useSequencer`, not `App.vue`'s own refs, and
/// the Swift port keeps that split too (see `LiveSequencer`/`ContentView`).
/// `soundBlocked` has no native equivalent (no autoplay policy on macOS) and
/// is intentionally dropped, per the plan.
@Observable
final class AppState {
    var bpm: Double = 90
    var top = Line(count: 3, sound: .edge)
    var bottom = Line(count: 4, sound: .bass)
    var strikes = Strikes()
    var bellTrigger = 0

    func recordStrike(_ sound: Sound, on line: LineId) {
        let nextId = (line == .top ? strikes.top?.id : strikes.bottom?.id).map { $0 + 1 } ?? 1
        let strike = Strike(sound: sound, line: line, id: nextId)
        switch line {
        case .top: strikes.top = strike
        case .bottom: strikes.bottom = strike
        }
    }

    func ringBell() {
        bellTrigger += 1
    }
}

import SwiftUI
import FrameBeatCore

// SwiftUI port of src/components/drum/DrumCanvas.vue: layered gradients
// standing in for a wood-and-hide frame drum, two independently animated
// mallets, and color-coded ripple strikes. Approximates (rather than
// pixel-matches) the web version's exact box-model transforms — DESIGN.md's
// full "Midnight Workshop" treatment (grain texture, precise tension-ring
// placement, drop-shadow tuning) is still Phase 5 work; this proves the
// geometry math (DrumGeometry, ported 1:1 from geometry.ts) drives real
// mallet aim and zone classification correctly.

private struct RippleModel: Identifiable {
    let id: Int
    let x: Double // drum-ellipse units, same space as DrumGeometry
    let y: Double
    let color: Color
}

private struct SwingState {
    var rotation: Double = 0
    var dx: Double = 0
    var dy: Double = 0
}

/// Q/W/E and I/O/P mirror the qwerty row's own left-right symmetry
/// outward-in, matching `DrumCanvas.vue`'s `KEY_MAP`: the outer keys of each
/// cluster hit the rim (Click), the innermost hit the center (Bass).
private let keyMap: [Character: (side: DrumGeometry.Side, sound: Sound)] = [
    "q": (.left, .click), "w": (.left, .edge), "e": (.left, .bass),
    "i": (.right, .bass), "o": (.right, .edge), "p": (.right, .click),
]

struct DrumView: View {
    /// Called with the classified sound + which mallet to animate whenever
    /// the drum is tapped directly or played via keyboard (not via the
    /// sequencer).
    var onStrike: (Sound, DrumGeometry.Side) -> Void
    var playing: Bool
    let strikes: Strikes

    @State private var ripples: [RippleModel] = []
    @State private var leftSwing = SwingState()
    @State private var rightSwing = SwingState()
    @State private var idCounter = 0
    @State private var dragActive = false
    @FocusState private var isFocused: Bool

    var body: some View {
        GeometryReader { geo in
            let size = geo.size
            ZStack {
                groundShadow(size)
                shellLayers(size)
                woodenFrame(size)
                drumhead(size)
                tensionRings(size)
                sheen(size)

                ForEach(ripples) { ripple in
                    Circle()
                        .stroke(ripple.color, lineWidth: 2)
                        .frame(width: size.width * 0.46, height: size.height * 0.30)
                        .position(
                            x: size.width * (DrumGeometry.drumCenterX + ripple.x * DrumGeometry.drumHalfW),
                            y: size.height * (DrumGeometry.drumCenterY + ripple.y * DrumGeometry.drumHalfH)
                        )
                        .modifier(RippleAnimator {
                            ripples.removeAll { $0.id == ripple.id }
                        })
                }

                MalletView(side: .left, state: leftSwing, containerSize: size)
                MalletView(side: .right, state: rightSwing, containerSize: size)

                // A custom ring matching the drum's own circular silhouette,
                // in place of `.focusable()`'s default system ring — which
                // draws a plain rectangle around the whole view's bounding
                // box, unaware that only the ellipse inside it is "the
                // drum." `.focusEffectDisabled()` below suppresses that
                // default so only this one shows.
                if isFocused {
                    Ellipse()
                        .stroke(Theme.Color.bassSky, lineWidth: 3)
                        .frame(width: size.width + 10, height: size.height + 10)
                        .position(x: size.width / 2, y: size.height / 2)
                }
            }
            .contentShape(Rectangle())
            .gesture(
                // Fire on the initial touch/press (`.onChanged`, guarded to
                // once per drag), not `.onEnded` — matching Vue's
                // `@pointerdown`. Firing on release adds felt latency the
                // web version doesn't have.
                DragGesture(minimumDistance: 0)
                    .onChanged { value in
                        guard !dragActive else { return }
                        dragActive = true
                        let x = value.location.x / size.width
                        let y = value.location.y / size.height
                        guard let hit = DrumGeometry.classify(x: x, y: y) else { return }
                        onStrike(hit.sound, hit.side)
                        visualStrike(sound: hit.sound, dx: hit.dx, dy: hit.dy, side: hit.side)
                    }
                    .onEnded { _ in dragActive = false }
            )
        }
        .aspectRatio(32.0 / 30.0, contentMode: .fit)
        .focusable()
        .focusEffectDisabled()
        .focused($isFocused)
        // Scoped to this element (not a window-level handler) so the
        // shortcuts don't hijack arrow/letter keys used elsewhere, matching
        // DrumCanvas.vue's element-bound @keydown.
        .onKeyPress(phases: .down) { press in
            if let chars = press.characters.lowercased().first, let mapped = keyMap[chars] {
                keyStrike(side: mapped.side, sound: mapped.sound)
                return .handled
            }
            switch press.key {
            case .leftArrow:
                keyStrike(side: .left, sound: .edge)
                return .handled
            case .rightArrow:
                keyStrike(side: .right, sound: .edge)
                return .handled
            default:
                return .ignored
            }
        }
        .onChange(of: playing) { _, isPlaying in
            if isPlaying { isFocused = true }
        }
        .onChange(of: strikes.top) { _, newValue in
            if let s = newValue { applySequencerStrike(s, side: .left) }
        }
        .onChange(of: strikes.bottom) { _, newValue in
            if let s = newValue { applySequencerStrike(s, side: .right) }
        }
    }

    private func keyStrike(side: DrumGeometry.Side, sound: Sound) {
        onStrike(sound, side)
        let point = DrumGeometry.zonePoint(sound: sound, side: side)
        visualStrike(sound: sound, dx: point.x, dy: point.y, side: side)
    }

    private func applySequencerStrike(_ strike: Strike, side: DrumGeometry.Side) {
        let point = DrumGeometry.zonePoint(sound: strike.sound, side: side)
        let jitterX = Double.random(in: -0.06...0.06)
        let jitterY = Double.random(in: -0.06...0.06)
        visualStrike(sound: strike.sound, dx: point.x + jitterX, dy: point.y + jitterY, side: side)
    }

    private func visualStrike(sound: Sound, dx: Double, dy: Double, side: DrumGeometry.Side) {
        idCounter += 1
        ripples.append(RippleModel(id: idCounter, x: dx, y: dy, color: soundColor(sound)))
        let swing = DrumGeometry.swing(side: side, gx: dx, gy: dy)
        let newState = SwingState(rotation: swing.rotationDegrees, dx: swing.dx, dy: swing.dy)
        withAnimation(.interpolatingSpring(stiffness: 260, damping: 14)) {
            if side == .left { leftSwing = newState } else { rightSwing = newState }
        }
    }

    // MARK: - Layers (approximating DrumCanvas.vue's gradient divs)

    private func groundShadow(_ size: CGSize) -> some View {
        Ellipse()
            .fill(Color.black.opacity(0.5))
            .frame(width: size.width * 0.92, height: size.height * 0.16)
            .blur(radius: 8)
            .position(x: size.width * 0.5, y: size.height * 0.86)
    }

    private func shellLayers(_ size: CGSize) -> some View {
        ZStack {
            Ellipse()
                .fill(LinearGradient(colors: [.init(white: 0.28), .black], startPoint: .top, endPoint: .bottom))
                .frame(width: size.width, height: size.height * 0.68)
                .position(x: size.width * 0.5, y: size.height * 0.52)
            Ellipse()
                .fill(LinearGradient(colors: [.init(red: 0.45, green: 0.25, blue: 0.05), .init(white: 0.16)], startPoint: .top, endPoint: .bottom))
                .frame(width: size.width, height: size.height * 0.68)
                .position(x: size.width * 0.5, y: size.height * 0.46)
        }
    }

    private func woodenFrame(_ size: CGSize) -> some View {
        Ellipse()
            .fill(AngularGradient(
                colors: [
                    .init(red: 0.54, green: 0.32, blue: 0.14), .init(red: 0.60, green: 0.38, blue: 0.19),
                    .init(red: 0.44, green: 0.25, blue: 0.10), .init(red: 0.54, green: 0.32, blue: 0.14),
                ],
                center: .center
            ))
            .frame(width: size.width, height: size.height * 0.68)
            .position(x: size.width * 0.5, y: size.height * 0.40)
    }

    private func drumhead(_ size: CGSize) -> some View {
        Ellipse()
            .fill(RadialGradient(
                colors: [
                    .init(red: 0.95, green: 0.90, blue: 0.80), .init(red: 0.89, green: 0.80, blue: 0.64),
                    .init(red: 0.79, green: 0.66, blue: 0.47), .init(red: 0.67, green: 0.53, blue: 0.34),
                ],
                center: UnitPoint(x: 0.38, y: 0.30), startRadius: 1, endRadius: size.width * 0.32
            ))
            .frame(width: size.width * 0.82, height: size.height * 0.56)
            .position(x: size.width * 0.5, y: size.height * 0.40)
    }

    private func tensionRings(_ size: CGSize) -> some View {
        ZStack {
            Ellipse().stroke(Color.init(red: 0.45, green: 0.25, blue: 0.05).opacity(0.2), lineWidth: 1)
                .frame(width: size.width * 0.74, height: size.height * 0.50)
                .position(x: size.width * 0.5, y: size.height * 0.40)
            Ellipse().stroke(Color.init(red: 0.45, green: 0.25, blue: 0.05).opacity(0.2), lineWidth: 1)
                .frame(width: size.width * 0.38, height: size.height * 0.28)
                .position(x: size.width * 0.5, y: size.height * 0.40)
        }
    }

    private func sheen(_ size: CGSize) -> some View {
        Ellipse()
            .fill(Color.white.opacity(0.10))
            .frame(width: size.width * 0.34, height: size.height * 0.22)
            .blur(radius: 2)
            .position(x: size.width * 0.33, y: size.height * 0.27)
    }
}

func soundColor(_ sound: Sound) -> Color {
    Theme.Color.forSound(sound)
}

private struct RippleAnimator: ViewModifier {
    // Must match the CSS transition duration on DrumCanvas.vue's
    // .ripple-enter-active — kept as one constant here so the animation and
    // its cleanup timer below can't drift out of sync with each other.
    static let duration: Double = 0.7

    let onFinished: () -> Void
    @State private var grown = false

    func body(content: Content) -> some View {
        content
            .scaleEffect(grown ? 1.4 : 0.2)
            .opacity(grown ? 0 : 0.85)
            .onAppear {
                withAnimation(.easeOut(duration: Self.duration)) { grown = true }
                DispatchQueue.main.asyncAfter(deadline: .now() + Self.duration) { onFinished() }
            }
    }
}

/// Ported from `Mallet.vue`'s box model, since that's what
/// `DrumGeometry.swing`'s numbers are computed against:
///
/// - The mallet's container div is `bottom-[2%] left-[2%]` (or `right-[2%]`
///   for the right mallet), sized `h-[58%] w-[38%]` of the drum
///   illustration — i.e. anchored by its own edge, not centered on the
///   pivot point.
/// - Its `transform-origin` is `12% 96%` (left) / `88% 96%` (right) *within
///   that box* — an off-center point near the bottom-outer corner, not
///   plain bottom-center. Rotating around bottom-center instead (as an
///   earlier version of this file did) visibly misaims every strike.
/// - The animated transform is `translate(x%, y%) rotate(rot deg)`, and CSS
///   composes that as "rotate first (around transform-origin), then
///   translate" — matching SwiftUI's `.rotationEffect` before `.offset`
///   below, since each later modifier acts in the outer/parent space.
/// - The stick itself carries its own *static* tilt (`rotate-[24deg]` /
///   `rotate-[-24deg]`) independent of the animated transform —
///   `DrumGeometry.swing`'s rotation is relative to that rest tilt
///   (`angle - baseRot`), so it must be added back here since this view has
///   no separate static-tilt inner element.
/// - `Mallet.vue`'s inner stick div is itself offset toward the box's outer
///   edge (`left-[8%]`/`right-[8%]`), not centered — its own rotation
///   origin (default bottom-center) lands almost exactly on the box's
///   `transform-origin` (the pivot). This view has one element for the
///   whole stick assembly, so it needs an explicit static offset toward
///   `anchor` before rotating to reproduce that: without it, the stick
///   rotates on a lever arm anchored well outside its own body, producing
///   wildly oversized swings that cross to the other mallet's side.
private struct MalletView: View {
    let side: DrumGeometry.Side
    let state: SwingState
    let containerSize: CGSize

    var body: some View {
        let boxW = DrumGeometry.malletBoxWidthFraction * containerSize.width
        let boxH = DrumGeometry.malletBoxHeightFraction * containerSize.height
        let marginX = 0.02 * containerSize.width
        let marginBottom = 0.02 * containerSize.height
        let boxOriginX = side == .left ? marginX : containerSize.width - marginX - boxW
        let boxOriginY = containerSize.height - marginBottom - boxH
        let boxCenter = CGPoint(x: boxOriginX + boxW / 2, y: boxOriginY + boxH / 2)
        let anchor: UnitPoint = side == .left ? UnitPoint(x: 0.12, y: 0.96) : UnitPoint(x: 0.88, y: 0.96)
        let baseRotation = DrumGeometry.baseRotationDegrees(side: side)

        ZStack(alignment: .bottom) {
            Capsule()
                .fill(LinearGradient(colors: [.init(red: 0.36, green: 0.22, blue: 0.10), .black.opacity(0.7)], startPoint: .top, endPoint: .bottom))
                .frame(width: boxW * 0.18, height: boxH * 0.7)
            Circle()
                .fill(Color(white: 0.88))
                .frame(width: boxW * 0.46, height: boxW * 0.46)
                .offset(y: -boxH * 0.7)
        }
        .frame(width: boxW, height: boxH, alignment: .bottom)
        // Shift the stick assembly so its own base sits at `anchor` (the
        // pivot) instead of the box's horizontal center — matching
        // Mallet.vue's inner stick, which is offset toward the box's outer
        // edge (`left-[8%]`/`right-[8%]`) rather than centered. Without
        // this, the stick rotates on a lever arm far from its own body,
        // producing wildly oversized swings that cross to the other side.
        .offset(x: (anchor.x - 0.5) * boxW)
        .rotationEffect(.degrees(state.rotation + baseRotation), anchor: anchor)
        .offset(x: state.dx * boxW, y: state.dy * boxH)
        .position(boxCenter)
    }
}

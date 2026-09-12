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

struct DrumView: View {
    /// Called with the classified sound + which mallet to animate whenever
    /// the drum is tapped directly (not via the sequencer).
    var onStrike: (Sound, DrumGeometry.Side) -> Void

    @Binding var topStrike: StrikeEvent?
    @Binding var bottomStrike: StrikeEvent?

    @State private var ripples: [RippleModel] = []
    @State private var leftSwing = SwingState()
    @State private var rightSwing = SwingState()
    @State private var idCounter = 0

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
                            x: size.width * (0.5 + ripple.x * 0.25),
                            y: size.height * (0.40 + ripple.y * 0.17)
                        )
                        .modifier(RippleAnimator {
                            ripples.removeAll { $0.id == ripple.id }
                        })
                }

                MalletView(side: .left, state: leftSwing, containerSize: size)
                MalletView(side: .right, state: rightSwing, containerSize: size)
            }
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onEnded { value in
                        let x = value.location.x / size.width
                        let y = value.location.y / size.height
                        guard let hit = DrumGeometry.classify(x: x, y: y) else { return }
                        onStrike(hit.sound, hit.side)
                        visualStrike(sound: hit.sound, dx: hit.dx, dy: hit.dy, side: hit.side)
                    }
            )
        }
        .aspectRatio(32.0 / 30.0, contentMode: .fit)
        .onChange(of: topStrike?.id) { _, _ in
            if let s = topStrike { applySequencerStrike(s, side: .left) }
        }
        .onChange(of: bottomStrike?.id) { _, _ in
            if let s = bottomStrike { applySequencerStrike(s, side: .right) }
        }
    }

    private func applySequencerStrike(_ strike: StrikeEvent, side: DrumGeometry.Side) {
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

/// The three sound accents from DESIGN.md — used here directly; a
/// Theme.swift token layer is still Phase 5 work.
func soundColor(_ sound: Sound) -> Color {
    switch sound {
    case .bass: return Color(red: 0.22, green: 0.74, blue: 0.97)
    case .edge: return Color(red: 0.98, green: 0.75, blue: 0.14)
    case .click: return Color(red: 0.98, green: 0.45, blue: 0.09)
    }
}

/// A strike arriving from the sequencer (as opposed to a direct tap) —
/// mirrors `Strike`/`Strikes` in src/types/drum.ts. `id` bumps on every new
/// strike so `onChange(of:)` fires even for repeated identical sounds.
struct StrikeEvent: Equatable {
    let sound: Sound
    let id: Int
}

private struct RippleAnimator: ViewModifier {
    let onFinished: () -> Void
    @State private var grown = false

    func body(content: Content) -> some View {
        content
            .scaleEffect(grown ? 1.4 : 0.2)
            .opacity(grown ? 0 : 0.85)
            .onAppear {
                withAnimation(.easeOut(duration: 0.7)) { grown = true }
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.7) { onFinished() }
            }
    }
}

private struct MalletView: View {
    let side: DrumGeometry.Side
    let state: SwingState
    let containerSize: CGSize

    var body: some View {
        let pivotCanvas = side == .left ? (x: 6.56, y: 89.7) : (x: 93.44, y: 89.7)
        let pivot = CGPoint(
            x: pivotCanvas.x / DrumGeometry.canvasW * containerSize.width,
            y: pivotCanvas.y / DrumGeometry.canvasH * containerSize.height
        )
        let boxW = 0.38 * containerSize.width
        let boxH = 0.58 * containerSize.height

        ZStack(alignment: .bottom) {
            Capsule()
                .fill(LinearGradient(colors: [.init(red: 0.36, green: 0.22, blue: 0.10), .black.opacity(0.7)], startPoint: .top, endPoint: .bottom))
                .frame(width: boxW * 0.18, height: boxH * 0.82)
            Circle()
                .fill(Color(white: 0.88))
                .frame(width: boxW * 0.46, height: boxW * 0.46)
                .offset(y: -boxH * 0.78)
        }
        .frame(width: boxW, height: boxH, alignment: .bottom)
        .rotationEffect(.degrees(state.rotation), anchor: .bottom)
        .offset(x: state.dx * boxW, y: state.dy * boxH)
        .position(x: pivot.x, y: pivot.y - boxH / 2)
    }
}

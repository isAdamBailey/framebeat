import SwiftUI
import FrameBeatCore

/// Port of `SequencerLine.vue`: one row of step dots, each positioned at its
/// true fractional time in the bar (`i / count`) so the playhead sweeps
/// exactly over a dot the moment it fires. On/off dots and the current
/// (playhead) step follow DESIGN.md's Step Dots spec.
struct SequencerLineView: View {
    @Binding var line: Line
    let current: Int?

    @State private var hovered: Int?

    var body: some View {
        GeometryReader { geo in
            ZStack {
                Capsule()
                    .fill(Theme.Color.track)
                    .frame(height: 4)
                    .position(x: geo.size.width / 2, y: geo.size.height / 2)
                ForEach(0..<line.count, id: \.self) { i in
                    dotView(i, geo: geo)
                }
            }
        }
        .frame(height: 32)
        .opacity(line.muted ? 0.4 : 1)
        .animation(.easeOut(duration: 0.15), value: line.muted)
    }

    private func dotView(_ i: Int, geo: GeometryProxy) -> some View {
        let on = line.dots[i]
        let isCurrent = current == i
        let isHovered = hovered == i
        let x = geo.size.width * (line.count <= 1 ? 0.5 : CGFloat(i) / CGFloat(line.count))
        let slot = geo.size.width / CGFloat(max(line.count, 1))
        let fullSize: CGFloat = on ? 24 : 20
        // DESIGN.md's narrow rows: under a 28pt slot both dots shrink by
        // (slot - 4pt) / 24pt, so neighbours keep a 4pt gap (Slide Over).
        let size = fullSize * min(1, max(0, (slot - 4) / 24))
        // Hit area 8pt past the dot on each side (the web's 36px target), but
        // never wider than one step's slot, so neighbours can't overlap.
        let hitWidth = min(fullSize + 16, slot)

        return Button {
            line.dots[i].toggle()
        } label: {
            Circle()
                .fill(on ? Theme.Color.forSound(line.sound) : (isHovered ? Theme.Color.controlFillPressed : Theme.Color.panelSolid))
                .overlay(Circle().stroke(isHovered ? Color.white : Theme.Color.controlBorderPressed, lineWidth: on ? 0 : 2))
                .overlay(isCurrent ? Circle().stroke(Color.white.opacity(0.8), lineWidth: 2) : nil)
                .brightness(on && isHovered ? 0.1 : 0)
                .frame(width: size, height: size)
                .scaleEffect(isCurrent ? 1.25 : (isHovered && !on ? 1.1 : 1))
                .frame(width: hitWidth, height: fullSize + 16)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .onHover { hovered = $0 ? i : (hovered == i ? nil : hovered) }
        .zIndex(isCurrent ? 1 : 0)
        .animation(.easeOut(duration: 0.08), value: isCurrent)
        .animation(.easeOut(duration: 0.15), value: isHovered)
        .position(x: x, y: geo.size.height / 2)
        .help("\(i + 1)")
        .accessibilityLabel("Step \(i + 1)")
        .accessibilityAddTraits(on ? .isSelected : [])
    }
}

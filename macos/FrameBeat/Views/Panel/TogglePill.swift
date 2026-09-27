import SwiftUI

/// Port of `TogglePill.vue`: the panel's one secondary-control shape — a
/// bordered pill with a readable label, pressed = lit neutral fill. Mute and
/// Chime on 1 both use it.
struct TogglePill<Label: View>: View {
    let pressed: Bool
    let action: () -> Void
    @ViewBuilder let label: () -> Label

    @State private var hovering = false

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) { label() }
                .font(.system(size: 12, weight: .semibold))
                .padding(.horizontal, 14)
                .frame(minWidth: 36, minHeight: 36)
                .contentShape(Capsule())
        }
        .buttonStyle(.plain)
        .foregroundStyle(pressed || hovering ? Color.white : Theme.Color.controlText)
        .background(
            Capsule().fill(pressed ? Theme.Color.controlFillPressed : (hovering ? Theme.Color.panelBorder : .clear))
        )
        .overlay(
            Capsule().stroke(pressed ? Theme.Color.controlBorderPressed : Theme.Color.controlBorder, lineWidth: 1)
        )
        .onHover { hovering = $0 }
        .accessibilityAddTraits(pressed ? .isSelected : [])
    }
}

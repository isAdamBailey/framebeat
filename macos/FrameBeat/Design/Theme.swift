import SwiftUI
import FrameBeatCore

/// Phase 5 (partial): DESIGN.md's color/typography tokens as a real token
/// layer. Colors are the frontmatter's `colors` block; fonts implement the
/// "One Display Face Rule" — Fraunces only for the title and headline
/// numerals (step counts, BPM), system sans everywhere else.
///
/// Fraunces is bundled as `fraunces-latin-soft-normal.ttf` (converted from
/// the npm package's `.woff2` via `woff2_decompress` — `ATSApplicationFontsPath`
/// does not register `.woff2` on macOS; confirmed empirically by checking
/// `NSFontManager.shared.availableFontFamilies` before/after the swap). The
/// family name Core Text sees is "Fraunces" (from the font's own `name`
/// table), independent of the file's `@fontsource` filename.
enum Theme {
    enum Color {
        static let stage = SwiftUI.Color(hex: 0x020617)
        static let panel = SwiftUI.Color(hex: 0x0f172a).opacity(0.7)
        static let panelBorder = SwiftUI.Color(hex: 0x1e293b)
        static let ink = SwiftUI.Color(hex: 0xf1f5f9)
        static let labelMuted = SwiftUI.Color(hex: 0x64748b)
        static let woodNeutral = SwiftUI.Color(hex: 0x78716c)
        static let woodNeutralStrong = SwiftUI.Color(hex: 0xd6d3d1)
        static let woodNeutralBorder = SwiftUI.Color(hex: 0x44403c)
        static let woodNeutralSurface = SwiftUI.Color(hex: 0x292524)

        /// The Three-Sound Rule: these three map 1:1 to Bass/Tone/Click and
        /// are never reused as a generic UI accent.
        static let bassSky = SwiftUI.Color(hex: 0x38bdf8)
        static let toneAmber = SwiftUI.Color(hex: 0xfbbf24)
        static let clickEmber = SwiftUI.Color(hex: 0xf97316)

        static func forSound(_ sound: Sound) -> SwiftUI.Color {
            switch sound {
            case .bass: return bassSky
            case .edge: return toneAmber
            case .click: return clickEmber
            }
        }
    }

    enum Typography {
        /// The instrument's name only — DESIGN.md's Display style.
        static func display(size: CGFloat = 28) -> Font {
            .custom("Fraunces", size: size).weight(.semibold)
        }

        /// Step counts and BPM only — DESIGN.md's Numeral style. Fraunces
        /// doesn't ship a static 900-weight instance in the bundled soft
        /// grade, so `.black` is the closest available weight.
        static func numeral(size: CGFloat) -> Font {
            .custom("Fraunces", size: size).weight(.black)
        }

        /// Section/line labels and units — always system sans, per the
        /// One Display Face Rule.
        static let label = Font.system(size: 11, weight: .semibold)
        static let body = Font.system(size: 14, weight: .regular)
    }
}

private extension SwiftUI.Color {
    init(hex: UInt32) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }
}

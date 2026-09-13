import SwiftUI

@main
struct FrameBeatApp: App {
    var body: some Scene {
        WindowGroup("FrameBeat") {
            ContentView()
                #if os(macOS)
                .frame(minWidth: 720, idealWidth: 780, minHeight: 820, idealHeight: 860)
                #endif
        }
        #if os(macOS)
        .windowResizability(.contentMinSize)
        #endif
        .commands {
            #if os(macOS)
            CommandGroup(replacing: .appInfo) {
                Button("About FrameBeat") {
                    NSApp.orderFrontStandardAboutPanel(options: [
                        .credits: NSAttributedString(string: "A frame drum & polyrhythmic step sequencer.\nAll audio is synthesized live — no samples, no accounts, no network."),
                    ])
                }
            }
            #endif
            // Keyboard-only shortcuts — dead weight on iOS/iPadOS without a
            // hardware keyboard, but harmless to keep declared (the APIs
            // themselves are cross-platform); a touch-first About/Help
            // surface for iPad is tracked as separate follow-up work.
            CommandMenu("Playback") {
                Button("Play/Pause") {
                    NotificationCenter.default.post(name: .fbTogglePlay, object: nil)
                }
                .keyboardShortcut(.space, modifiers: [])

                Button("Increase Tempo") {
                    NotificationCenter.default.post(name: .fbTempoUp, object: nil)
                }
                .keyboardShortcut(.upArrow, modifiers: .command)

                Button("Decrease Tempo") {
                    NotificationCenter.default.post(name: .fbTempoDown, object: nil)
                }
                .keyboardShortcut(.downArrow, modifiers: .command)

                Divider()

                Button("Toggle Bottom-Line Mute") {
                    NotificationCenter.default.post(name: .fbToggleMute, object: nil)
                }
                .keyboardShortcut("m", modifiers: .command)

                Divider()

                Button("Reset Pattern") {
                    NotificationCenter.default.post(name: .fbResetPattern, object: nil)
                }
            }
            #if os(macOS)
            CommandGroup(replacing: .help) {
                Link("FrameBeat Help & Privacy", destination: URL(string: "https://isadambailey.github.io/framebeat/privacy.html")!)
            }
            #endif
        }
    }
}

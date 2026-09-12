import SwiftUI

@main
struct FrameBeatApp: App {
    var body: some Scene {
        WindowGroup("FrameBeat") {
            ContentView()
        }
        .windowResizability(.contentSize)
    }
}

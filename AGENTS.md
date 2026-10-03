# AGENTS.md

## Project

FrameBeat is a frame drum and polyrhythmic step sequencer: click or tap the drum to play it directly, or program two step lines (each with its own step count, sound, and mute) and hit play. The bottom line sets the tempo and bar length; the top line divides that same bar into its own number of steps, so the two lines run independent polyrhythms while always landing together on beat one.

There is no backend, database, or persistence. All state is in-memory for the current session, and all audio is synthesized live.

Four apps, three implementations:

- **Web** (`src/`) — Vue 3, TypeScript, and Vite, plus the marketing site. This is the reference for product behavior. Sound goes through the Web Audio API.
- **Mac and iPad** (`macos/`) — one SwiftUI source tree (`macos/FrameBeat/`) and one `FrameBeatCore` package, built as the **FrameBeat** (macOS) and **FrameBeatiPad** (iPadOS 17+, iPad only) targets. The Xcode project is generated from `macos/project.yml` with XcodeGen; edit the YAML, then `xcodegen generate`. iPhone is out of scope until the portrait panel is redesigned. Both ship as one Universal Purchase (`io.adambailey.framebeat`).
- **Android** (`android/`) — Kotlin and Jetpack Compose, phone and tablet, built with Gradle (Kotlin DSL). `:engine` is a pure JVM module (types, geometry, voices, scheduling) and `:app` is the Compose UI, session state, and `AudioTrack` output. Application id `io.adambailey.framebeat`, `minSdk` 26. Not a WebView. The port plan is GitHub issue #16.

iPhone stays out of scope, including in the Android work.

`README.md` is the public introduction. Native build, signing, fonts, and known audio differences are in `macos/README.md`.

`spec/` at the repo root holds shared JSON test cases (geometry, envelopes, sequencer timing, defaults, ranges, key map). The web, Swift, and Android test suites each run them; that is how the three copies of every constant stay in agreement. Only tests read `spec/` — shipping code keeps its own constants.

## Keeping web and native in sync

The web stays the reference. Swift and Android are both ports of it; neither is a source of new behavior. A behavior change lands on the web, in Swift, and in Android in the same change. Mac and iPad compile that Swift, so one native edit covers both. Build the target you are changing; if you touched shared UI or the engine, build **FrameBeatiPad** as well.

Port these together. The web file is the reference:

| Web | Native | Android |
| --- | --- | --- |
| `src/types/drum.ts` | `Sound.swift` | |
| `src/lib/drumAudio.ts` | `VoiceSpec.swift`, `DrumSynth.swift`, `LiveAudioEngine.swift` | |
| `src/composables/useSequencer.ts` | `Sequencer.swift`, `LiveSequencer.swift`, `LiveScheduleMath.swift` | |
| `src/lib/geometry.ts` | `Geometry.swift` | |
| `src/components/drum/` | `macos/FrameBeat/Views/` | |
| `DESIGN.md` | `macos/FrameBeat/Design/Theme.swift` | `ui/Theme.kt` |

Swift files in that table live under `macos/FrameBeatCore/Sources/FrameBeatCore/` unless the path says otherwise. Android files live under `android/app/src/main/kotlin/io/adambailey/framebeat/` (UI, theme, audio output) or `android/engine/src/main/kotlin/io/adambailey/framebeat/engine/` (everything else). An empty Android cell is not ported yet; add the file to the table in the PR that creates it.

Leave these on one side:

- Web only: `src/components/site/`, SEO in `index.html`, `public/robots.txt`, `public/sitemap.xml`, the Smart App Banner. App Store and privacy URLs live in `src/lib/links.ts`; update `index.html`'s `apple-itunes-app` meta tag with them when the app ID changes. Marketing copy stays sourced from the codebase or `src/views/PrivacyView.vue` — no price or iPhone claims. App Store CTAs stay neutral (no sound colors, no glow).
- Native only: Mac menu bar and window chrome, the iOS `AVAudioSession` in `RealtimeAudio.swift`, signing and `project.yml`.
- Android only: Gradle build files, the manifest, `AudioTrack` output, and Play signing. Android ships with no permissions, no network, no analytics, and no third-party SDKs.
- Keep Android pull requests separate from Apple ones: a PR that touches `src/` or `macos/` contains no Android code, and an Android PR touches neither.

`VoiceSpec.swift` is the native source of truth for voice parameters, shared by the offline renderer and live playback. Those numbers are copied from `src/lib/drumAudio.ts`. Android copies the same numbers from `drumAudio.ts`, not from Swift. Neither native triangle oscillator nor noise table is sample-identical to Web Audio; judge voices against the parameter spec and by ear. Details are in `macos/README.md`.

## Commands

Web:

```bash
npm install
npm run dev          # Vite dev server
npm run build        # vue-tsc -b && vite-ssg build (type-check, then prerender index.html)
npm run preview
npm run lint         # eslint ., type-aware strict + strictTypeChecked
npx vue-tsc --noEmit
```

The web app has no test script. ESLint will catch `any`, unused vars, and unsafe conditionals. Fix the root cause rather than adding `eslint-disable` or widening a type to `any`.

Native engine, from `macos/FrameBeatCore`:

```bash
swift test
swift run frame-beat-selfcheck
```

Native apps, from `macos`:

```bash
xcodegen generate
xcodebuild -project FrameBeat.xcodeproj -scheme FrameBeat -configuration Debug build
xcodebuild -project FrameBeat.xcodeproj -scheme FrameBeatiPad \
  -destination 'platform=iOS Simulator,name=iPad Pro 13-inch (M5)' build
```

Android, from `android` (JDK 17 and the Android SDK; CI runs all three on Ubuntu):

```bash
./gradlew test           # :engine JVM unit tests, no emulator
./gradlew lint           # Android Lint on :app, warnings are errors
./gradlew assembleDebug
```

## Architecture

**Web state**: `src/views/HomeView.vue` owns `bpm`, the top and bottom step lines, `strikes`, `bellTrigger`, and `soundBlocked`, passes them down as props, and takes changes back through `toggle`, `patch`, `toggle-play`, and `bpm-change`. `src/App.vue` is the router shell. There is no Pinia store. Native state is `macos/FrameBeat/Model/AppState.swift`.

**Web scheduling** (`src/composables/useSequencer.ts`): a look-ahead scheduler plus animation-frame sync. Two step streams are booked on the `AudioContext` clock from a 25ms `setInterval`, and a `requestAnimationFrame` loop drains already-booked events so `currentTop`, `currentBottom`, and `progress` update when the sound is heard. The bottom line's `count` and `bpm` define the bar; the top line divides that bar (`topStepDur = barDur / top.count`), which is why both lines land together on step 0. When `bpm` or either count changes mid-playback, `anchor()` re-anchors both streams on the next bar. The native counterparts are `Sequencer.swift` (offline event list) and `LiveSequencer.swift` / `LiveScheduleMath.swift` (real-time look-ahead). This timing is sensitive — play the sequencer, and run `swift test` after a scheduler change.

**Web synthesis** (`src/lib/drumAudio.ts`): a singleton `AudioContext` → `GainNode` → `DynamicsCompressorNode` chain. `triggerDrumSound(type, when)` and `triggerDing(when)` resolve that context and clamp `when` to `>= currentTime`. It also handles the iOS audio-unlock workaround and `subscribeAudioState` / `isAudioBlocked` for the sound-blocked banner. Native playback is `LiveAudioEngine.swift` (AVAudioEngine) driven by `VoiceSpec.swift`.

**Drum**: `src/components/drum/DrumCanvas.vue` is a DOM/CSS drum of layered gradient divs, built on `src/lib/geometry.ts` (`zonePoint`, `swingFor`, mallet pivots). A click is classified as bass, tone, or click by distance from center and plays the sound plus a ripple and mallet swing. Sequencer strikes arrive on the `strikes` prop and use that same path. The native view is `macos/FrameBeat/Views/Drum/DrumView.swift`, using `Geometry.swift`.

The web drum is a real `<button>`, with its `@keydown` handler on the element itself. One `KEY_MAP` covers every shortcut: `Q`/`W`/`E` and `I`/`O`/`P` are the left and right mallet's Click/Tone/Bass zones (outer keys hit the rim, inner keys hit the center), and `ArrowLeft` / `ArrowRight` are a per-side Tone strike. `DrumCanvas` watches `playing` and focuses itself when playback starts. Keep that keyboard behavior in `DrumView` as well when it changes.

**Web animations**: Web Animations API and Vue `<TransitionGroup>`, no animation library. `src/composables/useAnimate.ts` exposes `replay(keyframes, options)`, used by `Mallet.vue` and `Bell.vue`. Ripples in `DrumCanvas.vue` remove themselves with a `setTimeout` of `RIPPLE_DURATION_MS`; keep that value matched to the CSS transition.

**Marketing**: the page header in `HomeView.vue` is the hero and sits directly above the drum. Keep the drum in the first viewport on phones. `src/components/site/` holds `AboutSections.vue`, `PolyrhythmFigure.vue`, and `AppStoreLink.vue`.

**SEO / prerendering**: `main.ts` boots through `vite-ssg` with `/` and `/privacy`, so `npm run build` renders both to static HTML (`dirStyle: 'nested'` writes `privacy/index.html`). Touch `window`, `document`, `AudioContext`, and `performance` only inside `onMounted`, event handlers, or other client-only paths — never at module top level or during `setup()`. Static SEO lives in `index.html` (canonical, Open Graph, `WebApplication` JSON-LD, Google Analytics `G-G8TGTBLPZD`) plus `public/robots.txt`, `public/sitemap.xml`, and `public/og-image.png`. The canonical origin is `https://framebeat.adambailey.io/`. Privacy links on the drum page open `/privacy` in a new tab so the in-memory session stays mounted.

**Types**: `Sound`, `Line`, `Strike`, `Strikes`, `Swing`, and `Ripple` live in `src/types/drum.ts`. The native domain types are `Sound.swift`. Import them; do not redefine them.

## Working notes

- Do not reintroduce a backend, database, auth, or persistence layer unless explicitly asked.
- Keep new web animations on the Web Animations API and `<TransitionGroup>`.
- Read `DESIGN.md` before restyling. Three accent colors map one-to-one to Bass, Tone, and Click. Fraunces (`@fontsource-variable/fraunces`, imported in `main.ts`; a TTF under `macos/FrameBeat/Resources/Fonts/` on Mac and iPad, copied to `android/app/src/main/res/font/fraunces.ttf` on Android) is for the title and headline numerals only.
- Before finishing a web change, run `npm run lint` and `npx vue-tsc --noEmit`. For playback or animation, verify in a browser: play/pause, tempo changes mid-play, mute, and step-count changes.
- Before finishing a behavior change, port the matching Swift and Android files, then run `swift test` in `macos/FrameBeatCore` and `./gradlew test lint` in `android`. Play the native apps when the change is audible or visible. A green web type-check does not mean the native ports happened.

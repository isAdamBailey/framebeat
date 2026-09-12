# FrameBeat for macOS

A native SwiftUI port of FrameBeat — a frame drum and polyrhythmic step
sequencer. Click the drum to play it directly, or program two step lines
(each with its own step count, sound, and mute) and hit play; the bottom
line sets the tempo/bar length, the top line divides the same bar into its
own step count, so the two lines run independent polyrhythms while always
landing together on beat one.

All audio is synthesized live (no samples) via a custom `AVAudioEngine`
render graph. There's no backend, database, or persistence — all state is
in-memory for the current session, matching the web app this is ported
from (see the repo root's `CLAUDE.md`/`DESIGN.md`).

## What's here

```
macos/project.yml               xcodegen spec for FrameBeat.xcodeproj — edit this,
                                 not the generated project, then `xcodegen generate`
macos/FrameBeat.xcodeproj/      generated Xcode project (tracked; regenerate, don't hand-edit)
macos/FrameBeat/                the app target
  App/                           FrameBeatApp.swift (SwiftUI @main), ContentView.swift
  Audio/                         RealtimeAudio.swift
  Model/                         AppState.swift — @Observable bpm/top/bottom/strikes/bellTrigger
  Views/Drum/                    DrumView.swift
  Design/                        Theme.swift — DESIGN.md color/typography tokens
  Sequencer/                     SequencerLineView etc.
  Resources/                     Assets.xcassets (AppIcon), Fonts/ (bundled Fraunces TTF, OFL license)
macos/FrameBeatCore/
  Package.swift
  Sources/FrameBeatCore/       the synth/scheduler engine (library), depended on by FrameBeat
    Sound.swift                 Sound/Line/Strike/Strikes — from src/types/drum.ts
    Envelope.swift               Web Audio AudioParam automation (exp ramps)
    Biquad.swift                 lowpass/highpass/bandpass filter coefficients
    DrumSynth.swift               the four voices — 1:1 port of src/lib/drumAudio.ts
    Sequencer.swift               offline polyrhythm event generator — from useSequencer.ts
    LiveAudioEngine.swift         real-time AVAudioEngine/AVAudioSourceNode graph
    LiveSequencer.swift/LiveScheduleMath.swift  real-time look-ahead scheduler
    WavWriter.swift               plain PCM16 WAV writer, no AVFoundation needed
  Sources/frame-beat-render/    CLI: renders a pattern to WAV + prints timing checks
  Sources/frame-beat-selfcheck/ CLI: regression suite (belt-and-braces alongside XCTest)
  Tests/FrameBeatCoreTests/     XCTest suite
```

## Build & run the app

```bash
cd macos
xcodegen generate                # regenerate FrameBeat.xcodeproj after editing project.yml
xcodebuild -project FrameBeat.xcodeproj -scheme FrameBeat -configuration Debug build
open "$(xcodebuild -project FrameBeat.xcodeproj -scheme FrameBeat -configuration Debug -showBuildSettings | awk -F'= ' '/ CODESIGNING_FOLDER_PATH/{print $2}')"
```

Or just open `macos/FrameBeat.xcodeproj` in Xcode and hit Run. The app
builds sandboxed (`com.apple.security.app-sandbox`) with hardened runtime
on — confirm with `codesign -dv --entitlements - FrameBeat.app`. `xcodegen`
is required to regenerate the project after editing `project.yml` (`brew
install xcodegen`); the `.xcodeproj` itself is tracked in git so a fresh
clone can build without it.

**If the app is already running when you rebuild**, `open` just refocuses
the existing (stale) process instead of relaunching the new build — quit it
first (`osascript -e 'tell application id "io.adambailey.framebeat" to quit'`)
or use `open -n` to force a new instance.

Every voice's envelope timings/values, and the scheduler's bar/step math, are
copied verbatim from `src/lib/drumAudio.ts` and
`src/composables/useSequencer.ts` — see the doc comments in each Swift file
for the mapping. Anything touching playback timing needs a real by-ear check
in the running app, not just `swift test`.

## Build & test the engine package

```bash
cd macos/FrameBeatCore
swift build
swift test                          # 13 XCTest cases
swift run frame-beat-selfcheck      # regression checks, same ground as XCTest
swift run frame-beat-render --bpm 90 --top 3 --top-sound edge \
  --bottom 4 --bottom-sound bass --bars 4 --out pattern.wav
```

Requires Xcode installed with its license accepted (`sudo xcodebuild
-license`) — without that, `swift build`/`swift test` fail with `error:
'framebeatcore': Invalid manifest ...` even on a trivial manifest, which is
`xcrun`'s license gate breaking SwiftPM, not a bug here.

`frame-beat-selfcheck` includes golden impulse-response samples captured
from a live `OfflineAudioContext` in Chrome for each of the three filter
configurations `drumAudio.ts` actually uses — that's how the
Q-is-in-decibels-for-lowpass/highpass-but-linear-for-bandpass behavior
documented in `Biquad.swift` was caught: the first implementation used
linear Q everywhere and was measurably wrong (~0.01 RMS impulse-response
error vs. Chrome) on two of the three filter types.

`frame-beat-render` renders a full pattern to WAV and prints PASS/FAIL for
the timing invariant that matters most: every bar boundary must have the bar
chime and top-line step 0 land at *exactly* the same sample as bottom-line
step 0, regardless of the two lines' independent step counts. Verified for
the app's default 3-vs-4 pattern and a less tidy 7-vs-5 pattern. **This WAV
is not directly A/B-able against a web-app recording** — see the comment on
`OfflineRenderer`'s noise table in `DrumSynth.swift` for why; compare
individual voices/filters instead of whole renders.

## Known audio differences from the web app

- `DrumSynth.swift`'s triangle oscillator is a plain (non-band-limited)
  triangle wave; Web Audio's built-in triangle oscillator is band-limited.
  Should sound very close but isn't bit-identical — affects the edge slap
  and wood click voices' upper harmonics.
- The two noise tables (Swift's vs. the browser's) are independent random
  draws with the same statistics, not the same values, so filtered-noise
  voices (open bass's low end, edge slap, wood click) will never null out
  against each other even if the filters are correct — judge them by ear/
  spectrum shape, not by diffing samples.

## Fonts

Fraunces is bundled as a plain TTF (`FrameBeat/Resources/Fonts/`),
registered via `ATSApplicationFontsPath`. **`ATSApplicationFontsPath` does
not register `.woff2`** — the npm `@fontsource-variable/fraunces` package
only ships `.woff2` files, so the TTF here was produced with `woff2_decompress`
(`brew install woff2`) from `node_modules/@fontsource-variable/fraunces/files/fraunces-latin-soft-normal.woff2`.
If a different weight/style is ever needed, convert the corresponding
`.woff2` the same way rather than assuming any given file in that package
will register as-is.

# FrameBeat for macOS — native port

Status: **Phase 1 of the port plan** (Xcode app project scaffold) done on
top of Phase 3/4 (synth engine + polyrhythm scheduler), which are
implemented and verified as a standalone Swift package, `FrameBeatCore`.
See `/Users/adambailey/.claude/plans/what-steps-do-i-abundant-turing.md`
(or ask Claude to re-surface it) for the full phased plan through App
Store submission. Phase 0 (Apple Developer Program enrollment, bundle ID,
privacy policy URL) is not done — needed before Phase 6/7 (store
submission), not before further UI/engine work.

## What's here

```
macos/project.yml               xcodegen spec for FrameBeat.xcodeproj — edit this,
                                 not the generated project, then `xcodegen generate`
macos/FrameBeat.xcodeproj/      generated Xcode project (tracked; regenerate, don't hand-edit)
macos/FrameBeat/                the real app target
  App/                           FrameBeatApp.swift (SwiftUI @main), ContentView.swift
  Audio/                         RealtimeAudio.swift
  Views/Drum/                    DrumView.swift
  Model/, Sequencer/, Design/    empty — Phase 2/5 work
  Resources/                     Assets.xcassets (AppIcon placeholder — real art is
                                 still Phase 5), Fonts/ (bundled Fraunces, OFL license)
macos/FrameBeatCore/
  Package.swift
  Sources/FrameBeatCore/       the ported engine (library), used by both FrameBeat and FrameBeatDemo
    Sound.swift                 Sound/Line — from src/types/drum.ts
    Envelope.swift               Web Audio AudioParam automation (exp ramps)
    Biquad.swift                 lowpass/highpass/bandpass filter coefficients
    DrumSynth.swift               the four voices — 1:1 port of src/lib/drumAudio.ts
    Sequencer.swift               polyrhythm event generator — from useSequencer.ts
    LiveSequencer.swift/LiveScheduleMath.swift  real-time look-ahead scheduler (Phase 4)
    WavWriter.swift               plain PCM16 WAV writer, no AVFoundation needed
  Sources/FrameBeatDemo/        SwiftUI demo app, runnable as a bare executable — no
                                 Xcode build/signing needed, useful for quick iteration;
                                 FrameBeat/ (the real app target) is a copy of this content
                                 wired into a proper sandboxed Xcode project instead
  Sources/frame-beat-render/    CLI: renders a pattern to WAV + prints timing checks
  Sources/frame-beat-selfcheck/ CLI: 11-check regression suite (belt-and-braces alongside XCTest)
  Tests/FrameBeatCoreTests/     XCTest suite
```

## Build & run the app (Phase 1 scaffold)

```bash
cd macos
xcodegen generate                # regenerate FrameBeat.xcodeproj after editing project.yml
xcodebuild -project FrameBeat.xcodeproj -scheme FrameBeat -configuration Debug build
open "$(xcodebuild -project FrameBeat.xcodeproj -scheme FrameBeat -configuration Debug -showBuildSettings | awk -F'= ' '/ CODESIGNING_FOLDER_PATH/{print $2}')"
```

Or just open `macos/FrameBeat.xcodeproj` in Xcode and hit Run. The app
builds sandboxed (`com.apple.security.app-sandbox`) with hardened runtime
on, per the plan's Phase 1 capabilities — confirm with `codesign -dv
--entitlements - FrameBeat.app`. `xcodegen` is required to regenerate the
project after editing `project.yml` (`brew install xcodegen`); the
`.xcodeproj` itself is tracked in git so a fresh clone can build without it.

Every voice's envelope timings/values, and the scheduler's bar/step math, are
copied verbatim from `src/lib/drumAudio.ts` and
`src/composables/useSequencer.ts` — see the doc comments in each Swift file
for the mapping.

## Build & test

```bash
cd macos/FrameBeatCore
swift build
swift test                          # 9 XCTest cases
swift run frame-beat-selfcheck      # 11 regression checks, same ground as XCTest
swift run frame-beat-render --bpm 90 --top 3 --top-sound edge \
  --bottom 4 --bottom-sound bass --bars 4 --out pattern.wav
```

Requires Xcode installed with its license accepted (`sudo xcodebuild
-license`) — without that, `swift build`/`swift test` fail with `error:
'framebeatcore': Invalid manifest ...` even on a trivial manifest, which is
`xcrun`'s license gate breaking SwiftPM, not a bug here. Confirmed fixed
once the license was accepted.

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

## Still unverified — do this before Phase 1 UI work

**Listen and compare** the two apps by ear (`npm run dev` for the web
version alongside `frame-beat-render`'s WAV output for individual strikes).
Two specific things flagged during the port:

- `DrumSynth.swift`'s triangle oscillator is a plain (non-band-limited)
  triangle wave; Web Audio's built-in triangle oscillator is band-limited.
  Should sound very close but isn't bit-identical — affects the edge slap
  and wood click voices' upper harmonics.
- The two noise tables (Swift's vs. the browser's) are independent random
  draws with the same statistics, not the same values, so filtered-noise
  voices (open bass's low end, edge slap, wood click) will never null out
  against each other even if the filters are correct — judge them by ear/
  spectrum shape, not by diffing samples.

## Next: Phase 1 (Xcode project scaffold)

This package becomes the `FrameBeatCore` dependency of the app target, with
`DrumSynth`'s offline voice-rendering math reused inside an
`AVAudioSourceNode` render callback for real-time playback (see Phase 3 in
the plan for the realtime wrapper design, including the accumulation-vs-
multiplication note for the scheduler).

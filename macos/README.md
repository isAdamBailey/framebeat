# FrameBeat for macOS — native port

Status: **Phase 3/4 of the port plan** (synth engine + polyrhythm scheduler)
implemented and verified as a standalone Swift package,
`FrameBeatCore`. No Xcode project exists yet — see
`/Users/adambailey/.claude/plans/what-steps-do-i-abundant-turing.md` (or ask
Claude to re-surface it) for the full phased plan through App Store
submission.

## What's here

```
macos/FrameBeatCore/
  Package.swift
  Sources/FrameBeatCore/       the ported engine (library)
    Sound.swift                 Sound/Line — from src/types/drum.ts
    Envelope.swift               Web Audio AudioParam automation (exp ramps)
    Biquad.swift                 lowpass/highpass/bandpass filter coefficients
    DrumSynth.swift               the four voices — 1:1 port of src/lib/drumAudio.ts
    Sequencer.swift               polyrhythm event generator — from useSequencer.ts
    WavWriter.swift               plain PCM16 WAV writer, no AVFoundation needed
  Sources/frame-beat-render/    CLI: renders a pattern to WAV + prints timing checks
  Tests/FrameBeatCoreTests/     XCTest suite (see "Known build issue" below)
```

Every voice's envelope timings/values, and the scheduler's bar/step math, are
copied verbatim from `src/lib/drumAudio.ts` and
`src/composables/useSequencer.ts` — see the doc comments in each Swift file
for the mapping.

## Known build issue on this machine

`swift build` / `swift test` fail with:

```
error: 'framebeatcore': Invalid manifest ...
Undefined symbols for architecture arm64:
  "PackageDescription.Package.__allocating_init(...)"
```

This is **not a bug in this package** — a trivial one-target manifest fails
identically. It reproduced with Command-Line-Tools-only Swift (6.3.3) *and*
after Xcode 26 was subsequently installed here (6.4.0) — same error either
way, so it isn't fixed by having Xcode present. As of this port, Xcode is
installed but its license hasn't been accepted yet
(`sudo xcodebuild -license`), which currently blocks the `xcodebuild`/
`swift build` toolchain entirely (`xcrun`: "You have not agreed to the Xcode
license agreements"). **Accept the license first**, then retry `swift build
&& swift test` — if the manifest error persists after that, it's a separate,
real SwiftPM issue worth investigating (possibly a stale/mismatched
`libPackageDescription.dylib`) rather than the license gate. Delete this
section once `swift build`/`swift test` are confirmed working.

Plain `swiftc` compiles fine throughout (with `DEVELOPER_DIR=/Library/Developer/CommandLineTools`
if the license block is in the way) — that's what was used to build and
verify everything below.

## How the engine was verified without a working `swift build`

The library and both CLI targets were compiled directly with `swiftc`
(bypassing SwiftPM's manifest step, which only `swift build`/`swift test`
need):

```bash
cd macos/FrameBeatCore
mkdir -p /tmp/fbc && export DEVELOPER_DIR=/Library/Developer/CommandLineTools  # only if the Xcode license block is active

swiftc -module-name FrameBeatCore -emit-module \
  -emit-module-path /tmp/fbc/FrameBeatCore.swiftmodule -emit-library \
  -o /tmp/fbc/libFrameBeatCore.dylib Sources/FrameBeatCore/*.swift

swiftc Sources/frame-beat-render/main.swift \
  -I /tmp/fbc -L /tmp/fbc -lFrameBeatCore -o /tmp/fbc/frame-beat-render
swiftc Sources/frame-beat-selfcheck/main.swift \
  -I /tmp/fbc -L /tmp/fbc -lFrameBeatCore -o /tmp/fbc/frame-beat-selfcheck

export DYLD_LIBRARY_PATH=/tmp/fbc
/tmp/fbc/frame-beat-selfcheck   # 11 checks: envelope math, biquad coefficients
                                  # (verified against real Chrome OfflineAudioContext
                                  # impulse responses — see Biquad.swift), NaN/clip
                                  # safety, mute behavior, polyrhythm bar-lock

/tmp/fbc/frame-beat-render --bpm 90 --top 3 --top-sound edge \
  --bottom 4 --bottom-sound bass --bars 4 --out pattern.wav
```

`frame-beat-selfcheck` (`Sources/frame-beat-selfcheck/`) is the package's
real regression check right now — it's committed to the repo (unlike a
`/tmp` scratch script) so it's there for the next person. It includes golden
impulse-response samples captured from a live `OfflineAudioContext` in
Chrome for each of the three filter configurations `drumAudio.ts` actually
uses, which is how the Q-in-dB-for-lowpass/highpass-but-linear-for-bandpass
behavior documented in `Biquad.swift` was caught and fixed — the initial
implementation used linear Q everywhere and was measurably wrong (~0.01 RMS
impulse-response error vs. Chrome) on two of the three filter types.

`frame-beat-render` additionally renders a full pattern to WAV and prints
PASS/FAIL for the timing invariant that matters most: every bar boundary
must have the bar chime and top-line step 0 land at *exactly* the same
sample as bottom-line step 0, regardless of the two lines' independent step
counts. Verified for the app's default 3-vs-4 pattern and a less tidy 7-vs-5
pattern. **This WAV is not directly A/B-able against a web-app recording**
— see the comment on `OfflineRenderer`'s noise table in `DrumSynth.swift`
for why; compare individual voices/filters instead of whole renders.

The `Tests/FrameBeatCoreTests` XCTest suite covers similar ground in the
proper XCTest form; run it once `swift build`/`swift test` work (see "Known
build issue" above) and retire `frame-beat-selfcheck` at that point if
`swift test` is preferred going forward.

## Once Xcode is installed

1. `sudo xcode-select -s /Applications/Xcode.app`
2. `cd macos/FrameBeatCore && swift build && swift test` — should now work
   without the manifest-linker error above.
3. **Listen and compare**: use `frame-beat-render` to render the same
   pattern the web app is playing (`npm run dev`, screen-record with system
   audio, or just play both side by side) and A/B by ear. Two things flagged
   as unverified-by-ear during the port (see doc comments for detail):
   - `Biquad.swift`'s filter coefficients were implemented from prior
     knowledge of the Web Audio spec's formulas, not the live spec text
     (network fetches during the port returned truncated pages). If the edge
     slap / wood click / open bass voices sound off spectrally, this is the
     first place to check.
   - `DrumSynth.swift`'s triangle oscillator is a plain (non-band-limited)
     triangle wave; Web Audio's built-in triangle oscillator is band-limited.
     Should sound very close but isn't bit-identical.
4. Continue with Phase 1 (Xcode project scaffold) in the plan — this package
   becomes the `FrameBeatCore` dependency of the app target, with
   `DrumSynth`'s offline voice-rendering math reused inside an
   `AVAudioSourceNode` render callback for real-time playback (Phase 3 in
   the plan describes the realtime wrapper).

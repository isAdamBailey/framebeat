# Shared test cases

JSON cases that every FrameBeat implementation's tests run, so the copies of each constant stay in agreement. The web app is the reference: when its behavior changes, update the case here in the same change.

| File | Covers | Web (`npm test`) | Swift (`swift test`) |
| --- | --- | --- | --- |
| `geometry.json` | tap to zone and side, zone points, mallet swing | `src/lib/geometry.ts` | `Geometry.swift` |
| `voices.json` | voice parameters, envelope values at sample times | `src/lib/voiceSpec.ts` | `VoiceSpec.swift`, `Envelope.swift` |
| `sequencer.json` | event times and audible flags, next bar boundary | `src/lib/schedule.ts` | `Sequencer.swift`, `LiveScheduleMath.swift` |
| `controls.json` | defaults, ranges, key map, instrument shortcuts | `src/lib/controls.ts` | app source, read as text |

Each file starts with an `about` string that gives its units and tolerances. Only tests read these files; shipping code keeps its own constants.

The Swift tests live in `macos/FrameBeatCore/Tests/FrameBeatCoreTests/SharedSpec*Tests.swift`. The Swift defaults, ranges, and key map live in the app target, which the package tests cannot import, so `SharedSpecControlsTests` reads those Swift files as text. Swift has no counterpart to the web's modifier and key-repeat filter (`instrumentShortcuts`), so only the web runs those cases.

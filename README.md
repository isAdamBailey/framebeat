# FrameBeat

[![Web](https://github.com/isAdamBailey/framebeat/actions/workflows/web.yml/badge.svg)](https://github.com/isAdamBailey/framebeat/actions/workflows/web.yml)
[![Native](https://github.com/isAdamBailey/framebeat/actions/workflows/native.yml/badge.svg)](https://github.com/isAdamBailey/framebeat/actions/workflows/native.yml)
[![Android](https://github.com/isAdamBailey/framebeat/actions/workflows/android.yml/badge.svg)](https://github.com/isAdamBailey/framebeat/actions/workflows/android.yml)

A frame drum and polyrhythmic step sequencer. Click or tap the drum to play it directly, or program two step lines (each with its own step count, sound, and mute) and hit play. The bottom line sets the tempo and bar length; the top line divides that same bar into its own number of steps, so the two lines can run independent polyrhythms while always landing together on beat one.

Every hit is synthesized live. There are no samples, no accounts, and nothing is saved — each session starts from the same state.

Play it in the browser at [framebeat.adambailey.io](https://framebeat.adambailey.io/), or get the [Mac and iPad app](https://apps.apple.com/app/id6811452156) on the App Store. Mac and iPad are one Universal Purchase.

## The apps

The web app in `src/` is Vue 3, TypeScript, and Vite. It is also the marketing site, and it is the reference for how the instrument behaves.

The Mac and iPad apps live in `macos/`. They are one SwiftUI codebase — the same screens and the same `FrameBeatCore` synth and scheduler — built as two targets. Build and signing notes are in [`macos/README.md`](macos/README.md).

An Android app for phones and tablets is in progress in `android/`: native Kotlin and Jetpack Compose, not a WebView. Its pure-JVM `:engine` module holds the geometry, voices, and sequencer, and `:app` is the Compose instrument; building and Play signing are in [`android/README.md`](android/README.md). The plan is [issue #16](https://github.com/isAdamBailey/framebeat/issues/16).

All three implementations run the same test cases from `spec/`, so their constants, voices, and timing stay in agreement.

## Development

```bash
npm install
npm run dev
```

```bash
npm run build
npm run preview
```

Tests:

```bash
npm test                            # web, against spec/
(cd macos/FrameBeatCore && swift test)
(cd android && ./gradlew test lint) # needs JDK 17 and the Android SDK
```

## Project structure

- `src/` — the web app: drum, sequencer, synth, and marketing page
- `macos/` — the Mac and iPad app, plus the shared Swift engine
- `android/` — the Android app (in progress): `:engine` and `:app`
- `spec/` — shared JSON test cases run by the web, Swift, and Android tests

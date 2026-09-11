# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Casual, curious visitors who land on the page and start clicking — no musical training or prior sequencer experience assumed. They come to play and explore, not to complete a task or produce a finished composition.

## Product Purpose

FrameBeat is a frame drum you can strike directly by clicking, and a two-line polyrhythmic step sequencer you can program and play back. It exists as a small, self-contained instrument/toy: success is an engaging, immediate, tactile experience rather than any workflow completion or saved output.

## Positioning

FrameBeat combines a live-playable instrument (click the drum, hear and see a real strike) with a programmable sequencer in the same surface — most web drum machines are one or the other, not both fused into a single object. The bottom line sets tempo/bar length; the top line divides the same bar into its own independent step count, and the two always land together on beat one regardless of their differing step counts.

## Operating Context

Single-page, session-only experience in a desktop or mobile browser. No accounts, no saved projects, no backend — every session starts from the same blank state. Audio is synthesized live via the Web Audio API (no samples/audio library), so first interaction may be gated by the browser's autoplay policy (handled via an audio-unlock workaround and a "sound is blocked" banner).

## Capabilities and Constraints

- Click-to-strike frame drum with zone-based sound classification (bass/tone/click) by click position.
- Fully keyboard-playable drum: `Q`/`W`/`E` and `I`/`O`/`P` strike the left/right mallet's Click/Tone/Bass zones (outer-to-inner mirroring the qwerty row), arrows are a quick per-side Tone strike; playable simultaneously with the sequencer, since starting playback hands the drum keyboard focus automatically.
- Two independently configurable step lines (step count, sound, mute per line); bottom line drives tempo/bar length, top line divides the same bar.
- Play/pause transport; tempo and step counts can change mid-playback and the two lines re-anchor together on the next bar.
- No persistence, accounts, backend, or database — intentionally client-only and in-memory for the session (see CLAUDE.md).
- No test suite; correctness for scheduling/audio is verified by manually playing the app in a browser, not by type-checking alone.

## Brand Commitments

A visual identity is now established — see `DESIGN.md` for the full system. In short: the drum and bell are staged as the app's one dimensional, warmly-lit object against an otherwise flat, near-black control surface; exactly three accent colors (sky/amber/orange) map one-to-one to the Bass/Tone/Click sounds and are never used decoratively; and Fraunces (a self-hosted display serif) is reserved for the title and the app's headline numerals (step counts, BPM), with the system sans everywhere else.

## Evidence on Hand

No assets, testimonials, or supplied content beyond the codebase itself (README.md, CLAUDE.md) and the current DOM/CSS-based frame drum implementation in `src/components/drum/DrumCanvas.vue`.

## Product Principles

- Immediacy over onboarding: the drum must be strikeable and rewarding within the first click, with no setup or explanation required.
- Play first, program second: direct-strike interaction and the step sequencer should feel like two facets of one instrument, not separate modes.
- Precision under the hood, playfulness on the surface: the polyrhythmic timing math must be exact even while the interface stays inviting and toy-like.
- Nothing to lose: since state is session-only and unsaved, the experience should encourage free experimentation rather than careful, cautious use.

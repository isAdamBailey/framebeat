# AGENTS.md

## Project Context

FrameBeat is a client-only Vue 3 + TypeScript + Vite app: a frame drum and polyrhythmic step sequencer. There is no backend, no database, and no persistence — all state is in-memory for the current session, and all sound is synthesized live via the Web Audio API.

Start with `README.md` for setup and project structure.

## Key Files

- `src/App.vue`: owns all app state (tempo, step lines, strikes) and wires the composables to the UI.
- `src/composables/useSequencer.ts`: the audio scheduler (look-ahead `setInterval` + `requestAnimationFrame` sync). Timing-sensitive — verify changes by actually playing the sequencer, not just type-checking.
- `src/composables/useAnimate.ts`: shared Web Animations API helper for the mallet/bell strike animations.
- `src/lib/drumAudio.ts`: Web Audio synthesis — pure functions, no framework dependency.
- `src/lib/geometry.ts`: pure geometry helpers for aiming mallets/ripples at a strike point.
- `src/components/drum/`: presentational sequencer and drum-canvas components.

## Working Notes

- No backend or persistence exists on purpose — don't reintroduce a database, API client, or auth layer unless explicitly asked.
- Animations are hand-rolled (Web Animations API + Vue `<TransitionGroup>`), not a library — keep new animations consistent with that approach.
- Run `npx vue-tsc --noEmit` before finishing changes.
- For anything touching playback timing or animation, start the dev server and manually verify in a browser (play/pause, tempo changes mid-play, mute, step-count/polyrhythm changes) — type-checking alone won't catch scheduler drift or visual glitches.

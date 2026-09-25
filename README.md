# FrameBeat

A frame drum & polyrhythmic step sequencer, built with Vue 3, TypeScript, and Vite.

Click the drum to play it directly, or program the two step lines (each with its own step count, sound, and mute) and hit play. The bottom line sets the tempo/bar length; the top line divides the same bar into its own number of steps, so the two lines can run independent polyrhythms while always landing together on beat one.

All audio is synthesized live with the Web Audio API — no samples, no backend, no persistence. Everything lives in memory for the current session.

## Development

```bash
npm install
npm run dev
```

## Build

```bash
npm run build
npm run preview
```

## Project structure

- `src/App.vue` — top-level state (tempo, step lines, strikes) and layout
- `src/composables/useSequencer.ts` — look-ahead audio scheduler + animation-frame sync
- `src/composables/useAnimate.ts` — small Web Animations API helper used by the mallet/bell animations
- `src/lib/drumAudio.ts` — Web Audio synthesis (bass/tone/click hits, bar chime)
- `src/lib/geometry.ts` — drum/mallet ellipse geometry for aiming strikes visually
- `src/components/drum/` — sequencer and drum-canvas UI components
- `src/components/site/` — marketing sections around the instrument (hero App Store link, polyrhythm explainer, native-app pitch)
- `src/lib/links.ts` — App Store and privacy-policy URLs

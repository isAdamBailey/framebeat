---
name: FrameBeat
description: A frame drum and polyrhythmic step sequencer staged like a real instrument under a single light, in an otherwise flat, dark control surface.
colors:
  stage: "#020617"
  panel: "#0f172a"
  panel-border: "#1e293b"
  ink: "#f1f5f9"
  label-muted: "#64748b"
  wood-neutral: "#78716c"
  wood-neutral-strong: "#d6d3d1"
  wood-neutral-border: "#44403c"
  wood-neutral-surface: "#292524"
  bass-sky: "#38bdf8"
  tone-amber: "#fbbf24"
  click-ember: "#f97316"
typography:
  display:
    fontFamily: "Fraunces Variable, ui-serif, Georgia, serif"
    fontSize: "clamp(1.5rem, 4vw, 3rem)"
    fontWeight: 600
    lineHeight: 1.1
    letterSpacing: "-0.025em"
  numeral:
    fontFamily: "Fraunces Variable, ui-serif, Georgia, serif"
    fontWeight: 900
    lineHeight: 1
    fontFeature: "tabular-nums"
  body:
    fontFamily: "ui-sans-serif, system-ui, sans-serif"
    fontSize: "0.875rem"
    fontWeight: 400
    lineHeight: 1.5
  label:
    fontFamily: "ui-sans-serif, system-ui, sans-serif"
    fontSize: "11px"
    fontWeight: 600
    letterSpacing: "0.14em"
rounded:
  sm: "6px"
  md: "12px"
  full: "9999px"
spacing:
  xs: "8px"
  sm: "16px"
  md: "24px"
  lg: "40px"
components:
  button-primary:
    backgroundColor: "{colors.bass-sky}"
    textColor: "{colors.stage}"
    rounded: "{rounded.full}"
    padding: "0 32px"
    height: "64px"
  kbd-key:
    backgroundColor: "{colors.wood-neutral-surface}"
    textColor: "{colors.wood-neutral-strong}"
    rounded: "{rounded.sm}"
    padding: "2px 6px"
---

# Design System: FrameBeat

## Overview

**Creative North Star: "The Midnight Workshop"**

FrameBeat reads as one warm, hand-built instrument sitting under a single light in an otherwise dark, quiet workshop. The drum and bell are the only objects rendered with real material depth — turned wood, stretched hide, cast brass — lit by a soft radial glow against a near-black stage. Everything below that light is deliberately flat: a plain dark control panel with circular pills, thin sliders, and bold serif numerals, the way a mixing console sits beneath the instrument it drives. The two halves are intentionally unlike each other; that contrast is the point. Nothing else in the UI competes with the drum's illustration for dimensionality, and nothing on the drum competes with the panel for information density.

Confirmed rejections: no gradient text, no card-grid dashboard scaffolding, no second illustrated/skeuomorphic element competing with the drum, no second colored glow shadow anywhere on the page.

**Key Characteristics:**
- One lit, dimensional object (the drum + bell) against an otherwise flat dark stage
- Exactly three accent colors, each tied to a real drum sound — never decorative
- A single warm serif voice for the instrument's name and its big numbers; everything else is plain UI sans
- Circular, pill-shaped controls throughout, echoing the drum's own form

## Colors

Three accent colors, one per drum sound, sit inside a near-black, mostly-neutral stage. Color is information here, not decoration.

### Primary
- **Bass Sky** (`#38bdf8`, Tailwind `sky-400`): the Bass sound's color, and by extension the app's single interactive accent — the Play/Pause button, the sequencer playhead, slider fill, and the drum's manual keyboard-focus ring all use it, since Bass/interaction share the "press this" role.

### Secondary
- **Tone Amber** (`#fbbf24`, `amber-400`): the Tone sound's color. Appears only on Tone-sound step dots, the Tone sound-picker swatch, and a line's step-count numeral when that line is set to Tone.

### Tertiary
- **Click Ember** (`#f97316`, `orange-500`): the Click sound's color, used the same way as Tone Amber but for the Click zone/sound.

### Neutral
- **Stage** (`#020617`, `slate-950`): the page background — a near-black, slightly blue-tinted dark.
- **Panel** (`#0f172a` at 70% opacity, `slate-900/70`): the transport + sequencer card surface, sitting one step lighter than the stage.
- **Panel Border** (`#1e293b`, `slate-800`): the card's hairline border.
- **Ink** (`#f1f5f9`, `slate-100`): base body text color.
- **Label Muted** (`#64748b`, `slate-500`): every uppercase micro-label (TOP LINE, TEMPO, the Bass/Tone/Click legend, "Bar chime") and secondary numeral captions (BPM unit).
- **Wood Neutral** (`#78716c`–`#d6d3d1`, `stone-500`–`stone-300`): reserved for the header zone only — the heading's italic ampersand and the keyboard-shortcut caption/kbd chips — a warm neutral distinct from the cooler `slate` used everywhere else, tying that one area back to the drum's own wood tones.

### Named Rules
**The Three-Sound Rule.** Sky, amber, and orange mean Bass, Tone, and Click, full stop. Never reach for one of them as a generic UI highlight or a fourth unrelated accent — if a new element needs color, it's because it maps to one of the three sounds, or it stays neutral.

**The One Glow Rule.** Only the Play/Pause button carries a colored shadow glow (`0 8px 24px rgba(56,189,248,0.35)`). It is the page's single primary action; no other element earns that treatment, including on hover.

## Typography

**Display/Numeral Font:** Fraunces Variable, "soft" grade (self-hosted via `@fontsource-variable/fraunces`), falling back to `ui-serif, Georgia, serif`.
**Body/UI Font:** `ui-sans-serif, system-ui, sans-serif` (the platform stack, used deliberately as a neutral, quiet voice for controls).

**Character:** A warm, rounded display serif — chosen for its "soft" optical grade, which echoes the drum's own curved, hand-turned forms — paired against a completely plain system sans for every functional control. The serif only ever appears as the instrument's name or as one of its big numbers; it never appears in a sentence.

### Hierarchy
- **Display** (weight 600, `text-2xl` → `text-3xl`, tight tracking): the page's h1, "Frame Drum & Step Sequencer," with a single restrained flourish — an italic ampersand — as the only display trick in the system.
- **Numeral** (weight 900/700, `text-4xl`–`text-5xl` for step counts, `text-xl` for BPM, tabular figures): the app's biggest, boldest marks — each step line's step count and the tempo readout. These are the "polyrhythm" the product is about, so they're sized to be read at a glance, not tucked beside their control.
- **Label** (weight 600, 11px, `tracking-[0.14em]`, uppercase, `label-muted` color): section/line labels and units (TOP LINE, TEMPO, BPM).
- **Body** (weight 400–500, `text-xs`–`text-sm`): captions, the keyboard-shortcut hint, sound names.

### Named Rules
**The One Display Face Rule.** Fraunces appears only where the content itself is the instrument's name or one of its headline numbers (title, step counts, BPM). Every label, button, and sentence of body copy stays in the system sans — adding Fraunces anywhere else (a button label, a tooltip, body prose) breaks the rule.

## Layout

Single centered column, `max-w-3xl`, generous vertical rhythm (`py-8`–`py-10` outer, `gap-6`–`gap-10` between the drum and bell). The page has two zones stacked vertically: an untethered "stage" zone (title, caption, drum, bell, sound legend) sitting directly on the dark background with a soft radial glow behind it, and a bordered flat "panel" zone (`rounded-xl border border-slate-800 bg-slate-900/70`) holding the transport controls and the two sequencer lines. Mobile (`<640px`) shrinks the drum's viewport-relative width, scales the bell down (`scale-75`) rather than reflowing its internal layout, and steps typography down one notch; nothing reflows to multiple columns at any width.

## Elevation & Depth

Mostly flat. The panel is a single flat surface with a 1px border, no shadow. Depth is spent in exactly two places:
1. **The drum illustration** — layered CSS gradients (wood shell, hide, tension rings) plus one soft drop-shadow (`drop-shadow-[0_20px_40px_rgba(0,0,0,0.55)]`) beneath the whole assembly. This is the system's one genuinely three-dimensional object.
2. **The Play/Pause button's glow** — see The One Glow Rule above.

### Shadow Vocabulary
- **Drum grounding shadow** (`drop-shadow(0 20px 40px rgba(0,0,0,0.55))`): under the entire drum illustration, seating it on the stage.
- **Primary-action glow** (`0 8px 24px rgba(56,189,248,0.35)`, brightening to `0.5` on hover): the Play/Pause button only.
- **Playhead glow** (`0 0 12px rgba(56,189,248,0.9)`): the thin vertical bar sweeping across the two step lines.

### Named Rules
**The Flat-Panel Rule.** The control panel and everything inside it (sliders, dots, labels) render flat at rest. Depth is never added to signal "this is a container" — only the two exceptions above earn a shadow.

## Shapes

Circles and pills dominate: the Play/Pause button, every step dot, every sound-picker swatch, the mute icon button, and the drum illustration itself are all fully rounded (`rounded-full` or `rounded-[50%]`), echoing the drum's own circular form. The one rectangular container is the panel (`rounded-xl`, 12px). The keyboard-shortcut `kbd` chips are the single place a right-angled shape appears on purpose — small rounded rectangles (`rounded`, ~6px) — reading deliberately as "a physical key," distinct from every musical control's circular language.

## Components

### Buttons
- **Shape:** full pill (`rounded-full`).
- **Primary (Play/Pause):** `h-16`, solid Bass Sky fill, near-black (`slate-950`) text/icon for maximum contrast, primary-action glow shadow, `hover:bg-sky-300`, `active:scale-[0.97]` for tactile press feedback. This is the only button in the system and it is unambiguously the main action.
- **Icon-only (mute):** no fill, no border; just an icon that shifts from muted slate to bright white/slate-200 when active. No visible focus ring style beyond the browser default — a candidate to revisit if keyboard-only mute toggling becomes a priority.

### Step Dots (Sequencer Line)
- **On:** filled circle (`h-6 w-6`) in the line's current sound color, with a soft shadow.
- **Off:** hollow ring (`h-5 w-5`, `border-2 border-slate-700`, `bg-slate-900`).
- **Current/playhead step:** scales to 1.25× with a white ring overlay, regardless of on/off state.
- **Track:** a thin (`h-1`) `slate-800` line connecting all steps.

### Sound Picker (per line)
- Row of small solid-color dots, one per sound (Bass/Tone/Click), no text labels — just a `title` tooltip and an `aria-label` per button.
- **Active:** `h-3 w-3`, full opacity. **Inactive:** `h-2 w-2`, 35% opacity.

### Step-Count / Tempo Numeral
- The line's step count sits directly beside the slider that changes it, in the line's sound color, at `text-4xl`–`text-5xl` — the single largest read-out in the UI, since the polyrhythm (e.g. "3 against 4") is the product's core idea.
- The BPM readout follows the same numeral treatment at a smaller size (`text-xl`), with its "BPM" unit in the Label style beside it.

### Kbd Key (keyboard-shortcut hint)
- Small rounded rectangle, `wood-neutral-surface` background at 60% opacity, `wood-neutral-border` border, `wood-neutral-strong` text — reads as a physical keycap, used only in the header caption beneath the title.

### Drum Canvas (signature component)
- The system's one fully custom illustration: layered gradient `div`s simulating a wood-and-hide frame drum, two independently animated mallets (Web Animations API), and color-coded ripple strikes.
- Fully keyboard-operable: `Q`/`W`/`E` and `I`/`O`/`P` map to the left/right mallet's Click/Tone/Bass zones (outer keys → rim Click, inner keys → center Bass, mirroring the qwerty row's own left-right symmetry); arrows are a quick single-key Tone strike per side.
- **A real `<button>`, not a `div[role="button"]`:** that's what makes the focus ring (plain Tailwind `focus-visible:`) correct for free — no ring on a plain click or on the programmatic focus the Play button hands the drum via a mouse click to start playback, but a ring on Tab-focus *and* on that same auto-focus handoff when Play was itself reached and activated by keyboard, since native `:focus-visible` tracks input modality across a scripted `.focus()` call in a way a hand-rolled ARIA-button div cannot.

## Do's and Don'ts

### Do:
- **Do** keep Bass Sky, Tone Amber, and Click Ember meaning exactly one thing each (Bass/Tone/Click) everywhere they appear.
- **Do** keep Fraunces confined to the title and the app's headline numerals (step counts, BPM); everything else stays in the system sans.
- **Do** make new interactive controls circular or pill-shaped unless they represent a physical key (kbd) or the one flat rectangular panel container.
- **Do** keep the drum illustration a real `<button>` — a `div[role="button"]` loses correct `:focus-visible` behavior (it can show the ring on a plain click in some engines, or lose it on a legitimate keyboard-driven focus), which a native button gets right automatically.

### Don't:
- **Don't** add a second colored glow shadow anywhere; the Play/Pause button is the one lit control.
- **Don't** add a second illustrated/skeuomorphic element — the drum is the system's one dimensional object, everything else stays flat.
- **Don't** use `stone` (warm neutral) outside the header/keyboard-hint area, or `slate` (cool neutral) inside it — the two neutral families are currently segregated by zone, not mixed freely.
- **Don't** widen the sound-color palette past three; a fourth accent would break The Three-Sound Rule's information mapping.

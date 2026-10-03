---
name: FrameBeat
description: A frame drum and polyrhythmic step sequencer staged like a real instrument under a single light, in an otherwise flat, dark control surface.
colors:
  stage: "#020617"
  panel: "#0f172a"
  panel-border: "#1e293b"
  ink: "#f1f5f9"
  label-muted: "#64748b"
  label: "#94a3b8"
  control-text: "#cbd5e1"
  control-border: "#64748b"
  control-fill-pressed: "#334155"
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
- **Label Muted** (`#64748b`, `slate-500`): micro-labels on the stage (the Bass/Tone/Click legend, the "Bell" caption), where they sit on the near-black background as quiet captions.
- **Label** (`#94a3b8`, `slate-400`): every uppercase micro-label inside the panel (TOP LINE, TEMPO) and the BPM unit. The panel is where people operate, so its labels must pass 4.5:1.
- **Control Text / Border / Pressed Fill** (`slate-300` / `slate-500` / `slate-700`): the toggle pills and sound switch. Borders stay at least 3:1 against the panel so every control's edge is visible at rest.
- **Wood Neutral** (`#78716c`–`#d6d3d1`, `stone-500`–`stone-300`): reserved for the heading's italic ampersand and the keyboard-shortcut kbd chips (in the header caption and under the bell) — a warm neutral distinct from the cooler `slate` used everywhere else, tying that one area back to the drum's own wood tones.

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

Single centered column, `max-w-3xl`, generous vertical rhythm (`pt-6 pb-8` outer, `sm:pt-14 sm:pb-10`; `gap-2` between the drum and bell, `sm:gap-10`). The page has two zones stacked vertically: an untethered "stage" zone (title, caption, drum, bell, sound legend) sitting directly on the dark background with a soft radial glow behind it, and a bordered flat "panel" zone (`rounded-xl border border-slate-800 bg-slate-900/70`) holding the transport controls and the two sequencer lines. Mobile (`<640px`) shrinks the drum's viewport-relative width, scales the bell down (`zoom: 0.94`, so its layout box shrinks too) rather than reflowing its internal layout, and steps typography down one notch; nothing reflows to multiple columns at any width.

### Phone and Tablet (native)

This is the contract for the Android app, phone and tablet, and for a later iPhone port. It is one composition at two sizes, not two layouts. The shipped iPad app predates it: its compact-width path (Slide Over) keeps a fixed 340pt drum, and changing that is out of scope here.

The breakpoint is 600dp, Android's compact/medium window-size boundary, not the web's 640px. The web's below-640px rules (the sound picker on its own row, Mute as an icon only) apply below 600dp here. Sizes are in dp (Android) and pt (iOS), which match CSS px. The web values below were measured in Chrome at the listed viewport widths.

**Compact width (under 600dp): one scrolling column.**
- Top to bottom: the title "FrameBeat", the caption "Tap the drum or the bell to play", the drum and bell in one row, then the panel. The column scrolls vertically. Nothing in it scrolls sideways.
- Side gutter 16dp, matching the web's `px-4`.
- **The drum gets the width first; the bell shrinks.** The drum is the instrument with three zones to hit; the bell is one target, and it stays easy to tap at a smaller size. This differs from the web phone layout, which gives the drum 60% of the viewport and keeps the bell at 0.94.
- **Bell:** scale between 0.5 and 0.94 of its 112 × 136dp base art (native `BellView`; the web's box is 112 × 138), so between about 56 × 68dp and 105 × 128dp. Its floor stays above a 48dp touch target. These sizes are the art alone; the 11sp "Bell" label sits below it, as on iPad, with no key chip, and the row centers the drum on the art and label together.
- **Drum:** the row width minus the 8dp gap and the bell at its 0.5 floor, capped at 340dp, the same size as on expanded widths, so the drum never shrinks when the window widens past 600dp. Height is width × 30/32. That gives 264dp at a 360dp window, 294dp at 390, and 316dp at 412. Once the drum reaches 340dp, at a 436dp window, the bell takes the extra width, up to 0.94. The drum and bell are 8dp apart, and the pair is centered. The row never scrolls sideways.
- **Zones:** at 264dp the Bass zone is about 69 × 44dp, and the Tone band is about 51dp wide at the sides and 33dp tall above and below. From a 390dp window up, the drum is at least 290dp and the Bass zone is at least 48dp tall. On the web phone layout today, the 216dp drum gives a Bass zone of about 56 × 36dp.
- **Under 360dp:** the same rule applies: the bell stays at its 0.5 floor and the drum takes the rest, 224dp at a 320dp window.
- Title at 36sp (`text-4xl`, as the web header draws it at this width), the caption at 12sp in `stone-500`, as on the web header (stone belongs to the header zone).
- **Panel:** full column width, 16dp padding (`p-4`). The sound picker is a full-width row of its own, as specified for below 640px under **Sound Picker**.

**Expanded width (600dp and up):** the same regions in the same order.
- Column centered, up to 768dp wide (`max-w-3xl`), with a 24dp side gutter. Title at 48sp (`sm:text-5xl`), caption at 14sp (`sm:text-sm`).
- **Drum:** 340dp, the iPad stage.
- **Bell:** full scale, 1.625 × the base art, about 182 × 221dp. The drum and bell sit 24dp apart, matching the iPad. At 600dp the row is 546dp plus 48dp of gutters, so it fits.
- **Panel:** 24dp padding (`sm:p-6`).
- **Short windows:** when the window is too short for the full stage above a full-size panel, scale the drum, bell, and stage glow down together, to a floor of 0.45. The panel keeps its full size. This is the `stageScale` rule in iPad's `ContentView.swift`. Unlike iPad, which has no scroll view, the column scrolls when even the floor does not fit.
- Do not invent a second composition for tablets. There are no side-by-side columns and no panel beside the drum.

**Rules that still apply:** the Named Rules above, unchanged: The Three-Sound Rule, The One Display Face Rule (title and the step-count and BPM numerals), Play as the only colored glow, and the flat panel (a 1px border, no shadow).

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
- **Toggle pill (Mute, Chime on 1):** `h-9` bordered pill, icon plus a text label (Mute collapses to icon-only below 640px, keeping its border and 36px target). At rest: `slate-500` border, `slate-300` text; pressed: `slate-700` fill, `slate-400` border, white text, `aria-pressed`. Never an accent color — these are settings, not sounds.
- **Every panel control** has a visible edge at rest, a readable label, a hit target of at least 32px, and a `sky-400` focus-visible ring.

### Step Dots (Sequencer Line)
- **On:** filled circle (`h-6 w-6`) in the line's current sound color, with a soft shadow.
- **Off:** hollow ring (`h-5 w-5`, `border-2 border-slate-400`, `bg-slate-900`), brightening to white on hover; the hit area extends 8px past the drawn dot.
- **Current/playhead step:** scales to 1.25× with a white ring overlay, regardless of on/off state.
- **Track:** a thin (`h-1`) `slate-700` line connecting all steps.

### Sound Picker (per line)
- A labelled segmented pill: Bass | Tone | Click, each with its color dot, `h-8` segments inside a `slate-600`-bordered track. Full width on its own row below 640px.
- **Active:** `slate-800` fill, an inset ring and text in that sound's color (the color is the sound, so this is its one legitimate use here). **Inactive:** `slate-300` text, brightening on hover.

### Step-Count / Tempo Numeral
- The line's step count sits directly beside the slider that changes it, in the line's sound color, at `text-4xl`–`text-5xl` — the single largest read-out in the UI, since the polyrhythm (e.g. "3 against 4") is the product's core idea.
- The BPM readout follows the same numeral treatment at a smaller size (`text-xl`), with its "BPM" unit in the Label style beside it.

### Kbd Key (keyboard-shortcut hint)
- Small rounded rectangle, `wood-neutral-surface` background at 60% opacity, `wood-neutral-border` border, `wood-neutral-strong` text — reads as a physical keycap, used only in the header caption beneath the title and the "Bell" caption (a smaller size there).

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

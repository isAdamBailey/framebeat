// The instrument's starting state, control ranges, and keyboard shortcuts,
// shared by the components and the spec/ tests.
import type { Line, Sound } from '../types/drum'

export const MAX_STEPS = 16

export const DEFAULTS = {
  bpm: 90,
  top: { count: 3, sound: 'edge' },
  bottom: { count: 4, sound: 'bass' },
  chimeOnOne: true,
} as const

export const RANGES = {
  bpm: { min: 40, max: 200 },
  count: { min: 1, max: MAX_STEPS },
} as const

// A fresh line: every step filled in, not muted.
export function defaultLine(line: 'top' | 'bottom'): Line {
  const { count, sound } = DEFAULTS[line]
  return { count, sound, dots: Array.from({ length: MAX_STEPS }, () => true), muted: false }
}

// Q W E / I O P mirror the qwerty row's own left-right symmetry outward-in:
// Q and P sit at the outer ends, so they play the rim Click zone; E and I
// sit innermost (closest to the row's centre), so they play the centre
// Bass zone; W and O in between play Tone — matching the drum's own
// centre-to-rim zone layout. The arrows are a quick tone strike per side.
export const DRUM_KEYS: Partial<Record<string, { side: 'left' | 'right'; sound: Sound }>> = {
  q: { side: 'left', sound: 'click' },
  w: { side: 'left', sound: 'edge' },
  e: { side: 'left', sound: 'bass' },
  arrowleft: { side: 'left', sound: 'edge' },
  i: { side: 'right', sound: 'bass' },
  o: { side: 'right', sound: 'edge' },
  p: { side: 'right', sound: 'click' },
  arrowright: { side: 'right', sound: 'edge' },
}

export type InstrumentShortcut = 'playPause' | 'bell'

// Space plays/pauses; B rings the bell. Key repeat and Ctrl, Alt, or Meta
// chords are ignored for both.
export function instrumentShortcut(e: {
  key: string
  repeat: boolean
  metaKey: boolean
  ctrlKey: boolean
  altKey: boolean
}): InstrumentShortcut | null {
  if (e.repeat || e.metaKey || e.ctrlKey || e.altKey) return null
  if (e.key === ' ') return 'playPause'
  if (e.key.toLowerCase() === 'b') return 'bell'
  return null
}

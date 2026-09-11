import type { Sound } from '../types/drum'

export const SOUND_META: Record<Sound, { label: string; dot: string; text: string; ripple: string }> = {
  bass: {
    label: 'Bass',
    dot: 'bg-sky-500',
    text: 'text-sky-400',
    ripple: 'rgba(56,189,248,0.55)',
  },
  edge: {
    label: 'Tone',
    dot: 'bg-amber-500',
    text: 'text-amber-400',
    ripple: 'rgba(251,191,36,0.55)',
  },
  click: {
    label: 'Click',
    dot: 'bg-orange-500',
    text: 'text-orange-400',
    ripple: 'rgba(249,115,22,0.6)',
  },
}

export const VALID_SOUNDS: Sound[] = ['bass', 'edge', 'click']

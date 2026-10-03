// Every voice's parameters as data, so drumAudio.ts builds its nodes from
// them and the shared spec/ tests can check them. The Swift port copies
// these into VoiceSpec.swift.
import type { Sound } from '../types/drum'

// An AudioParam automation: the first point is a setValueAtTime, every later
// point an exponentialRampToValueAtTime. Times are seconds after the strike.
export type Breakpoints = readonly (readonly [time: number, value: number])[]

export interface OscillatorSpec {
  type: 'sine' | 'triangle'
  freq: Breakpoints
  gain: Breakpoints
  stop: number
}

export interface NoiseSpec {
  filter: 'lowpass' | 'bandpass' | 'highpass'
  frequency: number
  q: number // 1 is the Web Audio default
  gain: Breakpoints
  stop: number
}

export interface VoiceSpec {
  oscillators: readonly OscillatorSpec[]
  noises: readonly NoiseSpec[]
}

// Bell chime marking the start of each bar ("the one"), also struck by hand.
// A small bell's partials around an E5 strike note: a low hum an octave
// below, the minor-third tierce and the quint that give a bell its color,
// and the octave nominal. Lower partials ring longest, so it blooms and
// then settles onto the hum.
const DING_PARTIALS = [
  { freq: 329.6, gain: 0.07, dur: 1.8 },
  { freq: 659.3, gain: 0.2, dur: 1.4 },
  { freq: 790.0, gain: 0.06, dur: 0.9 },
  { freq: 988.0, gain: 0.045, dur: 0.7 },
  { freq: 1318.5, gain: 0.04, dur: 0.5 },
]

export const VOICES: Record<Sound | 'ding', VoiceSpec> = {
  // Open Bass Tone: pitch-drop sine (100 -> ~58 Hz) + lowpass-filtered noise impact
  bass: {
    oscillators: [
      {
        type: 'sine',
        freq: [[0, 100], [0.3, 58]],
        gain: [[0, 0.0001], [0.008, 0.9], [0.55, 0.0001]],
        stop: 0.6,
      },
    ],
    noises: [{ filter: 'lowpass', frequency: 350, q: 1, gain: [[0, 0.5], [0.06, 0.0001]], stop: 0.08 }],
  },
  // Open Tone: a warm, pitched hand tone pitched about an octave above the
  // bass — a sine dropping 220 -> 150 Hz, the drumhead's first overtone at
  // ~1.59x the fundamental, and a soft band-limited skin thump for the attack.
  edge: {
    oscillators: [
      {
        type: 'sine',
        freq: [[0, 220], [0.18, 150]],
        gain: [[0, 0.0001], [0.006, 0.75], [0.34, 0.0001]],
        stop: 0.38,
      },
      {
        type: 'sine',
        freq: [[0, 350], [0.12, 240]],
        gain: [[0, 0.0001], [0.004, 0.22], [0.16, 0.0001]],
        stop: 0.2,
      },
    ],
    noises: [{ filter: 'bandpass', frequency: 700, q: 0.9, gain: [[0, 0.35], [0.04, 0.0001]], stop: 0.06 }],
  },
  // Rim Click: a pronounced wooden knock for hits on the frame's side —
  // sharp high transient + a resonant woodblock body.
  click: {
    oscillators: [
      { type: 'triangle', freq: [[0, 1450], [0.07, 1050]], gain: [[0, 0.55], [0.09, 0.0001]], stop: 0.1 },
      { type: 'sine', freq: [[0, 2400]], gain: [[0, 0.25], [0.035, 0.0001]], stop: 0.04 },
    ],
    noises: [{ filter: 'highpass', frequency: 2200, q: 1, gain: [[0, 1.0], [0.03, 0.0001]], stop: 0.05 }],
  },
  ding: {
    oscillators: DING_PARTIALS.map(({ freq, gain, dur }) => ({
      type: 'sine' as const,
      freq: [[0, freq]] as const,
      gain: [[0, 0.0001], [0.005, gain], [dur, 0.0001]] as const,
      stop: dur + 0.05,
    })),
    noises: [],
  },
}

// The value Web Audio gives an automation `t` seconds after the strike:
// held before the first point and after the last, and between points
// v0 * (v1 / v0) ^ ((t - t0) / (t1 - t0)).
export function envelopeValue(points: Breakpoints, t: number): number {
  let [t0, v0] = points[0]
  if (t <= t0) return v0
  for (const [t1, v1] of points.slice(1)) {
    if (t <= t1) return t1 === t0 ? v1 : v0 * Math.pow(v1 / v0, (t - t0) / (t1 - t0))
    t0 = t1
    v0 = v1
  }
  return v0
}

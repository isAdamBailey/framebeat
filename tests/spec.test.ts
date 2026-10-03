// Runs the shared spec/ cases against the web app. The Swift and Android
// test suites run the same files.
import { describe, expect, test } from 'vitest'
import controls from '../spec/controls.json'
import geometry from '../spec/geometry.json'
import sequencer from '../spec/sequencer.json'
import voices from '../spec/voices.json'
import { DEFAULTS, DRUM_KEYS, RANGES, defaultLine, instrumentShortcut } from '../src/lib/controls'
import { classify, swingFor, zonePoint } from '../src/lib/geometry'
import { generateEvents, nextBarBoundary, type ScheduledEvent } from '../src/lib/schedule'
import { VOICES, envelopeValue, type Breakpoints } from '../src/lib/voiceSpec'
import type { Line, Sound } from '../src/types/drum'

const close = (actual: number, expected: number, tolerance: number) => {
  expect(Math.abs(actual - expected)).toBeLessThanOrEqual(tolerance)
}

describe('geometry', () => {
  const tol = geometry.tolerance

  test.each(geometry.classify)('classify: $note', ({ x, y, expected }) => {
    const hit = classify(x, y)
    if (!expected) {
      expect(hit).toBeNull()
      return
    }
    expect(hit).not.toBeNull()
    expect(hit?.sound).toBe(expected.sound)
    expect(hit?.side).toBe(expected.side)
    close(hit?.dx ?? NaN, expected.dx, tol)
    close(hit?.dy ?? NaN, expected.dy, tol)
  })

  test.each(geometry.zonePoint)('zonePoint $sound $side', ({ sound, side, x, y }) => {
    const [zx, zy] = zonePoint(sound as Sound, side as 'left' | 'right')
    close(zx, x, tol)
    close(zy, y, tol)
  })

  test.each(geometry.swing)('swing $side to ($gx, $gy)', ({ side, gx, gy, rotation, x, y }) => {
    const s = swingFor(side as 'left' | 'right', gx, gy)
    close(s.rot, rotation, tol)
    close(Number(s.x.slice(0, -1)) / 100, x, geometry.swingTolerance)
    close(Number(s.y.slice(0, -1)) / 100, y, geometry.swingTolerance)
  })
})

describe('voices', () => {
  type Voice = keyof typeof VOICES

  test.each(Object.keys(voices.voices))('%s parameters', (name) => {
    const actual = VOICES[name as Voice]
    expect({
      oscillators: actual.oscillators.map((o) => ({ waveform: o.type, freq: o.freq, gain: o.gain })),
      noises: actual.noises.map((n) => ({ filter: n.filter, frequency: n.frequency, q: n.q, gain: n.gain })),
    }).toEqual(voices.voices[name as Voice])
  })

  test.each(voices.envelopes)('$voice $part', ({ voice, part, samples }) => {
    const [kind, index, param] = part.split('.')
    const spec = VOICES[voice as Voice]
    let points: Breakpoints
    if (kind === 'oscillators' && (param === 'freq' || param === 'gain')) points = spec.oscillators[Number(index)][param]
    else if (kind === 'noises' && param === 'gain') points = spec.noises[Number(index)].gain
    else throw new Error(`unknown envelope part ${part}`)
    for (const { t, value } of samples) close(envelopeValue(points, t), value, voices.tolerance)
  })
})

describe('sequencer', () => {
  const rank = (e: ScheduledEvent) => (e.kind === 'bell' ? 0 : e.line === 'bottom' ? 1 : 2)

  test.each(sequencer.sequences)('$name', ({ top, bottom, bpm, bars, chimeOnOne, events }) => {
    const actual = generateEvents(top as Line, bottom as Line, bpm, bars, chimeOnOne)
    expect(actual).toHaveLength(events.length)
    actual.forEach((event, i) => {
      const expected = events[i] as ScheduledEvent
      close(event.time, expected.time, sequencer.tolerance)
      expect({ ...event, time: 0 }).toEqual({ ...expected, time: 0 })
      if (i > 0) {
        const prev = actual[i - 1]
        const tie = Math.abs(event.time - prev.time) <= sequencer.tolerance
        expect(tie ? rank(event) > rank(prev) : event.time > prev.time).toBe(true)
      }
    })
  })

  test.each(sequencer.nextBarBoundary)('next bar boundary from $anchor, bar $bar, not before $notBefore', (c) => {
    expect(nextBarBoundary(c.anchor, c.bar, c.notBefore)).toBe(c.expected)
  })
})

describe('controls', () => {
  test('defaults', () => {
    expect({ ...DEFAULTS, top: defaultLine('top'), bottom: defaultLine('bottom') }).toEqual(controls.defaults)
  })

  test('ranges', () => {
    expect(RANGES).toEqual(controls.ranges)
  })

  test('drum keys', () => {
    expect(DRUM_KEYS).toEqual(controls.drumKeys)
  })

  test.each(controls.instrumentShortcuts)('instrument shortcut $key', ({ expected, ...press }) => {
    expect(instrumentShortcut(press)).toBe(expected)
  })
})

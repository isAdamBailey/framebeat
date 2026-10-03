// The sequencer's timing as pure functions, shared by the live scheduler in
// useSequencer.ts and the spec/ tests. Swift's counterparts are
// Sequencer.swift and LiveScheduleMath.swift.
import type { Line, Sound } from '../types/drum'

// The bar's timing. Held fixed from one anchor to the next, so a BPM or
// step-count change can't reshape a bar that is already partly booked.
export interface Shape {
  beat: number
  bottomCount: number
  topCount: number
}

export const shapeFor = (bpm: number, bottomCount: number, topCount: number): Shape => ({
  beat: 60 / bpm,
  bottomCount,
  topCount,
})

export const barDuration = (shape: Shape) => shape.beat * shape.bottomCount

// The first bar boundary (anchor + k bars, k >= 0) at or after `notBefore`.
export function barsUntil(anchor: number, barDur: number, notBefore: number) {
  return Math.max(0, Math.ceil((notBefore - anchor) / barDur))
}

export function nextBarBoundary(anchor: number, barDur: number, notBefore: number) {
  return anchor + barsUntil(anchor, barDur, notBefore) * barDur
}

// Two step streams counted from one anchor. Steps are timed as
// anchor + index * duration rather than by adding onto a running total, so
// bar boundaries land exactly where a re-anchor expects.
export interface Streams {
  shape: Shape
  anchorTime: number
  bIdx: number
  tIdx: number
}

export const bottomStepTime = (s: Streams, i: number) => s.anchorTime + i * s.shape.beat
export const topStepTime = (s: Streams, i: number) => s.anchorTime + (i * barDuration(s.shape)) / s.shape.topCount

// Book every step before `horizon`, in time order, stopping each line at its
// index limit. On a tie the bottom line goes first.
export function bookSteps(
  s: Streams,
  horizon: number,
  onStep: (line: 'top' | 'bottom', index: number, time: number) => void,
  bLimit = Infinity,
  tLimit = Infinity
) {
  for (;;) {
    const bt = s.bIdx < bLimit ? bottomStepTime(s, s.bIdx) : Infinity
    const tt = s.tIdx < tLimit ? topStepTime(s, s.tIdx) : Infinity
    if (bt >= horizon && tt >= horizon) return
    if (bt <= tt) {
      onStep('bottom', s.bIdx % s.shape.bottomCount, bt)
      s.bIdx++
    } else {
      onStep('top', s.tIdx % s.shape.topCount, tt)
      s.tIdx++
    }
  }
}

export type ScheduledEvent =
  | { time: number; kind: 'bell' }
  | { time: number; kind: 'step'; line: 'top' | 'bottom'; index: number; sound: Sound; audible: boolean }

// What one booked step produces. The "one" is when both lines restart
// together (bottom step 0): the bar-marker chime rings there, unless it is
// switched off, regardless of either line's mute.
export function stepEvents(
  line: 'top' | 'bottom',
  index: number,
  time: number,
  data: Line,
  chimeOnOne: boolean
): ScheduledEvent[] {
  const step: ScheduledEvent = {
    time,
    kind: 'step',
    line,
    index,
    sound: data.sound,
    audible: data.dots[index] && !data.muted,
  }
  return line === 'bottom' && index === 0 && chimeOnOne ? [{ time, kind: 'bell' }, step] : [step]
}

// Every event of `bars` whole bars from time 0, booked the way the live
// scheduler books them. The offline equivalent of Sequencer.swift.
export function generateEvents(top: Line, bottom: Line, bpm: number, bars: number, chimeOnOne: boolean) {
  const s: Streams = { shape: shapeFor(bpm, bottom.count, top.count), anchorTime: 0, bIdx: 0, tIdx: 0 }
  const events: ScheduledEvent[] = []
  bookSteps(
    s,
    Infinity,
    (line, index, time) => events.push(...stepEvents(line, index, time, line === 'top' ? top : bottom, chimeOnOne)),
    bars * bottom.count,
    bars * top.count
  )
  return events
}

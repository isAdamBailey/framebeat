import { onUnmounted, ref, watch, type Ref } from 'vue'
import { getAudioContext, triggerDrumSound, triggerDing } from '../lib/drumAudio'
import { barDuration, barsUntil, bookSteps, nextBarBoundary, shapeFor, stepEvents, type Shape, type Streams } from '../lib/schedule'
import type { Line, Sound } from '../types/drum'

interface UseSequencerArgs {
  top: Line
  bottom: Line
  bpm: Ref<number>
  // Whether the bar-marker chime rings on "the one". Read at booking time,
  // so flipping it mid-play takes effect from the next unbooked bar.
  chimeOnOne: Ref<boolean>
  onStep: (sound: Sound, line: 'top' | 'bottom') => void
  onDing: () => void
}

type QueueEvent =
  | { time: number; line: 'bell' }
  | { time: number; line: 'top'; index: number }
  | { time: number; line: 'bottom'; index: number; shape: Shape }

// Drift-free polyrhythm scheduler: two independent step streams booked on the
// AudioContext clock. The bottom line runs at the BPM and defines the bar;
// the top line divides the same bar into its own step count, so both lines
// always start together on "one". A rAF loop syncs visuals to the same timeline.
export function useSequencer({ top, bottom, bpm, chimeOnOne, onStep, onDing }: UseSequencerArgs) {
  const playing = ref(false)
  const progress = ref(0)
  const currentTop = ref<number | null>(null)
  const currentBottom = ref<number | null>(null)

  let timer: ReturnType<typeof setInterval> | null = null
  let raf: number | null = null
  const streams: Streams = { shape: shapeFor(60, 1, 1), anchorTime: 0, bIdx: 0, tIdx: 0 }
  let queue: QueueEvent[] = []
  let reanchor = false
  let lastBottom = { time: 0, idx: 0, shape: streams.shape }

  // Restart both step streams together on a fresh bar boundary at `startTime`,
  // picking up the current BPM and step counts.
  function anchor(startTime: number) {
    streams.shape = shapeFor(bpm.value, bottom.count, top.count)
    streams.anchorTime = startTime
    streams.bIdx = 0
    streams.tIdx = 0
  }

  function scheduleStep(line: 'top' | 'bottom', idx: number, time: number) {
    for (const event of stepEvents(line, idx, time, line === 'top' ? top : bottom, chimeOnOne.value)) {
      if (event.kind === 'bell') {
        triggerDing(time)
        // Visual event so the bell animation fires when the ding is *heard*.
        queue.push({ time, line: 'bell' })
        continue
      }
      if (event.audible) triggerDrumSound(event.sound, time)
      if (line === 'bottom') queue.push({ time, line, index: idx, shape: streams.shape })
      else queue.push({ time, line, index: idx })
    }
  }

  function book(horizon: number, bLimit = Infinity, tLimit = Infinity) {
    bookSteps(streams, horizon, scheduleStep, bLimit, tLimit)
  }

  function scheduler() {
    const audioCtx = getAudioContext()
    const horizon = audioCtx.currentTime + 0.12
    if (reanchor) {
      // Everything before the previous horizon is already booked. Finish the
      // old pattern up to its next bar boundary at or after this horizon, then
      // restart both lines there under the new BPM and counts, so nothing
      // booked is dropped, doubled, or overlapped.
      reanchor = false
      const { anchorTime, shape } = streams
      const bars = barsUntil(anchorTime, barDuration(shape), horizon)
      book(Infinity, bars * shape.bottomCount, bars * shape.topCount)
      anchor(nextBarBoundary(anchorTime, barDuration(shape), horizon))
    }
    book(horizon)
  }

  function frame() {
    const audioCtx = getAudioContext()
    // A sound booked at time T is actually *heard* at T + the device's output
    // latency (worse on Bluetooth). Firing the visual pulse on the raw audio
    // clock makes dots flash ahead of the audible beat, so compare against the
    // heard time instead.
    const latency = (audioCtx.outputLatency || 0) + (audioCtx.baseLatency || 0)
    const now = audioCtx.currentTime + latency
    while (queue.length && queue[0].time <= now) {
      const event = queue.shift()!
      if (event.line === 'bell') {
        onDing()
        continue
      }
      const data = event.line === 'top' ? top : bottom
      if (event.line === 'bottom') {
        lastBottom = { time: event.time, idx: event.index, shape: event.shape }
        currentBottom.value = event.index
      } else {
        currentTop.value = event.index
      }
      if (data.dots[event.index] && !data.muted) {
        onStep(data.sound, event.line)
      }
    }
    // Use the shape of the step being heard, which can lag the one being booked.
    const { beat, bottomCount } = lastBottom.shape
    const frac = Math.max(0, Math.min((now - lastBottom.time) / beat, 1))
    progress.value = ((lastBottom.idx + frac) % bottomCount) / bottomCount
    raf = requestAnimationFrame(frame)
  }

  function stop() {
    if (timer) clearInterval(timer)
    if (raf) cancelAnimationFrame(raf)
    timer = null
    raf = null
    queue = []
    playing.value = false
    currentTop.value = null
    currentBottom.value = null
    progress.value = 0
  }

  function start() {
    const audioCtx = getAudioContext()
    anchor(audioCtx.currentTime + 0.1)
    lastBottom = { time: streams.anchorTime, idx: 0, shape: streams.shape }
    reanchor = false
    queue = []
    scheduler()
    timer = setInterval(scheduler, 25)
    raf = requestAnimationFrame(frame)
    playing.value = true
  }

  function togglePlay() {
    if (playing.value) stop()
    else start()
  }

  // Re-anchor both streams on the next bar when timing params change mid-play,
  // so the two lines always restart together on "one".
  watch([bpm, () => top.count, () => bottom.count], () => {
    if (playing.value) reanchor = true
  })

  onUnmounted(() => {
    if (timer) clearInterval(timer)
    if (raf) cancelAnimationFrame(raf)
  })

  return { playing, togglePlay, progress, currentTop, currentBottom }
}

import { onUnmounted, ref, watch, type Ref } from 'vue'
import { getAudioContext, triggerDrumSound, triggerDing } from '../lib/drumAudio'
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

// The bar's timing. Held fixed from one anchor to the next, so a BPM or
// step-count change can't reshape a bar that is already partly booked.
interface Shape {
  beat: number
  bottomCount: number
  topCount: number
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
  let shape: Shape = { beat: 1, bottomCount: 1, topCount: 1 }
  // Steps are timed as anchor + index * duration rather than by adding onto
  // a running total, so bar boundaries land exactly where re-anchor expects.
  let anchorTime = 0
  let bIdx = 0
  let tIdx = 0
  let queue: QueueEvent[] = []
  let reanchor = false
  let lastBottom = { time: 0, idx: 0, shape }

  const readShape = (): Shape => ({ beat: 60 / bpm.value, bottomCount: bottom.count, topCount: top.count })
  const barDur = () => shape.beat * shape.bottomCount
  const bottomTime = (i: number) => anchorTime + i * shape.beat
  const topTime = (i: number) => anchorTime + (i * barDur()) / shape.topCount

  // Restart both step streams together on a fresh bar boundary at `startTime`,
  // picking up the current BPM and step counts.
  function anchor(startTime: number) {
    shape = readShape()
    anchorTime = startTime
    bIdx = 0
    tIdx = 0
  }

  function scheduleStep(line: 'top' | 'bottom', idx: number, time: number) {
    const data = line === 'top' ? top : bottom
    // The "one" is when both lines restart together (bottom step 0) — ring the
    // bar-marker chime there (unless switched off), regardless of line mutes.
    if (line === 'bottom' && idx === 0 && chimeOnOne.value) {
      triggerDing(time)
      // Visual event so the bell animation fires when the ding is *heard*.
      queue.push({ time, line: 'bell' })
    }
    if (data.dots[idx] && !data.muted) triggerDrumSound(data.sound, time)
    if (line === 'bottom') queue.push({ time, line, index: idx, shape })
    else queue.push({ time, line, index: idx })
  }

  // Book every step before `horizon`, stopping each line at its index limit.
  function book(horizon: number, bLimit = Infinity, tLimit = Infinity) {
    for (;;) {
      const bt = bIdx < bLimit ? bottomTime(bIdx) : Infinity
      const tt = tIdx < tLimit ? topTime(tIdx) : Infinity
      if (bt >= horizon && tt >= horizon) return
      if (bt <= tt) {
        scheduleStep('bottom', bIdx % shape.bottomCount, bt)
        bIdx++
      } else {
        scheduleStep('top', tIdx % shape.topCount, tt)
        tIdx++
      }
    }
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
      const bars = Math.max(0, Math.ceil((horizon - anchorTime) / barDur()))
      book(Infinity, bars * shape.bottomCount, bars * shape.topCount)
      anchor(anchorTime + bars * barDur())
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
    lastBottom = { time: anchorTime, idx: 0, shape }
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

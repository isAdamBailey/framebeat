import { onUnmounted, ref, watch, type Ref } from 'vue'
import { getAudioContext, triggerDrumSound, triggerDing } from '../lib/drumAudio'
import type { Line, Sound } from '../types/drum'

interface UseSequencerArgs {
  top: Line
  bottom: Line
  bpm: Ref<number>
  onStep: (sound: Sound, line: 'top' | 'bottom') => void
  onDing: () => void
}

interface Stream {
  next: number
  idx: number
}

type QueueEvent =
  | { time: number; line: 'bell' }
  | { time: number; line: 'top' | 'bottom'; index: number }

// Drift-free polyrhythm scheduler: two independent step streams booked on the
// AudioContext clock. The bottom line runs at the BPM and defines the bar;
// the top line divides the same bar into its own step count, so both lines
// always start together on "one". A rAF loop syncs visuals to the same timeline.
export function useSequencer({ top, bottom, bpm, onStep, onDing }: UseSequencerArgs) {
  const playing = ref(false)
  const progress = ref(0)
  const currentTop = ref<number | null>(null)
  const currentBottom = ref<number | null>(null)

  let timer: ReturnType<typeof setInterval> | null = null
  let raf: number | null = null
  let bStream: Stream = { next: 0, idx: 0 }
  let tStream: Stream = { next: 0, idx: 0 }
  let queue: QueueEvent[] = []
  let reanchor = false
  let lastBottom = { time: 0, idx: 0 }

  const beatDur = () => 60 / bpm.value
  const barDur = () => beatDur() * bottom.count
  const topStepDur = () => barDur() / top.count

  // Restart both step streams together on a fresh bar boundary at `startTime`.
  function anchor(startTime: number) {
    bStream = { next: startTime, idx: 0 }
    tStream = { next: startTime, idx: 0 }
    lastBottom = { time: startTime, idx: 0 }
  }

  function scheduleStep(line: 'top' | 'bottom', idx: number, time: number) {
    const data = line === 'top' ? top : bottom
    // The "one" is when both lines restart together (bottom step 0) — ring the
    // bar-marker chime there, regardless of line mutes.
    if (line === 'bottom' && idx === 0) {
      triggerDing(time)
      // Visual event so the bell animation fires when the ding is *heard*.
      queue.push({ time, line: 'bell' })
    }
    if (data.dots[idx] && !data.muted) triggerDrumSound(data.sound, time)
    queue.push({ time, line, index: idx })
  }

  function scheduler() {
    const audioCtx = getAudioContext()
    if (reanchor) {
      reanchor = false
      anchor(audioCtx.currentTime + 0.05)
    }
    const horizon = audioCtx.currentTime + 0.12
    const bd = beatDur()
    const tsd = topStepDur()
    while (bStream.next < horizon || tStream.next < horizon) {
      if (bStream.next <= tStream.next) {
        scheduleStep('bottom', bStream.idx, bStream.next)
        bStream.idx = (bStream.idx + 1) % bottom.count
        bStream.next += bd
      } else {
        scheduleStep('top', tStream.idx, tStream.next)
        tStream.idx = (tStream.idx + 1) % top.count
        tStream.next += tsd
      }
    }
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
        lastBottom = { time: event.time, idx: event.index }
        currentBottom.value = event.index
      } else {
        currentTop.value = event.index
      }
      if (data.dots[event.index] && !data.muted) {
        onStep(data.sound, event.line)
      }
    }
    const frac = Math.max(0, Math.min((now - lastBottom.time) / beatDur(), 1))
    progress.value = ((lastBottom.idx + frac) % bottom.count) / bottom.count
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

  // Re-anchor both streams to a fresh bar when timing params change mid-play,
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

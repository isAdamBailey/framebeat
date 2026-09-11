// Pure Web Audio API drum synthesis engine — no external assets.
import type { Sound } from '../types/drum'

let ctx: AudioContext | null = null
let masterGain: GainNode | null = null
let noiseBuffer: AudioBuffer | null = null
const listeners = new Set<(state: AudioContextState | null) => void>()

function notify() {
  listeners.forEach((cb) => {
    cb(ctx ? ctx.state : null)
  })
}

// Subscribe to audio-context state changes (returns an unsubscribe function).
export function subscribeAudioState(cb: (state: AudioContextState | null) => void) {
  listeners.add(cb)
  return () => listeners.delete(cb)
}

export function isAudioBlocked() {
  return !!ctx && ctx.state !== 'running'
}

// A tiny silent buffer: some browsers (notably iOS Safari) only fully unlock
// audio playback if a buffer source is started inside the user gesture.
function unlockSilent(audioCtx: AudioContext) {
  const buffer = audioCtx.createBuffer(1, 1, audioCtx.sampleRate)
  const source = audioCtx.createBufferSource()
  source.buffer = buffer
  source.connect(audioCtx.destination)
  source.start(0)
}

export function getAudioContext(): AudioContext {
  if (!ctx) {
    // The vendor-prefixed webkitAudioContext hasn't been needed since Safari
    // 14.1 (2021); every currently-supported browser exposes AudioContext.
    ctx = new AudioContext()
    masterGain = ctx.createGain()
    masterGain.gain.value = 0.85
    const compressor = ctx.createDynamicsCompressor()
    masterGain.connect(compressor)
    compressor.connect(ctx.destination)
    ctx.onstatechange = notify
    unlockSilent(ctx)
    notify()
  }
  if (ctx.state === 'suspended') {
    ctx.resume().then(notify).catch(notify)
  }
  return ctx
}

function getNoiseBuffer(audioCtx: AudioContext): AudioBuffer {
  if (!noiseBuffer) {
    const length = audioCtx.sampleRate
    noiseBuffer = audioCtx.createBuffer(1, length, audioCtx.sampleRate)
    const data = noiseBuffer.getChannelData(0)
    for (let i = 0; i < length; i++) data[i] = Math.random() * 2 - 1
  }
  return noiseBuffer
}

function noiseSource(audioCtx: AudioContext): AudioBufferSourceNode {
  const src = audioCtx.createBufferSource()
  src.buffer = getNoiseBuffer(audioCtx)
  src.loop = true
  return src
}

// Open Bass Tone: pitch-drop sine (100 -> ~58 Hz) + lowpass-filtered noise impact
function playOpenBass(audioCtx: AudioContext, t: number) {
  const osc = audioCtx.createOscillator()
  osc.type = 'sine'
  osc.frequency.setValueAtTime(100, t)
  osc.frequency.exponentialRampToValueAtTime(58, t + 0.3)

  const gain = audioCtx.createGain()
  gain.gain.setValueAtTime(0.0001, t)
  gain.gain.exponentialRampToValueAtTime(0.9, t + 0.008)
  gain.gain.exponentialRampToValueAtTime(0.0001, t + 0.55)

  osc.connect(gain)
  gain.connect(masterGain!)
  osc.start(t)
  osc.stop(t + 0.6)

  const noise = noiseSource(audioCtx)
  const lowpass = audioCtx.createBiquadFilter()
  lowpass.type = 'lowpass'
  lowpass.frequency.value = 350
  const noiseGain = audioCtx.createGain()
  noiseGain.gain.setValueAtTime(0.5, t)
  noiseGain.gain.exponentialRampToValueAtTime(0.0001, t + 0.06)

  noise.connect(lowpass)
  lowpass.connect(noiseGain)
  noiseGain.connect(masterGain!)
  noise.start(t)
  noise.stop(t + 0.08)
}

// Edge Slap: high-frequency bandpass noise burst + short tight pitch envelope
function playEdgeSlap(audioCtx: AudioContext, t: number) {
  const noise = noiseSource(audioCtx)
  const bandpass = audioCtx.createBiquadFilter()
  bandpass.type = 'bandpass'
  bandpass.frequency.value = 2600
  bandpass.Q.value = 1.4
  const noiseGain = audioCtx.createGain()
  noiseGain.gain.setValueAtTime(0.7, t)
  noiseGain.gain.exponentialRampToValueAtTime(0.0001, t + 0.09)

  noise.connect(bandpass)
  bandpass.connect(noiseGain)
  noiseGain.connect(masterGain!)
  noise.start(t)
  noise.stop(t + 0.12)

  const osc = audioCtx.createOscillator()
  osc.type = 'triangle'
  osc.frequency.setValueAtTime(420, t)
  osc.frequency.exponentialRampToValueAtTime(180, t + 0.06)
  const oscGain = audioCtx.createGain()
  oscGain.gain.setValueAtTime(0.4, t)
  oscGain.gain.exponentialRampToValueAtTime(0.0001, t + 0.12)

  osc.connect(oscGain)
  oscGain.connect(masterGain!)
  osc.start(t)
  osc.stop(t + 0.14)
}

// Rim Click: a pronounced wooden knock for hits on the frame's side —
// sharp high transient + a resonant woodblock body.
function playWoodClick(audioCtx: AudioContext, t: number) {
  const noise = noiseSource(audioCtx)
  const highpass = audioCtx.createBiquadFilter()
  highpass.type = 'highpass'
  highpass.frequency.value = 2200
  const noiseGain = audioCtx.createGain()
  noiseGain.gain.setValueAtTime(1.0, t)
  noiseGain.gain.exponentialRampToValueAtTime(0.0001, t + 0.03)

  noise.connect(highpass)
  highpass.connect(noiseGain)
  noiseGain.connect(masterGain!)
  noise.start(t)
  noise.stop(t + 0.05)

  const body = audioCtx.createOscillator()
  body.type = 'triangle'
  body.frequency.setValueAtTime(1450, t)
  body.frequency.exponentialRampToValueAtTime(1050, t + 0.07)
  const bodyGain = audioCtx.createGain()
  bodyGain.gain.setValueAtTime(0.55, t)
  bodyGain.gain.exponentialRampToValueAtTime(0.0001, t + 0.09)

  body.connect(bodyGain)
  bodyGain.connect(masterGain!)
  body.start(t)
  body.stop(t + 0.1)

  const spark = audioCtx.createOscillator()
  spark.type = 'sine'
  spark.frequency.value = 2400
  const sparkGain = audioCtx.createGain()
  sparkGain.gain.setValueAtTime(0.25, t)
  sparkGain.gain.exponentialRampToValueAtTime(0.0001, t + 0.035)

  spark.connect(sparkGain)
  sparkGain.connect(masterGain!)
  spark.start(t)
  spark.stop(t + 0.04)
}

// Soft bell chime marking the start of each bar ("the one").
// Layered inharmonic sine partials with staggered decays = gentle ding.
function playDing(audioCtx: AudioContext, t: number) {
  const partials = [
    { freq: 1318.5, gain: 0.15, dur: 1.1 },
    { freq: 1975.5, gain: 0.06, dur: 0.8 },
    { freq: 2637.0, gain: 0.035, dur: 0.6 },
  ]
  partials.forEach(({ freq, gain, dur }) => {
    const osc = audioCtx.createOscillator()
    osc.type = 'sine'
    osc.frequency.value = freq
    const g = audioCtx.createGain()
    g.gain.setValueAtTime(0.0001, t)
    g.gain.exponentialRampToValueAtTime(gain, t + 0.005)
    g.gain.exponentialRampToValueAtTime(0.0001, t + dur)
    osc.connect(g)
    g.connect(masterGain!)
    osc.start(t)
    osc.stop(t + dur + 0.05)
  })
}

export function triggerDing(when: number) {
  const audioCtx = getAudioContext()
  const t = Math.max(when, audioCtx.currentTime)
  playDing(audioCtx, t)
}

export function triggerDrumSound(type: Sound, when: number) {
  const audioCtx = getAudioContext()
  const t = Math.max(when, audioCtx.currentTime)
  if (type === 'edge') playEdgeSlap(audioCtx, t)
  else if (type === 'click') playWoodClick(audioCtx, t)
  else playOpenBass(audioCtx, t)
}

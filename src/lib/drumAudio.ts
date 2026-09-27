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

// Open Tone: a warm, pitched hand tone pitched about an octave above the
// bass — a sine dropping 220 -> 150 Hz, the drumhead's first overtone at
// ~1.59x the fundamental, and a soft band-limited skin thump for the attack.
function playOpenTone(audioCtx: AudioContext, t: number) {
  const osc = audioCtx.createOscillator()
  osc.type = 'sine'
  osc.frequency.setValueAtTime(220, t)
  osc.frequency.exponentialRampToValueAtTime(150, t + 0.18)
  const gain = audioCtx.createGain()
  gain.gain.setValueAtTime(0.0001, t)
  gain.gain.exponentialRampToValueAtTime(0.75, t + 0.006)
  gain.gain.exponentialRampToValueAtTime(0.0001, t + 0.34)

  osc.connect(gain)
  gain.connect(masterGain!)
  osc.start(t)
  osc.stop(t + 0.38)

  const overtone = audioCtx.createOscillator()
  overtone.type = 'sine'
  overtone.frequency.setValueAtTime(350, t)
  overtone.frequency.exponentialRampToValueAtTime(240, t + 0.12)
  const overtoneGain = audioCtx.createGain()
  overtoneGain.gain.setValueAtTime(0.0001, t)
  overtoneGain.gain.exponentialRampToValueAtTime(0.22, t + 0.004)
  overtoneGain.gain.exponentialRampToValueAtTime(0.0001, t + 0.16)

  overtone.connect(overtoneGain)
  overtoneGain.connect(masterGain!)
  overtone.start(t)
  overtone.stop(t + 0.2)

  const noise = noiseSource(audioCtx)
  const bandpass = audioCtx.createBiquadFilter()
  bandpass.type = 'bandpass'
  bandpass.frequency.value = 700
  bandpass.Q.value = 0.9
  const noiseGain = audioCtx.createGain()
  noiseGain.gain.setValueAtTime(0.35, t)
  noiseGain.gain.exponentialRampToValueAtTime(0.0001, t + 0.04)

  noise.connect(bandpass)
  bandpass.connect(noiseGain)
  noiseGain.connect(masterGain!)
  noise.start(t)
  noise.stop(t + 0.06)
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

// Bell chime marking the start of each bar ("the one"), also struck by hand.
// A small bell's partials around an E5 strike note: a low hum an octave
// below, the minor-third tierce and the quint that give a bell its color,
// and the octave nominal. Lower partials ring longest, so it blooms and
// then settles onto the hum.
function playDing(audioCtx: AudioContext, t: number) {
  const partials = [
    { freq: 329.6, gain: 0.07, dur: 1.8 },
    { freq: 659.3, gain: 0.2, dur: 1.4 },
    { freq: 790.0, gain: 0.06, dur: 0.9 },
    { freq: 988.0, gain: 0.045, dur: 0.7 },
    { freq: 1318.5, gain: 0.04, dur: 0.5 },
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
  if (type === 'edge') playOpenTone(audioCtx, t)
  else if (type === 'click') playWoodClick(audioCtx, t)
  else playOpenBass(audioCtx, t)
}

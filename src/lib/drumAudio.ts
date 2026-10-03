// Pure Web Audio API drum synthesis engine — no external assets.
import type { Sound } from '../types/drum'
import { VOICES, type Breakpoints, type VoiceSpec } from './voiceSpec'

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

function automate(param: AudioParam, points: Breakpoints, t: number) {
  const [[t0, v0], ...ramps] = points
  param.setValueAtTime(v0, t + t0)
  for (const [time, value] of ramps) param.exponentialRampToValueAtTime(value, t + time)
}

// Builds one voice from its spec in voiceSpec.ts: each oscillator through its
// own gain envelope, and each band of looped noise through a filter and gain.
function playVoice(audioCtx: AudioContext, voice: VoiceSpec, t: number) {
  for (const spec of voice.oscillators) {
    const osc = audioCtx.createOscillator()
    osc.type = spec.type
    automate(osc.frequency, spec.freq, t)
    const gain = audioCtx.createGain()
    automate(gain.gain, spec.gain, t)
    osc.connect(gain)
    gain.connect(masterGain!)
    osc.start(t)
    osc.stop(t + spec.stop)
  }
  for (const spec of voice.noises) {
    const noise = noiseSource(audioCtx)
    const filter = audioCtx.createBiquadFilter()
    filter.type = spec.filter
    filter.frequency.value = spec.frequency
    filter.Q.value = spec.q
    const gain = audioCtx.createGain()
    automate(gain.gain, spec.gain, t)
    noise.connect(filter)
    filter.connect(gain)
    gain.connect(masterGain!)
    noise.start(t)
    noise.stop(t + spec.stop)
  }
}

export function triggerDing(when: number) {
  const audioCtx = getAudioContext()
  const t = Math.max(when, audioCtx.currentTime)
  playVoice(audioCtx, VOICES.ding, t)
}

export function triggerDrumSound(type: Sound, when: number) {
  const audioCtx = getAudioContext()
  const t = Math.max(when, audioCtx.currentTime)
  playVoice(audioCtx, VOICES[type], t)
}

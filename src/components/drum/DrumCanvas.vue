<script setup lang="ts">
import { reactive, watch } from 'vue'
import { triggerDrumSound } from '../../lib/drumAudio'
import { SOUND_META } from '../../lib/drumSounds'
import { DRUM, swingFor, zonePoint } from '../../lib/geometry'
import Mallet from './Mallet.vue'
import SoundDot from './SoundDot.vue'
import type { Ripple, Strikes, Swing } from '../../types/drum'

// Must match the CSS transition duration on .ripple-enter-active below.
const RIPPLE_DURATION_MS = 700

const props = defineProps<{ strikes: Strikes }>()

const ripples = reactive<Ripple[]>([])
// One mallet per sequencer line: top line plays the left mallet,
// bottom line the right one.
const rest: Swing = { id: 0, rot: 0, x: '0%', y: '0%' }
const swings = reactive<{ top: Swing; bottom: Swing }>({ top: { ...rest }, bottom: { ...rest } })
let idCounter = 0

function addRipple(x: number, y: number, color: string) {
  const id = ++idCounter
  ripples.push({ id, x, y, color })
  setTimeout(() => removeRipple(id), RIPPLE_DURATION_MS)
}

function removeRipple(id: number) {
  const idx = ripples.findIndex((r) => r.id === id)
  if (idx !== -1) ripples.splice(idx, 1)
}

// Ripple + aimed mallet swing for a strike at (dx, dy) in drum-ellipse units.
function visualStrike(sound: keyof typeof SOUND_META, dx: number, dy: number, line: 'top' | 'bottom') {
  addRipple(dx, dy, SOUND_META[sound].ripple)
  const side = line === 'top' ? 'left' : 'right'
  swings[line] = { id: ++idCounter, ...swingFor(side, dx, dy) }
}

function strike(e: PointerEvent) {
  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
  const x = (e.clientX - rect.left) / rect.width
  const y = (e.clientY - rect.top) / rect.height
  const dx = (x - DRUM.cx) / DRUM.halfW
  const dy = (y - DRUM.cy) / DRUM.halfH
  const dist = Math.hypot(dx, dy)
  if (dist > 1.35) return // clicked the empty space around the drum
  // Zone radii match the mallet aim: Bass < 0.26 < Edge < 0.65 < Click.
  const sound = dist < 0.26 ? 'bass' : dist < 0.65 ? 'edge' : 'click'
  // triggerDrumSound clamps `when` to the audio clock's current time, so any
  // past timestamp plays immediately.
  triggerDrumSound(sound, 0)
  // The nearest mallet strikes the exact spot; clamp far clicks to the rim.
  const clamp = dist > 1.15 ? 1.15 / dist : 1
  visualStrike(sound, dx * clamp, dy * clamp, dx < 0 ? 'top' : 'bottom')
}

// Strikes arrive per line, each with an id that bumps on every new strike —
// watched independently per line (rather than one deep watch on the whole
// object) so a strike on one line doesn't re-scan the other.
function watchLineStrikes(line: 'top' | 'bottom') {
  watch(
    () => props.strikes[line],
    (s) => {
      if (!s) return
      const side = line === 'top' ? 'left' : 'right'
      const [zx, zy] = zonePoint(s.sound, side)
      visualStrike(s.sound, zx + (Math.random() - 0.5) * 0.12, zy + (Math.random() - 0.5) * 0.12, line)
    }
  )
}
watchLineStrikes('top')
watchLineStrikes('bottom')
</script>

<template>
  <div class="flex flex-col items-center gap-4">
    <div
      data-testid="drum-canvas"
      class="relative aspect-[32/30] w-[min(320px,80vw)] cursor-pointer touch-none select-none"
      @pointerdown="strike"
    >
      <!-- ground shadow -->
      <div class="absolute inset-x-[4%] top-[78%] h-[16%] rounded-[50%] bg-black/50 blur-md" />

      <!-- shell depth — dark wood layers peeking out below the frame -->
      <div class="absolute inset-x-0 top-[18%] h-[68%] rounded-[50%] bg-gradient-to-b from-stone-800 to-stone-950" />
      <div class="absolute inset-x-0 top-[12%] h-[68%] rounded-[50%] bg-gradient-to-b from-amber-900 to-stone-900" />

      <!-- wooden frame with turned wood grain -->
      <div
        class="absolute inset-x-0 top-[6%] h-[68%] rounded-[50%] bg-[repeating-conic-gradient(from_0deg_at_50%_40%,#8a5224_0deg,#9a6130_9deg,#714119_18deg,#8a5224_27deg)] shadow-[inset_0_3px_6px_rgba(255,236,200,0.22),inset_0_-5px_10px_rgba(0,0,0,0.5)]"
      />

      <!-- drumhead skin -->
      <div
        class="absolute inset-x-[9%] top-[12%] h-[56%] rounded-[50%] bg-[radial-gradient(ellipse_at_38%_30%,#f2e5cb,#e4cda2_42%,#c9a878_74%,#ab8757)] shadow-[inset_0_4px_10px_rgba(60,35,10,0.45),inset_0_-2px_4px_rgba(255,240,210,0.25)]"
      />
      <!-- tension ring + skin wrinkle hints -->
      <div class="absolute inset-x-[13%] top-[15%] h-[50%] rounded-[50%] border border-amber-900/20" />
      <div class="absolute inset-x-[31%] top-[26%] h-[28%] rounded-[50%] border border-amber-900/20" />
      <!-- sheen -->
      <div class="absolute left-[16%] top-[16%] h-[22%] w-[34%] rounded-[50%] bg-white/10 blur-[2px]" />

      <!-- strike ripples -->
      <TransitionGroup name="ripple" tag="div">
        <div
          v-for="ripple in ripples"
          :key="ripple.id"
          class="pointer-events-none absolute left-1/2 top-[40%] z-10 h-[30%] w-[46%] -translate-x-1/2 -translate-y-1/2 rounded-[50%] border-2"
          :style="{
            borderColor: ripple.color,
            marginLeft: `${ripple.x * 50}%`,
            marginTop: `${ripple.y * 34}%`,
          }"
        />
      </TransitionGroup>

      <!-- one mallet per sequencer line -->
      <Mallet side="left" :swing="swings.top" />
      <Mallet side="right" :swing="swings.bottom" />
    </div>
    <div class="flex flex-wrap items-center justify-center gap-x-4 gap-y-1 text-xs text-slate-400">
      <span v-for="(meta, key) in SOUND_META" :key="key" class="flex items-center gap-1.5">
        <SoundDot :dot-class="meta.dot" :label="meta.label" />
      </span>
    </div>
  </div>
</template>

<style scoped>
.ripple-enter-from {
  transform: translate(-50%, -50%) scale(0.2);
  opacity: 0.85;
}
.ripple-enter-active {
  transition: transform 0.7s ease-out, opacity 0.7s ease-out;
}
.ripple-enter-to {
  transform: translate(-50%, -50%) scale(1.4);
  opacity: 0;
}
</style>

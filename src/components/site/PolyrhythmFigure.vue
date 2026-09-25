<script setup lang="ts">
import { SOUND_META } from '../../lib/drumSounds'
import type { Sound } from '../../types/drum'

// Mirrors the instrument's default patch: Tone in 3 on top, Bass in 4 below,
// swept at the default 90 BPM (4 beats = 2.667s per bar).
const BAR_SECONDS = (60 / 90) * 4

const rows: { count: number; sound: Sound; label: string }[] = [
  { count: 3, sound: 'edge', label: 'Top line' },
  { count: 4, sound: 'bass', label: 'Bottom line' },
]

function positions(count: number) {
  return Array.from({ length: count }, (_, i) => i / count)
}
</script>

<template>
  <figure
    class="poly select-none"
    :style="{ '--bar': `${BAR_SECONDS}s` }"
    aria-label="Diagram: a top line of three steps and a bottom line of four steps share one bar and both start on beat one."
  >
    <div class="relative grid grid-cols-[2.5rem_1fr] items-center gap-x-4 gap-y-7 sm:grid-cols-[3.5rem_1fr]">
      <template v-for="row in rows" :key="row.sound">
        <span
          :class="['font-heading text-4xl font-black leading-none tabular-nums sm:text-5xl', SOUND_META[row.sound].text]"
          aria-hidden="true"
        >{{ row.count }}</span>
        <div class="relative h-6" aria-hidden="true">
          <div class="absolute inset-x-0 top-1/2 h-1 -translate-y-1/2 rounded-full bg-slate-800" />
          <span
            v-for="(p, i) in positions(row.count)"
            :key="i"
            :class="['poly-dot absolute top-1/2 h-5 w-5 rounded-full', SOUND_META[row.sound].dot]"
            :style="{ left: `${p * 100}%`, animationDelay: `${p * BAR_SECONDS}s` }"
          />
        </div>
      </template>
      <!-- Beat-one rule and sweeping playhead span both tracks, offset past the numeral column. -->
      <div class="pointer-events-none absolute -inset-y-2.5 left-[calc(2.5rem+1rem)] right-0 sm:left-[calc(3.5rem+1rem)]" aria-hidden="true">
        <div class="absolute inset-y-0 left-0 w-px -translate-x-1/2 bg-slate-500" />
        <div class="poly-head absolute inset-y-0 w-0.5 -translate-x-1/2 rounded-full bg-slate-300" />
      </div>
    </div>
    <figcaption class="mt-6 flex justify-between pl-[calc(2.5rem+1rem)] text-xs text-slate-500 sm:pl-[calc(3.5rem+1rem)]">
      <span>Beat one</span>
      <span>One bar</span>
    </figcaption>
  </figure>
</template>

<style scoped>
.poly-dot {
  transform: translate(-50%, -50%);
  animation: poly-hit var(--bar) cubic-bezier(0.16, 1, 0.3, 1) infinite;
}

.poly-head {
  animation: poly-sweep var(--bar) linear infinite;
}

@keyframes poly-sweep {
  from { left: 0; }
  to { left: 100%; }
}

/* Each dot flares as the playhead crosses it, then settles — the same
   1.25× + white ring the live sequencer gives its current step. */
@keyframes poly-hit {
  0% {
    transform: translate(-50%, -50%) scale(1.25);
    box-shadow: 0 0 0 2px rgb(255 255 255 / 0.9);
  }
  22%,
  100% {
    transform: translate(-50%, -50%) scale(1);
    box-shadow: 0 0 0 2px rgb(255 255 255 / 0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .poly-dot,
  .poly-head {
    animation: none;
  }

  .poly-head {
    display: none;
  }
}
</style>

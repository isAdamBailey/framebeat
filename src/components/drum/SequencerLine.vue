<script setup lang="ts">
import { computed } from 'vue'
import { SOUND_META } from '../../lib/drumSounds'
import type { Sound } from '../../types/drum'

const props = defineProps<{
  line: 'top' | 'bottom'
  dots: boolean[]
  sound: Sound
  current: number | null
  muted: boolean
}>()
const emit = defineEmits<{ toggle: [number] }>()

const meta = computed(() => SOUND_META[props.sound])
// Place each dot at its true time position in the bar (step i of n lands
// at i/n), so the playhead sweeps exactly over a dot the moment it fires.
const leftPct = (i: number) => (i / props.dots.length) * 100
</script>

<template>
  <div :data-line="line" class="relative h-10 transition-opacity" :class="muted ? 'opacity-40' : ''">
    <div class="absolute inset-x-0 top-1/2 h-0.5 -translate-y-1/2 rounded-full bg-slate-700" />
    <button
      v-for="(on, i) in dots"
      :key="i"
      type="button"
      :data-step-index="i"
      :data-on="on ? 'true' : 'false'"
      :style="{ left: `${leftPct(i)}%` }"
      :aria-label="`${line} step ${i + 1}`"
      class="absolute top-1/2 -translate-x-1/2 -translate-y-1/2 rounded-full transition-all duration-150 focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-400"
      :class="[
        on ? `h-5 w-5 ${meta.dot} shadow-lg` : 'h-4 w-4 border-2 border-slate-600 bg-slate-800 hover:border-slate-400',
        current === i ? 'z-10 scale-125 ring-2 ring-white/80' : '',
      ]"
      @click="emit('toggle', i)"
    />
  </div>
</template>

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
const hitWidth = computed(() => `min(36px, ${100 / props.dots.length}%)`)
// The dot's width, relative to its button. Once the button narrows to one
// step's slot, both dots shrink by (slot - 4px) / 24px, so neighbours keep a
// 4px gap (16 steps on a phone). Wider rows keep the full 24px and 20px.
const dotWidth = (on: boolean) => (on ? 'min(24px, calc(100% - 4px))' : 'min(20px, calc((100% - 4px) * 5 / 6))')
</script>

<template>
  <div :data-line="line" class="relative h-12 transition-opacity" :class="muted ? 'opacity-40' : ''">
    <div class="absolute inset-x-0 top-1/2 h-1 -translate-y-1/2 rounded-full bg-slate-700" />
    <!-- Each button is a transparent hit area up to 36px wide but never wider
         than one step's slot, so neighbouring targets can't overlap on phones;
         the visible dot is the span inside. -->
    <button
      v-for="(on, i) in dots"
      :key="i"
      type="button"
      :data-step-index="i"
      :data-on="on ? 'true' : 'false'"
      :style="{ left: `${leftPct(i)}%`, width: hitWidth }"
      :aria-label="`${line} line step ${i + 1}`"
      :aria-pressed="on"
      class="group absolute top-1/2 flex h-9 -translate-x-1/2 -translate-y-1/2 items-center justify-center focus:outline-none"
      :class="current === i ? 'z-10' : ''"
      @click="emit('toggle', i)"
    >
      <span
        class="aspect-square shrink-0 rounded-full transition-all duration-150 group-focus-visible:ring-2 group-focus-visible:ring-sky-400 group-focus-visible:ring-offset-2 group-focus-visible:ring-offset-slate-900"
        :class="[
          on
            ? `${meta.dot} shadow-lg group-hover:brightness-110`
            : 'border-2 border-slate-400 bg-slate-900 group-hover:scale-110 group-hover:border-white group-hover:bg-slate-700',
          current === i ? 'scale-125 ring-2 ring-white/80' : '',
        ]"
        :style="{ width: dotWidth(on) }"
      />
    </button>
  </div>
</template>

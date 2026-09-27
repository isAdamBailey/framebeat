<script setup lang="ts">
import { Volume2, VolumeX } from '@lucide/vue'
import { SOUND_META } from '../../lib/drumSounds'
import TogglePill from './TogglePill.vue'
import type { Sound } from '../../types/drum'

defineProps<{ label: string; count: number; sound: Sound; muted: boolean }>()
const emit = defineEmits<{
  countChange: [number]
  soundChange: [Sound]
  muteToggle: []
}>()
</script>

<template>
  <div class="mb-3">
    <div class="mb-2 flex items-center justify-between gap-3">
      <span class="text-[11px] font-semibold uppercase tracking-[0.14em] text-slate-400">
        {{ label }}
      </span>
      <!-- The visible text stays "Mute" so the accessible name contains it;
           aria-pressed (from TogglePill) carries the on/off state. -->
      <TogglePill :pressed="muted" :aria-label="`Mute ${label}`" @click="emit('muteToggle')">
        <VolumeX v-if="muted" class="h-4 w-4" />
        <Volume2 v-else class="h-4 w-4" />
        <!-- Icon-only on phones, where the long bottom-line label needs the row -->
        <span class="max-sm:sr-only">Mute</span>
      </TogglePill>
    </div>

    <div class="flex flex-wrap items-center gap-x-4 gap-y-3 sm:flex-nowrap sm:gap-x-5">
      <span
        class="w-[1.4ch] shrink-0 text-center font-heading text-4xl font-black leading-none tabular-nums transition-colors sm:text-5xl"
        :class="SOUND_META[sound].text"
      >
        {{ count }}
      </span>

      <input
        type="range"
        min="1"
        max="16"
        :value="count"
        class="h-2 min-w-0 flex-1 cursor-pointer accent-sky-400"
        :aria-label="`${label} step count`"
        @input="emit('countChange', Number(($event.target as HTMLInputElement).value))"
      />

      <!-- Sound picker: a labelled segmented switch of pressed-state buttons. The active segment takes
           its sound's color (the color *is* that sound), the rest stay
           readable neutral. Full width on its own row on phones. -->
      <div
        role="group"
        :aria-label="`${label} sound`"
        class="flex basis-full rounded-full border border-slate-600 bg-slate-950/60 p-0.5 sm:basis-auto"
      >
        <button
          v-for="(meta, key) in SOUND_META"
          :key="key"
          type="button"
          :aria-pressed="sound === key"
          class="flex h-8 flex-1 items-center justify-center gap-1.5 rounded-full px-3 text-xs font-semibold transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-400 sm:flex-none"
          :class="
            sound === key
              ? ['bg-slate-800 ring-1 ring-inset', meta.ring, meta.text]
              : 'text-slate-300 hover:bg-slate-800/70 hover:text-white'
          "
          @click="emit('soundChange', key as Sound)"
        >
          <span class="h-2 w-2 rounded-full" :class="meta.dot" />
          {{ meta.label }}
        </button>
      </div>
    </div>
  </div>
</template>

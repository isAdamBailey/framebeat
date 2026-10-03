<script setup lang="ts">
import { Bell, BellOff, Pause, Play } from '@lucide/vue'
import { RANGES } from '../../lib/controls'
import TogglePill from './TogglePill.vue'

defineProps<{ playing: boolean; bpm: number; chimeOnOne: boolean }>()
const emit = defineEmits<{ togglePlay: []; bpmChange: [number]; chimeToggle: [] }>()
</script>

<template>
  <div class="flex flex-col gap-6 sm:flex-row sm:items-center">
    <button
      type="button"
      class="group inline-flex h-16 w-full shrink-0 items-center justify-center gap-2.5 rounded-full bg-sky-400 px-8 text-lg font-bold text-slate-950 shadow-[0_8px_24px_rgba(56,189,248,0.35)] transition-all hover:bg-sky-300 hover:shadow-[0_8px_28px_rgba(56,189,248,0.5)] active:scale-[0.97] sm:w-44"
      :aria-label="playing ? 'Pause' : 'Play'"
      @click="emit('togglePlay')"
    >
      <Pause v-if="playing" class="h-6 w-6" fill="currentColor" />
      <Play v-else class="h-6 w-6" fill="currentColor" />
      {{ playing ? 'Pause' : 'Play' }}
    </button>
    <div class="flex flex-1 items-end gap-4 sm:gap-6">
      <label class="min-w-0 flex-1">
        <span class="flex items-baseline justify-between">
          <span class="text-[11px] font-semibold uppercase tracking-[0.14em] text-slate-400">Tempo</span>
          <span class="font-heading text-xl font-bold tabular-nums text-slate-200">{{ bpm }} <span class="text-xs font-medium text-slate-400">BPM</span></span>
        </span>
        <input
          type="range"
          :min="RANGES.bpm.min"
          :max="RANGES.bpm.max"
          :value="bpm"
          class="mt-1.5 h-2 w-full cursor-pointer accent-sky-400"
          @input="emit('bpmChange', Number(($event.target as HTMLInputElement).value))"
        />
      </label>
      <!-- The bar-marker chime on "the one". -->
      <TogglePill
        :pressed="chimeOnOne"
        title="Ring the bell on the first beat of every bar"
        @click="emit('chimeToggle')"
      >
        <Bell v-if="chimeOnOne" class="h-4 w-4" />
        <BellOff v-else class="h-4 w-4" />
        Chime on 1
      </TogglePill>
    </div>
  </div>
</template>

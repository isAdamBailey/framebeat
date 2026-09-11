<script setup lang="ts">
import { Pause, Play } from '@lucide/vue'

defineProps<{ playing: boolean; bpm: number }>()
const emit = defineEmits<{ togglePlay: []; bpmChange: [number] }>()
</script>

<template>
  <div class="flex flex-col gap-5 sm:flex-row sm:items-end">
    <button
      type="button"
      class="inline-flex h-11 w-full items-center justify-center gap-2 rounded-md bg-neutral-900 px-4 text-sm font-medium text-white shadow transition-colors hover:bg-neutral-800 sm:w-32"
      @click="emit('togglePlay')"
    >
      <Pause v-if="playing" class="h-4 w-4" />
      <Play v-else class="h-4 w-4" />
      {{ playing ? 'Pause' : 'Play' }}
    </button>
    <label class="flex-1">
      <span class="flex justify-between text-xs font-medium text-slate-400">
        <span>Tempo</span>
        <span>{{ bpm }} BPM</span>
      </span>
      <input
        type="range"
        min="40"
        max="200"
        :value="bpm"
        class="mt-1 w-full accent-sky-400"
        @input="emit('bpmChange', Number(($event.target as HTMLInputElement).value))"
      />
    </label>
  </div>
</template>

<script setup lang="ts">
import { Volume2, VolumeX } from '@lucide/vue'
import { SOUND_META } from '../../lib/drumSounds'
import SoundDot from './SoundDot.vue'
import type { Sound } from '../../types/drum'

defineProps<{ label: string; count: number; sound: Sound; muted: boolean }>()
const emit = defineEmits<{
  countChange: [number]
  soundChange: [Sound]
  muteToggle: []
}>()
</script>

<template>
  <div class="mb-2 flex flex-wrap items-center gap-x-5 gap-y-2">
    <span class="text-xs font-semibold uppercase tracking-wider text-slate-400">
      {{ label }}
    </span>
    <label class="flex items-center gap-2">
      <span class="text-xs text-slate-400">Notes</span>
      <input
        type="range"
        min="1"
        max="16"
        :value="count"
        class="w-24 accent-sky-400"
        @input="emit('countChange', Number(($event.target as HTMLInputElement).value))"
      />
      <span class="w-5 text-xs font-medium text-slate-300">{{ count }}</span>
    </label>
    <div class="flex items-center gap-1">
      <button
        v-for="(meta, key) in SOUND_META"
        :key="key"
        type="button"
        class="flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs transition"
        :class="sound === key ? 'bg-slate-700 text-slate-100' : 'text-slate-400 hover:bg-slate-800'"
        @click="emit('soundChange', key as Sound)"
      >
        <SoundDot :dot-class="meta.dot" :label="meta.label" />
      </button>
    </div>
    <button
      type="button"
      class="flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs transition"
      :class="muted ? 'bg-slate-700 text-slate-100' : 'text-slate-400 hover:bg-slate-800'"
      :aria-label="muted ? 'Unmute line' : 'Mute line'"
      @click="emit('muteToggle')"
    >
      <VolumeX v-if="muted" class="h-3.5 w-3.5" />
      <Volume2 v-else class="h-3.5 w-3.5" />
      {{ muted ? 'Muted' : 'Mute' }}
    </button>
  </div>
</template>

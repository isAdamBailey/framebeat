<script setup lang="ts">
import { Volume2, VolumeX } from '@lucide/vue'
import { SOUND_META } from '../../lib/drumSounds'
import type { Sound } from '../../types/drum'

defineProps<{ label: string; count: number; sound: Sound; muted: boolean }>()
const emit = defineEmits<{
  countChange: [number]
  soundChange: [Sound]
  muteToggle: []
}>()
</script>

<template>
  <div class="mb-2.5">
    <div class="mb-1.5 flex items-center justify-between">
      <span class="text-[11px] font-semibold uppercase tracking-[0.14em] text-slate-500">
        {{ label }}
      </span>
      <button
        type="button"
        class="rounded-full p-1.5 transition"
        :class="muted ? 'text-slate-200' : 'text-slate-600 hover:text-slate-400'"
        :aria-label="muted ? 'Unmute line' : 'Mute line'"
        @click="emit('muteToggle')"
      >
        <VolumeX v-if="muted" class="h-4 w-4" />
        <Volume2 v-else class="h-4 w-4" />
      </button>
    </div>

    <div class="flex items-center gap-4 sm:gap-5">
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
        class="h-1.5 w-full min-w-0 flex-1 accent-sky-400"
        :aria-label="`${label} step count`"
        @input="emit('countChange', Number(($event.target as HTMLInputElement).value))"
      />

      <div class="flex shrink-0 items-center gap-1">
        <button
          v-for="(meta, key) in SOUND_META"
          :key="key"
          type="button"
          class="flex h-6 w-6 items-center justify-center rounded-full transition"
          :aria-label="`Play ${meta.label} on this line`"
          :title="meta.label"
          @click="emit('soundChange', key as Sound)"
        >
          <span
            class="rounded-full transition-all"
            :class="[meta.dot, sound === key ? 'h-3 w-3' : 'h-2 w-2 opacity-35']"
          />
        </button>
      </div>
    </div>
  </div>
</template>

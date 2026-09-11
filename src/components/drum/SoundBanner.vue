<script setup lang="ts">
import { VolumeX } from '@lucide/vue'
import { getAudioContext } from '../../lib/drumAudio'
</script>

<template>
  <button
    type="button"
    class="mb-6 flex w-full items-center justify-center gap-2 rounded-lg border border-amber-700/60 bg-amber-900/30 px-4 py-3 text-sm text-amber-200 transition hover:bg-amber-900/50"
    @click="getAudioContext()"
  >
    <VolumeX class="h-4 w-4" />
    Sound is blocked by your browser — click here to enable audio
  </button>
</template>

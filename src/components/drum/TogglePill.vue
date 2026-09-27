<script setup lang="ts">
// The panel's one secondary-control shape: a bordered pill with a readable
// label, pressed = lit neutral fill. Mute and Chime on 1 both use it, so
// every toggle around the sequencer reads and hits the same way.
defineProps<{ pressed: boolean }>()
</script>

<template>
  <button
    type="button"
    class="inline-flex h-9 shrink-0 items-center justify-center gap-1.5 rounded-full border px-3.5 text-xs font-semibold transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-400 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-900 active:scale-[0.97]"
    :class="
      pressed
        ? 'border-slate-400 bg-slate-700 text-white hover:bg-slate-600'
        : 'border-slate-500 bg-transparent text-slate-300 hover:border-slate-300 hover:bg-slate-800 hover:text-white'
    "
    :aria-pressed="pressed"
  >
    <slot />
  </button>
</template>

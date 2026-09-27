<script setup lang="ts">
// DESIGN.md's Kbd Key: reads as a physical keycap. `small` is the caption size.
defineProps<{ small?: boolean }>()
</script>

<template>
  <kbd
    class="whitespace-nowrap rounded border border-stone-700 bg-stone-800/60 font-sans"
    :class="small ? 'px-1 text-[10px] leading-4 text-stone-400' : 'px-1.5 py-0.5 text-stone-300'"
  >
    <slot />
  </kbd>
</template>

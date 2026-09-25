<script setup lang="ts">
import { ArrowUpRight } from '@lucide/vue'
import { APP_STORE_URL } from '../../lib/links'

// `quiet` sits in the stone-toned hero; `solid` is the neutral CTA
// below the panel. Neither uses a sound color or a glow — Play stays the
// page's one lit control.
withDefaults(defineProps<{ variant?: 'quiet' | 'solid' }>(), { variant: 'quiet' })
</script>

<template>
  <a
    :href="APP_STORE_URL"
    target="_blank"
    rel="noopener"
    :class="[
      'inline-flex shrink-0 items-center justify-center gap-1.5 whitespace-nowrap rounded-full font-semibold transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-950',
      variant === 'solid'
        ? 'h-12 bg-slate-100 px-6 text-sm text-slate-950 hover:bg-white focus-visible:ring-slate-100 active:scale-[0.98]'
        : 'h-10 border border-stone-700 px-5 text-sm text-stone-300 hover:border-stone-500 hover:text-stone-100 focus-visible:ring-stone-300',
    ]"
  >
    <slot>Download on the App Store</slot>
    <ArrowUpRight :class="variant === 'solid' ? 'h-4 w-4' : 'h-3.5 w-3.5'" aria-hidden="true" />
    <span class="sr-only">(opens the App Store in a new tab)</span>
  </a>
</template>

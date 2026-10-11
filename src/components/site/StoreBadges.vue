<script setup lang="ts">
import { APP_STORE_URL, PLAY_STORE_LIVE, PLAY_STORE_URL } from '../../lib/links'

// The official Apple and Google badges, unaltered and at equal height, as
// both brand guidelines ask. Until the Android app leaves closed testing the
// Play badge is shown dimmed and unlinked, with a "Coming soon" note.
</script>

<template>
  <div class="flex flex-wrap items-start gap-3">
    <a
      :href="APP_STORE_URL"
      target="_blank"
      rel="noopener"
      class="rounded-lg transition-opacity hover:opacity-85 focus:outline-none focus-visible:ring-2 focus-visible:ring-stone-300 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-950"
    >
      <img src="/badges/app-store.svg" alt="Download on the App Store" width="120" height="40" class="block h-10 w-auto" />
      <span class="sr-only">(opens the App Store in a new tab)</span>
    </a>
    <a
      v-if="PLAY_STORE_LIVE"
      :href="PLAY_STORE_URL"
      target="_blank"
      rel="noopener"
      class="rounded-lg transition-opacity hover:opacity-85 focus:outline-none focus-visible:ring-2 focus-visible:ring-stone-300 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-950"
    >
      <img src="/badges/google-play.png" alt="Get it on Google Play" width="134" height="40" class="block h-10 w-auto" />
      <span class="sr-only">(opens Google Play in a new tab)</span>
    </a>
    <div v-else class="flex flex-col items-center gap-1">
      <img
        src="/badges/google-play.png"
        alt="Google Play, coming soon"
        width="134"
        height="40"
        class="block h-10 w-auto opacity-50"
      />
      <span class="text-[11px] font-medium uppercase tracking-wider text-stone-400">Coming soon</span>
    </div>
  </div>
</template>

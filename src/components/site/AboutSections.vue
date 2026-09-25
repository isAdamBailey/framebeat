<script setup lang="ts">
import AppStoreLink from './AppStoreLink.vue'
import PolyrhythmFigure from './PolyrhythmFigure.vue'
import { PRIVACY_URL } from '../../lib/links'

const facts: { title: string; body: string }[] = [
  {
    title: 'Synthesized, not sampled',
    body: 'The synth engine and scheduler are ported from this page, so every hit is still generated live on your device.',
  },
  {
    title: 'Keys under your fingers',
    body: 'Drum along on Q W E and I O P on the Mac — and on iPad, too, with a hardware keyboard attached.',
  },
  {
    title: 'Nothing leaves the device',
    body: 'No accounts, no analytics, no network access. Your pattern lives in memory until you close the app.',
  },
]
</script>

<template>
  <div class="mt-20 space-y-20 sm:mt-28 sm:space-y-28">
    <section aria-labelledby="how-heading" class="grid gap-10 sm:grid-cols-[1fr_1.1fr] sm:items-center sm:gap-12">
      <div>
        <h2 id="how-heading" class="text-balance text-2xl font-semibold tracking-tight text-slate-100 sm:text-3xl">
          One bar, counted two ways at once.
        </h2>
        <p class="mt-4 max-w-[46ch] text-pretty text-sm leading-relaxed text-slate-400 sm:text-base">
          The bottom line sets the tempo and the length of the bar. The top line slices that same bar into its own
          number of steps — three against four, five against seven, anything up to sixteen. However far apart they
          wander, both lines land together on beat one.
        </p>
      </div>
      <PolyrhythmFigure />
    </section>

    <section aria-labelledby="app-heading" class="border-t border-slate-800 pt-14 sm:pt-20">
      <div class="flex flex-col gap-6 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 id="app-heading" class="text-balance text-2xl font-semibold tracking-tight text-slate-100 sm:text-3xl">
            Take the drum with you, on Mac and iPad.
          </h2>
          <p class="mt-4 max-w-[52ch] text-pretty text-sm leading-relaxed text-slate-400 sm:text-base">
            FrameBeat is also a native app — the same instrument, rebuilt in SwiftUI, with one App Store listing for both.
          </p>
        </div>
        <AppStoreLink variant="solid" class="self-start sm:self-auto" />
      </div>

      <dl class="mt-12 grid gap-8 sm:grid-cols-3 sm:gap-10">
        <div v-for="fact in facts" :key="fact.title" class="border-t border-slate-800 pt-5">
          <dt class="text-sm font-semibold text-slate-200">{{ fact.title }}</dt>
          <dd class="mt-2 text-pretty text-sm leading-relaxed text-slate-400">{{ fact.body }}</dd>
        </div>
      </dl>
      <p class="mt-8 text-xs text-slate-500">
        Details in the
        <a
          :href="PRIVACY_URL"
          target="_blank"
          rel="noopener"
          class="underline decoration-slate-700 underline-offset-4 transition-colors hover:text-slate-300 hover:decoration-slate-400"
        >privacy policy</a>.
      </p>
    </section>
  </div>
</template>

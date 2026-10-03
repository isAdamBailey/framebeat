<script setup lang="ts">
import { useHead } from '@unhead/vue'
import { PRIVACY_URL } from '../lib/links'

useHead({
  title: 'Privacy — FrameBeat',
  link: [{ rel: 'canonical', href: PRIVACY_URL }],
  meta: [
    {
      name: 'description',
      content:
        'The FrameBeat website uses Google Analytics. The Mac, iPad, and Android apps do not collect personal data or usage data.',
    },
  ],
})
</script>

<template>
  <div class="min-h-screen bg-slate-950 text-slate-100">
    <div class="mx-auto max-w-3xl px-4 pb-8 pt-6 sm:pb-10 sm:pt-14">
      <p class="text-sm text-stone-500">
        <RouterLink to="/" class="transition-colors hover:text-stone-300">FrameBeat</RouterLink>
      </p>
      <h1 class="mt-8 font-heading text-balance text-4xl font-semibold leading-[1.05] tracking-tight text-stone-200 sm:text-5xl">
        Privacy
      </h1>
      <div class="mt-8 max-w-[62ch] space-y-5 text-pretty text-sm leading-relaxed text-stone-400 sm:text-base">
        <p>This website uses Google Analytics to count visits.</p>
        <p>
          The Mac, iPad, and Android apps do not collect, store, transmit, or share personal data or usage data.
          They have no accounts, no network access, no analytics, and no third-party SDKs. Audio is synthesized on the device, and
          the pattern you program stays in memory for the current session.
        </p>
        <p>
          Questions:
          <a
            href="mailto:adamjbailey7@gmail.com"
            class="underline decoration-stone-700 underline-offset-4 transition-colors hover:text-stone-300 hover:decoration-stone-400"
          >adamjbailey7@gmail.com</a>.
        </p>
      </div>
      <p class="mt-16 text-xs text-slate-600">Last updated October 2, 2026.</p>
    </div>
  </div>
</template>

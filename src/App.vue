<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import DrumCanvas from './components/drum/DrumCanvas.vue'
import Bell from './components/drum/Bell.vue'
import Sequencer from './components/drum/Sequencer.vue'
import TransportControls from './components/drum/TransportControls.vue'
import SoundBanner from './components/drum/SoundBanner.vue'
import { useSequencer } from './composables/useSequencer'
import { subscribeAudioState, isAudioBlocked } from './lib/drumAudio'
import type { Line, Strikes } from './types/drum'

const defaultDots = () => Array.from({ length: 16 }, () => true)

const bpm = ref(90)
const top = reactive<Line>({ count: 3, sound: 'edge', dots: defaultDots(), muted: false })
const bottom = reactive<Line>({ count: 4, sound: 'bass', dots: defaultDots(), muted: false })
const strikes = reactive<Strikes>({ top: null, bottom: null })
const bellTrigger = ref<number | null>(null)
const soundBlocked = ref(false)

const { playing, togglePlay, progress, currentTop, currentBottom } = useSequencer({
  top,
  bottom,
  bpm,
  onStep: (sound, line) => {
    strikes[line] = { sound, line, id: (strikes[line]?.id ?? 0) + 1 }
  },
  onDing: () => {
    bellTrigger.value = performance.now()
  },
})

let unsubscribeAudioState: (() => void) | undefined
onMounted(() => {
  unsubscribeAudioState = subscribeAudioState(() => {
    soundBlocked.value = isAudioBlocked()
  })
})
onUnmounted(() => unsubscribeAudioState?.())

function lineState(line: 'top' | 'bottom') {
  return line === 'top' ? top : bottom
}

function toggleDot(line: 'top' | 'bottom', idx: number) {
  const target = lineState(line)
  target.dots = target.dots.map((d, i) => (i === idx ? !d : d))
}

function patchLine(line: 'top' | 'bottom', patch: Partial<Line>) {
  Object.assign(lineState(line), patch)
}
</script>

<template>
  <div class="min-h-screen bg-slate-950 text-slate-100">
    <div class="mx-auto max-w-3xl px-4 py-8 sm:py-10">
      <header class="mb-6 text-center">
        <h1 class="font-heading text-balance text-2xl font-semibold tracking-tight text-stone-300 sm:text-3xl">
          Frame Drum <span class="italic text-stone-500">&amp;</span> Step Sequencer
        </h1>
        <p class="mt-2.5 text-xs text-stone-500 sm:text-sm">
          Click the drum, or focus it and drum along on
          <kbd class="rounded border border-stone-700 bg-stone-800/60 px-1.5 py-0.5 font-sans text-stone-300">Q W E</kbd>
          /
          <kbd class="rounded border border-stone-700 bg-stone-800/60 px-1.5 py-0.5 font-sans text-stone-300">I O P</kbd>
        </p>
      </header>

      <SoundBanner v-if="soundBlocked" />

      <div class="relative mb-8 flex items-end justify-center gap-2 py-4 sm:gap-10">
        <div
          class="pointer-events-none absolute inset-0 -z-10 bg-[radial-gradient(ellipse_at_center,rgba(30,41,59,0.55),transparent_65%)]"
        />
        <DrumCanvas :strikes="strikes" :playing="playing" />
        <div class="origin-bottom scale-75 sm:scale-100">
          <Bell :trigger="bellTrigger" />
        </div>
      </div>

      <section class="mb-6 rounded-xl border border-slate-800 bg-slate-900/70 p-4 sm:p-6">
        <TransportControls :playing="playing" :bpm="bpm" @toggle-play="togglePlay" @bpm-change="(v) => (bpm = v)" />
        <div class="mt-8">
          <Sequencer
            :top="top"
            :bottom="bottom"
            :current-top="currentTop"
            :current-bottom="currentBottom"
            :progress="progress"
            @toggle="toggleDot"
            @patch="patchLine"
          />
        </div>
      </section>

      <footer class="pb-2 text-center text-xs text-slate-600">
        <a
          href="https://adambailey.io"
          target="_blank"
          rel="noopener"
          class="transition-colors hover:text-slate-400"
        >
          &copy; Adam Bailey
        </a>
      </footer>
    </div>
  </div>
</template>

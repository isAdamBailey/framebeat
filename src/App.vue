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
    <div class="mx-auto max-w-3xl px-4 py-10 sm:py-12">
      <header class="mb-8 text-center">
        <h1 class="font-heading text-2xl font-bold tracking-tight sm:text-3xl">
          Frame Drum &amp; Step Sequencer
        </h1>
        <p class="mt-2 text-sm text-slate-400">
          Click the drum to play · toggle steps on either line · pick each line's sound
        </p>
      </header>

      <SoundBanner v-if="soundBlocked" />

      <div class="mb-8 flex items-end justify-center gap-6 sm:gap-10">
        <DrumCanvas :strikes="strikes" />
        <Bell :trigger="bellTrigger" />
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
    </div>
  </div>
</template>

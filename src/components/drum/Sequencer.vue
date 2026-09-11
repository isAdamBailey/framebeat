<script setup lang="ts">
import { computed } from 'vue'
import LineHeader from './LineHeader.vue'
import SequencerLine from './SequencerLine.vue'
import type { Line, Sound } from '../../types/drum'

const props = defineProps<{
  top: Line
  bottom: Line
  currentTop: number | null
  currentBottom: number | null
  progress: number
}>()
const emit = defineEmits<{
  toggle: [line: 'top' | 'bottom', idx: number]
  patch: [line: 'top' | 'bottom', patch: Partial<Line>]
}>()

// Memoized so the sliced array keeps a stable reference (and SequencerLine's
// dots prop with it) across the 60fps `progress` updates driving the playhead.
const topDots = computed(() => props.top.dots.slice(0, props.top.count))
const bottomDots = computed(() => props.bottom.dots.slice(0, props.bottom.count))
</script>

<template>
  <div>
    <div class="mx-6 sm:mx-10">
      <LineHeader
        label="Top line"
        :count="top.count"
        :sound="top.sound"
        :muted="top.muted"
        @count-change="(count) => emit('patch', 'top', { count })"
        @sound-change="(sound: Sound) => emit('patch', 'top', { sound })"
        @mute-toggle="emit('patch', 'top', { muted: !top.muted })"
      />
    </div>
    <div class="relative mx-6 py-2 sm:mx-10">
      <div
        data-testid="playhead"
        class="absolute inset-y-0 w-1 -translate-x-1/2 rounded-full bg-sky-400 shadow-[0_0_12px_rgba(56,189,248,0.9)]"
        :style="{ left: `${progress * 100}%` }"
      />
      <SequencerLine
        line="top"
        :dots="topDots"
        :sound="top.sound"
        :current="currentTop"
        :muted="top.muted"
        @toggle="(i) => emit('toggle', 'top', i)"
      />
      <div class="h-6" />
      <SequencerLine
        line="bottom"
        :dots="bottomDots"
        :sound="bottom.sound"
        :current="currentBottom"
        :muted="bottom.muted"
        @toggle="(i) => emit('toggle', 'bottom', i)"
      />
    </div>
    <div class="mx-6 mt-4 sm:mx-10">
      <LineHeader
        label="Bottom line — sets the pulse"
        :count="bottom.count"
        :sound="bottom.sound"
        :muted="bottom.muted"
        @count-change="(count) => emit('patch', 'bottom', { count })"
        @sound-change="(sound: Sound) => emit('patch', 'bottom', { sound })"
        @mute-toggle="emit('patch', 'bottom', { muted: !bottom.muted })"
      />
    </div>
  </div>
</template>

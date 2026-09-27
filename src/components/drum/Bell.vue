<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { useAnimate } from '../../composables/useAnimate'
import Kbd from './Kbd.vue'

const props = defineProps<{ trigger: number | null }>()
const emit = defineEmits<{ ring: [] }>()

const bodyEl = ref<HTMLElement | null>(null)
const clapperEl = ref<HTMLElement | null>(null)
const shineEl = ref<HTMLElement | null>(null)
const showShine = ref(false)

const body = useAnimate(bodyEl)
const clapper = useAnimate(clapperEl)
const shine = useAnimate(shineEl)

// A hanging chime that swings whenever it rings: on the bar's "one" (when
// the sequencer's chime is on), or when struck by hand.
watch(
  () => props.trigger,
  async (t) => {
    if (!t) return

    body.replay(
      [
        { transform: 'rotate(0deg)' },
        { transform: 'rotate(-16deg)' },
        { transform: 'rotate(12deg)' },
        { transform: 'rotate(-8deg)' },
        { transform: 'rotate(4deg)' },
        { transform: 'rotate(0deg)' },
      ],
      { duration: 800, easing: 'ease-out' }
    )

    clapper.replay(
      [
        { transform: 'rotate(0deg)' },
        { transform: 'rotate(14deg)' },
        { transform: 'rotate(-10deg)' },
        { transform: 'rotate(5deg)' },
        { transform: 'rotate(0deg)' },
      ],
      { duration: 700, easing: 'ease-out' }
    )

    showShine.value = true
    await nextTick()
    const shineAnim = shine.replay(
      [
        { opacity: 0, transform: 'translateX(0px)' },
        { opacity: 0.6, transform: 'translateX(8px)', offset: 0.5 },
        { opacity: 0, transform: 'translateX(16px)' },
      ],
      { duration: 600, easing: 'ease-out' }
    )
    shineAnim?.addEventListener('finish', () => {
      showShine.value = false
    })
  }
)

// Rings on pointer-down (like the drum) so a tap feels instant, and on
// Enter from the keyboard. There's no click handler, so Space's native
// button activation does nothing here; Space and B are page shortcuts
// handled in HomeView.
function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter') {
    e.preventDefault()
    emit('ring')
  }
}
</script>

<template>
  <div class="flex flex-col items-center gap-1.5">
    <!-- The art is drawn at its original base size; zoom (not transform: scale)
         sizes it per breakpoint and scales the layout box with it, so phones
         keep it small enough to sit beside the drum without sideways scroll. -->
    <button
      type="button"
      aria-label="Bell. Click or tap to ring it; B rings it while the drum or bell is focused. Space plays or pauses."
      class="group relative h-[138px] w-28 cursor-pointer [zoom:0.94] sm:[zoom:1.25] md:[zoom:1.625] touch-none select-none rounded-2xl border-0 bg-transparent p-0 focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-400 focus-visible:ring-offset-4 focus-visible:ring-offset-slate-950"
      @pointerdown="emit('ring')"
      data-instrument
      @keydown="onKeydown"
    >
      <!-- stand arm -->
      <div class="absolute left-1/2 top-0 h-6 w-2 -translate-x-1/2 rounded bg-stone-700 shadow" />

      <!-- hover lean hints that the bell can be struck; the ring swing replays on the inner element -->
      <div
        class="absolute left-1/2 top-5 -translate-x-1/2 origin-top transition-transform duration-300 ease-out group-hover:rotate-[-4deg] motion-reduce:transition-none"
      >
        <div ref="bodyEl" style="transform-origin: 50% 0%">
          <!-- cord -->
          <div class="mx-auto h-5 w-0.5 bg-stone-500" />

          <!-- hanging loop -->
          <div class="mx-auto -mb-1 h-3 w-3.5 rounded-t-full border-2 border-amber-700" />

          <!-- dome -->
          <div class="mx-auto h-6 w-11 rounded-t-full bg-[linear-gradient(160deg,#f8dd85,#d9a836_55%,#a06d1c)]" />

          <!-- body tapering to the mouth -->
          <div
            class="mx-auto h-6 w-14 rounded-b-[30%] bg-[linear-gradient(155deg,#f5d879_8%,#d9a836_50%,#8f6319)] shadow-[0_4px_10px_rgba(0,0,0,0.45)]"
          />

          <!-- flared lip -->
          <div
            class="mx-auto -mt-1 h-2 w-16 rounded-[50%] bg-[linear-gradient(160deg,#e9bd55,#8f6319)] shadow-[0_2px_5px_rgba(0,0,0,0.5)]"
          />

          <!-- clapper -->
          <div ref="clapperEl" class="mx-auto mt-0.5" style="transform-origin: 50% 0%">
            <div class="mx-auto h-2 w-0.5 bg-stone-600" />
            <div class="mx-auto h-2 w-2 rounded-full bg-amber-800 shadow-sm" />
          </div>
        </div>

        <!-- soft shine flash on ring -->
        <div
          v-if="showShine"
          ref="shineEl"
          class="pointer-events-none absolute left-0 top-5 h-10 w-5 rounded-full bg-white/70 blur-[3px]"
        />
      </div>
    </button>
    <span class="flex items-center gap-1.5 text-[11px] text-slate-500" aria-hidden="true">
      Bell
      <Kbd small>B</Kbd>
    </span>
  </div>
</template>

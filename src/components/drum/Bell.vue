<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { useAnimate } from '../../composables/useAnimate'

const props = defineProps<{ trigger: number | null }>()

const bodyEl = ref<HTMLElement | null>(null)
const clapperEl = ref<HTMLElement | null>(null)
const shineEl = ref<HTMLElement | null>(null)
const showShine = ref(false)

const body = useAnimate(bodyEl)
const clapper = useAnimate(clapperEl)
const shine = useAnimate(shineEl)

// A small hanging chime that swings when the bar's "one" ding is heard.
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
</script>

<template>
  <div class="flex flex-col items-center gap-2" aria-hidden="true">
    <div class="relative h-40 w-32">
      <!-- stand arm -->
      <div class="absolute left-1/2 top-0 h-6 w-2 -translate-x-1/2 rounded bg-stone-700 shadow" />

      <div class="absolute left-1/2 top-5 -translate-x-1/2">
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
          class="absolute left-0 top-5 h-10 w-5 rounded-full bg-white/70 blur-[3px]"
        />
      </div>
    </div>
    <span class="text-[11px] text-slate-500">Bar chime</span>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useAnimate } from '../../composables/useAnimate'
import type { Swing } from '../../types/drum'

const props = defineProps<{ swing: Swing; side: 'left' | 'right' }>()

const el = ref<HTMLElement | null>(null)
const { replay } = useAnimate(el)

// Small wind-up away from the drum before the tip lands on its target.
watch(
  () => props.swing.id,
  () => {
    const wind = props.side === 'left' ? -7 : 7
    replay(
      [
        { transform: 'translate(0%, 0%) rotate(0deg)', offset: 0 },
        { transform: `translate(0%, -3%) rotate(${wind}deg)`, offset: 0.28 },
        { transform: `translate(${props.swing.x}, ${props.swing.y}) rotate(${props.swing.rot}deg)`, offset: 0.5 },
        { transform: 'translate(0%, 0%) rotate(0deg)', offset: 1 },
      ],
      { duration: 260, easing: 'ease-out' }
    )
  }
)
</script>

<template>
  <div
    ref="el"
    class="pointer-events-none absolute bottom-[2%] z-20 h-[58%] w-[38%]"
    :class="side === 'left' ? 'left-[2%]' : 'right-[2%]'"
    :style="{ transformOrigin: side === 'left' ? '12% 96%' : '88% 96%' }"
  >
    <div
      class="absolute bottom-[4%] h-[70%] w-[10px] origin-bottom rounded-full bg-gradient-to-t from-amber-950 via-amber-800 to-amber-300 shadow-md"
      :class="side === 'left' ? 'left-[8%] rotate-[24deg]' : 'right-[8%] rotate-[-24deg]'"
    >
      <!-- felt tip -->
      <div
        class="absolute -top-2 left-1/2 h-6 w-6 -translate-x-1/2 rounded-full bg-[radial-gradient(circle_at_35%_30%,#efe0bd,#d3b686_55%,#a3805a)] shadow-[0_2px_4px_rgba(0,0,0,0.4)]"
      />
    </div>
  </div>
</template>

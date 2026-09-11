import type { Ref } from 'vue'

// Replays a Web Animations API keyframe animation on an element, cancelling
// any animation already in flight so rapid re-triggers don't stack.
export function useAnimate(el: Ref<HTMLElement | null>) {
  function replay(keyframes: Keyframe[], options: KeyframeAnimationOptions) {
    el.value?.getAnimations().forEach((a) => {
      a.cancel()
    })
    return el.value?.animate(keyframes, options)
  }
  return { replay }
}

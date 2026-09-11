import type { Sound } from '../types/drum'

// Drum ellipse geometry as fractions of the canvas box.
export const DRUM = { cx: 0.5, cy: 0.4, halfW: 0.5, halfH: 0.34 }

// Canvas-unit geometry (canvas = 100 x 93.75): where each mallet pivots and
// how far its felt tip sits from that pivot, so strikes can be aimed exactly.
export const CANVAS = { w: 100, h: 93.75 }
export const MALLETS = {
  left: { pivot: { x: 6.56, y: 89.7 }, baseRot: 24 },
  right: { pivot: { x: 93.44, y: 89.7 }, baseRot: -24 },
}
export const REACH = 0.7 * 0.58 * CANVAS.h // felt-tip distance from the pivot

// Each mallet rests on the frame's side — that resting spot is the Click
// zone. Edge sits two thirds of the way in from there, Bass at the centre.
const CLICK_POINT: Record<'left' | 'right', [number, number]> = {
  left: [-0.56, 0.55],
  right: [0.56, 0.55],
}

export function zonePoint(sound: Sound, side: 'left' | 'right'): [number, number] {
  const [cx, cy] = CLICK_POINT[side]
  if (sound === 'click') return [cx, cy]
  if (sound === 'edge') return [(cx * 2) / 3, (cy * 2) / 3]
  return [0, 0]
}

// Rotation + shift (as % of the mallet's own box) that land the felt tip on
// the drum-ellipse point (gx, gy).
export function swingFor(side: 'left' | 'right', gx: number, gy: number) {
  const { pivot, baseRot } = MALLETS[side]
  const rx = (DRUM.cx + DRUM.halfW * gx) * CANVAS.w - pivot.x
  const ry = (DRUM.cy + DRUM.halfH * gy) * CANVAS.h - pivot.y
  const d = Math.hypot(rx, ry) || 1
  const angle = (Math.atan2(rx, -ry) * 180) / Math.PI // 0 = straight up
  const box = { w: 0.38 * CANVAS.w, h: 0.58 * CANVAS.h }
  return {
    rot: angle - baseRot,
    x: `${(((rx - (REACH * rx) / d) / box.w) * 100).toFixed(2)}%`,
    y: `${(((ry - (REACH * ry) / d) / box.h) * 100).toFixed(2)}%`,
  }
}
